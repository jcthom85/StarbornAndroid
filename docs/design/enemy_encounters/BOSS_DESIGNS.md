# Boss encounter designs

Implementation update (November 13): the World 1 Warden and World 2 mechanics are implemented locally with temporary art. See [current progress](PROGRESS.md); other redesigns and final art remain proposed. The original design below remains the reference.
Status: proposed behavior, not implemented. This file defines the complete enemy side of each encounter. Exact HP, damage, timers and thresholds require earned-route testing. Thresholds below are initial prototype choices, not accepted balance numbers.

## Shared contracts

- Display the encounter's targets, current stage and any finite reserves. A targetable component has its own name, HP, hit feedback and destroyed appearance.
- Targetable does not imply an independent full-speed ATB actor. Linked components may be controlled by one chassis schedule.
- At least one accessible response must exist to each dangerous tell: interrupt, break, guard, remove a support or exploit recovery. Do not demand one optional talent.
- A phase changes the decision or the available attacks. A silent stat boost is insufficient. Do not clear player debuffs on the boss unless an explicit, limited transformation calls for it.
- Added enemies and components draw from one encounter reward budget. Parts, reserves and resummons cannot create independent farming rewards.
- State whether the body, all opponents, or a special disable condition determines victory. Cancel dead parts' queued actions, but show the final hit/death response before the transition.

## W1 — Iron Warden: discipline under strain

**Complete formation:** Iron Warden alone. One large sprite; no independent drones or parts. The existing Drone Support Protocol should be presented as an off-screen support strike if retained, not as a promise of a targetable deployment.

**Identity:** a human authority figure using disciplined armor and crowd-control equipment. The fight is the opening chapter's readable solo test.

**Initial state:** riot shield up, obvious bracing posture. The first heavy attack is preceded by a named lock-on/stance. The player sees one full warning/recovery sequence before the Warden combines pressure types.

**Pattern:** shield brace → committed burst or gas throw → recovery. Shield posture has an alternative basic-pressure route; optional Guard Break training and elemental tools shorten it. Gas is bounded pressure, not a permanent screen-wide denial state.

**Change at approximately 40% body HP:** Emergency Overclock once. The shield visibly drops or cracks, offense becomes more urgent, and recovery remains. The increased exposure compensates for the higher pressure. No additional full health bar.

**Targeting and victory:** body death ends the battle. No fake limb targets. Preserve `the_iron_warden`, the keycard, the Pod Bay lock and both quest/listener paths that record its defeat.

**Art:** retain the dedicated Warden candidate if it passes the unique-silhouette review. Add a clear damaged-shield/overclock overlay or pose; another enemy must not use this body.

**Acceptance:** solo Nova with mandatory gear, optional drill skipped, ordinary recovery stock, interrupt and direct-pressure approaches. The first tell must be legible on a phone. Overclock must not immediately take another turn and kill a player who just earned the threshold.

## W2 — Source Beast: hunt becomes frenzy

**Complete formation:** one large Source Beast, no minions and no targetable body parts. Its changing posture and animation sell the size.

**Identity:** a living apex creature with hunting behavior. Its fight should feel physically different from armored Warden discipline.

**Hunt pattern:** look/mark prey → committed claw or pounce → short exposed recovery. Roar announces the next heavy commitment rather than adding an unavoidable disable on top of it. Any focus on one crew member is bounded and visibly marked.

**Frenzy around 50% HP:** the stance lowers and movements become broader. A two-action sequence is explicitly shown as a sequence; its final overextension grants a longer recovery. Damage tuning belongs to the whole sequence. Remove some defensive behavior so attacking during the opening is rewarding.

**Counterplay:** Guard, basic damage during recovery and available Break/elemental options. No companion-only rescue paradox and no mandatory rare consumable. The player chooses whether to protect marked prey or interrupt the creature.

**Victory and story:** retain the player-selected `Face the Beast` action and `the_beast` victory contract at Canopy Ridge. Preserve Gh0st's failed trigger, the earned Anchor drill and the guaranteed Source Resin. A phase transition is not the trial's completion.

**Art:** one imposing organic silhouette with two readable poses and a wounded/frenzy treatment. The transformation is behavioral, not a second unrelated creature.

**Acceptance:** demonstrate two viable responses to pounce; test an unoptimized arrival party. Frenzy must change play without becoming a repeated all-party burst race.

## W3 — Administrator: command and limited reserves

**Complete formation:** Administrator plus one starting Aero Drone. Reserve pool: three Interdictor Drones. Maximum two living escorts, and no more than three reserve deployments in the whole battle. The starting Aero Drone counts toward the initial encounter pressure, not the three additional reserve units.

**Identity:** the city tries to manage the party as a service failure. The central intelligence is physically large and its escorts are discrete, targetable enemies.

**Command cycle:** announce either Laser Lattice or a deployment. A deployment takes a full readable preparation window. Breaking the Administrator during preparation cancels that attempt without silently spending the finite reserve. After cancellation it must perform a recovery action before attempting again.

**Escorts:** Aero Drone introduces the known disruption role. Interdictors have a fragile emitter and one announced interception attack, then recover. An escort's hard-control effect cannot line up with an unanswerable boss heavy attack.

**Player choices:** clear escorts for space; interrupt a deployment; or pressure the body during a command recovery. Killing the boss is always a valid win condition, so a strong party can skip unused reserves.

**Late behavior:** at low body HP or exhausted reserves, command chatter ends and the lattice becomes more predictable and exposed. No infinite replenishment and no surprise fresh army on the last sliver of HP.

**Victory:** body death cancels remaining commands and ends the encounter. Escorts show shutdown/withdrawal; they do not become a cleanup tax. One budget preserves Administrator rewards/AP and the `defeat_administrator` event. The preceding Aero Drone/Heavy Mech security fight stays a separate, short prelude with a deliberate opportunity to recover or withdraw.

**Art:** a large suspended concierge intelligence, the existing distinct Aero Drone and a new clamp-shaped Interdictor. Deployment entrance and shutdown cues need sound/animation; they must not appear as unexplained extra portraits.

**Acceptance:** body-first, escort-first and interrupt approaches; all deployment caps; duplicate instance selection; reserve persistence; no extra loot on retry. Revisit the player's earlier hard rooftop experience with actual arrival equipment.

## W4 — Titan Walker: four systems, one machine

**Complete formation:** five targetable sprites: Titan chassis, Missile Battery, Cutting Arm, Anchor Cannon and Shield Projector. The four systems have independent HP and target selectors. They are assembled into one machine, not five unrelated enemies in a row.

| System | Threat and tell | Permanent consequence of destruction |
| --- | --- | --- |
| Missile Battery | Pod doors open before a queued area salvo. | Removes the salvo from the chassis's available actions. |
| Cutting Arm | Saw extends and identifies its intended victim. | Removes the heavy cutting strike. |
| Anchor Cannon | A visible tether mark precedes a control shot. | Removes this source of restraint/disruption. |
| Shield Projector | Emitter aligns before a bounded defensive pulse. | Removes the chassis protection pulse; core remains damageable even while it exists. |

**Action budget:** the chassis owns the attack clock and chooses one working weapon action at a time. Components are targetable linked actors with no autonomous full attack stream. They can show the pending action and receive control/break effects. A destroyed or disabled component cannot fire its queued action.

**Decision:** remove the threat most troublesome to the party or spend damage on the chassis through its defenses. Every removal improves that specific situation. The player is not secretly punished with a stronger replacement weapon.

**Heat/recovery:** a committed salvo or cut leads to the established exposed-vent opening. The new architecture must preserve that rhythm rather than bury it beneath part management.

**Emergency mode near 40% chassis HP:** visibly redistribute power among surviving systems. Destroyed pieces stay destroyed. The chassis gets a limited stomp fallback; it does not regenerate weapons. The machine still has only one heavy action per cycle and retains recovery.

**All weapons destroyed:** the chassis is exposed and can use its weaker fallback. The encounter cannot become an inert softlock or require targeting a vanished part. Chassis death ends it even if weapons survive.

**Victory and rewards:** `titan_walker_boss` remains the core story identity; preserve Meltdown progression and the north escape gate. Weapons grant no independent XP, AP, drops or quest victories. Final reward remains one encounter budget, with existing guaranteed Power Cells accounted for.

**Art assembly:** one shared perspective, lighting and pixel scale; five separately rendered subjects with agreed attachment anchors. Provide intact, damaged and destroyed socket treatments. Avoid a base image with a second copy of each detachable weapon baked into it. Part labels and hit areas must remain legible on Pixel 8a; use an accessible target list if the geometry is crowded.

**Acceptance:** all 24 part-destruction orders, body-first victory, all parts first, interrupted pending shot, repeated same-type targeting, area attacks, disabled parts, KO allies, resume mid-fight, and Android/desktop layouts. Test a five-target proof before commissioning final pieces.

## W4 optional — Crucible Champion: deliberate solo duel

**Complete formation:** Champion alone in round three. Rounds one and two retain the distinct support/charge and Golem/repair formations. Rest between rounds stays possible.

**Pattern:** a short posture feint has low damage, followed by a clearly committed heavy strike and a reliable punish window. Feints cannot imitate the complete heavy tell so closely that the encounter becomes guessing. One visible late heat change shortens a recovery but adds no fresh body or parts.

**Purpose:** focused mastery and a memorable optional silhouette, distinct from Titan's dismantling. Do not just give the Champion Titan's stats and a smaller sprite.

**Contracts:** preserve all three round milestones, the final completion control and quest rewards. Round-three listeners currently name both Champion and Magma Drone under any-member matching. Reauthor them around the Champion encounter completion rather than retaining a nonexistent drone requirement. Rebudget the removed drone's optional rewards explicitly.

**Art:** crucible-alloy prizefighter, broad hammer fists, low stance, visible old repair scars. No Heavy Loader reuse.

## W5 — Compliance Avatar and Directive Conductor: break the agreement

**Complete formation:** one large Avatar body and one separate Directive Conductor. Both are targetable. There are no reserve replacements. The Conductor is an independent partner, not a detachable piece of the body.

**Linked state:** the Conductor announces a directive; the Avatar visibly prepares its execution. The directive may improve defense or designate a target, but cannot immediately combine an unavoidable disable with lethal damage. Only one heavy attack is scheduled in the shared cycle.

**Choices:** interrupt the fragile command preparation, destroy the Conductor to end coordination, or attack the Avatar through limited protection during its openings. The body is never absolutely immune while the partner lives.

**Conductor destroyed:** the link visibly snaps. The Avatar loses coordinated protection and designation permanently. It enters a simpler, more exposed purge rhythm with a clearly shown recalibration turn. Do not punish the player by secretly increasing total damage above the coordinated state.

**Avatar reaches approximately 50% HP with Conductor alive:** it announces Purge and temporarily exposes the link. Breaking either preparation cancels the joint action. If the player persists with body-first, this is a viable harder route, not a requirement to erase their prior damage and kill the partner first.

**Victory:** Avatar body death ends the encounter and collapses the Conductor. The Conductor cannot independently complete Critical Mass. Preserve `compliance_avatar`, its quest stage/defeat milestone, AP and existing reward entitlement once.

**Duplicate removal:** Boardroom Grave gets the distinct optional Boardroom Bailiff elite. It must never emit `compliance_avatar` victory credit or show the main boss's art/name.

**Art:** a ceremonial hard-light body and a floating angular halo of command tiles, with a legible linking effect. The link needs a broken state readable without relying on color.

**Acceptance:** body-first and partner-first wins with ordinary equipment; no repeat-target burst lock; no inherited instant attacks at a mode change; old main-boss completion and side-room victory receipts do not award a second reward.

## W6 — Vale: control, fracture, separate voices

**New-play sequence:** a staged confrontation in The Center. Human Soloist, transformed Silence and a short closing commitment belong to one dramatic encounter sequence. Walking through White Shore, Key Orbit and Silence Pulpit is no longer required between forms. Those rooms retain exploration and their existing discoveries; they are not deleted.

### Stage 1 — Soloist

One human Vale sprite, alone. He announces a precise command, executes it, then gives an opening. Silence pressure is bounded; basic attacks, Guard and recovery remain usable. Player-applied debuffs and chosen builds remain useful. His low-health change combines already learned commands in a visible sequence, not several unannounced free turns.

Defeating the human form commits the existing `ascended_vale` story stage once. Show a transformation interlude rather than a full celebration and return to exploration.

### Stage 2 — Silence

One enormous transformed body plus **Voice Anchor** and **Memory Anchor**, each independently targetable with distinct art and function. Maximum three targets. The whole formation uses one boss command schedule.

- Voice Anchor supplies the charged Silence command; destroying it removes that command source.
- Memory Anchor supplies the charged memory-pressure pulse; destroying it removes that source.
- Both anchors are optional targets. The body remains vulnerable during declared openings; either removal makes survival simpler.
- Destroyed manifestations do not regrow. No summons, no four-weapon Titan replay and no independent part rewards.
- Alternate the charged anchor action with body recovery. Interrupt/Break and Guard provide mandatory-tool answers; an optional Source build is not the sole solution.

### Closing commitment

At roughly the last 15% of body HP, enter one clearly announced Great Silence commitment and one generous exposed response window. No third full HP bar, no new minions and no long repetition of earlier phases. If the commitment repeats because the party is weak, retain the readable recovery; do not impose an invisible DPS deadline.

Body death ends combat after the final hit and collapse. The existing shared tuning focus remains the separate story resolution; the battle does not silently complete it or replace the game's ending choice with a combat menu.

### State and continuity

- Commit human-form and transformed-form quest victories at their actual boundaries, exactly once.
- Preserve the combined existing XP/AP entitlements with separate durable stage receipts. Repeated stage-two retries cannot pay stage one again.
- An explicit stage checkpoint may save/pause after transformation. It preserves HP, KO state, items and progression. Do not accidentally revive KO crew through room-return persistence, reset cooldowns invisibly or give an unannounced full heal. Earned level-up behavior must be inspected before finalizing this contract.
- On defeat, offer a clear retry from a recorded stage checkpoint or an earlier save. Snapshot the checkpoint's consumables and enemy state consistently so neither depletion traps nor item duplication are introduced.
- An old save with `defeat_vale` completed can resume only the remaining form. An old save at Silence Pulpit needs a visible resume path there; it must not require replaying stage one or lose access to the ending. Keep legacy victory recognition scoped to the right stage and arena.

**Art:** calm human Vale; one unmistakably different fractured-porcelain transformation; two manifestations with vertical versus horizontal silhouettes. Transformation, command cancellation and anchor destruction require visible effects and sound. Generate the assembled concept before individual final assets.

**Acceptance:** stage-boundary saves, legacy partial wins, KO persistence, alternate builds without one favored damage-over-time skill, core-first and anchor-first approaches, long but finite defensive play, reward idempotence and restoration of the postbattle tuning route. This is the highest integration-risk encounter and ships last.
