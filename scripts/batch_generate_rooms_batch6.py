#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 6 (Cooling Springs & The Forge).
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

BATCH_6_ROOMS = [
    {
        "id": "foundry_lava_tube_bypass",
        "output_subpath": "world_4/foundry_lava_tube_bypass",
        "prompt": (
            "A first-person view into a low industrial crawlspace passage cutting beneath the volcanic cooling springs of the Foundry. "
            "Large rotating industrial mineral fan blades in circular metal housings beat swirling clouds of hot white steam. "
            "Heavy copper pipes and valve wheels line rough volcanic basalt walls under soft amber emergency lighting. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, atmospheric steam and warm copper tones, "
            "with a clean paved stone path in the lower foreground. "
            "Empty room stage, no people, no workers, no hands, no readable tags, no letters, no logos."
        )
    },
    {
        "id": "foundry_cooling_springs_scale_01",
        "output_subpath": "world_4/foundry_cooling_springs_scale_01",
        "prompt": (
            "A first-person view of a tiered geothermal mist basin inside the Foundry cooling complex. "
            "Terraced pools of steaming turquoise coolant mineral water edged by thick crystalline mineral salt crusts and copper drainage pipes. "
            "Warm amber lantern light and cyan steam reflect across wet stone and riveted iron framing. "
            "Bold comic-cel sci-fi adventure environment, thick dark outlines, vibrant teal and orange palette, "
            "with clear stone walkway in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no tools on the ground, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_cooling_springs_scale_02",
        "output_subpath": "world_4/foundry_cooling_springs_scale_02",
        "prompt": (
            "A first-person view of a solemn makeshift memorial marker along the volcanic steam pipes of the Foundry. "
            "A heavy welded iron bracket mounted to insulated pipe casings holding a spent unlit mining helmet lamp as a humble shrine. "
            "Rough obsidian rock walls, dripping condensate, and glowing cyan mineral water in the background. "
            "Stylized anime-comic sci-fi adventure art, thick dark outlines, poignant blue-collar atmosphere, "
            "with a clean dark basalt floor in the foreground. "
            "Empty room stage, no people, no characters, no readable scratched names, no letters, no logos."
        )
    },
    {
        "id": "foundry_cooling_springs_scale_03",
        "output_subpath": "world_4/foundry_cooling_springs_scale_03",
        "prompt": (
            "A first-person view facing an intricate nest of heavy industrial coolant valves in the Foundry. "
            "A complex junction of massive criss-crossing copper and cast-iron conduits centered around a giant forged pressure wheel. "
            "Pressurized jets of white steam hiss from safety vents under warm yellow work lamps and glowing cyan water runoff channels. "
            "Chunky anime-comic sci-fi style, thick dark outlines, bold mechanical silhouettes, "
            "and clean stone deck plating in the lower third. "
            "Empty room stage, no people, no characters, no hands on the wheel, no readable gauge numbers, no letters, no logos."
        )
    },
    {
        "id": "foundry_cooling_springs_scale_04",
        "output_subpath": "world_4/foundry_cooling_springs_scale_04",
        "prompt": (
            "A first-person view along a secluded natural thermal rock shelf beneath a towering slag retention wall in the Foundry. "
            "A cool mineral spring seam trickles clear azure water across dark porous basalt rock shelves. "
            "Overhead, massive blackened industrial retaining bulkheads radiate dull orange heat, creating a stark contrast with the cool blue basin. "
            "Bold comic-cel sci-fi adventure look, thick dark outlines, dynamic heat versus cooling color contrast, "
            "with an open rocky terrace in the foreground. "
            "Empty room stage, no people, no characters, no loose items, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_cooling_springs_scale_final",
        "output_subpath": "world_4/foundry_cooling_springs_scale_final",
        "prompt": (
            "A first-person view inside a cavernous hidden steam pocket cavern beneath the Foundry. "
            "Patched industrial pipelines and bypass valves vent swirling luminescent cyan and cobalt steam into a natural volcanic hollow. "
            "Crystalline mineral formations glow with soft cold resonance against dark jagged basalt stone. "
            "Chunky anime-comic sci-fi environment, thick painted outlines, surreal ethereal lighting, "
            "with spacious clear stone flooring across the foreground. "
            "Empty room stage, no people, no characters, no readable stencils, no letters, no logos."
        )
    },
    {
        "id": "foundry_forge_control_alcove",
        "output_subpath": "world_4/foundry_forge_control_alcove",
        "prompt": (
            "A first-person view inside a heavily blast-shielded control alcove overlooking the central Forge in the Foundry. "
            "Armored reinforced viewport frame looking onto molten lava streams and drop-hammers. "
            "A rugged operator station with scorched heavy alloy levers, blank amber CRT status screens, and chunky toggle buttons. "
            "Bold anime-comic sci-fi adventure aesthetic, thick dark outlines, industrial wear and heat scorching, "
            "with clear diamond-plate deck in the lower half of the frame. "
            "Empty room stage, no people, no operators, no hands, no readable text on screens, no letters, no logos."
        )
    },
    {
        "id": "foundry_forge_scale_01",
        "output_subpath": "world_4/foundry_forge_scale_01",
        "prompt": (
            "A first-person view ascending an industrial bellows catwalk beside gigantic forced-air draft pistons powering the Forge. "
            "Colossal mechanical brass air cylinders and articulated iron bellows arms expand and contract against towering volcanic walls. "
            "Blinding incandescent orange forge fire shines from below, casting dramatic upward light through steel mesh walkways. "
            "Stylized comic-cel sci-fi art, thick dark outlines, immense mechanical energy and scale, "
            "with a clean grated metal pathway in the foreground. "
            "Empty room stage, no people, no workers, no loose tools, no readable pressure markings, no letters, no logos."
        )
    },
    {
        "id": "foundry_forge_scale_02",
        "output_subpath": "world_4/foundry_forge_scale_02",
        "prompt": (
            "A first-person view standing in the shadowed perimeter surrounding the colossal Anvil in the Forge. "
            "Massive forged titanium support pillars cast long ominous silhouettes across a blackened stone floor dusted with grey ash. "
            "In the center midground, an ancient engraved resonance plinth glows with faint cyan acoustic patterns beneath the ash. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, atmospheric forge ash and glowing embers, "
            "with clear floor space in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no loose weapons, no readable runes with letters, no logos."
        )
    },
    {
        "id": "foundry_forge_scale_03",
        "output_subpath": "world_4/foundry_forge_scale_03",
        "prompt": (
            "A first-person view inside a vast mold casting library adjoining the Forge. "
            "Towering heavy steel shelving units stacked with massive modular casting dies, geometric armor molds, and split casting matrices. "
            "Overhead electric crane rails and amber sodium lamps illuminate cracked dark foundry floor tiles. "
            "Bold comic-cel sci-fi environment, thick dark outlines, heavy geometric silhouettes, "
            "and uncluttered floor in the foreground. "
            "Empty room stage, no people, no workers, no hands, no readable labels on molds, no letters, no logos."
        )
    },
    {
        "id": "foundry_forge_scale_04",
        "output_subpath": "world_4/foundry_forge_scale_04",
        "prompt": (
            "A first-person view entering the Slag Chapel, a solemn blue-collar sanctuary built from welded structural steel inside the Forge. "
            "Architectural arches constructed from salvaged industrial I-beams, oversized prayer bolts, and geometric brass plates. "
            "A central modest altar plinth illuminated by glowing orange forge vents and votive amber industrial lamps. "
            "Stylized anime-comic sci-fi adventure style, thick inked outlines, industrial reverent atmosphere, "
            "with a clean steel plate aisle in the lower foreground. "
            "Empty room stage, no people, no characters, no readable etched names, no letters, no logos."
        )
    },
    {
        "id": "foundry_forge_scale_final",
        "output_subpath": "world_4/foundry_forge_scale_final",
        "prompt": (
            "A first-person view of an ancient acoustic draft tunnel beneath the foundation of the Forge. "
            "Monumental pre-Dominion cyclopean stone architecture fused with modern industrial heat baffles. "
            "Swirling atmospheric drafts pull delicate ribbons of incandescent orange ash toward a carved acoustic resonance arch in the distance. "
            "Chunky anime-comic sci-fi art, thick dark outlines, mysterious deep-time atmosphere, "
            "with clear dark flagstone flooring in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no readable symbols, no letters, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_6_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_6_ROOMS)}] Generating {room_id}...")
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

    print("\nBatch 6 completed successfully!")

if __name__ == "__main__":
    main()
