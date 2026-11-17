# Starborn — Canonical Musical Bible & Production Guide

**Status:** Authoritative Audio Architecture & Production Spec (supersedes earlier conflicting audio drafts; original files archived in `tools/backups/pre_rebrand_audio_2026-11-17/`).
**Machine-Readable Source:** [musical_bible.json](../../audio_studio/musical_bible.json)
**Pipeline CLI:** [audio_studio.py](../../scripts/audio_studio.py)
**Immutable Assets:** Great Frontier VHS tapes (`gf_01_unpayable_debt` through `gf_10_shackles`) are **never modified**.

---

## 1. Sonic Philosophy: *"Cassette-Age Space Opera"*

Every cue in *Starborn* balances three narrative-acoustic elements across Worlds 1–6:
1. **Working-Class Humanity (Acoustic & Analog Warmth):** Resonator/dobro, steel-string and 12-string guitars, dusty harmonica, Fender Rhodes, upright/fretless P-bass, wooden flute, upright/felt piano.
2. **Dominion Control & 1980s Cassette-Futurism (Mechanical & Electronic):** Prophet/Juno analog synth pads, 80s electronic/rock percussion, clockwork sequencers, dry metallic relays, pneumatic bulkheads, oppressive pipe organ.
3. **Veyra / The Source (Harmonic Resonance):** Glass armonica, Tibetan singing bowls, celesta/music box, bowed strings, wordless vowel choirs, wide stereo shimmer.

Per [Lore_Bible.md](../../docs/story/Lore_Bible.md), **The Skin** sounds dry, close-miked, and localized, while **The Source** opens into resonant, wide-stereo harmonics. Music expresses emotional and ideological stakes naturally without turning metaphysics into literal musical gadgets.

---

## 2. Canonical Leitmotif Architecture (Non-Colliding Keys)

MIDI motif seeds live in `audio_studio/midi_motifs/` (generated via `.tools/audio-venv/bin/python scripts/audio_studio.py midi`).

| Motif ID | Character / Force | Key / Mode | BPM | Signature Lead Instruments |
|---|---|---|---|---|
| `motif_nova` | **Nova Vance** (*The Spark*) | D Minor → D Major | 92 | Resonator / steel-string guitar → overdriven electric lead → guitar + strings |
| `motif_jed` | **Jed** (*The Fixer's Gift*) | G Major | 76 | Dusty harmonica & warm nylon/acoustic guitar |
| `motif_zeke` | **Zeke** (*Unbroken Iron*) | E Minor / Blues | 84 | Fender Rhodes & gritty P-bass |
| `motif_orion` | **Orion** (*Memory of Aethel*) | A Dorian (`3/4`) | 70 | Wooden flute, Tibetan singing bowls, solo cello |
| `motif_gh0st` | **Gh0st** (*The Phantom Trace*) | F# Minor | 104 | Clean muted guitar with dotted-8th tape delay, sub-bass pulse |
| `motif_elara` | **Elara** (*The Pure Voice*) | F# Major (`3/4`) | 84 | Glass celesta / music box & pure breathy soprano (shared `F#` center with Gh0st) |
| `motif_thorne` | **Mara Thorne** (*Closed Market*) | Bb Minor | 98 | Sleek analog synth brass, glassy marimba, cold Rhodes |
| `motif_vale` | **Arden Vale** (*The Soloist*) | C Minor (`C-B-Bb-A`) | 80 | Cold high-register piano → pipe organ + unison brass/strings |
| `motif_source` | **The Source / Chorus** (*The Current*) | A Dorian / Quartal | 64 | Glass armonica, singing bowls, wordless choral shimmer |

---

## 3. Hybrid Production Workflow (Suno + ElevenLabs)

1. **Music & Leitmotifs (Suno Studio / MCP):**
   * Consult [SUNO_PRODUCTION_PACK.md](../../audio_studio/suno_briefs/SUNO_PRODUCTION_PACK.md).
   * Upload the matching `audio_studio/midi_motifs/<motif>.mid` seed into Suno Create/Studio, apply the track's Style Prompt + Exclude list, and export `.wav`/`.mp3` (plus stems if trimming dialogue clutter) into `audio_studio/incoming/`.
   * Ingest: `.tools/audio-venv/bin/python scripts/audio_studio.py ingest audio_studio/incoming/<file>.wav --id <track_id> --provider suno`
2. **SFX, Ambience & ElevenLabs Music / Stingers (`elevenlabs_api_key.txt`):**
   * Music/Stingers: `.tools/audio-venv/bin/python scripts/audio_studio.py compose-elevenlabs --id <track_id> --use-plan --takes 2`
   * Stems: `.tools/audio-venv/bin/python scripts/audio_studio.py stems <take_path> --variation six_stems_v1`
   * SFX: `.tools/audio-venv/bin/python scripts/audio_studio.py sfx --group ui --takes 2`
3. **Automated Mastering & Loop Engineering (`ffmpeg` + EBU R128):**
   * `.tools/audio-venv/bin/python scripts/audio_studio.py master --id <cue_id>`
   * Exploration loops normalize to **-17 LUFS**, Combat/Bosses to **-14 / -13.5 LUFS**, SFX to **-12.5 to -14 LUFS** with a `-1.0 dBTP` ceiling and equal-power reverb tail-wrap loops (or authored `loop_start_ms` / `loop_end_ms`).
4. **Audition & Loop-Seam Review:**
   * `.tools/audio-venv/bin/python scripts/audio_studio.py build-board`
   * Open [audio_review_board.html](../../tools/audio_review_board.html) to A/B takes, test loop seams at `end - 4s`, and simulate exploration ↔ combat crossfades.
5. **Promote & Verify:**
   * `.tools/audio-venv/bin/python scripts/audio_studio.py promote --id <cue_id> --sync-bindings`
   * `.tools/audio-venv/bin/python scripts/audio_studio.py verify`

---

## 4. Runtime Engine Integration

* [AudioRouter.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/domain/audio/AudioRouter.kt) resolves exploration music hierarchically: **`room_music[roomId]` → `node_music[hubNodeId]` → `music[hubId]` → `music[worldId]` → tag fallback**.
* [CombatController.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/feature/combat/viewmodel/CombatController.kt) routes combat/boss themes through `combat_music` in [audio_bindings.json](../../app/src/main/assets/audio_bindings.json) (supporting `the_iron_warden`, `the_beast`, `administrator_boss`, `phantom_prototype`, `titan_walker_boss`, `crucible_champion`, `compliance_avatar`, `ascended_vale`, `ascended_god`).
* [AudioCuePlayer.kt](../../app/src/main/java/com/example/starborn/domain/audio/AudioCuePlayer.kt) and [DesktopAudioDriver.kt](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/DesktopAudioDriver.kt) execute equal-power crossfades, `loop_start_ms` / `loop_end_ms` loop points, and short motif stingers.
