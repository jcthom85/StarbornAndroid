# Exploration HUD and Jed reward repair

- Kept the right HUD as independent map, quest and party modules, with tighter spacing, muted section labels and a full-width Menu button.
- Added a subtle minimap grid and explicit Open map action; north remains up.
- Quest summary remains authored text, capped at three lines with the complete entry available in Journal.
- Party portraits are larger, with level and clearer HP bars; the roster opens the Party menu.
- Connected desktop dialogue triggers to ExplorationController, matching Android. Previously the fallback silently ignored player_action triggers, including Jed's starter kit.
- Existing saves with Jed's talked milestone but no completed starter-kit event replay the original event once when exploration resumes. The event completion guard prevents repeating the reward.
- Original events.json supplies mining pistol, Nova flux liner, cryo inductor, two scrap metal and a ration pack. No replacement reward definitions or dialogue text.

Validation: desktop Kotlin compilation passed. Native playthrough and visual review remain outstanding.
