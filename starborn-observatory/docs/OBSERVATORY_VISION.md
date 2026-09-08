# Starborn Observatory — Product Vision

## Purpose

Starborn Observatory is a standalone PC teaching, inspection, and playtest tool for understanding Starborn as a complete authored game system.

It exists because the game has accumulated a large amount of AI-assisted content. The tool must make the authored structure legible to a human: what happens, why it happens, what the player can do next, and which assets participate in each moment.

The Observatory is read-only by default. It may provide controlled simulations and playtest launches, but it must never silently rewrite the game or mutate shipped assets.

## Core questions the tool must answer

1. What is the intended story from the opening through the ending?
2. Where is the player, and how does the player reach the next story beat?
3. How is each quest started, advanced, completed, failed, or blocked?
4. Which dialogue, milestone, event, item, NPC, room, enemy, cinematic, and audio cue participate in that quest?
5. What changes in the world when a milestone or quest stage changes?
6. What can the player do at this point, and what can they not do yet?
7. What does a combat encounter actually contain and reward?
8. What art, music, ambience, voice, and cinematic assets are attached to a moment?
9. Where are the gaps, contradictions, unreachable content, duplicate IDs, or suspicious balance outliers?

## Primary workspaces

### 1. Story Navigator

An ordered, explorable campaign view. It should show:

- World and hub progression
- Main quests and side quests in intended order
- Quest stages as a timeline
- The trigger that starts each beat
- The location where it occurs
- Dialogue and cinematic references
- Required and produced milestones
- Player-facing summary and implementation-level explanation

Every story beat should have a “Show me why” panel that traces its dependencies backward and consequences forward.

### 2. Quest & State Explorer

Quest cards must show lifecycle state: not started, active, stage, complete, failed, or unreachable. Selecting a quest opens:

- Start conditions
- Start triggers
- Stage graph
- Task list and completion rules
- Dialogue selectors
- Milestones set or cleared
- Rewards and inventory changes
- Rooms/NPCs/events involved
- All references into and out of the quest

The explorer needs a state simulator. Users should be able to create a disposable hypothetical state, toggle a milestone, complete a task, add an item, or move to a room, then see which dialogue and actions become available. Simulation must be clearly labeled and never save to the game.

### 3. World Atlas & Map Room

The existing world/hub/node/room browser becomes a connected map system:

- World map with hubs
- Hub map with nodes
- Node map with rooms in their authored coordinates
- Room portrait art in 9:16 mobile framing
- Room descriptions and description variants
- Connections and blocked directions
- NPCs, items, actions, enemies, enemy parties, weather, lighting, and audio
- Story gates on each room or exit
- “What changes here?” milestone/state diff

Map views should support a narrative overlay showing where quests begin, where steps occur, where bosses are found, and where the player can get stuck.

### 4. Entity Library

Unified searchable library for:

- Characters and NPCs
- Enemies and enemy parties
- Items, weapons, armor, snacks, materials, and recipes
- Skills, statuses, and tuning puzzles
- Events, milestones, shops, and tutorials

Each entity page must show raw authored data, a human-readable summary, all inbound references, all outbound references, and warnings.

The library must treat these as full teaching surfaces rather than generic JSON records:

- **Enemies:** stats, tier, role, behavior, resistances, weaknesses, abilities, drops, encounters, movement routes, art, and balance comparisons
- **Abilities:** costs, damage/effects, cooldowns, targeting, unlock conditions, owner, status interactions, and every place used
- **Items:** type, price, effects, recipes, shops, drops, equipment stats, owners, and progression relevance
- **Characters:** stats, skills, level progression, gear, dialogue, story role, and party availability
- **Statuses:** application rules, duration, stacking, cleanses, immunities, and affected abilities
- **Encounters:** enemy groups, location, trigger, rewards, intended difficulty, and possible player strategies
- **Economy:** credits, XP, AP, shop prices, drops, crafting costs, and reward pacing
- **Combat rules:** damage formulas, elemental rules, stability/break rules, cooldowns, and AI behavior
- **Tutorials:** what each tutorial teaches, trigger conditions, completion conditions, and related content

Every page should expose asset provenance: source file, JSON path, art/audio file, and inbound/outbound references.

### 8. Scenario Lens

The Scenario Lens combines the Observatory's systems around one playable or authored situation. A user selects a room, quest stage, cinematic, dialogue beat, or enemy encounter and sees:

- Relevant location and map context
- Current story objective
- State and milestone requirements
- Available actions and why they are available
- NPCs, enemies, items, abilities, and rewards involved
- Dialogue, music, ambience, SFX, voice, and cinematic assets
- Rules that resolve the situation
- Downstream consequences and newly unlocked content

This is the primary “teach me this moment” workflow. It should be possible to move from a scenario to any participating asset and back without losing context.

### 5. Audio & Cinematic Lab

A safe playback and inspection surface for:

- Music
- Ambient loops
- Battle music
- SFX
- Voice clips
- Cinematics
- Dialogue lines with voice profiles

The lab needs play, pause, seek, loop, volume, and queue controls. It should show where each cue is used, what triggers it, and what layer it occupies. Cinematics should be launchable as isolated previews with their captions, timing, choices, and follow-up actions visible alongside the playback.

### 6. Playthrough Tutor

A guided “teach me the game” mode. It walks through a clean campaign state and explains each step:

- Current location and objective
- Available actions
- Why the action is available
- What state it changes
- What content it unlocks
- What the player is expected to understand

It should support branching explanations such as “What if I refuse?”, “What if I miss this item?”, and “What if I return later?”.

### 7. Audit & Health Center

Automated findings grouped by severity:

- Broken references
- Unreachable quests, rooms, dialogue, or cinematics
- Milestones that are set but never consumed
- Conditions that can never be true
- Assets referenced but missing
- Audio cues without catalog/binding entries
- Rooms without art or descriptions
- Enemies without encounters or drops
- Story beats with no player path
- Balance outliers in HP, speed, rewards, resistances, or encounter density

Every warning must link directly to the affected content and explain how the tool derived the warning.

## Information architecture

The left rail should expose the major mental models, not file types:

`Learn` → Playthrough Tutor, Story Navigator

`Explore` → Worlds & Maps, Quests & State, Entity Library

`Experience` → Audio Lab, Cinematic Lab, Dialogue Browser

`Validate` → Audit Center, Reference Graph, Balance Views

The top bar should always show the current campaign/world/space and whether the user is viewing authored data or a disposable simulation.

## Data model strategy

The Observatory should ingest the existing JSON assets without copying or rewriting them. A normalized in-memory index should be built at load time, preserving:

- Source file
- JSON path
- Stable ID
- Display name
- Typed relationships
- Conditions and triggers
- Asset paths

The raw JSON must remain inspectable. Normalized views are for navigation; raw data is the authority.

## Reference graph

The most important technical feature is a bidirectional reference graph. Nodes include worlds, hubs, nodes, rooms, quests, stages, tasks, milestones, dialogue lines, events, cinematics, audio cues, enemies, items, NPCs, and skills.

Edges should describe intent, for example:

- `quest starts_with dialogue`
- `dialogue sets milestone`
- `milestone unlocks room`
- `room contains enemy`
- `enemy drops item`
- `quest occurs_in room`
- `event launches cinematic`
- `cinematic plays audio`

The graph powers “why is this here?”, “what does this affect?”, and audit reports.

## Safety boundaries

- Never edit Android source code from the Observatory.
- Never modify the authoritative asset directories during normal use.
- Treat playthrough simulations as disposable snapshots.
- Require explicit user action before exporting, patching, or generating files.
- Show source file and JSON path for every displayed fact.
- Keep broken or ambiguous data visible; do not hide it behind forgiving fallbacks.

## Phased build plan

### Phase 1 — Foundation

- Standalone shell and navigation
- Asset loader and normalized index
- Search, filters, raw JSON inspector
- World/hub/node/room maps with portrait art
- Enemy and quest basic detail views

### Phase 2 — Teach the story

- Story Navigator timeline
- Quest lifecycle pages
- Milestone and condition visualizer
- Reference graph and “why” tracing
- Guided campaign walkthrough
- Scenario Lens for rooms, quest stages, dialogue beats, and encounters

### Phase 3 — Experience lab

- Audio catalog browser and playback
- Cinematic browser and playback
- Dialogue browser with trigger context
- Room atmosphere preview: music, ambience, weather, lighting

### Phase 4 — Safe simulation

- Disposable campaign state snapshots
- Condition evaluation
- Quest stage simulation
- Unlock/lock diff view
- Branch exploration and explainable next-action suggestions

### Phase 5 — Audit and balance

- Automated integrity findings
- Story reachability analysis
- Encounter/reward balance dashboards
- Exportable review reports
- Optional, explicit patch proposal workflow
- Cross-system economy and reward pacing reports

## Definition of done

The Observatory is successful when a developer unfamiliar with Starborn can open it and answer, without reading source code:

> “What is happening in the story right now, where do I go, why can I go there, what happens when I do, and what assets make that moment?”

It is also successful when a designer can identify a broken story gate, preview its dialogue/music/cinematic, inspect the affected map location, and understand the downstream consequences in one continuous workflow.
