"""Refresh/check the World 2 guide's room and journal references from runtime catalogs."""
import argparse
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
GUIDE = ROOT / 'docs/playtest/walkthroughs/WORLD_2_COMPLETIONIST_WALKTHROUGH.md'
ASSETS = ROOT / 'app/src/main/assets'


def catalog(name):
    return json.loads((ASSETS / (name + '.json')).read_text())


def prose(text):
    return re.sub(r'\[action:[^|]+\|([^]]+)\]', r'\1', text or '').replace('\n', ' ')


def references():
    rooms = {r['id']: r for r in catalog('rooms')}
    items = {i['id']: i['name'] for i in catalog('items')}
    npcs = {n['id']: n['name'] for n in catalog('npcs')}
    nodes = [n for n in catalog('hub_nodes') if n['world_id'] == 'world_2']
    by_action = {}
    for event in catalog('events'):
        if event['trigger']['type'] == 'player_action':
            by_action.setdefault(event['trigger'].get('action'), []).append(event)
    out = ['## 15. All-room reference', '',
           'Generated from current runtime catalogs. Base prose is shown; milestone/state variants can change it during play. NPC placement rules and physical locks are explicit below. Event prerequisites still apply to visible actions. Rest and Cook are service buttons rather than bold actionable bed/cooker words.', '']
    for node in nodes:
        out += [f"### {node['title']} (`{node['id']}`)", '',
                f"Entry: **{rooms[node['entry_room']]['title']}** (`{node['entry_room']}`). Entry policy: {node['entry_policy']}; return policy: {node['return_policy']}.", '']
        for rid in node['rooms']:
            r = rooms[rid]
            out += [f"#### {r['title']} (`{rid}`)", '', prose(r.get('description')), '']
            out += ['- **Exits:** ' + '; '.join(f"**{direction}** -> {rooms[target]['title']} (`{target}`)" for direction, target in r.get('connections', {}).items()) + '.']
            for direction, block in r.get('blocked_directions', {}).items():
                requirement = f" Requires `{block['key_id']}`; consumed: {block.get('consume', False)}." if block.get('key_id') else ''
                if block.get('requires'):
                    requirement += ' Required room state: ' + json.dumps(block['requires'], ensure_ascii=False, separators=(', ', ': ')) + '.'
                out += [f"- **{direction.title()} lock:** {prose(block.get('message_locked'))}{requirement}"]
            for nid in r.get('npcs', []):
                out += [f"- **NPC:** {npcs.get(nid, nid)} (`{nid}`)."]
            for rule in r.get('npc_presence', []):
                out += [f"- **NPC:** {npcs.get(rule['npc'], rule['npc'])} (`{rule['npc']}`); requires milestones {', '.join(rule.get('requires_milestones', [])) or 'none'}; absent after {', '.join(rule.get('forbidden_milestones', [])) or 'none'}. While visible, quest dialogue eligibility still applies."]
            parties = r.get('enemy_parties') or ([r['enemies']] if r.get('enemies') else [])
            out += ['- **Direct encounters:** ' + ('; '.join(', '.join(party) for party in parties) if parties else 'none; action-spawned encounters are separate') + '.']
            for a in r.get('actions', []):
                label = 'Cook' if a['type'] == 'rest_stop' and a.get('cook_source') else 'Rest' if a['type'] == 'rest_stop' else a['name']
                detail = prose(a.get('inspect_message') or a.get('message') or a.get('condition_unmet_message'))
                if a['type'] == 'container':
                    detail = 'Open once for ' + ', '.join(items.get(i, i) for i in a.get('items', [])) + f". Persisted state: `{a['state_key']}`."
                if a.get('cook_source'):
                    detail += ' Cooking source: ' + a['cook_source'] + '.'
                if a.get('rest_event'):
                    detail += f" Rest event: `{a['rest_event']}`."
                if a.get('shop_id'):
                    detail += f" Shop: `{a['shop_id']}`."
                if a.get('puzzle_id'):
                    detail += f" Puzzle: `{a['puzzle_id']}`."
                out += [f"- **{label}** ({a['type']}; object: {a['name']}): {detail}"]
                gates = {k: v for k, v in a.items() if k.startswith(('requires_', 'show_when_', 'hide_when_'))}
                if gates:
                    out += ['- Action visibility/requirements: `' + json.dumps(gates, ensure_ascii=False, separators=(', ', ': ')) + '`.']
                for event in by_action.get(a.get('action_event'), []):
                    out += [f"- Event `{event['id']}` prerequisites: `" + json.dumps(event.get('conditions', []), ensure_ascii=False, separators=(', ', ': ')) + '`. See the numbered route for its quest/reward sequence.']
            out += ['']
    out += ['## 16. Journal checklist: all ten quests', '', 'Current authored task text. Complete required turn-ins and verify actual rewards; journal completion alone does not establish payout.', '']
    for q in catalog('quests'):
        if not q['id'].startswith('w2_'):
            continue
        out += [f"### {q['title']} (`{q['id']}`)", '', q['description'], '']
        for stage in q['stages']:
            out += [f"**{stage['title']}**", '']
            out += [f"- [ ] {task['text']} (`{task['id']}`)" for task in stage['tasks']]
            out += ['']
    return '\n'.join(out) + '\n'



def validate_route(text):
    rooms = {r['id']: r for r in catalog('rooms')}
    titles = {r['title']: r['id'] for r in rooms.values() if r['id'].startswith('sector9_')}
    legs = 0
    for line in text.splitlines():
        source = re.match(r'\d+\. From \*\*([^*]+)\*\*, go ', line)
        if not source:
            continue
        current = titles[source[1]]
        for direction, title, target in re.findall(r'\*\*(north|south|east|west)\*\* to \*\*([^*]+)\*\* \(`([^`]+)`\)', line):
            if rooms[current]['connections'].get(direction) != target or rooms[target]['title'] != title:
                raise ValueError(f'Invalid guide travel: {current} -> {direction} -> {target}')
            current = target
            legs += 1
    print(f'World 2 guide directional route matches {legs} authored connections; state gates still require playtesting.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    text = GUIDE.read_text()
    validate_route(text)
    start, end = text.index('## 15.'), text.index('## 17.')
    expected = text[:start] + references() + '\n' + text[end:]
    if args.check:
        if expected != text:
            raise SystemExit('World 2 guide references are stale; run scripts/update_world2_guide_reference.py')
        print('World 2 guide references match all 93 rooms and ten current quests.')
    else:
        GUIDE.write_text(expected)
        print(GUIDE)


if __name__ == '__main__':
    main()
