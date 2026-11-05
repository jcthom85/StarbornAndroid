import json
from pathlib import Path
from collections import defaultdict, Counter, deque

BASE = Path('app/src/main/assets')
def read(name): return json.loads((BASE / (name+'.json')).read_text(encoding='utf-8-sig'))
rooms_list=read('rooms'); rooms={r['id']:r for r in rooms_list}; nodes=read('hub_nodes'); transitions=read('node_transitions')
owner={r:n for n in nodes for r in n['rooms']}
opp={'north':'south','south':'north','east':'west','west':'east','up':'down','down':'up','in':'out','out':'in'}
opp.update(northeast='southwest',southwest='northeast',northwest='southeast',southeast='northwest')
def world(r): return owner.get(r,{}).get('world_id','unassigned')
def path(a,b):
    q=deque([(a,[a])]); seen={a}
    while q:
        r,p=q.popleft()
        if r==b:return p
        for t in rooms.get(r,{}).get('connections',{}).values():
            if t not in seen: seen.add(t); q.append((t,p+[t]))
    return None
bad=[]; inbound=defaultdict(list); spatial=[]
for a,r in rooms.items():
    for d,b in r['connections'].items():
        inbound[b,d].append(a)
        back=rooms.get(b,{}).get('connections',{})
        if b not in rooms or back.get(opp.get(d))!=a:
            bad.append(dict(world=world(a),source=a,direction=d,target=b,expected=opp.get(d),opposite_target=back.get(opp.get(d)),actual_return=[k for k,v in back.items() if v==a],return_path=path(b,a),source_gate=r.get('blocked_directions',{}).get(d),opposite_gate=rooms.get(b,{}).get('blocked_directions',{}).get(opp.get(d)),dark=rooms.get(b,{}).get('dark',False)))
        if b in rooms and owner.get(a,{}).get('id')==owner.get(b,{}).get('id') and d in ('north','south','east','west'):
            x,y=r['pos'][:2]; xx,yy=rooms[b]['pos'][:2]; dx,dy=xx-x,yy-y
            good={'north':dx==0 and dy>0,'south':dx==0 and dy<0,'east':dy==0 and dx>0,'west':dy==0 and dx<0}[d]
            if not good: spatial.append([world(a),a,d,b,[dx,dy]])
funnels=[dict(world=world(b),target=b,direction=d,sources=aa,opposite_target=rooms.get(b,{}).get('connections',{}).get(opp.get(d))) for (b,d),aa in inbound.items() if len(set(aa))>1 and d in ('north','south','east','west')]
trans_bad=[]; disagreements=[]
for t in transitions:
    a,d,b=t['from_room'],t['direction'],t['to_room']
    if rooms.get(a,{}).get('connections',{}).get(d)!=b:disagreements.append(t)
    if rooms.get(b,{}).get('connections',{}).get(opp.get(d))!=a:trans_bad.append(t['id'])
affected={r for e in bad for r in (e['source'],e['target'])}
refs={}
for name in ('quests','events'):
    refs[name]=[]
    for item in read(name):
        raw=json.dumps(item)
        matches=sorted(r for r in affected if '"'+r+'"' in raw)
        if matches:refs[name].append(dict(id=item['id'],rooms=matches,data=item))
result=dict(counts=dict(rooms=len(rooms),edges=sum(len(r['connections']) for r in rooms.values()),transitions=len(transitions),bad_by_world=dict(Counter(e['world'] for e in bad))),bad=bad,funnels=funnels,transition_nonreciprocal=trans_bad,transition_disagreements=disagreements,spatial=spatial,references=refs,duplicate_rooms=[r for r,n in Counter(r['id'] for r in rooms_list).items() if n>1])
result['world_counts']={w:dict(rooms=sum(world(r)==w for r in rooms),edges=sum(len(r['connections']) for r in rooms.values() if world(r['id'])==w),bad=sum(e['world']==w for e in bad),funnels=sum(e['world']==w for e in funnels)) for w in sorted(set(world(r) for r in rooms))}
result['ownership_errors']=[r for r in rooms if sum(r in n['rooms'] for n in nodes)!=1]
result['transition_metadata_errors']=[t['id'] for t in transitions if owner.get(t['from_room'],{}).get('id')!=t['from_node'] or owner.get(t['to_room'],{}).get('id')!=t['to_node']]
result['duplicate_transition_keys']=[str(k) for k,n in Counter((t['from_room'],t['direction']) for t in transitions).items() if n>1]
result['duplicate_transition_ids']=[k for k,n in Counter(t['id'] for t in transitions).items() if n>1]
tk={(t['from_room'],t['direction'],t['to_room']) for t in transitions}
result['missing_transition_records']=[(a,d,b) for a,r in rooms.items() for d,b in r['connections'].items() if owner.get(a,{}).get('id')!=owner.get(b,{}).get('id') and (a,d,b) not in tk]
result['coordinate_collisions']=[dict(node=n['id'],pos=list(p),rooms=rr) for n in nodes for p,rr in [(p,[r for r in n['rooms'] if r in rooms and tuple(rooms[r]['pos'])==p]) for p in set(tuple(rooms[r]['pos']) for r in n['rooms'] if r in rooms)] if len(rr)>1]
focus=affected|{r for n in nodes if n['id'] in ('hall_of_echoes','spire_the_static','spire_sewers','spire_laundry','spire_skypark') for r in n['rooms']}
action_ids={x.get('action_event') for r in focus for x in rooms[r].get('actions',[]) if x.get('action_event')}
events=[e for e in read('events') if e['id'] in action_ids or any('"'+r+'"' in json.dumps(e) for r in focus)]
quest_ids={c.get('quest_id') for e in events for c in e.get('conditions',[])+e.get('actions',[]) if c.get('quest_id')}
result['linked_events']=events
result['linked_quests']=[q for q in read('quests') if q['id'] in quest_ids]
named_shortcuts=set()
# Retained launch artwork is not reachable by any room connection (also asserted by NavigationIntegrityTest).
def retained_launch_art(e):
    return (e['source']=='launch_rail' and rooms['launch_rail']['connections']=={'south':'launch_pod'}
        and not any('launch_rail' in r['connections'].values() for r in rooms.values()))

result['unexpected_nonreciprocal']=[e for e in bad if not retained_launch_art(e) and (
    (e['source'],e['direction'],e['target']) not in named_shortcuts or
    'one way' not in rooms[e['source']].get('special_exits',{}).get(e['direction'],''))]
result['world_three_non_cardinal']=[(a,d,b) for a,r in rooms.items() if world(a)=='world_3' for d,b in r['connections'].items() if d not in ('north','south','east','west')]
result['world_four_non_cardinal']=[(a,d,b) for a,r in rooms.items() if world(a)=='world_4' for d,b in r['connections'].items() if d not in ('north','south','east','west')]
result['world_five_non_cardinal']=[(a,d,b) for a,r in rooms.items() if world(a)=='world_5' for d,b in r['connections'].items() if d not in ('north','south','east','west')]
# NSEW is the walking default. These reviewed alternatives are named, reciprocal taps.
reviewed_vertical_routes = {
    ('spire_underrail_platform','down'): 'spire_transit_plaza',
    ('spire_transit_plaza','up'): 'spire_underrail_platform',
    ('spire_service_lift','down'): 'spire_archive_vault',
    ('spire_archive_vault','up'): 'spire_service_lift',
    ('orbital_service_shaft','down'): 'orbital_server_farm',
    ('orbital_server_farm','up'): 'orbital_service_shaft',
    ('deep_firewall_gamma','down'): 'deep_anchor_chamber',
    ('deep_anchor_chamber','up'): 'deep_firewall_gamma',
}
policy_errors=[]
for source,room in rooms.items():
    if world(source) not in ('world_1','world_2','world_3','world_4','world_5','world_6'): continue
    for direction,target in room['connections'].items():
        if direction not in ('north','south','east','west') and reviewed_vertical_routes.get((source,direction)) != target:
            policy_errors.append([source,direction,target,'unreviewed or diagonal exit'])
for (source,direction),target in reviewed_vertical_routes.items():
    label=rooms[source].get('special_exits',{}).get(direction,'')
    if rooms[source]['connections'].get(direction)!=target or not label or 'one way' in label.lower():
        policy_errors.append([source,direction,target,'missing named two-way alternative'])
for source,direction in [('deep_firewall_gamma','down'),('deep_anchor_chamber','up')]:
    block=rooms[source].get('blocked_directions',{}).get(direction,{})
    if not any(x.get('room_id')=='deep_anchor_chamber' and x.get('state_key')=='firewall_access_open' and x.get('value') is True for x in block.get('requires',[])):
        policy_errors.append([source,direction,'Anchor lift must retain Construct clearance'])
result['reviewed_direction_policy_errors']=policy_errors
if __name__=='__main__':
    import sys
    if '--check' in sys.argv:
        errors={k:result[k] for k in ('reviewed_direction_policy_errors','unexpected_nonreciprocal','funnels','spatial',
            'transition_disagreements','transition_metadata_errors','missing_transition_records',
            'duplicate_transition_keys','duplicate_transition_ids','duplicate_rooms','coordinate_collisions') if result[k]}
        traps=[e for e in bad if not e['return_path'] and not retained_launch_art(e)]
        if traps: errors['traps']=traps
        print(json.dumps(errors,indent=2) if errors else 'PASS: compass geometry, returns, funnels, room positions, and transition metadata.')
        sys.exit(bool(errors))
    else:
        print(json.dumps(result if '--json' in sys.argv else {k:v for k,v in result.items() if k not in ('references','linked_events','linked_quests','spatial')},indent=2))
