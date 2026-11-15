# World 6 finale proposal

The current implementation adds the campfire, three refusals, grave and Vale dialogues, the lift and threshold effects, and an expanded illustrated ending. The Final Note and A Long Story run for about 2 minutes 55 seconds at their authored timings. The shared Astra boarding sequence covers the hangar ramp, hub map, and Morning Street shuttle. The proposals below retain the broader design, including the playable return.

These are design proposals for discussion. The interaction and puzzle fixes are the implementation portion of this pass.

## Current findings

The campfire promise is a single message containing speaker prefixes and stage directions. The three Memory Stair refusals each set their milestone immediately and show one explanatory message. Vale's confrontation likewise credits the task before showing one message with Vale and Nova's names embedded in its text. These are the direct causes of the presentation problems in the notes.

The Source transition lasts 1,050 ms. The threshold event also shows a message that describes Nova rejecting Vale's offer before the player performs the three required refusals. That prematurely resolves the emotional question.

The Elevator is a room with west/east connections and no authored travel action. Walking through those connections gives ordinary room movement. A lift transition should happen on the elevator's eastward departure toward Memory Bridge, with its reverse leg supported as appropriate. Entering the elevator from Manager Patrol Route should establish the cage and doors before movement begins.

The existing epilogue credits cinematic has ten steps, and the final-note cinematic has six. Length and payoff should be judged across these scenes and their playable connections.

## Proposed sequence

1. Shared Memory Span becomes a brief crew conversation with individual portraits. Keep Gh0st's hand on the bridge as action narration between spoken lines. Award the promise task after the conversation finishes. Revisits offer a concise recollection.
2. Singularity threshold has a distinct first crossing of roughly three seconds. Hold the light, remove the room from view, then let the stair resolve. Use the existing chime and a controlled arrival. Repeat crossings can use the shorter Source style. Replace the premature refusal summary with uncertainty.
3. Jed's door offers a specific ordinary moment with Jed. Let Nova hesitate and speak. Her refusal should recognize the loss without claiming grief itself was beneficial.
4. Astra wreckage offers an undamaged ship. Orion identifies what that version erases. Nova refuses a recovery that removes a crewmate.
5. Foundry catwalk offers Gh0st freedom from his origin. Let Gh0st make the refusal himself. Nova supports his right to choose.
6. Each refusal has a clearly worded response that completes its scene. Players may step away and return. Award each milestone on the final response and preserve partial progress across reloads.
7. Aethel grave uses a separate remembrance scene centered on Orion and the names already traced. Keep its optional quest reward independent of the three required refusals.
8. Vale's confrontation uses a full conversation with portraits and action narration. He should connect reconstruction to the harm players witnessed and defend the removal of loss and separation. Nova's answer should draw on concrete choices from the journey. Credit Confront Vale at the end and then expose the fight.
9. Between Vale's combat forms, retain the existing recovery/save structure and make the transformation legible in the scene.
10. After the final victory, give the crew a quiet immediate aftermath, establish what happens to people bound into the Source, and make the journey out feel physical.
11. Expand the return with crew-specific closure, selected quest consequences where recorded, and Nova's personal response to Jed's absence. End on an ordinary action that echoes the beginning. Let the player advance dialogue at their own pace.

## Decisions to discuss

- How much permanent harm remains after the Source collapses, and what can the survivors rebuild?
- How should Nova's final scene resolve the existing memory and hearing details?
- Which optional quest outcomes should receive explicit ending acknowledgments?
- Should the extended return be playable, cinematic, or a mixture? A mixture would fit the current playable epilogue and preserve room for quiet exploration.

## Travel families for review

- Repeatable passages can keep the brief passage treatment.
- Elevators need doors, motion audio, a held travel beat, and arrival. Reuse one lift family with location colors.
- Routine Astra boarding and disembarking already use AIRLOCK in ExplorationController. Compare those entry points with the ending shuttle's authored event to find the missing presentation difference. Apply the selected treatment through the shared entry path.
- Launches between worlds should retain their longer departure treatment.
- First crossings into major Source spaces deserve individual pacing. Use a shorter variant for revisits.
- Forced story relocations should establish who moved the crew and why, then show arrival before follow-up dialogue.

SCRIPTED_TRAVEL_REVIEW.md and SCRIPTED_TRAVEL.json list the event transfers and room controls for that review. Ordinary directional elevator legs and runtime Astra entry/exit are additional entry points confirmed in code.
