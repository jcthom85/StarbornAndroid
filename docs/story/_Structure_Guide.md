# Starborn Story Documentation System

This directory uses a hierarchical structure to organize the game's narrative from high-level concepts down to specific implementation details.

## File Structure

### 1. Master Files (Root of `docs/story/`)
These files contain the canon "Bible" for the game.
- **`Starborn_Master_Story.md`**: The central source of truth for the plot, themes, and big beats.
- **`Characters.md`**: Detailed profiles for main cast members.
- **`Character_Arcs.md`**: The "Lie vs. Truth" progression for each main character.
- **`Hub_Requirements_Checklist.md`**: Entry/Exit state tracking for every Hub.
- **`Systems_Overview.md`**: How story intersects with gameplay mechanics.
- **`_World_Story_Checklists.md`**: A planning template to keep each World’s goal/reveal/turn/relic/payoff consistent.

### 2. World Directories (`docs/story/world_X_name/`)
Each major game world gets its own folder.
- **`hub_X_name/`**: Sub-directories for specific areas (Hubs) within that world.
Note: The Crash is treated as an **interlude** (not a full world) and may not have its own `world_X_` folder. Canon world numbering lives in `Starborn_Master_Story.md` even if folder names lag behind.

### 3. Hub Component Files
Inside each Hub folder, details are broken down by category:
- **`00_overview.md`**: High-level mood, themes, and summary of the area.
- **`01_quests.md`**: Quest flows, objectives, rewards, and IDs.
- **`02_npcs.md`**: Character rosters, dialogue scripts, and behaviors.
- **`03_locations.md`**: Detailed descriptions of specific rooms, nodes, or points of interest.
- **`04_events.md`**: Cutscenes and scripted events.

## Workflow
1.  **Design Phase:** Write the broad strokes in `Starborn_Master_Story.md`.
2.  **World Breakout:** Create a new folder for the world.
3.  **Hub Design:** Create hub folders and populate `overview.md`.
4.  **Implementation:** Fill out `quests.md`, `npcs.md`, etc., with data ready for the dev team to implement.
