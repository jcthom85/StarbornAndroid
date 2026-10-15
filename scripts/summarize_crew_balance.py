"""Validate and retain compact evidence from CrewBalanceExperiment; never simulate or invent results."""
import argparse
from collections import defaultdict
import hashlib
import json
from pathlib import Path
from statistics import mean


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def summarize(directory):
    rows = json.loads((directory / "results.json").read_text(encoding="utf-8"))
    chains = json.loads((directory / "attrition.json").read_text(encoding="utf-8"))
    assert rows and chains
    assert all(row["outcome"] in {"victory", "defeat"} and row["rejectedCommands"] == 0 for row in rows + chains)
    groups = defaultdict(list)
    for row in rows:
        groups[tuple(row[key] for key in ("checkpoint", "start", "room", "policy", "build"))].append(row)
        budget = {"early": 28 if row["start"] == "prepared" else 26, "middle": 36, "late": 44}[row["checkpoint"]]
        assert sum(row["purchases"].values()) + row["unusedAp"] == budget
        assert row["unusedAp"] == (budget if row["build"] == "unspent" else 0)
    seeds = sorted({row["seed"] for row in rows})
    summaries = []
    for key, group in sorted(groups.items()):
        assert sorted(row["seed"] for row in group) == seeds, key
        wins = [row for row in group if row["outcome"] == "victory"]
        control = {row["seed"]: row for row in groups[(*key[:-1], "unspent")]}
        summaries.append(dict(zip(("checkpoint", "start", "room", "policy", "build"), key),
            runs=len(group), wins=len(wins),
            win_only_mean_player_actions=round(mean(row["playerActions"] for row in wins), 3) if wins else None,
            mean_knockouts=round(mean(row["knockouts"] for row in group), 3),
            mean_items=round(mean(row["itemsUsed"] for row in group), 3),
            wins_when_control_lost=sum(row["outcome"] == "victory" and control[row["seed"]]["outcome"] == "defeat" for row in group),
            losses_when_control_won=sum(row["outcome"] == "defeat" and control[row["seed"]]["outcome"] == "victory" for row in group)))
    chain_summary = []
    for build in sorted({row["build"] for row in chains}):
        for policy in sorted({row["policy"] for row in chains}):
            group = [row for row in chains if row["build"] == build and row["policy"] == policy]
            starts = [row for row in group if row["room"] == "orbital_executive_dock"]
            assert sorted(row["seed"] for row in starts) == seeds
            chain_summary.append(dict(build=build, policy=policy, sequences=len(starts),
                completed=sum(row["room"] == "deep_tear" and row["outcome"] == "victory" for row in group),
                total_fights=len(group), total_items=sum(row["itemsUsed"] for row in group)))
    sources = [Path("app/src/main/assets") / name for name in
        ["enemies.json", "skills.json", "items.json", "characters.json", "rooms.json", "progression.json", "leveling_data.json",
         "playtest/crew_early.json", "playtest/crew_middle.json", "playtest/crew_late.json"]]
    sources += [Path("app/src/test/java/com/example/starborn/domain/playtest") / name for name in
        ["CrewBalanceExperiment.kt", "LegalRouteCampaignRunnerTest.kt"]]
    sources += [Path("gameRuntime/src/main/kotlin/com/example/starborn") / name for name in
        ["domain/combat/TalentEffects.kt", "domain/combat/CombatActionProcessor.kt", "domain/combat/CombatEngine.kt",
         "domain/combat/CrewEncounterTempo.kt", "feature/combat/viewmodel/CombatController.kt", "domain/leveling/CrewBuildPrototype.kt"]]
    return dict(policy_revision=2, encounter_tuning="crew-tempo-1", seeds=seeds, independent_fights=len(rows), attrition_fights=len(chains),
        attrition_sequences=len(chain_summary) * len(seeds), deterministic_replays=1,
        rejected_commands=0, unresolved_fights=0,
        limitations="Headless seeded controller experiment; scene placement is explicit. Fixed gear, three allocations, two heuristics. No optimal play, full campaign traversal, statistical superiority, human fun or release acceptance claim.",
        raw_sha256={name: digest(directory / name) for name in ("results.json", "attrition.json", "prepared-early.json", "prepared-middle.json", "prepared-late.json")},
        source_sha256={str(path).replace('\\', '/'): digest(path) for path in sources},
        groups=summaries, attrition=chain_summary)


def summarize_experiments(directory):
    names = ["baseline", "measured", "moderate", "strong", "cover_skip", "cover_proactive",
             "cover_duration3", "treatment35", "moderate_cover3", "moderate_treatment35",
             "baseline_holdout", "selected_holdout"]
    variants = {}
    for name in names:
        folder = directory / name
        rows = json.loads((folder / "results.json").read_text(encoding="utf-8"))
        assert len(rows) == 256 and all(row["rejectedCommands"] == 0 and row["outcome"] in {"victory", "defeat"} for row in rows)
        groups = defaultdict(list)
        for row in rows:
            groups[(row["room"], row["build"], row["policy"])].append(row)
        seeds = sorted({row["seed"] for row in rows})
        assert len(seeds) == 16
        summary = []
        for (room, build, policy), group in sorted(groups.items()):
            assert sorted(row["seed"] for row in group) == seeds
            summary.append(dict(room=room, build=build, policy=policy, runs=len(group),
                wins=sum(row["outcome"] == "victory" for row in group),
                mean_knockouts=round(mean(row["knockouts"] for row in group), 3),
                mean_items=round(mean(row["itemsUsed"] for row in group), 3),
                cover_casts=sum(row["skillsUsed"].get("zeke_cover", 0) for row in group),
                treatment_casts=sum(row["skillsUsed"].get("orion_field_treatment", 0) for row in group)))
        source_change = folder / "source-change.json"
        variants[name] = dict(seeds=seeds, parameters=(folder / "parameters.txt").read_text().strip(), groups=summary,
            source_change=json.loads(source_change.read_text()) if source_change.exists() else None,
            raw_sha256=digest(folder / "results.json"))
    assert set(variants["baseline"]["seeds"]).isdisjoint(variants["baseline_holdout"]["seeds"])
    assert variants["baseline_holdout"]["seeds"] == variants["selected_holdout"]["seeds"]
    return dict(selected="moderate", scope="Prototype version 1; Executive Dock fighters and Memory Fragments trio only.",
        retained_support_rules="Cover Shield 2 turns; Treatment 20%, Triage 25% below 35% HP.",
        screening_variants=10, holdout_variants=2, fights=3072, deterministic_replays=12, variants=variants)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=Path("app/build/reports/crew-balance"))
    parser.add_argument("--output", type=Path, default=Path("app/build/reports/crew-balance/aggregate.json"))
    parser.add_argument("--experiments", action="store_true", help="Summarize the twelve targeted tuning/holdout runs instead of a full matrix.")
    args = parser.parse_args()
    result = summarize_experiments(args.input) if args.experiments else summarize(args.input)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    if args.experiments:
        print(f"Verified {result['fights']} targeted tuning fights and disjoint holdout seeds; wrote {args.output}")
    else:
        print(f"Verified {result['independent_fights']} independent fights, {result['attrition_fights']} attrition fights, {len(result['seeds'])} seeds; wrote {args.output}")
