# World 5: Charles and exploration music

Date: 2026-11-12. Implements the approved recommendations for music, Charles's motivation and his materials favor. The Astra bunk corridor and the plan for unique art for every enemy are deferred for discussion. No device installation, commit, push or live walkthrough publication was requested or performed.

## Charles

Charles initially offers public schedules, withholds credentials and personal records, and treats Nova as someone whose work he needs to assess. His optional ethics topic gives a concrete reason for opposing the station's leadership: he supplied forecasts to Thorne's contractors, saw executive capacity go unused while staff waited, and now sees Vale's notices preventing those workers from leaving.

His MMO, systems-critique and Great Frontier reference topics remain. His identity and personal references are preserved.

The favor is still a small optional milestone favor, not another Journal quest:

1. Ask Charles whether his analytics can help tune the suit's tactical heuristics.
2. Leave the conversation and inspect the **biometric gates** in the same Customs Lounge. Nova compares advertised waits with actual public departures and identifies executive berth reservations masquerading as mechanical delays. The details popup acknowledges the finding; no passenger records are copied.
3. Return with **two Circuit Boards and one Focusing Lens**. Materials carried from earlier worlds remain valid.
4. Complete the conversation to consume those materials and receive the existing reward: **Venture Heuristic Slate, 400 XP and 500 credits**.

The queue comparison grants no items, XP or credits. It makes the material handover available only once it has been done, and then the gate becomes gray inspection text. An early gate inspection provides useful details but cannot credit an unaccepted favor. Charles has a reminder response while the comparison is pending.

Previously accepted favors can complete the new comparison. Previously completed favors stay complete, need no new audit and cannot be offered or rewarded again. The completed gate inspection also recognizes the old completion milestone.

## Music

The existing outer-ring prompt called for slow E-flat-major wonder, celestial shimmer and panoramic beauty. That direction explains the mismatch with the approved dangerous espionage mood. Deep Ring already has its separate restrained surveillance cue, so that binding remains unchanged.

The outer-ring hub now uses **Grand Concourse (Borrowed Time)**, resource `music_w5_orbital_intrigue`, a new 96-second instrumental cue generated with ElevenLabs. Its direction is an 80 BPM C-minor electronic pulse, controlled low end, radar-like tones and occasional muted warm chords, with space for dialogue and interface sounds. The generation prompt requests restrained dynamics and an unresolved returning texture rather than a battle climax or serene vista.

Integration:

- `app/src/main/res/raw/music_w5_orbital_intrigue.mp3`
- Registered as looping music with existing exploration fade timings and gain in `audio_catalog.json`.
- Bound to `hub_9_orbital_ring` in `audio_bindings.json`.
- Added to the music-production registry so later catalog synchronization retains the cue.
- Deep Ring continues using `music_w5_security_hub`.
- The previous calm audio file is retained; other music is unchanged.

[Full generation prompt, provider details, hash and measurements](../audio/world5_orbital_intrigue_generation.json). The provider request follows the [ElevenLabs compose API](https://elevenlabs.io/docs/api-reference/music/compose). The API key was read privately from the existing local key file and was not printed or stored in the provenance.

Decoded validation: 44.1 kHz stereo, approximately 96.03 seconds, -17.14 dBFS RMS, -1.24 dBFS peak and no full-scale clipped samples. This verifies a real, non-silent, decodable audio asset. Subjective listening, musical loop quality and the balance against effects remain playtest checks; no human listening approval or perfect seamless loop is claimed.

## Walkthrough and verification

The local World 5 completionist route now covers the queue comparison and allows prepared players to finish Charles's favor on their first visit. Its later return step handles both pending favors and already completed ones. Step numbering and every existing checklist ID are preserved; published Worlds 1–4 remain identical in the local preview. The live reader has not been updated ahead of the game build.

Public interaction tests exercise a prepared player's full favor through NPC choices, an early inspection, the audit, save/reload, material consumption and exact one-time rewards. Additional cases cover an accepted favor without materials, old completed saves, and outer/deep ring audio selection. The World 4 departure audio check now expects the new cue after arrival.

**Validation:** Android compilation succeeded; 60 focused tests passed across Charles’s full interaction flow, save compatibility, audio routing and arrival timing, World 5 navigation, inspection assets, walkthrough reachability, discoverability and data integrity. The previously documented 23 baseline failures in the broader Android suite remain outside this pass.

Evidence and test results: [verification folder](evidence/world5_charles_music_2026-11-12/). No player save or device state was used for these tests.
