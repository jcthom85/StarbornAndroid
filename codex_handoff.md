# Codex CLI Handoff Guide: Starborn Android Port

> **Historical document:** This handoff stops at World 3 and is no longer current. For the completed campaign state and the next production work, start with `antigravity_handoff.md` (updated 2026-06-20).

Welcome! This file provides a quick-start reference so you can pick up exactly where the previous assistant left off.

## 1. Project Context & Rules
- **Game Name:** *Starborn* (an Android/Jetpack Compose port of a Python/Kivy game).
- **Core Architecture:** Highly data-driven. **All gameplay content (worlds, hubs, rooms, quests, events, dialogues, enemies, items, and skills) is defined in JSON assets** located under `app/src/main/assets/`.
- **Lore Shift:** The lore has shifted from *Digital/Glitch* (ELARA PROTOCOL) to *Cosmic Resonance/Ocean* (Harmonic Lock / Elara's Song / Ocean / Ice / Currents).
- **Mechanics:** "Calories/Hunger" has been replaced with "Erosion/Burnout". "Source Points" in combat have been replaced with turn-based cooldowns (0-5 turns).
- **Asset Integrity:** Any modifications to room presence, milestones, dialogues, and audio triggers must be validated using Gradle tasks.

## 2. Current State
- **Completed:** World 3 Hub 1 (Lower City) is fully implemented, integrated, and verified (all tests pass green).
- **Walkthrough Reference:** See the list of modified files, code changes, and test results in [walkthrough.md](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/walkthrough.md).
- **Next Up:** World 3 Hub 2 (Upper City / Penthouse Heist).
- **Branch:** Currently on `world2-production`.

## 3. Immediate Next Steps & Tasks
- **Implementation Plan:** See [implementation_plan.md](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/implementation_plan.md) for the design of the Upper City hub.
- **Progress Tracking:** See [task.md](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/task.md) for the phase-by-phase implementation checklist.
- **Goal:**
  1. Add `hub_6_upper_city` to `hubs.json` and layout nodes in `hub_nodes.json`.
  2. Implement the 5 new rooms in `rooms.json` (Laundry Service, Skypark, Exec Lounge, Archive, Landing Pad).
  3. Wire quests (`w3_mq13`, `w3_mq14`, `w3_mq15`, `w3_sq14`, `w3_sq15`) in `quests.json` and transition events in `events.json`.
  4. Write dialogue nodes in `dialogue.json` and new enemies/items.
  5. Add milestones to `validate_room_presence.ps1`, author `Hub4CriticalFlowTest.kt` unit test, and verify integrity.

## 4. Helpful Commands
To run static asset verification:
```powershell
.\gradlew.bat :app:runAssetIntegrity
```

To run the JUnit test suites:
```powershell
.\gradlew.bat :app:testDebugUnitTest
```
