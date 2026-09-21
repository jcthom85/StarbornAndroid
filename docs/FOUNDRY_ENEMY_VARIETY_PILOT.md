# Foundry enemy and encounter variety pilot

## Scope

First playable mechanics pass within the broader enemy-variety plan. Keep the 47-enemy roster and existing IDs. No art generated or API credentials accessed in this pass. Existing shared artwork remains a known limitation. Full campaign balancing and the complete cross-source encounter audit remain follow-up work.

## Decisions implemented

- Slag Golem exchanges generic Guard Protocol for a cooling action after Molten Slam. Cooling applies the existing Radiators Exposed status, reducing defense and creating an attack opportunity. Freeze/Shock weaknesses remain.
- Welder Bot becomes defensive support with Field Weld: squad healing, three-turn cooldown, two uses per battle. Torch Cut remains its offensive option. Remove the misleading destruction-explosion description; Death Burst was an ordinary selected skill, not an authored death trigger.
- Magma Drone retains fast area pressure and evasion. Replace its unsupported `harrier` behavior label with the implemented aggressive profile.
- Enemy `recovery_after` maps replace Titan's ID-specific AI exception and also support Slag Golem. Definitions and histories are indexed by combat instance, including duplicates. Normal disable/cooldown gates still apply.
- Optional explicit skill targeting supports the new squad repair without changing fallback targeting for existing skills.

## Encounter makeup

| Location | Before | After | Intended decision |
| --- | --- | --- | --- |
| Conveyor Belt | two Welders; separate Phantom | Welder + Magma Drone; separate Phantom | Stop repairs or remove the fast damage source first. Avoid redundant double-healer fights. |
| Sorter Spine, optional dead end | Welder | Welder + Magma Drone | A support-and-pressure test after simpler encounters. |
| Core Shadow, optional dead end | Slag Golem | Slag Golem + Welder | Eliminate repair support or exploit the golem's cooling window. |

Retain early solo drones, solo golems, and the Waste Intake's solo/mixed parties. Do not make every encounter a larger party. The two optional pairs increase total enemy rewards and encounter health; assess their value and difficulty in playtests before expanding the rollout.

## Verification and remaining checks

Automated coverage: authored Titan recovery, duplicate Golem recovery, status application, Jammed gate, squad healing without healing opponents, repair cooldown and finite supplies, existing combat and asset integrity checks.

Next playtest: expected World 4 party with ordinary equipment. Compare turns, incoming damage, healing items, target order, and whether cooling is noticeable. Repeat with a weaker party. Verify repair supplies prevent healing loops. Check a cooldown or disable during recovery and save/reload during combat. This pass does not add a windup/intent interface or claim a completed balance audit.

Next content/art pass: distinct silhouettes for Golem, Welder, and Magma Drone after mechanics are validated; then expand the encounter-composition audit to scripted battles and moving parties before applying the approach to Worlds 5 and 6.
