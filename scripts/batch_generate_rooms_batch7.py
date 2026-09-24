#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 7 (Power Core & Service Airlock).
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

BATCH_7_ROOMS = [
    {
        "id": "foundry_engine_service_ring",
        "output_subpath": "world_4/foundry_engine_service_ring",
        "prompt": (
            "A first-person view along a curved mechanical service gantry closely encircling the colossal Deep-Core Engine in the Foundry. "
            "Massive thermal clamp assemblies, heavy ceramic heat-shielding tiles, and vibrating pneumatic conduits follow the curvature of the ring. "
            "Blinding incandescent golden-orange engine heat radiates from the central spindle, framed by cyan coolant manifolds. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, intense industrial energy, "
            "with clear curved floor grating in the foreground for mobile UI. "
            "Empty room stage, no people, no workers, no hands, no readable gauge text, no letters, no logos."
        )
    },
    {
        "id": "foundry_power_core_scale_01",
        "output_subpath": "world_4/foundry_power_core_scale_01",
        "prompt": (
            "A first-person view standing on an elevated turbine observation balcony overlooking the high-speed rotor core in the Foundry. "
            "Massive circular magnetic turbine housing spinning with intense motion blur behind reinforced safety stanchions. "
            "Amber tachometer dials without numbers, heavy vibration dampeners, and safety railings. "
            "Bold comic-cel sci-fi adventure environment, thick dark outlines, dynamic rotational machinery feel, "
            "with clean steel deck plating in the lower third. "
            "Empty room stage, no people, no operators, no loose tools, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_power_core_scale_02",
        "output_subpath": "world_4/foundry_power_core_scale_02",
        "prompt": (
            "A first-person view beneath the colossal structural cradle supporting the Deep-Core Engine. "
            "Gigantic hydraulic piston braces, articulated titanium mounting struts, and heavy dampening springs bolted to bedrock. "
            "Under-lighting of deep amber, red status rings, and cyan resonance conduits reflects across steel deck plates. "
            "Stylized anime-comic sci-fi art, thick inked outlines, monumental mechanical weight, "
            "with a spacious clear floor plane in the lower foreground. "
            "Empty room stage, no people, no characters, no tools on ground, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_power_core_scale_03",
        "output_subpath": "world_4/foundry_power_core_scale_03",
        "prompt": (
            "A first-person view facing an armored bank of phase-array locker vaults in the core power facility. "
            "Reinforced modular vault lockers with magnetic phase seals and heavy rotating locking handles built into the dark alloy bulkhead. "
            "Overhead crimson security floodlights and amber warning beacons pulse across dark non-slip floor tiles. "
            "Chunky anime-comic sci-fi style, thick dark outlines, high-security industrial aesthetic, "
            "and clean open flooring in the foreground. "
            "Empty room stage, no people, no guards, no hands, no readable warning labels, no letters, no logos."
        )
    },
    {
        "id": "foundry_power_core_scale_04",
        "output_subpath": "world_4/foundry_power_core_scale_04",
        "prompt": (
            "A first-person view inside the Coolant Choir pump corridor flanking the power core in the Foundry. "
            "Ranks of rhythmic, pulsing royal-blue coolant pipes vibrating against heavy structural wall clamps like organ pipes. "
            "A manual pressure-relief bypass valve manifold with blank round dials under cool cyan and warm amber spotlighting. "
            "Bold comic-cel sci-fi adventure look, thick dark outlines, rich blue coolant glow contrasted with warm iron, "
            "with clear diamond-plate flooring in the lower half of the frame. "
            "Empty room stage, no people, no characters, no leaks on floor, no readable dial numbers, no letters, no logos."
        )
    },
    {
        "id": "foundry_power_core_scale_final",
        "output_subpath": "world_4/foundry_power_core_scale_final",
        "prompt": (
            "A first-person view in a quiet, shadowed maintenance recess behind the power core mounting pylons. "
            "High-contrast angular shadows cast by monumental engine support struts against dark volcanic masonry walls. "
            "An unpowered calibration pedestal with dormant optical readouts bathed in dramatic cyan rim light and amber floor reflections. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, foreboding industrial silhouette, "
            "and clear dark floor plating across the foreground. "
            "Empty room stage, no people, no characters, no loose items, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_decon_chamber",
        "output_subpath": "world_4/foundry_decon_chamber",
        "prompt": (
            "A first-person view inside an industrial decontamination washdown chamber before the Foundry exterior. "
            "Heavy stainless steel wall panels with overhead pressurized chemical spray nozzles, drainage grates in the floor, and airtight portal frames. "
            "Dense swirling chemical disinfectant mist illuminated by sterile cyan and cool white LED strip lights. "
            "Bold comic-cel sci-fi adventure environment, thick dark outlines, clean geometric forms, "
            "with clear grated drainage flooring in the lower third. "
            "Empty room stage, no people, no workers, no suits, no hands, no readable hazard placards, no letters, no logos."
        )
    },
    {
        "id": "foundry_service_airlock_scale_01",
        "output_subpath": "world_4/foundry_service_airlock_scale_01",
        "prompt": (
            "A first-person view into a manual seal control nook nestled beside the massive airlock door in the Foundry. "
            "A rugged operator station with an oversized mechanical locking lever, manual wheel cog, and hydraulic pressure hoses. "
            "Muted amber work lights and red seal indicator lamps reflect on brushed steel paneling and concrete. "
            "Stylized anime-comic sci-fi adventure style, thick inked outlines, chunky mechanical controls, "
            "and uncluttered floor space in the foreground for mobile UI. "
            "Empty room stage, no people, no operators, no hands, no readable clipboard text, no letters, no logos."
        )
    },
    {
        "id": "foundry_service_airlock_scale_02",
        "output_subpath": "world_4/foundry_service_airlock_scale_02",
        "prompt": (
            "A first-person view looking down a chemical drainage sump trench under the airlock facility floor. "
            "Heavy cast-iron drainage grates over swirling chemical runoff that carries shimmering metallic specks under bright cyan underglow lights. "
            "Angular concrete retaining walls and yellow-and-black hazard curbs line the walkway. "
            "Chunky anime-comic sci-fi look, thick dark outlines, dynamic underfloor lighting, "
            "with a solid steel plate walkway in the foreground. "
            "Empty room stage, no people, no workers, no trash, no readable stencils, no letters, no logos."
        )
    },
    {
        "id": "foundry_service_airlock_scale_03",
        "output_subpath": "world_4/foundry_service_airlock_scale_03",
        "prompt": (
            "A first-person view along a narrow pressure lock transition gallery between the airlock and inner foundries. "
            "Rows of circular analog pressure dial gauges with yellow indicator rims mounted along heavy reinforced bulkheads. "
            "Overhead industrial floodlights cast long geometric shadows across dark riveted steel floor plates. "
            "Bold comic-cel sci-fi environment, thick dark outlines, rhythmic mechanical repetition, "
            "with clear open floor space in the lower half of the frame. "
            "Empty room stage, no people, no characters, no readable gauge numbers, no letters, no logos."
        )
    },
    {
        "id": "foundry_service_airlock_scale_04",
        "output_subpath": "world_4/foundry_service_airlock_scale_04",
        "prompt": (
            "A first-person view facing an industrial maintenance storage bay adjoining the service airlock. "
            "Heavy steel supply shelves neatly storing oversized rubber gasket seals, replacement pneumatic hoses, and brass coupling collars. "
            "Warm amber utility bulkhead lamps illuminate grey painted tool benches and clean concrete flooring. "
            "Stylized anime-comic sci-fi adventure aesthetic, thick dark outlines, chunky equipment silhouettes, "
            "with an open work floor in the foreground for mobile UI. "
            "Empty room stage, no people, no workers, no hands, no readable notes or labels, no letters, no logos."
        )
    },
    {
        "id": "foundry_service_airlock_scale_final",
        "output_subpath": "world_4/foundry_service_airlock_scale_final",
        "prompt": (
            "A first-person view inside a secluded emergency seal backroom behind the main airlock staging area. "
            "Reinforced wall lockers with glass door panels holding dormant emergency respirator units and oxygen tanks. "
            "A central preparation bench under soft teal ambient light and warm golden ceiling sconces. "
            "Chunky comic-cel sci-fi art, thick dark outlines, quiet utilitarian atmosphere, "
            "and clear dark tiled flooring across the foreground. "
            "Empty room stage, no people, no characters, no suits walking, no readable wall charts, no letters, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_7_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_7_ROOMS)}] Generating {room_id}...")
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

    print("\nBatch 7 completed successfully!")

if __name__ == "__main__":
    main()
