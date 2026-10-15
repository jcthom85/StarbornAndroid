# Desktop combat presentation audit

The combat view should answer three questions: who can act, what will an action do, and what happened? Text earns its place by answering one of them.

## Keep prominent

- Enemy and character names: identify the recipient of an action. Full names remain in the inspection card and selected-target panel when battlefield labels truncate.
- HP and stability values in inspection: support damage and break decisions without placing numbers over every creature.
- Discovered weaknesses: support informed attack selection without disclosing unlearned affinities.
- Ability effect, element, resolved target scope, cooldown, and unavailability reason: prevent costly guesses before committing. Cooldown timing explicitly refers to the acting character's turns.
- READY, CHOOSING, ACTING, DOWN, and SELECTED TARGET: describe meaningful states. Selection also has a visual marker.
- Enemy intent, stability break, and charging notices: affect immediate decisions. Fixed space prevents these notices from resizing sprites.
- Damage, healing, weakness, and break callouts: explain outcomes where they occur. The recent-actions feed and full battle log preserve information after effects disappear.
- Contextual keyboard shortcuts: keep input help in one consistent footer; action buttons still expose their direct shortcuts.

## Remove or consolidate

- Hostile Contact: repeats what the battlefield already communicates and gives no next step.
- FOCUSED and AVAILABLE TARGET over enemies: duplicate rings, brackets, and targeting dimming. Keep the selected-target label.
- The targeting paragraph repeating arrow/confirm/cancel instructions: the footer already gives this help. The command panel retains the action and selected target's full name.
- Auto / Turn Readiness Meter Charging: Auto is not a key. Replace it with Waiting for a turn; the main prompt also distinguishes waiting from a genuinely ready character.
- Known weaknesses / Weaknesses: unknown: one heading (Weaknesses) and one empty state (Not discovered) are sufficient.
- On cooldown followed immediately by Ready in N turns: retain the useful countdown once. Other blocking reasons remain visible.
- Tactical combat chronicle and chronological round events & engagements: Battle log and Actions and results are clearer.
- Awaiting initial tactical contact: No actions yet accurately describes an empty log.
- Readiness percentages in every party card: the progress bar plus READY state supplies the decision-relevant information. HP numbers remain.
- General ability mechanics paragraph in every ability menu: retain the crucial cooldown-turn clarification; each ability's description, scope, element, and cooldown explain its actual effect.

## Layout decisions

Crowded wide encounters and bosses with companions reclaim some side-panel width for the stage. Compact encounters with enough horizontal space use a single enemy formation rather than shrinking two rows. Enemy HUDs reserve 56 dp directly below the sprite, rather than leaving a large blank gap. Party formations and boss dominance remain intact.

Minimum readable sizes are verified for the supported crowded layouts, rather than treating lack of overlap as sufficient. Extreme counts or smaller-than-tested windows still require a separate layout policy; this change does not certify every possible viewport.

## Validation scope

Geometry checks cover party sizes 1-4, mixed boss formations, and minimum sprite/label sizes in crowded layouts. UI fixtures cover six enemies with four characters, compact and wide windows, and real keyboard/command flows. Screenshots assess hierarchy and readability. They do not replace playtesting animated effects, reaction prompts, or a newcomer's understanding of the combat mechanics.

## Follow-up corrections

Both party displays use the same state resolver: READY is available to act, CHOOSING is awaiting the player's command, and ACTING is executing the attack animation. DOWN takes priority over all action states. Waiting is shown only in the shortcut footer. Recent actions occupies a fixed area below the scrollable encounter, inspection, and party information, so a full party cannot push results out of view. Boss-with-companion fixtures verify a larger visual allocation in the wider stage.
