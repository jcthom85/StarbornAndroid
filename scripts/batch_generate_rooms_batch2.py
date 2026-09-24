#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 2.
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

BATCH_2_ROOMS = [
    {
        "id": "spire_landing_pad_scale_01",
        "output_subpath": "world_3/spire_landing_pad_scale_01",
        "prompt": (
            "A first-person view onto an exposed metallic fuel bridge gantry high on the storm-swept roof of the Spire. "
            "Heavy industrial braided fueling conduits, lock-couplings, and yellow hazard railings line the wet grated metal catwalk. "
            "Sideways wind and rain streak through glowing amber warning lights and deep cyan horizon clouds. "
            "Bold anime-comic sci-fi adventure environment, thick dark outlines, chunky industrial piping, "
            "and uncluttered wet catwalk flooring in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_landing_pad_scale_02",
        "output_subpath": "world_3/spire_landing_pad_scale_02",
        "prompt": (
            "A first-person view from a high-altitude rooftop overlook on the Spire, framing the Shield Sightline. "
            "Reinforced steel parapets and sensor pylons look out into swirling dark storm clouds where a translucent cyan harmonic energy shield ripples overhead. "
            "Angular floodlight pylons cast warm amber lighting across the damp non-slip deck plates. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, high-altitude scale, clear silhouette layers, "
            "and calm negative space across the foreground roof platform. "
            "Empty room stage, no people, no characters, no readable writing, no letters, no logos."
        )
    },
    {
        "id": "spire_landing_pad_scale_03",
        "output_subpath": "world_3/spire_landing_pad_scale_03",
        "prompt": (
            "A first-person view across a rain-slicked rooftop pad in the Spire with mechanical debris. "
            "Scattered burnt armor plating, twisted steel struts, and dented chassis parts of a heavy industrial drone are embedded against a reinforced barrier wall in the midground. "
            "Dull warning strobes cast flickering red and amber highlights across the dark wet asphalt and metal panels. "
            "Bold comic-cel sci-fi adventure style, thick inked outlines, chunky mechanical scrap shapes, "
            "and clean open foreground for mobile UI. "
            "Empty room stage, no people, no characters, no living beings, no readable text, no logos."
        )
    },
    {
        "id": "spire_landing_pad_scale_04",
        "output_subpath": "world_3/spire_landing_pad_scale_04",
        "prompt": (
            "A first-person view walking along an enclosed boarding causeway gantry stretching out toward a starship docking clamp high on the Spire. "
            "Ribbed structural steel arches with reinforced glass viewports line the walkway, under rhythmic amber emergency beacon lighting. "
            "Through the glass, the rainy evening sky and towering city skyscrapers glow with cyan and gold accents. "
            "Stylized anime-comic sci-fi environment, thick dark outlines, clear perspective down the corridor toward an airlock hatch, "
            "with clean deck plating in the lower foreground. "
            "Empty room stage, no people, no characters, no hands, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "spire_uniform_sorting",
        "output_subpath": "world_3/spire_uniform_sorting",
        "prompt": (
            "A first-person view entering an industrial uniform sorting department deep in the Spire's utility levels. "
            "Long ceiling-mounted conveyor tracks with suspended garment hangers and heavy rolling wire laundry carts fill the room. "
            "Industrial fluorescent strip lighting reflects on pale institutional linoleum tiles, accented by teal and brass plumbing conduits along concrete walls. "
            "Clean chunky anime-comic sci-fi style, bold outlines, organized geometric shapes, "
            "with wide clear floor space in the foreground. "
            "Empty room stage, no people, no characters, no clothing reading as people, no readable tags, no letters, no logos."
        )
    },
    {
        "id": "spire_service_lift",
        "output_subpath": "world_3/spire_service_lift",
        "prompt": (
            "A first-person view inside a heavy freight service lift elevator cage rising through the structural spine of the Spire. "
            "Sturdy corrugated metal walls, heavy overhead counterweight pulleys, and reinforced diamond-plate steel flooring. "
            "A utilitarian control pedestal with unreadable toggle switches and amber status indicators sits to one side. "
            "Bold comic-cel sci-fi adventure aesthetic, thick dark outlines, industrial yellow and charcoal grey palette, "
            "with clear open floor space in the lower half of the frame. "
            "Empty room stage, no people, no characters, no hands, no readable text, no numbers, no logos."
        )
    },
    {
        "id": "spire_staff_checkpoint",
        "output_subpath": "world_3/spire_staff_checkpoint",
        "prompt": (
            "A first-person view approaching a sterile corporate staff security checkpoint between the service corridors and executive suites of the Spire. "
            "Sleek white and chrome turnstile barriers and biometric sensor archways glow with calm cyan and amber status rings. "
            "Polished grey terrazzo floor and geometric wall panels lead toward a secure glass partitioned portal. "
            "Clean anime-comic sci-fi adventure art, thick dark outlines, sharp modern silhouettes, "
            "and generous unobstructed negative space across the foreground. "
            "Empty room stage, no people, no characters, no guards, no readable screens, no letters, no logos."
        )
    },
    {
        "id": "spire_laundry_scale_01",
        "output_subpath": "world_3/spire_laundry_scale_01",
        "prompt": (
            "A first-person view inside a high-capacity steam laundry chamber in the Spire. "
            "Large cylindrical stainless steel steam tanks, pressurized brass valves, and copper pipes vent wisps of white vapor into the air. "
            "Suspended steel drying racks flank the damp tiled walkway under warm utility spotlights. "
            "Chunky anime-comic sci-fi adventure style, thick inked outlines, clean brass and metallic forms, "
            "with a clear floor path in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no laundry reading as bodies, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_laundry_scale_02",
        "output_subpath": "world_3/spire_laundry_scale_02",
        "prompt": (
            "A first-person view of a garment press line in the Spire service facilities. "
            "Row of heavy pneumatic hydraulic press machines with polished steel heating plates and brass pressure gauges set along a wide concrete work floor. "
            "Exposed ductwork and electrical conduits curve across the ceiling under bright overhead work lamps. "
            "Bold cel-shaded comic sci-fi environment, thick dark outlines, chunky machinery, "
            "with spacious clear floor plating in the lower foreground. "
            "Empty room stage, no people, no characters, no hands, no readable text, no letters, no logos."
        )
    },
    {
        "id": "spire_laundry_scale_03",
        "output_subpath": "world_3/spire_laundry_scale_03",
        "prompt": (
            "A first-person view into a utility disposal bay with large industrial service bins and sorting receptacles in the Spire basement. "
            "Heavy galvanized metal recycling hoppers and wheeled storage containers sit beneath overhead pneumatic chute vents. "
            "Stained concrete floor with yellow painted boundary marks under muted amber wall lamps. "
            "Stylized anime-comic sci-fi adventure look, thick dark outlines, chunky geometric bins, "
            "and calm negative space in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no discarded passes with legible letters, no logos."
        )
    },
    {
        "id": "spire_safehouse_roof",
        "output_subpath": "world_3/spire_safehouse_roof",
        "prompt": (
            "A first-person view stepping onto a cramped, secluded rooftop safehouse perch high in the Spire. "
            "Weathered clotheslines, metal ventilation duct cowls, and makeshift corrugated scrap shelters line the weathered concrete gravel roof. "
            "Beyond the low parapet, glowing neon skyscrapers and towering corporate spires pierce through evening haze and cyan clouds. "
            "Rich comic-cel sci-fi adventure aesthetic, thick dark outlines, warm ambient window glow contrasted with cool night air, "
            "with clean open roof space in the lower foreground. "
            "Empty room stage, no people, no characters, no hanging clothes that look like bodies, no readable text, no letters, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_2_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_2_ROOMS)}] Generating {room_id}...")
        try:
            response = client.images.generate(
                model="gpt-image-2",
                prompt=item["prompt"],
                n=1,
                size="1088x1920",
                quality="low"
            )
            
            # Decode image
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
                
            # Convert to WebP (quality=90, method=6)
            im = Image.open(png_path)
            im.save(webp_path, "WEBP", quality=90, method=6)
            print(f"  -> Generated PNG: {png_path.name}")
            print(f"  -> Encoded WebP: {webp_path.name} ({webp_path.stat().st_size // 1024} KB)")
            
        except Exception as e:
            print(f"Error generating {room_id}: {e}", file=sys.stderr)
            sys.exit(1)
            
        time.sleep(2)

    print("\nBatch 2 completed successfully!")

if __name__ == "__main__":
    main()
