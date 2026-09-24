#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 4 (World 3 Final).
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

BATCH_4_ROOMS = [
    {
        "id": "spire_glasswalk",
        "output_subpath": "world_3/spire_glasswalk",
        "prompt": (
            "A first-person view along a breathtaking curved glass walkway cantilevered off the edge of the high Spire Skypark dome. "
            "A seamless structural glass floor reveals the sprawling vertical city lights and sea of clouds far below. "
            "Sleek brass and dark titanium handrails line the pathway beneath curved geodesic dome ribs. "
            "Clean anime-comic sci-fi adventure aesthetic, thick dark outlines, dramatic high-altitude vertigo, "
            "with a wide transparent floor walkway in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_donor_gallery",
        "output_subpath": "world_3/spire_donor_gallery",
        "prompt": (
            "A first-person view entering the opulent Donor Gallery corridor in the Upper Spire. "
            "Polished black marble and brass archways line the grand hall, flanking elegant backlit blank holographic silhouette frames and commemorative gold wall insets. "
            "Warm recessed spotlights and soft amber ambient lighting wash across polished diamond-pattern stone flooring. "
            "Bold comic-cel sci-fi art, thick dark outlines, luxurious corporate architecture, "
            "with uncluttered floor space in the lower half of the frame. "
            "Empty room stage, no people, no portraits with realistic faces, no readable names or plaques, no letters, no logos."
        )
    },
    {
        "id": "spire_skypark_scale_01",
        "output_subpath": "world_3/spire_skypark_scale_01",
        "prompt": (
            "A first-person view down an exotic hydroponic orchid greenhouse path inside the Spire Skypark dome. "
            "Lush engineered bioluminescent cyan and magenta orchids cascade from tiered brass vertical planters. "
            "Fine aromatic water mist drifts from copper nozzles beneath warm golden grow-lamps and white pebble walkways. "
            "Chunky anime-comic sci-fi adventure environment, thick dark outlines, vibrant floral and bronze colors, "
            "with a clean open garden path in the foreground. "
            "Empty room stage, no people, no characters, no gardeners, no readable signage, no letters, no logos."
        )
    },
    {
        "id": "spire_skypark_scale_02",
        "output_subpath": "world_3/spire_skypark_scale_02",
        "prompt": (
            "A first-person view of a floating circular registry pavilion in the center of the lush Skypark garden. "
            "An elegant white marble and gold rotunda with a central curved guest terminal plinth glowing with soft blank amber holographic light. "
            "Surrounding ornamental pruned bonsai trees and reflecting pools mirror the evening sky outside the dome. "
            "Stylized comic-cel sci-fi aesthetic, thick dark outlines, balanced harmonious composition, "
            "and clean marble terrace paving in the lower foreground. "
            "Empty room stage, no people, no characters, no readable text on terminals, no letters, no logos."
        )
    },
    {
        "id": "spire_skypark_scale_03",
        "output_subpath": "world_3/spire_skypark_scale_03",
        "prompt": (
            "A first-person view across an artificial pristine lawn of crystalline synthetic turf laid over transparent structural glass in the Skypark. "
            "Manicured geometric hedges and sculptural chrome fountains stand under the vast curved glass dome. "
            "Through the transparent grass panels, the hazy neon lights of the lower city glimmer hundreds of stories beneath. "
            "Clean anime-comic sci-fi adventure art, thick painted outlines, surreal luxurious scale, "
            "with spacious clear lawn in the foreground. "
            "Empty room stage, no people, no characters, no animals, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "spire_sewers_scale_01",
        "output_subpath": "world_3/spire_sewers_scale_01",
        "prompt": (
            "A first-person view into a colossal underground pump gallery beneath the foundation of the Spire. "
            "Massive piston-driven hydraulic pump cylinders and giant rusted iron flywheel valves rise from dark drainage channels. "
            "Amber warning beacons and pale turquoise runoff water shimmer across riveted iron catwalks and mossy masonry walls. "
            "Chunky anime-comic sci-fi adventure environment, thick dark outlines, heavy industrial scale, "
            "and a clear metal grated walkway in the lower foreground. "
            "Empty room stage, no people, no workers, no creatures, no readable pressure gauges, no letters, no logos."
        )
    },
    {
        "id": "spire_sewers_scale_02",
        "output_subpath": "world_3/spire_sewers_scale_02",
        "prompt": (
            "A first-person view of a heavy debris filter sluice in the subterranean storm canal under the Spire. "
            "Thick iron filter grates trap floating mechanical scraps and industrial runoff from rushing dark water. "
            "A wet service catwalk with safety handrails runs beside the canal under dull yellow utility spotlights and concrete arches. "
            "Bold comic-cel sci-fi art, thick inked outlines, murky teal and rusted iron palette, "
            "with clean concrete decking in the foreground. "
            "Empty room stage, no people, no creatures, no readable badges, no letters, no logos."
        )
    },
    {
        "id": "spire_sewers_scale_03",
        "output_subpath": "world_3/spire_sewers_scale_03",
        "prompt": (
            "A first-person view into a secluded dry ratline nook branching off the main sewer tunnel of the Spire. "
            "A raised dry stone ledge with makeshift wooden bench crates, an unlit oil drum brazier, and bundles of insulated cables. "
            "Dim warm lantern glow illuminates rough arched brickwork and water runoff gutters. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, cozy outlaw hideout atmosphere, "
            "and clear dry stone ledge in the lower foreground. "
            "Empty room stage, no people, no characters, no readable graffiti, no letters, no logos."
        )
    },
    {
        "id": "spire_runoff_lock",
        "output_subpath": "world_3/spire_runoff_lock",
        "prompt": (
            "A first-person view facing a massive sealed hydraulic runoff floodgate in the sewer depths beneath the Spire. "
            "A heavy reinforced steel sluice door with hydraulic locking pistons and yellow hazard striping holds back churning murky water. "
            "An enclosed vertical ladder cage climbs up the damp concrete wall into swirling steam under red emergency lamps. "
            "Stylized comic-cel sci-fi adventure look, thick dark outlines, heavy mechanical tension, "
            "with a dry iron landing platform in the foreground. "
            "Empty room stage, no people, no characters, no readable stencils, no letters, no logos."
        )
    },
    {
        "id": "spire_maintenance_sluice",
        "output_subpath": "world_3/spire_maintenance_sluice",
        "prompt": (
            "A first-person view looking down a narrow, steamy maintenance sluice corridor running above the Spire drainage network. "
            "Overhead hot steam release valves and rows of heavy copper conduits flank a narrow metal grating gangway. "
            "A blank electrical control terminal with flickering amber indicator LEDs is mounted to the dripping concrete wall. "
            "Clean anime-comic sci-fi adventure environment, thick dark outlines, chunky pipe fittings, "
            "and clear catwalk grating in the lower foreground. "
            "Empty room stage, no people, no characters, no hands, no readable schematics, no letters, no logos."
        )
    },
    {
        "id": "spire_old_subway_car",
        "output_subpath": "world_3/spire_old_subway_car",
        "prompt": (
            "A first-person view inside a converted decommissioned vintage subway car retrofitted as an annex lounge beside The Static bar in the Spire. "
            "Worn teal vinyl passenger booth seats, curved stainless steel handrail poles, and a colorful non-functioning jukebox in the back. "
            "Warm amber mood lighting and soft neon exterior glow stream through rain-streaked windows. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, nostalgic retro-futuristic charm, "
            "with a clear center aisle floor in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no trash, no readable stickers or posters, no letters, no logos."
        )
    },
    {
        "id": "spire_signal_stairwell",
        "output_subpath": "world_3/spire_signal_stairwell",
        "prompt": (
            "A first-person view up a utilitarian concrete stairwell behind The Static bar, crowded with pirate radio equipment. "
            "Spools of coaxial cable, amateur antenna masts, and a humming analog frequency monitoring terminal with glowing oscilloscope dials. "
            "Muted yellow stairwell lighting and flickering blue vacuum tube displays illuminate the raw industrial walls. "
            "Bold comic-cel sci-fi adventure style, thick inked outlines, scrappy pirate tech aesthetic, "
            "with clear concrete landing steps in the foreground. "
            "Empty room stage, no people, no characters, no hands, no readable dials or text, no letters, no logos."
        )
    },
    {
        "id": "spire_the_static_scale_01",
        "output_subpath": "world_3/spire_the_static_scale_01",
        "prompt": (
            "A first-person view behind the rustic wooden service counter of The Static dive bar in the Spire. "
            "Shelves of glass bottles with glowing amber and violet synth-liquors, a brass draught tap, and vintage acoustic audio meters. "
            "Warm golden under-shelf lighting and dark polished wood surfaces reflect ambient neon light from the barroom beyond. "
            "Stylized anime-comic sci-fi adventure look, thick dark outlines, atmospheric rebel sanctuary vibe, "
            "with clean bar countertop and floor in the lower foreground. "
            "Empty room stage, no people, no bartender, no loose customer items, no readable bottle labels, no letters, no logos."
        )
    },
    {
        "id": "spire_the_static_scale_02",
        "output_subpath": "world_3/spire_the_static_scale_02",
        "prompt": (
            "A first-person view on a weathered exterior concrete fire-escape stairwell ascending toward the roof behind The Static bar. "
            "Riveted iron fire-escape steps and landings cling to the outside brick wall of an old Spire tenement. "
            "Distant glowing neon billboards and hovering transit lights cast vivid cyan and magenta reflections across damp pavement and railings. "
            "Chunky comic-cel sci-fi art, thick dark outlines, urban alley atmosphere, "
            "and clean iron landing in the lower third for mobile UI. "
            "Empty room stage, no people, no characters, no readable graffiti, no letters, no logos."
        )
    },
    {
        "id": "spire_the_static_scale_03",
        "output_subpath": "world_3/spire_the_static_scale_03",
        "prompt": (
            "A first-person view of an emotional tenant memorial shrine tucked into a quiet brick nook near The Static bar. "
            "A rustic wooden plinth adorned with dozens of glowing warm beeswax candles and weathered paper prayer lanterns. "
            "Old defunct unlit neon tubing forms an artistic archway against the dark alley wall under gentle amber and violet illumination. "
            "Bold anime-comic sci-fi adventure environment, thick dark outlines, poignant community warmth, "
            "with clear stone pavement in the foreground. "
            "Empty room stage, no people, no characters, no portraits with realistic faces, no readable text on notices, no letters, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_4_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_4_ROOMS)}] Generating {room_id}...")
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

    print("\nBatch 4 completed successfully! World 3 is 100% complete!")

if __name__ == "__main__":
    main()
