# Starborn Playtest Execution Guide

This document contains instructions for automated playtesting of **Starborn** using both the fast headless simulation suite and the visual Maestro UI test flows.

---

## 1. Quick Verification: Headless Engine Suite (No Emulator Required)

Run the deterministic test suite to verify all 30 main quests, 30 side quests, combat skills, progression events, and room linkages:

```powershell
# Set JDK and run unit test suite
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
./gradlew testDebugUnitTest
```

### Content Validation Scripts
Run the seven project content validator scripts:
```powershell
powershell.exe -ExecutionPolicy Bypass -File ./scripts/validate_narrative_prose.ps1
powershell.exe -ExecutionPolicy Bypass -File ./scripts/validate_world1_content.ps1
powershell.exe -ExecutionPolicy Bypass -File ./scripts/validate_audio_references.ps1
powershell.exe -ExecutionPolicy Bypass -File ./scripts/validate_progression_references.ps1
powershell.exe -ExecutionPolicy Bypass -File ./scripts/validate_room_presence.ps1
powershell.exe -ExecutionPolicy Bypass -File ./scripts/audit_fun_cadence.ps1
```

---

## 2. Visual UI Playtests with Maestro (Emulator / Device Required)

### Prerequisites
1. **Launch Android Emulator** (API 34 or 35 recommended).
2. **Install Debug Build**:
   ```powershell
   ./gradlew installDebug
   ```
3. **Verify Device Connection**:
   ```bash
   adb devices
   ```

### Running Individual Modular Flows
Each flow targets a specific world, mechanic, or system using the in-game Debug Scenarios:

| Flow File | Description | Target Areas |
|---|---|---|
| `playtests/maestro/playtest_01_prologue_to_mine_boss.yaml` | World 1: Mine Descent & Field Menu | Mining Shaft, Inventory, Tinker, Journal, Map, Stats |
| `playtests/maestro/playtest_02_astra_hub_and_tape_deck.yaml` | Astra Home Base & Accessibility | Common Room, VHS Tapes, Accessibility & Comfort toggles |
| `playtests/maestro/playtest_03_world3_ancient_spires.yaml` | World 3: Ancient Spires | Upper City infiltration, Service route vents, Quest journal |
| `playtests/maestro/playtest_04_world4_foundry_titan.yaml` | World 4: The Foundry | Slag Pits, Forge, Meltdown escape, Field Kit tinkering |
| `playtests/maestro/playtest_05_fishing_multi_biome.yaml` | Multi-Biome Fishing & Inventory | Full Inventory start, Gear previews, Bait & Tackle |
| `playtests/maestro/playtest_06_world6_climax_and_ending.yaml` | World 6: The Source & Finale | The Center, White Shore, Final party roster |
| `playtests/maestro/menu_full_audit.yaml` | Comprehensive Field Menu Audit | Complete tab-by-tab deep audit with sub-navigation |

### Run Command Example
```bash
# Run a specific flow
maestro test playtests/maestro/playtest_01_prologue_to_mine_boss.yaml

# Run the full master suite
maestro test playtests/maestro/playtest_suite_all.yaml
```

---

## 3. Interpreting Output & Failure Resolution

1. **Assertion Errors on Text**:
   - Check if a room title or menu label was modified (e.g. `"Tinkering"` vs `"Tinkering & Assembly"`).
2. **Timeout Errors on Screen Transitions**:
   - If an emulator runs slowly, increase `extendedWaitUntil: timeout: 20000` in the corresponding YAML file.
3. **Screenshots**:
   - Maestro automatically outputs screenshots into the current directory or `~/.maestro/tests/`.
