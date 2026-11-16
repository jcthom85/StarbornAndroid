# Emotional continuity and character review

Date: November 15, 2026

Status: Source-led assessment and proposed editing plan. Game assets, progression, saves and installed builds were not changed for this review. The working copy already contains the earlier approved dialogue/item/shop/skill corrections; this assessment reads that copy.

Implementation follow-up: [revision details, review saves and validation](08_NARRATIVE_REVISION_REVIEW.md). The assessment below records the pre-edit findings.

## Judgment

The story has a strong emotional foundation. Jed's practical affection, Nova's impulse to fix danger herself, Gh0st's offered hand, and Orion's disorientation after waking are specific enough to carry a good ending. Several recent scenes already deliver the action and breathing room the older dramatic audit requested.

The next improvement is to connect these scenes more reliably. Some callbacks depend on a moment the player has not experienced. Some important reactions remain optional or summarized. Thorne has a clear motive but little personal presence before her authority begins slipping. Explanatory room and quest prose sometimes tells the player what to feel before a character gets to respond.

I recommend a bounded continuity and characterization pass. Preserve the effective scenes and the approved cinematic presentation. Work inside the current route and quest structure wherever possible.

### Evidence boundary

Reviewed the sequence of all 30 numbered main quests, with targeted reads of dialogue chains, cinematic steps, event conditions and completion actions, room descriptions/variants, relevant side quests and the current walkthroughs. Followed Jed/Nova from the opening to the ending; Thorne/Vale from their first named exchanges to the finale; Gh0st/Elara across the Spire, Foundry, Ring and Source. Examined crew milestone dialogue and rest-text selection, and sampled recurring NPC conversations for voice and motive.

This is **not another exhaustive line-by-line certification of all 536 dialogue entries or all 477 rooms**. The NPC discussion is a named sample. Mandatory means required by the authored quest route inspected here, not proven unavoidable through every runtime recovery path. No fresh continuous playthrough, audio assessment or screen-timing measurement was performed. A source-supported continuity defect is distinguished below from an editorial recommendation.

## Keep what is working

| Scene | Why it works |
| --- | --- |
| Jed's cutter repair and governor disagreement | Nova risks her body to make quota. Jed wants her alive and pushes back without taking her decision away. Their disagreement comes from incompatible immediate needs. |
| Jed at Cargo Lift | “You always have a spare” grows out of the earlier workshop exchange. The failed wrench supplies a physical reason he cannot simply leave. Preserve the farewell. |
| Lift separation | `scene_w1_lift_separation` now shows the car moving, shutters closing and radio falling silent; the room has an after-state. This is already implemented. |
| Orion's awakening | The ceiling he painted and the roots make twenty years personal before explaining the Bridge. Zeke offering a seat gives another character something humane to do. |
| Anchor Drill | Gh0st offers a hand, Zeke grounds the circuit, Orion steadies it, and Nova accepts a shared solution. This is the best existing setup for the finale's crew circuit. |
| Failed Construct and Rylos spared | Gh0st catches Nova; later he lowers his blade by his own choice. The sparing scene exists now and should not be commissioned again. |
| Elara's reunion and Stasis | Gh0st answers her now. Elara works the lock, directs their hands, and stays involved after Stasis. Her passage into the Tear and rescue during the ending are explicitly staged. |
| Jed's door | Nova says she wants to open it. The refusal costs her something. Preserve “I miss him. Leave him as he was.” |
| Campfire promise | The squeaking boot and four sets of footsteps establish recognition through familiarity. It is short enough to be a pause. |
| Diner saucer and breakfast | Zeke steadies a saucer instead of curing Nova. Her decision to eat before working is a good answer to the opening's quota pressure. |

The older [dramatic audit](06_DRAMATIC_MOMENTS_AUDIT.md) remains historical evidence, but its missing-scene findings are not a current production checklist.

## 1. Jed and Nova: make recognition belong to the player

### The existing route

| Point | What the player can know | Assessment |
| --- | --- | --- |
| Opening bunk conversation | Jed sees the dangerous cutter, feeds Nova and sends her to his workshop. | Strong mandatory relationship setup. |
| Workshop arrival | He holds the housing; she notices he kept a spare. | The farewell's spare callback is earned. The arrival event starts Jed's dialogue. |
| Repair and live test | She argues for the governor bypass; he warns her and steadies her hand after the surge. | Both have agency. Preserve the playable repair and test. |
| Cargo Lift | He has already tried the wrench, stays at the lever and hands over the Chime. | The sacrifice follows his established behavior. |
| Warden and pod | Nova names what Jed did; she carries his Chime into the escape. | Good immediate follow-through. |
| Worlds 2–5 | The crew replaces isolation with practical care; Jed receives little direct attention in the inspected main-story exchanges. | A long emotional interval. One restrained reminder would help; repeated mourning scenes would not. |
| Optional Jed's Echo | A popup reports that he offers perfection and Nova remembers a lesson. | Weak delivery and substantial overlap with the later, stronger door temptation. |
| Memory Stair | A wrench taps twice. Jed asks Nova to hold the light; Vale offers continued access to him. | Excellent temptation, but its specific sensory setup is under-delivered earlier. |
| Center and diner | Nova explicitly challenges Vale over the two taps; later recalls Jed handing her a lamp. | These deserve witnessed preparation, not only an assertion of shared history. |

**Finding J1 — high priority, source-supported delivery gap.** The two-tap setup exists in `scene_jed_tinkering_prompt`, but I found no reference to that scene outside its catalog definition in the current game assets/runtime. The active workshop arrival starts the current Jed dialogue, which does not contain the taps or lamp. Do not count the old cinematic as player-experienced setup. The lamp recollection is plausible offscreen history; the editorial issue is that the finale treats these details as especially meaningful recognition cues.

**Smallest change:** Put the habit inside the existing workshop briefing. For example, open with an action beat: “Jed taps the bench twice with his wrench. Nova cuts the power; he passes her the lamp.” Then his practical instruction: “Set it here. You fit the cold loop. I'll hold the housing.” Keep the existing spare exchange and the repair task. This would need no new cinematic asset or side quest.

**Finding J2 — medium priority, editorial.** Jed's Echo currently promises a full lesson but delivers `w6_sq26_hear_lesson` as a summary: “Jed's echo offers to stay perfect forever.” This spends the door scene's temptation early and treats Jed as a theme-delivery mechanism.

**Smallest change:** Give the optional bench a short remembered repair exchange about checking Nova's own work. End with Nova completing the remembered task without an answer from him. Reserve Vale's offer to keep Jed alive for Memory Stair. Preserve discovery of the crooked marks, lesson credit, carry-forward action, Legacy reward and the ability to skip this side quest. The main-story refusal must stand without it.

**Finding J3 — lower priority, editorial.** Add at most one brief, unforced reminder between the escape and World 6. A quiet safehouse repair is a better location than another emergency. Nova reaches to pass a tool to someone who is not there; another crewmate waits rather than explains her grief. First check whether the new opening setup and existing Chime references are enough in a continuous read before adding this.

Sources: [opening/farewell dialogue](../../../app/src/main/assets/dialogue.json), IDs `jed_w1_mq01_workshop_brief_1` through `_3`, `jed_w1_mq01_repair_done_1` through `_5`, `jed_w1_mq04_sacrifice_1` through `_7`; [events](../../../app/src/main/assets/events.json), `w1_mq01_reach_workshop`, `w1_mq04_complete`, `w6_sq26_hear_lesson`; [cinematics](../../../app/src/main/assets/cinematics.json), `scene_jed_tinkering_prompt`, `scene_w1_lift_separation`, `scene_w6_epilogue_credits`.

## 2. Fix the memory continuity before adding more drama

**Finding C1 — highest priority, continuity defect.** The Astra refusal conflates two different journeys. Orion touches the seamless Astra, Vale promises no crash, then Orion says, “I wasn't aboard. In this version, my pod never opens.”

- `scene_launch_crash`: Nova and Zeke crash a cargo pod into Sector 9, before finding Orion.
- `scene_w2_astra_departure`: Orion is already aboard the Astra. He directs the approach to the Spire; Gh0st catches his harness during the descent.
- `w6_mq29_refuse_astra_revision_line_1` through `_4`: the memory visually identifies the latter ship but uses the former crash's causal stakes.

**Recommended correction:** Make the offered revision visibly concern the original cargo pod's departure and crash. Retain internal event/milestone identities where possible; deliberately update the displayed object name, room wording, completed inspection and walkthrough wording together. Supported action-display aliases can preserve the stable action key if appropriate. Do not alter the real travel history to justify the existing line.

Draft direction, not implemented:

> Orion: “The cargo pod. Its hull is whole.”
>
> Vale: “A clear launch. No impact. You and Zeke reach safety.”
>
> Orion: “You never reach Sector 9. My pod stays closed.”
>
> Nova: “We found you. I'm keeping that.”

The player's refusal should explicitly keep the journey that brought the crew together. Preserve the three independent refusals and the leave/reopen behavior.

**Finding C2 — high priority, unearned specific callback.** The diner unconditionally recalls Tideglass sand, two ruined filters, a thrown canteen and a photograph. Tideglass Day is optional and concerns gathering/crafting Source Resin. I found no staged filter/canteen/photo moment in the inspected live story assets or rest text. Completing that quest alone would not establish the detailed anecdote. Shared Memory Span also names Tideglass unconditionally.

**Recommended correction:** Anchor the universal diner exchange in an experienced event, such as Zeke and Nova bracing each other during the cargo-pod crash, or the crew's first Astra descent. Keep the saucer, breakfast and repair invitation. A beach photograph can remain a new post-adventure memory if the scene clearly establishes that it was taken later. If the beach is intended to be a campaign payoff, author the actual earlier scene and gate its later reference on seeing it; that is a larger option and is not necessary for the first batch.

**Finding C3 — medium priority, chronology ambiguity.** `w6_mq28_build_bridge` calls “the Colony's first real morning” a memory shared before the finale. The epilogue uses that morning to establish the result of the finale. An ordinary earlier morning is possible, but the current phrasing strongly suggests the future payoff. Replace this one bridge ingredient with an already experienced moment; do not require players to infer Source time travel.

**Finding C4 — medium priority, agency clarity.** The bridge says Gh0st lowered his rifle “after the command ended”; Anchor Drill says the order is still in his head and he lowered it anyway. An unanswered command channel and an internalized order can coexist, but the callback obscures the important distinction. Recall the choice to lower the weapon while still carrying the order, consistent with the actual drill. Do not imply that silence automatically made him good.

Sources: [Memory Stair dialogue](../../../app/src/main/assets/dialogue.json), `w6_mq29_refuse_astra_revision_line_1` through `_4`; [room and completion prose](../../../app/src/main/assets/rooms.json), `source_memory_stair`; [cinematics](../../../app/src/main/assets/cinematics.json), `scene_launch_crash`, `scene_w2_astra_departure`, `scene_anchor_drill`, `scene_w6_epilogue_credits`; [events](../../../app/src/main/assets/events.json), `w6_mq28_build_bridge`, `w6_mq28_anchor_ghost`, `w2_sq03_gather`.

## 3. Thorne and Vale: strengthen the transfer of power

Their motives remain distinct in the implemented dialogue. Thorne sells access and treats people, hardware and authority as owned assets. Vale considers independent will an intolerable source of suffering. The problem is exposure and consequence, not a need to replace either motive.

| Stretch | Current dramatic function | Recommendation |
| --- | --- | --- |
| Worlds 1–2 | Dominion's violence has human consequences. The Warden has a concrete motive: one successful escape encourages others. Thorne has no named dialogue in these worlds. | Keep the Warden as the immediate antagonist. Connect one existing mandatory order to Thorne's policy or office so the player can later recognize who profits. No detached villain monologue. |
| World 3 vault | Thorne's freight suspension physically changes a display; she wants the Lens and protects paid Harmony access. Vale immediately extends the recovery authority into lethal control. | Keep the display and order. Establish one earlier consequence of Thorne's ownership before the vault, using an existing interaction. The empire should feel like hers before it is taken. |
| World 3 escape | Thorne opens a launch route when her countermand fails. | Make the self-interest explicit in a short line: getting the Lens away from Vale protects her leverage. Helping Nova should not read as an unexplained conversion. |
| World 4 flight | Vale invalidates her signature; seals change; Gh0st notices announcements stop naming her. | Strong escalation, already staged. Preserve it. |
| World 5 mainframe | Thorne admits signing the orders and supplies the firewall route. | Let Nova or Gh0st challenge what helping her will cost. Thorne can give verifiable access without earning forgiveness. |
| Throne Room | Thorne asks Vale to let a woman finish asking about her sister. Vale kills her and turns many voices into one. | Keep the small human limit. Her last act complicates her; it need not absolve her. Give the fall room to register in playback. |
| Memory Stair and Center | Vale offers Nova a personally attractive revision, then reveals the demand for control. | This is his strongest presence. Protect Nova's temptation and the refusal. Avoid expanding his speeches into an explanation of the theme. |

**Finding V1 — high priority, editorial.** Thorne's period of recognizable control is compressed. Her first named spoken sequence is already close to Vale overruling her. Two small links in existing required interactions—one early corporate order, one Spire consequence—would do more than additional late exposition. Attribute exploitation to her decisions, without making her personally issue every local order or making the Warden a puppet with no motive.

**Finding V2 — medium priority, editorial.** The Spire help and Ring admission move quickly from threat to useful assistance. Add a short negotiation or refusal inside those exchanges. Suggested Spire direction: she needs the Lens kept out of Vale's hands and still considers it hers. Suggested Ring direction: Gh0st demands the occupied-seat list before trusting the route; Thorne supplies evidence because her own access is gone. Keep her admission of responsibility.

**Finding V3 — high priority, presentation/prose.** The mandatory mainframe message explains Harmony through “phase corridors, chordprint locks, relay augments,” then declares what Thorne and Vale mean. The follow-up repeats a cage/faith/altar interpretation. These summaries compete with the characters' confrontation.

Replace the first with a concrete recovered record: named occupants, enforced retention, and who authorized it, drawing from established canon. Let a character react to one detail. After Thorne's dialogue, report the route or lock changing rather than summarize her moral position again. Keep the log's actual revelation and the task/reward sequence.

Sources: [dialogue](../../../app/src/main/assets/dialogue.json), `warden_boss_intro_1` through `_3`, `thorne_w3_lens_recovery_order`, `thorne_w3_mq14_trap_vale`, `vale_w4_mq20_coup` and continuation, `thorne_w5_mq22_intro` and continuation, `vale_w5_mq25_soloist` and continuation, `vale_w6_final_confrontation`; [events](../../../app/src/main/assets/events.json), `w5_mq22_access_mainframe`, `w5_mq22_find_thorne`.

## 4. Gh0st and Elara: let the relationship precede the rescue

**Finding E1 — high priority, editorial.** Elara is not wholly hidden behind an optional quest. The required Spire database task, `w3_mq12_interrogate_guard`, names her deleted transfer. Cold Case additionally identifies her as Gh0st's sister and follows the trail. The mandatory Phantom record later reveals her neural map. This is a real investigative spine.

The weaker part is knowing Elara as a person. The excellent line about her handwriting is milestone-dependent companion dialogue. At Cooling Springs the mandatory exchange moves from the Source Lock directly to giving Nova the override key. It gives little space to what the record means to Gh0st.

**Smallest change:** Bring one specific Elara memory into the mandatory Phantom/Springs sequence before the key. Use their established shared history, without inventing copied memories: something she did during their time together, what he recognized in her handwriting, or how she gave him time to answer. A short question from Nova lets him choose what to share. Retain the key as an act of trust and his instruction to wait until he is ready.

**Finding E2 — medium priority, editorial.** Elara's present agency is materially better than the old audit described: she unlocks the cradle, directs the rescue and helps maintain the route. Preserve this. Add one reciprocal personal beat at the reunion that recognizes the earlier memory. The aim is for “brother” to affirm a relationship the player recognizes, not serve as its only emotional definition.

Do not add another kidnapping, make her an imitation of Gh0st, or turn her into an exposition source. The rescue and the diner already confirm survival. A postgame preference or small decision would round her out, but it is lower priority than earning the reunion.

Sources: [Spire/Phantom events](../../../app/src/main/assets/events.json), `w3_mq12_interrogate_guard`, `w4_mq17_access_phantom_terminal`; [dialogue](../../../app/src/main/assets/dialogue.json), `w3_sq12_ghost_intro`, `ghost_w4_post_phantom_idle`, `gh0st_w4_springs_intro`, `gh0st_w4_springs_key`, `elara_w5_mq24_reunion`, `gh0st_w5_answer_elara`, `elara_w5_release_cradle`; [cinematics](../../../app/src/main/assets/cinematics.json), `scene_w5_anchor_stasis`, `scene_w5_cross_the_tear`, `scene_w6_final_note`.

## 5. Distinct voices and believable friction

The cast should have more than distinct vocabulary. The useful test is whether their different priorities change what happens in a scene.

| Character | Preserve | Next adjustment |
| --- | --- | --- |
| Nova | Mechanical fluency, practicality under fear, unwillingness to abandon someone; the admission that she wants Jed back. | Carry her impatience and uncertainty into a later decision. Let a crewmate's objection change one part of her plan. Avoid only giving her task instructions and final answers. |
| Jed | Maintenance as affection, the spare he kept, quiet insistence about danger. | Establish the exact recalled habits on the real route. No extra farewell speech. |
| Zeke | He understands systems, admits uncertainty and notices the cuff, seals and shaking hand. The checkpoint complicity is uncomfortable in a useful way. | Give one later exchange room for shame or disagreement without returning to paperwork as his whole identity. Let him be cared for even when he cannot solve the problem. |
| Orion | The painted ceiling, unfamiliar roots, asking for time, incomplete knowledge. | Balance his many listening/sound observations with judgment, curiosity or an ordinary preference. He should sometimes want something that delays the group, then choose what to do about it. |
| Gh0st | An offered hand, counting empty tanks, taking his own name before Rylos, sparing him. | Give the Elara relationship a witnessed detail. Increasing trust can make him more specific without making him chatty. |
| Elara | Instructions from inside the trap and active work on the locks. | A personal exchange that belongs to the siblings, plus a choice beyond maintaining equipment if space permits. |
| Thorne | Ownership, prices, signatures, commercial leverage. | More visible control before the coup; assistance with a price or motive. |
| Vale | Calm offers, fear of contingency, physically suppressing other voices. | Make his proposals understandable and locally attractive before revealing their cost. The Jed scene already models this. |

**Finding P1 — medium priority, editorial.** Much of the middle campaign has cooperative competence, but relatively few exchanges in which two sympathetic characters want different immediate actions. Avoid manufacturing constant hostility. At the Lens, Zeke's warning and Nova's urgency can produce a small agreed precaution. At Phantom, Gh0st can decide whether Nova reads with him. When using Thorne's route, the crew can demand proof rather than accept the next objective. Existing scenes can hold these changes.

**Finding P2 — medium priority, runtime delivery limitation.** Rest banter selects the first eligible party member in fixed order. In Sector 9, Zeke wins when he is present, hiding later Orion/Gh0st branches; in the Foundry and Ring, Zeke also wins; in the Spire, Gh0st precedes Orion; in the Source, Orion precedes Gh0st. Several otherwise useful lines are therefore poor evidence of normal full-party characterization. This is not proof that the characters have no other dialogue.

Consider a deterministic seen-line rotation for rest banter later, with eligibility based on party, world and milestones. It is a separate runtime change: no quest-critical setup should depend on resting or getting a particular rotation. Verify repeated rests and save/reload if implemented.

Source: [rest selection](../../../gameRuntime/src/main/kotlin/com/example/starborn/feature/exploration/viewmodel/ExplorationController.kt), `computeRestBanter`.

### Recurring NPC sample

- **Jax:** Credible terms for shelter: protect his customers and keep the stairs clear. The offer has a local motive. Preserve it.
- **Scrapper:** Risk avoidance, commerce and a boundary around trading the dead's names distinguish him from a generic contraband shop.
- **David:** Putting more food on when someone is overdue is excellent characterization. Do not replace it with an explicit statement that he cares.
- **Cameron:** Family, inherited gloves and the need to finish safely provide more than a material-request personality. Keep the personal references; avoid polishing them into the same voice as every other mechanic.
- **Charles:** Public departure records, privacy boundaries and willingness to admit Nova caught something he missed justify his help. The current motive works.
- **Pam:** Direct care and blunt humor feel distinct, although the initial “I'm Pam” to an addressed-by-name Nova deserves a small familiarity check.
- **Curator:** Status policing works. The sanitation/security insult is unusually theatrical; the later sleeve over the registry and paying-guest distinction feel more natural and threatening. Prefer that behavior over piling on more insults.
- **Rylan:** The press and Talia acknowledgment provide credible stakes. Some other advice sounds like equipment UI; keep precise system instruction in objectives where possible.

These are selective judgments, not a claim that every NPC branch is finished.

## 6. Let the world supply evidence

**Finding T1 — high priority around climaxes, broader cleanup later.** Several descriptions still speak like design notes. Examples in current rooms:

- `deep_throne_room_scale_06`: a halo “large enough to frame a boss fight.”
- `deep_throne_room_scale_02`, containment seam: “That is the coup in one strip of light.”
- `source_memory_stair`, completed inspections: “love edited into comfort is only another cage” and “choice only matters after the damage is real.”
- Memory Bridge actions prescribe each character's abstract lesson instead of describing the thing being remembered.

These are especially costly when adjacent dialogue has already earned the emotion. Replace them with observable conditions and consequences. For example, after Jed's refusal: “The latch is still beneath Nova's hand. She lets it go. The light under the door fades.” For an already completed revisit, use a settled state rather than replaying that immediate action.

The player's reason for refusing is that they love these particular people and will not let Vale own their choices. Avoid wording that accidentally makes suffering itself sound necessary or good.

Quest summaries need the same treatment. “Find Elara, prove she is not infrastructure” is an authorial aim; a player-facing goal can be to reach her and break the machinery keeping her captive. Preserve concrete objective instructions, especially around required interactions.

**Finding T2 — medium priority, comprehension.** The Final Note opens with five protocol signatures completing the Key's access pattern. The next personal line—someone asking for a door—is much easier to care about. Lead with the people still trapped after Vale's defeat, then show what the crew does to free them. Preserve the established five-signature rule without requiring a technical recap before the emotional consequence.

The existing illustrated ending and its timing should remain the baseline. More frames or longer duration would not fix these textual dependencies by themselves.

## Editing order and acceptance

### Batch 1 — continuity and earned recognition

1. Establish Jed's taps/lamp in the active workshop conversation.
2. Correct the cargo-pod/Astra refusal across scene, displayed control, room aftermath and walkthrough.
3. Make the diner callback valid on a route that skips Tideglass; remove the premature morning reference from the bridge.
4. Align Gh0st's bridge memory with his actual choice during the Anchor Drill.
5. Remove explanatory moral commentary from the immediate Memory Stair completion text.

Acceptance: The player can identify when each memory happened. The three refusals remain independent, leaving does not grant credit, revisiting does not replay rewards, and the route still works without Jed's Echo or Tideglass Day. No new quest, art or combat work is needed for this batch.

### Batch 2 — relationships and antagonist pressure

1. Give Thorne two earlier, concrete traces of authority using existing mandatory interactions.
2. Clarify her self-interested help at the Spire and the terms of cooperation on the Ring.
3. Give Gh0st one personal Elara detail in the mandatory Phantom/Springs sequence and pay it off at reunion.
4. Replace the Great Silence/Thorne summary messages with evidence and immediate consequences.
5. Give one or two existing crew decisions genuine resistance that changes the agreed action.

Acceptance: Without side quests, a player can say who Thorne is before the coup, why she helps, why Vale is different, and why Elara matters to Gh0st. Preserve Thorne's culpability, Elara's agency and Gh0st's independent identity. Do not require additional fights or change the heist/flight order.

### Batch 3 — optional scenes and surrounding prose

1. Rebuild Jed's Echo as a distinct remembered lesson rather than an earlier version of the door temptation.
2. Clean nearby room/action/quest prose that explains the theme or exposes design terminology.
3. Evaluate rest-banter rotation separately; sample NPC branches for familiarity, motives and overexplaining.
4. Simplify the finale's technical opening while retaining the crew circuit and outcome.

Acceptance: Optional content deepens a complete main story. Characterization remains recognizable without repeated verbal tics. Changes to rest selection do not add story requirements or repeat sensitive scenes.

### Validation after edits

- Check changed dialogue continuations, triggers and conditions; use existing dialogue/event and narrative regression tests for the affected routes.
- Exercise a main-story route with the relevant optional content skipped and a completionist route with it present. A complete reading of one does not establish the other.
- Replay the workshop, lift, cargo-pod crash, actual Astra departure, Phantom/Springs, Thorne exchanges, reunion, Memory Stair and ending in that order.
- Check that quest/item feedback does not obscure the emotional holds. Source action order alone is not proof of visible overlap.
- Verify that old saves retain their quest state, especially if any displayed action name changes. Keep stable internal identities unless a tested migration is necessary.
- Judge successful setup by what the player can recall, not by whether a keyword exists somewhere in a JSON catalog.

No need to commission additional cinematics before this pass. The approved presentation can carry much stronger emotion once these dependencies are sound.
