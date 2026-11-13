# Implementation progress — November 13, 2026

The campaign now has authored combat through the Foundry's first mechanics pass. The Titan Walker has original artwork. Worlds 5–6 and the remaining art batches are planned. Moss Borer and Mist Glider use temporary sprites.

## Implemented

- Opt-in authored action cycles, real warning/response turns, one-time HP phases and recovery.
- A five-target Titan laboratory encounter: one body clock, four independently destructible weapon sources, immediate interruption when their source is disabled, and one encounter payout.
- A finite reinforcement laboratory encounter: actual targetable enemies, live escort cap, exhausted reserve budget, body-controlled victory and no extra spawn rewards.
- Persistent attack/target warnings and independently readable multipart target controls in Android and desktop combat.
- A basic Guard action on both platforms, without an optional skill prerequisite.
- The existing World 1 solo Warden now has shield, prepared offense, recovery and a shield-down overclock phase. Existing stats, rewards, formation and story gates remain.
- Two isolated Android Test Session entries for the laboratory encounters. Temporary art is intentional; neither laboratory formation is connected to a campaign room.

## World 2 — implemented November 13

- Same 11 encounter opportunities, with nine world-specific identities: Moss Borer and Mist Glider replace the two borrowed Pit identities in their Sector 9 rooms only.
- Staggered opening cycles for hound/borer pairs, a damage-interruptible thorn coil, bounded plant shelter and healing, and prepared guardian/orb attacks with recovery.
- The Acoustics Lab sentry/plant remix uses reduced instances and the previous single-plant XP/credit/drop budget.
- Solo Source Beast: marked hunting lunge, one half-HP frenzy transition, two separately warned strikes and exposed recovery. Existing stats, rewards, resin, player-selected confrontation, Gh0st recruitment and Anchor drill remain.
- Old cleared-room receipts and interrupted-battle victories retire the corresponding changed formations. Same-room save restoration now refreshes the enemy display.
- Local World 2 walkthrough updated without renumbering or publication.

See the [World 2 verification report](../../testing/WORLD2_AUTHORED_COMBAT_2026-11-13.md).

## Targeting UI follow-up — November 13

The expanding warning card is replaced by compact cues in existing message space, plus static threatened-portrait markers. Warning/phase changes no longer reflow the formation. See [the presentation report](../../testing/COMBAT_TARGET_CUES_2026-11-13.md). The Pixel still needs this follow-up installed.

## World 3 — The Spire

- The nine encounter opportunities now include a moving assassin between Shield Sightline and Drone Wreck Roof. Its victory pays the former stationary encounter’s ration drop. Saved victories carry over to the new formation.
- Archive Vault and Fuel Bridge add an interruptible shield relay. Service Dumbwaiter adds a Riot Guard without raising the encounter’s payout. The sewer pair alternates its feeding and acid preparations.
- The roof exposes the Administrator after the Heavy Mech and Aero Drone fall. The west exit stays available between fights. The Administrator can deploy three Interdictors, with at most two living escorts. Breaking its preparation interrupts deployment. Below 35% HP it switches to exposed, single-target emergency fire.
- The attack cue now centers the move and target together. The portrait brackets identify the threatened crew member, and the Root Reserves counter is removed.
- The Administrator now has original porcelain-and-brass concierge art generated through the OpenAI Image API. When its party is the sole visible encounter, exploration centers the boss and positions its Aero Drone escort to one side.
- Boss silhouettes can extend beyond their exploration frames while room text and travel controls retain drawing and tap priority. The Administrator sits slightly lower so its face clears the map control. Combat status cards draw above oversized sprites; the Titan's formation rises to the card edge on Android and desktop.

## World 4 — The Foundry

- Titan Dock still tracks one boss victory. Combat adds a Missile Battery, Cutting Arm, Anchor Cannon and Shield Projector, each with independent HP. The chassis shares one action clock with all four systems. Destroying the source cancels a pending attack and the chassis skips that weapon in future turns.
- The Titan uses an assembled portrait in exploration and five original sprites in battle. When a system is destroyed, the combat sprite disappears to expose its chassis mount.
- After heavy attacks the Titan exposes its vents. Emergency Power begins below 40% chassis HP and introduces the warned Hydraulic Stomp. The stomp and vent cycle also keeps a fully disarmed chassis active.
- Slag River's drone shows a marked Heat Burst followed by venting. Slag Golem and Flame Trooper warn before their heavy attacks. On Conveyor Belt, the Welder Bot's limited repairs can be interrupted by damaging it during the charge.
- An earned route verified the north gate, one victory receipt, save and reload, and the hardware checks before departure to World 5. Review saves 18–20 are exported by that route.

## Verified / limits

See the [verification report](../../testing/AUTHORED_COMBAT_PROTOTYPE_AND_WORLD1_2026-11-12.md) for test scope and evidence. Earned launch-checkpoint progression and World 2 save/reload are verified. A fresh full World 1 playthrough remains open.

Both World 3 completionist branches and a main-story run reached the Foundry from earned World 2 saves. The main-story run used a second rooftop combat seed and skipped the rooftop supply purchase. The routes verified roof encounter order, quest rewards, and defeated patrol persistence after reload. A separate runtime test exercised all three Administrator reserves and escort shutdown on victory. The Android emulator test verified the patrol countdown appears before automatic combat.

Combat restarts from battle entry when reopened. The source reconciliation records the current asset hashes alongside the historical baseline.

## Next

Playtest the Slag River, Conveyor Belt and Titan Dock checkpoints on a phone. Compare a chassis rush with dismantling the Titan and check whether the sprite arrangement and target names are easy to read. The wider Foundry roster and route redesign remains proposed.
