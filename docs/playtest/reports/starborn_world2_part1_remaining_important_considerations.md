# World 2 Part 1 — important considerations status

October 30, 2026. Consolidated from both transcript halves, with duplicate observations combined. Updated after the approved fishing and important-considerations implementation: **33 addressed; active-quest organization ignored for now. Residents/Megan completed October 31.** Critical fixes are tracked separately; resident backstory/placement (#7) was completed October 31.

## Complete status list

| ID | Consideration | Original decision / scope | Current status |
|---|---|---|---|
| IC-01 | NPC names in room descriptions | Replace the background-highlighted treatment; decide whether names remain inline actions and how colors relate to room themes. | **Addressed:** Themed inline actions; removed background highlight. |
| IC-02 | Party-character selector | Make scrolling discoverable and the layout work with both two and four companions. | **Addressed:** Smaller cards and overflow swipe cue. |
| IC-03 | NPC/service pill widths | Fit labels more closely while preserving usable touch targets; no larger pills requested. | **Addressed:** Reduced minimum widths; touch heights retained. |
| IC-04 | Enemy Fight pill appearance | Review together; preserve its useful distinction between enemies and NPCs. | **Addressed:** Pill now reads HOSTILE. |
| IC-05 | Combat ability layout and detail density | Reduce the left-heavy/vertical presentation and reconsider attack-power percentages. | **Addressed:** Full-width summaries, clearer controls, removed power percentages. |
| IC-06 | Combat enemy-name truncation | Verify the report and provide adequate room for names, possibly relocating numeric HP. | **Addressed:** Wrapping name above separate HP line. |
| IC-07 | Victory-screen timing | Test a short, readable celebration; roughly two seconds was suggested, not finalized. | **Addressed:** Two-second victory presentation. |
| IC-08 | Tutorial heading/copy standard | Remove redundant Combat/Combat labeling and developer-facing 'World 2' wording; review tutorial consistency. | **Addressed:** Consistent headings and player-facing combat copy. |
| IC-09 | Quest-detail spoilers | Audit information against what the player currently knows. Deferred; not tonight's blocker repair. | **Addressed:** World 2 summaries/objectives retain current leads without naming later reveals or prescribing puzzle answers; future-stage gating verified. |
| IC-10 | Active-quest organization | First establish whether it is actually a usability problem. Completed-world titles are already fixed. | **Ignored for now:** per user request. |
| IC-11 | New-quest announcement | Explore a slower, more dramatic arrival/fade/dimming treatment without applying that delay indiscriminately to routine updates. | **Addressed:** 650 ms entrance fade and dimming; manual dismissal retained. |
| IC-12 | Save/load ordering | Autosave above Quicksave. | **Addressed:** Autosave above Quicksave on Android and desktop. |
| IC-13 | Discovery-description standard and audit | Review containers/object interactions in Worlds 2–6 individually. Includes emergency locker, reliquary, cargo crates, tide-worn chest, trampled supply pack and lost surveyor pack. Decide when narration adds value; avoid blanket duplication. | **Addressed:** 51 individually authored container discovery messages across Worlds 2–6; narrated before rewards; existing event discoveries retained. |
| IC-14 | Details-before-rewards sequencing | Finish discovery details, dismiss them, then show item notifications. Remove the brief reward flash/overlay replacement. | **Addressed:** Narration blocks rewards until dismissed; ordering regression tested. |
| IC-15 | Film 3 discovery copy | Describe recovering the waterproof cargo case's film rather than repeating the item notification. | **Addressed:** Discovery prose describes recovery rather than repeating the award. |
| IC-16 | Cooking screen and feedback | Review layout and interaction logic; unify the visual language and remove redundant/inconsistent toast/banner feedback. | **Addressed:** Single styled cooking feedback; duplicate snackbar removed; logic reviewed. |
| IC-17 | Shop feedback and dialogue | Restyle purchase notifications; align shop dialogue with the standard panel and desired portrait pulse animation. | **Addressed:** Styled purchase feedback; standard dialogue overlay reused. |
| IC-18 | Dialogue typewriter tap area | Before choices appear, tapping anywhere in the panel should reveal text; once choices appear, taps must select them safely. Scrollable exits are fixed separately. | **Addressed:** Whole-panel reveal tap; choices remain safe; Android test passed. |
| IC-19 | Physical-action choice styling | Replace placeholder-looking bracket conventions with a clear treatment for actions versus speech. | **Addressed:** Action tag replaces raw brackets; existing recognized tags retained. |
| IC-20 | Charlie's identity and interaction payoff | Workshop name discovery, personality/spore-puffball play and the existing reward/help together. Do not independently rewrite or add a second reward without reviewing the current interaction. | **Addressed:** Unseen Charlie puffball interaction, persistent trust, one-time comfort stone and repeat play; no NPC portrait or dialogue panel. |
| IC-21 | Music transitions | Improve immediate crossfades; keep a deeper composition/timing/audio pass separately scoped. | **Addressed:** Longer looping-track fades; deeper audio review remains deferred. |
| IC-22 | Bioluminescent Prism presentation | Improve Starborn styling and keep the primary Capture Light control reachable at a glance; support large text rather than hard clipping. | **Addressed:** Compact styled panel with Capture outside scrolling content. |
| IC-23 | Enemy-art mismatch | Investigate Stalker-Vine's bat image and other actual placeholder assignments before commissioning replacements. | **Addressed:** Original transparent Stalker-Vine art replaces bat assignment. |
| IC-24 | Encounter-party variety | Audit W2–6 composition, size, synergies, pacing and recovery with intent; don't assume every fight is single-enemy or enforce a numeric quota. | **Addressed:** World 2 regrouped into three tested pairs and seven solo fights; total enemy roster/XP retained. Later worlds retain their existing groups. |
| IC-25 | Rest jingle | Choose a fitting short stinger; separate from reusable REST behavior, which is fixed. | **Addressed:** Original short rest chime replaces bunk-light cue. |
| IC-26 | Ability terminology | Use 'ability' rather than 'skill' in player-facing acquisition language and related copy. | **Addressed:** Victory acquisition uses New Ability/Abilities. |
| IC-27 | Crafting material distribution and incentives to fight | Review the cutter bundle; consider discoverable exploration/combat sources without RNG locks, repetitive formulas or mandatory grinding. Discuss before moving rewards. | **Addressed:** Guaranteed, room-local organic and mechanical materials complement existing supplies; cutter repair stays deterministic. |
| IC-28 | Aggression and encounter-initiation animation | Discuss desired enemy behavior, warning/approach feedback and player control before changing aggression globally. | **Addressed:** World 2 keeps player-initiated static fights, with a brief approach cue; existing timed patrol behavior remains unchanged. |
| IC-29 | Camp first-visit supplies | Understand whether the Neural Stabilizer grant belongs at camps. Repeated REST no longer repeats these grants; whether the initial gift is appropriate remains open. | **Addressed:** Removed ten automatic camp stabilizer gifts in Worlds 2–5; unlimited HP rest and earned medical supplies retained. |
| IC-30 | Sentinel-3 voice treatment | Replace placeholder-looking bracketed tone directions with suitable machine sounds while retaining accessible text. | **Addressed:** Machine murmurs replace bracketed tone directions; readable text retained. |
| IC-31 | Temple-to-Hall reveal | Workshop an entry/gate-opening effect that feels physical and remarkable, rather than implying teleportation. | **Addressed:** Gate opens in place with Chime sound and a short stone-slide animation, then normal eastward travel; no forced warp. |
| IC-32 | Fishing design pass | Discuss portrait-oriented playfield, controls, tutorial, contrast, accessibility and catch/result flow. Design subsequently approved by the user. | **Addressed:** Portrait Android fishing, swipe/button hook, contextual tutorial and clear result. |
| IC-33 | Signal Reflection at Beach Cliffs | Verify the action's purpose and feedback before deciding it needs a change. | **Addressed:** Contextual fallback when the optional quest event is inactive. |
| IC-34 | Natural scan-object wording | Functional quest gating is fixed; decide whether descriptive nouns such as ferns/spores should replace scan instructions in the underlying prose. Work through wording together. | **Addressed:** World 2 scan/gather/salvage actions use natural object labels and prose; original quest/event IDs and visibility gates retained. |

## Completed resident continuity issue

Completed October 31: the coastal survivors share food, shelter and water filtration; Kaelen arrived separately. Megan's clinical hygiene background supports Tidewatch Station's practical sanitation work. Existing IDs, favors and rewards are retained.


## Implementation and evidence

See [implementation report](../../testing/WORLD2_FISHING_AND_IMPORTANT_CONSIDERATIONS_2026-10-30.md) for changes, tests and remaining runtime checks. The original scope column records the notes before implementation; the status column is current.

Follow-up: [World 2 encounter and salvage pass](../../testing/WORLD2_ENCOUNTER_AND_SALVAGE_PASS_2026-10-30.md).

Final delegated pass: [quest, discovery, camp and gate changes](../../testing/WORLD2_QUEST_DISCOVERY_CAMP_AND_GATE_PASS_2026-10-30.md). The residents/Megan pass is now completed too. Active-quest organization remains ignored by request.

October 31 follow-up: [unseen Charlie implementation](../../testing/CHARLIE_PUFFBALL_IMPLEMENTATION_2026-10-31.md).

October 31 completion: [resident continuity and final handoff](../../testing/WORLD2_RESIDENT_CONTINUITY_AND_HANDOFF_2026-10-31.md).
