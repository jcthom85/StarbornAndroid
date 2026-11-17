# Starborn — Suno Studio & MCP Production Pack

Aesthetic: **Cassette-Age Space Opera**
All Great Frontier tapes (`gf_01`–`gf_10`) remain untouched.

## Workflow Checklist (Per Track)
1. Use Suno MCP (if OAuth connected) or Suno Studio / Create (Premier) with the cue's Reference Audio `.wav` (`audio_studio/reference_audio/`) or `.mid` motif (`audio_studio/midi_motifs/`).
2. Set Instrumental mode (except `music_elaras_song` which has vocals) and apply the **Style Prompt** + **Negative Exclusions** (auto-refined from your `style_preferences.json`).
3. Generate 4 candidates (2 x two-output runs), run `analyze` + Hydrogen evaluation, listen via the Suno URLs / Review Board, and pick A/B/C/Reroll/Revise/Hold.
4. Export lossless `.wav` (or `.mp3`) into `audio_studio/incoming/` and run: `.tools/audio-venv/bin/python scripts/audio_studio.py match-incoming --auto-master`
5. Integrate approved candidates cleanly via `integrate-approved --sync-bindings`.

---

## Category: CORE (6 tracks)

### `music_title_theme` — Starborn Main Title (The Spark in the Dark)
- **Recommended Tool:** `suno` | **Key:** D Minor -> D Major | **BPM:** 88 | **Length:** ~110s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_source.mid`
- **Style Prompt:** `80s analog cassette sci-fi JRPG main title theme, 88 BPM, D minor opening into hopeful D major. Solo fingerpicked acoustic steel-string guitar states a memorable 5-note ascending theme, joined by warm Prophet-5 analog synth pads, lyrical solo cello, crystalline glass chimes, and a steady warm pulse. Nostalgic analog tape saturation, expansive cosmic wonder, seamless loop.`
- **Exclude Styles:** `vocals, choir lyrics, trap beats, abrasive EDM drops, singing`

### `music_astra_common_room` — The Astra Lounge (Coffee & Cassettes)
- **Recommended Tool:** `suno` | **Key:** G Major | **BPM:** 76 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/pilot_music_astra_common_room.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_jed.mid`
- **Style Prompt:** `Warm 80s analog cassette lo-fi folk-synthwave, 76 BPM, G major. Warm Fender Rhodes electric piano with chorus, gentle nylon/acoustic fingerpicking, mellow analog sub-bass, soft brushed kit, subtle tape hiss and cozy cabin intimacy. Safe harbor between space missions, relaxed, uncluttered mids for companion dialogue, seamless loop.`
- **Exclude Styles:** `vocals, harsh distortion, cinematic brass climaxes, alarm sounds, singing`

### `music_astra_bridge` — Flight Bridge (Charting the Frontier)
- **Recommended Tool:** `elevenlabs` | **Key:** A Minor | **BPM:** 88 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_orion.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_orion.mid`, `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `Warm analog space synthwave and downtempo navigation underscore, 88 BPM, A minor. Steady arpeggiated analog synth sequencer, clean muted electric guitar plucks with tape delay, subtle retro radar tones, gentle bass pulse, wide vintage stereo shimmer. Focused, hopeful, restrained for UI clarity, seamless loop.`
- **Exclude Styles:** `vocals, combat drums, heavy metal guitars, loud risers, singing`

### `music_victory_standard` — Victory Fanfare (Battle Won)
- **Recommended Tool:** `suno` | **Key:** D Major | **BPM:** 126 | **Length:** ~20s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `16-bit and 80s analog JRPG victory fanfare, 126 BPM, D major. Punchy brass-synth stabs quoting a bright ascending 5-note heroic hook, crisp electric guitar cadence, tight bass and rock drum groove settling into a celebratory loopable/clean ending cadence.`
- **Exclude Styles:** `vocals, dark minor chords, ambient drone, singing`

### `music_victory_boss` — Major Victory (Breakthrough)
- **Recommended Tool:** `suno` | **Key:** D Major | **BPM:** 128 | **Length:** ~30s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `Triumphant JRPG major boss victory fanfare evolving into uplifting analog synth-orchestral relief, 128 BPM, D major. Soaring electric guitar and warm brass-strings answering a heroic 5-note ascending cadence, resonant timpani, warm analog tape sheen.`
- **Exclude Styles:** `vocals, tragic minor ending, singing`

### `music_game_over` — The Signal Fades (Defeat)
- **Recommended Tool:** `elevenlabs` | **Key:** D Minor | **BPM:** 60 | **Length:** ~24s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `Poetic somber JRPG defeat cue, 60 BPM, D minor. Felt upright piano playing a slow fading broken motif, low cello sigh, soft cassette tape deceleration and quiet radio static tail. Reflective, fragile, never loud.`
- **Exclude Styles:** `vocals, drums, cheerful major chords, harsh jump scare, singing`

## Category: CHARACTER (9 tracks)

### `music_nova_theme` — Nova's Theme (The Spark)
- **Recommended Tool:** `suno` | **Key:** D Minor -> D Major | **BPM:** 92 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `Expressive JRPG character theme for a gritty 22-year-old mechanic, 92 BPM, D minor resolving to D major. Fingerpicked resonator acoustic guitar lead carrying a stubborn ascending 5-note melody, backed by warm cello, dusty brushed snare, upright bass, and subtle Juno synth warmth. Resilient, practical, quietly tender.`
- **Exclude Styles:** `vocals, pompous royalty fanfare, modern EDM, singing`

### `music_jed_theme` — Jed's Theme (The Fixer's Gift)
- **Recommended Tool:** `suno` | **Key:** G Major | **BPM:** 76 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_jed.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_jed.mid`
- **Style Prompt:** `Intimate warm frontier mentor theme, 76 BPM, G major. Solo nylon/steel acoustic guitar and dusty mellow harmonica playing a gentle falling plagal cadence, soft upright bass, subtle workbench room air and analog tape warmth. Unhurried, caring, blue-collar grace.`
- **Exclude Styles:** `vocals, distortion, fast percussion, synthwave drops, singing`

### `music_zeke_theme` — Zeke's Theme (Unbroken Iron)
- **Recommended Tool:** `suno` | **Key:** E Minor | **BPM:** 84 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_zeke.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_zeke.mid`
- **Style Prompt:** `Soulful working-class blues character theme, 84 BPM, E minor. Warm Fender Rhodes chords, gritty overdriven P-bass walking line, understated slide guitar fills, crisp dry rimshot groove, analog cassette warmth. Loyal, self-deprecating courage rising from nervous tension.`
- **Exclude Styles:** `vocals, metal blast beats, glossy EDM, singing`

### `music_orion_theme` — Orion's Theme (Memory of Aethel)
- **Recommended Tool:** `suno` | **Key:** A Dorian | **BPM:** 70 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_orion.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_orion.mid`, `audio_studio/midi_motifs/motif_source.mid`
- **Style Prompt:** `Ancient elegiac chamber-folk theme in 3/4 time, 70 BPM, A Dorian. Lyrical wooden flute and deep solo cello weaving over resonant Tibetan singing bowls, delicate harp arpeggios, and wide sacred reverb. Dignified grief softening into quiet guidance.`
- **Exclude Styles:** `vocals, electronic drum beats, distorted guitars, singing`

### `music_gh0st_theme` — Gh0st's Theme (The Phantom Trace)
- **Recommended Tool:** `suno` | **Key:** F# Minor | **BPM:** 104 | **Length:** ~92s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_gh0st.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_gh0st.mid`, `audio_studio/midi_motifs/motif_elara.mid`
- **Style Prompt:** `Solitary stealth cyber-noir theme, 104 BPM, F# minor. Clean muted electric guitar arpeggios through dark analog tape delay, deep fretless sub-bass, tight dry electronic rim clicks, faint warm glass celesta harmonic answer hinting at lost humanity. Restrained, watchful, protective.`
- **Exclude Styles:** `vocals, loud metal guitars, festive brass, singing`

### `music_elaras_song` — Elara's Song (The Pure Voice)
- **Recommended Tool:** `suno` | **Key:** F# Major | **BPM:** 84 | **Length:** ~120s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_elara.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_elara.mid`
- **Style Prompt:** `Heartbreaking 1980s 4-track cassette folk lullaby in 3/4 waltz time, 84 BPM, F# major. Fingerpicked acoustic guitar, delicate music box celesta, warm chorus bass, gentle tape flutter and hiss, with a soft breathy pure female soprano singing a tender wordless/short lullaby about starlight and keeping the door open.`
- **Exclude Styles:** `heavy drums, club beat, operatic belting, modern autotune`

### `music_thorne_theme` — Thorne's Theme (Closed Market)
- **Recommended Tool:** `suno` | **Key:** Bb Minor | **BPM:** 98 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_thorne.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_thorne.mid`
- **Style Prompt:** `Cold executive corporate noir theme, 98 BPM, Bb minor. Sleek muted analog synth brass, glassy marimba ostinato, crisp dry electronic hi-hats, deep sub-bass stride, polished sterile lounge reverb. Confident, calculating monopoly.`
- **Exclude Styles:** `vocals, warm rustic acoustic guitar, heavy rock drums, singing`

### `music_vale_theme` — Vale's Theme (The Soloist)
- **Recommended Tool:** `suno` | **Key:** C Minor | **BPM:** 80 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_vale.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_vale.mid`
- **Style Prompt:** `Hauntingly calm minimalist chamber theme, 80 BPM, C minor. High-register sterile solo piano playing a slow 4-note descending chromatic line (C-B-Bb-A), supported by cold sustained cello harmonics and distant low pipe organ swell. Merciless perfection, quiet ideological certainty.`
- **Exclude Styles:** `vocals, blues groove, playful melodies, loud rock drums, singing`

### `music_theme_emotional` — Shared Ground (Emotional Theme)
- **Recommended Tool:** `suno` | **Key:** D Minor -> G Major | **BPM:** 68 | **Length:** ~90s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_jed.mid`
- **Style Prompt:** `Tender bittersweet JRPG acoustic scene score, 68 BPM. Intimate fingerpicked acoustic guitar carrying Nova's warm melody answered by expressive solo cello and soft felt piano chords, subtle analog room warmth. Unspoken loyalty and quiet grief.`
- **Exclude Styles:** `vocals, drums, electronic drops, singing`

## Category: STINGER (5 tracks)

### `stinger_motif_nova` — Motif Stinger: Nova's Spark
- **Recommended Tool:** `elevenlabs` | **Key:** D Minor | **BPM:** 92 | **Length:** ~6s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `Short 6-second JRPG musical motif phrase, 92 BPM, D minor. Clean resonant acoustic guitar playing an ascending 5-note determined melody supported by a single warm synth swell, ending with natural clean decay.`
- **Exclude Styles:** `vocals, drums, harsh distortion, singing`

### `stinger_motif_jed` — Motif Stinger: Jed's Workbench
- **Recommended Tool:** `elevenlabs` | **Key:** G Major | **BPM:** 76 | **Length:** ~6s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_jed.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_jed.mid`
- **Style Prompt:** `Short 6-second warm acoustic guitar and soft harmonica musical phrase in G major, 76 BPM. Gentle falling plagal cadence, dusty workshop warmth, clean natural ring-out.`
- **Exclude Styles:** `vocals, drums, synth bass, singing`

### `stinger_motif_elara` — Motif Stinger: Elara's Signal
- **Recommended Tool:** `elevenlabs` | **Key:** F# Major | **BPM:** 84 | **Length:** ~6s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_elara.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_elara.mid`
- **Style Prompt:** `Short 6-second delicate glass celesta and music-box lullaby phrase in F# major 3/4 time with faint 80s tape flutter and distant crystalline reverb tail.`
- **Exclude Styles:** `vocals, percussion, bass, singing`

### `stinger_motif_vale` — Motif Stinger: Soloist Signal
- **Recommended Tool:** `elevenlabs` | **Key:** C Minor | **BPM:** 80 | **Length:** ~6s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_vale.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_vale.mid`
- **Style Prompt:** `Short 6-second chilling solo cold piano and low string chime in C minor playing a slow 4-note descending chromatic line, sterile hall decay.`
- **Exclude Styles:** `vocals, warm guitar, rock drums, singing`

### `stinger_motif_source` — Motif Stinger: Echo Resonance
- **Recommended Tool:** `elevenlabs` | **Key:** A Dorian | **BPM:** 64 | **Length:** ~6s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_source.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_source.mid`
- **Style Prompt:** `Short 6-second ethereal Tibetan singing bowl and glass armonica chord bloom in A Dorian, shimmering cosmic resonance, clean cathedral tail.`
- **Exclude Styles:** `vocals, drums, distortion, singing`

## Category: W1 (4 tracks)

### `music_w1_homestead_explore` — Stellarium Colony (Homestead & Trade Row)
- **Recommended Tool:** `suno` | **Key:** D Minor | **BPM:** 86 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/pilot_music_w1_homestead_explore.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_zeke.mid`
- **Style Prompt:** `Dusty industrial blues RPG exploration underscore, 86 BPM, D minor. Dry close-miked resonator steel-string guitar carrying Nova's restrained ascending D-F-G-A motif, subtle dusty harmonica and warm Rhodes harmony, muted metallic pipe percussion, warm P-bass, subtle analog tape grit. Working-class survival, restrained dynamics for dialogue, seamless loop.`
- **Exclude Styles:** `vocals, huge cathedral reverb, epic brass, metal drums, singing`

### `music_w1_jed_workshop` — Jed's Workshop (Warm Steel & Grease)
- **Recommended Tool:** `suno` | **Key:** G Major | **BPM:** 76 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_jed.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_jed.mid`
- **Style Prompt:** `Intimate warm acoustic blues-folk workshop theme quoting Jed's Theme, 76 BPM, G major. Fingerpicked acoustic steel-string guitar, soft warm harmonica counter-phrase, gentle brushed percussion, subtle analog warmth. Safe, practical mentorship inside a dusty colony, seamless loop.`
- **Exclude Styles:** `vocals, distortion, heavy drums, scary drone, singing`

### `music_w1_logistics_explore` — Logistics Sector (Quota & Concrete)
- **Recommended Tool:** `elevenlabs` | **Key:** E Minor | **BPM:** 90 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_zeke.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_zeke.mid`, `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `Industrial blues-noir RPG exploration underscore, 90 BPM, E minor. Dry muted electric guitar ostinato, low steady P-bass pulse, rhythmic pneumatic industrial clicks, distant Rhodes chords, dusty analog tape grain. Watchful, pressured corporate facility, uncluttered mids, seamless loop.`
- **Exclude Styles:** `vocals, loud guitar solos, happy upbeat pop, singing`

### `music_w1_deep_mine` — Deep Mine & Echo Chamber (Sub-Level 4)
- **Recommended Tool:** `elevenlabs` | **Key:** C Minor / A Modal | **BPM:** 72 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_source.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_source.mid`, `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `Dark industrial subterranean exploration underscore, 72 BPM. Deep cavern bass pulse, slow metallic conduit clinks in cave reverb, sparse low acoustic guitar harmonics, and faint mysterious crystalline glass-chime echoes hinting at something ancient buried below. Ominous, restrained, seamless loop.`
- **Exclude Styles:** `vocals, battle drums, bright brass, singing`

## Category: W2 (4 tracks)

### `music_w2_sector9_explore` — Sector 9 Wilds (Wreckage in Bloom)
- **Recommended Tool:** `suno` | **Key:** A Dorian | **BPM:** 76 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_source.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_source.mid`, `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `Organic dark folk and atmospheric sci-fi jungle exploration, 76 BPM, A Dorian. 12-string acoustic guitar picking, breathy wooden flute melodies, bowed contrabass, soft hand frame drums, warm analog poly-synth swells. Lush, humid, mysterious nature reclaiming ruins, seamless loop.`
- **Exclude Styles:** `vocals, heavy metal, EDM drops, singing`

### `music_w2_tideglass_beach` — Tideglass Beach (Glass Sand & Driftwood)
- **Recommended Tool:** `suno` | **Key:** A Dorian / G Major | **BPM:** 72 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_source.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_source.mid`, `audio_studio/midi_motifs/motif_orion.mid`
- **Style Prompt:** `Serene shimmering coastal ambient folk, 72 BPM, A Dorian. Warm nylon guitar arpeggios, soft Fender Rhodes swells, delicate glass chimes, solo cello warmth, ocean-like wide analog pad shimmer. Restful wonder and quiet camaraderie after the crash, seamless loop.`
- **Exclude Styles:** `vocals, aggressive drums, scary horror noise, singing`

### `music_w2_ancient_gateway` — Sanctuary Ruins (Hall of Echoes)
- **Recommended Tool:** `suno` | **Key:** A Dorian / F# Minor | **BPM:** 78 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_orion.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_orion.mid`, `audio_studio/midi_motifs/motif_source.mid`
- **Style Prompt:** `Sacred ancient sci-fi ruins exploration theme, 78 BPM, A Dorian. Resonant singing bowls, kalimba/harp patterns, expressive wooden flute echoing Orion's motif, deep frame drum pulse, wide cathedral reverb. Reverent, uncanny, alive with ancient memory, seamless loop.`
- **Exclude Styles:** `vocals, electric metal guitars, trap percussion, singing`

### `music_w2_stasis_sanctuary` — Stasis Chamber (The Sleeping Archive)
- **Recommended Tool:** `elevenlabs` | **Key:** A Dorian | **BPM:** 66 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_orion.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_orion.mid`
- **Style Prompt:** `Quiet icy-sacred stasis vault underscore, 66 BPM, A Dorian. High glass harmonic drones, sparse solo cello notes quoting Orion's motif, soft rhythmic cryogenic pulse, vast hushed stone acoustics, seamless loop.`
- **Exclude Styles:** `vocals, heavy drums, fast tempos, singing`

## Category: W3 (4 tracks)

### `music_w3_spire_explore` — Lower Spire (Neon Rain & Night Market)
- **Recommended Tool:** `suno` | **Key:** C# Minor / E Minor | **BPM:** 100 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_zeke.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_zeke.mid`, `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `Rain-slicked 80s cyber-noir jazz-synthwave exploration, 100 BPM, C# minor. Slick fretless bassline, warm Fender Rhodes chords, smoky muted saxophone phrases with tape delay, crisp brushed electronic snare, neon analog pads. Hustling, street-smart, covert, seamless loop.`
- **Exclude Styles:** `vocals, loud dubstep drops, heavy metal, singing`

### `music_w3_the_static` — The Static & Safehouse (Signal Under the Rail)
- **Recommended Tool:** `suno` | **Key:** E Minor | **BPM:** 86 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_zeke.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_zeke.mid`, `audio_studio/midi_motifs/motif_gh0st.mid`
- **Style Prompt:** `Cozy underground cyberpunk safehouse lo-fi jazz-synth, 86 BPM, E minor. Warm upright/Rhodes chords, mellow muted guitar, faint vinyl/tape radio static, soft sub-pulse. Crew regrouping in a hidden apartment above the rail, intimate and loyal, seamless loop.`
- **Exclude Styles:** `vocals, harsh alarms, battle drums, singing`

### `music_w3_upper_city` — Upper Spire (Glasswalk & Politeness)
- **Recommended Tool:** `suno` | **Key:** Bb Minor | **BPM:** 98 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_thorne.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_thorne.mid`
- **Style Prompt:** `Sleek minimalist cyber-luxury lounge & corporate espionage, 98 BPM, Bb minor. Crystal synthesizer arpeggios, glassy vibraphone chords, cool sterile string swells, deep polished bass pulse, polite clockwork hi-hats. Opulent yet quietly menacing, seamless loop.`
- **Exclude Styles:** `vocals, dirty blues guitar, aggressive screams, singing`

### `music_w3_archive_stealth` — Spire Archive (Prism Vault Heist)
- **Recommended Tool:** `elevenlabs` | **Key:** Bb Minor / F# Minor | **BPM:** 108 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_thorne.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_thorne.mid`, `audio_studio/midi_motifs/motif_gh0st.mid`
- **Style Prompt:** `Taut high-stakes sci-fi stealth infiltration underscore, 108 BPM, Bb minor. Ticking precision analog sequencer, filtered sub-bass pulse, muted pizzicato strings, restrained glass synth pings. Clean, suspenseful heist focus without battle drums, seamless loop.`
- **Exclude Styles:** `vocals, loud guitar solos, dramatic endings, singing`

## Category: W4 (3 tracks)

### `music_w4_foundry_explore` — Slag Pits (Molten Channels)
- **Recommended Tool:** `suno` | **Key:** D Minor | **BPM:** 112 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_zeke.mid`
- **Style Prompt:** `Grinding industrial blues-rock exploration underscore, 112 BPM, D minor. Low chugging palm-muted guitars, heavy hydraulic machine rhythm, simmering analog bass drone, occasional dark slide harmonics. Oppressive furnace heat, relentless working machinery, seamless loop.`
- **Exclude Styles:** `vocals, cheerful pop, bright bells, singing`

### `music_w4_power_core` — Assembly Line & Deep Forge
- **Recommended Tool:** `suno` | **Key:** B Minor | **BPM:** 122 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_vale.mid`
- **Style Prompt:** `Driven dark industrial rock & synth-metal exploration, 122 BPM, B minor. Mechanized conveyor rhythm, crunchy bass synth, sharp metallic percussion, tense electric guitar motifs and urgent analog arpeggios. Volatile reactor tension, seamless loop.`
- **Exclude Styles:** `vocals, serene acoustic folk, singing`

### `music_w4_conditioning` — Conditioning Wing (Unit 734)
- **Recommended Tool:** `suno` | **Key:** F# Minor | **BPM:** 94 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_gh0st.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_gh0st.mid`, `audio_studio/midi_motifs/motif_elara.mid`
- **Style Prompt:** `Cold unsettling industrial-noir theme echoing Gh0st's Theme, 94 BPM, F# minor. Mechanical assembly clicks, dark tape-delayed clean guitar arpeggios, hollow cold synth choir hum, faint broken music-box celesta overtones. Personal reckoning with manufactured origins, seamless loop.`
- **Exclude Styles:** `vocals, upbeat groove, heroic fanfare, singing`

## Category: W5 (4 tracks)

### `music_w5_orbital_intrigue` — Grand Concourse (Borrowed Time)
- **Recommended Tool:** `elevenlabs` | **Key:** C Minor | **BPM:** 80 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_vale.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_vale.mid`, `audio_studio/midi_motifs/motif_zeke.mid`
- **Style Prompt:** `Restrained orbital espionage and uneasy human resolve, 80 BPM, C minor, 4/4 underscore. Warm analog sequencer eighth-notes, rounded low synth bass, light dry electronic ticks, narrow cold radar tones answering muted electric-piano chords, subtle C-B-Bb-A figure. Uncluttered mids for dialogue, seamless loop.`
- **Exclude Styles:** `vocals, alarm sirens, battle drums, triumphant climax, singing`

### `music_w5_solarium` — Orbital Solarium (Artificial Paradise)
- **Recommended Tool:** `suno` | **Key:** C Minor / Eb Major | **BPM:** 74 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_thorne.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_thorne.mid`, `audio_studio/midi_motifs/motif_vale.mid`
- **Style Prompt:** `Uncanny pristine chamber waltz-ambient inside a gilded orbital cage, 74 BPM. Too-clean glass harp, cool celesta, delicate string quartet harmonics laced with subtle dissonant bass undertone. Artificial beauty masking trapped lives, seamless loop.`
- **Exclude Styles:** `vocals, distortion, heavy drums, singing`

### `music_w5_security_hub` — Deep Ring Mainframe (Silent Surveillance)
- **Recommended Tool:** `elevenlabs` | **Key:** G# Minor / C Minor | **BPM:** 84 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_vale.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_vale.mid`
- **Style Prompt:** `Cold minimalist sci-fi chamber electronica, 84 BPM, C minor. High sterile felt piano notes echoing in server coldness, ticking clock percussion, pulsing square-wave sub, icy string sustains. Clinical surveillance pressure, seamless loop.`
- **Exclude Styles:** `vocals, warm acoustic guitar, celebratory brass, singing`

### `music_w5_anchor_cathedral` — Anchor Containment & Command Deck (One Breath)
- **Recommended Tool:** `suno` | **Key:** C Minor / F# Major tension | **BPM:** 72 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_vale.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_vale.mid`, `audio_studio/midi_motifs/motif_elara.mid`, `audio_studio/midi_motifs/motif_gh0st.mid`
- **Style Prompt:** `Chilling neo-classical sci-fi suspense, 72 BPM, C minor. Sparse cold pipe organ drone and descending piano notes colliding with fragile high celesta harmonics and taut string tremolos. Approaching Vale's Tear while holding onto Elara, seamless loop.`
- **Exclude Styles:** `vocals, dance beat, cozy lounge, singing`

## Category: W6 (7 tracks)

### `music_w6_campfire` — The Gathering Fire (Still Ourselves)
- **Recommended Tool:** `suno` | **Key:** D Minor / G Major | **BPM:** 68 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_jed.mid`
- **Style Prompt:** `Intimate fragile acoustic hearth inside surreal cosmic emptiness, 68 BPM, D minor. Solo warm fingerpicked acoustic guitar and soft cello duet echoing Nova and Jed's warmth surrounded by faint distant glass harmonic breeze. Quiet refuge before walking into nightmares, seamless loop.`
- **Exclude Styles:** `vocals, percussion, industrial metal, singing`

### `music_w6_nightmare_zeke` — Zeke's Nightmare (Endless Performance Review)
- **Recommended Tool:** `suno` | **Key:** E Minor | **BPM:** 78 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_zeke.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_zeke.mid`, `audio_studio/midi_motifs/motif_vale.mid`
- **Style Prompt:** `Warped surreal dark-jazz nightmare variation of Zeke's Rhodes theme, 78 BPM, E minor. Detuned tape-wobbly Fender Rhodes, oppressive ticking office clockwork, claustrophobic low bass drone, muffled checkpoint buzzer textures. Psychological bureaucracy nightmare, seamless loop.`
- **Exclude Styles:** `vocals, triumphant rock, bright pop, singing`

### `music_w6_nightmare_gh0st` — Gh0st's Nightmare (Kill-Suite Loop)
- **Recommended Tool:** `suno` | **Key:** F# Minor -> F# Major | **BPM:** 96 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_gh0st.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_gh0st.mid`, `audio_studio/midi_motifs/motif_elara.mid`
- **Style Prompt:** `Haunting glitch-noir and fractured memory variation of Gh0st's Theme, 96 BPM, F# minor. Mechanical glitch stutters and dark delay guitar locked in a loop, pierced from underneath by a clear stubborn F# major celesta fragment of Elara's lullaby. Emotional defiance inside command noise, seamless loop.`
- **Exclude Styles:** `vocals, happy funk, comedy, singing`

### `music_w6_nightmare_orion` — Orion's Nightmare (The Silent Shore)
- **Recommended Tool:** `suno` | **Key:** A Dorian | **BPM:** 62 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_orion.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_orion.mid`, `audio_studio/midi_motifs/motif_source.mid`
- **Style Prompt:** `Hollow surreal elegy in 3/4 time echoing Orion's Theme, 62 BPM, A Dorian. Distant bowed cello harmonics, reversed tape swells, lonely solo wood flute answers fading into vast empty ocean-cavern reverb. Grief tempting him to fade away, seamless loop.`
- **Exclude Styles:** `vocals, drums, heavy metal, singing`

### `music_w6_source_explore` — World-Fracture & Memory Stair (Fragments)
- **Recommended Tool:** `suno` | **Key:** A Modal / D Minor | **BPM:** 64 | **Length:** ~100s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_source.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_source.mid`, `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_elara.mid`
- **Style Prompt:** `Surreal transcendental ambient-orchestral journey, 64 BPM, A Dorian/D minor. Ethereal wordless choral shimmer, glass armonica, fragmented acoustic guitar and celesta echoes drifting across reverse analog tape swells and deep sub-harmonic tides. Profound revelation, seamless loop.`
- **Exclude Styles:** `modern EDM drops, upbeat pop, harsh synthwave, vocals, singing`

### `music_w6_the_center` — The White Shore (Heart of the Source)
- **Recommended Tool:** `suno` | **Key:** D Modal / C Minor tension | **BPM:** 70 | **Length:** ~100s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_source.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_source.mid`, `audio_studio/midi_motifs/motif_vale.mid`
- **Style Prompt:** `Monumental sacred choral and pipe organ underscore at the edge of reality, 70 BPM. Vast polyphonic wordless choir, cathedral pipe organ, crystalline singing bowls, taut low orchestral strings building suspended awe before the final confrontation, seamless loop.`
- **Exclude Styles:** `drums, rock guitar, playful instruments, vocals, singing`

### `music_w6_new_world` — Beyond the Silence (Regional Opening)
- **Recommended Tool:** `suno` | **Key:** D Major | **BPM:** 76 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_jed.mid`
- **Style Prompt:** `Radiant grounded sunrise acoustic-orchestral resolution, 76 BPM, D major. Warm steel-string acoustic guitar playing Nova's resolved major motif, lush warm strings, gentle Rhodes, faint natural morning air. Human survival, openness, and quiet hope, seamless loop.`
- **Exclude Styles:** `vocals, aggressive drums, ominous dissonance, singing`

## Category: COMBAT (6 tracks)

### `music_w1_combat` — Homestead Scrap Skirmish
- **Recommended Tool:** `suno` | **Key:** D Minor | **BPM:** 138 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/pilot_music_w1_combat.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_zeke.mid`
- **Style Prompt:** `High-octane 80s analog blues-rock JRPG battle theme, 138 BPM, D minor. Overdriven resonator and lead electric guitar propelling Nova's ascending D-F-G-A battle hook, punchy live rock drum kit, fat gritty P-bass groove, rhythmic metallic clanks. Gritty working-class adrenaline, seamless loop.`
- **Exclude Styles:** `vocals, modern dubstep, serene ambient, singing`

### `music_w2_combat` — Swamp Ambush
- **Recommended Tool:** `suno` | **Key:** A Dorian | **BPM:** 132 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_orion.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_orion.mid`, `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `Energetic organic folk-rock & tribal orchestral JRPG battle theme, 132 BPM, A Dorian. Driving taiko & frame drums, urgent bowed string ostinato, electric bass groove, wild wooden flute trills and crunchy rhythmic guitar. Primal jungle survival, seamless loop.`
- **Exclude Styles:** `vocals, cheesy EDM, slow lullaby, singing`

### `music_w3_combat` — Spire Security Chase
- **Recommended Tool:** `suno` | **Key:** C# Minor | **BPM:** 136 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_zeke.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_zeke.mid`, `audio_studio/midi_motifs/motif_gh0st.mid`
- **Style Prompt:** `High-energy 80s darksynth-funk JRPG battle theme, 136 BPM, C# minor. Snapping fretless slap bass, pulsing analog saw leads, punchy retro electronic/rock drums, gritty organ/Rhodes stabs. Slick urban heist getaway under laser fire, seamless loop.`
- **Exclude Styles:** `vocals, slow ambient, rustic banjo, singing`

### `music_w4_combat` — Foundry Line Battle
- **Recommended Tool:** `suno` | **Key:** D Minor | **BPM:** 144 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_gh0st.mid`
- **Style Prompt:** `Relentless industrial metal JRPG combat theme, 144 BPM, D minor. Down-tuned chugging metal guitars, pneumatic hammer percussion, ferocious double-kick rock drums, screaming analog synth bass, blazing lead guitar melody. Blistering molten combat, seamless loop.`
- **Exclude Styles:** `vocals, calm ambient, soft jazz, singing`

### `music_w5_combat` — Zero-G Skirmish
- **Recommended Tool:** `suno` | **Key:** C Minor | **BPM:** 154 | **Length:** ~90s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_vale.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_vale.mid`, `audio_studio/midi_motifs/motif_gh0st.mid`
- **Style Prompt:** `Cold neo-classical chamber drum-and-bass / breakbeat JRPG battle, 154 BPM, C minor. Rapid clipped breakbeats, frantic chamber string ostinato, sharp harpsichord/piano runs, deep Reese bassline, clinical synth stabs. Surgical precision inside orbital security, seamless loop.`
- **Exclude Styles:** `vocals, rustic blues, cozy jazz, singing`

### `music_w6_combat` — Fracture Echo Battle
- **Recommended Tool:** `suno` | **Key:** A Dorian / D Minor | **BPM:** 144 | **Length:** ~96s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_source.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_source.mid`, `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `Transcendental symphonic rock JRPG battle, 144 BPM, D minor. Soaring electric lead guitar and dramatic string orchestra, wordless choral swells, thunderous live drums, crystalline glass resonance hits. Battling living memories inside the Tear, seamless loop.`
- **Exclude Styles:** `pop lyrics, comedic brass, vocals, singing`

## Category: BOSS (8 tracks)

### `music_w1_boss_warden` — The Iron Warden (Heavy Duty)
- **Recommended Tool:** `suno` | **Key:** C Minor / D Minor | **BPM:** 142 | **Length:** ~120s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/pilot_music_w1_boss_warden.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_vale.mid`
- **Style Prompt:** `Heavy industrial hard-rock JRPG boss battle, 142 BPM, C minor. Crushing distorted guitar power chords, pounding anvil hits, screeching analog synth sirens, driving double-kick drums, defiant lead guitar quoting Nova's ascending hook. Overwhelming armored corporate enforcer, seamless loop.`
- **Exclude Styles:** `vocals, peaceful flute, jazz lounge, singing`

### `music_w2_boss_guardian` — The Source Beast (Canopy Apex)
- **Recommended Tool:** `suno` | **Key:** D Minor / A Dorian | **BPM:** 140 | **Length:** ~120s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_source.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_source.mid`, `audio_studio/midi_motifs/motif_orion.mid`
- **Style Prompt:** `Epic orchestral-rock JRPG boss theme, 140 BPM, D minor. Thunderous tribal war drums, relentless cello & violin ostinato, roaring brass stabs, shimmering resonant glass overtones and electric guitar counter-melody. Terrifying ancient predator warped by the Source, seamless loop.`
- **Exclude Styles:** `vocals, funky slap bass, retro chiptune, singing`

### `music_w3_boss_administrator` — The Administrator (Polite Annihilation)
- **Recommended Tool:** `suno` | **Key:** Bb Minor | **BPM:** 142 | **Length:** ~120s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_thorne.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_thorne.mid`
- **Style Prompt:** `Sleek terrifying cyber-luxury JRPG boss theme, 142 BPM, Bb minor. Blistering glass synth arpeggios, cold analog brass stabs quoting Thorne's Bbm theme, razor-sharp electronic breakbeats, distorted sub-bass, soaring electric guitar duel. High-society security AI smiling while venting the room, seamless loop.`
- **Exclude Styles:** `vocals, warm folk guitar, singing`

### `music_w3_boss_phantom` — Phantom Prototype (Phase Blade Duel)
- **Recommended Tool:** `suno` | **Key:** F# Minor | **BPM:** 146 | **Length:** ~120s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_gh0st.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_gh0st.mid`, `audio_studio/midi_motifs/motif_elara.mid`
- **Style Prompt:** `Hyper-kinetic darkwave synth-metal duel quoting Gh0st's F# minor motif, 146 BPM. Dueling phase-shifted electric guitars, blistering 16th-note analog arpeggios, tight breakbeat rock drums, ghostly celesta echoes flickering underneath. Facing a hollow copy built without a self, seamless loop.`
- **Exclude Styles:** `vocals, slow ballads, singing`

### `music_w4_boss_titan` — Titan Walker & Crucible (Molten Siege)
- **Recommended Tool:** `suno` | **Key:** B Minor / D Minor | **BPM:** 150 | **Length:** ~130s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_gh0st.mid`
- **Style Prompt:** `Colossal industrial symphonic-metal boss battle, 150 BPM, B minor. Multi-ton hydraulic stomps, scorching twin electric guitar solos quoting Nova's defiant theme, blaring industrial brass horns, relentless double-kick drumming, molten analog synth sweeps. Fighting Commander Rylos's siege frame over a volcano vent, seamless loop.`
- **Exclude Styles:** `vocals, delicate music box, singing`

### `music_w5_boss_compliance` — Compliance Avatar (Orbital Lockdown)
- **Recommended Tool:** `suno` | **Key:** C Minor | **BPM:** 148 | **Length:** ~120s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_vale.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_vale.mid`, `audio_studio/midi_motifs/motif_thorne.mid`
- **Style Prompt:** `Clinical authoritarian neo-classical synth-metal boss theme, 148 BPM, C minor. Rapid pipe organ and harpsichord ostinatos, chugging metal guitars, robotic snare precision, relentless 4-note C-B-Bb-A chromatic horn stabs. Vale's orbital compliance machine turning deadly, seamless loop.`
- **Exclude Styles:** `vocals, warm country blues, singing`

### `music_w6_boss_final` — Ascended Vale: The Soloist (Phase 1)
- **Recommended Tool:** `suno` | **Key:** C Minor <-> D Minor | **BPM:** 152 | **Length:** ~150s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_vale.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_vale.mid`, `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_source.mid`
- **Style Prompt:** `Grand JRPG symphonic rock final boss Part 1, 152 BPM, C minor clashing against D minor. Massive church organ and cold unison choir pounding Vale's 4-note chromatic descent, answered defiantly by soaring lead electric guitar and passionate orchestral strings carrying Nova's rising motif. Ideological clash of wills, seamless loop.`
- **Exclude Styles:** `pop vocals, lo-fi chill, vocals, singing`

### `music_w6_boss_vale_p2` — Ascended Silence (Final Climax Phase 2)
- **Recommended Tool:** `suno` | **Key:** C Minor -> D Major | **BPM:** 158 | **Length:** ~160s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_vale.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_vale.mid`, `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_elara.mid`, `audio_studio/midi_motifs/motif_source.mid`
- **Style Prompt:** `Towering all-out JRPG final boss climax symphony, 158 BPM, C minor erupting into blazing D major. Full 60-piece orchestra, dramatic wordless operatic choir, pipe organ, blistering electric guitar heroics, and glimmering celesta resonance uniting the whole crew's circuit against absolute silence, seamless loop.`
- **Exclude Styles:** `modern EDM trap, dry minimalist silence, vocals, singing`

## Category: CINEMATIC (6 tracks)

### `music_intro_breach` — Sector 9 Breach (Cold Open)
- **Recommended Tool:** `elevenlabs` | **Key:** A Dorian -> D Minor | **BPM:** 135 | **Length:** ~32s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_orion.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_orion.mid`, `audio_studio/midi_motifs/motif_source.mid`, `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `32-second high-stakes sci-fi JRPG cold-open cinematic score, 135 BPM. Urgent pulsing strings and containment breach tension (0-14s), dropping suddenly into shimmering sacred wide-stereo singing bowl & choir chord (14-22s), shocking dissonance impact (23s), resolving into solo acoustic guitar playing Nova's 5-note rising theme.`
- **Exclude Styles:** `vocals, upbeat pop, singing`

### `music_crash_flight` — Pod Trajectory & Shield Impact
- **Recommended Tool:** `elevenlabs` | **Key:** D Minor | **BPM:** 140 | **Length:** ~45s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_zeke.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_zeke.mid`, `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `45-second escalating sci-fi escape pod crash score, 140 BPM, D minor. Accelerating urgent string ostinatos, analog alarm synths, rushing atmospheric wind roar, culminating in violent impact hit decaying into quiet rain.`
- **Exclude Styles:** `vocals, joyful brass, singing`

### `music_cinematic_prologue` — The Launch & The Fall
- **Recommended Tool:** `elevenlabs` | **Key:** D Minor | **BPM:** 78 | **Length:** ~45s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_jed.mid`
- **Style Prompt:** `45-second emotional narrative cinematic cue, 78 BPM, D minor. Solo acoustic guitar evolving into warm French horn and urgent strings, abrupt radio crackle tension.`
- **Exclude Styles:** `vocals, trap drums, singing`

### `music_cinematic_crash` — Planetary Impact
- **Recommended Tool:** `elevenlabs` | **Key:** C Drone | **BPM:** 52 | **Length:** ~36s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_source.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_source.mid`
- **Style Prompt:** `36-second atmospheric crash aftermath score, 52 BPM. Sub-bass impact shock, descending cello harmonics, ear-ringing glass frequency fading softly into humid jungle rain ambience.`
- **Exclude Styles:** `vocals, fast percussion, singing`

### `music_epilogue` — Morning in the Colony (Epilogue)
- **Recommended Tool:** `suno` | **Key:** G Major / D Major | **BPM:** 70 | **Length:** ~140s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_jed.mid`, `audio_studio/midi_motifs/motif_zeke.mid`
- **Style Prompt:** `Heartfelt grounded JRPG epilogue piece, 70 BPM, G major into D major. Intimate fingerpicked acoustic guitar, warm Fender Rhodes chords, gentle nostalgic harmonica breath honoring Jed, soft warm strings, plus one faint delicate high glass harmonic echoing Nova's lasting tinnitus. Peaceful ordinary morning after survival.`
- **Exclude Styles:** `vocals, loud rock drums, dark horror, singing`

### `music_credits_ending` — The Great Frontier (End Credits Suite)
- **Recommended Tool:** `suno` | **Key:** D Major | **BPM:** 106 | **Length:** ~180s (One-Shot)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`, `audio_studio/midi_motifs/motif_zeke.mid`, `audio_studio/midi_motifs/motif_orion.mid`, `audio_studio/midi_motifs/motif_elara.mid`
- **Style Prompt:** `80s analog cassette end-credits soft-rock/synth-pop suite, 106 BPM, D major. Chiming 12-string acoustic guitar, warm Juno poly-synths, punchy 80s Simmons/acoustic drums, soulful saxophone solo, triumphant electric guitar reprise of Nova's rising hook. Warm joyful nostalgia looking toward open stars.`
- **Exclude Styles:** `harsh screamo, modern trap, vocals, singing`

## Category: ACTIVITIES (5 tracks)

### `music_fishing_ambient` — Tideglass Angler (Fishing)
- **Recommended Tool:** `suno` | **Key:** C Major | **BPM:** 72 | **Length:** ~80s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_jed.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_jed.mid`
- **Style Prompt:** `Relaxing acoustic water folk & cassette lo-fi fishing theme, 72 BPM, C major. Warm nylon-string fingerpicking, gentle wooden kalimba chimes, upright bass, shimmering shoreline synth haze. Peaceful meditative flow, seamless loop.`
- **Exclude Styles:** `vocals, heavy drums, distortion, singing`

### `music_arcade_cabinet` — Hyperion 1986 (Arcade Cabinet)
- **Recommended Tool:** `suno` | **Key:** F Major | **BPM:** 134 | **Length:** ~75s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_nova.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_nova.mid`
- **Style Prompt:** `Authentic 1986 coin-op FM synth & 8-bit chiptune arcade shooter theme, 134 BPM, F major. Bouncy square-wave lead melody, punchy FM slap bass, crisp noise-snare chiptune percussion, energetic arpeggios. Retro CRT speaker character, seamless loop.`
- **Exclude Styles:** `vocals, orchestral strings, modern trap, singing`

### `music_tinkering_focus` — Workstation Flow (Tinkering)
- **Recommended Tool:** `suno` | **Key:** G Major | **BPM:** 80 | **Length:** ~80s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_jed.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_jed.mid`
- **Style Prompt:** `Cozy mechanical lo-fi workshop groove quoting Jed's G major cadence, 80 BPM. Mellow Fender Rhodes chords, subtle rhythmic socket-wrench and brass clicks, muted guitar harmonics, warm analog bass. Focused creative tinkering, seamless loop.`
- **Exclude Styles:** `vocals, loud guitars, alarms, singing`

### `music_cooking_kitchen` — Mess Hall Stew (Cooking)
- **Recommended Tool:** `suno` | **Key:** Bb Major | **BPM:** 112 | **Length:** ~75s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_zeke.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_zeke.mid`
- **Style Prompt:** `Playful upbeat acoustic gypsy-jazz & kitchen swing, 112 BPM, Bb major. Bouncy acoustic rhythm guitars, cheerful pizzicato strings, upright walking bass, subtle wooden spoon percussion. Warm camaraderie around shared food, seamless loop.`
- **Exclude Styles:** `vocals, ominous synth, metal, singing`

### `music_shop_cozy` — Frontier Exchange (Markets & Shops)
- **Recommended Tool:** `suno` | **Key:** D Minor | **BPM:** 88 | **Length:** ~80s (Seamless Loop)
- **Reference Audio (.wav):** `audio_studio/reference_audio/motif_zeke.wav`
- **Motif MIDI Seeds:** `audio_studio/midi_motifs/motif_zeke.mid`
- **Style Prompt:** `Warm eclectic frontier trader lo-fi groove, 88 BPM, D minor. Acoustic lute/guitar riff, warm Fender Rhodes backing, gentle hand percussion, intriguing analog synth flute hook, analog tape saturation. Curious, street-smart trading post vibe, seamless loop.`
- **Exclude Styles:** `vocals, heavy combat drums, singing`
