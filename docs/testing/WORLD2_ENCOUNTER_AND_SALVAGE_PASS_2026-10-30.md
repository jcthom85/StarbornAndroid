# World 2 encounter, salvage and engagement pass

October 30, 2026. User authorized Fight → Hostile and implementation of encounter variety, crafting incentives and engagement behavior using judgment. Active-quest organization is explicitly ignored for now. Scope is World 2 content plus the shared Android hostile-pill wording; Worlds 3–6 encounter balance is unchanged.

## Encounter decisions

The existing 13 enemies now form **seven solo encounters and three pairs**. No enemies, enemy XP or credits were added. Three quieter rooms give breathing space rather than another isolated opponent:

| Encounter | Purpose | Local tuning |
|---|---|---|
| Glow-Moss Cavern: two Echo-Borers | Freeze/physical setup and target selection against burrowing enemies | Existing borer stats; second borer moved from Windy Ledges |
| Resonant Canopy: Stalker-Vine + Spore-Spitter | Support-first priority: remove healing before finishing the ranged threat | Vine HP 60, vitality 4, stability 60; spitter HP 30, vitality 3, stability 30. Spitter moved from Stone Courtyard |
| Hound Nest: two Shard-Hounds | Faster pack pressure and deciding when to heal | Each HP 40, vitality 3, stability 28; second hound moved from Architect Gateway |

Other enemy stats and global definitions remain unchanged, including bosses, early combat lessons and later-world reuse. Room descriptions telegraph the new groups and preserve actionable object names. The completionist walkthrough reflects their new locations. The Signal's existing alternative Echo-Borer victory credit remains available; no quest trigger was removed.

Full-strength pairing was too expensive: initial tests found a hound defeat and a healing stalemate. Local adjustments above resolved those cases without globally weakening enemies. At level 8 with Nova/Zeke, basic starting weapons, no special mods or armor, three medkits available and a simple skill/heal policy, all **15 runs** (three groups × five seeds) won using **zero or one medkit each**. Tests assert that resource ceiling. These start each fight rested; this is not an uninterrupted attrition certification. Existing rest opportunities remain available.

## Crafting rewards

Added room-local guaranteed single materials, alongside existing random drops:

- Resonant Sedge Spore-Spitter: Herb.
- One Hound Nest hound: Beast Meat.
- Left Reliquary Ruin-Guardian: Scrap Metal.
- Conduit Junction Sentinel Orb: Wiring Bundle.
- Vents Gantry Sentinel Orb: Battery.

These rewards use existing encounter-instance extra drops, not a new global loot rule. They give combat a dependable small payoff without encouraging rerolling rare drops. The patrol cache retains the damaged cutter, schematic and minimum repair materials: accepting its reward must not become an RNG or mandatory-grinding gate. Broader material redistribution and later-world economies were not changed. One-time exploration supplies remain intact.

## Enemy engagement

Android's enemy pill now says **HOSTILE**. World 2 static enemies remain player-initiated: changing all wildlife into timed auto-attacks would turn reading and exploration into a race. Starting a World 2 encounter now gives a short 350 ms approach cue and subtle enemy-stage scale before combat navigation. Disabling screen shake also suppresses that scale. Existing moving-patrol timers, retreat grace and modal pausing remain unchanged. The brief presentation does not add a new combat simulation phase or change enemy AI.

## Verification and limits

- **61 unit/integration tests passed**, zero failures: new real-controller pair simulations and material delivery, existing World 2 gauntlet, local interactions, movement manager, data integrity, production World 2 route and both Part 1 regression groups.
- Twelve material-delivery simulations (four single-enemy reward cases × three seeds) verified actual victory-to-inventory delivery; the hound reward is exercised by the paired simulations.
- Debug APK assembled successfully. `git diff --check` passed.
- Full `runAssetIntegrity` could not launch its PowerShell validators because this machine lacks `pwsh`; this is an environment limitation, not a passing validator result. Its Kotlin DataIntegrityTest ran successfully in the targeted suite.
- [Evidence manifest and seeded output](evidence/WORLD2_ENCOUNTER_AND_SALVAGE_PASS_2026-10-30.json).

Physical Pixel testing remains necessary for the short approach presentation, grouped-target readability and subjective pacing. Existing saves can already have cleared a source or destination room; relocating enemies is not a migration that reconstructs a player's combat history. A fresh World 2 checkpoint gives the intended comparison. No physical-device install, commit or push was performed in this pass.

## Remaining conversations

1. Why the residents are here, Megan's role and the station's fit (separately deferred critical #7).
2. Charlie's name discovery, play interaction and payoff (IC-20).
3. Stage-appropriate quest information/spoilers (IC-09).
4. Which discoveries need narration before rewards (IC-13).
5. Natural object names versus scan instructions (IC-34).
6. Whether first-visit camp supplies belong there (IC-29).
7. Temple-to-Hall physical reveal (IC-31).

Active-quest organization (IC-10) is ignored for now by request. The [full consideration matrix](../playtest/reports/starborn_world2_part1_remaining_important_considerations.md) is updated to 27 addressed, six awaiting discussion and one ignored, plus the separate resident issue.
