#!/usr/bin/env python3
"""Import earned checkpoints after the completionist test, or verify packaged freshness."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'app/src/debug/assets/playtest/world1'
SOURCE = ROOT / 'app/build/reports/world1-checkpoints'
IDS = [f'w1_mq0{i}' for i in range(2, 6)] + [f'w1_sq0{i}' for i in range(1, 6)] + ['w1_minecart_puzzle']


def digests():
    return {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((ROOT / 'app/src/main/assets').glob('*.json'))}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--import-earned', action='store_true')
    args = parser.parse_args()
    if args.import_earned:
        receipt = json.loads((SOURCE / 'passed.json').read_text())
        report = ROOT / 'app/build/test-results/testDebugUnitTest/TEST-com.example.starborn.domain.playtest.LegalRouteCampaignRunnerTest.xml'
        case = next((case for case in ET.parse(report).getroot().findall('testcase') if case.get('name', '').rstrip('()') == receipt['test']), None)
        if (case is None or any(case.find(tag) is not None for tag in ('failure', 'error', 'skipped'))
                or receipt.get('roomId') != 'sector9_crash_site' or receipt.get('assistedActions') != 0):
            raise SystemExit('A successful, unassisted completionist route and JUnit case are required before import.')
        evidence = json.loads((SOURCE / 'sources.json').read_text())
        if evidence != digests():
            raise SystemExit('Sources changed after capture. Rerun the production completionist test.')
        DEST.mkdir(parents=True, exist_ok=True)
        records = {}
        captures = []
        for name in IDS:
            filename = f'world1_{name}.json'
            source = SOURCE / filename
            state = json.loads(source.read_text())
            if state['worldId'] != 'world_1' or state['partyMembers'] != ['nova'] or state.get('pendingBattleJson') or state.get('battleCheckpoint'):
                raise SystemExit(f'Invalid checkpoint: {name}')
            captures.append((source, DEST / filename))
            records[name] = {'file': filename, 'roomId': state['roomId'], 'sha256': hashlib.sha256(source.read_bytes()).hexdigest()}
        for source, destination in captures:
            shutil.copyfile(source, destination)
        manifest = {'provenance': 'LegalRouteCampaignRunnerTest.productionWorld1CompletionistRouteFinishesAllTenQuestsAndTheCartDetour',
                    'controllerAcceptance': receipt,
                    'limitations': 'Earned completionist route, fixed seed 17 and fixed combat policy; authored puzzle answers and accelerated presentation. Not UI or balance acceptance.',
                    'sources': evidence, 'checkpoints': records}
        (DEST / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    manifest = json.loads((DEST / 'manifest.json').read_text())
    if manifest['sources'] != digests():
        raise SystemExit('World 1 checkpoint sources changed. Regenerate using the production completionist test and --import-earned.')
    if set(manifest['checkpoints']) != set(IDS):
        raise SystemExit('World 1 checkpoint catalog is incomplete.')
    for name, entry in manifest['checkpoints'].items():
        data = (DEST / entry['file']).read_bytes()
        if hashlib.sha256(data).hexdigest() != entry['sha256']:
            raise SystemExit(f'Checkpoint hash mismatch: {name}')
        state = json.loads(data)
        if state['roomId'] != entry['roomId']:
            raise SystemExit(f'Checkpoint room mismatch: {name}')
    print(f'World 1 checkpoints: {len(IDS)} earned states match current content.')


if __name__ == '__main__':
    main()
