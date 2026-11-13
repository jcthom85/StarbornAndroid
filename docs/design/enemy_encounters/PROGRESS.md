# Implementation progress — November 13, 2026

The shared behavior prototype, conservative World 1 Warden change and World 2 mechanics pass are implemented locally. World 3 now has its authored squads, rooftop sequence, and patrol. Worlds 4–6 and final artwork remain proposed. Moss Borer and Mist Glider have separate definitions and temporary existing sprites until their art batch.

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

## Verified / limits

See the [verification report](../../testing/AUTHORED_COMBAT_PROTOTYPE_AND_WORLD1_2026-11-12.md) for test scope and evidence. Earned launch-checkpoint progression and World 2 save/reload are verified; a fresh full World 1 playthrough is still required. This is not a balance or final-art approval for all six worlds.

Both World 3 completionist branches and a main-story run reached the Foundry from earned World 2 saves. The main-story run used a second rooftop combat seed and skipped the rooftop supply purchase. The routes verified roof encounter order, quest rewards, and defeated patrol persistence after reload. A separate runtime test exercised all three Administrator reserves and escort shutdown on victory. The Android emulator test verified the patrol countdown appears before automatic combat.

Combat uses the existing battle-entry restart checkpoint after reopening; exact in-progress warning/component/reserve restoration is not newly implemented. The new state itself is serializable and tested, but that is not equivalent to a complete midbattle persistence feature.

The baseline is retained as historical evidence. SOURCE_RECONCILIATION.json records intentional canonical asset changes by exact old/new hash; future unreviewed asset changes still fail validation. The newly tracked status catalog has a separate supplemental baseline.

## Next

Playtest the rooftop sequence and the assassin patrol on a phone before the World 4 Titan pass. The automated campaign routes exercise known solutions and earned equipment. Final enemy art follows the encounter and layout reviews.
