# Starborn systems overview

Status: Implementation reference, reviewed October 14, 2026. Describes inspected code and data, not release or device acceptance. See [Current State](../CURRENT_STATE.md) and [the index](../README.md).

## Runtime and platforms

- `gameRuntime/src/main/kotlin/com/example/starborn/` owns shared gameplay controllers, models, events, quests, inventory, crafting, combat, movement, and persistence.
- `app/src/main/java/com/example/starborn/` contains Android services, navigation/lifecycle wiring, and Compose UI.
- `desktopApp/src/main/kotlin/com/example/starborn/desktop/` contains Windows services, input, UI, audio, and packaging adapters.
- `shared/` contains shared Compose resources/presentation infrastructure; not every screen is shared.
- Content comes from `app/src/main/assets/` and `world_assets/src/main/assets/`. Desktop stages source assets and checks their packaged bytes.

Inspect live routes before treating a helper, preview, or old ViewModel as production behavior.

## Exploration

Worlds contain hubs, nodes, and rooms. Android uses portrait illustrated rooms, cardinal swipes and visible controls, tappable prose, and authored special exits. Desktop presents shared actions through mouse/keyboard controls and a separate layout.

`RoomPresentation.kt` resolves descriptions, explicit links, and action presentation. Room state, milestones, variants, and gates affect what players see and can do. A generic action can dispatch a substantial event; its type alone does not establish depth.

Enemy movement is implemented through `EnemyMovementManager`, `enemy_movement.json`, the exploration controller, and persistent enemy-party state. Not every encounter moves. Check the room, catalog, progression, and visibility/lifecycle gates.

## Combat

`CombatController`, `CombatEngine`, and `CombatActionProcessor` handle ATB readiness, actions, targeting, cooldowns, statuses, stability, AI, and outcomes. Cooldowns count the acting character's turns. Source abilities use authored acquisition rules. There is no universal enforced six-core-plus-six-Source kit.

Weakness can increase damage, accelerate active cooldowns, and increase stability damage. Break creates an opening for direct damage. Use the current affinity rules and action processor for exact values.

HP, equipment, owned supplies, learned abilities, and meals affect encounters. Battles do not reset everyone to an equal full-health baseline. Menus, tutorials, animations, and background state participate in ATB pause handling; independent prompts/minigame timers require separate lifecycle verification.

The catalog status Erosion is a damage-over-time effect. It does not establish a persistent hunger/burnout meter reducing field HP or Focus. Narrative Source strain remains a story fact.

## Progression

`LevelingManager` uses `leveling_data.json`; `progression.json` defines level-based skill unlocks. Combat stats derive from character data, equipment, supported passives, and meals. Level is not a general automatic stat-growth multiplier.

Skill trees use AP, prerequisites, and tier rules. A tree entry or successful purchase does not prove its advertised effect is supported. The October 14 evaluation identified unsupported effects and field/combat HP presentation disagreement. Use `PartyHealth` for effective HP rules and check caller consistency.

A growth curve, replacement skill kit, or respec system is a gameplay change requiring implementation and balance work, not a documentation correction.

## Equipment, food, and crafting

Weapons, armor, accessories, snacks, and gear modifications have ownership, compatibility, unlock, and assignment checks. Use `GearRules`, `ModEquipmentRules`, controllers, and catalogs.

- Ordinary healing items consume inventory according to their effects.
- Equipped snacks supply reusable cooldown abilities. Their target scope is authored, not universally self-only.
- Prepared meals consume portions and apply party bonuses for three encounters under `MealRules`. Chef selection can change bonuses; replacement and cooking have explicit rules.
- Tinkering uses recipe discovery, tools, and ingredients through field-menu and authored station routes. Check actual eligibility.
- Shops calculate prices, gates, stock, and accepted sale items through `ShopController`. Catalog arithmetic alone does not establish campaign affordability.

Fishing and six arcade games have shared gameplay and separate presentation. Verify acquisition, cancellation, acknowledgment, and repeat rewards through live routes.

## Astra, cinematics, and persistence

The five-room Astra supports travel, preparation, rest, discoveries, optional activities, and chapter-dependent crew check-ins. [Astra functionality](hub_ship_astra/01_functionality.md) distinguishes facilities from retired proposals. Simulations use normal combat rewards and consumable costs.

Cinematics include illustrated timed scenes and text/card sequences. Settings include effects quality and accessibility/comfort preferences. Verify each screen consumes them; model fields alone are not acceptance.

Session state, protobuf persistence, inventory synchronization, pending scenes, and battle checkpoints support save/load and recovery. Serialization parity does not prove every process-death boundary. Check interruption around payout, encounter retirement, progression, puzzles, and scene callbacks.

## Authoring and verification

Stable content IDs are compatibility contracts. Reuse current IDs and schemas rather than imposing an old naming convention. Preserve event ownership and exact-once rewards; use supported explicit action references when prose labels change.

Checks live in `scripts/`, Android-hosted tests in `app/src/test/`, device tests in `app/src/androidTest/`, and desktop tests in `desktopApp/src/test/`. Record build, command, result, and limits. Historical passing totals do not establish a current green baseline.

Art framing and sprite workflows belong to the [Art Production Guide](Starborn_Art_Production_Guide.md). The old mandatory black bars, fixed sprite dimensions, universal skill counts, and equal-baseline combat claims are retired.
