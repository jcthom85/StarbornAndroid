# Starborn dialogue, narrative, quests, and game text

## Decision

Start with a focused World 1 interaction and quest pass, then carry the same feedback rules into World 2. Develop the Spire and Ring antagonist scenes as a separate writing pass. The opening needs a clear route through its mechanics. The middle and late campaign need more time for characters to make consequential decisions on screen.

This is a read-only game assessment of the current local checkout. It includes the later World 6 dialogue and illustrated ending work. Evidence comes from the two prior reports, live assets, the Android and desktop entry points, and focused automated runs. I did not perform a blind device playthrough or verify every NPC and interaction state.

## Findings ranked for implementation

| Category | Finding | What to do |
| --- | --- | --- |
| Progress Blockers | The fresh World 1 automated route currently fails to complete Wake Up Call after the live cutter test. The focused opening discoverability tests pass, and the earned World 2 route reaches World 3. | Reproduce the cutter sequence in the Android client. Check task flags, cinematic start and completion, and Journal handoff before changing the route. The automated failure identifies the symptom. Its cause and player reproduction are still open. |
| Progress Blockers | Android can give no visible response when an event-backed World 1 or 2 interaction has no eligible handler. This makes a reachable task appear stuck. Some gates already have their own early feedback. | Pair runtime fallback changes with a reviewed set of authored messages. Test an early tap, successful tap, repeat tap, and reload. |
| Critical Bugs | The Shared Showers drainage grate says the trap is empty while scalding steam still blocks it. The post-Warden Pod Bay variants omit the exact names cargo rails and escape pod. | Give the grate a useful prerequisite hint. Restore deliberate inline references where they still serve a purpose. Pod Core remains reachable through navigation, so the missing inline references alone are not a confirmed softlock. |
| Critical Bugs | Several finished controls still look ready. A generic rule based on any completed handler can retire a control prematurely when the same trigger also has feedback or repeatable phases. | Use explicit success receipts for multi-handler controls. Retire the terminal or other multi-use object only after its last meaningful use. Audit any existing opt-in that resolves a completion receipt by trigger. |
| Critical Bugs | Some response menus show two exits. The bulkhead exit leads to a closing line, which the current heuristic fails to recognize. | Add explicit authored exit metadata that supports a closing line. Convert genuinely linear acknowledgments to a normal advance. Keep real turn-in decisions and puzzle decisions escapable. |
| Important Considerations | Heavy Lifting is filed as a side quest while Boggs requires the certification. Its optional loader and rail tasks sit in a stage the main route advances past. The loader action itself advances that stage too. | Keep the useful Guard Break lesson as an explicitly required certification beat. Make the cargo practice independently visible and optional, preserving IDs and old save progress. |
| Important Considerations | VHS Tape 2 is live in the discarded document locker in Zeke's Booth. The World 1 guide still sends players to Jed's Office. | Update the local completionist route and room index. Keep the authored pickup location. |
| Important Considerations | Thorne's execution and the Tear opening are compressed into one message immediately after one spoken Vale line. | Stage the execution and rupture as a proper exchange and visible change in the room or an illustrated insert. Preserve the fight and Tear gates. |

## Verification of the previous reports

**Gemini is right about the symptoms and too broad about the causes.** A room action names a player-action trigger. Look up all events by that trigger and evaluate their conditions before calling an action silent. The Core Hub terminal and thaw control, the medbay vents, and the Pod Core navigation console already have authored wrong-order feedback. The Core Hub's output ports are a separate passive clue with no quest mutation. The early tap can reveal useful information without thawing the console.

**Claude's review is mostly sound, with these corrections and updates.**

- Wayfarer completion and its reward now fire on wayfarer_quest_conclude, after Nova's personal thanks. The added exit can interrupt the scene, but the favor can be retried while the completion milestone is absent and the player still has the components. I found no current permanent loss from that exit.
- Vale explicitly connects variance with pain and calls for one voice in World 5. World 6 develops what that means for the mine and the crew. The narrative gap is the gap between his early orders and the later explanation. His first override shows him taking the guns, and his coup shows him taking command. Neither gives the player much reason yet to suspect what he intends to do with Harmony.
- The conditional-skip trigger is real. I found the same two current targets Claude identified, both in Charles's favor. Their incoming choices have identical guards. Add a data check if we start authoring more of this pattern. The evidence does not justify a global runtime behavior change now.
- The alternate Boggs introduction does not explicitly advance the drill stage. Its registration event requires a prior drill victory. The main-route review explicitly advances the stage, and using the loader can also advance it. Treat the optional tasks' presentation and ordering as separate issues.
- The three suspected unused entry lines still have no direct reference in the game assets. A missing ID reference alone cannot prove a dialogue line is unreachable. NPC dialogue also resolves by speaker, available conditions, and entry priority. The lounge shop line has an explicit room reference.
- Director Sorrell is the current displayed name in the Skyline Salon. The old expose_vance task ID remains. No authored task update for it was found. Quest completion and explicit task credit are separate state, so check what the completed Journal shows before claiming the task is backfilled.
- The Center's post-Silence and completed descriptions already name shared tuning focus. The claimed need to tap the Key and then the focus a second time does not describe the current path. The old return to the fracture shortcut is authored but is absent from the checked prose. The south walking route provides a way back to The Center. Desktop exposes a separate Actions menu, while I found no equivalent Android opener for this room.

The previous two audits quote an earlier finale. The live local version has a longer Vale conversation, distinct memory refusals, and an illustrated two-part ending. Those should be reviewed in playback on their own merits.

## Independent assessment by world

| World | What works | Highest-value change |
| --- | --- | --- |
| 1. Colony and Deep Mine | Jed gives the opening a personal stake, and the repair and clearance problems give the player things to do. | Make each tool, certification requirement, and handoff legible in the Journal and the room. Verify the cutter handoff before adding lore. |
| 2. Sector 9 | The environment and Orion's discovery give the Chime and cold open a payoff. | Check variant prose at the point each action unlocks. Make first boarding and departure language agree about where the player goes and what they do next. |
| 3. Spire | The heist gives the crew distinct practical roles. Thorne visibly enforces the Lens recovery order. | Give Vale a small early action or line that hints at a world with no dissent, while keeping the exact plan and takeover ahead. Preserve the distinction between recovery authority here and the full coup in World 4. |
| 4. Foundry | Project Phantom makes the crew confront what the factory produces. Vale's coup gives the departure a change in power. | Make records, Gh0st's reaction and the coup build toward one another. Make the coup audible and visible as its own beat. Check Anvil cradle wording by state before treating every absent phrase as a bug. |
| 5. Ring | Thorne's admission and Elara's reunion give the abstract system a human cost. | Make Thorne's evidence and assistance easy to encounter. Give her and Vale a consequential exchange before the execution. The mandatory route should establish enough without requiring a return visit to the Mainframe Nave. |
| 6. Source | The playable refusals now give Nova, Orion and Gh0st concrete experiences that can carry the answer to Vale. | Review the illustrated ending and preceding scenes in sequence. Trim the places where captions explain an action the art already shows. Give the player's earlier quest decisions visible consequences during the return where the saved outcomes support them. |

## Text and quest standards for the next pass

For each required interaction, answer three questions in its current game state. What can the player do now? What tells them that? What confirms that it worked? Match the Journal's destination and verb to the exact room and action labels. Write unavailable messages from the player's present situation. Make completed text acknowledge the change. Keep repeatable services and real alternate routes visibly available.

Use a short state matrix for each interaction the first chapter touches. Include before quest, unmet prerequisite, ready, success, completion, reload, and any alternate route. For NPCs, include the current greeting, intended close or next step, and post-quest presence. Verify the actual UI path as well as event execution. Restore a passive label only when it serves a player purpose.

For narrative scenes, write the action that changes the situation first. Let dialogue respond to it. Use a character's vocabulary and relationships. Use brackets or narration for physical action, and reserve room prose for what the player can observe. The Spire order, Foundry coup, Ring execution and Source confrontation should read as one escalating chain.

## First implementation slice

1. Reproduce and isolate the fresh World 1 cutter failure. Check whether it is a test setup issue or a player-facing progression defect.
2. Fix feedback and completion states for the opening workshop, Shared Showers, Pod Bay, and the affected Core Hub and medbay controls. Preserve existing wrong-order handlers.
3. Clarify Heavy Lifting's required certification and optional cargo practice. Update VHS Tape 2 and the affected steps in the walkthrough.
4. Resolve linear and duplicate-exit cases with explicit response intent. Check the Wayfarer reward timing and revisit behavior.
5. Verify a fresh and resumed World 1 run, including the actual Android action references and a completionist route. Then apply the proven pattern to World 2.

## Automated evidence

The five WorldOneDiscoverabilityStateTest checks passed. The earned World 2 main route from the launch checkpoint passed. The fresh World 1 main-route test failed at the Wake Up Call completion assertion immediately after live cutter test. That test uses the production public interaction harness. It warrants reproduction and triage before labeling it a confirmed device blocker.

Relevant source files are app/src/main/assets/rooms.json, events.json, dialogue.json and quests.json, plus ExplorationController.kt, DialogueService.kt, RoomPresentation.kt, EventManager.kt, and the completionist walkthroughs. Test output for this run is in /tmp/starborn-dialogue-assessment.log.
