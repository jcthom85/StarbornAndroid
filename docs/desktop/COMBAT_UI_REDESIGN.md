# Desktop combat presentation

The desktop combat screen now uses two battlefield formations over the room art.
Each formation adapts between one and two columns, with scroll access for larger
encounters. Figures have compact status panels beneath them instead of the former
horizontal portrait rows.

## Presentation changes

- Selected actor/target uses a gold border. Valid targets use cyan; invalid
  targets dim and cannot be clicked while targeting.
- Controller lunge and miss tokens drive movement. HP changes drive recoil;
  damage and healing labels rise above the affected figure.
- Health and readiness bars animate. Stability has its own bar, with break,
  defeat, status and enemy intention labels.
- Command panel stays below the battlefield. Existing mouse and keyboard
  commands retain shared controller validation.
- Ability choices show targeting mode, authored cooldown and unavailable reasons.
- Victory shows XP, AP, credits, named item drops and actual level-up summaries,
  including stat changes and unlocked skills. Long results scroll.

## Validation boundary

Follow-up interaction checks and screenshot review are documented in
[Focused UI verification](FOCUSED_UI_VERIFICATION.md). The paragraph below records
the boundary at initial implementation time.

The code compiles and the portable Windows packaging task completes. No automated
tests were run for this redesign. Interactive checks of the new formations,
animations, crowded encounters and window sizes remain necessary. Existing
gameplay controllers and reward rules remain the source of combat behavior.
