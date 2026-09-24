#!/usr/bin/env python3
"""
Batch generation script for Starborn room backgrounds - Batch 10B (Service Shaft & Server Farm).
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

BATCH_10B_ROOMS = [
    {
        "id": "orbital_zero_g_junction",
        "output_subpath": "world_5/orbital_zero_g_junction",
        "prompt": (
            "A first-person view inside a spherical zero-gravity utility junction tunnel in the orbital ring. "
            "Three cylindrical maintenance tubes intersect around a massive gimballed gyroscopic stabilizer wheel rotating slowly in the center. "
            "A few loose lightweight repair wrenches and spools of insulated wire float weightlessly in mid-air under warm yellow caution lights. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, dramatic floating zero-g props, "
            "with a clear passage view leading into the tunnel in the lower third for mobile UI. "
            "Empty room stage, no people, no workers, no hands, no readable diagnostic text, no letters, no logos."
        )
    },
    {
        "id": "orbital_service_shaft_scale_01",
        "output_subpath": "world_5/orbital_service_shaft_scale_01",
        "prompt": (
            "A first-person view into a utilitarian maintenance access alcove inside the vertical service shaft of the orbital ring. "
            "Steel rung ladders, hydraulic conduit junctions, and manual emergency shutoff levers set into curving titanium hull plates. "
            "Muted amber work lamps and blue indicator status rings illuminate clean diamond-plate deck landings. "
            "Bold comic-cel sci-fi adventure environment, thick dark outlines, functional industrial architecture, "
            "with uncluttered floor space in the lower foreground. "
            "Empty room stage, no people, no characters, no tools on the floor, no readable markings, no letters, no logos."
        )
    },
    {
        "id": "orbital_service_shaft_scale_02",
        "output_subpath": "world_5/orbital_service_shaft_scale_02",
        "prompt": (
            "A first-person view looking down a high-pressure conduit gallery running parallel to the central service shaft. "
            "Heavy circular pressurized ring bulkheads with glowing cyan status seams and yellow locking clamps. "
            "Reinforced observation portholes show the exterior vacuum and solar array wings. "
            "Stylized anime-comic sci-fi adventure art, thick dark outlines, rhythmic mechanical perspective, "
            "and clean steel decking in the foreground. "
            "Empty room stage, no people, no characters, no hands, no readable text, no letters, no logos."
        )
    },
    {
        "id": "orbital_service_shaft_scale_03",
        "output_subpath": "world_5/orbital_service_shaft_scale_03",
        "prompt": (
            "A first-person view from a suspended service overlook niche cantilevered inside the colossal vertical spine of the station. "
            "Looking up and down a dizzying multi-kilometer vertical shaft lined with concentric transit rings, elevator cables, and coolant pipes. "
            "Atmospheric blue and amber guide lights recede into the extreme distance. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, breathtaking vertical abyss scale, "
            "with a solid steel grating platform in the lower third for mobile UI. "
            "Empty room stage, no people, no moving vehicles, no loose items, no readable text, no letters, no logos."
        )
    },
    {
        "id": "orbital_service_shaft_scale_04",
        "output_subpath": "world_5/orbital_service_shaft_scale_04",
        "prompt": (
            "A first-person view facing an emergency manual valve control pocket in the service shaft. "
            "Heavy mechanical rotary valve wheels, hydraulic pressure bypass cogs, and armored cable conduits recessed into dark alloy bulkheads. "
            "Soft amber work spotlights and red status beacons cast sharp shadows across clean steel floor plates. "
            "Bold comic-cel sci-fi art, thick dark outlines, chunky tactile machinery, "
            "and clear negative space across the foreground floor. "
            "Empty room stage, no people, no operators, no hands, no readable labels, no letters, no logos."
        )
    },
    {
        "id": "orbital_service_shaft_scale_05",
        "output_subpath": "world_5/orbital_service_shaft_scale_05",
        "prompt": (
            "A first-person view inside a secured maintenance cache nook tucked behind vertical elevator guide tracks. "
            "Watertight storage lockers containing magnetic tether clamps, replacement air seals, and emergency repair canisters. "
            "Cool cyan floor runners and warm overhead lamps illuminate an organized work deck. "
            "Stylized anime-comic sci-fi adventure look, thick dark outlines, clean geometric storage forms, "
            "with an open walkway leading into the scene. "
            "Empty room stage, no people, no characters, no loose items, no readable shorthand notes, no letters, no logos."
        )
    },
    {
        "id": "orbital_service_shaft_scale_06",
        "output_subpath": "world_5/orbital_service_shaft_scale_06",
        "prompt": (
            "A first-person view along a narrow acoustic utility corridor running behind the elevator shafts. "
            "Vibrating conduit pipes and sound-dampening ribbed wall panels recede into a shadowy distance under rhythmic amber strobe lights. "
            "Polished dark steel deck plates with subtle reflections of blue circuit conduits along the baseboards. "
            "Clean comic-cel sci-fi art, thick dark outlines, quiet subterranean machine atmosphere, "
            "with clear unobstructed flooring across the lower third. "
            "Empty room stage, no people, no workers, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "orbital_service_shaft_scale_final",
        "output_subpath": "world_5/orbital_service_shaft_scale_final",
        "prompt": (
            "A first-person view of a humble zero-g drift chapel nestled in an open maintenance bay of the service shaft. "
            "Magnetic tether rings, soft insulated golden thermal blankets draped like tapestries, and small unlit commemorative prayer tags float in weightlessness. "
            "Gentle warm amber work lights and distant starfields visible through high maintenance portholes create a quiet reverent mood. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, moving blue-collar sanctuary, "
            "with a clean magnetic anchor deck in the lower foreground for mobile UI. "
            "Empty room stage, no people, no bodies, no readable prayer text, no letters, no logos."
        )
    },
    {
        "id": "deep_mainframe_nave",
        "output_subpath": "world_5/deep_mainframe_nave",
        "prompt": (
            "A first-person view down the colossal central aisle of the deep cryogenic Mainframe Nave. "
            "Towering multi-story supercomputer monoliths encased in frosted glass and cryogenic coolant frost rise like cathedral columns. "
            "In the center midground, an imposing quantum core terminal glows with intricate cyan acoustic light lattices and frozen vapor mist. "
            "Bold comic-cel sci-fi adventure aesthetic, thick dark outlines, majestic crystalline architecture, "
            "with a clean frost-dusted server floor path in the foreground. "
            "Empty room stage, no people, no operators, no hands, no readable terminal text, no letters, no logos."
        )
    },
    {
        "id": "deep_firewall_alpha",
        "output_subpath": "world_5/deep_firewall_alpha",
        "prompt": (
            "A first-person view facing Firewall Node Alpha inside a freezing mainframe server bay. "
            "A massive floating geometric crystalline firewall core suspended inside a polygonal cage of radiant blue hard-light forcefields. "
            "Heavy frosted coolant pipelines and icy brass fittings frame the chamber under cool cyan and deep indigo lighting. "
            "Stylized anime-comic sci-fi adventure art, thick dark outlines, striking energy geometry, "
            "and clean frost-edged deck plating in the lower foreground. "
            "Empty room stage, no people, no characters, no floating numbers or readable code, no letters, no logos."
        )
    },
    {
        "id": "deep_firewall_beta",
        "output_subpath": "world_5/deep_firewall_beta",
        "prompt": (
            "A first-person view inside Firewall Node Beta surrounded by concentric recursion security fields. "
            "Floating holographic portal frames nested inside one another project shimmering cyan and violet geometric barriers down the aisle. "
            "Frosted server stacks with icicle stalactites flank the walkway under cold ambient starlight. "
            "Chunky comic-cel sci-fi look, thick dark outlines, surreal layered spatial depth, "
            "with a clear ice-slick floor path in the foreground for mobile UI. "
            "Empty room stage, no people, no characters, no readable symbols or text, no letters, no logos."
        )
    },
    {
        "id": "deep_firewall_gamma",
        "output_subpath": "world_5/deep_firewall_gamma",
        "prompt": (
            "A first-person view before Firewall Node Gamma in the deep server vault. "
            "A central pedestal projecting an abstract, blank geometric hard-light humanoid crystalline avatar mannequin without facial features, glowing with pale cyan energy. "
            "Flanking cryogenic server banks venting swirling frost vapors under deep blue and purple spotlights. "
            "Bold anime-comic sci-fi adventure aesthetic, thick dark outlines, mysterious crystalline sentinel, "
            "with an open walkway leading toward the pedestal. "
            "Empty room stage, no living people, no faces, no readable code text, no letters, no logos."
        )
    },
    {
        "id": "deep_backup_rack",
        "output_subpath": "world_5/deep_backup_rack",
        "prompt": (
            "A first-person view of an isolated, heavily frosted emergency backup data rack in a cryogenic vault. "
            "A towering server cabinet covered in delicate white rime frost, with a single warm golden diagnostic light blinking through the ice. "
            "Cold blue coolant hoses snake across frozen steel floor plates under dim utility lighting. "
            "Chunky anime-comic sci-fi style, thick inked outlines, stark cryogenic solitude, "
            "with clear floor space in the lower third for mobile UI. "
            "Empty room stage, no people, no characters, no loose items on the ground, no readable text, no letters, no logos."
        )
    },
    {
        "id": "deep_sysadmin_nest",
        "output_subpath": "world_5/deep_sysadmin_nest",
        "prompt": (
            "A first-person view inside a cramped, abandoned system administrator observation nest suspended among cryogenic coolant conduits. "
            "Curved glass console terminal with blank glowing amber CRTs, empty ergonomic swivel chair, and hanging utility cables. "
            "Subtle cyan frost lighting and warm amber desk lamps illuminate compact steel deck plating. "
            "Bold comic-cel sci-fi adventure environment, thick dark outlines, atmospheric blue-collar tech aesthetic, "
            "and clean floor space across the foreground. "
            "Empty room stage, no people, no dead bodies, no characters, no hands, no readable printouts, no letters, no logos."
        )
    },
    {
        "id": "deep_server_farm_scale_01",
        "output_subpath": "world_5/deep_server_farm_scale_01",
        "prompt": (
            "A first-person view into a frosted utility maintenance alcove off the cryogenic server aisle. "
            "White enamel walls peeling back to show thick insulated liquid helium conduits, brass bypass valves, and temperature regulators. "
            "Soft cyan glow and warm amber maintenance lamps illuminate clean frozen metal deck plates. "
            "Stylized anime-comic sci-fi adventure art, thick dark outlines, freezing tech infrastructure, "
            "with clear floor space in the lower foreground. "
            "Empty room stage, no people, no technicians, no tools on ground, no readable markings, no letters, no logos."
        )
    },
    {
        "id": "deep_server_farm_scale_02",
        "output_subpath": "world_5/deep_server_farm_scale_02",
        "prompt": (
            "A first-person view along a sub-zero pressure monitoring corridor bordering the mainframe vaults. "
            "Heavy reinforced cryogenic bulkheads with horizontal cyan and violet indicator strips rimed with frost. "
            "Observation viewports looking into vast server halls where icy vapor drifts between endless blue monoliths. "
            "Chunky anime-comic sci-fi aesthetic, thick dark outlines, monumental computational scale, "
            "and clean frost-edged decking in the lower third for mobile UI. "
            "Empty room stage, no people, no characters, no hands, no readable gauge text, no letters, no logos."
        )
    },
    {
        "id": "deep_server_farm_scale_03",
        "output_subpath": "world_5/deep_server_farm_scale_03",
        "prompt": (
            "A first-person view from an elevated maintenance catwalk overlook high above the infinite cryogenic server farm. "
            "Looking across endless rows of glowing cyan quantum server towers stretching into a frosty atmospheric haze. "
            "Heavy yellow safety handrails and suspension trusses frame the awe-inspiring technological abyss. "
            "Bold comic-cel sci-fi art, thick dark outlines, dizzying scale, "
            "with a solid steel grating platform in the foreground. "
            "Empty room stage, no people, no operators, no readable signs, no letters, no logos."
        )
    },
    {
        "id": "deep_server_farm_scale_04",
        "output_subpath": "world_5/deep_server_farm_scale_04",
        "prompt": (
            "A first-person view facing an emergency manual thermal dump control pocket in the mainframe sector. "
            "A heavy recessed wall panel holding oversized mechanical coolant dump levers, circular pressure wheels, and copper pipe fittings. "
            "Pulsing red emergency indicator lights contrast with freezing cyan server lighting on the polished deck. "
            "Stylized anime-comic sci-fi adventure look, thick dark outlines, tactile industrial controls, "
            "and clean dark flooring in the lower half of the frame. "
            "Empty room stage, no people, no operators, no hands, no readable panel text, no letters, no logos."
        )
    }
]

def main():
    api_key = get_api_key()
    client = OpenAI(api_key=api_key)
    
    base_dir = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "rooms"
    
    for idx, item in enumerate(BATCH_10B_ROOMS, 1):
        room_id = item["id"]
        png_path = base_dir / f"{item['output_subpath']}.png"
        webp_path = base_dir / f"{item['output_subpath']}.webp"
        
        png_path.parent.mkdir(parents=True, exist_ok=True)
        
        print(f"[{idx}/{len(BATCH_10B_ROOMS)}] Generating {room_id}...")
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

    # Wire all completed Batch 10B rooms into rooms.json immediately
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
    print(f"\nBatch 10B complete! Wired {updated_count} new rooms into rooms.json.")

if __name__ == "__main__":
    main()
