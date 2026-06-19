# Tier 1 Tutorial Decisions

These decisions lock the remaining W1 tutorial scope. The guiding rule is: do not teach what the player cannot immediately use.

## Bed / Rest Recovery

Decision: do not add rest recovery at Nova's Bunk.

Nova's Bunk remains narrative flavor in W1. The first true safe rest point should be the Astra Quarters bed after the ship becomes the player's hub. This preserves W1 resource pressure and keeps the Astra feeling like a real upgrade.

Implementation:
- No W1 rest tutorial.
- No HP/resource recovery at Nova's Bunk.
- Teach rest at Astra Quarters when the bed interaction actually restores the party.

## First Party Combat

Decision: teach party basics during the first combat with 2+ party members.

Zeke recruitment is story-wired before the player has a meaningful party-combat interaction. The tutorial should fire when the player can immediately practice switching active characters.

Implementation:
- Script ID: `party_basics`.
- Trigger condition for W2 wiring: first combat where `partyMembers.size > 1`.
- Prompt: "Tap a portrait to switch the active character. Each party member has unique skills."

## Snack Slot

Decision: teach snacks on first snack command use in combat.

W1 does not guarantee snack acquisition on the critical path, and adding a snack reward just to teach the slot would increase W1 tutorial load. The mechanic should explain itself when the player actually uses it.

Implementation:
- Script ID: `snack_slot`.
- Trigger condition for future wiring: first snack command use in combat.
- Prompt: "Snacks are reusable combat tools. Use them like skills - they recharge after a cooldown."
- Keep status effects implicit through chips, log text, and combat feedback unless playtests show confusion.

## W1 Impact

None. These decisions keep W1 locked and move tutorial wiring to W2 or contextual combat hooks.
