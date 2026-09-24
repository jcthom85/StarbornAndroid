#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 13 (The Source Part 2: Memory Stair, New World, Orion Nightmare, Spire of Thought, Zeke Nightmare).
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

BATCH_13_ROOMS = [
    # --- Memory Stair (6 rooms) ---
    {
        "id": "source_memory_fragments",
        "output_subpath": "world_6/source_memory_fragments",
        "prompt": (
            "A first-person view of a surreal cosmic landing where fractured floating islands from five worlds drift in zero gravity around a grand spiral staircase. "
            "Floating chunks of brown mine rock, glowing emerald canopy moss, neon-streaked wet Spire glass, slag-orange foundry iron, and frost-rimed white orbital ring tiles slowly orbit the central landing. "
            "Gentle auroral light ribbons of cyan, gold, and violet weave between the fragments into the starry cosmic indigo void. "
            "Chunky anime comic sci-fi adventure scenery, thick dark outlines, dramatic floating archipelago composition, "
            "with a wide, clean composite stone landing platform in the lower foreground for mobile UI. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_memory_stair_node_scale_01",
        "output_subpath": "world_6/source_memory_stair_node_scale_01",
        "prompt": (
            "A first-person view ascending a wide, monolithic stone stair step split down the center by two contrasting textures. "
            "The left half is made of rough, dark subterranean mine rock covered in fine orange dust and faint stylized tool chisel scratches. "
            "The right half is polished to mirror-smooth, pristine porcelain-white stone by celestial light. "
            "Sturdy wooden mining timber beams frame the rough side under warm amber lantern light, while pale starlight bathes the polished side. "
            "Bold comic-cel anime sci-fi environment, thick dark contours, striking symbolic contrast, "
            "with the broad lower step expanse filling the foreground. "
            "Empty room stage, no people, no characters, no readable carving text, no letters, no logos."
        )
    },
    {
        "id": "source_memory_stair_node_scale_02",
        "output_subpath": "world_6/source_memory_stair_node_scale_02",
        "prompt": (
            "A first-person view of a living stair step formed from colossal intertwined gnarled swamp canopy roots. "
            "Thick emerald moss and bioluminescent turquoise fungi cushion the dark wooden step, which bears deep stylized wilderness notches. "
            "Lush hanging jungle vines and soft floating green spore motes drift through warm shafts of morning sunlight piercing the cosmic canopy. "
            "Vivid anime adventure art style, chunky botanical shapes, bold ink outlines, enchanting organic atmosphere, "
            "with a wide solid mossy root landing in the foreground. "
            "Empty room stage, no animals, no people, no creatures, no readable markings, no letters, no logos."
        )
    },
    {
        "id": "source_memory_stair_node_scale_03",
        "output_subpath": "world_6/source_memory_stair_node_scale_03",
        "prompt": (
            "A first-person view ascending a sleek staircase step constructed from thick, dark tinted glass that glows with internal neon city reflections. "
            "Underneath the wet glass surface, abstract patterns of electric teal and hot magenta neon light grids flicker like a distant cyberpunk metropolis in the rain. "
            "Faint hairline fractures in the glass reveal glowing cyan energy veins. "
            "Stylized anime adventure aesthetic, thick outlines, rich high-contrast neon reflections, "
            "with a clean dark glass step landing in the foreground. "
            "Empty room stage, no people, no characters, no readable neon signs or numbers, no letters, no logos."
        )
    },
    {
        "id": "source_memory_stair_node_scale_04",
        "output_subpath": "world_6/source_memory_stair_node_scale_04",
        "prompt": (
            "A first-person view of an austere, sound-dampening architectural stair step in the Void sanctuary. "
            "Wedge-shaped acoustic baffle panels of matte white composite material line the walls, absorbing light into gentle muted shades of pale silver. "
            "In the center, a solitary vibrant cyan holographic audio waveform ribbon cuts cleanly through the chamber like a bright laser thread. "
            "Chunky anime-comic sci-fi scenery, thick dark outlines, minimalist geometric grandeur, "
            "with a wide smooth matte white step platform in the foreground. "
            "Empty room stage, no people, no characters, no readable labels, no letters, no logos."
        )
    },
    {
        "id": "source_memory_stair_node_scale_final",
        "output_subpath": "world_6/source_memory_stair_node_scale_final",
        "prompt": (
            "A first-person view of the grand culmination step of the memory stair, forged from battle-tested materials from every world. "
            "The monumental landing combines riveted steel plates, rough mine rock, living green root inlays, and sleek orbital brass brackets, proud and unpolished. "
            "Ahead, the staircase arches directly toward a brilliant golden sunrise cresting over cosmic cloud banks. "
            "Triumphant anime adventure art, bold comic outlines, warm and vibrant heroic lighting, "
            "with an expansive mosaic landing in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },

    # --- New World / The Epilogue (4 rooms) ---
    {
        "id": "source_new_world_node_scale_01",
        "output_subpath": "world_6/source_new_world_node_scale_01",
        "prompt": (
            "A first-person view inside a cozy, sunny retro-futuristic colony diner. "
            "A rounded chrome-trimmed vinyl booth upholstered in cheerful teal and yellow sits beside a large sunlit window. "
            "On the nearby clean laminate counter sits a round glass coffee pot gently steaming on a warm hotplate beside a clean ceramic mug. "
            "Warm golden morning sunlight streams through the window, illuminating wooden ceiling beams and hanging orange pendant lamps. "
            "Chunky anime-comic adventure aesthetic, thick dark outlines, peaceful welcoming warmth, "
            "with a clean linoleum floor in the lower foreground for game UI. "
            "Empty room stage, no people, no customers, no staff, no readable text on menus, no letters, no logos."
        )
    },
    {
        "id": "source_new_world_node_scale_02",
        "output_subpath": "world_6/source_new_world_node_scale_02",
        "prompt": (
            "A first-person view facing an oversized wooden community bulletin board mounted on the wall of a sunlit seaside village hall. "
            "The cork board is filled with colorful blank flyer cards, hand-drawn map sketches, and a prominent stylized polaroid-style instant photo of a sunny beach pinned by a brass thumbtack. "
            "Warm sunlight spills from an open doorway beside the board, washing across polished cedar floorboards. "
            "Chunky anime comic art style, bold outlines, cheerful community warmth, "
            "with wide clear wooden flooring in the foreground. "
            "Empty room stage, no people, no faces with readable features, no readable writing on flyers, no letters, no logos."
        )
    },
    {
        "id": "source_new_world_node_scale_03",
        "output_subpath": "world_6/source_new_world_node_scale_03",
        "prompt": (
            "A first-person view looking out through a large, wide-open wood-and-steel casement window of a high coastal lodge. "
            "Outside lies a breathtaking, unbounded real horizon: rolling green hills, a sparkling blue ocean under a vast open turquoise sky, and a brilliant golden morning sun rising without any shields or dome walls. "
            "A gentle coastal breeze softly rustles light linen curtains framing the window ledge. "
            "Vibrant anime adventure scenery, clean bold outlines, uplifting painterly atmosphere, "
            "with a calm wooden windowsill and floor in the lower foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_new_world_node_scale_final",
        "output_subpath": "world_6/source_new_world_node_scale_final",
        "prompt": (
            "A first-person view looking down a charming, bustling morning street in a newly founded frontier colony town. "
            "Sturdy wooden and painted-steel modular homes and cafes line a cobblestone avenue under a clear blue sky with puffy white clouds. "
            "Parked near the street corner is a rugged yellow utility hover-transport, and in the distance a sleek Astra passenger shuttle sits peacefully on a seaside landing pad. "
            "Bright, optimistic anime comic adventure environment, bold silhouettes, cheerful palette of yellow, cyan, and terra cotta, "
            "with a broad cobblestone street in the foreground. "
            "Empty room stage, no people, no crowd, no characters, no readable signs or shop text, no letters, no logos."
        )
    },

    # --- Orion's Nightmare / The Tide-Well (5 rooms) ---
    {
        "id": "source_orion_tide_well",
        "output_subpath": "world_6/source_orion_tide_well",
        "prompt": (
            "A first-person view standing at the edge of a deep circular oceanic well of pitch-black water set into dark volcanic tide-rocks. "
            "Gentle luminescent ripples pulse across the dark water surface, carrying faint concentric rings of soft cyan and violet light from the deep. "
            "The surrounding air is filled with cool ocean mist beneath a twilight sky of dim indigo and distant salt-white stars. "
            "Stylized anime adventure comic art, thick dark contours, solemn atmospheric mood, "
            "with a wide, flat basalt rock rim in the foreground. "
            "Empty room stage, no people, no swimmers, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_orion_chorus",
        "output_subpath": "world_6/source_orion_chorus",
        "prompt": (
            "A first-person view looking down into the oceanic tide-pool where dozens of submerged bioluminescent lamps and ancient sunken bronze relics glow softly beneath the crystal-clear water. "
            "The warm golden and teal underwater lights illuminate ancient hand-carved stone steps and weathered bronze diving bells resting peacefully on the seabed. "
            "Bioluminescent jellyfish-like light motes float gently toward the surface. "
            "Bold graphic anime sci-fi scenery, rich aquatic hues, thick dark outlines, comforting spiritual resonance, "
            "with an open rocky shore in the foreground. "
            "Empty room stage, no people, no skeletons, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_orion_nightmare_node_scale_01",
        "output_subpath": "world_6/source_orion_nightmare_node_scale_01",
        "prompt": (
            "A first-person view along a wide, desolate black tide shelf composed of wet basalt glass under an alien salt-white starfield. "
            "Resting on a solitary stone shelf in the midground is a heavy, weathered brass nautical chronometer encased in thick curved glass, its internal brass gears and dials glowing with soft amber warmth. "
            "The completely calm obsidian ocean stretches to the flat horizon under cold white star clusters. "
            "Chunky anime-comic sci-fi scenery, bold outlines, haunting quiet beauty, "
            "with an expansive dark rock foreground for UI cards. "
            "Empty room stage, no people, no characters, no readable clock numbers, no letters, no logos."
        )
    },
    {
        "id": "source_orion_nightmare_node_scale_02",
        "output_subpath": "world_6/source_orion_nightmare_node_scale_02",
        "prompt": (
            "A first-person view inside an ancient, weathered seaside astronomical observatory perched on a coastal sea cliff. "
            "A monumental brass and verdigris telescope points out through a massive domed aperture toward the ocean and the starry night sky. "
            "Reflected in the massive glass lens of the telescope are dancing bioluminescent blue and green light nodes originating from deep underwater. "
            "Stylized comic-cel anime sci-fi art, chunky brass mechanisms, thick outlines, nostalgic cosmic atmosphere, "
            "with a clear stone flagged floor in the foreground. "
            "Empty room stage, no people, no astronomers, no characters, no readable chart text, no letters, no logos."
        )
    },
    {
        "id": "source_orion_nightmare_node_scale_03",
        "output_subpath": "world_6/source_orion_nightmare_node_scale_03",
        "prompt": (
            "A first-person view facing the monumental Chorus Door half-submerged at the base of a sea cliff. "
            "A colossal circular archway of carved jade-stone and sea-polished bronze, inscribed with abstract fluid wave reliefs. "
            "Radiant aqua and emerald harmonic light spills from the gap beneath the water, casting rippling caustics across the vaulted cavern ceiling. "
            "Bold anime comic adventure environment, thick dark outlines, majestic mythical atmosphere, "
            "with a solid wet pebble beach in the foreground. "
            "Empty room stage, no people, no characters, no readable runes, no letters, no logos."
        )
    },

    # --- Spire of Thought / Vale's Defense (6 rooms) ---
    {
        "id": "source_spire_archive",
        "output_subpath": "world_6/source_spire_archive",
        "prompt": (
            "A first-person view into a dizzying conceptual archive where towering curved crystalline bookshelves stretch endlessly into a purple cosmic void. "
            "Floating translucent scrolls and glowing holographic pages hover and turn gently in zero gravity without any wind. "
            "Looming dramatically in the background, suspended among the archive stacks, is a colossal scorched spaceship hull fragment glowing with dying orange ember veins. "
            "Epic anime adventure sci-fi art, bold outlines, vivid contrast of purple starlight and amber fire, "
            "with a solid glass bridge platform in the foreground. "
            "Empty room stage, no people, no characters, no readable text on pages, no letters, no logos."
        )
    },
    {
        "id": "source_spire_arena",
        "output_subpath": "world_6/source_spire_arena",
        "prompt": (
            "A first-person view entering a stark, hard-light gladiatorial memory defense arena inside the Source. "
            "The circular arena floor is made of interlocking black obsidian hexagons illuminated by harsh crimson and electric white perimeter lines. "
            "At the far end of the arena stands a monumental empty armor bracket cradle made of jagged black carbon composite. "
            "Severe comic-cel anime sci-fi aesthetic, sharp angular geometry, thick dark contours, intense dramatic tension, "
            "with an expansive hex-paved arena floor in the foreground. "
            "Empty room stage, no people, no warriors, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_spire_thought_node_scale_01",
        "output_subpath": "world_6/source_spire_thought_node_scale_01",
        "prompt": (
            "A first-person view inside the Revision Gallery where massive floating gilded frames display idealized utopian holographic vistas of familiar worlds. "
            "One frame shows clean gleaming mines, another lush manicured canopy gardens, and another radiant sunlit golden spires. "
            "In the center of the gallery rests an empty glowing glass thought pedestal projecting a complex holographic city blueprint with thick abstract outlines. "
            "Chunky anime sci-fi scenery, thick dark outlines, elegant yet false museum perfection, "
            "with a wide polished white marble promenade in the foreground. "
            "Empty room stage, no people, no characters in or out of frames, no readable blueprint text, no letters, no logos."
        )
    },
    {
        "id": "source_spire_thought_node_scale_02",
        "output_subpath": "world_6/source_spire_thought_node_scale_02",
        "prompt": (
            "A first-person view of a catastrophic reality fracture splitting a pristine white technological cathedral down the middle. "
            "A giant jagged fissure zigzags through the white marble walls and vaulted ceiling, bursting with wild explosions of saturated comic-book colors: magenta, sunshine yellow, lime green, and electric blue. "
            "Abstract geometric sound-burst shapes and dynamic celebration confetti particles radiate from the fracture, breaking the sterile silence. "
            "Dynamic comic-book anime adventure art, bold high-energy composition, thick ink outlines, "
            "with a sturdy cracked stone floor in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "source_spire_thought_node_scale_03",
        "output_subpath": "world_6/source_spire_thought_node_scale_03",
        "prompt": (
            "A first-person view of a colossal modular barrier wall assembled from interlocking layers of Warden heavy armor plating, industrial steel beams, and translucent amber hard-light shields. "
            "The defensive barricade bristles with chunky technological conduits, warning beacons, and power coils, yet gaps in the armor reveal warm human light leaking through. "
            "Bold graphic anime sci-fi aesthetic, heavy mechanical silhouettes, thick dark outlines, "
            "with a clear reinforced iron deck in the foreground. "
            "Empty room stage, no people, no guards, no characters, no readable stencils, no letters, no logos."
        )
    },
    {
        "id": "source_spire_thought_node_scale_04",
        "output_subpath": "world_6/source_spire_thought_node_scale_04",
        "prompt": (
            "A first-person view reaching the apex of the thought spiral where a grand dais stands completely open and crownless. "
            "Instead of a monolithic throne, the circular white marble platform features a welcoming circular council hearth ringed with glowing gold and cyan floor conduits. "
            "Above the dais, a panoramic 360-degree skylight reveals a breathtaking cosmic sunrise bursting across the universe. "
            "Majestic anime adventure environment, bold comic outlines, triumphant luminous palette, "
            "with an open polished circular dais floor in the foreground. "
            "Empty room stage, no people, no king on a throne, no characters, no readable text, no letters, no logos."
        )
    },

    # --- Zeke's Nightmare / Corporate Absurdity (5 rooms) ---
    {
        "id": "source_zeke_review_loop",
        "output_subpath": "world_6/source_zeke_review_loop",
        "prompt": (
            "A first-person view down an impossible, infinitely repeating corporate conference room in an uncanny twilight skyscraper. "
            "A massive polished mahogany conference table stretches into infinity under harsh overhead fluorescent grid troffers, flanked by endless rows of identical high-backed ergonomic office chairs. "
            "Stacked on the table are towering stacks of blank manila file folders and unreadable holographic performance charts that cycle endless downward graphs. "
            "Satirical anime comic sci-fi adventure scenery, exaggerated surreal perspective, bold dark linework, "
            "with a clean commercial carpet floor in the foreground. "
            "Empty room stage, no people, no managers, no characters, no readable text on documents, no letters, no logos."
        )
    },
    {
        "id": "source_zeke_break_room",
        "output_subpath": "world_6/source_zeke_break_room",
        "prompt": (
            "A first-person view inside a depressing yet comical corporate break room with pale institutional green cinderblock walls. "
            "In the center hums an oversized retro-futuristic vending machine glowing with gaudy red and yellow lights, its display coils holding rolled-up paper resignation scrolls instead of food. "
            "Beside it stands a chunky vintage coffee maker with a half-full glass pot, and on the right wall a heavy metal exit door glows with a green emergency exit sign frame. "
            "Chunky anime-comic adventure art style, bold outlines, playful sci-fi satire, "
            "with a checkered linoleum tile floor in the foreground. "
            "Empty room stage, no people, no workers, no characters, no readable sign text, no letters, no logos."
        )
    },
    {
        "id": "source_zeke_nightmare_node_scale_01",
        "output_subpath": "world_6/source_zeke_nightmare_node_scale_01",
        "prompt": (
            "A first-person view through an endless, labyrinthine maze of drab grey fabric cubicle partition walls under flickering fluorescent lights. "
            "Mounted on the cubicle dividers at regular intervals are large, absurd round corporate smiley badges made of yellow plastic with exaggerated cartoon grins. "
            "One partition wall in the midground has cracked open, revealing a glimpse of warm blue sky and distant fluffy clouds beyond the office maze. "
            "Exaggerated anime-comic adventure environment, strong geometric perspective, thick dark outlines, "
            "with a clear grey office carpet walkway in the foreground. "
            "Empty room stage, no people, no employees, no characters, no readable badge text or numbers, no letters, no logos."
        )
    },
    {
        "id": "source_zeke_nightmare_node_scale_02",
        "output_subpath": "world_6/source_zeke_nightmare_node_scale_02",
        "prompt": (
            "A first-person view facing an absurd, colossal mountain altar constructed entirely from neatly stacked white reams of paper and corporate binders that reaches all the way to the acoustic ceiling tiles. "
            "Perched proudly atop the paper altar like a sacred religious idol is a solitary, oversized, gleaming bright candy-apple red manual desktop stapler. "
            "Dramatic golden volumetric god-rays from an unseen skylight illuminate the red stapler like a divine relic. "
            "Comic-cel anime adventure art, playful visual storytelling, bold saturated primary colors, thick ink contours, "
            "with an open office floor in the foreground. "
            "Empty room stage, no people, no characters, no readable text on papers, no letters, no logos."
        )
    },
    {
        "id": "source_zeke_nightmare_node_scale_03",
        "output_subpath": "world_6/source_zeke_nightmare_node_scale_03",
        "prompt": (
            "A first-person view of a massive floor-to-ceiling glass executive window at the end of a dark cubicle hallway. "
            "The huge window has been swung wide open, letting in a flood of glorious golden late-afternoon sunshine and fresh wind. "
            "Outside spreads a vast, panoramic view of a free, open sky with soaring birds and warm mountain ranges on the horizon. "
            "Triumphant anime adventure art, dramatic high-contrast lighting, bold linework, liberating atmosphere, "
            "with a wide open office floor in the foreground. "
            "Empty room stage, no people, no characters, no readable text on window frame, no letters, no logos."
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
    
    total = len(BATCH_13_ROOMS)
    print(f"Starting Batch 13: {total} rooms in World 6 (Memory Stair, New World, Orion Nightmare, Spire of Thought, Zeke Nightmare)...")
    
    completed_ids = []
    
    for idx, item in enumerate(BATCH_13_ROOMS, 1):
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
    print("\nWiring Batch 13 assets into app/src/main/assets/rooms.json...")
    for r_id, webp_rel_path in completed_ids:
        if r_id in rooms_by_id:
            rooms_by_id[r_id]["background_image"] = webp_rel_path
        else:
            print(f"WARNING: Room ID {r_id} not found in rooms.json!")
            
    with open(rooms_file, "w", encoding="utf-8") as f:
        json.dump(rooms_data, f, indent=2, ensure_ascii=False)
        f.write("\n")
        
    print(f"\nBatch 13 complete! Wired {len(completed_ids)} new rooms into rooms.json. World 6 is 100% complete!")

if __name__ == "__main__":
    process_batch()
