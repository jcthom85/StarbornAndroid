#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 12 (The Source Part 1: Campfire, Center, Echo Mines, Gh0st Nightmare, Memory Bridge).
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

BATCH_12_ROOMS = [
    # --- The Campfire Scale Nodes (5 rooms) ---
    {
        "id": "source_campfire_node_scale_01",
        "output_subpath": "world_6/source_campfire_node_scale_01",
        "prompt": (
            "A first-person view of a surreal cosmic campsite in the Source where concentric open rings of glowing iridescent blue ash drift softly across dark starlit stone. "
            "In the center, a gentle cosmic hearth embers with deep cobalt and soft teal flames that cast flickering light into the void. "
            "The ash rings spiral outwards leaving noticeable wide gaps across the dark ground. "
            "Bold anime comic sci-fi adventure art style, chunky stylized silhouettes, thick dark outlines, luminous dreamlike atmosphere, "
            "with a wide uncluttered expanse of dark stone ground in the foreground for game UI. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_campfire_node_scale_02",
        "output_subpath": "world_6/source_campfire_node_scale_02",
        "prompt": (
            "A first-person view standing before three distinct dark pathways branching outward from a blue-burning cosmic campfire into abstract starry space. "
            "One path is lined with jagged jagged dark industrial iron spikes, another with cold flickering static-fog pillars, and the third with deep abyssal shadows. "
            "Vivid cobalt and violet firelight catches the edges of the fractured thresholds. "
            "Graphic comic-cel sci-fi environment, bold outlines, heightened adventure color palette, "
            "with a clear flat stone foreground landing. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_campfire_node_scale_03",
        "output_subpath": "world_6/source_campfire_node_scale_03",
        "prompt": (
            "A first-person view beside a quiet cosmic campfire on a solitary floating terrace of dark cosmic rock. "
            "Resting on an empty flat stone ledge next to the blue hearth embers is a neatly folded, thick rough-spun geometric survival blanket with dark teal and charcoal blocks. "
            "Beyond the terrace lies the infinite indigo void studded with faint geometric constellations and drifting star-dust. "
            "Expressive anime sci-fi scenery, chunky props, bold outlines, serene yet poignant solitude, "
            "with an open dark stone floor in the lower foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_campfire_node_scale_04",
        "output_subpath": "world_6/source_campfire_node_scale_04",
        "prompt": (
            "A first-person view looking directly into the heart of a cosmic hearth fire where a singular multifaceted golden-white key ember levitates and slowly spins within the blue flames. "
            "Fractured crystalline sparks of amber, gold, and turquoise radiate outward in slow floating arcs across the dark polished hearth basin. "
            "Stylized anime adventure environmental art, rich saturated contrast, chunky linework, dreamlike mythic sci-fi, "
            "with a clean calm foreground stone border for interface framing. "
            "Empty room stage, no people, no hands, no characters, no readable glyphs, no letters, no logos."
        )
    },
    {
        "id": "source_campfire_node_scale_final",
        "output_subpath": "world_6/source_campfire_node_scale_final",
        "prompt": (
            "A first-person view overlooking a vast celestial memory shore where swirling tide marks made of blue ash, gold sand, and slag-red dust ripple across dark obsidian glass. "
            "In the midground, a towering campfire bonfire burns with multicolored radiant flames of cobalt, amber, and vibrant violet, uniting the ash lines. "
            "Looming in the celestial sky above are vast dreamy translucent aurora vistas of mining caverns, neon spires, and orbital domes. "
            "Bold comic-book anime adventure aesthetic, strong silhouettes, vibrant celebratory hues, "
            "with an expansive smooth dark shore in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },

    # --- The Center Scale Nodes (4 rooms) ---
    {
        "id": "source_center_node_scale_01",
        "output_subpath": "world_6/source_center_node_scale_01",
        "prompt": (
            "A first-person view along a curved ethereal white shore in the deep Source, bordering a completely motionless mirror-still celestial sea. "
            "The pale porcelain-like sand gleams with faint mother-of-pearl iridescence under a radiant sky of soft violet nebula clouds. "
            "Gentle harmonic ripples made of pure light form along the water's edge. "
            "Chunky anime adventure environment, clean bold geometric landforms, thick dark outlines, luminous dreamscape, "
            "with a wide flat expanse of white sand in the foreground for mobile UI cards. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_center_node_scale_02",
        "output_subpath": "world_6/source_center_node_scale_02",
        "prompt": (
            "A first-person view of a surreal cosmic orbit chamber at the Source Center where concentric rings of floating crystalline root sparks and glowing amber embers rotate slowly through the air. "
            "Prismatic light refracts through geometric crystal shards, casting kaleidoscopic patterns of warm gold, cyan, and magenta across pristine white terrace stones. "
            "Bold graphic anime sci-fi scenery, expressive shapes, thick dark contours, "
            "with an uncluttered white stone platform in the foreground. "
            "Empty room stage, no people, no floating bodies, no hands, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_center_node_scale_03",
        "output_subpath": "world_6/source_center_node_scale_03",
        "prompt": (
            "A first-person view of the Silence Pulpit rising out of the still white ocean at the Center of the Source. "
            "A towering, austere architectural pulpit constructed from interlocking monolithic geometric planes of blinding white light and polished gray obsidian. "
            "Massive beam pillars of stark white luminescence shoot straight into the dark void above, framing a calm minimalist shrine. "
            "Stylized comic-cel anime sci-fi art, monumental crystalline geometry, thick outlines, stark dramatic contrast, "
            "with a spacious stone jetty in the foreground for UI. "
            "Empty room stage, no people, no figures on the pulpit, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_center_node_scale_final",
        "output_subpath": "world_6/source_center_node_scale_final",
        "prompt": (
            "A first-person view standing at the heart of the Center where multiple intersecting harmonic voice orbits of vibrant chromatic light weave together in a grand spherical constellation. "
            "Ribbons of luminous gold, electric cyan, fiery orange, and deep sapphire trace harmonious intersecting orbital paths above a luminous hexagonal stone dais. "
            "The surrounding void glows with warm welcoming starlight and soft pastel cosmic auroras. "
            "Epic anime adventure environment, bold comic outlines, triumphant colorful composition, "
            "with a clear hexagonal stone foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },

    # --- Echo of the Mines (7 rooms) ---
    {
        "id": "source_echo_workbench",
        "output_subpath": "world_6/source_echo_workbench",
        "prompt": (
            "A first-person view of a nostalgic mining workbench manifested inside a cavern of swirling cosmic stardust and dark rock. "
            "A heavy steel and timber workbench sits under draped translucent yellow shelter plastic, with neatly organized chunky retro-futuristic hand tools, wrenches, and solder torches resting on hooks and magnetic strips. "
            "Warm amber work-lamp light illuminates the worn benchtop against the surrounding cool purple void. "
            "Chunky anime-comic sci-fi aesthetic, bold dark outlines, stylized industrial details, "
            "with an open dust-dusted steel floor in the foreground. "
            "Empty room stage, no people, no characters, no ghost figures, no loose loot, no readable labels, no letters, no logos."
        )
    },
    {
        "id": "source_echo_patrol",
        "output_subpath": "world_6/source_echo_patrol",
        "prompt": (
            "A first-person view of a surreal mining security checkpoint in the memory caverns. "
            "Overlapping holographic red patrol grid lines and translucent scan-cones slice through thick amber subterranean dust. "
            "Beyond the patrol zone stands an imposing arched Dominion shift gate made of riveted dark iron plates, standing slightly ajar with warm light spilling through. "
            "Stylized anime adventure comic art, bold industrial silhouettes, thick black linework, rich dramatic lighting, "
            "with a wide clear iron catwalk in the foreground. "
            "Empty room stage, no people, no security guards, no drones, no readable signage, no letters, no logos."
        )
    },
    {
        "id": "source_echo_mines_node_scale_01",
        "output_subpath": "world_6/source_echo_mines_node_scale_01",
        "prompt": (
            "A first-person view down an infinite dreamlike corridor of battered steel mining lockers repeating into misty cosmic darkness. "
            "The lockers are painted distressed teal and industrial yellow, dented and coated with fine rock dust. "
            "On one prominent locker door is a large bright round unreadable stylized cartoon sticker with thick outlines. "
            "Beside it hangs an old wooden chalkboard framed in iron, covered in chalk tally marks. "
            "Chunky anime-comic environment, strong perspective lines, atmospheric dust motes lit by an overhead yellow caged lamp, "
            "with a clear grated floor in the foreground. "
            "Empty room stage, no people, no characters, no readable words or numbers, no logos."
        )
    },
    {
        "id": "source_echo_mines_node_scale_02",
        "output_subpath": "world_6/source_echo_mines_node_scale_02",
        "prompt": (
            "A first-person view of a cavernous mining chamber where countless glowing holographic slips and punch-cards drift through the air like slow-falling autumn leaves. "
            "The drifting slips emit faint warm amber and cyan phosphor glows against rough dark cavern walls reinforced with heavy timber and steel arches. "
            "Shafts of golden volumetric light pierce the subterranean haze. "
            "Bold graphic anime sci-fi scenery, thick dark outlines, expressive visual storytelling, "
            "with a solid clear rock and plank floor in the foreground. "
            "Empty room stage, no people, no characters, no readable text on the slips, no letters, no logos."
        )
    },
    {
        "id": "source_echo_mines_node_scale_03",
        "output_subpath": "world_6/source_echo_mines_node_scale_03",
        "prompt": (
            "A first-person view inside a cavern where enormous sheets of industrial yellow and orange shelter plastic flap and ripple in an unseen breeze. "
            "Torn gaps between the billowing plastic sheets reveal a breathtaking, impossible subterranean sunrise glowing with radiant gold, coral, and pink light across jagged cavern pillars. "
            "Rough mine support beams and heavy cables frame the vibrant vista. "
            "Vivid anime comic adventure art style, strong silhouette shapes, thick dark contours, "
            "with an open rocky dirt floor in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_echo_mines_node_scale_04",
        "output_subpath": "world_6/source_echo_mines_node_scale_04",
        "prompt": (
            "A first-person view of a quiet hidden alcove tucked between two sweeping crimson holographic surveillance cones in a dark rock mine. "
            "The alcove floor is made of worn, scuffed steel plates with a couple of overturned sturdy wooden crates arranged as a makeshift resting spot. "
            "A small warm battery lantern casts a cozy orange glow inside the shadow pocket, completely safe from the crimson grid outside. "
            "Chunky comic-cel sci-fi environment, thick dark outlines, atmospheric contrast, "
            "with a wide clear foreground floor for game cards. "
            "Empty room stage, no people, no workers, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_echo_mines_node_scale_final",
        "output_subpath": "world_6/source_echo_mines_node_scale_final",
        "prompt": (
            "A first-person view of a triumphant mine egress corridor where a series of heavy blast doors stand permanently unlatched and swung wide open from the inside. "
            "Through the sequence of open bulkheads, radiant morning sunlight and blue sky pour into the dark rock tunnel, banishing the subterranean gloom. "
            "Overhead industrial cables and yellow safety rails guide the path toward freedom. "
            "Bold anime comic adventure aesthetic, dramatic high-key lighting, thick outlines, hopeful atmosphere, "
            "with an open steel ramp in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },

    # --- Endless War / Gh0st's Nightmare (5 rooms) ---
    {
        "id": "source_gh0st_kill_suite",
        "output_subpath": "world_6/source_gh0st_kill_suite",
        "prompt": (
            "A first-person view inside a harrowing military cybernetic command hub bathed in deep blood-red emergency light. "
            "Glowing red targeting reticles, lock-on brackets, and tactical vector lines crawl across brutalist gunmetal bulkhead walls and armored server consoles. "
            "Flickering holographic war displays glitch and repeat warning patterns into the smoky air. "
            "Stylized anime adventure comic art, chunky military sci-fi consoles, bold dark outlines, intense crimson and charcoal palette, "
            "with a clean steel grated floor in the lower foreground. "
            "Empty room stage, no people, no soldiers, no characters, no readable text on screens, no letters, no logos."
        )
    },
    {
        "id": "source_gh0st_elara_signal",
        "output_subpath": "world_6/source_gh0st_elara_signal",
        "prompt": (
            "A first-person view of a tranquil, shielded alcove nestled within a chaotic red military bunker. "
            "In the center of the alcove stands a solitary communication terminal displaying a pure, steady, glowing cyan audio waveform ribbon that loops serenely. "
            "The calm cyan luminescence forms a protective sphere of soft light, repelling the harsh crimson tactical static outside. "
            "Chunky anime sci-fi scenery, thick dark outlines, poignant emotional atmosphere, "
            "with a clear gunmetal floor in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_gh0st_nightmare_node_scale_01",
        "output_subpath": "world_6/source_gh0st_nightmare_node_scale_01",
        "prompt": (
            "A first-person view looking down into an immense vertical command well inside a military fortress. "
            "Tiered balconies of uncrewed tactical command terminals plunge down through layers of flickering grey-green radar static and amber warning beacons. "
            "From a central console in the foreground, several blank stylized silver metallic identity tags on small ball chains hang motionless. "
            "Bold graphic anime sci-fi environment, dizzying vertical perspective, thick dark outlines, "
            "with a solid armored catwalk in the immediate foreground. "
            "Empty room stage, no people, no officers, no characters, no readable tag text, no letters, no logos."
        )
    },
    {
        "id": "source_gh0st_nightmare_node_scale_02",
        "output_subpath": "world_6/source_gh0st_nightmare_node_scale_02",
        "prompt": (
            "A first-person view through an eerie, surreal indoor hall where dozens of stylized, chunky mechanical rifle barrels grow upward from the steel floor like a dead forest of iron trees. "
            "Every single barrel is lowered and tilted downward toward the floor in total deactivation. "
            "Muted gunmetal grey and cold blue hues dominate the hall, illuminated by pale overhead fluorescent strips reflected in the damp floor. "
            "Chunky comic-cel sci-fi art, bold outlines, haunting symbolic adventure scenery, "
            "with a clear winding path between the iron barrels in the foreground. "
            "Empty room stage, no people, no soldiers, no hands, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_gh0st_nightmare_node_scale_03",
        "output_subpath": "world_6/source_gh0st_nightmare_node_scale_03",
        "prompt": (
            "A first-person view of a sacred radio sanctuary deep within the nightmare military facility. "
            "A magnificent, oversized retro broadcast transceiver unit glows with luminous sapphire and cyan light, projecting a giant translucent holographic ribbon that twists gracefully in the air like a soothing auroral scarf. "
            "The oppressive industrial surroundings fade into soft indigo twilight under its warm glow. "
            "Expressive anime adventure art, thick dark outlines, magical technology aesthetic, "
            "with an open metal floor in the foreground. "
            "Empty room stage, no people, no characters, no readable dial text, no letters, no logos."
        )
    },

    # --- Memory Bridge (7 rooms) ---
    {
        "id": "source_memory_bridge_span",
        "output_subpath": "world_6/source_memory_bridge_span",
        "prompt": (
            "A first-person view standing on an extraordinary composite memory bridge spanning an infinite cosmic abyss. "
            "The bridge surface transitions harmoniously between translucent turquoise tideglass sand, glowing slag-orange foundry steel, and sleek wet rain-slick Spire glass. "
            "Beside the path sits a small glowing memory altar chest of warm brass emitting gentle golden particles into the starlight. "
            "Epic anime adventure comic art style, rich chromatic diversity, thick bold contours, "
            "with a wide sturdy bridge span in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_memory_threshold",
        "output_subpath": "world_6/source_memory_threshold",
        "prompt": (
            "A first-person view approaching the narrow final threshold of the memory bridge where it reaches a radiant singularity. "
            "A bright, vertical teardrop rift of pure pearlescent white and golden light tears through the cosmic sky like a magnificent celestial wound. "
            "Floating geometric stone shards and glowing star-strands converge toward the radiant portal. "
            "Chunky comic-cel anime sci-fi scenery, thick dark outlines, awe-inspiring scale and dramatic contrast, "
            "with an uncluttered converging bridge walkway in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_memory_bridge_node_scale_01",
        "output_subpath": "world_6/source_memory_bridge_node_scale_01",
        "prompt": (
            "A first-person view walking across a translucent tideglass causeway segment of the memory bridge. "
            "The path is formed of wet, hardened crystalline beach sand that glows with internal turquoise bioluminescence, with faint stylized barefoot impressions fossilized in the glass. "
            "Beneath the bridge, deep sapphire ocean tides blend seamlessly into a starry cosmic sky. "
            "Vibrant anime adventure environmental art, thick outlines, luminous water and crystal shaders, "
            "with a clear glassy walkway in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_memory_bridge_node_scale_02",
        "output_subpath": "world_6/source_memory_bridge_node_scale_02",
        "prompt": (
            "A first-person view along an industrial Foundry heat rib locked into the memory bridge structure. "
            "Massive forged steel trusses and glowing obsidian beams radiate warm slag-orange light along the edges of the pathway, casting long dramatic shadows. "
            "Overhead, great curved mechanical arches frame a backdrop of purple starfields and drifting molten embers. "
            "Bold graphic anime sci-fi aesthetic, heavy industrial outlines, high-contrast warm and cool lighting, "
            "with a solid dark steel plate floor in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_memory_bridge_node_scale_03",
        "output_subpath": "world_6/source_memory_bridge_node_scale_03",
        "prompt": (
            "A first-person view along a neon-drenched Spire rain line bridging the cosmic divide. "
            "Sleek black glass catwalks with glowing teal and magenta neon underglow are washed in a gentle drizzle of luminous neon raindrops that hang suspended in zero gravity. "
            "Each floating water droplet catches prismatic reflections of distant skyscraper silhouettes against the dark void. "
            "Cyberpunk anime adventure environment, clean bold comic linework, vibrant neon reflections, "
            "with an open slick glass floor in the foreground. "
            "Empty room stage, no people, no characters, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "source_memory_bridge_node_scale_04",
        "output_subpath": "world_6/source_memory_bridge_node_scale_04",
        "prompt": (
            "A first-person view following a narrow, radiant golden beam rail representing a spaceship engine note extending across dark cosmic static. "
            "The golden rail pulses with warm sunlight and acoustic vibration ripples, flanked by twin rows of small amber indicator pylons. "
            "Surrounding the bridge are swirling clouds of dark purple nebula dust and gentle gold star clusters. "
            "Bold anime comic sci-fi scenery, thick dark outlines, heroic adventure atmosphere, "
            "with a clear sturdy gold-accented deck in the foreground. "
            "Empty room stage, no people, no characters, no hands, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_memory_bridge_node_scale_final",
        "output_subpath": "world_6/source_memory_bridge_node_scale_final",
        "prompt": (
            "A first-person view along the grand united crew span of the memory bridge. "
            "The wide walkway is constructed of interlocking mosaic stones in gold, turquoise, slag-orange, and violet, which glow brilliantly wherever overlapping shadows touch the floor. "
            "Grand illuminated archways constructed of celestial crystal and brass frame a magnificent view of all six worlds visible as radiant distant jewels in the cosmic dawn. "
            "Triumphant anime adventure art style, rich vibrant color harmony, bold comic outlines, "
            "with an expansive mosaic landing in the foreground for UI cards. "
            "Empty room stage, no people, no silhouettes, no characters, no readable text, no letters, no logos."
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
    import base64
    import urllib.request
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
    
    total = len(BATCH_12_ROOMS)
    print(f"Starting Batch 12: {total} rooms in World 6 (Campfire, Center, Echo Mines, Gh0st Nightmare, Memory Bridge)...")
    
    completed_ids = []
    
    for idx, item in enumerate(BATCH_12_ROOMS, 1):
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
    print("\nWiring Batch 12 assets into app/src/main/assets/rooms.json...")
    for r_id, webp_rel_path in completed_ids:
        if r_id in rooms_by_id:
            rooms_by_id[r_id]["background_image"] = webp_rel_path
        else:
            print(f"WARNING: Room ID {r_id} not found in rooms.json!")
            
    with open(rooms_file, "w", encoding="utf-8") as f:
        json.dump(rooms_data, f, indent=2, ensure_ascii=False)
        f.write("\n")
        
    print(f"\nBatch 12 complete! Wired {len(completed_ids)} new rooms into rooms.json.")

if __name__ == "__main__":
    process_batch()
