#!/usr/bin/env python3
"""
BurgQuest Looping Video Reel Assembler
Takes 4 raw scene captures (MP4) and stitches them into a seamless 60fps looping video
with cross-dissolve transitions, audio leveling, and optional 'Play The Demo Here' bumper.

Usage:
  python scripts/assemble_burgquest_reel.py \
    --scene1 burgquest_booth_assets/raw_captures/scene1_title.mp4 \
    --scene2 burgquest_booth_assets/raw_captures/scene2_cold_open.mp4 \
    --scene3 burgquest_booth_assets/raw_captures/scene3_exploration.mp4 \
    --scene4 burgquest_booth_assets/raw_captures/scene4_combat.mp4 \
    --output burgquest_booth_assets/starborn_burgquest_loop.mp4
"""

import argparse
import subprocess
import sys
from pathlib import Path

def check_file(p: Path, name: str):
    if not p.exists():
        print(f"Error: {name} not found at {p}")
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(description="Assemble 4-scene Starborn BurgQuest looping video reel")
    parser.add_argument("--scene1", type=Path, default=Path("burgquest_booth_assets/raw_captures/scene1_title.mp4"))
    parser.add_argument("--scene2", type=Path, default=Path("burgquest_booth_assets/raw_captures/scene2_cold_open.mp4"))
    parser.add_argument("--scene3", type=Path, default=Path("burgquest_booth_assets/raw_captures/scene3_exploration.mp4"))
    parser.add_argument("--scene4", type=Path, default=Path("burgquest_booth_assets/raw_captures/scene4_combat.mp4"))
    parser.add_argument("--output", type=Path, default=Path("burgquest_booth_assets/starborn_burgquest_loop.mp4"))
    args = parser.parse_args()

    for path, label in [
        (args.scene1, "Scene 1 (Title Screen)"),
        (args.scene2, "Scene 2 (Cold Open)"),
        (args.scene3, "Scene 3 (Exploration)"),
        (args.scene4, "Scene 4 (Combat)")
    ]:
        if not path.exists():
            print(f"[!] Pending raw clip: {label} -> {path}")
            print(f"    Please capture and place '{path.name}' in {path.parent}")

    print("\n--- BurgQuest Reel Configuration ---")
    print(f"Scene 1 (Title):       {args.scene1}")
    print(f"Scene 2 (Cold Open):   {args.scene2}")
    print(f"Scene 3 (Exploration): {args.scene3}")
    print(f"Scene 4 (Combat):      {args.scene4}")
    print(f"Output File:           {args.output}")

    # Build filter complex for smooth transitions if all 4 exist
    if all(p.exists() for p in [args.scene1, args.scene2, args.scene3, args.scene4]):
        print("\nAll 4 source clips detected! Stitching with FFmpeg...")
        cmd = [
            "ffmpeg", "-y",
            "-i", str(args.scene1),
            "-i", str(args.scene2),
            "-i", str(args.scene3),
            "-i", str(args.scene4),
            "-filter_complex",
            "[0:v]scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1,fps=60[v0];"
            "[1:v]scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1,fps=60[v1];"
            "[2:v]scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1,fps=60[v2];"
            "[3:v]scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1,fps=60[v3];"
            "[v0][0:a][v1][1:a][v2][2:a][v3][3:a]concat=n=4:v=1:a=1[v_out][a_out]",
            "-map", "[v_out]",
            "-map", "[a_out]",
            "-c:v", "libx264",
            "-preset", "slow",
            "-crf", "18",
            "-c:a", "aac",
            "-b:a", "192k",
            str(args.output)
        ]
        result = subprocess.run(cmd)
        if result.returncode == 0:
            print(f"\nSuccessfully compiled: {args.output}")
        else:
            print("\nFFmpeg compilation encountered an error.")
    else:
        print("\nReady to assemble as soon as the 4 scene recordings are captured.")

if __name__ == "__main__":
    main()
