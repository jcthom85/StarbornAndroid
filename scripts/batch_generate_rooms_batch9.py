#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 9 (World 5: Executive Dock & Grand Concourse).
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

BATCH_9_ROOMS = [
    {
        "id": "orbital_airlock_gallery",
        "output_subpath": "world_5/orbital_airlock_gallery",
        "prompt": (
            "A first-person view along a curved glass observation gallery overlooking the docking collar in the orbital Void ring. "
            "Panoramic curved floor-to-ceiling glass reveals the blue arc of the planet and black vacuum of space outside. "
            "Pristine white ceramic wall archways, recessed gold insets, and subtle cyan status runners along the floor. "
            "Clean anime-comic sci-fi aesthetic, thick dark outlines, grand orbital solitude, "
            "with a wide polished white floor path in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no readable manifest signs, no letters, no logos."
        )
    },
    {
        "id": "orbital_executive_dock_scale_01",
        "output_subpath": "world_5/orbital_executive_dock_scale_01",
        "prompt": (
            "A first-person view into a concealed service alcove behind the VIP boarding gates of the orbital ring. "
            "Pristine cream and brass architectural wall cladding gives way to exposed titanium conduit runs and manual maintenance hatches. "
            "Soft amber utility downlights illuminate clean dark floor plates with yellow alignment markers. "
            "Bold comic-cel sci-fi adventure environment, thick dark outlines, sharp architectural transitions, "
            "and uncluttered floor space in the lower foreground. "
            "Empty room stage, no people, no characters, no tools on the floor, no readable markings, no letters, no logos."
        )
    },
    {
        "id": "orbital_executive_dock_scale_02",
        "output_subpath": "world_5/orbital_executive_dock_scale_02",
        "prompt": (
            "A first-person view down a pressurized secondary service corridor hugging the exterior orbital ring hull. "
            "Reinforced structural ribs with rhythmic pulsing cyan pressure warning strips and magnetic hatch latches. "
            "Narrow high-pressure viewports look out into the glittering starfield and orbital superstructure. "
            "Chunky anime-comic sci-fi style, thick inked outlines, clean futuristic geometry, "
            "with clear diamond-pattern decking in the lower third. "
            "Empty room stage, no people, no workers, no hands, no readable gauge text, no letters, no logos."
        )
    },
    {
        "id": "orbital_executive_dock_scale_03",
        "output_subpath": "world_5/orbital_executive_dock_scale_03",
        "prompt": (
            "A first-person view stepping into an elevated overlook niche looking past the luxury facade into the ring's industrial infrastructure. "
            "A heavy brass-railed observation balcony framing massive structural gyro-stabilizers and counterweight rings rotating slowly in vacuum beyond glass. "
            "Warm golden lighting contrasts against deep interstellar darkness and glowing blue planet light. "
            "Stylized comic-cel sci-fi art, thick dark outlines, immense structural scale, "
            "with a clean paved landing platform in the foreground. "
            "Empty room stage, no people, no characters, no readable text, no letters, no logos."
        )
    },
    {
        "id": "orbital_executive_dock_scale_04",
        "output_subpath": "world_5/orbital_executive_dock_scale_04",
        "prompt": (
            "A first-person view facing an ornate hidden control pocket set into the marble corridor wall of the orbital ring. "
            "A concealed service panel slides open to reveal manual bypass circuit breakers, copper bus bars, and unlabelled toggle switches. "
            "Subtle cyan holographic diagnostic aura and recessed amber wall sconces illuminate the polished white flooring. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, sleek luxury concealing secret tech, "
            "and clear negative space in the lower half for mobile UI. "
            "Empty room stage, no people, no operators, no hands, no readable panel text, no letters, no logos."
        )
    },
    {
        "id": "orbital_executive_dock_scale_05",
        "output_subpath": "world_5/orbital_executive_dock_scale_05",
        "prompt": (
            "A first-person view into a covert maintenance cache alcove nestled between VIP docking suites in the orbital ring. "
            "Clean recessed wall lockers holding spare synthetic vacuum gaskets, sealed thermal coils, and magnetic utility canisters. "
            "Soft teal floor runners and warm overhead ceiling cove lighting illuminate pristine white composite decking. "
            "Bold comic-cel sci-fi adventure look, thick dark outlines, neat minimalist storage forms, "
            "with clear floor space in the foreground. "
            "Empty room stage, no people, no characters, no loose items, no readable shorthand notes, no letters, no logos."
        )
    },
    {
        "id": "orbital_executive_dock_scale_06",
        "output_subpath": "world_5/orbital_executive_dock_scale_06",
        "prompt": (
            "A first-person view down a silent, acoustically dampened reverberation corridor known as the Echo Walk in the orbital ring. "
            "Rhythmic geometric sound-absorbing wall fins in white ceramic and brushed gold trim recede into the distance. "
            "Soft ambient cyan resonance light pulses softly along the baseboards, reflecting off polished glass floor panels. "
            "Clean anime-comic sci-fi adventure art, thick dark outlines, elegant rhythmic perspective, "
            "with unobstructed flooring across the lower third. "
            "Empty room stage, no people, no characters, no readable signage, no letters, no logos."
        )
    },
    {
        "id": "orbital_executive_dock_scale_final",
        "output_subpath": "world_5/orbital_executive_dock_scale_final",
        "prompt": (
            "A first-person view facing a solemn memorial plinth in an orbital docking rotunda overlooking the stars. "
            "A central polished black granite pedestal with an abstract cyan holographic eternal flame projection. "
            "Floor-to-ceiling panoramic viewports frame the curvature of the Earth-like planet below against deep space. "
            "Stylized comic-cel sci-fi adventure environment, thick dark outlines, poignant celestial majesty, "
            "with clean white marble paving in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no readable names or text, no letters, no logos."
        )
    },
    {
        "id": "orbital_customs_lounge",
        "output_subpath": "world_5/orbital_customs_lounge",
        "prompt": (
            "A first-person view inside an eerie, zero-gravity customs arrival lounge in the orbital Void ring. "
            "Luxurious velvet queuing stanchions with floating velvet ropes and sleek automated biometric turnstiles. "
            "Empty sleek aerodynamic traveler luggage cases float suspended weightlessly in mid-air under warm gold cove lights and large observation windows. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, surreal floating environmental props, "
            "with a wide clear polished floor in the lower foreground. "
            "Empty room stage, no people, no travelers, no hands, no readable tags or letters, no logos."
        )
    },
    {
        "id": "orbital_grand_concourse_scale_01",
        "output_subpath": "world_5/orbital_grand_concourse_scale_01",
        "prompt": (
            "A first-person view inside a utility maintenance cul-de-sac branching off the Grand Concourse. "
            "Sleek architectural facade panels peel back to show heavy hydraulic air filtration ducts and pneumatic conduits. "
            "Gentle amber maintenance lighting washes over clean dark non-slip floor tiles. "
            "Bold comic-cel sci-fi art, thick dark outlines, industrial structure hidden beneath paradise, "
            "with clear open floor space in the lower half of the frame. "
            "Empty room stage, no people, no workers, no tools on the ground, no readable markings, no letters, no logos."
        )
    },
    {
        "id": "orbital_grand_concourse_scale_02",
        "output_subpath": "world_5/orbital_grand_concourse_scale_02",
        "prompt": (
            "A first-person view along a high-vacuum pressure monitoring gallery beside the Grand Concourse. "
            "Reinforced titanium pressure bulkheads with horizontal cyan and amber indicator bars embedded in the wall seams. "
            "Sleek minimalist arches lead toward a distant airlock portal under cool celestial starlight. "
            "Stylized anime-comic sci-fi adventure look, thick dark outlines, sharp modern silhouettes, "
            "and clean composite flooring in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no hands, no readable gauges, no letters, no logos."
        )
    },
    {
        "id": "orbital_grand_concourse_scale_03",
        "output_subpath": "world_5/orbital_grand_concourse_scale_03",
        "prompt": (
            "A first-person view from a secluded architectural overlook niche high on the wall of the Grand Concourse. "
            "A floating cantilevered glass-bottom balcony looking down onto the vast polished white promenade below. "
            "Giant golden structural support arches frame the panoramic view of the sunlit planet through the vaulted glass ceiling. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, dizzying monumental space architecture, "
            "with a clean glass balcony floor in the foreground. "
            "Empty room stage, no people, no crowds, no readable advertisements, no letters, no logos."
        )
    },
    {
        "id": "orbital_grand_concourse_scale_04",
        "output_subpath": "world_5/orbital_grand_concourse_scale_04",
        "prompt": (
            "A first-person view of a hidden environmental automation hub concealed behind decorative concourse pillars. "
            "A sleek curved terminal console with blank amber holographic displays, manual rotary dials, and fiber-optic conduits. "
            "Overhead circular skylights channel bright orbital sunlight across pristine polished white floor tiles. "
            "Bold comic-cel sci-fi environment, thick dark outlines, harmonious architectural shapes, "
            "and clear negative space across the foreground. "
            "Empty room stage, no people, no operators, no hands, no readable screen text, no letters, no logos."
        )
    },
    {
        "id": "orbital_grand_concourse_scale_05",
        "output_subpath": "world_5/orbital_grand_concourse_scale_05",
        "prompt": (
            "A first-person view into a concealed emergency supply alcove off the main concourse avenue. "
            "Built-in pressurized wall cupboards with brushed aluminum latches, spare atmospheric canisters, and sealed emergency rations. "
            "Soft turquoise baseboard lighting and warm indirect ceiling illumination across dark terrazzo flooring. "
            "Chunky anime-comic sci-fi style, thick inked outlines, clean modular lines, "
            "with an open walkway leading into the frame. "
            "Empty room stage, no people, no characters, no loose items, no readable labels, no letters, no logos."
        )
    },
    {
        "id": "orbital_grand_concourse_scale_06",
        "output_subpath": "world_5/orbital_grand_concourse_scale_06",
        "prompt": (
            "A first-person view along an acoustic transit gallery carrying low harmonic hums throughout the orbital ring. "
            "Curving white acoustic baffles with golden trim and illuminated cyan resonance tubes set into the vaulted ceiling. "
            "A long tranquil corridor stretching toward a glowing glass portal overlooking open space. "
            "Stylized anime-comic sci-fi adventure art, thick dark outlines, elegant fluid perspective, "
            "with clean reflective floor tiles in the lower third for mobile UI. "
            "Empty room stage, no people, no characters, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "orbital_grand_concourse_scale_final",
        "output_subpath": "world_5/orbital_grand_concourse_scale_final",
        "prompt": (
            "A first-person view inside an abandoned orbital courtesy theater overlooking the Grand Concourse. "
            "Tiers of plush cream and gold amphitheater seating facing an empty circular stage bathed in soft golden spotlighting. "
            "Behind the stage, a colossal panoramic glass dome reveals the swirling clouds of the planet and distant starfields. "
            "Chunky comic-cel sci-fi aesthetic, thick dark outlines, surreal luxurious emptiness, "
            "with clean carpeted aisle in the foreground for mobile UI. "
            "Empty room stage, no people, no audiences, no video screens with faces or words, no letters, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_9_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_9_ROOMS)}] Generating {room_id}...")
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

    # Wire all completed Batch 9 rooms into rooms.json immediately
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
    print(f"\nBatch 9 complete! Wired {updated_count} new rooms into rooms.json.")

if __name__ == "__main__":
    main()
