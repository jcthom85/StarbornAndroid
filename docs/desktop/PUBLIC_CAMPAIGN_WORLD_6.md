# World 6 and complete public campaign

The campaign driver now completes all 30 main quests through public controller actions and real combat. The final seven synthetic victories and five hidden action calls have been removed, along with their fallback implementations. Missing public routes or actions now fail the run.

## Finale route

World 6 rescues use the actual Manager Projection, the single Endless War Echo at `source_gh0st_nightmare_node_scale_01`, and Silent Shore Wraith encounters. The driver then visits the song-fire, patrol, bridge anchors, promise, singularity, stair refusals, memory fragments, and spire arena before confronting Vale. It defeats Ascended Vale and Ascended God, solves the visible Harmonic Choir Lock, and advances the authored ending cinematics through credits.

The GH0ST rescue uses an authored single-enemy encounter whose canonical victory satisfies the quest. The paired kill-suite encounter defeated the earlier policy; this run does not demonstrate that the pair is balanced or beatable by this policy. Memory Fragments uses its full three-enemy group, and Spire Arena uses its actual two-enemy group.

The public navigation driver now handles quest-triggered warps by replanning from the actual destination. It also explicitly enters Mine Alpha after the World 1 landing rather than relying on the old route overshoot.

## Earned preparation and combat

Before entering the tear, the party sells its owned Grav Boots for 1,000 credits and buys six medkit-plus items for 978 and three Orbital Glimmer Tonics for 1,125, leaving 228 credits. The route eats two tonics as meals; the third is consumed as combat healing. All preparation uses public inventory, merchant, equipment, skill-tree, and rest controls.

The Source policy uses Nova's earned Stasis ability against single enemies, Blast Wave first against groups, and existing control, healing, and guard abilities. Area targeting checks match the party's actual skill-use event instead of accidentally inspecting an immediately following enemy action. A staggered actor must produce a real skipped-turn event.

The successful seeded run records GH0ST knocked out during the single Echo fight and Nova during Memory Fragments. Both final forms finish without party knockouts or healing-item consumption. Reports distinguish combat knockouts from HP persisted after recovery.

## Ending gate fix

The final regression exposed a production issue: players could open and apparently solve final tuning before both Ascended victories, although the event's conditions prevented quest completion. The shared controller now checks the puzzle's success-event eligibility before opening and submitting. It uses the event manager's existing condition evaluator and completed-event checks, so the authored quest requirements remain the source of truth on Android and desktop.

The campaign verifies tuning is blocked before either victory and after Vale alone, then succeeds after God. The completed ending is saved and loaded into a recreated controller, with exact state parity and the credits milestone preserved.

## Coverage and limits

Validation passes 26 Android-hosted shared-runtime tests and three desktop tests, including event eligibility, campaign/procurement, snack contracts, status ticks, combat clarity, and reward gear. Android debug and Windows portable builds pass, with parity verified for all 1,328 staged game assets. The packaged Windows launcher also passes its startup/content/title-audio decode smoke check; this is not an audible playback test.

Generated evidence is under `app/build/reports/campaign-systems/`, including `public-full-campaign-combat.txt` and `assistance.txt`. The complete campaign now reports zero assisted actions, zero synthetic victories, and zero room-position shortcuts.

This is a seeded controller integration run with virtual combat time, authored puzzle answers, disabled tutorials, public animation-completion callbacks, and the documented starter-component fixture. It proves this runtime route and persistence work; it does not rate mouse/touch feel, readable animations, audible title music, or balance across random seeds. Windows and Android share the tested runtime, but device interaction still needs human playtesting.

Remaining content observations include the World 4 conditioning-records action completing `defeat_prototypes` without a fight and the World 5 Customs Lounge being reachable before the airlock narrative gate. Those are separate authored-content decisions.
