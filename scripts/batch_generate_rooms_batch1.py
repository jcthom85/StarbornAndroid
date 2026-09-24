#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds.
Follows docs/story/Starborn_Art_Production_Guide.md:
- Model: gpt-image-2
- Quality: low
- Size: 1088x1920
- Format: Save source PNG, export shipping WebP (quality=90, method=6)
- Constraints: Ghost town rule, no hands/characters, no readable text/logos.
"""

import sys
import time
from pathlib import Path
from PIL import Image
from openai import OpenAI

PROJECT_ROOT = Path(__file__).resolve().parent.parent

def get_api_key() -> str:
    key_file = PROJECT_ROOT / "openai_api_key.txt"
    if key_file.exists():
        return key_file.read_text(encoding="utf-8").strip()
    import os
    if os.environ.get("OPENAI_API_KEY"):
        return os.environ["OPENAI_API_KEY"]
    raise RuntimeError("OpenAI API key not found in openai_api_key.txt or OPENAI_API_KEY env var.")

BATCH_1_ROOMS = [
    {
        "id": "spire_drone_test_alcove",
        "output_subpath": "world_3/spire_drone_test_alcove",
        "prompt": (
            "A first-person view entering an industrial drone test alcove behind reinforced scratched security glass inside the high-tech Spire. "
            "Heavy weapon racks and recharging cradles flank the dark metal walls, surrounded by bold yellow-and-black hazard painted warning stripes. "
            "In the center midground stands a clean calibration test bay with overhead spot illumination and dormant drone docking clamps. "
            "Chunky anime-comic sci-fi architecture, thick dark outlines, bold metallic geometry, clean reflections on the steel deck plate, "
            "and calm negative space in the lower foreground. "
            "Empty room stage, no people, no characters, no hands, no active robots, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_archive_scale_01",
        "output_subpath": "world_3/spire_archive_scale_01",
        "prompt": (
            "A first-person view entering a towering high-tech catalog atrium inside the Archive wing of the Spire. "
            "Massive multi-story crystalline data filing columns and brass-trimmed server pillars stretch upward into a vaulted ceiling. "
            "Soft ambient amber readouts and cyan resonance light wash across polished dark terrazzo floor tiles. "
            "In the midground stands a blank curved index console plinth beneath geometric brass arches. "
            "Chunky anime-comic cel-shaded sci-fi environment, thick dark outlines, clear silhouette layers, "
            "and uncluttered foreground negative space. "
            "Empty room stage, no people, no characters, no readable writing, no letters, no logos."
        )
    },
    {
        "id": "spire_archive_scale_02",
        "output_subpath": "world_3/spire_archive_scale_02",
        "prompt": (
            "A first-person view into an optical prism maintenance service bay adjoining the Spire Archive. "
            "Clean technician workbenches with precision brass clamps, modular component shelves, and circular optical calibration rings line the side walls. "
            "Overhead industrial daylight lamps mix with teal and amber status lights across clean metallic flooring. "
            "In the center midground, an empty optical alignment pedestal stands ready for lens fitting. "
            "Bold anime-comic sci-fi adventure aesthetic, chunky mechanical geometry, thick painted outlines, "
            "and calm lower-third foreground for mobile UI. "
            "Empty room stage, no people, no characters, no hands, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_archive_scale_03",
        "output_subpath": "world_3/spire_archive_scale_03",
        "prompt": (
            "A first-person view looking down an ominous narrow security corridor known as the Alarm Spine, located behind the Archive vault in the Spire. "
            "Clusters of thick glowing crimson and amber conduit pipes run along the dark gunmetal ceiling and walls, pulsing with bright warning light. "
            "Angular reinforced security bulkheads frame the perspective toward a locked heavy blast door in the distance. "
            "Cel-shaded comic sci-fi adventure environment, thick dark outlines, high-contrast red and slate-grey color blocks, "
            "and clear unobstructed dark flooring in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no warning letters, no logos."
        )
    },
    {
        "id": "spire_exec_lounge_scale_01",
        "output_subpath": "world_3/spire_exec_lounge_scale_01",
        "prompt": (
            "A first-person view into the opulent Velvet Hall of the Upper Spire Executive Lounge. "
            "Plush acoustic sound-dampening wall panels upholstered in deep crimson and plum fabric line the corridor, interspersed with vertical brass light sconces. "
            "Polished obsidian stone floor with subtle warm reflections leads toward an elegant curved lounge entryway in the midground. "
            "Stylized chibi-anime comic sci-fi adventure style, clean luxurious forms, thick dark outlines, rich crimson and brass palette, "
            "with spacious calm negative space across the floor plane. "
            "Empty room stage, no people, no characters, no signs, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_exec_lounge_scale_02",
        "output_subpath": "world_3/spire_exec_lounge_scale_02",
        "prompt": (
            "A first-person view looking into an exclusive private executive booth overlooking the city lights outside the Spire. "
            "A curved luxury burgundy leather banquette wraps around a low dark lacquered table. "
            "To the right, a floor-to-ceiling panoramic observation window reveals glowing golden skyscraper spires through evening mist. "
            "Recessed amber mood lighting softly illuminates decorative brass ceiling insets and dark carpet. "
            "Bold anime-comic sci-fi adventure art, thick dark outlines, chunky furniture silhouettes, "
            "and clean foreground for mobile UI. "
            "Empty room stage, no people, no characters, no drinks, no readable text, no screens with letters, no logos."
        )
    },
    {
        "id": "spire_exec_lounge_scale_03",
        "output_subpath": "world_3/spire_exec_lounge_scale_03",
        "prompt": (
            "A first-person view stepping out onto a secluded outdoor balcony high up in the Spire, known as the Blackmail Balcony. "
            "An ornate brass and dark alloy railing borders the terrace, looking out into the sprawling vertical metropolis illuminated by teal and amber towers. "
            "Intricately carved acoustic wall reliefs and discreet brass ventilation grilles flank the seating area. "
            "Rich comic-cel sci-fi environment, thick dark outlines, atmospheric evening skyline, clean paved stone terrace in the lower foreground. "
            "Empty room stage, no people, no characters, no cameras visible as characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_exec_lounge_scale_04",
        "output_subpath": "world_3/spire_exec_lounge_scale_04",
        "prompt": (
            "A first-person view inside a concealed high-security ledger office tucked behind the executive lounge in the Spire. "
            "Heavy reinforced safe deposit compartments and dark wood-paneled acoustic walls surround a sleek minimalist executive desk. "
            "Subtle cyan holographic terminal glow and warm under-desk lighting illuminate the polished dark floor. "
            "Chunky anime-comic sci-fi environment, bold dark outlines, clean geometric shapes, rich palette of mahogany, brass, and cyan, "
            "with calm negative space in the lower foreground. "
            "Empty room stage, no people, no characters, no paper clutter, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_exec_lounge_scale_05",
        "output_subpath": "world_3/spire_exec_lounge_scale_05",
        "prompt": (
            "A first-person view of a service dumbwaiter and utility corridor behind the executive lounge in the Spire. "
            "A heavy vertical brass and steel service elevator hatch with counterweight cables is set into the metallic back wall. "
            "Narrow stainless steel preparation counters and utility conduits run along the corridor under soft yellow industrial utility lighting. "
            "Clean comic-cel sci-fi adventure style, chunky machinery, thick painted outlines, and clear dark floor plating in the foreground. "
            "Empty room stage, no people, no characters, no hands, no readable text, no numbers, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_1_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_1_ROOMS)}] Generating {room_id}...")
        try:
            response = client.images.generate(
                model="gpt-image-2",
                prompt=item["prompt"],
                n=1,
                size="1088x1920",
                quality="low"
            )
            
            # Decode image
            if response.data[0].b64_json:
                import base64
                img_bytes = base64.b64decode(response.data[0].b64_json)
                png_path.write_bytes(img_bytes)
            elif response.data[0].url:
                import urllib.request
                req = urllib.request.Request(response.data[0].url, headers={'User-Agent': 'Mozilla/5.0'})
                with urllib.request.urlopen(req) as resp:
                    png_path.write_bytes(resp.read())
            else:
                raise ValueError("No image data returned from OpenAI API.")
                
            # Convert to WebP (quality=90, method=6)
            im = Image.open(png_path)
            im.save(webp_path, "WEBP", quality=90, method=6)
            print(f"  -> Generated PNG: {png_path.name}")
            print(f"  -> Encoded WebP: {webp_path.name} ({webp_path.stat().st_size // 1024} KB)")
            
        except Exception as e:
            print(f"Error generating {room_id}: {e}", file=sys.stderr)
            sys.exit(1)
            
        time.sleep(2)  # Brief pause between calls to respect rate limits

    print("\nBatch 1 completed successfully!")

if __name__ == "__main__":
    main()
