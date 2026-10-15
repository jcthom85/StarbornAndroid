# Starborn: lighter Source language and stronger puzzle identity

Date: October 14, 2026
Status: Active documentation/editorial direction; puzzle replacements remain proposals. The documentation foundation has been rewritten in the cleanup pass; game content and puzzle logic have not been changed by this plan.

Implementation update, October 14: documentation cleanup is underway; see [the cleanup record](../DOCUMENTATION_CLEANUP.md). The owner clarified that most documents predate the implemented game. Current code/assets and live routes are the default baseline; discrepancies normally require a documentation correction. Prose and puzzle redesign proposals below do not authorize changing working logic merely to restore an old specification.

## Creative direction

Starborn is an adventure about workers, fugitives, a small crew, and dangerous technology. The Source supplies mystery and unusual physical experiences. Sound is one way people encounter or interpret it. Musical imagery should gain significance from occasional, well-chosen use.

The player should readily understand what physically happened, what someone wants, what is dangerous, and what they can do next. Characters can disagree about what the Source means. The game does not need a musical explanation for every machine, feeling, relationship, power, or puzzle.

Preserve the established causal story: Nova's practical choices awaken the Fork; the encounter changes her; the Chime authenticates rather than powers; the Bridge connects distinct willing minds; Gh0st chooses his allegiance; Source use has bodily costs; Nova's final sacrifice preserves her identity. Reducing metaphor does not require undoing those repairs.

## Why the documents need substantial work

The current Writer's Handbook already asks for plain speech and occasional musical language. Other authoring inputs encourage the opposite:

- `data/assistant_briefing.md` declares the universe acoustic and reality a Song. It frames magic, modifications, food, and colors musically, including mapping chords to colors.
- `Lore_Bible.md` describes ancient artifacts as mixing decks for reality and treats acoustics as a comprehensive engineering model.
- World/hub documents repeat their own explanations and proposed scripts, so a narrow prose correction can be overwritten by an older local brief.
- Historical migrations and audits carry imperative instructions for older tasks. They remain useful history but should not be mistaken for current creative direction.
- `_Structure_Guide.md` does not reflect the newer full authority hierarchy and contains an ID convention that differs from much of the implemented content.
- `Systems_Overview.md` mixes implementation facts, historical layout descriptions, and future plans.

There are 98 Markdown documents under `docs/story` outside archive directories in the inventory reviewed. That count alone is not a problem. Conflicting authority, repeated specifications, and unclear implementation status are the problem. Do not rewrite all 98 indiscriminately.

## Document disposition

| Material | Treatment | Reason |
|---|---|---|
| `data/assistant_briefing.md` | Replace heavily | It is a likely route for reintroducing pervasive acoustic framing. Make it a short index and accurate project orientation, with links to current authorities. |
| `Starborn_Master_Story.md` | Focused structural edit | Keep plot causality and the six-world journey. Reduce cosmic terminology in premise and summaries; separate committed story from old room/playtime targets. |
| `Lore_Bible.md` | Heavy rewrite and contraction | Preserve necessary facts and limits. Separate observed effects, institutional theories, and unresolved questions. Remove decorative pseudo-physics and repeated explanations. |
| `Starborn_Writer_Handbook.md` and `Content_Standards.md` | Consolidate overlapping guidance | Retain plain speech and concrete action. Add a few approved examples. Give each document a distinct purpose or merge them. |
| `Characters.md`, `Character_Arcs.md`, emotional/conflict maps | Reconcile and simplify | One profile per character, behavioral progression, relationship tensions. Keep planning shorthand out of scripts. |
| World story files and storybeat packets | Selective heavy editing | Define each world's human conflict, physical setting, discoveries, character turns, and interaction identity. Remove repeated cosmology summaries. |
| Hub overview, quest, NPC, location, and event files | Reconcile, merge, or archive by use | Keep current authoring contracts. Avoid parallel scene scripts that disagree with production assets. Clearly distinguish proposals from implemented content. |
| Puzzle masterplaybooks and tuning specs | Rewrite relevant designs | Give puzzles distinct reasoning tasks and physical consequences. Remove the assumption that every ancient device needs frequency alignment. |
| Combat/system documents | Factual refresh | Describe actual rules, supported effects, and what players need to know. Preserve useful mechanics without requiring musical justifications. |
| Audio/music production guides | Mostly retain; edit overreach | Real music, motifs, sound production, and technical specifications are legitimate. Their terminology should not dictate every character's speech or every world system. |
| Art/prompt guides | Focused edit | Preserve the established colorful visual identity. Make Source effects contextual instead of an automatic glowing motif on every object. |
| Handoffs, old audits, checkpoints, abandoned implementation plans | Archive or mark historical | Preserve evidence and decisions. Remove their authority over new creative work. Keep a clear current-status entry point. |
| Exact duplicates and obsolete templates | Delete after reference review | Keep the maintained version. Repair inbound links and remove duplicate prompt inputs. |

Archive documents with an explicit historical status, replacement link, and exclusion from default authoring/generation inputs. Moving files into an archive folder is insufficient if a tool still loads them.

## A small authority structure

Use a short entry point that directs contributors to four distinct responsibilities:

1. **Creative brief and prose standard:** tone, player experience, character voices, examples of acceptable Source imagery.
2. **Story canon:** causality, character histories, relic functions and limitations, world transitions, ending.
3. **Current content specifications:** world/quest/puzzle intent, required beats, choices, consequences, current implementation status.
4. **Technical contracts:** schemas, stable IDs, callbacks, progression conditions, save compatibility, supported mechanics.

Existing files can fulfil these roles. Avoid creating another parallel set of bibles. Each mutable fact needs one owner; supporting files should link to it. Production assets establish what is implemented; approved design documents establish intended changes. A discrepancy should be recorded and resolved rather than silently treating either as universally correct.

## Editorial triage

Classify passages by purpose rather than banning words:

- **Keep:** ordinary industrial sounds, actual music, personal musical objects, a useful acoustic clue, an earned cultural reference, an occasional striking Source experience.
- **Ground:** sentences where a character or device can describe heat, pressure, movement, damage, access, control, or neurological symptoms directly.
- **Cut:** repeated cosmic explanations, stacked metaphors, self-explanatory theme statements, technical terminology with no player-facing consequence, and author commentary leaked into room descriptions.
- **Redesign:** interactions whose only reasoning is adjusting abstract parameters to target numbers.

Retain established names such as the Source, the Fork, the Chime, and Project Harmony where they serve recognition and causality. A familiar name can carry the motif without surrounding every mention with another musical explanation. Distinguish names, literal engineering, cultural language, and decorative metaphors during review.

Example targets from current assets:

- The Sanctuary right-wing description stacks a frequency dampener, resonance crystal, seismic filtering, and an answering harmonic. Establish the physical column, crystal, blocked system, and observable response first.
- The Prism Gallery combines audible color, alarm chords, braided chords, and frequencies. Focus the scene on shutters, light paths, surveillance, and the escape problem.
- `source_center_node_scale_final` literally says its tones “state the game's final answer in room form.” Delete author-facing analysis from player prose. Show what happens and let the player interpret it.
- Orion can use occasional musical imagery. Nova should sound like someone handling a dangerous tool; Zeke like someone understanding access and procedure; Gh0st like someone assessing a threat. They do not all need the same vocabulary.

## Puzzle disposition

The six tuning definitions share three target-value sliders, with specialized presentation for the opening and prism. They are not the complete puzzle inventory. Source reconciliation confirms separate dialogue-choice sequences for World 2 stasis, Source Gate and mural sockets, and milestone/action investigation for the main Spire Gallery. Preserve those distinctions and inspect actual live interactions before proposing replacements. The table below concerns the six tuning definitions only; these are proposed redesigns, not corrections required by old documents.

| Puzzle | Direction | What the player should reason about |
|---|---|---|
| World 1 Fork counter-tuning | Keep and simplify explanation | Recognize the dangerous cutter feedback, control heat, and establish a safe ground. Earn precision through clues and instrument feedback. |
| World 2 bioluminescent prism | Develop existing optical presentation | Redirect a visible beam through shutters/prisms to a receiver. Observe where light goes and what obstructs it. |
| World 3 chrono coils | Replace abstract calibration with an archive/security problem | Read a repeatable security cycle and choose a safe operation sequence. Tie clues and feedback to the heist. Exact replacement requires checking its current room and reward contract. |
| World 4 magma conduit | Rebuild as a machinery problem | Trace coolant/pressure routes, isolate the dangerous branch, and release the cradle safely. Actions visibly change connected machinery. |
| World 5 graviton well | Rebuild around spatial consequences | Understand how a local gravity control moves or stabilizes objects. Use a small readable board with discrete states suitable for touch. |
| World 6 Choir Lock | Redesign as a short culmination of learned crew roles | Coordinate distinct contributions and release the dangerous control. Give the player understandable responsibility at the emotional turn. Avoid a final numeric calibration or mandatory musical timing test. |

Do not turn all six into large minigames. Build the smallest expressive interaction for each purpose; reuse simple state-machine infrastructure. Clues must be available before commitment, failure must be understandable, and every essential relationship needs visual/text feedback. Preserve valid save states and route eligibility across renderer changes.

## Implementation sequence and review gates

### 1. Establish the new authority

Inventory current authoring entry points, prompt builders, devkit retrieval, handoffs, and scripts. Assign each relevant document: retain, edit, consolidate, archive, or delete. Record its authority and replacement. Rewrite the briefing and conflicting core passages first, then update the documentation index. Completion means a fresh content author receives one coherent direction.

### 2. Approve a small cross-world sample

Rewrite a short Sanctuary sequence, the Prism Gallery, one later crew conversation, and a Source approach. Include immediate interaction feedback and quest text, not room prose alone. Preserve plot facts and action accessibility. Read the sample as a continuous player sequence. Completion means it feels clearer and less self-conscious while retaining mystery and character.

### 3. Reconcile active world documents, then shipped prose

Work in complete sequences, beginning with the worst concentrations. Review rooms and variants, dialogue, event messages, quests/tasks, item and skill descriptions, cinematics, and runtime strings. Keep World 1's physical cause-and-effect; avoid rewriting good opening scenes merely for uniformity. Complete one world coherently before widening the pass.

### 4. Redesign puzzles separately

Prototype one replacement, preferably the Foundry's physical machinery problem. Establish clue clarity, feedback, touch usability, completion callbacks, retry behavior, and save/reload before applying the approach elsewhere. Keep the finale replacement late in the process so it can draw on actual earlier learning.

### 5. Verify the new direction survives production

Update exact-text tests and walkthroughs when wording changes, preserving their behavioral intent. Preserve stable IDs, reward ownership, quest gates, and save compatibility. Use explicit action references or update action/display names together; an editorial change must not hide a required action. Verify Android first and desktop presentation afterward.

Run content/reference checks and affected public route tests. Test changed puzzles through completion, failure, dismissal, background/resume, and reload. Treat keyword reports as review aids, not hard bans or automatic rewrite rules. Ordinary hums and literal frequencies can be appropriate.

## Completion criteria

- Core documents and generation inputs agree on restrained acoustic imagery.
- Archived plans no longer act as current instructions; retained history stays findable.
- A player can explain each revised sequence's physical problem and immediate goal in ordinary language.
- Companions have distinct voices and reveal growth through behavior.
- Each retained musical image serves a particular clue, culture, character, or emotional moment.
- Revised puzzles require distinct reasoning, with understandable feedback and reliable progression.
- The Source remains strange without every character confidently explaining its physics.
- Combat rules remain clear and satisfying; the prose pass does not introduce another resonance mechanic or rebalance combat incidentally.

This is a coordinated rewrite of creative authority, selected content, and repeated puzzle structures. A global word substitution or a new style guide layered over contradictory old instructions would leave the underlying problem in place.
