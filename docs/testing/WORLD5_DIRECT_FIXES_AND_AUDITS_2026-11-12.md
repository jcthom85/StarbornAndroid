# World 5 playtest: direct fixes and audits

Date: 2026-11-12. Changes are local; no installation, save migration, commit, push or walkthrough publication was performed for this pass.

## Completed

- **Arcade acquisition:** all six discoveries announce the named repair schematic with a gamepad icon and distinct acquisition heading, explaining that repair through **Menu → Tinker** makes the game playable in Astra Recreation. Discovery grants the component and schematic once, rather than claiming that the game is already playable. Inspection after discovery remains gray and descriptive until repair. The first Hyperion cabinet now follows the same behavior as the other five.
- **Astra Cargo Bay:** removed the obsolete workbench action and its text link. The repair bench remains atmospheric prose. Menu tinkering remains available. Demo/debug instructions now point to the menu.
- **World 5 room details:** repaired 64 passive inspections across 32 service rooms. The original labels did not occur in their room descriptions, so existing action definitions produced no tappable words. Labels now match actual objects in the prose and their detail text describes those objects. These are optional, readable details; reading them changes their color without awarding quest credit, resources or XP.
- **World 6 room details:** audited all 65 rooms and their description variants. Added small, setting-specific inspections to Silent Shore and Echo of the Mines; also preserved the Silent Shore links in its later description. Tide-Well remains combat-focused and Echo Elevator remains an automatic story transfer. These are the only two World 5/6 rooms with no action definitions.
- **Enemy art:** audited primary art paths for all 48 enemy definitions. Null-G Drone was using Sentinel Orb art and lacked its own sprite. It now has a dedicated transparent combat/exploration sprite. Riot Guard and Aero Drone were also using other enemies' art despite their own existing files; both bindings are corrected. All primary art paths resolve.
- **KO portraits:** added distinct, unconscious-but-alive portraits for Nova, Zeke, Orion and Gh0st in the established illustration style. Both battle UIs use the existing down-state asset lookup, now backed by actual assets, with explicit KO labels and accessibility descriptions.
- **Quest notifications:** reviewed the existing shared queue, dialogue/travel/reward deferral, timer pausing, objective checkmarks and completion priority. Added session-level deduplication for detail cards so a repeated start/completion/failure event cannot replay an already dismissed card and stinger. A genuine restart or session reset can announce again. Existing progress and summary coordination is retained.

Room IDs, connections, encounter rosters, combat gates and quest rewards were not changed.

## Walkthrough alignment

Updated local World 5 instructions to use the repaired inspection labels and added the new optional World 6 details to existing steps. World 5 still has 235 numbered route steps and 81 room references; World 6 still has 187 and 65. The local reader build preserves every World 5/6 checklist ID and leaves the published editions of Worlds 1–4 identical. The source includes a compatibility note for older builds.

The reader preview was built locally for checking. The live GitHub walkthrough was not published, so it does not ask players on the older installed build to tap these newly restored details.

## Enemy art still shared

These are existing shared-art bindings, not missing files. They warrant a separate visual review rather than an automatic replacement campaign; some Source echoes may intentionally reuse earlier silhouettes.

| Shared art | Enemies |
| --- | --- |
| Heavy Loader | Faulted Loader, Heavy Mech, Slag Golem, Welder Bot, Conveyor Crusher, HK Droid, Crucible Champion |
| Echo Borer | Echo Borer, Glitch Hound |
| Dominion Dampener | Dominion Dampener, Flame Trooper |
| Resonance Buoy | Resonance Buoy, Sentinel Mk. I, Memory Leak |
| Iron Warden | Iron Warden, Ascended God |
| Ruin Guardian | Ruin Guardian, Firewall Construct, Silent Shore Wraith |
| Sentinel Orb | Sentinel Orb, Void Turret |
| Riot Guard | Riot Guard, Corporate Assassin, Phantom Prototype, Compliance Officer, Elite Guard, Nightmare Guard |
| Aero Drone | Aero Drone, Orbital Fighter |
| Warden | Administrator, Titan Walker, Compliance Avatar, Manager Projection, Ascended Vale |
| Security Drone | Magma Drone, Distorted Sentinel |
| Phantom Assassin | Endless War Echo, Source Shadow |

## Assets and visual evidence

Generated using the built-in image generator with existing portrait/enemy art as visual references; no API key was read. Full prompts, references and final output paths: [art provenance](assets/WORLD5_PLAYTEST_ART_PROMPTS.json).

Final project assets:

- `world_assets/src/main/assets/images/characters/emotes/nova_down.png`
- `world_assets/src/main/assets/images/characters/emotes/zeke_down.png`
- `world_assets/src/main/assets/images/characters/emotes/orion_down.png`
- `world_assets/src/main/assets/images/characters/emotes/gh0st_down.png`
- `world_assets/src/main/assets/images/enemies/null_g_drone_combat.png`

All five images have transparent backgrounds. Screenshots and test reports are in [evidence](evidence/world5_direct_fixes_2026-11-12/).

## Verification

- Android compilation succeeded.
- Public World 5/6 navigation, readiness, Worlds 4–6 interactions and walkthrough reachability checks passed (23 tests).
- Android prompt-manager, World 5/6 discoverability and data-integrity checks passed (57 tests).
- Arcade discovery/reload, optional inspection, menu-tinkering, asset and notification tests passed (18 tests). Total: **98 focused checks passing**; reports are preserved with the evidence.
- The full Android suite has **23 pre-existing failures**, reproduced on unchanged revision `c52dcf48` in an isolated checkout (816 tests). These cover campaign-route assertions and older Astra/music/cooling tests. The initial changed build reproduced the same failure names plus one stale cabinet-disappearance expectation; that expectation was updated for the consistent gray-until-repair behavior, and the focused data-integrity suite now passes. These older failures remain outside this pass; the full suite is not green. See the verification summary and logs in the evidence folder.
- Reviewed the acquisition card at phone width and the four KO portraits in the desktop battle formation. Android was compiled, but this pass did not perform a fresh phone playthrough or balance test.

## Remaining decisions at the end of this pass

Follow-up: the approved music and Charles recommendations below are implemented in [the Charles and music pass](WORLD5_CHARLES_AND_MUSIC_2026-11-12.md). The bunk corridor and the plan for unique artwork for every enemy remain deferred.

1. World 5 music direction and a replacement track.
2. Charles's motivation and how much trust he should extend before helping.
3. Whether Charles's two circuit boards and one focus lens should stay satisfiable from carried inventory, or gain a World 5-specific requirement.
4. The Astra bunk corridor and the role/presentation of crew downtime.
5. Which remaining shared enemy silhouettes deserve dedicated art.
