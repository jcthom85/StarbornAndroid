# Desktop combat presentation pass

- Exploration Menu is a compact framed icon control, retaining keyboard access and large target settings.
- Combat uses the original Starborn typography.
- Tutorial modal and instruction capsule follow Android framing and shared CombatTutorialCopy. Continue/skip use shared controller gating; current tutorials intentionally report canSkip=false.
- Combat action rows have dark fills, restrained accent borders, and command icons.
- Removed duplicate target and ready-character instructions, and replaced the persistent event card with an optional battle log control.
- Right commands anchor toward the portrait battlefield on ultrawide screens.
- Reduced battlefield darkness. Flash suppression keeps ordinary weather visible.

Validation: Kotlin compilation and offscreen screenshot review of exploration, combat, and a tutorial modal. The tutorial screenshot is a presentation fixture using the shared BRIEF state, not a complete tutorial playthrough. No automated tests were run.

Remaining separate work: Android target/selection and meter parity, skill/item row redesign, weapon-specific and persistent status effects, multipart bosses, and staged victory/level-up presentation. Battle log and outcomes still use their prior dialogs.
