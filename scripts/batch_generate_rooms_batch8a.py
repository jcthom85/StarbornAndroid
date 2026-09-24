#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 8A (Slag Landing & Slag River).
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

BATCH_8A_ROOMS = [
    {
        "id": "foundry_obsidian_overlook",
        "output_subpath": "world_4/foundry_obsidian_overlook",
        "prompt": (
            "A first-person view standing at the cracked edge of a natural obsidian rock overlook above the glowing Foundry caldera basin. "
            "Below the rocky precipice, sprawling industrial slag channels, smoking refinery towers, and massive intake piston towers stretch to the horizon. "
            "Charred volcanic basalt with glowing orange cracks and cyan resonance veins in the rock. "
            "Bold anime-comic sci-fi adventure environment, thick dark outlines, dramatic panoramic depth, "
            "with a wide clear dark rock terrace in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no readable telemetry on screens, no letters, no logos."
        )
    },
    {
        "id": "foundry_obsidian_shelf_scale_01",
        "output_subpath": "world_4/foundry_obsidian_shelf_scale_01",
        "prompt": (
            "A first-person view of a high volcanic ash survey point on an obsidian rock shelf in the Foundry. "
            "A rugged metal surveyor tripod with blank optical lenses overlooks dark fields of glassy black volcanic sand and lava streams. "
            "Heavy industrial hazard warning pylons with unlit beacon lamps line the perimeter. "
            "Stylized comic-cel sci-fi art, thick dark outlines, volcanic charcoal and amber palette, "
            "with a clean obsidian stone platform in the lower foreground. "
            "Empty room stage, no people, no surveyors, no hands, no readable survey notes, no letters, no logos."
        )
    },
    {
        "id": "foundry_obsidian_shelf_scale_02",
        "output_subpath": "world_4/foundry_obsidian_shelf_scale_02",
        "prompt": (
            "A first-person view inside a shadowed thermal shelter alcove cut into a towering basalt slag cliff. "
            "Heavy hanging metallic heat-shield curtains and stacked ceramic coolant canisters buffer against blistering furnace winds. "
            "Deep orange glow from distant molten channels filters into the cool shadowed rocky nook. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, moody contrast, "
            "and clean stone floor space in the foreground. "
            "Empty room stage, no people, no characters, no readable tags or letters, no logos."
        )
    },
    {
        "id": "foundry_obsidian_shelf_scale_03",
        "output_subpath": "world_4/foundry_obsidian_shelf_scale_03",
        "prompt": (
            "A first-person view facing an improvised industrial signal relay post perched on the edge of the obsidian shelf. "
            "A tall radio mast welded from heavy railway iron and copper antenna coils leans out over the volcanic valley. "
            "Flashing amber navigation beacons cast rhythmic pulses across the jagged black basalt deck. "
            "Bold comic-cel sci-fi adventure look, thick dark outlines, atmospheric smoky sky, "
            "with clear open rock paving in the lower third for mobile UI. "
            "Empty room stage, no people, no characters, no readable markings, no letters, no logos."
        )
    },
    {
        "id": "foundry_obsidian_shelf_scale_04",
        "output_subpath": "world_4/foundry_obsidian_shelf_scale_04",
        "prompt": (
            "A first-person view into a reinforced volcanic survey cache nook in the Foundry. "
            "Heavy watertight industrial supply lockboxes bolted to the natural rock wall beneath an overhang. "
            "A dormant optical scope mounted on a stone pedestal overlooks the volcanic heat haze. "
            "Stylized anime-comic sci-fi adventure environment, thick dark outlines, rugged field expedition feel, "
            "and uncluttered basalt flooring in the lower half. "
            "Empty room stage, no people, no characters, no loose items on the ground, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_obsidian_shelf_scale_final",
        "output_subpath": "world_4/foundry_obsidian_shelf_scale_final",
        "prompt": (
            "A first-person view of an emergency navigation beacon post installed in the black glass flats of the Foundry. "
            "A heavy reinforced alloy beacon pylon with a cracked, weather-beaten orange warning prism glowing continuously. "
            "Surrounding volcanic stone and dark glassy obsidian ground reflect the eerie rhythmic light under churning smoke clouds. "
            "Chunky anime-comic sci-fi art, thick dark outlines, striking solitary landmark silhouette, "
            "with a clean dark floor platform across the foreground. "
            "Empty room stage, no people, no characters, no readable tallies or numbers, no letters, no logos."
        )
    },
    {
        "id": "foundry_slag_stepping_stones",
        "output_subpath": "world_4/foundry_slag_stepping_stones",
        "prompt": (
            "A first-person view looking across a treacherous path of natural hexagonal basalt stepping stone pillars across a molten lava river. "
            "Glowing rivers of molten orange slag and blue-white acoustic energy currents surge between the stone pedestals. "
            "Massive industrial retaining canal bulkheads with safety stanchions flank the riverbanks in the distance. "
            "Bold comic-cel sci-fi adventure aesthetic, thick dark outlines, vibrant fiery contrast, "
            "with a solid hexagonal basalt platform in the immediate foreground. "
            "Empty room stage, no people, no characters leaping, no loose items, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_slag_river_scale_01",
        "output_subpath": "world_4/foundry_slag_river_scale_01",
        "prompt": (
            "A first-person view along a zigzagging industrial basalt switchback trail hugging the vertical canyon wall of the slag river. "
            "Riveted steel heat shields and heavy alloy safety railings protect a descending stone path beside glowing molten falls. "
            "A vertical emergency ladder frame glows red-hot at the edges against the dark volcanic cliff. "
            "Chunky anime-comic sci-fi style, thick dark outlines, dynamic vertical canyon perspective, "
            "with clean switchback stone walkway in the foreground for mobile UI. "
            "Empty room stage, no people, no workers, no hands, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "foundry_slag_river_scale_02",
        "output_subpath": "world_4/foundry_slag_river_scale_02",
        "prompt": (
            "A first-person view of an industrial coolant spillway discharging into the molten slag river in the Foundry. "
            "Huge square conduits pour torrents of glowing chemical cyan coolant into churning orange lava, generating thick swirling curtains of white steam. "
            "An elevated steel observation gangway with yellow safety curbs spans across the misty basin. "
            "Stylized comic-cel sci-fi adventure look, thick dark outlines, dramatic cyan and orange color contrast, "
            "with clear gangway decking in the lower third. "
            "Empty room stage, no people, no characters, no figures in steam, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_slag_river_scale_03",
        "output_subpath": "world_4/foundry_slag_river_scale_03",
        "prompt": (
            "A first-person view facing an industrial slag ferry crossing terminal on the banks of the lava river. "
            "A heavy steel cable winch drum with stripped mechanical gear teeth, counterweights, and rusted mooring tethers anchored to the stone quay. "
            "Fiery molten slag reflects blinding orange highlights against dark iron machinery and volcanic stone. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, heavy mechanical silhouettes, "
            "and a clear stone landing quay in the lower foreground. "
            "Empty room stage, no people, no ferry boat, no hands, no readable stencils, no letters, no logos."
        )
    },
    {
        "id": "foundry_slag_river_scale_04",
        "output_subpath": "world_4/foundry_slag_river_scale_04",
        "prompt": (
            "A first-person view inside a rugged river flow gauge observation station overlooking the slag channel. "
            "A cantilevered steel control pulpit with heavy armored blast glass and oversized hydraulic dial gauges without numbers. "
            "Fiery lava surges splash against reinforced deflection pillars outside in the fiery canyon. "
            "Bold comic-cel sci-fi environment, thick dark outlines, tense industrial atmosphere, "
            "with clean metal floor plating across the lower foreground. "
            "Empty room stage, no people, no operators, no hands, no readable text on gauges, no letters, no logos."
        )
    },
    {
        "id": "foundry_slag_river_scale_final",
        "output_subpath": "world_4/foundry_slag_river_scale_final",
        "prompt": (
            "A first-person view inside a weather-beaten slag river control booth perched on a volcanic outcropping. "
            "Scorched metal consoles, heavy manual gate wheels, and an oversized industrial emergency rescue hook hanging on the wall. "
            "Wide panoramic viewports look down upon converging lava channels glowing with intense amber light. "
            "Stylized anime-comic sci-fi adventure art, thick dark outlines, blue-collar survival grit, "
            "and clear dark floor plating in the foreground for mobile UI. "
            "Empty room stage, no people, no workers, no hands, no readable panel text, no letters, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_8A_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_8A_ROOMS)}] Generating {room_id}...")
        try:
            response = client.images.generate(
                model="gpt-image-2",
                prompt=item["prompt"],
                n=1,
                size="1088x1920",
                quality="low"
            )
            
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
                
            im = Image.open(png_path)
            im.save(webp_path, "WEBP", quality=90, method=6)
            print(f"  -> Generated PNG: {png_path.name}")
            print(f"  -> Encoded WebP: {webp_path.name} ({webp_path.stat().st_size // 1024} KB)")
            
        except Exception as e:
            print(f"Error generating {room_id}: {e}", file=sys.stderr)
            sys.exit(1)
            
        time.sleep(2)

    print("\nBatch 8A completed successfully!")

if __name__ == "__main__":
    main()
