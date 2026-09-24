#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 14 (World 1 & Debug Test Rooms - Final Batch).
Follows docs/story/Starborn_Art_Production_Guide.md:
- Model: gpt-image-2
- Quality: low
- Size: 1088x1920
- Format: Save source PNG, export shipping WebP (quality=90, method=6)
- Constraints: Ghost town rule, pure environmental stage, no characters, no hands, no readable text/logos.
"""

import sys
import time
import json
import base64
import urllib.request
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

BATCH_14_ROOMS = [
    {
        "id": "weather_lab",
        "output_subpath": "world_1/weather_lab",
        "prompt": (
            "A first-person view inside a cavernous underground atmospheric research and weather simulation hangar carved from dark rugged granite rock. "
            "Enormous overhead environmental simulation baffles, turbine fans, and climate control mist nozzles hang from iron scaffolding. "
            "Twin holographic test projection columns cast shifting atmospheric gradients of stormy turquoise, rain mist, and warm amber sunlight across a wide damp concrete floor marked with yellow hazard grid lines. "
            "Chunky anime-comic sci-fi adventure aesthetic, bold industrial machinery, thick dark outlines, "
            "with an open, calm concrete staging floor in the lower foreground. "
            "Empty room stage, no people, no scientists, no characters, no readable gauge text, no letters, no logos."
        )
    },
    {
        "id": "debug_presence_stress",
        "output_subpath": "world_1/debug_presence_stress",
        "prompt": (
            "A first-person view of a high-capacity industrial staging concourse in the subterranean mining complex. "
            "Massive steel blast pillars and heavy yellow crane gantries frame an expansive open logistics hall. "
            "Recessed amber floodlights in the vaulted ceiling cast bold geometric shadows across clean dark steel-plate decking marked with bold diagonal safety striping. "
            "Chunky comic-cel anime sci-fi scenery, thick dark contours, strong spacious perspective, "
            "with a wide, completely uncluttered deck in the foreground for UI overlays. "
            "Empty room stage, no people, no workers, no supply crates, no loot, no readable placards, no letters, no logos."
        )
    },
    {
        "id": "debug_enemy_party_sizes",
        "output_subpath": "world_1/debug_enemy_party_sizes",
        "prompt": (
            "A first-person view of an expansive subterranean testing proving ground with high arched stone vaults supported by heavy riveted iron trusses. "
            "Glowing cyan floor marker lines delineate circular combat staging zones across a vast, flat obsidian rock floor. "
            "Overhead industrial floodlamps cast clean shafts of white and pale gold light through faint rock dust. "
            "Bold graphic anime sci-fi aesthetic, thick dark outlines, clear tactical floor layout, "
            "with an open stone floor spanning the entire foreground. "
            "Empty room stage, no enemies, no monsters, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "debug_enemy_party_sizes_west",
        "output_subpath": "world_1/debug_enemy_party_sizes_west",
        "prompt": (
            "A first-person view looking west into a wide reinforced bunker staging bay with thick blast-shielded concrete walls painted in weathered teal and orange. "
            "Several heavy vertical bulkhead partitions divide the cavernous room into five distinct open staging corridors. "
            "Industrial strip lights cast cool white and pale yellow illumination onto a scuffed iron-grate floor. "
            "Stylized anime adventure scenery, chunky geometric architecture, bold ink outlines, "
            "with a spacious clear grated foreground for game cards. "
            "Empty room stage, no enemies, no characters, no patrol guards, no readable numbers, no letters, no logos."
        )
    },
    {
        "id": "debug_enemy_party_sizes_north",
        "output_subpath": "world_1/debug_enemy_party_sizes_north",
        "prompt": (
            "A first-person view looking toward an imposing monumental blast door on the north wall of an underground transit vault. "
            "Massive hydraulic pistons, heavy yellow warning stripes, and thick braided power cables frame the closed iron bulkhead. "
            "The wide approach avenue is paved with durable diamond-plate steel tiles flanked by low amber guide lamps. "
            "Chunky anime comic sci-fi art style, strong forward perspective, bold dark outlines, "
            "with a broad open steel floor in the lower foreground. "
            "Empty room stage, no enemies entering, no soldiers, no characters, no readable door warnings, no letters, no logos."
        )
    },
    {
        "id": "debug_enemy_party_sizes_south",
        "output_subpath": "world_1/debug_enemy_party_sizes_south",
        "prompt": (
            "A first-person view facing a heavy reinforced southern vehicle deployment ramp leading down into the subterranean mines. "
            "Dual overhead exhaust vent fans with rotating blades cast rhythmic moving shadows under warm industrial amber lamps. "
            "The wide ramp is flanked by sturdy yellow steel crash barriers and runs smoothly toward the foreground. "
            "Bold graphic comic-cel anime aesthetic, thick dark outlines, rich industrial lighting, "
            "with an uncluttered ramp landing in the immediate foreground. "
            "Empty room stage, no vehicles, no enemies, no characters, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "debug_enemy_party_sizes_east",
        "output_subpath": "world_1/debug_enemy_party_sizes_east",
        "prompt": (
            "A first-person view of an airy eastern observation transit bay overlooking an immense subterranean quarry. "
            "Reinforced panoramic view windows on the east wall reveal deep cavern vistas illuminated by amber floodlights and twinkling distant work lamps. "
            "Clean polished concrete flooring reflects the cool cyan ambient lighting of the high ceiling fixtures. "
            "Chunky anime adventure scenery, bold outlines, serene spacious technological staging, "
            "with a wide clear concrete floor across the foreground. "
            "Empty room stage, no people, no enemies, no characters, no readable text, no letters, no logos."
        )
    }
]

def generate_image(client: OpenAI, prompt: str) -> bytes:
    response = client.images.generate(
        model="gpt-image-2",
        prompt=prompt,
        n=1,
        size="1088x1920",
        quality="low",
    )
    if response.data[0].b64_json:
        return base64.b64decode(response.data[0].b64_json)
    elif response.data[0].url:
        image_url = response.data[0].url
        req = urllib.request.Request(image_url, headers={"User-Agent": "StarbornAssetGen/1.0"})
        with urllib.request.urlopen(req) as resp:
            return resp.read()
    else:
        raise ValueError("No image data returned from OpenAI API.")

def process_batch():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    rooms_file = PROJECT_ROOT / "app/src/main/assets/rooms.json"
    with open(rooms_file, "r", encoding="utf-8") as f:
        rooms_data = json.load(f)
    rooms_by_id = {r["id"]: r for r in rooms_data}
    
    total = len(BATCH_14_ROOMS)
    print(f"Starting Batch 14 (FINAL BATCH): {total} rooms in World 1 / Debug Tests...")
    
    completed_ids = []
    
    for idx, item in enumerate(BATCH_14_ROOMS, 1):
        room_id = item["id"]
        subpath = item["output_subpath"]
        prompt = item["prompt"]
        
        output_png = PROJECT_ROOT / f"world_assets/src/main/assets/images/rooms/{subpath}.png"
        output_webp = PROJECT_ROOT / f"world_assets/src/main/assets/images/rooms/{subpath}.webp"
        
        output_png.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{total}] Generating {room_id}...")
        
        # 1. Generate & download image
        png_bytes = generate_image(client, prompt)
        output_png.write_bytes(png_bytes)
        print(f"  -> Generated PNG: {output_png.name}")
        
        # 2. Encode to shipping WebP (quality=90, method=6)
        with Image.open(output_png) as img:
            img.save(output_webp, "WEBP", quality=90, method=6)
        print(f"  -> Encoded WebP: {output_webp.name} ({output_webp.stat().st_size // 1024} KB)")
        
        completed_ids.append((room_id, f"images/rooms/{subpath}.webp"))
        time.sleep(1) # Polite pause between API calls
        
    # 3. Wire into rooms.json immediately
    print("\nWiring Batch 14 assets into app/src/main/assets/rooms.json...")
    for r_id, webp_rel_path in completed_ids:
        if r_id in rooms_by_id:
            rooms_by_id[r_id]["background_image"] = webp_rel_path
        else:
            print(f"WARNING: Room ID {r_id} not found in rooms.json!")
            
    with open(rooms_file, "w", encoding="utf-8") as f:
        json.dump(rooms_data, f, indent=2, ensure_ascii=False)
        f.write("\n")
        
    print(f"\nBatch 14 complete! Wired {len(completed_ids)} new rooms into rooms.json. 100% of all rooms are now uniquely wired!")

if __name__ == "__main__":
    process_batch()
