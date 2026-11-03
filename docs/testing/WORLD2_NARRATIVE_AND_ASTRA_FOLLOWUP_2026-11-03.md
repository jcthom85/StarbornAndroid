# World 2 narrative and Astra followup

This pass implements the four approved followups: Orion's identity reveal, Botanist completion beside the player, Gh0st's recruitment pacing, and entering the repaired Astra before its first departure. Other UI redesigns, roaming enemies, puzzle bonuses and later-world content remain outside this pass.

## Orion

- NPC pill and dialogue speaker display `???` until Orion introduces himself. Internal NPC identity, portraits, voice profiles and dialogue routing still use Orion, so interaction and audio lookups remain stable. The portrait's accessibility label follows the displayed identity too.
- A short introduction records `ms_w2_orion_identified` when dismissed. Subsequent lines and the NPC pill use his name. Loading an earlier save restores the unknown identity.
- Murals, pod inspection, Ring Array retries, awakening narration and the first awake room description avoid prematurely naming him. Sleeping Giant's journal giver is Sanctuary records.
- The evening-tide line now refers to his own hope of waking again after sealing the pod to preserve the signal. It no longer invents someone who promised to wake him.
- Saves that already record his completed introduction/briefing preserve the known identity. Other-world debug states do not become unidentified merely because they omit World 2 history.

## Botanist

The fifth scan keeps its normal inspection popup. After the player dismisses pending narration, a normal Zeke dialogue opens where the player is. Finishing that conversation records the review task and gives the existing rewards: 100 event XP, two Neural Stabilizers and the normal quest-completion AP. No reward was increased.

The check accepts any scan order and also handles older saves that already have all five scans. It respects active dialogue/cinematics and loading. A duplicate scan or completed-quest reload cannot pay again.

Zeke's optional offer remains available at Stasis, where the root specimen supplies a relevant starting point. That placement is an opportunity to start the quest; it is no longer a mandatory return destination. Keeping the normal eligible turn-in dialogue also supports resuming the conversation through Zeke.

## Gh0st

- The confrontation has a brief pause: Nova challenges the unacknowledged order, and Gh0st warns her about the approaching Beast.
- Victory leaves a moment of quiet and the deliberate Anchor Drill interaction, rather than immediately announcing recruitment.
- The existing regroup/drill cinematic begins with a cautious agreement. Gh0st acknowledges choosing to warn Nova and wants answers about the silent channel; Nova accepts him with a boundary on the rifle. This is a practical alliance, not instant trust.
- Gh0st joins once when the cinematic finishes, alongside the existing Link unlock and Hunter completion. Saving during the drill and resuming it retains these one-time effects.
- Existing saves that recruited him at victory retain him. The revised completion does not duplicate recruitment or AP. New runs recruit him after the drill; leaving to do optional fights before finishing that interaction will therefore leave him outside the party until the agreement is completed.
- The drill's old reference to Zeke's wound was replaced with Nova's shaking hand, consistent with the opening injury cleanup.

## Astra

1. Restore the ship at Hangar Bay as before.
2. Use launch cradle to board at the cargo bay. Boarding records the exact Hangar Bay docking location and uses the existing ship rooms and audio.
3. Walk east through the common room, then north to the bridge.
4. Use nav console for the first-flight confirmation. The normal destination browser is deferred until that story departure is complete.
5. Stay docked if desired; return south, west and through the cargo ramp to Hangar Bay. Repairs, quests and docking state persist through save/load and reboarding.
6. Confirm departure from the bridge. Only completion of the existing cinematic grants Liftoff rewards and begins World 3.

The shared event layer gains a small `board_astra` action and an explicit `in_room` condition. Boarding and confirmation are physically gated to Hangar Bay and Astra Bridge respectively. Direct stale confirmation calls outside the bridge fail. Existing first-flight cinematic recovery and normal later Astra navigation remain intact.

## Documentation and checkpoints

The World 2 completionist walkthrough now describes the final-scan review, later Gh0st join and first-flight ship route. Its generated catalog references and directional-route check are refreshed. Earned crew playtest snapshots were regenerated from the production campaign route; changes record the identity milestone, actual Astra visit/dock and persisted ground-loot state.

These are local changes. The Pixel build being playtested was not replaced, and this pass did not commit, push or publish the guide.

## Verification

Validation completed:

- Android host suite: **784 tests, zero failures, zero skipped**.
- Targeted desktop suite: **19 tests, zero failures, zero skipped**, including the copied Pixel Tideglass save and prior persistence/gate regressions.
- Fresh Android debug APK assembles successfully. It has not been installed in this pass.
- World 1 checkpoint manifest: **10 earned states** match current content after regeneration from the successful completionist receipt.
- World 2 walkthrough: **245 authored directional connections**, plus generated references for **93 World 2 rooms and ten quests**, pass the guide check.
- `git diff --check` passes.

Targeted production-controller tests cover unknown/known identity and earlier-load reset; a final moss scan at Crash Site with one-time reward; Beast victory without premature recruitment, a saved pending drill and one-time completion; and boarding, staying docked, save/load, disembarking, reboarding and departing from the bridge.

Manual checks still matter for the emotional pacing, typewriter/tap timing, portrait presentation, ship music transition and sense of arrival. Check these on the next Android build and on desktop; automated state progression does not certify their feel.
