"""Build the debug-suite inventory from current assets; never manufacture test passes.

Generated files contain assignments, not execution evidence. Run records live separately.
Usage: python scripts/debug_coverage.py [--check]
"""
import argparse
import collections
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
OUT = ROOT / "docs/testing"


def read(name):
    return json.loads((ASSETS / f"{name}.json").read_text(encoding="utf-8-sig"))


def build():
    rows = []
    hubs = {h["id"]: h for h in read("hubs")}
    nodes = read("hub_nodes")
    room_hubs = collections.defaultdict(set)
    for node in nodes:
        for room in set(node.get("rooms", []) + [node["entry_room"]]):
            room_hubs[room].add(node["hub_id"])

    def add(kind, entry, parent=None, assignment=None):
        asset_id = entry["id"]
        hub_ids = [entry["hub_id"]] if entry.get("hub_id") else sorted(room_hubs.get(asset_id, []))
        world = entry.get("world_id") or next((hubs[h]["world_id"] for h in hub_ids if h in hubs), None)
        if not world and re.match(r"w[1-6]_", asset_id):
            world = f"world_{asset_id[1]}"
        scenario = assignment or (f"route_{world}" if world else f"system_{kind}")
        rows.append(dict(
            coverage_id=f"{kind}:{asset_id}", asset_id=asset_id, kind=kind,
            title=entry.get("title") or entry.get("name") or asset_id,
            world=world, hubs=hub_ids, parent=parent, planned_scenario=scenario,
            assignment_status="planned_route" if scenario.startswith("route_") else "planned_system",
            content_review="pending", implementation="planned", execution="not_run",
            source_digest=hashlib.sha256(json.dumps(entry, sort_keys=True).encode()).hexdigest()[:16],
        ))

    for kind in ("worlds", "hubs", "hub_nodes", "rooms", "quests", "enemies", "skills", "statuses",
                 "items", "recipes_cooking", "recipes_tinkering", "tuning_puzzles", "tutorial_scripts",
                 "cinematics", "events", "characters", "milestones", "dialogue", "npcs"):
        for entry in read(kind):
            scenario = f"campaign_{entry['id']}" if kind == "quests" else None
            add(kind, entry, assignment=scenario)
            if kind == "quests":
                for stage in entry.get("stages", []):
                    add("quest_stage", dict(stage, id=f"{entry['id']}/{stage['id']}", hub_id=entry["hub_id"]),
                        parent=f"quests:{entry['id']}", assignment=scenario)
                    for task in stage.get("tasks", []):
                        add("quest_task", dict(task, id=f"{entry['id']}/{stage['id']}/{task['id']}",
                                               title=task.get("text"), hub_id=entry["hub_id"]),
                            parent=f"quest_stage:{entry['id']}/{stage['id']}", assignment=scenario)
            if kind == "rooms":
                for direction, target in {**entry.get("connections", {}), **entry.get("special_exits", {})}.items():
                    add("exit", dict(id=f"{entry['id']}/{direction}", title=f"{entry['id']} -> {target}",
                                     target=target), parent=f"rooms:{entry['id']}",
                        assignment=f"route_{hubs[sorted(room_hubs[entry['id']])[0]]['world_id']}"
                        if room_hubs[entry['id']] else "system_navigation")
                for index, action in enumerate(entry.get("actions", [])):
                    add("room_action", dict(action, id=f"{entry['id']}/{index}", title=action.get("name")),
                        parent=f"rooms:{entry['id']}", assignment="system_room_actions")
    for key, entry in read("shops").items():
        add("shops", dict(entry, id=key), assignment=f"shop_{key}")
    fish = read("recipes_fishing")
    for kind in ("rods", "lures"):
        for entry in fish[kind]:
            add(f"fishing_{kind}", entry, assignment="system_fishing")
    for key, entries in fish["zones"].items():
        add("fishing_zone", dict(id=key, catches=entries), assignment=f"fish_{key}")
        for index, catch in enumerate(entries):
            add("fishing_catch", dict(catch, id=f"{key}/{index}"),
                parent=f"fishing_zone:{key}", assignment=f"fish_{key}")
    for entry in read("enemy_movement").get("zones", []):
        add("patrol_zone", entry, assignment="system_patrols")
    for cabinet in ("deep_mine_asteroid_drill", "canopy_hopper", "spire_infiltrator", "slag_catcher",
                    "orbital_defense", "harmonic_pulse"):
        add("arcade", dict(id=cabinet), assignment=f"arcade_{cabinet}")

    system_cases = {
        "combat_actions": "Targeting, readiness, invalid actions, cooldowns and resource spending",
        "combat_party": "Switching, downed members, health carryover and supported recovery",
        "combat_weapons": "Weapon attack styles, momentum, overcharge and stability break",
        "combat_status": "Buff/debuff application, resistance, duration and expiry",
        "combat_outcomes": "Victory, defeat, retreat, re-entry and exactly-once rewards",
        "leveling": "XP, level and AP changes, skill spending and recruitment",
        "inventory": "Acquire, use, equip, unequip, restrictions and empty/quantity states",
        "shop_boundaries": "Exact and insufficient credits, buy/sell and repeated transactions",
        "recipe_boundaries": "Exact/missing ingredients, missing tools, cancellation and repeat crafting",
        "meals": "Chef effects, consumption/replacement, healing and expiry through actual combat",
        "fishing": "Rod/lure selection, success/failure/cancel and catch rewards",
        "arcade_rewards": "Discovery, repair, installation, high scores and tier claim persistence",
        "node_progression": "Hidden/revealed/locked/unlocked/visited/completed state transitions",
        "astra": "Crew dialogue, room gates, rest, provisioning and return location",
        "tutorial_lifecycle": "First trigger, completion, repeat suppression and interruption",
        "save_slots": "Manual, quick and autosave; empty slots; load and overwrite behavior",
        "recovery_battle": "Interrupt active battle and unpaid/paid victory boundaries",
        "recovery_cinematic": "Interrupt scene playback and progression callbacks",
        "recovery_transactions": "Interrupt quest rewards, crafting, purchases and consumable use",
        "recovery_puzzle": "Cancel, background and restart unsolved/solved puzzles",
        "recovery_travel": "Interrupt recruitment, world handoffs and Astra travel",
        "legacy_saves": "Supported legacy migration and stranded-save recovery",
        "completion_ngplus": "Ending unlock; NG+ carryover, reset and reopening quest",
        "ui_accessibility": "Large fonts, touch targets, contrast, descriptions, scrolling and Back",
        "audio_lifecycle": "Music, ambience, voice, focus loss, mute and background/resume",
        "device_performance": "Startup, crowded combat, prolonged play and repeated sessions",
        "campaign_continuous": "Normal new game through completion and NG+ without debug grants",
        "campaign_economy": "Mandatory purchases, supply costs, backtracking and optional rewards",
    }
    for key, title in system_cases.items():
        add("system_behavior", dict(id=key, title=title), assignment=f"system_{key}")

    ids = [r["coverage_id"] for r in rows]
    duplicates = {key: count for key, count in collections.Counter(ids).items() if count > 1}
    occurrences = collections.Counter()
    for row in rows:
        key = row["coverage_id"]
        if key in duplicates:
            occurrences[key] += 1
            row["coverage_id"] = f"{key}#occurrence{occurrences[key]}"
            row["content_review"] = "duplicate_asset_id_review_required"
    counts = dict(sorted(collections.Counter(r["kind"] for r in rows).items()))
    registry = (ROOT / "app/src/main/java/com/example/starborn/debug/DebugTestRegistry.kt").read_text()
    implemented = set(re.findall(r'test\("([^"$]+)"', registry))
    implemented.update(f"arcade_{key}" for key in re.findall(r'DebugArcadeCabinet\("([^"]+)"', registry))
    systems = (ROOT / "app/src/main/java/com/example/starborn/debug/DebugSystemScenarios.kt").read_text()
    implemented.update(re.findall(r'scenario\("([^"$]+)"', systems))
    implemented.update(f"fish_{key}" for key in re.findall(r'DebugFishingLocation\("([^"]+)"', systems))
    campaign_scenarios = (ROOT / "app/src/main/java/com/example/starborn/debug/DebugCampaignScenarios.kt").read_text()
    implemented.update(re.findall(r'entry\("([^"$]+)"', campaign_scenarios))
    shop_block = systems.split("val shopIds = listOf(", 1)[1].split(")", 1)[0]
    for shop in re.findall(r'"([^"]+)"', shop_block):
        implemented.update((f"shop_{shop}", f"shop_{shop}_empty"))
    for row in rows:
        if row["kind"] == "recipes_cooking":
            row["planned_scenario"] = "system_cooking"
        elif row["kind"] == "recipes_tinkering":
            row["planned_scenario"] = "system_tinkering"
        if row["planned_scenario"] in implemented:
            row["implementation"] = "built"
    inventory = dict(schema_version=1, meaning="Planned assignments only; content reachability still needs review.",
                     counts=counts, implemented_scenario_ids=sorted(implemented), duplicate_asset_ids=duplicates, rows=rows)

    catalog_source = (ROOT / "app/src/main/java/com/example/starborn/feature/mainmenu/DebugScenario.kt").read_text()
    bootstrap = (ROOT / "app/src/main/java/com/example/starborn/di/AppServices.kt").read_text()
    menu_ids = set(re.findall(r'(?:scenario|hub|hubScenario)\("([^"]+)"', catalog_source))
    dispatch = bootstrap.split("private fun launchLegacyDebugScenario", 1)[-1] if "private fun launchLegacyDebugScenario" in bootstrap else bootstrap.split("fun startDebugScenario", 1)[1]
    dispatch = dispatch.split("private fun startNewGameAtTinkeringTutorial", 1)[0]
    launchers = dict(re.findall(r'"([^"]+)" -> ([^\r\n]+)', dispatch))
    legacy = []
    for key in sorted(menu_ids | launchers.keys()):
        target = launchers.get(key, "authored hub launcher")
        aliases = sorted(k for k, v in launchers.items() if v == target and k != key)
        legacy.append(dict(id=key, visible=key in menu_ids, launcher=target, shared_setup_with=aliases,
                           disposition="merge_review" if aliases else "replace_review",
                           removal_gate="replacement gameplay verified and callers migrated"))

    lines = ["# Generated debug coverage inventory", "", "Regenerate: `python scripts/debug_coverage.py`.",
             "Check for drift: `python scripts/debug_coverage.py --check`.", "",
             "These are planned assignments, not verified routes or test passes. Runtime reachability and exact fixture prerequisites require review.",
             "Execution evidence belongs in `runs/`; regeneration never rewrites run records.", "",
             f"Rebuilt registry: {len(implemented)} implemented entries. Built does not mean gameplay passed.", "",
             "## Asset inventory", "", "| Kind | Rows |", "| --- | ---: |"]
    lines += [f"| {kind} | {count} |" for kind, count in counts.items()]
    lines += ["", "## Exact quest scenario backlog", "",
              "Each quest scenario must start before acceptance/its first action and finish after payout and onward access.",
              "Stage and task rows in the JSON ledger enumerate the required intermediate checks.", "",
              "| Planned ID | Quest | Hub |", "| --- | --- | --- |"]
    lines += [f"| campaign_{q['id']} | {q['title']} | {q['hub_id']} |" for q in read("quests")]
    lines += ["", "## Legacy review", "", f"{len(menu_ids)} existing menu entries; {len(legacy)} menu/explicit dispatch IDs in the migration inventory.",
              "See `debug-legacy-migration.json` for shared setup groups and hidden launchers.",
              "No legacy entry is deleted merely because it shares a destination.", ""]
    assignments = collections.defaultdict(list)
    for row in rows:
        assignments[row["planned_scenario"]].append(row)
    catalog = ["# Replacement coverage catalog", "",
               "Generated from the coverage ledger. These are exact planned scenario/family IDs, not claims that fixtures exist.",
               "Large route/system families must be split into playable variants during prerequisite review.", "",
               "| Planned ID | Coverage kinds | Rows |", "| --- | --- | ---: |"]
    catalog += [f"| {key} | {', '.join(sorted({r['kind'] for r in entries}))} | {len(entries)} |"
                for key, entries in sorted(assignments.items())]
    catalog += ["", "## System behavior requirements", "", "| Planned ID | Acceptance scope |", "| --- | --- |"]
    catalog += [f"| system_{key} | {title} |" for key, title in system_cases.items()]
    catalog += ["", "## NG+ contract observed in AppServices", "",
                "The current implementation retains party, levels/XP, inventory, equipped items and weapon/armor unlocks; credits have a 5,000 minimum.",
                "It restarts in Nova's bunk with Wake Up Call, resets tutorial and quest progress, and seeds Master Protocol plus the World 2 completion milestone.",
                "AP, skill unlocks, HP, discovery, arcade progress and meal state are not explicitly copied into the new seed; verify the resulting runtime behavior and intended design before acceptance.",
                "This is an implementation inventory, not a declaration that all current carryover choices are correct.", ""]
    return {
        "debug-coverage.json": json.dumps(inventory, indent=2, ensure_ascii=False) + "\n",
        "debug-legacy-migration.json": json.dumps(legacy, indent=2) + "\n",
        "debug-coverage.md": "\n".join(lines),
        "debug-replacement-catalog.md": "\n".join(catalog),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    stale = []
    for name, content in build().items():
        path = OUT / name
        if args.check:
            if not path.exists() or path.read_text(encoding="utf-8") != content:
                stale.append(name)
        else:
            OUT.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
    if stale:
        raise SystemExit("Coverage inventory is stale: " + ", ".join(stale))
    print("Coverage inventory checked." if args.check else "Coverage inventory generated in docs/testing.")


if __name__ == "__main__":
    main()
