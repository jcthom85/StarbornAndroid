#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 10A (Security Hub & Solarium).
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

BATCH_10A_ROOMS = [
    {
        "id": "orbital_surveillance_pit",
        "output_subpath": "world_5/orbital_surveillance_pit",
        "prompt": (
            "A first-person view looking down into a sunken surveillance control pit inside the orbital Void station. "
            "Tiers of curved metallic console desks facing an amphitheater wall of glowing blank crimson and cyan security monitors. "
            "Clusters of optical sensor lenses and camera housings embedded in dark acoustic wall panels. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, foreboding panopticon atmosphere, "
            "with a clean white terrazzo landing in the lower foreground. "
            "Empty room stage, no people, no operators, no hands, no readable text on screens, no letters, no logos."
        )
    },
    {
        "id": "orbital_security_hub_scale_01",
        "output_subpath": "world_5/orbital_security_hub_scale_01",
        "prompt": (
            "A first-person view into a utility maintenance alcove off the orbital security sector. "
            "White ceramic wall plating stripped back to reveal armored biometric wiring bundles, yellow fiber trunks, and circuit junctions. "
            "Cool cyan floor runners and warm amber utility lamps illuminate dark non-slip floor tiles. "
            "Bold comic-cel sci-fi adventure look, thick dark outlines, sharp contrast between luxury cladding and security machinery, "
            "with clear open floor space in the lower third. "
            "Empty room stage, no people, no guards, no tools on the ground, no readable markings, no letters, no logos."
        )
    },
    {
        "id": "orbital_security_hub_scale_02",
        "output_subpath": "world_5/orbital_security_hub_scale_02",
        "prompt": (
            "A first-person view down a reinforced vacuum pressure gallery encircling the orbital detention hub. "
            "Heavy circular titanium bulkheads with interlocking locking teeth and horizontal red warning indicator strips. "
            "Narrow armored viewports peer into the black starry void outside the station hull. "
            "Stylized anime-comic sci-fi adventure art, thick dark outlines, high-security mechanical tension, "
            "and clean composite deck plating in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no hands, no readable text, no letters, no logos."
        )
    },
    {
        "id": "orbital_security_hub_scale_03",
        "output_subpath": "world_5/orbital_security_hub_scale_03",
        "prompt": (
            "A first-person view from a hidden observation niche looking through a one-way mirrored viewport into the security concourse. "
            "A secluded steel observation post overlooking the stark white corridors and automated checkpoint gates. "
            "Soft amber status monitors and brass handrails contrast with cold fluorescent lighting beyond the glass. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, clandestine surveillance angle, "
            "with clear flooring in the lower half of the frame. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "orbital_security_hub_scale_04",
        "output_subpath": "world_5/orbital_security_hub_scale_04",
        "prompt": (
            "A first-person view of a concealed manual override control pocket in the security sector wall. "
            "A wall panel swings outward to expose heavy toggle levers, mechanical rotary breakers, and gold-plated bus bars. "
            "Pulsing cyan diagnostic lights illuminate the dark polished floor tiles. "
            "Bold comic-cel sci-fi environment, thick dark outlines, tactile mechanical details, "
            "and uncluttered foreground flooring. "
            "Empty room stage, no people, no operators, no hands, no readable labels, no letters, no logos."
        )
    },
    {
        "id": "orbital_security_hub_scale_05",
        "output_subpath": "world_5/orbital_security_hub_scale_05",
        "prompt": (
            "A first-person view into a covert emergency supply locker recess in the security sector. "
            "Recessed magnetic wall racks neatly holding spare biometric override keys, sealed emergency seals, and diagnostic scanners. "
            "Clean white composite walls with subtle amber LED accent lighting and dark grey deck plates. "
            "Stylized anime-comic sci-fi adventure style, thick dark outlines, neat modular storage silhouettes, "
            "with clear open floor in the foreground. "
            "Empty room stage, no people, no characters, no loose items, no readable notes, no letters, no logos."
        )
    },
    {
        "id": "orbital_security_hub_scale_06",
        "output_subpath": "world_5/orbital_security_hub_scale_06",
        "prompt": (
            "A first-person view along a quiet security resonance gallery known as the Echo Walk. "
            "Monolithic white sound-absorbing archways with gold trim and vertical crimson status lights flanking a long hallway. "
            "Highly reflective black obsidian floor panels reflect glowing cyan conduits embedded along the ceiling. "
            "Clean comic-cel sci-fi art, thick dark outlines, striking geometric symmetry, "
            "with a wide clear floor walkway in the lower third for mobile UI. "
            "Empty room stage, no people, no guards, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "orbital_security_hub_scale_final",
        "output_subpath": "world_5/orbital_security_hub_scale_final",
        "prompt": (
            "A first-person view inside the towering Threat Cathedral in the heart of orbital security. "
            "A monumental vaulted chamber where towering multi-story holographic projection arrays stack glowing crimson and cyan surveillance displays like stained glass. "
            "A central elevated control altar stands silent beneath sweeping gold and white structural arches. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, awe-inspiring technological majesty, "
            "with spacious clear white marble paving across the foreground. "
            "Empty room stage, no people, no operators, no characters, no readable screen text, no letters, no logos."
        )
    },
    {
        "id": "orbital_mirror_walk",
        "output_subpath": "world_5/orbital_mirror_walk",
        "prompt": (
            "A first-person view along a warm, humid maintenance walkway flanked by angled parabolic solar mirrors behind the Solarium. "
            "Massive polished brass and silver mirror panels redirecting intense golden sunlight through structural ceiling frames. "
            "Exposed copper irrigation pipes, condensation mist, and lush tropical vine tendrils peeking through maintenance grates. "
            "Bold comic-cel sci-fi adventure aesthetic, thick dark outlines, luminous golden and amber lighting, "
            "with clean concrete floor grating in the lower foreground. "
            "Empty room stage, no people, no gardeners, no hands, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "orbital_solarium_scale_01",
        "output_subpath": "world_5/orbital_solarium_scale_01",
        "prompt": (
            "A first-person view into an environmental maintenance alcove tucked behind the Solarium planters. "
            "Smooth white curved walls giving way to automated nutrient slurry pumps, brass irrigation manifolds, and water pressure valves. "
            "Warm solar sunlight streams through high glass clerestory windows across pale stone pavers. "
            "Stylized anime-comic sci-fi adventure look, thick dark outlines, blend of organic greenhouse and clean orbital tech, "
            "with clear open floor space in the lower third. "
            "Empty room stage, no people, no workers, no tools on the ground, no readable markings, no letters, no logos."
        )
    },
    {
        "id": "orbital_solarium_scale_02",
        "output_subpath": "world_5/orbital_solarium_scale_02",
        "prompt": (
            "A first-person view along a structural pressure gallery framing the exterior dome of the Solarium. "
            "Heavy curved white support struts and golden pressure seals bordering vast panoramic glass panes looking out into space. "
            "Cyan and amber warning strips embedded along the floor edge glow against deep interstellar darkness. "
            "Chunky anime-comic sci-fi style, thick inked outlines, grand orbital vistas, "
            "with an unobstructed walkway in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no hands, no readable gauge text, no letters, no logos."
        )
    },
    {
        "id": "orbital_solarium_scale_03",
        "output_subpath": "world_5/orbital_solarium_scale_03",
        "prompt": (
            "A first-person view from a tranquil overlook niche high above the synthetic beach of the Solarium. "
            "A curved golden terrace balcony looking across palm canopies, turquoise lagoons, and the giant orbital dome structure. "
            "Through the massive glass vault, the bright blue curve of the planet and white cloud formations fill the sky. "
            "Bold comic-cel sci-fi adventure environment, thick dark outlines, tropical luxury meets space station architecture, "
            "with clean terrace tiling in the lower foreground. "
            "Empty room stage, no people, no sunbathers, no furniture, no readable text, no letters, no logos."
        )
    },
    {
        "id": "orbital_solarium_scale_04",
        "output_subpath": "world_5/orbital_solarium_scale_04",
        "prompt": (
            "A first-person view facing an automated climate control pocket built into an ornamental marble grotto in the Solarium. "
            "Sleek brass decorative grilles concealing atmospheric moisture vaporizers, temperature dials, and blank holographic readouts. "
            "Soft azure under-water lighting and warm sunbeams illuminate clean flagstone paving. "
            "Stylized anime-comic sci-fi art, thick dark outlines, hidden technology beneath paradise, "
            "and clear floor space across the foreground. "
            "Empty room stage, no people, no operators, no hands, no readable screen text, no letters, no logos."
        )
    },
    {
        "id": "orbital_solarium_scale_05",
        "output_subpath": "world_5/orbital_solarium_scale_05",
        "prompt": (
            "A first-person view inside a quiet garden storage cache niche along the Solarium perimeter. "
            "White enamel utility cupboards holding spare climate filters, brass mist nozzles, and synthetic soil containers. "
            "Potted miniature tropical ferns and warm golden indirect lighting cast gentle shadows across pale sand-colored tiles. "
            "Chunky anime-comic sci-fi look, thick dark outlines, peaceful aesthetic, "
            "with an open walkway leading into the scene. "
            "Empty room stage, no people, no characters, no loose tools, no readable labels, no letters, no logos."
        )
    },
    {
        "id": "orbital_solarium_scale_06",
        "output_subpath": "world_5/orbital_solarium_scale_06",
        "prompt": (
            "A first-person view along a peaceful acoustic promenade known as the Echo Walk flanking the Solarium garden. "
            "Arched white trellises intertwined with lush bioluminescent cyan flowering creepers and golden acoustic resonators. "
            "Polished marble floor tiles reflect soft sunlight and the vast starfield visible through the glass ceiling. "
            "Clean comic-cel sci-fi adventure art, thick dark outlines, serene harmonious perspective, "
            "with a wide unobstructed floor path in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "orbital_solarium_scale_final",
        "output_subpath": "world_5/orbital_solarium_scale_final",
        "prompt": (
            "A first-person view standing at the mechanical shoreline where an artificial tidal generator powers the Solarium lagoon. "
            "Concealed hydraulic wave-maker paddles and stainless steel intake grates visible beneath pristine turquoise water and white synthetic sand. "
            "Above the lagoon, the colossal glass dome showcases the planet and stars under a brilliant artificial golden sun. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, striking juxtaposition of tropical resort and space machinery, "
            "with a clean white sand platform in the lower third. "
            "Empty room stage, no people, no swimmers, no beach chairs, no readable text, no letters, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_10A_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_10A_ROOMS)}] Generating {room_id}...")
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

    # Wire all completed Batch 10A rooms into rooms.json immediately
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
    print(f"\nBatch 10A complete! Wired {updated_count} new rooms into rooms.json.")

if __name__ == "__main__":
    main()
