#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 8B (Titan Dock & Waste Intake - World 4 Final).
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

BATCH_8B_ROOMS = [
    {
        "id": "foundry_escape_catwalk",
        "output_subpath": "world_4/foundry_escape_catwalk",
        "prompt": (
            "A first-person view along a narrow vibrating steel catwalk spanning a fiery abyss between the Titan Dock and an escape gantry. "
            "Heavy industrial grated decking with intense incandescent orange heat rising through the steel floor mesh. "
            "Safety yellow railings, overhead steam conduits, and distant volcanic furnace walls bathed in amber and red emergency strobes. "
            "Bold anime-comic sci-fi adventure environment, thick dark outlines, thrilling high-tension perspective, "
            "with a clean catwalk path in the lower foreground. "
            "Empty room stage, no people, no characters running, no loose tools, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_titan_dock_scale_01",
        "output_subpath": "world_4/foundry_titan_dock_scale_01",
        "prompt": (
            "A first-person view of a massive multi-tiered walker gantry scaffold framing an empty mech bay in the Titan Dock. "
            "Heavy hydraulic robotic servicing arms, dangling umbilical power cables, and reinforced yellow access ladders. "
            "Fresh industrial weld smoke drifts through bright overhead halogen floodlights and amber warning beacons. "
            "Stylized comic-cel sci-fi art, thick dark outlines, monumental robotic engineering scale, "
            "with clear steel deck plating in the lower third for mobile UI. "
            "Empty room stage, no people, no pilots, no mechs, no active walkers, no readable stencils, no letters, no logos."
        )
    },
    {
        "id": "foundry_titan_dock_scale_02",
        "output_subpath": "world_4/foundry_titan_dock_scale_02",
        "prompt": (
            "A first-person view facing an armored heavy ammunition lift shaft beside the Titan Dock. "
            "Massive industrial chain hoists, pneumatic locking clamps, and heavy vertical guide rails holding empty steel artillery cradle pallets. "
            "A friction brake drum glows cherry red behind wire safety mesh under harsh sodium-vapor lighting. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, heavy industrial machinery, "
            "and clean steel plate flooring in the foreground. "
            "Empty room stage, no people, no workers, no loose ammo shells, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_titan_dock_scale_03",
        "output_subpath": "world_4/foundry_titan_dock_scale_03",
        "prompt": (
            "A first-person view along an elevated industrial monorail escape spur line extending from the Titan Dock. "
            "Heavy reinforced steel track bed leading out through an open blast portal into the volcanic night sky. "
            "Cut hydraulic lines and severed mechanical couplers hang safely to the sides under pulsing amber clearance lights. "
            "Bold comic-cel sci-fi adventure look, thick dark outlines, dramatic escape route framing, "
            "with a clear track platform in the lower foreground. "
            "Empty room stage, no people, no rail vehicles, no loose debris, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_titan_dock_scale_04",
        "output_subpath": "world_4/foundry_titan_dock_scale_04",
        "prompt": (
            "A first-person view from a high observation overlook witnessing an impending reactor thermal cascade across the Foundry core. "
            "Blinding white and cyan plasma fire crawls up towering cyclopean alloy bulkheads in the distance like electric lightning. "
            "Heavy blast shutters partially lower over reinforced observation windows, casting graphic barred shadows across the dark floor. "
            "Stylized anime-comic sci-fi art, thick dark outlines, apocalyptic sci-fi scale, "
            "with clean dark floor tiles in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no readable alarm screens, no letters, no logos."
        )
    },
    {
        "id": "foundry_titan_dock_scale_final",
        "output_subpath": "world_4/foundry_titan_dock_scale_final",
        "prompt": (
            "A first-person view standing at the final boarding gantry projecting directly out over the Titan Dock abyss. "
            "A cantilevered titanium gangway with heavy pneumatic release bolts, yellow-and-black hazard curbs, and red status beacons. "
            "Surrounding molten lava falls and industrial steam billow through the cavernous underground dome. "
            "Chunky anime-comic sci-fi aesthetic, thick inked outlines, heroic climactic atmosphere, "
            "with a wide clear boarding platform in the lower third. "
            "Empty room stage, no people, no ship, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_intake_overlook",
        "output_subpath": "world_4/foundry_intake_overlook",
        "prompt": (
            "A first-person view from an industrial grated balcony overlooking the colossal waste intake crushers of the Foundry. "
            "Below the pitted steel railing, gigantic pneumatic crusher pistons smash scrap machinery into raw feedstock amid showers of sparks. "
            "Heavy industrial exhaust hoods and orange molten metal sluices illuminate the cavernous factory interior. "
            "Bold comic-cel sci-fi adventure aesthetic, thick dark outlines, intense manufacturing drama, "
            "with clean steel floor grating across the lower foreground. "
            "Empty room stage, no people, no workers, no loose scrap on the walkway, no readable serial numbers, no letters, no logos."
        )
    },
    {
        "id": "foundry_waste_intake_scale_01",
        "output_subpath": "world_4/foundry_waste_intake_scale_01",
        "prompt": (
            "A first-person view peering directly into the massive jaws of the main scrap crusher throat in the Foundry. "
            "Gigantic toothed alloy crusher drums and hydraulic ram wedges set into blackened reinforced concrete walls. "
            "Deep red and amber heat glow radiates from the intake pit below under heavy yellow crane gantries. "
            "Chunky anime-comic sci-fi style, thick dark outlines, brutal mechanical forms, "
            "with a solid steel maintenance staging deck in the foreground. "
            "Empty room stage, no people, no characters, no jammed robots that look like living people, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_waste_intake_scale_02",
        "output_subpath": "world_4/foundry_waste_intake_scale_02",
        "prompt": (
            "A first-person view inside an industrial records shed and manifest office near the waste intake bay. "
            "A corrugated steel outpost hut with heavy wire-mesh security windows, empty metal shipping pallets, and blank barcode scanners. "
            "Dull green fluorescent lighting and amber outdoor floodlights filter through the industrial haze. "
            "Stylized comic-cel sci-fi adventure environment, thick dark outlines, clandestine salvage aesthetic, "
            "with clear concrete floor space in the lower foreground. "
            "Empty room stage, no people, no clerks, no loose papers, no readable crate labels, no letters, no logos."
        )
    },
    {
        "id": "foundry_waste_intake_scale_03",
        "output_subpath": "world_4/foundry_waste_intake_scale_03",
        "prompt": (
            "A first-person view facing a powerful magnetic separation ramp in the waste processing line. "
            "A steep inclined steel conveyor ramp flanked by massive electromagnetic coils glowing with turquoise magnetic field lines. "
            "Streams of fine metallic filings cling in crystalline patterns to the polished magnetic plates under yellow work lamps. "
            "Chunky anime-comic sci-fi look, thick dark outlines, striking electromagnetic energy effects, "
            "and clean deck plating in the lower third for mobile UI. "
            "Empty room stage, no people, no workers, no hands, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_waste_intake_scale_04",
        "output_subpath": "world_4/foundry_waste_intake_scale_04",
        "prompt": (
            "A first-person view along a high suspension catwalk swinging above the cavernous scrap intake pit in the Foundry. "
            "Steel cable trusses, diagonal sway braces, and safety mesh floors suspended high above churning scrap sorting drums. "
            "Atmospheric orange dust and rising heat shimmer catch overhead industrial spotlights. "
            "Bold comic-cel sci-fi adventure art, thick dark outlines, vertigo-inducing industrial height, "
            "with a clear catwalk path leading forward in the foreground. "
            "Empty room stage, no people, no workers, no loose tools, no readable signatures on beams, no letters, no logos."
        )
    },
    {
        "id": "foundry_waste_intake_scale_final",
        "output_subpath": "world_4/foundry_waste_intake_scale_final",
        "prompt": (
            "A first-person view of the master sorting spine conveyor corridor behind the Foundry intake facility. "
            "A continuous vertical bucket-elevator conveyor tower carrying heavy mechanical scrap upward into sorting cyclones. "
            "Heavy forged iron chains, drive gears, and yellow hazard cages pulse under rhythmic red and amber status lights. "
            "Stylized anime-comic sci-fi aesthetic, thick dark outlines, intricate industrial machinery, "
            "with clean dark floor plating across the lower foreground. "
            "Empty room stage, no people, no characters, no loose items on the floor, no readable text, no letters, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_8B_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_8B_ROOMS)}] Generating {room_id}...")
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

    print("\nBatch 8B completed successfully! World 4 is 100% complete!")

if __name__ == "__main__":
    main()
