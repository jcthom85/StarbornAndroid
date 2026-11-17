#!/usr/bin/env python3
"""Check the NPC cast's distinct identities, consumers, transparency and scene references.

Requires Pillow. Run from any directory; reads the checked-in production manifest.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'app/src/main/assets'
ART = ROOT / 'world_assets/src/main/assets'
AUDIT = ROOT / 'docs/playtest/npc_portrait_audit'


def read(path: Path):
    return json.loads(path.read_text(encoding='utf-8'))


def main() -> None:
    manifest = read(AUDIT / 'production_manifest.json')
    roster = read(AUDIT / 'inventory.json')['liveProductionIdentities']
    npcs = {npc['id']: npc for npc in read(DATA / 'npcs.json')}
    shops = read(DATA / 'shops.json')
    shop_owners = {
        'scrappers_contraband': 'scrapper',
        'upper_city_lounge': 'upper_city_concierge',
        'arcade_prize_shop': 'arcade_prize_dispenser',
        'noodle_row_stall': 'noodle_cook_ren',
        'foundry_welder_shop': 'forge_hand_rylan',
        'orbital_dispensary': 'zenith_dispensary',
        'davids_provisions': 'chef_david',
        'general_store': 'shopkeep',
        'sentinel_scraps': 'sentinel_3',
    }
    new = {entry['id']: entry for entry in manifest['portraits']}
    assert len(new) == len(manifest['portraits']), 'Duplicate production identities'
    assert set(new) == {r['id'] for r in roster if r['recommendation'] != 'Retain'}
    paths = {}
    hashes = {}
    for row in roster:
        ident = row['id']
        path = new[ident]['portrait'] if ident in new else row['currentPortrait']
        assert path not in paths, f"Unrelated identities share a portrait: {ident}, {paths.get(path)}"
        paths[path] = ident
        file = ART / path
        assert file.is_file(), f'Missing portrait: {file}'
        digest = hashlib.sha256(file.read_bytes()).hexdigest()
        assert digest not in hashes, f'Duplicate portrait bytes: {ident}, {hashes.get(digest)}'
        hashes[digest] = ident
        if ident in npcs:
            assert npcs[ident]['portrait'] == path, f'NPC points at stale art: {ident}'
        if ident not in new:
            continue
        with Image.open(file) as image:
            assert image.size == (1024, 1024), (ident, image.size)
            assert image.mode == 'RGBA', (ident, image.mode)
            alpha = image.getchannel('A')
            box = alpha.getbbox()
            assert box and min(box[0], box[1], 1024-box[2], 1024-box[3]) >= 48, (ident, box)
            assert all(alpha.getpixel(p) == 0 for p in [(0, 0), (1023, 0), (0, 1023), (1023, 1023)])
            assert sum(a > 128 for a in alpha.getdata()) > 1024*1024*.1, f'Empty portrait: {ident}'
            # Near-pure key green must not survive, including enclosed claw/prop holes.
            green = sum(a > 32 and g > 210 and r < 50 and b < 50 for r, g, b, a in image.getdata())
            assert green == 0, f'{ident}: {green} chroma-key pixels remain'
    for shop, owner in shop_owners.items():
        expected = new[owner]['portrait'] if owner in new else npcs[owner]['portrait']
        assert shops[shop]['portrait'] == expected, f'Shop and identity disagree: {shop}'
    for speaker, ident in [('Rylos', 'rylos'), ('Concierge', 'upper_city_concierge')]:
        npc = npcs[ident]
        assert speaker in [npc['name'], *npc['aliases']], f'Unregistered speaker: {speaker}'
        assert npc['portrait'] == new[ident]['portrait']
    scenes = read(DATA / 'cinematics.json')
    for scene in scenes:
        for step in scene.get('steps', []):
            for field in ['portrait', 'image']:
                path = step.get(field)
                if path:
                    assert (ART / path).is_file() or (DATA / path).is_file(), f'{scene["id"]}: {path}'
    for scene_id, expected in {
        'scene_w4_outer_shield': 'images/cinematics/w4_coup_display_v2.webp',
        'scene_w5_anchor_stasis': 'images/cinematics/w5_stasis_elara_v2.webp',
        'scene_w5_cross_the_tear': 'images/cinematics/w5_tear_hands_v2.webp',
    }.items():
        scene = next(s for s in scenes if s['id'] == scene_id)
        assert any(step.get('image') == expected for step in scene['steps'])
        with Image.open(ART / expected) as image:
            assert image.size == (1088, 1920)
        for step in scene['steps']:
            speaker = step.get('speaker', '').lower()
            for ident in ['vale', 'thorne', 'elara']:
                if ident in speaker and step.get('portrait'):
                    assert step['portrait'] == new[ident]['portrait']
    print(f'PASS: {len(roster)} distinct cast identities, {len(new)} new exports, '
          f'{len(shop_owners)} matching shops, scripted speakers and cinematic references.')


if __name__ == '__main__':
    main()
