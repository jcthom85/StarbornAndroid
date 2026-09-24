#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 5 (Foundry Part 1).
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

BATCH_5_ROOMS = [
    {
        "id": "foundry_conditioning_observation",
        "output_subpath": "world_4/foundry_conditioning_observation",
        "prompt": (
            "A first-person view inside a sterile clinical observation booth overlooking the conditioning chambers of the Foundry. "
            "Reinforced green-tinted one-way glass overlooks an industrial testing pit far below. "
            "A sleek wrap-around control console with blank glowing green and amber telemetry monitors, recessed toggle switches, and ergonomic metal framing. "
            "Stylized anime-comic sci-fi aesthetic, thick dark outlines, clean clinical geometry, "
            "and uncluttered floor tiles in the foreground for mobile UI. "
            "Empty room stage, no people, no operators, no hands, no readable text on screens, no letters, no logos."
        )
    },
    {
        "id": "foundry_conditioning_scale_01",
        "output_subpath": "world_4/foundry_conditioning_scale_01",
        "prompt": (
            "A first-person view into a searing heat-soak testing chamber in the Foundry. "
            "Massive spiraling ceramic thermal coils glow intense incandescent orange along dark insulated composite walls. "
            "Heavy hydraulic robotic clamp arms hang open and dormant from the reinforced ceiling over heat-resistant floor plates. "
            "Bold comic-cel sci-fi art, thick dark outlines, blistering orange and charred gunmetal palette, "
            "with a clean ceramic tiled floor path in the foreground. "
            "Empty room stage, no people, no characters, no machines reading as creatures, no readable gauges, no letters, no logos."
        )
    },
    {
        "id": "foundry_conditioning_scale_02",
        "output_subpath": "world_4/foundry_conditioning_scale_02",
        "prompt": (
            "A first-person view down a cryogenic quench lane tunnel in the Foundry. "
            "Overhead high-pressure nitrogen vapor jets billow thick clouds of pale turquoise and white steam. "
            "Heavy automated roller tracks with frost-rimed steel guardrails run down the center beneath flashing yellow strobe lights. "
            "Chunky anime-comic sci-fi style, thick dark outlines, dramatic vapor lighting, "
            "with clear frost-dusted diamond-plate flooring in the lower foreground. "
            "Empty room stage, no people, no workers, no hands, no readable warning signs, no letters, no logos."
        )
    },
    {
        "id": "foundry_conditioning_scale_03",
        "output_subpath": "world_4/foundry_conditioning_scale_03",
        "prompt": (
            "A first-person view standing before a reinforced glass defect inspection window in the Foundry. "
            "Thick laminated glass with wire mesh inserts looks into an isolated testing cubicle lit by cold cyan overhead light. "
            "Heavy hydraulic seal frames, yellow hazard boundaries, and an unpowered mechanical diagnostic rig. "
            "Stylized comic-cel sci-fi adventure look, thick dark outlines, solid industrial framing, "
            "and clear dark floor plating in the lower half of the frame. "
            "Empty room stage, no people, no characters, no robots, no readable diagnostic text, no letters, no logos."
        )
    },
    {
        "id": "foundry_conditioning_scale_04",
        "output_subpath": "world_4/foundry_conditioning_scale_04",
        "prompt": (
            "A first-person view looking into a heavy impact calibration pit in the Foundry. "
            "A reinforced circular pit floor made of cracked industrial ceramic slabs engraved with abstract circular calibration target rings. "
            "Overhead drop-weight crane gantries and laser guide projectors cast intersecting red alignment beams through the hazy air. "
            "Bold anime-comic sci-fi adventure art, thick painted outlines, gritty yet vibrant industrial palette, "
            "with unobstructed perimeter deck plating in the foreground. "
            "Empty room stage, no people, no characters, no tools on the floor, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_conditioning_scale_final",
        "output_subpath": "world_4/foundry_conditioning_scale_final",
        "prompt": (
            "A first-person view inside a cavernous storage bay holding dormant mechanical chassis behind heavy glass partitions in the Foundry. "
            "Rows of vertical alcoves containing blank, deactivated bipedal robotic armatures resting silently in dark charging sockets. "
            "Dim ambient violet and amber status indicators cast subtle glows across polished industrial concrete flooring. "
            "Chunky comic-cel sci-fi aesthetic, thick dark outlines, atmospheric depth, "
            "with wide clear negative space across the foreground floor for mobile UI. "
            "Empty room stage, no people, no living characters, no active robots, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_belt_service_walk",
        "output_subpath": "world_4/foundry_belt_service_walk",
        "prompt": (
            "A first-person view along a narrow suspended catwalk walkway running high above the Foundry conveyor belts. "
            "Heavy industrial steel mesh floor grating, yellow tubular handrails, and rhythmic alternating amber warning beacons. "
            "Below the railing, colossal automated fabrication machinery and orange molten metal vats glow through the industrial smog. "
            "Bold comic-cel sci-fi environment, thick dark outlines, dizzying industrial scale, "
            "with a clean catwalk path stretching into the frame. "
            "Empty room stage, no people, no workers, no hands, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "foundry_reject_bay",
        "output_subpath": "world_4/foundry_reject_bay",
        "prompt": (
            "A first-person view entering a sunken scrap reject bay beneath the Foundry assembly lines. "
            "Massive chutes slope down into a sorting pit filled with discarded stamped metal plates, bent structural beams, and scrap gear cogwheels. "
            "Harsh overhead tungsten work lamps cast deep angular shadows against blackened reinforced concrete walls. "
            "Stylized anime-comic sci-fi adventure look, thick dark outlines, chunky geometric scrap piles, "
            "and a clear concrete walkway border in the foreground. "
            "Empty room stage, no people, no characters, no moving machines, no readable labels, no letters, no logos."
        )
    },
    {
        "id": "foundry_conveyor_belt_scale_01",
        "output_subpath": "world_4/foundry_conveyor_belt_scale_01",
        "prompt": (
            "A first-person view facing an imposing timing control tower overlooking the Foundry conveyor network. "
            "An elevated control pulpit with heavy riveted steel pylons, massive revolving gear teeth, and rhythmic amber timing strobe lights. "
            "Large mechanical tick-counters and abstract dial gauges without letters dominate the tower facade. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, strong architectural presence, "
            "and clear steel deck plating in the lower foreground. "
            "Empty room stage, no people, no characters, no readable clock faces or numbers, no letters, no logos."
        )
    },
    {
        "id": "foundry_conveyor_belt_scale_02",
        "output_subpath": "world_4/foundry_conveyor_belt_scale_02",
        "prompt": (
            "A first-person view of a high-speed pneumatic reject chute diversion junction along the conveyor line. "
            "A massive angled steel hopper chute with heavy rubber baffle curtains and pneumatic diversion pistons. "
            "Safety yellow-and-black hazard paint on structural support girders under flickering orange sodium-vapor lighting. "
            "Bold comic-cel sci-fi environment, thick dark outlines, powerful mechanical silhouettes, "
            "with clear floor grating in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no debris falling in air, no readable text, no letters, no logos."
        )
    },
    {
        "id": "foundry_conveyor_belt_scale_03",
        "output_subpath": "world_4/foundry_conveyor_belt_scale_03",
        "prompt": (
            "A first-person view crouching into a low industrial roller underpass beneath a giant assembly conveyor. "
            "Ceiling composed of hundreds of heavy steel roller cylinders and drive chains turning under yellow guide rails. "
            "Stained concrete floor marked with dark grease tracks leading toward a distant amber light. "
            "Claustrophobic anime-comic sci-fi adventure style, thick inked outlines, chunky machinery, "
            "with clear floor path in the foreground. "
            "Empty room stage, no people, no characters, no crawling figures, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "foundry_conveyor_belt_scale_04",
        "output_subpath": "world_4/foundry_conveyor_belt_scale_04",
        "prompt": (
            "A first-person view stepping onto an elevated control perch platform hanging directly over the central conveyor line. "
            "A heavy wire mesh security cage surrounds a manual emergency override lever console. "
            "Direct sightlines down the fiery length of the smelting factory floor with orange molten rivets and cyan sparks in the far background. "
            "Stylized comic-cel sci-fi art, thick dark outlines, dramatic industrial view, "
            "with a clean steel landing platform in the lower third. "
            "Empty room stage, no people, no operators, no hands, no readable text on panels, no letters, no logos."
        )
    },
    {
        "id": "foundry_conveyor_belt_scale_final",
        "output_subpath": "world_4/foundry_conveyor_belt_scale_final",
        "prompt": (
            "A first-person view inside the master belt nerve junction beneath the main Foundry floor. "
            "Dense clusters of heavy hydraulic conduits, pulsating copper coolant pipes, and massive rotating drive camshafts converge into a central drive core. "
            "Pulsing rhythm of amber, cyan, and deep orange light reflects on wet metallic conduits and dark steel floor plating. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, intricate yet readable mechanical forms, "
            "with clear foreground floor space for mobile UI. "
            "Empty room stage, no people, no characters, no readable gauges, no numbers, no letters, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_5_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_5_ROOMS)}] Generating {room_id}...")
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

    print("\nBatch 5 completed successfully!")

if __name__ == "__main__":
    main()
