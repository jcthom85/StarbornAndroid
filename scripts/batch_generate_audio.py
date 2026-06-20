#!/usr/bin/env python3
import subprocess
import time

TRACKS = [
    "music_w3_combat",
    "music_w4_foundry_explore",
    "music_w4_combat",
    "music_w5_void_explore",
    "music_w5_combat",
    "music_w6_source_explore",
    "music_w6_combat"
]

def main():
    print(f"Starting batch audio generation for {len(TRACKS)} tracks...")
    for idx, track_id in enumerate(TRACKS):
        print(f"Generating track {idx + 1}/{len(TRACKS)}: {track_id}...")
        try:
            subprocess.run([
                "python", "scripts/generate_audio.py", "music", track_id
            ], check=True)
            print(f"Successfully generated {track_id}")
        except Exception as e:
            print(f"Error generating {track_id}: {e}")
        
        # Rate limit safety delay
        time.sleep(2)
    print("Batch audio generation completed.")

if __name__ == "__main__":
    main()
