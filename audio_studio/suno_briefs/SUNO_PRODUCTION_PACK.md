# Starborn — Argon Master Suno MCP Blueprint (Batches 1–12, All 71 Cues)

- **Suno Workspace (`Starborn OST`):** `c2259127-5f36-43d3-8024-09a306074382`
- **All 9 Pre-Uploaded Motif Reference Audio IDs (Live on Suno):**
  - `motif_nova.wav` (`D Minor`, *Starborn Motif - Nova*): `2104928f-0b87-4365-9ada-84dcf1e04264`
  - `motif_jed.wav` (`G Major`, *Starborn Motif - Jed*): `5bd8f244-ebb0-46b5-804d-14ba3b550c8c`
  - `motif_zeke.wav` (`E Minor`, *Starborn Motif - Zeke*): `7d80cd57-8425-4dff-bf09-4bd106eb3204`
  - `motif_orion.wav` (`A Dorian`, *Starborn Motif - Orion*): `09388aa3-000f-4f73-b59c-bfb47e5e9a89`
  - `motif_gh0st.wav` (`F# Minor`, *Starborn Motif - Gh0st*): `60b741bd-2489-4f45-a55d-41c2b7589ca3`
  - `motif_elara.wav` (`F# Major`, *Starborn Motif - Elara*): `7e3ba6c5-7508-4de6-a685-e47e8ed673f4`
  - `motif_thorne.wav` (`Bb Minor`, *Starborn Motif - Thorne*): `d6d4ceca-f315-42bb-b1df-f28892534b56`
  - `motif_vale.wav` (`C Minor`, *Starborn Motif - Vale*): `991e19c9-97e8-4658-b538-b70d550eb31b`
  - `motif_source.wav` (`A Dorian`, *Starborn Motif - Source*): `3f556f02-4430-44c3-a4d9-51ba1d1cad1b`
  - `motif_nova.mid` (MIDI Upload ID): `8299f5f3-b82b-47e1-86e1-a62f355b6a7b`

## Hydrogen / Batch Execution Protocol (Per Batch)
1. For the requested Batch $N$, read all cues with `"batch": N` from `audio_studio/suno_briefs/suno_briefs.json`.
2. For each cue, call `call_mcp_tool(ServerName="suno", ToolName=cue["suno_mcp_tool"], Arguments=cue["suno_mcp_arguments"])`.
3. Call `audio-wait` (`until: "complete"`) on all returned `clip_ids`, then present a table of **Take A** and **Take B** `https://suno.com/song/<id>` links to the user (**0 download credits spent**).
4. Wait for the user to **Thumbs-Up (Like)** their winning takes on Suno (and **never delete unliked takes**—keep them in the workspace).
5. Call `library-list(workspace_id="c2259127-5f36-43d3-8024-09a306074382", liked=true, limit=50)` to detect the liked winners.
6. For any cue with 0 liked takes, run `audio-listen` to diagnose, adjust `suno_mcp_arguments`, and regenerate until approved.
7. Once approved, call `audio-download(confirm=true, format="mp3", id="<liked_clip_id>")` **only on the 1 liked winner per cue**, then run:
   - `.tools/audio-venv/bin/python scripts/audio_studio.py ingest --id <cue_id> --provider suno --notes "suno_mcp:<clip_id>" <file.mp3>`
   - `.tools/audio-venv/bin/python scripts/audio_studio.py master --id <cue_id> --loop-mode <wrap|none>`
   - `.tools/audio-venv/bin/python scripts/audio_studio.py promote --id <cue_id> --force --sync-bindings`
   - `.tools/audio-venv/bin/python scripts/audio_studio.py verify --strict-catalog`

---

## Batch 1: Pilot Core & W1 Vertical Slice (COMPLETED) (5 Cues)

### `music_title_theme` — Starborn Main Title (The Spark in the Dark)
- **Status:** `completed_and_promoted` | **Key:** `D Minor -> D Major` | **BPM:** `88` | **Loop:** `True`
- **Leitmotif Tier:** Tier 1M: Main Title Theme — MIDI-Guided (Bookended 5-Note Theme + 45s Soaring B-Section Expansion)
- **Suno MCP Tool:** `audio-create`
- **Approved Winner ID:** `9182975a-269f-49ef-a3c6-576523e0d88b` (Backups kept: `6ee79c36-b3fc-4d5b-8bb8-abdf08448a2a`)
```json
{
  "title": "Starborn Main Title (The Spark in the Dark)",
  "instrumental": true,
  "midi_upload_id": "8299f5f3-b82b-47e1-86e1-a62f355b6a7b",
  "model": "v6-wild",
  "duration": 110,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_pilot_title_v1",
  "description": "Play my MIDI melody as an 80s analog cassette sci-fi JRPG main title theme at 88 BPM in D minor opening into hopeful D major. Solo fingerpicked acoustic steel-string guitar states the 5-note ascending theme, joined by warm Prophet-5 analog synth pads, lyrical solo cello, crystalline glass chimes, and a steady warm pulse. Nostalgic analog tape saturation, expansive cosmic wonder, seamless loop. Strictly instrumental, no vocals, no singing, no trap beats, no EDM drops."
}
```

### `music_astra_common_room` — The Astra Lounge (Coffee & Cassettes)
- **Status:** `completed_and_promoted` | **Key:** `G Major` | **BPM:** `76` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
- **Approved Winner ID:** `3dbb4fd9-9b15-4a48-968d-88c146c34ce5` (Backups kept: `60d3dbaf-641b-44e1-9781-b76013f4f177, 6090311e-d92c-4487-a27a-3fb492ea9f6d, 44f4c158-a81f-4e27-8ef1-04e8eb9d542b`)
```json
{
  "title": "The Astra Lounge (Coffee & Cassettes)",
  "instrumental": true,
  "reference_audio_ids": [
    "2dcad836-9af3-4161-b85f-40199f11b108"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_astra_common_room_v1",
  "description": "Create a spacious RPG exploration underscore at 76 BPM in G Major. Warm 80s analog cassette lo-fi folk-synthwave, 76 BPM, G major. Warm Fender Rhodes electric piano with chorus, gentle nylon/acoustic fingerpicking, mellow analog sub-bass, soft brushed kit, subtle tape hiss and cozy cabin intimacy. Safe harbor between space missions, relaxed, uncluttered mids for companion dialogue, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Pilot - Astra Common Room once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, harsh distortion, cinematic brass climaxes, alarm sounds, singing, nonstop soloing."
}
```

### `music_w1_homestead_explore` — Stellarium Colony (Homestead & Trade Row)
- **Status:** `completed_and_promoted` | **Key:** `D Minor` | **BPM:** `86` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
- **Approved Winner ID:** `ca24ef48-dd3d-49cc-ba10-265849a5f477` (Backups kept: `c14f4023-fc19-4238-81bc-345020f789eb`)
```json
{
  "title": "Stellarium Colony (Homestead & Trade Row)",
  "instrumental": true,
  "reference_audio_ids": [
    "2104928f-0b87-4365-9ada-84dcf1e04264"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w1_homestead_explore_v1",
  "description": "Create a spacious RPG exploration underscore at 86 BPM in D Minor. Dusty industrial blues RPG exploration underscore, 86 BPM, D minor. Dry close-miked resonator steel-string guitar carrying Nova's restrained ascending D-F-G-A motif, subtle dusty harmonica and warm Rhodes harmony, muted metallic pipe percussion, warm P-bass, subtle analog tape grit. Working-class survival, restrained dynamics for dialogue, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Nova once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, huge cathedral reverb, epic brass, metal drums, singing, nonstop soloing."
}
```

### `music_w1_combat` — Homestead Scrap Skirmish
- **Status:** `completed_and_promoted` | **Key:** `D Minor` | **BPM:** `138` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3C: Standard World Combat Loop — Custom Style Key-Lock (Immediate Bar-1 Drive, Riff/Groove Contrast, No Repetitive 4-Bar Motif Fatigue)
- **Suno MCP Tool:** `audio-create`
- **Approved Winner ID:** `e9e85c73-badc-4223-b6c9-8d091090ca3f` (Backups kept: `6dbd6d4f-bfb1-4982-bdbd-129efd2f3193`)
```json
{
  "title": "Homestead Scrap Skirmish",
  "instrumental": true,
  "style": "High-octane 80s analog blues-rock JRPG battle theme, 138 BPM, D minor. Overdriven resonator and lead electric guitar propelling Nova's ascending D-F-G-A battle hook, punchy live rock drum kit, fat gritty P-bass groove, rhythmic metallic clanks. Gritty working-class adrenaline, seamless loop. Hit immediately at bar 1 with driving combat rhythm and regional sonic texture in D Minor at 138 BPM. Alternate tight rhythm-section grooves with dynamic melodic lead flourishes and a contrasting B-section bridge rather than repeating a literal 5-note character melody every 8 bars, preventing ear fatigue across repeated battles. End on an energetic transition turnaround for a seamless combat loop, strictly instrumental",
  "negative_style": "vocals, modern dubstep, serene ambient, singing, slow ambient intro, fading outro",
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w1_combat_v1"
}
```

### `music_w1_boss_warden` — The Iron Warden (Heavy Duty)
- **Status:** `completed_and_promoted` | **Key:** `C Minor / D Minor` | **BPM:** `142` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3B: Boss Battle Loop — Custom Style Key-Lock (80% Oppressive Boss Identity & Weight First, 20% Mid-Bridge Defiant Crew Counter-Motif)
- **Suno MCP Tool:** `audio-create`
- **Approved Winner ID:** `2ce01339-586c-4afc-8e98-de789f9493e8` (Backups kept: `d10d4a8e-df88-419e-b7b3-ef460e615f12, 34cf3146-dcfb-4718-aa5c-773479c23f28, 6c90e0ab-d957-4e42-82f9-b69d7f03d4c7`)
```json
{
  "title": "The Iron Warden (Heavy Duty)",
  "instrumental": true,
  "style": "Heavy industrial hard-rock JRPG boss battle, 142 BPM, C minor. Crushing distorted guitar power chords, pounding anvil hits, screeching analog synth sirens, driving double-kick drums, defiant lead guitar quoting Nova's ascending hook. Overwhelming armored corporate enforcer, seamless loop. Establish the boss's oppressive, menacing rhythmic and timbral weight first at 142 BPM in C Minor / D Minor—focus on heavy groove, signature boss instrumentation, and tension rather than a nonstop high-pitched guitar solo from bar 1. Only in the middle bridge section, let a defiant heroic counter-phrase break through the boss's rhythm in call-and-response before the full-weight boss riff slams back in for a seamless loop. Strictly instrumental",
  "negative_style": "vocals, peaceful flute, jazz lounge, singing, nonstop high-pitched shred solo, cheerful pop, fading outro",
  "duration": 120,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w1_boss_warden_v1"
}
```

## Batch 2: Core Character Themes Part 1 (Crew Founders) (4 Cues)

### `music_nova_theme` — Nova's Theme (The Spark)
- **Status:** `ready_for_generation` | **Key:** `D Minor -> D Major` | **BPM:** `92` | **Loop:** `True`
- **Leitmotif Tier:** Tier 1: Character Theme (A -> B -> A Arc; Motif Leads in A, Contrasting B-Section Prevents Mechanical Repetition)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Nova's Theme (The Spark)",
  "instrumental": true,
  "reference_audio_ids": [
    "2104928f-0b87-4365-9ada-84dcf1e04264"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_nova_theme_v1",
  "description": "Use the melodic motif from Starborn Motif - Nova to create an expressive JRPG character theme at 92 BPM in D Minor -> D Major. Expressive JRPG character theme for a gritty 22-year-old mechanic, 92 BPM, D minor resolving to D major. Fingerpicked resonator acoustic guitar lead carrying a stubborn ascending 5-note melody, backed by warm cello, dusty brushed snare, upright bass, and subtle Juno synth warmth. Resilient, practical, quietly tender. Structure with restraint and emotional depth: open with 12-15 seconds of intimate rhythm and room texture, state the character's signature melodic motif clearly on the lead instrument, then transition into a contrasting 30-second middle B-section counter-melody and harmonic expansion so the 5-note hook is never hammered repetitively. Keep the midrange warm and uncluttered for RPG dialogue, and reprise the motif gently before a seamless loop turn. Strictly instrumental; exclude vocals, pompous royalty fanfare, modern EDM, singing."
}
```

### `music_jed_theme` — Jed's Theme (The Fixer's Gift)
- **Status:** `ready_for_generation` | **Key:** `G Major` | **BPM:** `76` | **Loop:** `True`
- **Leitmotif Tier:** Tier 1: Character Theme (A -> B -> A Arc; Motif Leads in A, Contrasting B-Section Prevents Mechanical Repetition)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Jed's Theme (The Fixer's Gift)",
  "instrumental": true,
  "reference_audio_ids": [
    "5bd8f244-ebb0-46b5-804d-14ba3b550c8c"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_jed_theme_v1",
  "description": "Use the melodic motif from Starborn Motif - Jed to create an expressive JRPG character theme at 76 BPM in G Major. Intimate warm frontier mentor theme, 76 BPM, G major. Solo nylon/steel acoustic guitar and dusty mellow harmonica playing a gentle falling plagal cadence, soft upright bass, subtle workbench room air and analog tape warmth. Unhurried, caring, blue-collar grace. Structure with restraint and emotional depth: open with 12-15 seconds of intimate rhythm and room texture, state the character's signature melodic motif clearly on the lead instrument, then transition into a contrasting 30-second middle B-section counter-melody and harmonic expansion so the 5-note hook is never hammered repetitively. Keep the midrange warm and uncluttered for RPG dialogue, and reprise the motif gently before a seamless loop turn. Strictly instrumental; exclude vocals, distortion, fast percussion, synthwave drops, singing."
}
```

### `music_zeke_theme` — Zeke's Theme (Unbroken Iron)
- **Status:** `ready_for_generation` | **Key:** `E Minor` | **BPM:** `84` | **Loop:** `True`
- **Leitmotif Tier:** Tier 1: Character Theme (A -> B -> A Arc; Motif Leads in A, Contrasting B-Section Prevents Mechanical Repetition)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Zeke's Theme (Unbroken Iron)",
  "instrumental": true,
  "reference_audio_ids": [
    "7d80cd57-8425-4dff-bf09-4bd106eb3204"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_zeke_theme_v1",
  "description": "Use the melodic motif from Starborn Motif - Zeke to create an expressive JRPG character theme at 84 BPM in E Minor. Soulful working-class blues character theme, 84 BPM, E minor. Warm Fender Rhodes chords, gritty overdriven P-bass walking line, understated slide guitar fills, crisp dry rimshot groove, analog cassette warmth. Loyal, self-deprecating courage rising from nervous tension. Structure with restraint and emotional depth: open with 12-15 seconds of intimate rhythm and room texture, state the character's signature melodic motif clearly on the lead instrument, then transition into a contrasting 30-second middle B-section counter-melody and harmonic expansion so the 5-note hook is never hammered repetitively. Keep the midrange warm and uncluttered for RPG dialogue, and reprise the motif gently before a seamless loop turn. Strictly instrumental; exclude vocals, metal blast beats, glossy EDM, singing."
}
```

### `music_orion_theme` — Orion's Theme (Memory of Aethel)
- **Status:** `ready_for_generation` | **Key:** `A Dorian` | **BPM:** `70` | **Loop:** `True`
- **Leitmotif Tier:** Tier 1: Character Theme (A -> B -> A Arc; Motif Leads in A, Contrasting B-Section Prevents Mechanical Repetition)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Orion's Theme (Memory of Aethel)",
  "instrumental": true,
  "reference_audio_ids": [
    "09388aa3-000f-4f73-b59c-bfb47e5e9a89"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_orion_theme_v1",
  "description": "Use the melodic motif from Starborn Motif - Orion to create an expressive JRPG character theme at 70 BPM in A Dorian. Ancient elegiac chamber-folk theme in 3/4 time, 70 BPM, A Dorian. Lyrical wooden flute and deep solo cello weaving over resonant Tibetan singing bowls, delicate harp arpeggios, and wide sacred reverb. Dignified grief softening into quiet guidance. Structure with restraint and emotional depth: open with 12-15 seconds of intimate rhythm and room texture, state the character's signature melodic motif clearly on the lead instrument, then transition into a contrasting 30-second middle B-section counter-melody and harmonic expansion so the 5-note hook is never hammered repetitively. Keep the midrange warm and uncluttered for RPG dialogue, and reprise the motif gently before a seamless loop turn. Strictly instrumental; exclude vocals, electronic drum beats, distorted guitars, singing."
}
```

## Batch 3: Core Character Themes Part 2 (Shadows, Antagonists & Bonds) (5 Cues)

### `music_gh0st_theme` — Gh0st's Theme (The Phantom Trace)
- **Status:** `ready_for_generation` | **Key:** `F# Minor` | **BPM:** `104` | **Loop:** `True`
- **Leitmotif Tier:** Tier 1: Character Theme (A -> B -> A Arc; Motif Leads in A, Contrasting B-Section Prevents Mechanical Repetition)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Gh0st's Theme (The Phantom Trace)",
  "instrumental": true,
  "reference_audio_ids": [
    "60b741bd-2489-4f45-a55d-41c2b7589ca3"
  ],
  "duration": 92,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_gh0st_theme_v1",
  "description": "Use the melodic motif from Starborn Motif - Gh0st to create an expressive JRPG character theme at 104 BPM in F# Minor. Solitary stealth cyber-noir theme, 104 BPM, F# minor. Clean muted electric guitar arpeggios through dark analog tape delay, deep fretless sub-bass, tight dry electronic rim clicks, faint warm glass celesta harmonic answer hinting at lost humanity. Restrained, watchful, protective. Structure with restraint and emotional depth: open with 12-15 seconds of intimate rhythm and room texture, state the character's signature melodic motif clearly on the lead instrument, then transition into a contrasting 30-second middle B-section counter-melody and harmonic expansion so the 5-note hook is never hammered repetitively. Keep the midrange warm and uncluttered for RPG dialogue, and reprise the motif gently before a seamless loop turn. Strictly instrumental; exclude vocals, loud metal guitars, festive brass, singing."
}
```

### `music_elaras_song` — Elara's Song (The Pure Voice)
- **Status:** `ready_for_generation` | **Key:** `F# Major` | **BPM:** `84` | **Loop:** `False`
- **Leitmotif Tier:** Tier 1V: Vocal Character Lullaby (A -> B -> A Emotional Arc)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Elara's Song (The Pure Voice)",
  "instrumental": false,
  "reference_audio_ids": [
    "7e3ba6c5-7508-4de6-a685-e47e8ed673f4"
  ],
  "duration": 120,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_elaras_song_v1",
  "description": "Use the gentle 3/4 waltz melody from Starborn Motif - Elara to create a heartbreaking 1980s 4-track cassette folk lullaby at 84 BPM in F# Major. Heartbreaking 1980s 4-track cassette folk lullaby in 3/4 waltz time, 84 BPM, F# major. Fingerpicked acoustic guitar, delicate music box celesta, warm chorus bass, gentle tape flutter and hiss, with a soft breathy pure female soprano singing a tender wordless/short lullaby about starlight and keeping the door open. Open with 12 seconds of intimate fingerpicked acoustic guitar and delicate music-box celesta before the soft, pure breathy female voice enters with the lullaby motif; expand into a tender, spacious middle B-section with warm chorus bass and wordless vocal sighs so the melody breathes naturally, then return for a quiet, fragile final verse. Avoid harsh pop belting, loud drums, or EDM synths."
}
```

### `music_thorne_theme` — Thorne's Theme (Closed Market)
- **Status:** `ready_for_generation` | **Key:** `Bb Minor` | **BPM:** `98` | **Loop:** `True`
- **Leitmotif Tier:** Tier 1: Character Theme (A -> B -> A Arc; Motif Leads in A, Contrasting B-Section Prevents Mechanical Repetition)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Thorne's Theme (Closed Market)",
  "instrumental": true,
  "reference_audio_ids": [
    "d6d4ceca-f315-42bb-b1df-f28892534b56"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_thorne_theme_v1",
  "description": "Use the melodic motif from Starborn Motif - Thorne to create an expressive JRPG character theme at 98 BPM in Bb Minor. Cold executive corporate noir theme, 98 BPM, Bb minor. Sleek muted analog synth brass, glassy marimba ostinato, crisp dry electronic hi-hats, deep sub-bass stride, polished sterile lounge reverb. Confident, calculating monopoly. Structure with restraint and emotional depth: open with 12-15 seconds of intimate rhythm and room texture, state the character's signature melodic motif clearly on the lead instrument, then transition into a contrasting 30-second middle B-section counter-melody and harmonic expansion so the 5-note hook is never hammered repetitively. Keep the midrange warm and uncluttered for RPG dialogue, and reprise the motif gently before a seamless loop turn. Strictly instrumental; exclude vocals, warm rustic acoustic guitar, heavy rock drums, singing."
}
```

### `music_vale_theme` — Vale's Theme (The Soloist)
- **Status:** `ready_for_generation` | **Key:** `C Minor` | **BPM:** `80` | **Loop:** `True`
- **Leitmotif Tier:** Tier 1: Character Theme (A -> B -> A Arc; Motif Leads in A, Contrasting B-Section Prevents Mechanical Repetition)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Vale's Theme (The Soloist)",
  "instrumental": true,
  "reference_audio_ids": [
    "991e19c9-97e8-4658-b538-b70d550eb31b"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_vale_theme_v1",
  "description": "Use the melodic motif from Starborn Motif - Vale to create an expressive JRPG character theme at 80 BPM in C Minor. Hauntingly calm minimalist chamber theme, 80 BPM, C minor. High-register sterile solo piano playing a slow 4-note descending chromatic line (C-B-Bb-A), supported by cold sustained cello harmonics and distant low pipe organ swell. Merciless perfection, quiet ideological certainty. Structure with restraint and emotional depth: open with 12-15 seconds of intimate rhythm and room texture, state the character's signature melodic motif clearly on the lead instrument, then transition into a contrasting 30-second middle B-section counter-melody and harmonic expansion so the 5-note hook is never hammered repetitively. Keep the midrange warm and uncluttered for RPG dialogue, and reprise the motif gently before a seamless loop turn. Strictly instrumental; exclude vocals, blues groove, playful melodies, loud rock drums, singing."
}
```

### `music_theme_emotional` — Shared Ground (Emotional Theme)
- **Status:** `ready_for_generation` | **Key:** `D Minor -> G Major` | **BPM:** `68` | **Loop:** `False`
- **Leitmotif Tier:** Tier 1: Character Theme (A -> B -> A Arc; Motif Leads in A, Contrasting B-Section Prevents Mechanical Repetition)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Shared Ground (Emotional Theme)",
  "instrumental": true,
  "reference_audio_ids": [
    "2104928f-0b87-4365-9ada-84dcf1e04264"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_theme_emotional_v1",
  "description": "Use the melodic motif from Starborn Motif - Nova to create an expressive JRPG character theme at 68 BPM in D Minor -> G Major. Tender bittersweet JRPG acoustic scene score, 68 BPM. Intimate fingerpicked acoustic guitar carrying Nova's warm melody answered by expressive solo cello and soft felt piano chords, subtle analog room warmth. Unspoken loyalty and quiet grief. Structure with restraint and emotional depth: open with 12-15 seconds of intimate rhythm and room texture, state the character's signature melodic motif clearly on the lead instrument, then transition into a contrasting 30-second middle B-section counter-melody and harmonic expansion so the 5-note hook is never hammered repetitively. Keep the midrange warm and uncluttered for RPG dialogue, and reprise the motif gently before a seamless loop turn. Strictly instrumental; exclude vocals, drums, electronic drops, singing."
}
```

## Batch 4: Narrative Motif Stingers (sounds-create) (5 Cues)

### `stinger_motif_nova` — Motif Stinger: Nova's Spark
- **Status:** `ready_for_generation` | **Key:** `D Minor` | **BPM:** `92` | **Loop:** `False`
- **Leitmotif Tier:** Tier 0: Short 5-7s Narrative Motif Stinger (100% Motif Phrase, One-Shot)
- **Suno MCP Tool:** `sounds-create`
```json
{
  "title": "Motif Stinger: Nova's Spark",
  "description": "Short 6-second JRPG narrative leitmotif stinger at 92 BPM in D Minor: Short 6-second JRPG musical motif phrase, 92 BPM, D minor. Clean resonant acoustic guitar playing an ascending 5-note determined melody supported by a single warm synth swell, ending with natural clean decay. Single expressive phrase with natural resonant decay tail, no drum loop, no vocals.",
  "key": "Dm",
  "tempo": 92,
  "loop": false,
  "idempotency_key": "starborn_stinger_motif_nova_v1"
}
```

### `stinger_motif_jed` — Motif Stinger: Jed's Workbench
- **Status:** `ready_for_generation` | **Key:** `G Major` | **BPM:** `76` | **Loop:** `False`
- **Leitmotif Tier:** Tier 0: Short 5-7s Narrative Motif Stinger (100% Motif Phrase, One-Shot)
- **Suno MCP Tool:** `sounds-create`
```json
{
  "title": "Motif Stinger: Jed's Workbench",
  "description": "Short 6-second JRPG narrative leitmotif stinger at 76 BPM in G Major: Short 6-second warm acoustic guitar and soft harmonica musical phrase in G major, 76 BPM. Gentle falling plagal cadence, dusty workshop warmth, clean natural ring-out. Single expressive phrase with natural resonant decay tail, no drum loop, no vocals.",
  "key": "G",
  "tempo": 76,
  "loop": false,
  "idempotency_key": "starborn_stinger_motif_jed_v1"
}
```

### `stinger_motif_elara` — Motif Stinger: Elara's Signal
- **Status:** `ready_for_generation` | **Key:** `F# Major` | **BPM:** `84` | **Loop:** `False`
- **Leitmotif Tier:** Tier 0: Short 5-7s Narrative Motif Stinger (100% Motif Phrase, One-Shot)
- **Suno MCP Tool:** `sounds-create`
```json
{
  "title": "Motif Stinger: Elara's Signal",
  "description": "Short 6-second JRPG narrative leitmotif stinger at 84 BPM in F# Major: Short 6-second delicate glass celesta and music-box lullaby phrase in F# major 3/4 time with faint 80s tape flutter and distant crystalline reverb tail. Single expressive phrase with natural resonant decay tail, no drum loop, no vocals.",
  "key": "F#",
  "tempo": 84,
  "loop": false,
  "idempotency_key": "starborn_stinger_motif_elara_v1"
}
```

### `stinger_motif_vale` — Motif Stinger: Soloist Signal
- **Status:** `ready_for_generation` | **Key:** `C Minor` | **BPM:** `80` | **Loop:** `False`
- **Leitmotif Tier:** Tier 0: Short 5-7s Narrative Motif Stinger (100% Motif Phrase, One-Shot)
- **Suno MCP Tool:** `sounds-create`
```json
{
  "title": "Motif Stinger: Soloist Signal",
  "description": "Short 6-second JRPG narrative leitmotif stinger at 80 BPM in C Minor: Short 6-second chilling solo cold piano and low string chime in C minor playing a slow 4-note descending chromatic line, sterile hall decay. Single expressive phrase with natural resonant decay tail, no drum loop, no vocals.",
  "key": "Cm",
  "tempo": 80,
  "loop": false,
  "idempotency_key": "starborn_stinger_motif_vale_v1"
}
```

### `stinger_motif_source` — Motif Stinger: Echo Resonance
- **Status:** `ready_for_generation` | **Key:** `A Dorian` | **BPM:** `64` | **Loop:** `False`
- **Leitmotif Tier:** Tier 0: Short 5-7s Narrative Motif Stinger (100% Motif Phrase, One-Shot)
- **Suno MCP Tool:** `sounds-create`
```json
{
  "title": "Motif Stinger: Echo Resonance",
  "description": "Short 6-second JRPG narrative leitmotif stinger at 64 BPM in A Dorian: Short 6-second ethereal Tibetan singing bowl and glass armonica chord bloom in A Dorian, shimmering cosmic resonance, clean cathedral tail. Single expressive phrase with natural resonant decay tail, no drum loop, no vocals.",
  "key": "Am",
  "tempo": 64,
  "loop": false,
  "idempotency_key": "starborn_stinger_motif_source_v1"
}
```

## Batch 5: Astra Flight Bridge & Battle Resolution Cues (4 Cues)

### `music_astra_bridge` — Flight Bridge (Charting the Frontier)
- **Status:** `ready_for_generation` | **Key:** `A Minor` | **BPM:** `88` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Flight Bridge (Charting the Frontier)",
  "instrumental": true,
  "reference_audio_ids": [
    "09388aa3-000f-4f73-b59c-bfb47e5e9a89"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_astra_bridge_v1",
  "description": "Create a spacious RPG exploration underscore at 88 BPM in A Minor. Warm analog space synthwave and downtempo navigation underscore, 88 BPM, A minor. Steady arpeggiated analog synth sequencer, clean muted electric guitar plucks with tape delay, subtle retro radar tones, gentle bass pulse, wide vintage stereo shimmer. Focused, hopeful, restrained for UI clarity, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Orion once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, combat drums, heavy metal guitars, loud risers, singing, nonstop soloing."
}
```

### `music_victory_standard` — Victory Fanfare (Battle Won)
- **Status:** `ready_for_generation` | **Key:** `D Major` | **BPM:** `126` | **Loop:** `False`
- **Leitmotif Tier:** Tier 3S: Cinematic Action / Fanfare One-Shot — Custom Style (Narrative Pacing + Clean Ending Cadence)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Victory Fanfare (Battle Won)",
  "instrumental": true,
  "style": "16-bit and 80s analog JRPG victory fanfare, 126 BPM, D major. Punchy brass-synth stabs quoting a bright ascending 5-note heroic hook, crisp electric guitar cadence, tight bass and rock drum groove settling into a celebratory loopable/clean ending cadence. Narrative one-shot pacing (20s) with a clean, decisive musical ending cadence. Strictly instrumental",
  "negative_style": "vocals, dark minor chords, ambient drone, singing",
  "duration": 20,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_victory_standard_v1"
}
```

### `music_victory_boss` — Major Victory (Breakthrough)
- **Status:** `ready_for_generation` | **Key:** `D Major` | **BPM:** `128` | **Loop:** `False`
- **Leitmotif Tier:** Tier 3S: Cinematic Action / Fanfare One-Shot — Custom Style (Narrative Pacing + Clean Ending Cadence)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Major Victory (Breakthrough)",
  "instrumental": true,
  "style": "Triumphant JRPG major boss victory fanfare evolving into uplifting analog synth-orchestral relief, 128 BPM, D major. Soaring electric guitar and warm brass-strings answering a heroic 5-note ascending cadence, resonant timpani, warm analog tape sheen. Narrative one-shot pacing (30s) with a clean, decisive musical ending cadence. Strictly instrumental",
  "negative_style": "vocals, tragic minor ending, singing",
  "duration": 30,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_victory_boss_v1"
}
```

### `music_game_over` — The Signal Fades (Defeat)
- **Status:** `ready_for_generation` | **Key:** `D Minor` | **BPM:** `60` | **Loop:** `False`
- **Leitmotif Tier:** Tier 3S: Cinematic Action / Fanfare One-Shot — Custom Style (Narrative Pacing + Clean Ending Cadence)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "The Signal Fades (Defeat)",
  "instrumental": true,
  "style": "Poetic somber JRPG defeat cue, 60 BPM, D minor. Felt upright piano playing a slow fading broken motif, low cello sigh, soft cassette tape deceleration and quiet radio static tail. Reflective, fragile, never loud. Narrative one-shot pacing (24s) with a clean, decisive musical ending cadence. Strictly instrumental",
  "negative_style": "vocals, drums, cheerful major chords, harsh jump scare, singing",
  "duration": 24,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_game_over_v1"
}
```

## Batch 6: Frontier Activities & Mini-Games (5 Cues)

### `music_fishing_ambient` — Tideglass Angler (Fishing)
- **Status:** `ready_for_generation` | **Key:** `C Major` | **BPM:** `72` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2A: Activity & Mini-Game Loop — Custom Style Key-Lock (85% Tactile Groove, 15% Subtle B-Section Melodic Nod)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Tideglass Angler (Fishing)",
  "instrumental": true,
  "style": "Relaxing acoustic water folk & cassette lo-fi fishing theme, 72 BPM, C major. Warm nylon-string fingerpicking, gentle wooden kalimba chimes, upright bass, shimmering shoreline synth haze. Peaceful meditative flow, seamless loop. Focus 85% on a cozy, tactile, non-fatiguing rhythmic and chordal pocket suitable for 10-minute crafting, fishing, cooking, shopping, or arcade sessions; avoid repetitive short 4-bar melody loops, introducing a warm, understated melodic hook only in the middle turnaround before cycling back cleanly for a seamless loop. Strictly instrumental",
  "negative_style": "vocals, heavy drums, distortion, singing, repetitive annoying 4-bar toy loop, harsh piercing leads",
  "duration": 80,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_fishing_ambient_v1"
}
```

### `music_arcade_cabinet` — Hyperion 1986 (Arcade Cabinet)
- **Status:** `ready_for_generation` | **Key:** `F Major` | **BPM:** `134` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2A: Activity & Mini-Game Loop — Custom Style Key-Lock (85% Tactile Groove, 15% Subtle B-Section Melodic Nod)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Hyperion 1986 (Arcade Cabinet)",
  "instrumental": true,
  "style": "Authentic 1986 coin-op FM synth & 8-bit chiptune arcade shooter theme, 134 BPM, F major. Bouncy square-wave lead melody, punchy FM slap bass, crisp noise-snare chiptune percussion, energetic arpeggios. Retro CRT speaker character, seamless loop. Focus 85% on a cozy, tactile, non-fatiguing rhythmic and chordal pocket suitable for 10-minute crafting, fishing, cooking, shopping, or arcade sessions; avoid repetitive short 4-bar melody loops, introducing a warm, understated melodic hook only in the middle turnaround before cycling back cleanly for a seamless loop. Strictly instrumental",
  "negative_style": "vocals, orchestral strings, modern trap, singing, repetitive annoying 4-bar toy loop, harsh piercing leads",
  "duration": 75,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_arcade_cabinet_v1"
}
```

### `music_tinkering_focus` — Workstation Flow (Tinkering)
- **Status:** `ready_for_generation` | **Key:** `G Major` | **BPM:** `80` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2A: Activity & Mini-Game Loop — Custom Style Key-Lock (85% Tactile Groove, 15% Subtle B-Section Melodic Nod)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Workstation Flow (Tinkering)",
  "instrumental": true,
  "style": "Cozy mechanical lo-fi workshop groove quoting Jed's G major cadence, 80 BPM. Mellow Fender Rhodes chords, subtle rhythmic socket-wrench and brass clicks, muted guitar harmonics, warm analog bass. Focused creative tinkering, seamless loop. Focus 85% on a cozy, tactile, non-fatiguing rhythmic and chordal pocket suitable for 10-minute crafting, fishing, cooking, shopping, or arcade sessions; avoid repetitive short 4-bar melody loops, introducing a warm, understated melodic hook only in the middle turnaround before cycling back cleanly for a seamless loop. Strictly instrumental",
  "negative_style": "vocals, loud guitars, alarms, singing, repetitive annoying 4-bar toy loop, harsh piercing leads",
  "duration": 80,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_tinkering_focus_v1"
}
```

### `music_cooking_kitchen` — Mess Hall Stew (Cooking)
- **Status:** `ready_for_generation` | **Key:** `Bb Major` | **BPM:** `112` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2A: Activity & Mini-Game Loop — Custom Style Key-Lock (85% Tactile Groove, 15% Subtle B-Section Melodic Nod)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Mess Hall Stew (Cooking)",
  "instrumental": true,
  "style": "Playful upbeat acoustic gypsy-jazz & kitchen swing, 112 BPM, Bb major. Bouncy acoustic rhythm guitars, cheerful pizzicato strings, upright walking bass, subtle wooden spoon percussion. Warm camaraderie around shared food, seamless loop. Focus 85% on a cozy, tactile, non-fatiguing rhythmic and chordal pocket suitable for 10-minute crafting, fishing, cooking, shopping, or arcade sessions; avoid repetitive short 4-bar melody loops, introducing a warm, understated melodic hook only in the middle turnaround before cycling back cleanly for a seamless loop. Strictly instrumental",
  "negative_style": "vocals, ominous synth, metal, singing, repetitive annoying 4-bar toy loop, harsh piercing leads",
  "duration": 75,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_cooking_kitchen_v1"
}
```

### `music_shop_cozy` — Frontier Exchange (Markets & Shops)
- **Status:** `ready_for_generation` | **Key:** `D Minor` | **BPM:** `88` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2A: Activity & Mini-Game Loop — Custom Style Key-Lock (85% Tactile Groove, 15% Subtle B-Section Melodic Nod)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Frontier Exchange (Markets & Shops)",
  "instrumental": true,
  "style": "Warm eclectic frontier trader lo-fi groove, 88 BPM, D minor. Acoustic lute/guitar riff, warm Fender Rhodes backing, gentle hand percussion, intriguing analog synth flute hook, analog tape saturation. Curious, street-smart trading post vibe, seamless loop. Focus 85% on a cozy, tactile, non-fatiguing rhythmic and chordal pocket suitable for 10-minute crafting, fishing, cooking, shopping, or arcade sessions; avoid repetitive short 4-bar melody loops, introducing a warm, understated melodic hook only in the middle turnaround before cycling back cleanly for a seamless loop. Strictly instrumental",
  "negative_style": "vocals, heavy combat drums, singing, repetitive annoying 4-bar toy loop, harsh piercing leads",
  "duration": 80,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_shop_cozy_v1"
}
```

## Batch 7: World 1 Landmarks & World 2 Sector 9 Exploration (7 Cues)

### `music_w1_jed_workshop` — Jed's Workshop (Warm Steel & Grease)
- **Status:** `ready_for_generation` | **Key:** `G Major` | **BPM:** `76` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Jed's Workshop (Warm Steel & Grease)",
  "instrumental": true,
  "reference_audio_ids": [
    "5bd8f244-ebb0-46b5-804d-14ba3b550c8c"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w1_jed_workshop_v1",
  "description": "Create a spacious RPG exploration underscore at 76 BPM in G Major. Intimate warm acoustic blues-folk workshop theme quoting Jed's Theme, 76 BPM, G major. Fingerpicked acoustic steel-string guitar, soft warm harmonica counter-phrase, gentle brushed percussion, subtle analog warmth. Safe, practical mentorship inside a dusty colony, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Jed once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, distortion, heavy drums, scary drone, singing, nonstop soloing."
}
```

### `music_w1_logistics_explore` — Logistics Sector (Quota & Concrete)
- **Status:** `ready_for_generation` | **Key:** `E Minor` | **BPM:** `90` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Logistics Sector (Quota & Concrete)",
  "instrumental": true,
  "reference_audio_ids": [
    "7d80cd57-8425-4dff-bf09-4bd106eb3204"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w1_logistics_explore_v1",
  "description": "Create a spacious RPG exploration underscore at 90 BPM in E Minor. Industrial blues-noir RPG exploration underscore, 90 BPM, E minor. Dry muted electric guitar ostinato, low steady P-bass pulse, rhythmic pneumatic industrial clicks, distant Rhodes chords, dusty analog tape grain. Watchful, pressured corporate facility, uncluttered mids, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Zeke once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, loud guitar solos, happy upbeat pop, singing, nonstop soloing."
}
```

### `music_w1_deep_mine` — Deep Mine & Echo Chamber (Sub-Level 4)
- **Status:** `ready_for_generation` | **Key:** `C Minor / A Modal` | **BPM:** `72` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Deep Mine & Echo Chamber (Sub-Level 4)",
  "instrumental": true,
  "reference_audio_ids": [
    "3f556f02-4430-44c3-a4d9-51ba1d1cad1b"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w1_deep_mine_v1",
  "description": "Create a spacious RPG exploration underscore at 72 BPM in C Minor / A Modal. Dark industrial subterranean exploration underscore, 72 BPM. Deep cavern bass pulse, slow metallic conduit clinks in cave reverb, sparse low acoustic guitar harmonics, and faint mysterious crystalline glass-chime echoes hinting at something ancient buried below. Ominous, restrained, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Source once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, battle drums, bright brass, singing, nonstop soloing."
}
```

### `music_w2_sector9_explore` — Sector 9 Wilds (Wreckage in Bloom)
- **Status:** `ready_for_generation` | **Key:** `A Dorian` | **BPM:** `76` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Sector 9 Wilds (Wreckage in Bloom)",
  "instrumental": true,
  "reference_audio_ids": [
    "3f556f02-4430-44c3-a4d9-51ba1d1cad1b"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w2_sector9_explore_v1",
  "description": "Create a spacious RPG exploration underscore at 76 BPM in A Dorian. Organic dark folk and atmospheric sci-fi jungle exploration, 76 BPM, A Dorian. 12-string acoustic guitar picking, breathy wooden flute melodies, bowed contrabass, soft hand frame drums, warm analog poly-synth swells. Lush, humid, mysterious nature reclaiming ruins, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Source once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, heavy metal, EDM drops, singing, nonstop soloing."
}
```

### `music_w2_tideglass_beach` — Tideglass Beach (Glass Sand & Driftwood)
- **Status:** `ready_for_generation` | **Key:** `A Dorian / G Major` | **BPM:** `72` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Tideglass Beach (Glass Sand & Driftwood)",
  "instrumental": true,
  "reference_audio_ids": [
    "3f556f02-4430-44c3-a4d9-51ba1d1cad1b"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w2_tideglass_beach_v1",
  "description": "Create a spacious RPG exploration underscore at 72 BPM in A Dorian / G Major. Serene shimmering coastal ambient folk, 72 BPM, A Dorian. Warm nylon guitar arpeggios, soft Fender Rhodes swells, delicate glass chimes, solo cello warmth, ocean-like wide analog pad shimmer. Restful wonder and quiet camaraderie after the crash, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Source once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, aggressive drums, scary horror noise, singing, nonstop soloing."
}
```

### `music_w2_ancient_gateway` — Sanctuary Ruins (Hall of Echoes)
- **Status:** `ready_for_generation` | **Key:** `A Dorian / F# Minor` | **BPM:** `78` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Sanctuary Ruins (Hall of Echoes)",
  "instrumental": true,
  "reference_audio_ids": [
    "09388aa3-000f-4f73-b59c-bfb47e5e9a89"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w2_ancient_gateway_v1",
  "description": "Create a spacious RPG exploration underscore at 78 BPM in A Dorian / F# Minor. Sacred ancient sci-fi ruins exploration theme, 78 BPM, A Dorian. Resonant singing bowls, kalimba/harp patterns, expressive wooden flute echoing Orion's motif, deep frame drum pulse, wide cathedral reverb. Reverent, uncanny, alive with ancient memory, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Orion once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, electric metal guitars, trap percussion, singing, nonstop soloing."
}
```

### `music_w2_stasis_sanctuary` — Stasis Chamber (The Sleeping Archive)
- **Status:** `ready_for_generation` | **Key:** `A Dorian` | **BPM:** `66` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Stasis Chamber (The Sleeping Archive)",
  "instrumental": true,
  "reference_audio_ids": [
    "09388aa3-000f-4f73-b59c-bfb47e5e9a89"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w2_stasis_sanctuary_v1",
  "description": "Create a spacious RPG exploration underscore at 66 BPM in A Dorian. Quiet icy-sacred stasis vault underscore, 66 BPM, A Dorian. High glass harmonic drones, sparse solo cello notes quoting Orion's motif, soft rhythmic cryogenic pulse, vast hushed stone acoustics, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Orion once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, heavy drums, fast tempos, singing, nonstop soloing."
}
```

## Batch 8: World 2 Combat/Boss & World 3 The Spire Exploration (6 Cues)

### `music_w3_spire_explore` — Lower Spire (Neon Rain & Night Market)
- **Status:** `ready_for_generation` | **Key:** `C# Minor / E Minor` | **BPM:** `100` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2C: Exploration & Place Loop — Custom Style Key-Lock (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Lower Spire (Neon Rain & Night Market)",
  "instrumental": true,
  "style": "Rain-slicked 80s cyber-noir jazz-synthwave exploration, 100 BPM, C# minor. Slick fretless bassline, warm Fender Rhodes chords, smoky muted saxophone phrases with tape delay, crisp brushed electronic snare, neon analog pads. Hustling, street-smart, covert, seamless loop. Establish a spacious, lived-in groove and atmospheric texture for the first 40 seconds with NO intrusive or busy lead melody, keeping the midrange wide open and uncluttered for RPG dialogue. Only in the middle section (around 0:45-1:05), weave in one brief, understated melodic phrase before returning to the spacious chordal and rhythmic groove for a seamless cyclical loop ending. Strictly instrumental",
  "negative_style": "vocals, loud dubstep drops, heavy metal, singing, busy constant lead melody, nonstop soloing",
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w3_spire_explore_v1"
}
```

### `music_w3_the_static` — The Static & Safehouse (Signal Under the Rail)
- **Status:** `ready_for_generation` | **Key:** `E Minor` | **BPM:** `86` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "The Static & Safehouse (Signal Under the Rail)",
  "instrumental": true,
  "reference_audio_ids": [
    "7d80cd57-8425-4dff-bf09-4bd106eb3204"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w3_the_static_v1",
  "description": "Create a spacious RPG exploration underscore at 86 BPM in E Minor. Cozy underground cyberpunk safehouse lo-fi jazz-synth, 86 BPM, E minor. Warm upright/Rhodes chords, mellow muted guitar, faint vinyl/tape radio static, soft sub-pulse. Crew regrouping in a hidden apartment above the rail, intimate and loyal, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Zeke once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, harsh alarms, battle drums, singing, nonstop soloing."
}
```

### `music_w3_upper_city` — Upper Spire (Glasswalk & Politeness)
- **Status:** `ready_for_generation` | **Key:** `Bb Minor` | **BPM:** `98` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Upper Spire (Glasswalk & Politeness)",
  "instrumental": true,
  "reference_audio_ids": [
    "d6d4ceca-f315-42bb-b1df-f28892534b56"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w3_upper_city_v1",
  "description": "Create a spacious RPG exploration underscore at 98 BPM in Bb Minor. Sleek minimalist cyber-luxury lounge & corporate espionage, 98 BPM, Bb minor. Crystal synthesizer arpeggios, glassy vibraphone chords, cool sterile string swells, deep polished bass pulse, polite clockwork hi-hats. Opulent yet quietly menacing, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Thorne once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, dirty blues guitar, aggressive screams, singing, nonstop soloing."
}
```

### `music_w3_archive_stealth` — Spire Archive (Prism Vault Heist)
- **Status:** `ready_for_generation` | **Key:** `Bb Minor / F# Minor` | **BPM:** `108` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Spire Archive (Prism Vault Heist)",
  "instrumental": true,
  "reference_audio_ids": [
    "d6d4ceca-f315-42bb-b1df-f28892534b56"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w3_archive_stealth_v1",
  "description": "Create a spacious RPG exploration underscore at 108 BPM in Bb Minor / F# Minor. Taut high-stakes sci-fi stealth infiltration underscore, 108 BPM, Bb minor. Ticking precision analog sequencer, filtered sub-bass pulse, muted pizzicato strings, restrained glass synth pings. Clean, suspenseful heist focus without battle drums, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Thorne once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, loud guitar solos, dramatic endings, singing, nonstop soloing."
}
```

### `music_w2_combat` — Swamp Ambush
- **Status:** `ready_for_generation` | **Key:** `A Dorian` | **BPM:** `132` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3C: Standard World Combat Loop — Custom Style Key-Lock (Immediate Bar-1 Drive, Riff/Groove Contrast, No Repetitive 4-Bar Motif Fatigue)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Swamp Ambush",
  "instrumental": true,
  "style": "Energetic organic folk-rock & tribal orchestral JRPG battle theme, 132 BPM, A Dorian. Driving taiko & frame drums, urgent bowed string ostinato, electric bass groove, wild wooden flute trills and crunchy rhythmic guitar. Primal jungle survival, seamless loop. Hit immediately at bar 1 with driving combat rhythm and regional sonic texture in A Dorian at 132 BPM. Alternate tight rhythm-section grooves with dynamic melodic lead flourishes and a contrasting B-section bridge rather than repeating a literal 5-note character melody every 8 bars, preventing ear fatigue across repeated battles. End on an energetic transition turnaround for a seamless combat loop, strictly instrumental",
  "negative_style": "vocals, cheesy EDM, slow lullaby, singing, slow ambient intro, fading outro",
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w2_combat_v1"
}
```

### `music_w2_boss_guardian` — The Source Beast (Canopy Apex)
- **Status:** `ready_for_generation` | **Key:** `D Minor / A Dorian` | **BPM:** `140` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3B: Boss Battle Loop — Custom Style Key-Lock (80% Oppressive Boss Identity & Weight First, 20% Mid-Bridge Defiant Crew Counter-Motif)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "The Source Beast (Canopy Apex)",
  "instrumental": true,
  "style": "Epic orchestral-rock JRPG boss theme, 140 BPM, D minor. Thunderous tribal war drums, relentless cello & violin ostinato, roaring brass stabs, shimmering resonant glass overtones and electric guitar counter-melody. Terrifying ancient predator warped by the Source, seamless loop. Establish the boss's oppressive, menacing rhythmic and timbral weight first at 140 BPM in D Minor / A Dorian—focus on heavy groove, signature boss instrumentation, and tension rather than a nonstop high-pitched guitar solo from bar 1. Only in the middle bridge section, let a defiant heroic counter-phrase break through the boss's rhythm in call-and-response before the full-weight boss riff slams back in for a seamless loop. Strictly instrumental",
  "negative_style": "vocals, funky slap bass, retro chiptune, singing, nonstop high-pitched shred solo, cheerful pop, fading outro",
  "duration": 120,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w2_boss_guardian_v1"
}
```

## Batch 9: World 3 Combat/Bosses & World 4 The Foundry (8 Cues)

### `music_w4_foundry_explore` — Slag Pits (Molten Channels)
- **Status:** `ready_for_generation` | **Key:** `D Minor` | **BPM:** `112` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Slag Pits (Molten Channels)",
  "instrumental": true,
  "reference_audio_ids": [
    "2104928f-0b87-4365-9ada-84dcf1e04264"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w4_foundry_explore_v1",
  "description": "Create a spacious RPG exploration underscore at 112 BPM in D Minor. Grinding industrial blues-rock exploration underscore, 112 BPM, D minor. Low chugging palm-muted guitars, heavy hydraulic machine rhythm, simmering analog bass drone, occasional dark slide harmonics. Oppressive furnace heat, relentless working machinery, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Nova once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, cheerful pop, bright bells, singing, nonstop soloing."
}
```

### `music_w4_power_core` — Assembly Line & Deep Forge
- **Status:** `ready_for_generation` | **Key:** `B Minor` | **BPM:** `122` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2C: Exploration & Place Loop — Custom Style Key-Lock (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Assembly Line & Deep Forge",
  "instrumental": true,
  "style": "Driven dark industrial rock & synth-metal exploration, 122 BPM, B minor. Mechanized conveyor rhythm, crunchy bass synth, sharp metallic percussion, tense electric guitar motifs and urgent analog arpeggios. Volatile reactor tension, seamless loop. Establish a spacious, lived-in groove and atmospheric texture for the first 40 seconds with NO intrusive or busy lead melody, keeping the midrange wide open and uncluttered for RPG dialogue. Only in the middle section (around 0:45-1:05), weave in one brief, understated melodic phrase before returning to the spacious chordal and rhythmic groove for a seamless cyclical loop ending. Strictly instrumental",
  "negative_style": "vocals, serene acoustic folk, singing, busy constant lead melody, nonstop soloing",
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w4_power_core_v1"
}
```

### `music_w4_conditioning` — Conditioning Wing (Unit 734)
- **Status:** `ready_for_generation` | **Key:** `F# Minor` | **BPM:** `94` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Conditioning Wing (Unit 734)",
  "instrumental": true,
  "reference_audio_ids": [
    "60b741bd-2489-4f45-a55d-41c2b7589ca3"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w4_conditioning_v1",
  "description": "Create a spacious RPG exploration underscore at 94 BPM in F# Minor. Cold unsettling industrial-noir theme echoing Gh0st's Theme, 94 BPM, F# minor. Mechanical assembly clicks, dark tape-delayed clean guitar arpeggios, hollow cold synth choir hum, faint broken music-box celesta overtones. Personal reckoning with manufactured origins, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Gh0st once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, upbeat groove, heroic fanfare, singing, nonstop soloing."
}
```

### `music_w3_combat` — Spire Security Chase
- **Status:** `ready_for_generation` | **Key:** `C# Minor` | **BPM:** `136` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3C: Standard World Combat Loop — Custom Style Key-Lock (Immediate Bar-1 Drive, Riff/Groove Contrast, No Repetitive 4-Bar Motif Fatigue)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Spire Security Chase",
  "instrumental": true,
  "style": "High-energy 80s darksynth-funk JRPG battle theme, 136 BPM, C# minor. Snapping fretless slap bass, pulsing analog saw leads, punchy retro electronic/rock drums, gritty organ/Rhodes stabs. Slick urban heist getaway under laser fire, seamless loop. Hit immediately at bar 1 with driving combat rhythm and regional sonic texture in C# Minor at 136 BPM. Alternate tight rhythm-section grooves with dynamic melodic lead flourishes and a contrasting B-section bridge rather than repeating a literal 5-note character melody every 8 bars, preventing ear fatigue across repeated battles. End on an energetic transition turnaround for a seamless combat loop, strictly instrumental",
  "negative_style": "vocals, slow ambient, rustic banjo, singing, slow ambient intro, fading outro",
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w3_combat_v1"
}
```

### `music_w4_combat` — Foundry Line Battle
- **Status:** `ready_for_generation` | **Key:** `D Minor` | **BPM:** `144` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3C: Standard World Combat Loop — Custom Style Key-Lock (Immediate Bar-1 Drive, Riff/Groove Contrast, No Repetitive 4-Bar Motif Fatigue)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Foundry Line Battle",
  "instrumental": true,
  "style": "Relentless industrial metal JRPG combat theme, 144 BPM, D minor. Down-tuned chugging metal guitars, pneumatic hammer percussion, ferocious double-kick rock drums, screaming analog synth bass, blazing lead guitar melody. Blistering molten combat, seamless loop. Hit immediately at bar 1 with driving combat rhythm and regional sonic texture in D Minor at 144 BPM. Alternate tight rhythm-section grooves with dynamic melodic lead flourishes and a contrasting B-section bridge rather than repeating a literal 5-note character melody every 8 bars, preventing ear fatigue across repeated battles. End on an energetic transition turnaround for a seamless combat loop, strictly instrumental",
  "negative_style": "vocals, calm ambient, soft jazz, singing, slow ambient intro, fading outro",
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w4_combat_v1"
}
```

### `music_w3_boss_administrator` — The Administrator (Polite Annihilation)
- **Status:** `ready_for_generation` | **Key:** `Bb Minor` | **BPM:** `142` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3B: Boss Battle Loop — Custom Style Key-Lock (80% Oppressive Boss Identity & Weight First, 20% Mid-Bridge Defiant Crew Counter-Motif)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "The Administrator (Polite Annihilation)",
  "instrumental": true,
  "style": "Sleek terrifying cyber-luxury JRPG boss theme, 142 BPM, Bb minor. Blistering glass synth arpeggios, cold analog brass stabs quoting Thorne's Bbm theme, razor-sharp electronic breakbeats, distorted sub-bass, soaring electric guitar duel. High-society security AI smiling while venting the room, seamless loop. Establish the boss's oppressive, menacing rhythmic and timbral weight first at 142 BPM in Bb Minor—focus on heavy groove, signature boss instrumentation, and tension rather than a nonstop high-pitched guitar solo from bar 1. Only in the middle bridge section, let a defiant heroic counter-phrase break through the boss's rhythm in call-and-response before the full-weight boss riff slams back in for a seamless loop. Strictly instrumental",
  "negative_style": "vocals, warm folk guitar, singing, nonstop high-pitched shred solo, cheerful pop, fading outro",
  "duration": 120,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w3_boss_administrator_v1"
}
```

### `music_w3_boss_phantom` — Phantom Prototype (Phase Blade Duel)
- **Status:** `ready_for_generation` | **Key:** `F# Minor` | **BPM:** `146` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3B: Boss Battle Loop — Custom Style Key-Lock (80% Oppressive Boss Identity & Weight First, 20% Mid-Bridge Defiant Crew Counter-Motif)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Phantom Prototype (Phase Blade Duel)",
  "instrumental": true,
  "style": "Hyper-kinetic darkwave synth-metal duel quoting Gh0st's F# minor motif, 146 BPM. Dueling phase-shifted electric guitars, blistering 16th-note analog arpeggios, tight breakbeat rock drums, ghostly celesta echoes flickering underneath. Facing a hollow copy built without a self, seamless loop. Establish the boss's oppressive, menacing rhythmic and timbral weight first at 146 BPM in F# Minor—focus on heavy groove, signature boss instrumentation, and tension rather than a nonstop high-pitched guitar solo from bar 1. Only in the middle bridge section, let a defiant heroic counter-phrase break through the boss's rhythm in call-and-response before the full-weight boss riff slams back in for a seamless loop. Strictly instrumental",
  "negative_style": "vocals, slow ballads, singing, nonstop high-pitched shred solo, cheerful pop, fading outro",
  "duration": 120,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w3_boss_phantom_v1"
}
```

### `music_w4_boss_titan` — Titan Walker & Crucible (Molten Siege)
- **Status:** `ready_for_generation` | **Key:** `B Minor / D Minor` | **BPM:** `150` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3B: Boss Battle Loop — Custom Style Key-Lock (80% Oppressive Boss Identity & Weight First, 20% Mid-Bridge Defiant Crew Counter-Motif)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Titan Walker & Crucible (Molten Siege)",
  "instrumental": true,
  "style": "Colossal industrial symphonic-metal boss battle, 150 BPM, B minor. Multi-ton hydraulic stomps, scorching twin electric guitar solos quoting Nova's defiant theme, blaring industrial brass horns, relentless double-kick drumming, molten analog synth sweeps. Fighting Commander Rylos's siege frame over a volcano vent, seamless loop. Establish the boss's oppressive, menacing rhythmic and timbral weight first at 150 BPM in B Minor / D Minor—focus on heavy groove, signature boss instrumentation, and tension rather than a nonstop high-pitched guitar solo from bar 1. Only in the middle bridge section, let a defiant heroic counter-phrase break through the boss's rhythm in call-and-response before the full-weight boss riff slams back in for a seamless loop. Strictly instrumental",
  "negative_style": "vocals, delicate music box, singing, nonstop high-pitched shred solo, cheerful pop, fading outro",
  "duration": 130,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w4_boss_titan_v1"
}
```

## Batch 10: World 5 Orbital Ring & Deep Ring (6 Cues)

### `music_w5_orbital_intrigue` — Grand Concourse (Borrowed Time)
- **Status:** `ready_for_generation` | **Key:** `C Minor` | **BPM:** `80` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Grand Concourse (Borrowed Time)",
  "instrumental": true,
  "reference_audio_ids": [
    "991e19c9-97e8-4658-b538-b70d550eb31b"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w5_orbital_intrigue_v1",
  "description": "Create a spacious RPG exploration underscore at 80 BPM in C Minor. Restrained orbital espionage and uneasy human resolve, 80 BPM, C minor, 4/4 underscore. Warm analog sequencer eighth-notes, rounded low synth bass, light dry electronic ticks, narrow cold radar tones answering muted electric-piano chords, subtle C-B-Bb-A figure. Uncluttered mids for dialogue, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Vale once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, alarm sirens, battle drums, triumphant climax, singing, nonstop soloing."
}
```

### `music_w5_solarium` — Orbital Solarium (Artificial Paradise)
- **Status:** `ready_for_generation` | **Key:** `C Minor / Eb Major` | **BPM:** `74` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2C: Exploration & Place Loop — Custom Style Key-Lock (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Orbital Solarium (Artificial Paradise)",
  "instrumental": true,
  "style": "Uncanny pristine chamber waltz-ambient inside a gilded orbital cage, 74 BPM. Too-clean glass harp, cool celesta, delicate string quartet harmonics laced with subtle dissonant bass undertone. Artificial beauty masking trapped lives, seamless loop. Establish a spacious, lived-in groove and atmospheric texture for the first 40 seconds with NO intrusive or busy lead melody, keeping the midrange wide open and uncluttered for RPG dialogue. Only in the middle section (around 0:45-1:05), weave in one brief, understated melodic phrase before returning to the spacious chordal and rhythmic groove for a seamless cyclical loop ending. Strictly instrumental",
  "negative_style": "vocals, distortion, heavy drums, singing, busy constant lead melody, nonstop soloing",
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w5_solarium_v1"
}
```

### `music_w5_security_hub` — Deep Ring Mainframe (Silent Surveillance)
- **Status:** `ready_for_generation` | **Key:** `G# Minor / C Minor` | **BPM:** `84` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Deep Ring Mainframe (Silent Surveillance)",
  "instrumental": true,
  "reference_audio_ids": [
    "991e19c9-97e8-4658-b538-b70d550eb31b"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w5_security_hub_v1",
  "description": "Create a spacious RPG exploration underscore at 84 BPM in G# Minor / C Minor. Cold minimalist sci-fi chamber electronica, 84 BPM, C minor. High sterile felt piano notes echoing in server coldness, ticking clock percussion, pulsing square-wave sub, icy string sustains. Clinical surveillance pressure, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Vale once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, warm acoustic guitar, celebratory brass, singing, nonstop soloing."
}
```

### `music_w5_anchor_cathedral` — Anchor Containment & Command Deck (One Breath)
- **Status:** `ready_for_generation` | **Key:** `C Minor / F# Major tension` | **BPM:** `72` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Anchor Containment & Command Deck (One Breath)",
  "instrumental": true,
  "reference_audio_ids": [
    "991e19c9-97e8-4658-b538-b70d550eb31b"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w5_anchor_cathedral_v1",
  "description": "Create a spacious RPG exploration underscore at 72 BPM in C Minor / F# Major tension. Chilling neo-classical sci-fi suspense, 72 BPM, C minor. Sparse cold pipe organ drone and descending piano notes colliding with fragile high celesta harmonics and taut string tremolos. Approaching Vale's Tear while holding onto Elara, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Vale once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, dance beat, cozy lounge, singing, nonstop soloing."
}
```

### `music_w5_combat` — Zero-G Skirmish
- **Status:** `ready_for_generation` | **Key:** `C Minor` | **BPM:** `154` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3C: Standard World Combat Loop — Custom Style Key-Lock (Immediate Bar-1 Drive, Riff/Groove Contrast, No Repetitive 4-Bar Motif Fatigue)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Zero-G Skirmish",
  "instrumental": true,
  "style": "Cold neo-classical chamber drum-and-bass / breakbeat JRPG battle, 154 BPM, C minor. Rapid clipped breakbeats, frantic chamber string ostinato, sharp harpsichord/piano runs, deep Reese bassline, clinical synth stabs. Surgical precision inside orbital security, seamless loop. Hit immediately at bar 1 with driving combat rhythm and regional sonic texture in C Minor at 154 BPM. Alternate tight rhythm-section grooves with dynamic melodic lead flourishes and a contrasting B-section bridge rather than repeating a literal 5-note character melody every 8 bars, preventing ear fatigue across repeated battles. End on an energetic transition turnaround for a seamless combat loop, strictly instrumental",
  "negative_style": "vocals, rustic blues, cozy jazz, singing, slow ambient intro, fading outro",
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w5_combat_v1"
}
```

### `music_w5_boss_compliance` — Compliance Avatar (Orbital Lockdown)
- **Status:** `ready_for_generation` | **Key:** `C Minor` | **BPM:** `148` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3B: Boss Battle Loop — Custom Style Key-Lock (80% Oppressive Boss Identity & Weight First, 20% Mid-Bridge Defiant Crew Counter-Motif)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Compliance Avatar (Orbital Lockdown)",
  "instrumental": true,
  "style": "Clinical authoritarian neo-classical synth-metal boss theme, 148 BPM, C minor. Rapid pipe organ and harpsichord ostinatos, chugging metal guitars, robotic snare precision, relentless 4-note C-B-Bb-A chromatic horn stabs. Vale's orbital compliance machine turning deadly, seamless loop. Establish the boss's oppressive, menacing rhythmic and timbral weight first at 148 BPM in C Minor—focus on heavy groove, signature boss instrumentation, and tension rather than a nonstop high-pitched guitar solo from bar 1. Only in the middle bridge section, let a defiant heroic counter-phrase break through the boss's rhythm in call-and-response before the full-weight boss riff slams back in for a seamless loop. Strictly instrumental",
  "negative_style": "vocals, warm country blues, singing, nonstop high-pitched shred solo, cheerful pop, fading outro",
  "duration": 120,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w5_boss_compliance_v1"
}
```

## Batch 11: World 6 The Source, Nightmares & New World (7 Cues)

### `music_w6_campfire` — The Gathering Fire (Still Ourselves)
- **Status:** `ready_for_generation` | **Key:** `D Minor / G Major` | **BPM:** `68` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "The Gathering Fire (Still Ourselves)",
  "instrumental": true,
  "reference_audio_ids": [
    "2104928f-0b87-4365-9ada-84dcf1e04264"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w6_campfire_v1",
  "description": "Create a spacious RPG exploration underscore at 68 BPM in D Minor / G Major. Intimate fragile acoustic hearth inside surreal cosmic emptiness, 68 BPM, D minor. Solo warm fingerpicked acoustic guitar and soft cello duet echoing Nova and Jed's warmth surrounded by faint distant glass harmonic breeze. Quiet refuge before walking into nightmares, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Nova once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, percussion, industrial metal, singing, nonstop soloing."
}
```

### `music_w6_nightmare_zeke` — Zeke's Nightmare (Endless Performance Review)
- **Status:** `ready_for_generation` | **Key:** `E Minor` | **BPM:** `78` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Zeke's Nightmare (Endless Performance Review)",
  "instrumental": true,
  "reference_audio_ids": [
    "7d80cd57-8425-4dff-bf09-4bd106eb3204"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w6_nightmare_zeke_v1",
  "description": "Create a spacious RPG exploration underscore at 78 BPM in E Minor. Warped surreal dark-jazz nightmare variation of Zeke's Rhodes theme, 78 BPM, E minor. Detuned tape-wobbly Fender Rhodes, oppressive ticking office clockwork, claustrophobic low bass drone, muffled checkpoint buzzer textures. Psychological bureaucracy nightmare, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Zeke once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, triumphant rock, bright pop, singing, nonstop soloing."
}
```

### `music_w6_nightmare_gh0st` — Gh0st's Nightmare (Kill-Suite Loop)
- **Status:** `ready_for_generation` | **Key:** `F# Minor -> F# Major` | **BPM:** `96` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Gh0st's Nightmare (Kill-Suite Loop)",
  "instrumental": true,
  "reference_audio_ids": [
    "60b741bd-2489-4f45-a55d-41c2b7589ca3"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w6_nightmare_gh0st_v1",
  "description": "Create a spacious RPG exploration underscore at 96 BPM in F# Minor -> F# Major. Haunting glitch-noir and fractured memory variation of Gh0st's Theme, 96 BPM, F# minor. Mechanical glitch stutters and dark delay guitar locked in a loop, pierced from underneath by a clear stubborn F# major celesta fragment of Elara's lullaby. Emotional defiance inside command noise, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Gh0st once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, happy funk, comedy, singing, nonstop soloing."
}
```

### `music_w6_nightmare_orion` — Orion's Nightmare (The Silent Shore)
- **Status:** `ready_for_generation` | **Key:** `A Dorian` | **BPM:** `62` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Orion's Nightmare (The Silent Shore)",
  "instrumental": true,
  "reference_audio_ids": [
    "09388aa3-000f-4f73-b59c-bfb47e5e9a89"
  ],
  "duration": 90,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w6_nightmare_orion_v1",
  "description": "Create a spacious RPG exploration underscore at 62 BPM in A Dorian. Hollow surreal elegy in 3/4 time echoing Orion's Theme, 62 BPM, A Dorian. Distant bowed cello harmonics, reversed tape swells, lonely solo wood flute answers fading into vast empty ocean-cavern reverb. Grief tempting him to fade away, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Orion once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, drums, heavy metal, singing, nonstop soloing."
}
```

### `music_w6_source_explore` — World-Fracture & Memory Stair (Fragments)
- **Status:** `ready_for_generation` | **Key:** `A Modal / D Minor` | **BPM:** `64` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "World-Fracture & Memory Stair (Fragments)",
  "instrumental": true,
  "reference_audio_ids": [
    "3f556f02-4430-44c3-a4d9-51ba1d1cad1b"
  ],
  "duration": 100,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w6_source_explore_v1",
  "description": "Create a spacious RPG exploration underscore at 64 BPM in A Modal / D Minor. Surreal transcendental ambient-orchestral journey, 64 BPM, A Dorian/D minor. Ethereal wordless choral shimmer, glass armonica, fragmented acoustic guitar and celesta echoes drifting across reverse analog tape swells and deep sub-harmonic tides. Profound revelation, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Source once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude modern EDM drops, upbeat pop, harsh synthwave, vocals, singing, nonstop soloing."
}
```

### `music_w6_the_center` — The White Shore (Heart of the Source)
- **Status:** `ready_for_generation` | **Key:** `D Modal / C Minor tension` | **BPM:** `70` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "The White Shore (Heart of the Source)",
  "instrumental": true,
  "reference_audio_ids": [
    "3f556f02-4430-44c3-a4d9-51ba1d1cad1b"
  ],
  "duration": 100,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w6_the_center_v1",
  "description": "Create a spacious RPG exploration underscore at 70 BPM in D Modal / C Minor tension. Monumental sacred choral and pipe organ underscore at the edge of reality, 70 BPM. Vast polyphonic wordless choir, cathedral pipe organ, crystalline singing bowls, taut low orchestral strings building suspended awe before the final confrontation, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Source once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude drums, rock guitar, playful instruments, vocals, singing, nonstop soloing."
}
```

### `music_w6_new_world` — Beyond the Silence (Regional Opening)
- **Status:** `ready_for_generation` | **Key:** `D Major` | **BPM:** `76` | **Loop:** `True`
- **Leitmotif Tier:** Tier 2R: Exploration & Place Loop — Reference-Conditioned (80% Place/Groove, 20% Mid-Track Motif; No Lead Melody First 40s)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Beyond the Silence (Regional Opening)",
  "instrumental": true,
  "reference_audio_ids": [
    "2104928f-0b87-4365-9ada-84dcf1e04264"
  ],
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w6_new_world_v1",
  "description": "Create a spacious RPG exploration underscore at 76 BPM in D Major. Radiant grounded sunrise acoustic-orchestral resolution, 76 BPM, D major. Warm steel-string acoustic guitar playing Nova's resolved major motif, lush warm strings, gentle Rhodes, faint natural morning air. Human survival, openness, and quiet hope, seamless loop. Follow strict leitmotif restraint for long-session exploration: establish the atmospheric groove, chordal accompaniment, and room texture first for the opening 40-45 seconds with NO intrusive or repetitive lead melody, keeping the midrange wide open for NPC dialogue. Only in the middle B-section (around 0:45-1:08), let a single understated lead phrase gently state or echo the melodic motif from Starborn Motif - Nova once as an emotional thread, then strip back into the spacious groove for a seamless cyclical loop handoff. Strictly instrumental; exclude vocals, aggressive drums, ominous dissonance, singing, nonstop soloing."
}
```

## Batch 12: World 6 Finale Bosses & Story Cinematics (9 Cues)

### `music_w6_combat` — Fracture Echo Battle
- **Status:** `ready_for_generation` | **Key:** `A Dorian / D Minor` | **BPM:** `144` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3C: Standard World Combat Loop — Custom Style Key-Lock (Immediate Bar-1 Drive, Riff/Groove Contrast, No Repetitive 4-Bar Motif Fatigue)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Fracture Echo Battle",
  "instrumental": true,
  "style": "Transcendental symphonic rock JRPG battle, 144 BPM, D minor. Soaring electric lead guitar and dramatic string orchestra, wordless choral swells, thunderous live drums, crystalline glass resonance hits. Battling living memories inside the Tear, seamless loop. Hit immediately at bar 1 with driving combat rhythm and regional sonic texture in A Dorian / D Minor at 144 BPM. Alternate tight rhythm-section grooves with dynamic melodic lead flourishes and a contrasting B-section bridge rather than repeating a literal 5-note character melody every 8 bars, preventing ear fatigue across repeated battles. End on an energetic transition turnaround for a seamless combat loop, strictly instrumental",
  "negative_style": "pop lyrics, comedic brass, vocals, singing, slow ambient intro, fading outro",
  "duration": 96,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w6_combat_v1"
}
```

### `music_w6_boss_final` — Ascended Vale: The Soloist (Phase 1)
- **Status:** `ready_for_generation` | **Key:** `C Minor <-> D Minor` | **BPM:** `152` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3B: Boss Battle Loop — Custom Style Key-Lock (80% Oppressive Boss Identity & Weight First, 20% Mid-Bridge Defiant Crew Counter-Motif)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Ascended Vale: The Soloist (Phase 1)",
  "instrumental": true,
  "style": "Grand JRPG symphonic rock final boss Part 1, 152 BPM, C minor clashing against D minor. Massive church organ and cold unison choir pounding Vale's 4-note chromatic descent, answered defiantly by soaring lead electric guitar and passionate orchestral strings carrying Nova's rising motif. Ideological clash of wills, seamless loop. Establish the boss's oppressive, menacing rhythmic and timbral weight first at 152 BPM in C Minor <-> D Minor—focus on heavy groove, signature boss instrumentation, and tension rather than a nonstop high-pitched guitar solo from bar 1. Only in the middle bridge section, let a defiant heroic counter-phrase break through the boss's rhythm in call-and-response before the full-weight boss riff slams back in for a seamless loop. Strictly instrumental",
  "negative_style": "pop vocals, lo-fi chill, vocals, singing, nonstop high-pitched shred solo, cheerful pop, fading outro",
  "duration": 150,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w6_boss_final_v1"
}
```

### `music_w6_boss_vale_p2` — Ascended Silence (Final Climax Phase 2)
- **Status:** `ready_for_generation` | **Key:** `C Minor -> D Major` | **BPM:** `158` | **Loop:** `True`
- **Leitmotif Tier:** Tier 3B: Boss Battle Loop — Custom Style Key-Lock (80% Oppressive Boss Identity & Weight First, 20% Mid-Bridge Defiant Crew Counter-Motif)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Ascended Silence (Final Climax Phase 2)",
  "instrumental": true,
  "style": "Towering all-out JRPG final boss climax symphony, 158 BPM, C minor erupting into blazing D major. Full 60-piece orchestra, dramatic wordless operatic choir, pipe organ, blistering electric guitar heroics, and glimmering celesta resonance uniting the whole crew's circuit against absolute silence, seamless loop. Establish the boss's oppressive, menacing rhythmic and timbral weight first at 158 BPM in C Minor -> D Major—focus on heavy groove, signature boss instrumentation, and tension rather than a nonstop high-pitched guitar solo from bar 1. Only in the middle bridge section, let a defiant heroic counter-phrase break through the boss's rhythm in call-and-response before the full-weight boss riff slams back in for a seamless loop. Strictly instrumental",
  "negative_style": "modern EDM trap, dry minimalist silence, vocals, singing, nonstop high-pitched shred solo, cheerful pop, fading outro",
  "duration": 160,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_w6_boss_vale_p2_v1"
}
```

### `music_intro_breach` — Sector 9 Breach (Cold Open)
- **Status:** `ready_for_generation` | **Key:** `A Dorian -> D Minor` | **BPM:** `135` | **Loop:** `False`
- **Leitmotif Tier:** Tier 3S: Cinematic Action / Fanfare One-Shot — Custom Style (Narrative Pacing + Clean Ending Cadence)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Sector 9 Breach (Cold Open)",
  "instrumental": true,
  "style": "32-second high-stakes sci-fi JRPG cold-open cinematic score, 135 BPM. Urgent pulsing strings and containment breach tension (0-14s), dropping suddenly into shimmering sacred wide-stereo singing bowl & choir chord (14-22s), shocking dissonance impact (23s), resolving into solo acoustic guitar playing Nova's 5-note rising theme. Narrative one-shot pacing (32s) with a clean, decisive musical ending cadence. Strictly instrumental",
  "negative_style": "vocals, upbeat pop, singing",
  "duration": 32,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_intro_breach_v1"
}
```

### `music_crash_flight` — Pod Trajectory & Shield Impact
- **Status:** `ready_for_generation` | **Key:** `D Minor` | **BPM:** `140` | **Loop:** `False`
- **Leitmotif Tier:** Tier 3S: Cinematic Action / Fanfare One-Shot — Custom Style (Narrative Pacing + Clean Ending Cadence)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Pod Trajectory & Shield Impact",
  "instrumental": true,
  "style": "45-second escalating sci-fi escape pod crash score, 140 BPM, D minor. Accelerating urgent string ostinatos, analog alarm synths, rushing atmospheric wind roar, culminating in violent impact hit decaying into quiet rain. Narrative one-shot pacing (45s) with a clean, decisive musical ending cadence. Strictly instrumental",
  "negative_style": "vocals, joyful brass, singing",
  "duration": 45,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_crash_flight_v1"
}
```

### `music_cinematic_prologue` — The Launch & The Fall
- **Status:** `ready_for_generation` | **Key:** `D Minor` | **BPM:** `78` | **Loop:** `False`
- **Leitmotif Tier:** Tier 1C: Narrative Cinematic Suite — Reference-Conditioned (Story Arc Resolution with Motif Reprise)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "The Launch & The Fall",
  "instrumental": true,
  "reference_audio_ids": [
    "2104928f-0b87-4365-9ada-84dcf1e04264"
  ],
  "duration": 45,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_cinematic_prologue_v1",
  "description": "Use the melodic motif from Starborn Motif - Nova to score a narrative JRPG cinematic cue at 78 BPM in D Minor (45 seconds). 45-second emotional narrative cinematic cue, 78 BPM, D minor. Solo acoustic guitar evolving into warm French horn and urgent strings, abrupt radio crackle tension. Pace the motif with cinematic restraint: let the scene's atmosphere and harmonic progression breathe first, weave in the ascending motif at the emotional turning point, and resolve naturally to a clean ending cadence. Strictly instrumental; exclude vocals, trap drums, singing."
}
```

### `music_cinematic_crash` — Planetary Impact
- **Status:** `ready_for_generation` | **Key:** `C Drone` | **BPM:** `52` | **Loop:** `False`
- **Leitmotif Tier:** Tier 3S: Cinematic Action / Fanfare One-Shot — Custom Style (Narrative Pacing + Clean Ending Cadence)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Planetary Impact",
  "instrumental": true,
  "style": "36-second atmospheric crash aftermath score, 52 BPM. Sub-bass impact shock, descending cello harmonics, ear-ringing glass frequency fading softly into humid jungle rain ambience. Narrative one-shot pacing (36s) with a clean, decisive musical ending cadence. Strictly instrumental",
  "negative_style": "vocals, fast percussion, singing",
  "duration": 36,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_cinematic_crash_v1"
}
```

### `music_epilogue` — Morning in the Colony (Epilogue)
- **Status:** `ready_for_generation` | **Key:** `G Major / D Major` | **BPM:** `70` | **Loop:** `False`
- **Leitmotif Tier:** Tier 1C: Narrative Cinematic Suite — Reference-Conditioned (Story Arc Resolution with Motif Reprise)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "Morning in the Colony (Epilogue)",
  "instrumental": true,
  "reference_audio_ids": [
    "2104928f-0b87-4365-9ada-84dcf1e04264"
  ],
  "duration": 140,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_epilogue_v1",
  "description": "Use the melodic motif from Starborn Motif - Nova to score a narrative JRPG cinematic cue at 70 BPM in G Major / D Major (140 seconds). Heartfelt grounded JRPG epilogue piece, 70 BPM, G major into D major. Intimate fingerpicked acoustic guitar, warm Fender Rhodes chords, gentle nostalgic harmonica breath honoring Jed, soft warm strings, plus one faint delicate high glass harmonic echoing Nova's lasting tinnitus. Peaceful ordinary morning after survival. Pace the motif with cinematic restraint: let the scene's atmosphere and harmonic progression breathe first, weave in the ascending motif at the emotional turning point, and resolve naturally to a clean ending cadence. Strictly instrumental; exclude vocals, loud rock drums, dark horror, singing."
}
```

### `music_credits_ending` — The Great Frontier (End Credits Suite)
- **Status:** `ready_for_generation` | **Key:** `D Major` | **BPM:** `106` | **Loop:** `False`
- **Leitmotif Tier:** Tier 1C: Narrative Cinematic Suite — Reference-Conditioned (Story Arc Resolution with Motif Reprise)
- **Suno MCP Tool:** `audio-create`
```json
{
  "title": "The Great Frontier (End Credits Suite)",
  "instrumental": true,
  "reference_audio_ids": [
    "2104928f-0b87-4365-9ada-84dcf1e04264"
  ],
  "duration": 180,
  "duration_strict": false,
  "workspace_id": "c2259127-5f36-43d3-8024-09a306074382",
  "idempotency_key": "starborn_music_credits_ending_v1",
  "description": "Use the melodic motif from Starborn Motif - Nova to score a narrative JRPG cinematic cue at 106 BPM in D Major (180 seconds). 80s analog cassette end-credits soft-rock/synth-pop suite, 106 BPM, D major. Chiming 12-string acoustic guitar, warm Juno poly-synths, punchy 80s Simmons/acoustic drums, soulful saxophone solo, triumphant electric guitar reprise of Nova's rising hook. Warm joyful nostalgia looking toward open stars. Pace the motif with cinematic restraint: let the scene's atmosphere and harmonic progression breathe first, weave in the ascending motif at the emotional turning point, and resolve naturally to a clean ending cadence. Strictly instrumental; exclude harsh screamo, modern trap, vocals, singing."
}
```
