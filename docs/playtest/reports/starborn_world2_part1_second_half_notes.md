# World 2, Part 1 — second-half playtest notes

**Implementation update:** the critical items below are now addressed locally; see [the fix record](../../testing/WORLD2_PART1_SECOND_HALF_CRITICAL_FIXES_2026-10-30.md). The descriptions preserve the original triage; phone acceptance is pending.

Organized October 30, 2026 from the supplied recording transcript. Categories follow the existing playtest reports: Progress Blockers, Critical Bugs/Issues, Important Considerations. These new notes are triage, not implementation authorization. Overlaps with the previously approved first-half repair work are identified below. Resident backstory/placement decisions remain deferred for discussion.

## 1. Progress Blockers

No additional confirmed progression blocker appears in this half of the transcript. The fishing catch crash from the first half remains the session's reported minigame blocker; its Android saved-state payload fix is implemented locally and regression-tested, with phone acceptance pending.

## 2. Critical Bugs/Issues

### W2P1B-CB-01 — Long connections disappear from the minimap

Glow-Moss Cavern and Waterlogged Tunnel connect across several grid spaces. Their connecting line disappears when the other endpoint lies outside the minimap viewport, even though it is visible from an intermediate viewpoint. Retain the room layout unless inspection reveals a separate problem. Show a clipped continuation/edge indicator for long links; do not require both endpoint dots to be visible. **New issue:** distinct from the first-half cross-node arrow fix.

### W2P1B-CB-02 — Quest-dependent scan language/actions appear before use

Ferns and drifting spores are described as scan instructions before Botanist makes scanning available. Review similar actions individually. **Overlap:** the approved first-half fix now gates scan actions to the relevant active quest and hides them after their event completes, leaving ordinary descriptive text. **Still a collaborative presentation decision:** prefer natural object names/prose over prematurely telling the player to scan; no new naming rewrite was made for this attachment.

### W2P1B-CB-03 — Patrol Graveyard transceiver appears inert

The walkthrough recommends inspecting it for context, but tapping it produces no apparent response. Verify its quest prerequisites and intended purpose before deciding between a quest-gated action and always-available contextual inspection. Distinguish this transceiver from the Patrol Transceiver in Burned Glade, which grants cutter-related rewards. **New, unverified cause; not fixed in this pass.**

### W2P1B-CB-04 — Repeat REST fails, including Hunter's Blind

The same one-use behavior occurs beyond the falls. **Overlap implemented:** repeated rest now heals and provides feedback even at full HP. First-visit camp supplies remain one-time rather than becoming an infinite item source. Controller tests cover all ten affected one-use camps across Worlds 2–5; normal repeatable rest paths remain intact. Phone acceptance pending.

### W2P1B-CB-05 — Ridge Overlook Cache remains actionable after collection

At Sanctuary Outlook, the cache remains bold/clickable after granting its contents. Verify its depletion state and whether any repeat interaction is intentionally meaningful. If exhausted, remove its action consistently with other collected objects, preserving appropriate room prose. **New issue:** the first-half read-color treatment is for passive inspections and does not blanket-convert exhausted loot into gray clickable objects.

### W2P1B-CB-06 — Orion introduction implies prior familiarity

The Stasis Chamber's pod text says it bears “Orion's name” before Nova has met him. Introduce the inscription without implying recognition (for example, “bears the name Orion”), or omit it if the reveal works better without it. **New continuity correction; not implemented in this pass.**

## 3. Important Considerations

### W2P1B-IC-01 — Consistent, individually assessed discovery descriptions

Additional examples lacking a details popup: Tide-Worn Chest (Sea Cavern), Trampled Supply Pack (Hound Nest), Lost Surveyor Pack (Beast Run). These extend the first-half container/discovery audit. Establish a consistent standard, then inspect each action's role and existing presentation; do not add duplicate narration indiscriminately to every puzzle, quest, cache or flavor interaction. If using discovery text, it should precede item-obtained notices.

### W2P1B-IC-02 — Bioluminescent Prism presentation

Keep the puzzle's basic concept. Improve the popup's Starborn styling and visual quality; make the controls and Capture Light button fit an ordinary phone viewport without scrolling to reach the primary action. Account for larger text/accessibility rather than imposing a hard fixed layout.

### W2P1B-IC-03 — Stalker-Vine artwork mismatch

The enemy appears to use a bat image in the canopy. Investigate asset assignment and placeholder reuse, then decide which enemies actually need new art. Do not infer every shared sprite is wrong without checking its intended creature.

### W2P1B-IC-04 — Enemy names truncate too aggressively in combat

Names become ellipses after very few characters. Give names more horizontal space and consider relocating numeric HP rather than sacrificing identification. Check long names, multi-enemy parties and larger font settings.

### W2P1B-IC-05 — Encounter composition needs an intentional variety audit

The played section feels dominated by single enemies. Verify authored parties and actual presentation before asserting all W2 encounters are singles. Review progression, party sizes, enemy synergies, optional/required fights and recovery opportunities across Worlds 2–6. Variety should serve a deliberate encounter role, not a quota or an automatic increase in difficulty.

### W2P1B-IC-06 — Dedicated rest sound

Use a suitable short rest jingle/stinger. Separate this audio request from the now-addressed repeat-use bug.

### W2P1B-IC-07 — Player-facing ability terminology

Use “ability” rather than “skill” in the acquisition notification. Choose a clear phrase such as “New Ability” and review related player-facing copy for consistency without renaming internal identifiers unnecessarily.

### W2P1B-IC-08 — Cutter crafting materials and reasons to fight

Burned Glade's Patrol Transceiver apparently supplies the damaged cutter, schematic and all ingredients together. Review whether some materials should instead come from discoverable exploration, salvage or combat. Ensure reliable supply and clear guidance; avoid RNG softlocks and forced grinding. Preserve variety rather than making every schematic use the same acquisition pattern. **Discuss before redistributing rewards.**

### W2P1B-IC-09 — Enemy aggression and engagement feedback

Consider stronger reasons to fight and potentially more aggressive enemies. Workshop an approach/attack animation that communicates an enemy initiating combat. **Explicitly a discussion item:** do not change global aggression or automatic combat rules from this note alone.

### W2P1B-IC-10 — More deliberate new-quest presentation

The new-quest announcement should feel consequential: slower entry/fade and considered screen dimming. Assess timing and interruption policy alongside the notification queue; avoid slowing routine task updates in the same way automatically.

### W2P1B-IC-11 — Understand camp Neural Stabilizer grants

The user asks whether receiving one during rest is intentional and whether it creates an economy problem. Source inspection shows the affected camp events grant `painkillers` with their first rest. **Current repair preserves that first-visit grant only; repeated REST does not repeat it.** Whether camps should grant the item at all remains a design/economy discussion, not an automatic removal.

### W2P1B-IC-12 — Sentinel-3 vocal presentation

Replace the placeholder-looking “[high-pitched tone]” delivery with a fitting machine-like murmur/beep treatment, while retaining meaningful accessible text/subtitles. Workshop the sound/voice behavior instead of deleting necessary information.

### W2P1B-IC-13 — Temple-to-Hall reveal

Workshop a brief, magical-feeling gate-opening/entry transition when the stone lock opens into Hall of Echoes. It should communicate physical entry into the revealed place, not suggest teleportation. Preserve the sense of discovery before Orion's introduction.

## Decisions deliberately left open

Resident explanations/placement (first-half #7); Charlie's characterization; container narration standard; natural scan-object wording; encounter variety/reward redistribution/aggression; new enemy art; cinematic/transition styling. The second-half attachment does not authorize implementing those decisions.
