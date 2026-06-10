# Starborn — Music Production & Leitmotif Process
**Status:** Creative Blueprint & Generation Guide  
**Focus:** JRPG Motif Design | ElevenLabs Music prompting | Variation & Theme Adaptation

---

## 1. The JRPG Musical Architecture
In a JRPG, music is a narrative engine. By using recurring musical signatures (**leitmotifs**), we anchor the player's emotional state, trace character arcs, and signal thematic shifts. 

For *Starborn*, we define three core leitmotifs that represent the central elements of our story:

```
                  +----------------------------------+
                  |      The Starborn Motif Trio     |
                  +----------------------------------+
                                   |
         +-------------------------+-------------------------+
         |                         |                         |
         v                         v                         v
   [Nova's Theme]            [Vale's Theme]           [Veyra's Chorus]
    "The Spark"              "The Soloist"             "The Current"
  5-note ascending          4-note chromatic          3-chord modal
  Acoustic to Metal         High-register Piano       Choral Choir Pad
```

---

## 2. Core Leitmotif Designs

### A. Nova's Motif: "The Spark"
*   **Thematic Meaning:** Hope, struggle, defiance, and found family. Starts lonely and dusty, becomes powerful and electric, and ends with a sacrificial fading echo.
*   **Musical Structure:** A simple, optimistic **5-note ascending scale fragment** (e.g., `E - G - A - B - D` in E Minor). It is highly active, rhythmic, and always striving upward.
*   **Prompt Anchor Phrase:** *"A prominent, driving 5-note ascending melodic hook"*

### B. Vale's Motif: "The Soloist"
*   **Thematic Meaning:** Absolute control, cold mercy, and the erasure of individuality. It sounds perfect, sterile, and inevitable.
*   **Musical Structure:** A **4-note descending chromatic line** (e.g., `C - B - Bb - A` in C Minor). It is slow, rigid, and resolves downward, representing constriction and fading identity.
*   **Prompt Anchor Phrase:** *"A chilling, slow 4-note descending chromatic melody"*

### C. Veyra's Motif: "The Chorus"
*   **Thematic Meaning:** The Source, the psionic ocean, the flowing currents of consciousness. It is vast, deep, and borderless.
*   **Musical Structure:** A sweeping, unresolved **3-chord progression** (e.g., `Amin9 - Fmaj7 - G6` in A Dorian). It has no clear resolution, mimicking the rise and fall of ocean tides.
*   **Prompt Anchor Phrase:** *"A shifting, unresolved 3-chord chordal pad progression"*

---

## 3. The ElevenLabs Music Prompting Engine
Because generative AI models do not share a persistent musical "memory" (like a MIDI file or project file), we must use **structural prompt engineering** to force the generator to create melodic consistency.

### The Standard Prompt Template
To generate cohesive variations, wrap your prompts in this exact 6-part template:
```
[GENRE/VIBE] -> [TEMPO/BPM] -> [LEAD INSTRUMENTS] -> [RHYTHM/SPACE] -> [EMOTIONAL CUE] -> [MOTIF ANCHOR PHRASE]
```

### Prompt Formulas for Variations

#### Nova's Theme: "The Spark" (Acoustic -> Metal -> Symphonic)

*   **World 1 (Starting Area / Bluesy Acoustic):**
    > `Warm working-class acoustic blues, 80 BPM, slide resonator guitar, acoustic steel-string fingerpicking, dusty hand percussion, intimate cabin reverb. Nostalgic and determined mood. Featuring a prominent, slow 5-note ascending melodic hook on a steel-string guitar.`
*   **World 1 (Combat / Harder Rock):**
    > `Aggressive blues-rock, 130 BPM, overdriven slide electric guitar, gritty rhythm section, punchy drum kit, dry room acoustics. Energetic and defiant mood. Featuring a prominent, driving 5-note ascending melodic hook played on a distorted electric lead guitar.`
*   **World 4 (Foundry Combat / Metal):**
    > `Aggressive industrial metal, 145 BPM, chugging distorted electric rhythm guitars, heavy double-kick drums, grinding synth bass, wide industrial warehouse reverb. Furious and heavy mood. Featuring a prominent, driving 5-note ascending melodic hook played as a fast, screaming electric guitar solo.`
*   **World 6 (Climax / Symphonic JRPG Rock):**
    > `Epic JRPG symphonic rock, 120 BPM, soaring electric guitar lead, full orchestra, dramatic strings, grand brass section, church organ, massive cathedral reverb. High-stakes emotional climax. Featuring a prominent, driving 5-note ascending melodic hook played in unison by the lead guitar and violins.`

#### Vale's Theme: "The Soloist" (Melancholy -> Electronic -> Pure Choral)

*   **World 5 (Void Station / Solo Piano):**
    > `Melancholic solo piano, 70 BPM, high-register classical piano keys, long sustain, vast empty sterile room reverb. Cold, lonely, and clinical mood. Featuring a chilling, slow 4-note descending chromatic melody played on the piano.`
*   **World 5 (Vale's Boss Fight / Orchestral Metal):**
    > `Epic neo-classical orchestral metal, 135 BPM, rapid church organ ostinatos, heavy chugging metal guitars, thunderous drums, sharp harpsichord runs, cold empty room acoustics. Intense, theatrical conflict. Featuring a chilling, slow 4-note descending chromatic melody played by the pipe organ and brass.`
*   **World 6 (Melting Reality / Choral Distortion):**
    > `Surreal experimental ambient, 60 BPM, massive cathedral choir, pitch-shifted vocal pads, acoustic waveforms warping, sub-bass pressure, wet cavernous reverb. Haunting, cosmic horror mood. Featuring a chilling, slow 4-note descending chromatic melody sung by a sterile operatic choir.`

---

## 4. The Audio Production Process (Step-by-Step)
Use this 4-step workflow to build, refine, and integrate the game's audio assets:

```
+--------------------+      +--------------------+      +--------------------+      +--------------------+
| 1. Generate & Sort | ---> |  2. Edit & Loop    | ---> | 3. Register & Bind | ---> |   4. Test & Tune   |
|   Prompt variations|      | Trim tails, adjust |      | Update JSON catalogs|      | Run game, check    |
|   using ElevenLabs  |      | volume envelopes   |      | & room audio bindings|      | fades & transition |
+--------------------+      +--------------------+      +--------------------+      +--------------------+
```

### Step 1: Generation & Curation
We have provided an automated script, [generate_audio.py](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/scripts/generate_audio.py), that interfaces directly with the ElevenLabs API to generate music, sound effects, and voiceovers.

#### How to Run the Script:
1.  **Set your API Key:** Export it as an environment variable or place it in `elevenlabs_api_key.txt` in the project root:
    ```bash
    # Windows PowerShell
    $env:ELEVENLABS_API_KEY="your_api_key"
    ```
2.  **List available assets in the catalog:**
    ```bash
    python scripts/generate_audio.py list
    ```
3.  **Generate a music track:**
    ```bash
    python scripts/generate_audio.py music music_w1_homestead_explore
    ```
4.  **Generate a sound effect (SFX):**
    ```bash
    python scripts/generate_audio.py sfx ui_confirm
    ```
5.  **Generate a voiceover line:**
    ```bash
    python scripts/generate_audio.py voice shop_mechanic_vo
    ```

The generated files are saved directly into your Android [raw resources folder](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/res/raw/) as `.mp3` files, ready for the build.

### Step 2: Post-Processing & Envelope Trimming
Because AI generators add random silences or pads to the start and end of tracks:
1.  **Trim Silence:** Load the generated file into an audio editor (like Audacity) and cut any empty space at the beginning to ensure the track loops instantly.
2.  **Export Format:** Save the file as `.ogg` or `.mp3` at `44.1kHz, 16-bit` (recommended for mobile performance).
3.  **Place in Resources:** Ensure the final, processed files live in the [raw resources folder](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/res/raw/).


### Step 3: Engine Asset Registration
Add your assets to the game's data catalogs.
1.  Open [audio_catalog.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/audio_catalog.json) and register the track. Define its volume `gain`, `loop` status, and crossfade duration:
    ```json
    {
      "id": "music_w1_homestead_explore",
      "type": "music",
      "loop": true,
      "fade_in_ms": 1200,
      "fade_out_ms": 1000,
      "gain": 0.85
    }
    ```
2.  Open [audio_bindings.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/audio_bindings.json) and bind the track to the homestead hub:
    ```json
    "music": {
      "hub_1_homestead": "music_w1_homestead_explore"
    }
    ```

### Step 4: Real-time Debugging & Verification
1.  Run the application locally.
2.  Verify the transition from Homestead Quarter to combat:
    *   Does the acoustic guitar fade out smoothly?
    *   Does the aggressive combat rock kick in without popping?
3.  Check the debug console. The `AudioCuePlayer` is wired to print logs like:
    `AudioCuePlayer: Missing audio cue resource for '...'` if a file is improperly named.
