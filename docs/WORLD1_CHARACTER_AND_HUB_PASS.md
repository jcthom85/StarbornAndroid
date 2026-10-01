# World 1 character and hub pass

## Character direction

- Nova: practical, quota-driven, resistant to help; accepts shared responsibility during escape.
- Jed: care through food, spare parts, preparation, and familiar arguments. He wants Nova safe without endorsing her dangerous cutter bypass.
- Zeke: procedural precision and nervous humor. Seeing Nova's disposal order forces him to acknowledge what his paperwork does; escaping means choosing a side.
- Boggs: work and accountability first. His authorization offers a usable record, not a promise to erase the disposal order.
- Warden: protects institutional control; one successful escape threatens the colony's obedience.

Rewritten scenes: Jed's opening and workshop, Zeke's retirement discovery, Boggs' descent authorization, Jed's sacrifice, Warden confrontation, and Zeke's escape/pod conversation. Existing dialogue chains, quest triggers, rewards, and choice IDs are retained.

## Dialogue context

`room:<room_id>` is supported by both production dialogue condition evaluators and the progression reference validator.

Zeke's booth responses require `checkpoint_booth`. His pre-fight response requires `launch_bay` and an undefeated Warden. Post-fight and pod-splice conversations require their corresponding rooms. His global fallback contains no booth reference.

## First Deep Mine descent

Boggs' authorization queues the Deep Mine reveal. The first northward crossing from `admin_elevator` returns to the Logistics hub before visiting `mine_landing`. Deep Mine is selected, announces Boggs' authorization, and its artwork fades/scales into place with a spring animation and existing map selection cue. The first departure action is **Plan mine descent**; the destination button is **Descend**. Later lift actions read **Ride mine lift**.

The reveal is also available if the player returns to the map manually after authorization. Pending/seen flags use persisted room state. The artwork and label appear together. The reveal is acknowledged by a deliberate map selection or entry, never by elapsed time. Already-visited mines and later lift crossings retain direct travel. Mine-to-Echo and Echo-to-Launch connections are unchanged.

## Cinematic and surrounding text follow-through

- Reworked cutter surge, Fork handshake, and launch/crash text around physical events and character reactions. Mechanical causes, calibration targets, audio cues, completion callbacks, and existing main-scene step counts remain intact.
- The launch sequence now reaches the shield before the storm and canopy impact. Nova responds to Zeke during the crash.
- Revised the illustrated prologue's short captions, workshop beats, mine power restoration, isolated server decryption, and lost-shift datapad.
- Jed's farewell is split across three final popups; its existing reward trigger executes after the last beat. Zeke's pre-Warden response acknowledges fear without a joke about the loss.
- Expanded Doc and Scrapper's voices; revised Jed's checkpoint handoff and Boggs' review.
- Corrected the usable mine return route, removed premature Orion knowledge from pod inspect text, updated descent instructions, and replaced the launch outcome spoiler with **Survive the launch**.
- Pod Bay description variants reflect the defeated Warden and Zeke moving into the pod.
- Server Room is identified as an optional records investigation in its hub description.
- First entry into Echo Chamber and Launch Bay gives a brief status announcement and map selection cue. It keeps exploration continuous and adds no compulsory map stop.

## Implementation checks and next playtest

Android and desktop Kotlin compilation passed. Existing scripted routes and Maestro selectors were updated; no automated tests or device flows were run for this pass.

Playtest from before Boggs' authorization to evaluate the new unlock rhythm. Existing saves past the mine will not replay it. Check Zeke before and after the Warden, then inside the pod. Read the revised first-time scenes at normal pace, particularly the retirement discovery and sacrifice; emotional impact and popup length still need player feedback.
