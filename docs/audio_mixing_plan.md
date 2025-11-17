# Audio Mixing & Runtime Controls

_Updated: Phase 4 polish sprint._

## Current State
- `AudioRouter` exposes cue routing and combat/ambient channel selection but has no helper APIs for synchronized fades, stacked ambience, or user volume overrides.
- `AudioCuePlayer` applies raw gain, yet the Exploration settings menu only toggles booleans; there is no real‑time verification that sliders persist or re‑duck music after cinematics.
- Content JSON references loop IDs without envelope metadata (attack/decay), so transitions between exploration ↔ combat ↔ cinematics snap hard.

## Target Experience
1. **Layered Mixes**
   - Each environment registers `music_primary`, `music_pad`, and `ambience` cues with default gains.
   - Transitions crossfade over 600‑900 ms (configurable per cue) to hide seams.
2. **Runtime Controls**
   - Settings panel sliders immediately reflect/drive `AudioRouter` mix nodes; values persist in `UserSettingsStore`.
   - Cinematic playback ducks music (-8 dB) with auto‑restore after fade, even when cinematics queue.
3. **Authoring Hooks**
   - JSON event actions can request `audio_fade` or `set_layer` verbs (start pad, fade ambience, mute combat stingers) without inline Kotlin.
   - Diagnostic logging surfaces missing cue assets and overlapping fades during development builds.

## Implementation Plan
1. **Router Enhancements**
   - Add `AudioLayer` enum (MusicPrimary, MusicPad, Ambience, Stinger, UI) with dedicated `fadeTo(targetGain, durationMs)` operations backed by `CoroutineScope`.
   - Track active cues per layer so `AudioRouter` can pause/replace gracefully instead of spawning multiple MediaPlayers.
2. **Settings Integration**
   - Introduce `AudioSettings` data class (music, sfx, ambience gains) in `UserSettingsStore`.
   - Update `ExplorationViewModel` to subscribe to store changes and push them to `AudioRouter` on launch + slider drag.
3. **Cinematic Ducking**
   - Replace the simple `duckForCinematic()/restoreAfterCinematic()` stubs with layer fade calls (music -> −8 dB, ambience -> −4 dB).
   - Ensure `pendingFadeCallbacks` wait for router completion before progressing cutscene `on_complete`.
4. **Event Actions**
   - Extend `EventAction` parser with `type: "audio_layer"` supporting `layer`, `cue_id`, `gain`, `fade_ms`.
   - Wire `EventManager` to delegate to `AudioRouter` via new hook `onAudioLayerCommand`.
5. **Testing & Tooling**
   - Add JVM tests that simulate slider updates, cinematic ducking, and concurrent fades.
   - Log cue transitions (layer, from → to gain, duration) guarded by `BuildConfig.DEBUG`.

## Progress
- **2025-02-15** – `ExplorationViewModel` now listens to `UserSettingsStore`, updates UI sliders + `AudioCuePlayer` when persisted settings change, and pushes slider edits back into DataStore so runtime fades obey saved gains.

## Open Questions
- Should ambience loops pause when entering combat, or merely duck? (Need UX direction.)
- Is there a separate “voiceover” bus that should stay untouched by the global SFX slider?
- For low‑end devices, do we keep multiple MediaPlayers pooled or fallback to sequential playback?

Once the above is implemented we’ll close Phase 4’s remaining audio fidelity requirement and can pivot to Phase 5 save/persistence QA. 
