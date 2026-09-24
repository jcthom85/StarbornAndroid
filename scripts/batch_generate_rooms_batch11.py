#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 11 (Anchor Chamber, Throne Room, The Tear - World 5 Final).
Follows docs/story/Starborn_Art_Production_Guide.md:
- Model: gpt-image-2
- Quality: low
- Size: 1088x1920
- Format: Save source PNG, export shipping WebP (quality=90, method=6)
- Constraints: Ghost town rule, no hands/characters, no readable text/logos.
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

BATCH_11_ROOMS = [
    {
        "id": "deep_anchor_chamber_scale_01",
        "output_subpath": "world_5/deep_anchor_chamber_scale_01",
        "prompt": (
            "A first-person view entering the grand Cable Narthex before the central Anchor Chamber in the orbital Void ring. "
            "Monumental braided golden umbilical cables and translucent cyan conduit conduits run in solemn vaulted arches along white marble walls. "
            "Pulsing rhythm of amber, cyan, and violet resonance light washes across dark polished stone floor tiles. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, sacred technological majesty, "
            "with a wide clear processional walkway in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "deep_anchor_chamber_scale_02",
        "output_subpath": "world_5/deep_anchor_chamber_scale_02",
        "prompt": (
            "A first-person view of a secondary acoustic resonance tank chamber flanking the Anchor Chamber. "
            "A towering cylindrical containment vat of clear structural glass filled with radiant swirling cyan resonance mist. "
            "Surrounding acoustic resonator rings and brass tuning coils pulse softly under warm indirect cove lighting. "
            "Bold comic-cel sci-fi adventure environment, thick dark outlines, clean circular geometry, "
            "with uncluttered white marble flooring in the lower foreground. "
            "Empty room stage, no people, no characters inside or outside the tank, no readable meter text, no letters, no logos."
        )
    },
    {
        "id": "deep_anchor_chamber_scale_03",
        "output_subpath": "world_5/deep_anchor_chamber_scale_03",
        "prompt": (
            "A first-person view facing an intricate automated Anchor Loom spindle mechanism in the Void sanctuary. "
            "A delicate yet monumental circular apparatus of spinning golden magnetic filaments and rotating brass rings suspended from the vaulted ceiling. "
            "Vibrant cyan and gold light waves weave intricate cymatic patterns across dark reflective floor tiles. "
            "Stylized anime-comic sci-fi art, thick inked outlines, mesmerizing cosmic acoustic machinery, "
            "and clean negative space across the foreground. "
            "Empty room stage, no people, no characters, no loose items on the ground, no readable text, no letters, no logos."
        )
    },
    {
        "id": "deep_anchor_chamber_scale_04",
        "output_subpath": "world_5/deep_anchor_chamber_scale_04",
        "prompt": (
            "A first-person view inside a climate-controlled thaw gallery adjoining the cryogenic anchor systems. "
            "Thermal radiator panels with warm glowing amber coils line white composite corridor walls, melting delicate ice crystals into drops of water. "
            "A manual emergency release handrail runs along the side beneath soft cyan ceiling fixtures. "
            "Chunky anime-comic sci-fi style, thick dark outlines, clean mechanical forms, "
            "with clear diamond-plate flooring in the lower third for mobile UI. "
            "Empty room stage, no people, no characters, no hands on rails, no readable stencils, no letters, no logos."
        )
    },
    {
        "id": "deep_anchor_chamber_scale_05",
        "output_subpath": "world_5/deep_anchor_chamber_scale_05",
        "prompt": (
            "A first-person view standing at the Living Cable Threshold crossing into the core anchor sanctum. "
            "Thick organic-looking acoustic transmission cables embedded beneath transparent floor tiles, glowing with pulsing turquoise and violet light veins. "
            "White curved architectural ribs and gold-inlaid archways frame the entrance to the inner chamber. "
            "Bold comic-cel sci-fi adventure look, thick dark outlines, striking energy underfloor lighting, "
            "with a solid threshold platform in the foreground. "
            "Empty room stage, no people, no characters, no readable markings, no letters, no logos."
        )
    },
    {
        "id": "deep_anchor_chamber_scale_06",
        "output_subpath": "world_5/deep_anchor_chamber_scale_06",
        "prompt": (
            "A first-person view facing an empty relic cradle pedestal radiating an ethereal resonance afterimage. "
            "A sleek stepped dais of white marble and brass insets holding an empty glowing harmonic bracket. "
            "Faint cyan and magenta acoustic interference rings ripple through the air where the stolen relic once rested. "
            "Stylized anime-comic sci-fi adventure aesthetic, thick dark outlines, haunting ceremonial void, "
            "and clean marble pavement across the lower foreground. "
            "Empty room stage, no people, no characters, no loose loot, no readable inscriptions, no letters, no logos."
        )
    },
    {
        "id": "deep_anchor_chamber_scale_07",
        "output_subpath": "world_5/deep_anchor_chamber_scale_07",
        "prompt": (
            "A first-person view facing the monumental Mercy Wall in the orbital ring sanctuary. "
            "A massive curved white monolith wall adorned with abstract horizontal light strips and non-readable geometric illuminated bars in gentle ivory light. "
            "A tranquil reflecting pool with still black water mirrors the softly lit architecture under vaulted arches. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, serene minimalist grandeur, "
            "with clear white stone pavers in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no readable words or sentences, no letters, no logos."
        )
    },
    {
        "id": "deep_anchor_chamber_scale_08",
        "output_subpath": "world_5/deep_anchor_chamber_scale_08",
        "prompt": (
            "A first-person view of an emergency manual release plinth concealed under ceremonial glass beside the anchor chamber. "
            "A faceted glass case housing an unpowered oversized manual bypass key lever made of polished brass and titanium. "
            "Soft amber status rings and cyan edge-lighting illuminate clean dark floor tiling. "
            "Bold comic-cel sci-fi art, thick inked outlines, clean functional focal point, "
            "with open floor space in the lower half of the composition. "
            "Empty room stage, no people, no operators, no hands, no readable emergency labels, no letters, no logos."
        )
    },
    {
        "id": "deep_throne_room_scale_01",
        "output_subpath": "world_5/deep_throne_room_scale_01",
        "prompt": (
            "A first-person view inside an imposing observation apse overlooking the crumbling planetary energy shield from the throne room. "
            "Vast curved panoramic viewports show the planet below scarred by web-like purple and cyan fracture lines across its atmosphere. "
            "Semi-circular tiers of sleek white executive spectator seating face the cosmic spectacle under soft golden ambient downlights. "
            "Stylized anime-comic sci-fi adventure environment, thick dark outlines, dramatic apocalyptic scale, "
            "with clear marble flooring in the foreground for mobile UI. "
            "Empty room stage, no people, no executives, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "deep_throne_room_scale_02",
        "output_subpath": "world_5/deep_throne_room_scale_02",
        "prompt": (
            "A first-person view facing an isolated high-security stasis holding cell adjoining the Void throne room. "
            "A cylindrical force-field containment cage glowing with intense cyan hard-light beams between polished titanium emitter rings. "
            "Dark obsidian floor tiles with red status traces lead up to the empty containment platform. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, stark technological prison atmosphere, "
            "and uncluttered floor space in the lower third. "
            "Empty room stage, no people, no prisoners, no characters, no readable terminal screens, no letters, no logos."
        )
    },
    {
        "id": "deep_throne_room_scale_03",
        "output_subpath": "world_5/deep_throne_room_scale_03",
        "prompt": (
            "A first-person view of an elevated soloist conductor dais perched at the front of the throne hall. "
            "A circular white marble platform bathed in a vertical column of pure white spotlighting from above. "
            "Floating abstract geometric harmonic tuning rods pulse with soft magenta and gold light around the podium. "
            "Bold comic-cel sci-fi adventure art, thick dark outlines, dramatic solitary stage presence, "
            "with clean reflective dark floor panels in the foreground. "
            "Empty room stage, no people, no conductor, no characters, no readable sheet music, no letters, no logos."
        )
    },
    {
        "id": "deep_throne_room_scale_04",
        "output_subpath": "world_5/deep_throne_room_scale_04",
        "prompt": (
            "A first-person view inside a desolate boardroom overlooking the Void abyss, known as the Boardroom Grave. "
            "A colossal oval black obsidian conference table surrounded by empty high-backed minimalist executive chairs. "
            "A dormant blank holographic table projector emits a faint cyan grid across the polished dark wood and stone floor. "
            "Stylized anime-comic sci-fi style, thick inked outlines, chilling corporate stillness, "
            "with wide clear flooring in the lower foreground for mobile UI. "
            "Empty room stage, no people, no characters, no loose agendas or papers, no readable text, no letters, no logos."
        )
    },
    {
        "id": "deep_throne_room_scale_05",
        "output_subpath": "world_5/deep_throne_room_scale_05",
        "prompt": (
            "A first-person view stepping out onto a dramatic cantilevered coup balcony projecting over the station core. "
            "Polished white titanium parapets and gold railings look out over the glowing multi-colored energy conduits of the orbital command crown. "
            "Below in the abyss, streams of purple and cyan data lights pulse violently through massive structural rings. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, vertiginous monumental architecture, "
            "with a clean terrace deck in the lower foreground. "
            "Empty room stage, no people, no characters, no readable consoles, no letters, no logos."
        )
    },
    {
        "id": "deep_throne_room_scale_06",
        "output_subpath": "world_5/deep_throne_room_scale_06",
        "prompt": (
            "A first-person view facing the grand Avatar Gate portal at the climax of the throne sanctum. "
            "A massive circular portal frame of white composite and gold trim projecting a radiant concentric halo of rotating white and cyan hard-light rings. "
            "The portal opens into a shimmering cosmic gradient of deep space and acoustic ripples. "
            "Bold comic-cel sci-fi adventure environment, thick dark outlines, climactic boss arena grandeur, "
            "with a wide clear marble processional runway in the foreground for mobile UI. "
            "Empty room stage, no boss, no enemies, no characters, no readable runes or text, no letters, no logos."
        )
    },
    {
        "id": "deep_throne_room_scale_07",
        "output_subpath": "world_5/deep_throne_room_scale_07",
        "prompt": (
            "A first-person view walking along the final Tear Runway extending toward the dimensional rift beyond the throne. "
            "A narrow cantilevered glass walkway with brass edge lighting reaching out into deep space. "
            "Ahead, the fabric of space itself shimmers with iridescent magenta, cyan, and gold cosmic aurora waves. "
            "Stylized anime-comic sci-fi art, thick dark outlines, transcendent climactic threshold, "
            "with an open glass walkway leading forward across the lower third. "
            "Empty room stage, no people, no characters, no debris, no readable text, no letters, no logos."
        )
    },
    {
        "id": "deep_tear_scale_01",
        "output_subpath": "world_5/deep_tear_scale_01",
        "prompt": (
            "A first-person view of a monumental gravity suture beam attempting to seal the dimensional Tear in the Void. "
            "Colossal mechanical pylon clamps anchored to shattered ring bulkheads emit intense, twisting beams of dark violet and black gravitational energy. "
            "Distorted space warps the background starfield into curving gravitational lensing rings. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, mind-bending cosmic physics, "
            "with a sturdy shattered deck landing in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no floating bodies, no readable text, no letters, no logos."
        )
    },
    {
        "id": "deep_tear_scale_02",
        "output_subpath": "world_5/deep_tear_scale_02",
        "prompt": (
            "A first-person view standing before the Choir Gap where the hull has dissolved into pure acoustic resonance. "
            "A gaping breach in the white titanium superstructure opens onto a celestial ocean of shimmering crystalline waves and spectral aurora curtains in cyan, amber, and indigo. "
            "Floating geometric fragments of shattered archways drift peacefully in zero gravity. "
            "Bold comic-cel sci-fi adventure environment, thick dark outlines, poetic cosmic wonder, "
            "with clean deck plating in the lower foreground. "
            "Empty room stage, no people, no characters, no creatures, no readable text, no letters, no logos."
        )
    },
    {
        "id": "deep_tear_scale_03",
        "output_subpath": "world_5/deep_tear_scale_03",
        "prompt": (
            "A first-person view along a violently twisted structural ring shear section of the orbital station. "
            "Massive white and gold curved deck sections have folded and sheered at impossible angles, revealing glowing internal power grids and cosmic voids. "
            "Vibrant cyan electrical arcs dance between severed structural beams under deep space starlight. "
            "Stylized anime-comic sci-fi adventure art, thick dark outlines, dramatic structural catastrophe, "
            "with a clear fractured deck path leading into the scene. "
            "Empty room stage, no people, no characters, no loose items on the ground, no readable warning signs, no letters, no logos."
        )
    },
    {
        "id": "deep_tear_scale_04",
        "output_subpath": "world_5/deep_tear_scale_04",
        "prompt": (
            "A first-person view standing directly at the Event Lip of the dimensional Tear. "
            "The edge of the shattered station deck glows with blinding cerulean blue Cherenkov radiation as it borders a swirling cosmic vortex. "
            "Far across the rift, a warm golden beacon campfire glow flickers like an impossible shore in the dark. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, breathtaking boundary between realities, "
            "with solid deck flooring across the lower third for mobile UI. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "deep_tear_scale_05",
        "output_subpath": "world_5/deep_tear_scale_05",
        "prompt": (
            "A first-person view facing the final sealed emergency blast bulkhead holding back the vacuum and dimensional rift. "
            "A massive circular alloy blast door with an oversized manual dogging wheel, hydraulic locking wedges, and glowing red seal seams. "
            "Dense clouds of venting atmosphere mist swirl across fractured diamond-plate steel deck flooring. "
            "Bold comic-cel sci-fi art, thick inked outlines, intense final-stand engineering tension, "
            "with clear open floor space in the foreground. "
            "Empty room stage, no people, no characters, no hands on the wheel, no readable gauge numbers, no letters, no logos."
        )
    },
    {
        "id": "deep_tear_scale_06",
        "output_subpath": "world_5/deep_tear_scale_06",
        "prompt": (
            "A first-person view peering off the edge of the final precipice known as the Source Drop into the infinite cosmic sea. "
            "The fractured white hull terminates abruptly into a bottomless celestial expanse of cascading spectral waterfalls of light and swirling nebulas in cyan, magenta, and gold. "
            "Floating shattered archways and glowing dust motes drift toward a distant warm luminous horizon. "
            "Stylized anime-comic sci-fi adventure aesthetic, thick dark outlines, sublime cosmic destination, "
            "with a wide sturdy edge platform in the foreground for mobile UI. "
            "Empty room stage, no people, no characters falling, no loose debris, no readable text, no letters, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_11_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_11_ROOMS)}] Generating {room_id}...")
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

    # Wire all completed Batch 11 rooms into rooms.json immediately
    rooms_file = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "rooms.json"
    data = json.load(rooms_file.open("r", encoding="utf-8"))
    updated_count = 0
    for room in data:
        r_id = room.get("id")
        if not r_id:
            continue
        custom_webp = f"images/rooms/world_5/{r_id}.webp"
        if (PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / custom_webp).exists():
            if room.get("background_image") != custom_webp:
                room["background_image"] = custom_webp
                updated_count += 1
                
    rooms_file.write_text(json.dumps(data, indent=2), encoding="utf-8")
    print(f"\nBatch 11 complete! Wired {updated_count} new rooms into rooms.json. World 5 is 100% complete!")

if __name__ == "__main__":
    main()
