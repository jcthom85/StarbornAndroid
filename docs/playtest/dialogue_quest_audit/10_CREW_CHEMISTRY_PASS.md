# Crew chemistry and rest delivery

November 16, 2026. Follows the completed [continuous scene review](09_CONTINUOUS_SCENE_REVIEW.md).

## Editorial scope

This pass strengthens optional crew interactions between the major scenes, primarily in Worlds 2–5. It does not add compulsory scenes, new tasks, relationship scores or rewards. The opening, major losses, Elara reunion, refusals and ending retain their previous writing and presentation.

Four existing companion conversations now have short continuations, nine new dialogue entries in total:

| Existing entry | New interaction | Purpose |
| --- | --- | --- |
| `orion_w2_post_stasis_idle` | Nova offers to wait or go back inside. Orion chooses to look a little longer, then take the path. | Give Orion a preference and let Nova accommodate it without deciding for him. |
| `zeke_w3_post_social_idle` | Nova offers to watch the door so Zeke can stop checking the scanner. He admits he checked three times, then pockets it. | Turn anxiety into an exchange where someone else takes responsibility; keep the humor small. |
| `ghost_w4_post_assembly_idle` | Nova offers another minute. Gh0st instead asks her to walk with him to the door. | Allow him to request company without another speech about the production line. |
| `orion_w5_post_docking_idle` | Nova offers her hand; Orion asks for a slower pace and company. She shortens her stride. | Let help be negotiated, with a visible response rather than a declaration of trust. |

The original entry conditions and text remain. Continuations are scripted-only and do not become standalone NPC topics. These exchanges cannot advance a quest or supply a second reward.

Rest copy now includes 27 individually identified entries across the existing regions, including retained lines and new short exchanges. Examples include Nova and Zeke sorting out their tea cups, Gh0st helping hold a boot seal, Zeke checking his reflection before entering executive spaces, Orion asking Nova to leave some light, and Zeke leaving a seat for Gh0st without requiring him to talk. Not every entry is a joke or a disagreement.

The heavier material remains gated. The Foundry seat exchange follows Cooling Springs; the names exchange follows Thorne supplying the list. The Spire reflection joke stops after taking the Lens. The Foundry vibration joke stops after discovering Phantom records. The Ring carpet complaint stops after finding Thorne. Pair exchanges require both participants, and companion-specific lines require that companion in the current party.

## Why the delivery needed changing

The former rest selector returned the first eligible character. With the usual party, that prevented later branches from appearing. There was a second problem: most rest events supplied an authored message, which bypassed companion selection entirely. Changing only the speaker priority would not have fixed normal rest-pill behavior.

The new selector includes the authored location message in the same rotation. On an unvisited rest spot, that description gets the first turn. Subsequent rests select unheard eligible entries. Once that eligible set is exhausted, it cycles again and avoids immediately repeating the previous entry when another is available. Newly recruited companions and newly unlocked material become eligible without resetting everyone else's history.

The opening's authored rest messages remain unchanged. The Astra is treated separately from the world it is visiting. Deep Ring rooms now use Ring material instead of falling through to mine dialogue. The Source uses quiet, party-aware lines, including when companions are absent during the rescue sequence.

Selection happens only after the controller can restore a recognized party. Resting at full HP still gives a conversational turn. Healing calculations, camp rewards, cooking and quest progress are unchanged. The existing event system still combines a rest event's location message into one recovery notice; the rotation does not create an extra message popup.

On Android, recovery text was limited to four lines with an ellipsis. It now has a bounded scrollable text area so the complete exchange remains readable. The location-specific title, recovery information, and tap-to-dismiss behavior remain. Desktop already displays the rest message in its modal.

## Save behavior

`heardRestBanter` and `lastRestBanterId` are dedicated session fields, serialized in new protobuf fields 63 and 64. Older saves default to empty history. They are separate from quest milestones, tutorial flags and completed events, and contribute to the session fingerprint so a changed history can be saved. History tracks selected rest messages, not a claim that the player read every word.

Only currently eligible entries are cleared when a cycle finishes. Absent characters retain their prior history. Reloading continues the rotation rather than always selecting the first speaker. New-game defaults start with an empty history.

Implementation is in `gameRuntime/src/main/kotlin/com/example/starborn/domain/narrative/RestBanter.kt`; the normal exploration rest controller uses it for event-driven and fallback rests. Existing one-time camp supplies remain managed by their original events.

## Verification

The first focused run passed **51 checks**: rest rotation (6), session persistence (27), healing/controller integration (1), dialogue service (7), dialogue/event integration (2), and campaign catalog integrity (8).

The new coverage checks location-copy rotation, all eligible entries before repetition, cycle boundaries, absent/recruited companions, story gates, Astra/Deep Ring selection, old protobuf defaults, save/reload continuity, session fingerprint changes, and event-message coalescing. The controller check verifies that authored messages no longer bypass rotation and repeated full-HP rests do not change healing behavior.

The dialogue comparison confirms that the four existing entries only gained continuation links; their original text, conditions and triggers were preserved. All new continuation targets resolve. The final earned main-campaign route also reached credits, exported all eleven review saves and passed the pending-Echo save/reload branch. The rest checks passed again, and the regular Android build succeeded. The later portrait addition is verified separately below.

## Expression art follow-up

The user authorized generating useful expressions through their OpenAI API key. After inspecting Nova's cool/puzzled/happy portraits and Zeke's cool/puzzled/worried portraits, I chose one restrained addition: **Zeke, sheepish**. The existing worried face reads as alarm and the cool face reads as smug confidence; neither fits his small admission that he checked the scanner three times. An eye roll would make Nova's response to anxiety less sympathetic, so it is not added to that exchange.

The approved `zeke_cool.png` was edited with the image-generation CLI using the OpenAI API, `gpt-image-2`, `quality=low`, 1024×1024. The shipped file is `world_assets/src/main/assets/images/characters/emotes/zeke_sheepish_v1.png`. It is wired as an explicit portrait override on `zeke_w3_pocket_the_scanner`, using the existing dialogue portrait consumer. The neutral portrait and combat/victory expressions are unaffected. Rest cards remain text-based; this expression appears in the companion conversation.

Only the generated face interior is blended into the original portrait. The original alpha is preserved byte-for-byte, and pixels outside the feathered face region remain the original art. The final image was inspected at full size, at 64-pixel dialogue scale, and in a 128-pixel comparison against the source. The prompt and raw output are retained locally in `tmp/imagegen/zeke-sheepish/prompt.txt` and `output/imagegen/zeke_sheepish_edit_v1.png`; neither includes the API key.

After integration, the portrait/emote validator passed (51 emote references checked), the focused rest/dialogue/catalog checks passed, and the regular Android build succeeded. The APK includes the exact final portrait and dialogue assets. The build reflects the current shared workspace, including the separate fishing update; this pass did not edit fishing files. Nothing was installed to either device during this pass.

Final generation prompt:

```text
Use case: identity-preserve
Asset type: Starborn dialogue portrait expression variant, Zeke sheepish.
Input image: edit target is the approved Zeke portrait. Change ONLY the facial expression inside the face mask. Keep his exact identity, spiky blond hair, amber eyes, freckles, blue jacket with yellow trim, white shirt, gloved hand at chest, pose, framing, proportions, thick dark outlines and crisp warm cel shading unchanged.
Expression: quietly caught out, a slightly sheepish admission to a trusted friend. A small closed-mouth asymmetrical apologetic smile; one eyebrow slightly lifted and the inner eyebrows softened; amber eyes glance slightly sideways. Warm, self-conscious, mildly funny, not smug, distressed, angry, or laughing. Read clearly at 64-pixel portrait size without exaggerated sweat drops or comic symbols.
Preserve all non-face pixels and the existing backdrop/transparent silhouette. No text, no extra objects, no redesign, no camera or pose changes.
```

## Editorial judgment and boundaries

The additions give the crew more ways to be together: asking, waiting, accepting a correction, sharing a seat, and making small practical mistakes. Humor comes from the immediate situation. Gh0st's brevity is still chiefly protective rather than a recurring punchline; Zeke's competence and fear are not reduced to paperwork; Orion can ask for something ordinary; Nova changes what she does when someone asks.

No essential emotional setup depends on repeatedly resting or exhausting optional topics. This pass is reviewed as writing and tested for delivery/progression; it is not a new manual device playthrough or proof of player enjoyment. It does not require another user editorial review to be considered implemented.
