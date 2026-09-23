# BurgQuest readiness review — September 22, 2026

Event website: https://burg.quest/ lists September 25–27, 2026 in Williamsburg. Prioritize booth readiness over the ongoing finale balance investigation for this week.

This is the **pre-implementation review**. See [the focused prep record](BURGQUEST_PREP_IMPLEMENTATION.md) for subsequent fixes and their verification status.

## Verdict

The four options are functional scenario shortcuts, not yet four curated 5–10 minute experiences. Do not advertise all four as independently validated booth demos. This review inspected the scenario catalog, menu launch path, setup functions, room definitions and prior arcade evidence. Fresh debug/instrumentation builds passed, but the emulator stalled during APK installation; the added packaged-fixture audit could not execute. No timed newcomer playthrough was completed. Installation attempts were stopped; normal saves were not cleared.

| Track | Actual setup | Assessment |
| --- | --- | --- |
| Story Opening | Ordinary `startNewGame()`, Nova's bunk, intro cinematic, normal opening quest/tutorials | Authentic introduction; useful for story-minded visitors. No authored short-demo endpoint or measured guarantee of reaching combat within ten minutes. |
| Tactical Combat | Calls party tutorial shortcut: Nova and Zeke only, Sector 9 crash site, no enemies in starting room, tutorials reset | Misleading description promises full party, Canopy skirmish and advanced systems. Needs correction and a purpose-built combat fixture before being the primary booth demo. |
| Astra | Four crew members, bridge start, all six restored arcades, full debug inventory | Best guided low-pressure showcase. Prior emulator checks verified all six cabinet launches. Exploration/crafting/arcades need a simple route and goal; bridge start does not immediately present the games. |
| Titan Walker | Four-member debug party in Titan Dock with Titan enemy, late Foundry quest state | Good spectacle potential. It inherits generic debug equipment, skills and stock, rather than the campaign-earned fixture used by the Titan balance tests. Those victories do not certify this demo's difficulty or duration. |

## Shared issues

- Non-story tracks use `startNewGame(debugFullInventory=true)`: full inventory, all equipment unlocked, all skills for the initially seeded party, 50,000 credits, and generic starting level. Quest completion helpers do not reproduce earned XP/AP progression. This can overwhelm newcomers and undermine consistent combat difficulty.
- Menu titles still say **BURGFEST**, despite the BurgQuest dialog/button.
- No dedicated BurgQuest intro objective, completion screen, time-boxed route, or one-tap play-again/choose-another flow was found in the shortcut implementation. Campaign continuation remains possible.
- Launches use the ordinary services/session through `MainMenuViewModel`; these legacy shortcuts bypass the rebuilt test-session gate. Treat the booth device as dedicated demo state; demo launch replaces the active session and uses the normal persistence path. Do not assume player autosave isolation merely because the scenarios are named debug.
- New Game is now hidden by a local temporary visibility flag; its handler and campaign functionality remain available for restoration. Story Opening still deliberately starts the normal game. BurgQuest Demo, Debug Scenarios, Load Game and Settings remain present as requested.

## Recommended booth preparation

1. Make Tactical Combat the recommended first choice: short briefing, actual named encounter, fixed appropriate party/equipment, a small useful skill selection and limited supplies. Target one understandable fight followed by a clear completion screen.
2. Curate Titan independently and verify repeated survival with the exact demo loadout. Explain its recovery opening before the fight. Avoid using the older campaign fixture's win rate as booth validation.
3. Give Astra a suggested five-minute path: common room → one arcade → optional workshop. Keep it as a guided exploration option.
4. Label Story as the campaign opening, with realistic expectations; either trim it into a slice or reserve it for visitors who want narrative.
5. Correct public labels, add simple reset/exit instructions, and time each intended route on the actual booth device. Check readability, audio in the venue, offline operation and repeat launch behavior.

Scenario redesign is recommended, not implemented in this review. The only production change this pass hides the title-screen New Game button. No version bump, commit, push or Play upload occurred. Finale work remains paused for this event-readiness detour.
