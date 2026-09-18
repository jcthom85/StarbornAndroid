#!/usr/bin/env python3
"""
Exports a tight, curated "Best of Starborn" playlist for BurgQuest.
Focuses strictly on the signature acoustic/blues space-western and character vibe,
removing dissonant/ambient-heavy swamp (W2) and abrasive industrial metallic (W4) tracks.
"""

import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES_RAW = ROOT / "app/src/main/res/raw"
DEST_DIR = ROOT / "burgquest_booth_assets/audio"
DEST_DIR.mkdir(parents=True, exist_ok=True)

# Curated "Best of Starborn" Core Vibe (5-6 peak tracks, ~20 mins total loop)
TIGHT_PLAYLIST = [
    ("01_music_title_theme.mp3", "music_title_theme.mp3", "Starborn - Title Theme (Acoustic / Orchestral)"),
    ("02_music_w1_homestead_explore.mp3", "music_w1_homestead_explore.mp3", "Starborn - Frontier Homestead (Space Blues)"),
    ("03_music_shop_cozy.mp3", "music_shop_cozy.mp3", "Starborn - General Store (Cozy Acoustic)"),
    ("04_music_w1_combat.mp3", "music_w1_combat.mp3", "Starborn - Skirmish (Frontier Rock Combat)"),
    ("05_music_astra_common_room.mp3", "music_astra_common_room.mp3", "Starborn - Astra Common Room (Crew Downtime)"),
    ("06_music_elaras_song.mp3", "music_elaras_song.mp3", "Starborn - Elara's Song (Acoustic Motif)"),
    ("07_music_credits_ending.mp3", "music_credits_ending.mp3", "Starborn - Starborn Anthem (Ending Credits)")
]

def main():
    m3u_lines = ["#EXTM3U\n"]
    print("Exporting Curated 'Best of Starborn' Table Playlist...")

    for dest_name, src_name, title in TIGHT_PLAYLIST:
        src_path = RES_RAW / src_name
        dest_path = DEST_DIR / dest_name
        if src_path.exists():
            shutil.copy2(src_path, dest_path)
            print(f"  [OK] Copied {src_name} -> {dest_name}")
            m3u_lines.append(f"#EXTINF:-1,{title}\n")
            m3u_lines.append(f"{dest_name}\n")
        else:
            print(f"  [WARN] Source file not found: {src_path}")

    playlist_path = DEST_DIR / "starborn_burgquest_playlist.m3u"
    playlist_path.write_text("".join(m3u_lines), encoding="utf-8")
    print(f"\nTight playlist generated: {playlist_path}")

if __name__ == "__main__":
    main()
