#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 3 (Spire Lower City).
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

BATCH_3_ROOMS = [
    {
        "id": "spire_noodle_row",
        "output_subpath": "world_3/spire_noodle_row",
        "prompt": (
            "A first-person view looking down Noodle Row in the rainy Lower Spire night market. "
            "Narrow alley flanked by humble street vendor stalls with striped vinyl tarps, steaming broth pots, and hanging paper lanterns. "
            "Neon signage glows in teal, warm red, and amber through gentle rain mist, reflecting on dark wet asphalt. "
            "Stylized anime-comic sci-fi adventure environment, thick dark outlines, chunky stall geometry, "
            "and clean open foreground alley floor for mobile UI. "
            "Empty room stage, no people, no characters, no bowls of food on counters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_lantern_bridge",
        "output_subpath": "world_3/spire_lantern_bridge",
        "prompt": (
            "A first-person view crossing a rustic pedestrian footbridge in the Lower Spire above a drainage canal. "
            "Weathered steel bridge with railings strung with colorful glowing red and gold paper lanterns and decorative fluttering ribbon charms. "
            "Heavy industrial power cables and neon billings glow in cyan and amber along the distant building facades through evening fog. "
            "Bold comic-cel sci-fi aesthetic, thick dark outlines, clear bridge perspective, "
            "with clean open wooden plank and steel decking in the lower foreground. "
            "Empty room stage, no people, no characters, no readable writing, no letters, no logos."
        )
    },
    {
        "id": "spire_backstreet_clinic",
        "output_subpath": "world_3/spire_backstreet_clinic",
        "prompt": (
            "A first-person view into a covert backstreet medical clinic tucked into an alley in the Lower Spire. "
            "A utilitarian examination bed with hanging surgical lamps, clean medical supply carts, and drawn teal privacy curtains. "
            "Soft amber battery lanterns and quiet cyan equipment status lights illuminate the modest concrete room. "
            "Chunky anime-comic sci-fi style, thick dark outlines, organized clinical shapes, "
            "with clear open floor in the lower half of the composition. "
            "Empty room stage, no people, no patients, no doctor, no loose lootable items, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_night_market_scale_01",
        "output_subpath": "world_3/spire_night_market_scale_01",
        "prompt": (
            "A first-person view looking across a low market rooftop terrace strung with dense festival lights in the Lower Spire. "
            "Criss-crossing strings of glowing amber and turquoise bulbs hang under corrugated tin overhangs. "
            "Rooftop ventilation cowls, water tanks, and metal storage chests under warm atmospheric lighting. "
            "Bold anime-comic sci-fi adventure look, thick painted outlines, vibrant night colors, "
            "with a clean paved roof terrace in the foreground. "
            "Empty room stage, no people, no characters, no hands, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "spire_night_market_scale_02",
        "output_subpath": "world_3/spire_night_market_scale_02",
        "prompt": (
            "A first-person view into a crowded sensory alleyway known as Spice Alley in the Lower Spire. "
            "Market stalls with closed wooden roll-down shutters, hanging bundles of dried aromatic roots, copper distillation tubes, and brass scales. "
            "Deep magenta and warm golden lantern glow cuts through thin aromatic steam over cobblestone pavement. "
            "Stylized comic-cel sci-fi adventure environment, thick dark outlines, rich warm palette, "
            "and unobstructed floor in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no readable labels, no letters, no logos."
        )
    },
    {
        "id": "spire_night_market_scale_03",
        "output_subpath": "world_3/spire_night_market_scale_03",
        "prompt": (
            "A first-person view of a sheltered rainwater cache alcove beneath a torn green industrial awning in the Lower Spire. "
            "Large cylindrical water cistern barrels, metal overflow pipes, and stacked watertight equipment cases sit against rough brick walls. "
            "Soft teal reflection on wet stone flags under dim golden security wall sconces. "
            "Chunky anime-comic sci-fi style, thick inked outlines, clean silhouettes, "
            "and calm negative space across the wet stone floor. "
            "Empty room stage, no people, no characters, no loose loot, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_underrail_platform",
        "output_subpath": "world_3/spire_underrail_platform",
        "prompt": (
            "A first-person view onto an abandoned subterranean commuter train platform in the Underrail beneath the Spire. "
            "Heavy industrial steel track bed with rusted rails on the right, safety yellow tactile platform edge, and riveted iron support columns. "
            "Flickering fluorescent strip lights and distant cyan tunnel signals reflect off damp concrete platform tiles. "
            "Bold comic-cel sci-fi adventure environment, thick dark outlines, chunky architecture, "
            "with generous clear platform space in the foreground. "
            "Empty room stage, no people, no characters, no trains, no readable posters, no letters, no logos."
        )
    },
    {
        "id": "spire_checkpoint_queue",
        "output_subpath": "world_3/spire_checkpoint_queue",
        "prompt": (
            "A first-person view along a zigzagging security queuing lane in the Lower Spire transit hub. "
            "Heavy tubular chrome guide stanchions, retractable hazard-stripe belts, and overhead scanning sensors create a winding path. "
            "Overhead institutional white and cyan floodlights cast sharp graphic shadows across pale grey flooring. "
            "Clean anime-comic sci-fi style, thick dark outlines, geometric repetition, "
            "with an open walkway leading into the midground. "
            "Empty room stage, no people, no commuters, no guards, no readable screens, no letters, no logos."
        )
    },
    {
        "id": "spire_security_kiosk",
        "output_subpath": "world_3/spire_security_kiosk",
        "prompt": (
            "A first-person view facing an enclosed glass security observation kiosk nestled beneath a transit plaza concrete staircase in the Spire. "
            "Reinforced bullet-resistant tinted glass booth with blank amber diagnostic screens, metallic intercom grille, and service counter. "
            "Industrial grey concrete columns and tiled transit plaza floor illuminated by harsh fluorescent wall lights. "
            "Stylized comic-cel sci-fi adventure art, thick dark outlines, chunky solid structures, "
            "and clear foreground plaza floor for mobile UI. "
            "Empty room stage, no people, no characters, no hands, no readable text on monitors, no letters, no logos."
        )
    },
    {
        "id": "spire_elevator_service_gate",
        "output_subpath": "world_3/spire_elevator_service_gate",
        "prompt": (
            "A first-person view of a heavy industrial service gate set into the massive steel base of the Great Elevator in the Spire. "
            "A reinforced steel mesh roll-up gate flanked by hydraulic rams, yellow warning stripes, and thick armored power conduits. "
            "Dull amber utility lamps cast deep shadows against the colossal dark superstructure wall. "
            "Chunky anime-comic sci-fi environment, thick dark outlines, monumental scale, "
            "and uncluttered steel diamond-plate deck in the lower foreground. "
            "Empty room stage, no people, no workers, no vehicles, no readable text, no warning letters, no logos."
        )
    },
    {
        "id": "spire_transit_plaza_scale_01",
        "output_subpath": "world_3/spire_transit_plaza_scale_01",
        "prompt": (
            "A first-person view inside a cavernous commercial advertising concourse in the Spire Transit Plaza. "
            "Towering holographic display billboards showing colorful abstract cyan, magenta, and gold geometric light waves without readable words. "
            "Curved futuristic architectural balconies and polished composite flooring reflecting vibrant neon hues. "
            "Bold anime-comic sci-fi aesthetic, thick dark outlines, dynamic colorful lighting, "
            "with wide clear flooring in the lower third for mobile UI. "
            "Empty room stage, no people, no crowds, no readable writing, no corporate logos, no letters."
        )
    },
    {
        "id": "spire_transit_plaza_scale_02",
        "output_subpath": "world_3/spire_transit_plaza_scale_02",
        "prompt": (
            "A first-person view into a quiet shadowed recess behind a bank of automated fare ticketing terminals in the Transit Plaza. "
            "The backs of angular metallic ticket kiosks with exposed wiring conduits and ventilation grilles frame the left side. "
            "A dim alcove lit by subtle turquoise underglow contrasting with the bustling bright concourse in the background. "
            "Stylized comic-cel sci-fi adventure look, thick dark outlines, sharp architectural silhouettes, "
            "and clean shadowed floor space in the foreground. "
            "Empty room stage, no people, no characters, no tickets on floor, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_transit_plaza_scale_03",
        "output_subpath": "world_3/spire_transit_plaza_scale_03",
        "prompt": (
            "A first-person view inside an architectural transit coordination room in the Spire. "
            "Wall-mounted illuminated schematic boards showing abstract stylized color-coded transit line diagrams without readable words. "
            "A central drafting console and metal blueprint filing racks under clean industrial work lights. "
            "Chunky anime-comic sci-fi adventure aesthetic, thick dark outlines, teal and yellow accents, "
            "with unobstructed tiled flooring across the foreground. "
            "Empty room stage, no people, no characters, no readable text, no route station names, no logos."
        )
    },
    {
        "id": "spire_rain_gutter_alley",
        "output_subpath": "world_3/spire_rain_gutter_alley",
        "prompt": (
            "A first-person view down a narrow drenched service alleyway in the Lower Spire known as Rain Gutter Alley. "
            "Heavy copper downspouts and corrugated gutters cascade shimmering rainwater into iron drain gratings. "
            "A circular convex security mirror is mounted to an old brick wall beneath flickering amber streetlamps. "
            "Bold comic-cel sci-fi environment, thick dark outlines, rich blues and wet glistening reflections on dark cobblestones, "
            "with clean foreground negative space for mobile UI. "
            "Empty room stage, no people, no characters, no figures in mirror, no readable graffiti, no letters, no logos."
        )
    },
    {
        "id": "spire_vent_output_scale_01",
        "output_subpath": "world_3/spire_vent_output_scale_01",
        "prompt": (
            "A first-person view of a massive mechanical condensate exhaust pipe run outside the Spire lower levels. "
            "Huge ribbed metal pipes with insulated joints channel warm steam and dripping water along an exterior industrial gantry. "
            "Muted yellow warning lights and hazy teal fog create a dramatic atmospheric mood across the metal grated catwalk. "
            "Stylized anime-comic sci-fi adventure art, thick dark outlines, chunky pipe geometry, "
            "and clear metal grating path in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no stencils with letters, no logos."
        )
    },
    {
        "id": "spire_vent_output_scale_02",
        "output_subpath": "world_3/spire_vent_output_scale_02",
        "prompt": (
            "A first-person view inside a cramped maintenance crawl space between giant ventilation shafts in the Spire. "
            "Low ceiling crisscrossed with insulated coolant conduits, circular ductwork, and steel support beams. "
            "Faint amber emergency bulkhead lights cast long graphic shadows down the narrow passageway. "
            "Chunky comic-cel sci-fi environment, thick dark outlines, claustrophobic industrial feel, "
            "with clear diamond-plate flooring in the lower half of the frame. "
            "Empty room stage, no people, no characters, no hands, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_vent_output_scale_03",
        "output_subpath": "world_3/spire_vent_output_scale_03",
        "prompt": (
            "A first-person view standing before a heavy iron rain grate overlook peering down toward the city below the Spire. "
            "A sturdy industrial steel railing and stormwater drainage grates frame an opening overlooking distant glowing neon skyscrapers through mist. "
            "Water cascades through side chutes under bright cyan and amber ambient lights. "
            "Bold anime-comic sci-fi adventure aesthetic, thick dark outlines, dramatic vertical depth, "
            "with a clean steel deck platform in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_3_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_3_ROOMS)}] Generating {room_id}...")
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

    print("\nBatch 3 completed successfully!")

if __name__ == "__main__":
    main()
