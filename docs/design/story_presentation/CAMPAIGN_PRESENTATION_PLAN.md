# Campaign story presentation plan

## Status and intent

Draft for review, based on the live campaign assets on November 13, 2026. The world transition concepts and sample dialogue below are proposals. Existing story facts are identified separately.

Build five world handoffs and a substantial ending using the illustrated language of the cold open. Develop World 2 to World 3 as the first production scene. Develop Memory Stair as the first interactive dramatic sequence. Use the resulting patterns for the rest of the campaign.

## Verified baseline

- intro_prologue has ten illustrated steps using six image paths, camera transforms, timed captions, ambient sound, music, cuts and fades.
- scene_launch_crash already stages World 1's pod launch, shield rejection, and crash in seven card steps.
- scene_w2_astra_departure already stages the Astra flight and hard landing in five card steps. w2_mq05_confirm_departure queues it from astra_bridge and uses on_complete for World 3 arrival and quest handoff.
- w3_mq15_launch_astra uses a launch warp and a message explaining the low-altitude Foundry route.
- w4_mq20_escape_launch warps to Executive Dock, shows a launch message, then starts vale_w4_mq20_coup. Vale revokes Thorne's credentials here.
- w5_mq25_enter_tear uses a Source warp and a message describing the leap and blue fire arrival.
- The campfire promise, three Memory Stair refusals, and Vale confrontation are show_message actions. Refusal milestones and the confrontation task are credited before those messages.
- scene_w6_final_note has six card steps. Its on_complete grants the final reward and warps to source_new_world, then queues the ten-step scene_w6_epilogue_credits.
- The current ending opens one regional corridor. Dominion traffic continues beyond the sector. Nova's Source connection burns out. These are current story facts, with any revisions requiring an explicit narrative decision.
- CinematicScene supports CARD and ILLUSTRATED. Steps support images, camera transforms, duration, portrait, caption style, sound, voice and music cues, cuts, fades and final fade-out. Interactive response choices belong to the dialogue system.

Source files are app/src/main/assets/cinematics.json, events.json, dialogue.json and quests.json, plus gameRuntime/src/main/kotlin/com/example/starborn/domain/cinematic/CinematicModels.kt. The existing Thorne/Vale plan is docs/design/THORNE_VALE_CAMPAIGN_PLAN.md.

## Campaign progression

| Segment | What the player should understand by its end | Main presentation | Supporting storytelling |
| --- | --- | --- | --- |
| Cold open | Someone sent the Chime away during containment failure, and Orion chose stasis | Existing illustrated scene | Recognizable tube, brass instrument, containment sounds |
| World 1 | Nova's labor is disposable, Zeke chooses to help her, the Chime leads outward | Playable escape and character dialogue | Institutional orders, changed lockdown spaces, Jed's absence |
| World 1 to 2 | They escaped the colony but the shield prevents freedom | Expand Planetary Impact into illustrated shots | Nova physically helps Zeke, launch sounds become storm and canopy |
| World 2 | The signal has a human history and the emerging crew needs one another | Discovery and recruitment scenes | Environmental traces connect back to the cold open without answering every mystery |
| World 2 to 3 | The Astra can carry them together, corporate interference controls where they can go | Illustrated Airlane to the Spire | Hands-on cooperation, first wide view of city hierarchy |
| World 3 | Thorne exercises ownership and Vale exceeds the recovery authority she lent him | Playable heist, visible orders, short illustrated lockdown insert | Workers react to imposed restrictions, optional salon consequences |
| World 3 to 4 | The Lens brought consequences and the Foundry hardware is necessary to climb higher | Illustrated low-altitude escape and landing | Brief disagreement about the next risk, practical crew repair work |
| World 4 | Gh0st faces his origin, Thorne owns the project, Vale takes command | Existing discovery and confrontation expanded selectively | Assembly line behavior, worker evidence, Gh0st choosing what to do next |
| World 4 to 5 | Vale removes Thorne's authority and turns the Ring against the crew | Illustrated escape, command takeover and dock approach | Flight shows why the stolen engine and arrays matter |
| World 5 | Thorne bears responsibility, Elara is being used, Vale's goal threatens individual voices | Character scenes, observable system changes, reunion | Thorne's concrete assistance costs her something, crew reacts privately |
| World 5 to 6 | Entering the Tear is a deliberate attempt to reach Elara and stop Vale | Illustrated crossing from physical space into the Source | One last recognizable crew gesture before separation |
| World 6 opening | Vale's painless reality removes identity and choice | Playable rescues with tailored dialogue | Each rescued crewmate does something that contradicts their nightmare |
| Shared Memory Span | The crew can depend on one another before the final ascent | Portrait conversation with action narration | Optional rest and food remain a quiet pause |
| Memory Stair | Each person rejects a particular false repair of their history | Three interactive memory scenes | Changes to room image or sound show each refusal taking effect |
| Vale and finale | The player can explain Vale's offer and the crew's answer | Full dialogue confrontation, combat, illustrated final action | Clear transformation between forms, distinct role for each crewmate |
| Return and credits | The crew understands what survived, what it cost, and what comes next | Illustrated aftermath, playable return, final diner scene | Selected quest consequences, ordinary work and sound, an opening callback |

World 3's overreach and World 4's coup must remain distinct. The Archive gives Vale temporary reach. The Foundry departure establishes that he has stripped Thorne of command. Avoid revealing the completed takeover in an earlier antagonist insert.

## Scene budget and rhythm

Initial editorial targets are 35–60 seconds for the pod crash, 45–60 for the first Astra departure, 30–45 for the Foundry approach, 60–90 for the coup and Ring arrival, and 35–55 for the Tear. These are draft pacing targets subject to phone readability and scene review.

Reserve roughly 25–45 seconds each for two special illustrated inserts, the Archive lockdown and the Project Phantom revelation. Build the latter around Gh0st's reaction after the player discovers the truth. Give the Elara reunion a portrait conversation with one exceptional image if the scene benefits from it.

For the ending, explore roughly two minutes of illustrated final action and aftermath, followed by a playable return and a two-to-three-minute character scene. Reading time and optional conversations can vary. Resolve the outstanding ending choices before writing that material.

Alternate pressure with quiet. Let the first Astra lift breathe before interference arrives. Let the campfire remain quiet. Give the grave silence and space. An image held with sound alone can carry a beat.

## First production scene

Develop Airlane to the Spire. It already has the complete crew, an existing scene ID and completion callback, a clear route, and an established visual consequence. It exercises illustrated travel, portrait captions, practical character action, sound and arrival without introducing a new story event.

The shot-by-shot draft is WORLD2_TO_WORLD3_STORYBOARD.md. Production would expand scene_w2_astra_departure and consolidate its duplicate arrival summary.

## Other methods to use throughout

| Method | Concrete application | Delivery constraint |
| --- | --- | --- |
| Conversation during practical work | Zeke grounds a live bus while disagreeing with Nova's route choice | Give the action a visible consequence |
| Changed locations | A completed worker quest changes a broadcast, a queue, or a resident's routine | Drive changes from saved outcomes |
| Optional private conversations | Gh0st reacts to the Foundry once immediate danger passes | Give the main route enough information on its own |
| Antagonist communication | Thorne issues an enforceable order, Vale then overrides its limit | Show the system obeying and preserve the distinction between overreach and takeover |
| Playable memory | The false mine lets Nova approach Jed's door and hear him | Allow a clear return and preserve refusal progress |
| Repeated image or sound | A closing containment door returns as an elevator cage, then as Jed's offered door | Use repetition at moments with changed meaning |
| Short travel vignette | Repeat Astra boarding uses a concise door/crew/engine sequence | Keep routine travel responsive |
| Aftermath | A crew member helps another stand before discussing victory | Establish physical survival before exposition |

Choose a small number of deliberate callbacks. The Chime signal, hands sharing a dangerous load, and doors closing or opening are initial candidates. Check their actual recurrence while scripting.

## Production sequence

1. Review the campaign map and first storyboard together. Resolve destination framing and any disputed story facts.
2. Produce the first Astra departure using three original illustrations, current character references and the cold open's visual treatment. Test shot timing with temporary holds before final sound work.
3. Play the entire World 2 departure into the first World 3 room on Android. Verify normal completion, skip, interruption, reload, arrival music and readable captions.
4. Build Memory Stair's first interactive scene, then use the approved structure for the other two. Verify partial completion in every order.
5. Expand the remaining world handoffs. Review the Archive, coup and Ring revelations as a continuous antagonist arc.
6. Finish the special scenes and extended ending after deciding Nova's personal cost and the surviving world's condition.
7. Reconcile each affected walkthrough after triggers and scene flow are stable.

Use the authorized OpenAI API workflow for commissioned images and ElevenLabs for any new music or sound. Match the cold open's existing art before commissioning a batch. Final asset count and audio needs follow storyboard review.

## Completion and save behavior

Author a cinematic as presentation with one owning progression callback. Skipping reaches the same completed state and reward as watching. An interrupted pending scene must resume or safely restart from its checkpoint without a second payout or an early world unlock.

Verify the current coordinator's skip, pending-scene and on_complete behavior before wiring additional sequences. Longer scenes increase the importance of that verification. Test a save made before departure, while a scene is pending, and after arrival.

On return visits, short transport effects and optional scene replay can use separate presentation paths. Show the destination before its queued conversation starts. Hand music from the scene to the destination room cleanly.

## Open narrative decisions

- Keep the current regional victory or broaden the ending's effect on Dominion?
- Keep Nova's Source burnout and existing memory/hearing details as the personal cost, or revise them?
- Which optional outcomes deserve an explicit consequence during the playable return?

The proposed scenes assume the current regional victory and Source burnout until those decisions are made.
