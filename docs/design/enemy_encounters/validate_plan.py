#!/usr/bin/env python3
"""Validate design coverage and contracts; never edits gameplay or generates assets."""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path


def validate(packet: Path, root: Path, check_sources: bool = True) -> dict:
    baseline = json.loads((packet / "BASELINE.json").read_text())
    plan = json.loads((packet / "PLAN.json").read_text())
    sources = baseline["encounters"]
    decisions = plan["encounter_decisions"]
    roster = plan["roster"]

    def unique(rows, key, label):
        counts = Counter(row[key] for row in rows)
        assert all(n == 1 for n in counts.values()), f"Duplicate {label}: {counts}"
        return {row[key]: row for row in rows}

    src = unique(sources, "id", "source")
    dec = unique(decisions, "source_id", "decision")
    kinds = unique(roster, "id", "roster ID")
    new = unique(plan["new_encounters"], "id", "new encounter")
    assert set(src) == set(dec), "Every source needs exactly one decision"
    assert not (set(new) & set(src)), "New encounter ID collides with existing source"
    assert {e["id"] for e in baseline["enemies"]} <= set(kinds), "Existing enemy missing from roster"
    em = {e["id"]: e for e in baseline["enemies"]}
    active = {key: row for key, row in dec.items() if row["party"]} | new
    seen = set()

    for row in roster:
        assert row["world"] in {f"world_{i}" for i in range(1, 7)}
        for field in ("role", "signature", "counterplay", "visual_brief", "reward_policy"):
            assert row[field].strip(), f"Missing {field}: {row['id']}"

    for key, row in dec.items():
        old = src[key]
        assert row["world"] == old["world"] and row["room_id"] == old["room_id"], key
        assert row["purpose"].strip(), f"No purpose: {key}"
        if row["merged_into"]:
            target = active.get(row["merged_into"])
            assert target and target["world"] == row["world"], f"Bad merge/relocation: {key}"
            assert not row["party"], f"Merged source creates a duplicate active fight: {key}"
        elif not row["party"]:
            assert row["disposition"] == "retire", f"Unexplained empty fight: {key}"
        if "baseline_gate_contract" in row:
            assert row["baseline_gate_contract"] == old.get("blocked_directions", {}), f"Gate contract changed: {key}"
        for gate in old.get("blocked_directions", {}).values():
            if gate.get("type") == "enemy":
                room_fights = [p for p in active.values() if p["room_id"] == old["room_id"]]
                assert room_fights, f"Removed every fight behind a combat gate: {key}"
                if gate.get("enemy_id"):
                    assert any(gate["enemy_id"] in p["party"] for p in room_fights), f"Orphaned enemy gate: {key}"
        extras = [drop for inst in old.get("enemy_instances", [])
                  for drop in inst.get("extra_drops", []) if drop.get("chance", 1) == 1]
        assert len(row.get("guaranteed_materials_to_keep", [])) == len(extras), f"Instance guarantees omitted: {key}"
        if extras:
            assert row["party"] or row["merged_into"], f"Retired a guaranteed material source: {key}"
        base_guarantees = [drop for enemy in old["enemies"] for drop in em[enemy]["drops"]
                           if drop.get("chance", 1) == 1]
        assert len(row["baseline_base_guarantees"]) == len(base_guarantees), f"Base guarantees omitted: {key}"
        if base_guarantees:
            assert row["party"] or row["merged_into"], f"Retired guaranteed loot without destination: {key}"

    for key, row in active.items():
        for enemy in row["party"]:
            assert enemy in kinds, f"Unknown proposed enemy {enemy}: {key}"
            assert kinds[enemy]["world"] == row["world"], f"Unapproved cross-world ID reuse {enemy}: {key}"
            seen.add(enemy)
        if key in new:
            assert row.get("absorbs"), f"New stage has no accounted source: {key}"
            assert all(s in dec and dec[s]["merged_into"] == key for s in row["absorbs"]), key

    for boss in plan["boss_designs"]:
        assert boss["victory_core"] in boss["actors"], boss["id"]
        for enemy in boss["actors"] + boss["reserves"]:
            assert enemy in kinds and kinds[enemy]["world"] == boss["world"], boss["id"]
            seen.add(enemy)
        if boss["reserves"]:
            assert boss["reserve_count"] > 0 and boss["max_live_escorts"] > 0
        if boss.get("stages"):
            assert set(sum(boss["stages"], [])) == set(boss["actors"]), boss["id"]
    assert seen == set(kinds), f"Unused roster entries: {set(kinds) - seen}"

    if (packet / "ART_MANIFEST.json").exists():
        manifest = json.loads((packet / "ART_MANIFEST.json").read_text())
        art = unique(manifest["subjects"], "enemy_id", "art subject")
        assert set(art) == set(kinds), "Art manifest does not cover the roster"
        assert len({r["proposed_base_path"] for r in art.values()}) == len(art), "Base sprite destinations collide"

    if check_sources:
        reconciliation_path = packet / "SOURCE_RECONCILIATION.json"
        reconciliation = json.loads(reconciliation_path.read_text()) if reconciliation_path.exists() else {}
        reconciled = reconciliation.get("sources", {})
        for name, expected in baseline["source_sha256"].items():
            actual = hashlib.sha256((root / "app/src/main/assets" / f"{name}.json").read_bytes()).hexdigest()
            accepted = reconciled.get(name, {})
            if actual != expected:
                assert accepted.get("baseline_sha256") == expected and accepted.get("implemented_sha256") == actual and accepted.get("reason"), f"Source changed since snapshot: {name}; reconcile the design deliberately"

        for name, accepted in reconciliation.get("supplemental_sources", {}).items():
            actual = hashlib.sha256((root / "app/src/main/assets" / f"{name}.json").read_bytes()).hexdigest()
            assert actual == accepted["implemented_sha256"] and accepted.get("reason"), f"Supplemental source changed: {name}"

    worlds = {}
    for i in range(1, 7):
        w = f"world_{i}"
        worlds[w] = {"baseline": sum(e["world"] == w for e in sources),
                     "proposed": sum(e["world"] == w for e in active.values()),
                     "roster": sum(e["world"] == w for e in roster)}
    return {"baseline_sources": len(src), "covered_sources": len(dec),
            "proposed_opportunities_or_stages": len(active), "roster": len(kinds), "worlds": worlds}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--packet-dir", type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument("--skip-source-freshness", action="store_true",
                        help="Review an old packet without asserting that production assets still match")
    args = parser.parse_args()
    print(json.dumps(validate(args.packet_dir, Path(__file__).resolve().parents[3],
                              not args.skip_source_freshness), indent=2))
