# BurgQuest Booth Assets & Video Reel Guide

This directory contains the ready-to-use audio and video assets for the **Starborn** vendor table at **BurgQuest** (https://burg.quest).

---

## 1. Table Ambient Music Playlist (`burgquest_booth_assets/audio/`)

A tight, 7-track "Best of Starborn" selection (~20-minute cycle). This cut removes the atmospheric swamp/ambient cues (W2) and heavy metallic industrial clash (W4), focusing purely on Starborn's signature identity: **Space-western acoustic fingerpicking, blues guitar grooves, cozy warmth, and punchy rock combat**.

### Tracklist:
1. `01_music_title_theme.mp3` – **Title Theme** *(Acoustic guitar melody & orchestral warmth)*
2. `02_music_w1_homestead_explore.mp3` – **Frontier Homestead** *(Signature space-blues acoustic groove)*
3. `03_music_shop_cozy.mp3` – **General Store** *(Warm, cozy, melodic downtime)*
4. `04_music_w1_combat.mp3` – **Skirmish** *(Driving rock beat showing off the game's battle energy)*
5. `05_music_astra_common_room.mp3` – **Astra Common Room** *(Relaxed ship-crew vibe)*
6. `06_music_elaras_song.mp3` – **Elara's Song** *(Memorable character acoustic motif)*
7. `07_music_credits_ending.mp3` – **Starborn Anthem** *(Triumphant melodic crescendo before looping)*

### How to Play at the Table:
- Open [`burgquest_booth_assets/audio/starborn_burgquest_playlist.m3u`](file:///C:/Users/jcthomas/StudioProjects/StarbornAndroid/burgquest_booth_assets/audio/starborn_burgquest_playlist.m3u) in VLC, iTunes, Windows Media Player, or transfer the `audio/` folder to a phone/tablet connected to a portable speaker.
- Set to **Repeat / Loop All** at a comfortable conversational background volume.

---

## 2. Looping Video Reel (`burgquest_booth_assets/`)

### The 4 Scenes:
1. **Scene 1: Title Screen Loop (`scene1_title.mp4`)**
   - **How to Capture:** Launch the game to the Main Title screen. Record 12–15 seconds of the logo fade-in, glowing particles, and "Press Any Button" prompt.
2. **Scene 2: Intro Cinematic Cold Open (`scene2_cold_open.mp4`)**
   - **How to Capture:** Press "New Game" to trigger the Sector 9 containment breach animatic (`intro_prologue`). Record 20–25 seconds through the siren alert and Chime tube launch.
3. **Scene 3: Exploration, Room Transition & Menu (`scene3_exploration.mp4`)**
   - **How to Capture:** Launch via **Debug Scenarios** -> `"BURGFEST: 1. Story Opening"` or `"BURGFEST: 3. The Astra Flagship"`. Walk between two rooms to show camera pan/transition, tap `MENU` to show Inventory/Tinkering/Stats, and view the Hub node map.
4. **Scene 4: 4-Character Tactical Combat (`scene4_combat.mp4`)**
   - **How to Capture:** Launch via **Debug Scenarios** -> `"BURGFEST: 2. Tactical Combat (Canopy Skirmish)"` or `"W4 / Meltdown Escape"`. Execute 2–3 big attacks with Nova, Zeke, Orion, and Gh0st, triggering combos, guard breaks, and flashy special FX.

### Automatic Assembly:
Place the 4 recorded `.mp4` files into `burgquest_booth_assets/raw_captures/`:
- `scene1_title.mp4`
- `scene2_cold_open.mp4`
- `scene3_exploration.mp4`
- `scene4_combat.mp4`

Then run:
```powershell
python scripts/assemble_burgquest_reel.py
```
This will automatically crop/scale to 1080p 60fps, align audio, and output:
`burgquest_booth_assets/starborn_burgquest_loop.mp4`
