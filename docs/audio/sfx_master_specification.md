# Starborn — Sound Effects (SFX) Master Specification & Generation Bible

## 1. Aesthetic Foundation: 1980s Analog Cassette-Futurism

All sound effects in *Starborn* must feel tactile, physical, and grounded in 1980s analog sci-fi technology:
- **Mechanical & Pneumatic**: Heavy clunks, solenoids, airlock pressure releases, relay switches.
- **Synthesized Transients**: FM synth chirps, 8-bit digital beeps, CRT warm buzzes.
- **Physical Foley**: Gritty scrap metal rattle, rubber hose flex, wooden spoon clatter, water ripples.
- **Zero Modern Sterile Clicks**: Avoid modern generic iOS/flat UI blips; prefer warm mechanical relays and tape transport clicks.

---

## 2. Master SFX Registry & Prompt Cards

### Category A: UI & System Navigation
| Cue ID | Description | Duration | Prompt |
| :--- | :--- | :--- | :--- |
| `sfx_ui_button_click` | Crisp mechanical relay button click | 0.5s | *Tactile mechanical relay switch click, vintage sci-fi terminal button press, crisp transient, subtle metallic spring.* |
| `sfx_ui_confirm` | Positive electronic confirmation tone | 0.8s | *Warm two-tone FM synth chime, retro computer confirmation beep, positive sci-fi terminal prompt.* |
| `sfx_ui_cancel` | Soft electronic back/cancel click | 0.5s | *Subtle descending electronic click, terminal back button press, clean tape deck stop click.* |
| `sfx_ui_error` | Low mechanical buzz / error buzzer | 1.0s | *Low vintage buzzer tone, 80s terminal access denied buzz, analog circuitry overload warning.* |
| `sfx_ui_tab_switch` | Tape transport click / tab swipe | 0.5s | *Analog cassette player deck head engagement click, fast tactile slider snap.* |
| `sfx_ui_equip_item` | Heavy leather strap and metallic holster click | 1.0s | *Tactile gear holster click, heavy metal buckle latch, military equipment equip sound.* |
| `sfx_ui_item_pickup` | Bright electronic chime with coin rattle | 1.0s | *Bright 8-bit loot pickup chime, metallic scrap coins clinking together, rewarding discovery sound.* |

---

### Category B: Exploration & Environmental Interactions
| Cue ID | Description | Duration | Prompt |
| :--- | :--- | :--- | :--- |
| `sfx_door_airlock_open` | Heavy pneumatic depressurization & slide | 3.0s | *Heavy sci-fi airlock door opening, intense pneumatic air release hiss, motorized sliding heavy steel door.* |
| `sfx_door_airlock_close` | Hydraulic clamp & airtight seal thud | 2.5s | *Heavy bulkhead door sliding shut, hydraulic clamp engaging with deep resonant steel thud.* |
| `sfx_terminal_hack_success` | Rapid dial-up chirp & green console unlock | 2.5s | *Vintage computer modem handshake burst, rapid electronic decoding beeps, triumphant green terminal chime.* |
| `sfx_terminal_boot` | CRT monitor hum & electronic degauss coil | 3.0s | *Vintage CRT monitor power on degauss coil hum, static electric pop, 80s terminal phosphor hum.* |
| `sfx_loot_crate_open` | Latches uncoupling and motorized lid opening | 2.5s | *Heavy industrial cargo container latches unbuckling, pneumatic seal pop, motorized lid opening.* |
| `sfx_tape_insert` | Physical cassette tape insertion and door snap | 1.5s | *Tactile cassette tape being pushed into a vintage tape deck, mechanical spring catch click, play button engaged.* |
| `sfx_scavenge_metal` | Scrap metal clinking and tools digging | 2.0s | *Scrap metal pieces clattering in a dust bin, wrench digging through spare bolts, gritty industrial scavenging.* |

---

### Category C: Combat & Weaponry
| Cue ID | Description | Duration | Prompt |
| :--- | :--- | :--- | :--- |
| `sfx_combat_blaster_fire` | 80s analog synthesizer laser shot | 1.0s | *Vintage 80s sci-fi blaster gunshot, punchy analog synthesizer laser beam, resonant plasma discharge.* |
| `sfx_combat_heavy_wrench` | Heavy blunt metal impact with bone crunch | 1.2s | *Crushing pipe wrench melee strike, heavy steel impact against armor plate, resonant metallic thud.* |
| `sfx_combat_plasma_arc` | Crackling high-voltage plasma shock | 1.8s | *Crackling electric plasma arc discharge, high-voltage Tesla zap, sizzling ionized air.* |
| `sfx_combat_phase_blade` | High-frequency hum and clean energy slice | 1.5s | *High-frequency vibrating energy katana slash, clean air displacement whoosh, harmonic laser blade cut.* |
| `sfx_combat_shield_deflect` | Resonant hexagonal energy barrier deflection | 1.5s | *Sci-fi kinetic energy shield impact, resonant glass-metallic deflection ping, harmonic forcefield hum.* |
| `sfx_combat_crit_hit` | Massive bass drop impact and glass shatter | 2.0s | *Massive sub-bass explosion punch, critical strike shatter impact, dramatic cinematic impact hit.* |
| `sfx_combat_enemy_screech` | Alien bioluminescent predator roar | 2.0s | *Eerie organic alien creature roar, guttural insectoid hiss and high-pitched predatory shriek.* |
| `sfx_combat_robot_stomp` | Multi-ton hydraulic mech footstep | 2.5s | *Gigantic multi-ton mechanical robot step, heavy ground shake impact, hydraulic piston hiss.* |

---

### Category D: Activities & Mini-Games
| Cue ID | Description | Duration | Prompt |
| :--- | :--- | :--- | :--- |
| `sfx_fishing_cast` | Fishing rod line whipping through air | 1.5s | *Fishing rod line cast whoosh, spinning reel whirring buzz, high-speed line tension.* |
| `sfx_fishing_splash` | Water droplet and lure plop | 1.2s | *Clean fishing bobber splashing into water, gentle ripple and bubbly aquatic plop.* |
| `sfx_fishing_bite` | Sharp tug on fishing line and alert beep | 1.2s | *Sharp fishing rod flex strain, water splash, subtle excitement chime.* |
| `sfx_fishing_catch` | Rewarding splash and glittering catch chime | 2.0s | *Triumphant water splash with a glittering magical discovery chime, fish flopping on boat deck.* |
| `sfx_arcade_coin_insert` | Metallic quarter falling through coin slot | 1.5s | *1980s coin-op arcade coin drop, metallic coin sliding down slot and triggering electronic credit chirp.* |
| `sfx_arcade_jump` | 8-bit square wave jump chirp | 0.5s | *Retro 8-bit video game jump sound, classic arcade square wave upward pitch sweep.* |
| `sfx_arcade_laser` | 8-bit space invader shot | 0.5s | *Authentic 1980s arcade space shooter laser zap, punchy 8-bit noise pulse.* |
| `sfx_arcade_game_over` | Descending 8-bit death jingle | 2.5s | *Classic 1980s arcade game over jingle, descending 8-bit arpeggio with sad final noise burst.* |
| `sfx_tinkering_ratchet` | Socket wrench clicking and tightening | 1.8s | *Vintage mechanical socket wrench ratcheting back and forth, tight bolt squeak, workbench tool clink.* |
| `sfx_cooking_sizzle` | Hot oil pan sizzle and wooden spatula stir | 2.5s | *Hot cast iron pan sizzling with butter, sizzling stew bubbling, wooden spoon stirring delicious food.* |

---

## 3. Playback Pipeline & Integration

1. **Storage**: All audio files live in `app/src/main/res/raw/<cue_id>.mp3`.
2. **AudioCuePlayer**: Loaded via `SoundPool` for instantaneous low-latency multi-stream playback.
3. **Bindings**: Registered in `app/src/main/assets/audio_catalog.json` with `type: "sfx"` or `"ui"`.
