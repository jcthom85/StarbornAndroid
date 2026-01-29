# Starborn - Master Tutorial List

This document catalogues every tutorialized mechanic in the game, its trigger location, and the method of teaching.

## 1. Meta & System Basics
| Mechanic | Method | Location / Trigger | Notes |
| :--- | :--- | :--- | :--- |
| **New Game / Load** | Implicit UI | Main Menu | Standard buttons. |
| **Narrative Advance** | Implicit UI | Prologue: "The Signal" | Tap screen to advance text/cinematics. |
| **Orion's Perspective** | Narrative | Prologue: Lab Sequence | Introduces "Source" lore implicitly. |
| **Autosave** | Prompt | World 1: Homestead (Arrival) | "Progress saves automatically..." |

## 2. Exploration & Interaction (World 1)
| Mechanic | Method | Location / Trigger | Notes |
| :--- | :--- | :--- | :--- |
| **Room Movement (Swipe)** | Script (`movement`) | W1_MQ01: Leaving Nova's Bunk | Forced swipe interaction. |
| **Minimap Exits** | Script (`movement`) | W1_MQ01: Leaving Nova's Bunk | "Watch the minimap highlight..." |
| **Hotspots** | Implicit + Prompt | Nova's Bunk | Pulse highlighted text. Prompt if idle. |
| **Interaction Menu** | Gated Step | Nova's Bunk (Trunk/Switch) | Force choice (Examine vs Use). |
| **NPC Dialogue** | Script (`npc_talk`) | W1_MQ01: Talk to Jed | "Tap a character's name..." |
| **Quest Tracking** | Script (`scene_market_journal`) | W1_MQ01: After "Talk to Jed" | "Open your journal... Quest tracked." |
| **Inventory (Bag)** | Script (`bag_basics`) | W1_MQ01: First item reward | "Swipe up or tap backpack..." |

## 3. Crafting & Equipment (World 1)
| Mechanic | Method | Location / Trigger | Notes |
| :--- | :--- | :--- | :--- |
| **Equip Gear** | Gated Step | Jed's Workshop (Before Mines) | Gate exit until weapon equipped. |
| **Tinkering** | Script (`scene_tinkering_tutorial`) | Jed's Workshop (Table) | "Drag parts... Watch stability." |

## 4. Combat Basics (World 1)
| Mechanic | Method | Location / Trigger | Notes |
| :--- | :--- | :--- | :--- |
| **Combat Entry** | Prompt | W1_MQ03: Deep Mine | "Combat is turn-based..." |
| **Attack / Skills** | Implicit UI | First Turn | Pulse "Attack" button. |
| **Cooldowns** | Prompt | W1_MQ03: Skill on CD | "Skills need recharge. Use Attacks." |
| **Targeting** | Implicit UI | First Multi-Enemy Fight | Pulse enemy portraits. |
| **Shields (0 Dmg)** | Feedback | W1_MQ03: Hit Shielded Enemy | "Guard Up! Use Guard Break skill." |
| **Weakness** | Feedback | W1_MQ03: Hit Weakness | "Weakness Hit! Cooldowns -1." |
| **Snack Slot** | Prompt | First Snack Equip/Combat | "Snacks are reusable..." |

## 5. Advanced Combat (World 2+)
| Mechanic | Method | Location / Trigger | Notes |
| :--- | :--- | :--- | :--- |
| **Party Switching** | Script (`scene_ollie_recruitment`) | Recruit Zeke/Ollie | "Tap portrait to swap..." |
| **Status Effects** | Tooltips | World 1 Late / World 2 | Tap icons for info. |
| **Jammed (Silence)** | Feedback | World 2: Sector 9 | "Jammed! Skills disabled." |
| **Marked (Crit)** | Feedback | World 2: Sector 9 | "Marked! Next hit critical." |

## 6. Survival & Minigames (World 2+)
| Mechanic | Method | Location / Trigger | Notes |
| :--- | :--- | :--- | :--- |
| **First Aid** | Script (`first_aid_failure`) | World 2: Crash Site | "Hold until green window." |
| **Cooking** | Script (`cooking_failure`) | World 2: Campfire (Optional) | "Release in gold zone." |
| **Fishing** | Script (`fishing_basics`) | World 2: Tideglass (Optional) | "Tap in green band." |

## 7. Late Game Systems
| Mechanic | Method | Location / Trigger | Notes |
| :--- | :--- | :--- | :--- |
| **Stealth** | UI Overlay | World 3: The Spire | Visibility Meter. |
| **Scan (Lens)** | Prompt | World 3: Archive | "Use Scan to reveal seams." |
| **Space Combat** | Prompt | World 5: Orbital Ring | "Tap to fire. Swipe to aim." |
