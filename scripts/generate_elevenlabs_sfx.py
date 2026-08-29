#!/usr/bin/env python3
"""
Starborn ElevenLabs Sound Effects (SFX) Generator & Audio Catalog Wiring Script
Generates bespoke sound effects using the ElevenLabs Sound Generation API
and registers them into audio_catalog.json and app/src/main/res/raw/.
"""

import os
import sys
import json
import time
import argparse
import requests

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
CATALOG_PATH = os.path.join(ROOT_DIR, "app", "src", "main", "assets", "audio_catalog.json")
RAW_AUDIO_DIR = os.path.join(ROOT_DIR, "app", "src", "main", "res", "raw")

SFX_REGISTRY = [
    # Category A: UI & System Navigation
    {
        "id": "sfx_ui_button_click",
        "title": "Mechanical Relay Button Click",
        "category": "ui",
        "type": "ui",
        "gain": 0.90,
        "duration": 0.5,
        "tags": ["ui", "button", "mechanical"],
        "prompt": "Tactile mechanical relay switch click, vintage sci-fi terminal button press, crisp transient, subtle metallic spring."
    },
    {
        "id": "sfx_ui_confirm",
        "title": "Confirmation Tone",
        "category": "ui",
        "type": "ui",
        "gain": 0.90,
        "duration": 0.8,
        "tags": ["ui", "confirm", "synth"],
        "prompt": "Warm two-tone FM synth chime, retro computer confirmation beep, positive sci-fi terminal prompt."
    },
    {
        "id": "sfx_ui_cancel",
        "title": "Cancel Click",
        "category": "ui",
        "type": "ui",
        "gain": 0.85,
        "duration": 0.5,
        "tags": ["ui", "cancel", "click"],
        "prompt": "Subtle descending electronic click, terminal back button press, clean tape deck stop click."
    },
    {
        "id": "sfx_ui_error",
        "title": "Access Denied Buzzer",
        "category": "ui",
        "type": "ui",
        "gain": 0.90,
        "duration": 1.0,
        "tags": ["ui", "error", "buzzer"],
        "prompt": "Low vintage buzzer tone, 80s terminal access denied buzz, analog circuitry overload warning."
    },
    {
        "id": "sfx_ui_tab_switch",
        "title": "Tape Deck Tab Switch",
        "category": "ui",
        "type": "ui",
        "gain": 0.85,
        "duration": 0.5,
        "tags": ["ui", "tab", "mechanical"],
        "prompt": "Analog cassette player deck head engagement click, fast tactile slider snap."
    },
    {
        "id": "sfx_ui_equip_item",
        "title": "Gear Holster Equip",
        "category": "ui",
        "type": "ui",
        "gain": 0.90,
        "duration": 1.0,
        "tags": ["ui", "inventory", "equip"],
        "prompt": "Tactile gear holster click, heavy metal buckle latch, military equipment equip sound."
    },
    {
        "id": "sfx_ui_item_pickup",
        "title": "Scrap Discovery Chime",
        "category": "ui",
        "type": "ui",
        "gain": 0.90,
        "duration": 1.0,
        "tags": ["ui", "inventory", "pickup"],
        "prompt": "Bright 8-bit loot pickup chime, metallic scrap coins clinking together, rewarding discovery sound."
    },

    # Category B: Exploration & Environmental Interactions
    {
        "id": "sfx_door_airlock_open",
        "title": "Airlock Depressurization & Open",
        "category": "world",
        "type": "sfx",
        "gain": 0.95,
        "duration": 3.0,
        "tags": ["world", "door", "pneumatic"],
        "prompt": "Heavy sci-fi airlock door opening, intense pneumatic air release hiss, motorized sliding heavy steel door."
    },
    {
        "id": "sfx_door_airlock_close",
        "title": "Bulkhead Hydraulic Seal",
        "category": "world",
        "type": "sfx",
        "gain": 0.95,
        "duration": 2.5,
        "tags": ["world", "door", "hydraulic"],
        "prompt": "Heavy bulkhead door sliding shut, hydraulic clamp engaging with deep resonant steel thud."
    },
    {
        "id": "sfx_terminal_hack_success",
        "title": "Terminal Bypass Success",
        "category": "world",
        "type": "sfx",
        "gain": 0.90,
        "duration": 2.5,
        "tags": ["world", "terminal", "hack"],
        "prompt": "Vintage computer modem handshake burst, rapid electronic decoding beeps, triumphant green terminal chime."
    },
    {
        "id": "sfx_terminal_boot",
        "title": "CRT Monitor Boot Coil",
        "category": "world",
        "type": "sfx",
        "gain": 0.88,
        "duration": 3.0,
        "tags": ["world", "terminal", "crt"],
        "prompt": "Vintage CRT monitor power on degauss coil hum, static electric pop, 80s terminal phosphor hum."
    },
    {
        "id": "sfx_loot_crate_open",
        "title": "Cargo Container Unlatch",
        "category": "world",
        "type": "sfx",
        "gain": 0.90,
        "duration": 2.5,
        "tags": ["world", "loot", "cargo"],
        "prompt": "Heavy industrial cargo container latches unbuckling, pneumatic seal pop, motorized lid opening."
    },
    {
        "id": "sfx_tape_insert",
        "title": "Cassette Deck Insert & Play",
        "category": "world",
        "type": "sfx",
        "gain": 0.90,
        "duration": 1.5,
        "tags": ["world", "tape", "cassette"],
        "prompt": "Tactile cassette tape being pushed into a vintage tape deck, mechanical spring catch click, play button engaged."
    },
    {
        "id": "sfx_scavenge_metal",
        "title": "Industrial Scrap Scavenge",
        "category": "world",
        "type": "sfx",
        "gain": 0.90,
        "duration": 2.0,
        "tags": ["world", "scavenge", "metal"],
        "prompt": "Scrap metal pieces clattering in a dust bin, wrench digging through spare bolts, gritty industrial scavenging."
    },

    # Category C: Combat & Weaponry
    {
        "id": "sfx_combat_blaster_fire",
        "title": "Analog Plasma Blaster",
        "category": "combat",
        "type": "battle",
        "gain": 0.95,
        "duration": 1.0,
        "tags": ["combat", "weapon", "laser"],
        "prompt": "Vintage 80s sci-fi blaster gunshot, punchy analog synthesizer laser beam, resonant plasma discharge."
    },
    {
        "id": "sfx_combat_heavy_wrench",
        "title": "Pipe Wrench Strike",
        "category": "combat",
        "type": "battle",
        "gain": 0.95,
        "duration": 1.2,
        "tags": ["combat", "weapon", "melee"],
        "prompt": "Crushing pipe wrench melee strike, heavy steel impact against armor plate, resonant metallic thud."
    },
    {
        "id": "sfx_combat_plasma_arc",
        "title": "High-Voltage Plasma Shock",
        "category": "combat",
        "type": "battle",
        "gain": 0.95,
        "duration": 1.8,
        "tags": ["combat", "weapon", "plasma"],
        "prompt": "Crackling electric plasma arc discharge, high-voltage Tesla zap, sizzling ionized air."
    },
    {
        "id": "sfx_combat_phase_blade",
        "title": "Phase Blade Slice",
        "category": "combat",
        "type": "battle",
        "gain": 0.95,
        "duration": 1.5,
        "tags": ["combat", "weapon", "blade"],
        "prompt": "High-frequency vibrating energy katana slash, clean air displacement whoosh, harmonic laser blade cut."
    },
    {
        "id": "sfx_combat_shield_deflect",
        "title": "Hex-Barrier Deflection",
        "category": "combat",
        "type": "battle",
        "gain": 0.95,
        "duration": 1.5,
        "tags": ["combat", "defense", "shield"],
        "prompt": "Sci-fi kinetic energy shield impact, resonant glass-metallic deflection ping, harmonic forcefield hum."
    },
    {
        "id": "sfx_combat_crit_hit",
        "title": "Critical Impact Boom",
        "category": "combat",
        "type": "battle",
        "gain": 1.0,
        "duration": 2.0,
        "tags": ["combat", "impact", "crit"],
        "prompt": "Massive sub-bass explosion punch, critical strike shatter impact, dramatic cinematic impact hit."
    },
    {
        "id": "sfx_combat_enemy_screech",
        "title": "Alien Predator Screech",
        "category": "combat",
        "type": "battle",
        "gain": 0.95,
        "duration": 2.0,
        "tags": ["combat", "enemy", "alien"],
        "prompt": "Eerie organic alien creature roar, guttural insectoid hiss and high-pitched predatory shriek."
    },
    {
        "id": "sfx_combat_robot_stomp",
        "title": "Hydraulic Mech Stomp",
        "category": "combat",
        "type": "battle",
        "gain": 1.0,
        "duration": 2.5,
        "tags": ["combat", "enemy", "mech"],
        "prompt": "Gigantic multi-ton mechanical robot step, heavy ground shake impact, hydraulic piston hiss."
    },

    # Category D: Activities & Mini-Games
    {
        "id": "sfx_fishing_cast",
        "title": "Fishing Line Cast",
        "category": "activities",
        "type": "sfx",
        "gain": 0.90,
        "duration": 1.5,
        "tags": ["fishing", "minigame"],
        "prompt": "Fishing rod line cast whoosh, spinning reel whirring buzz, high-speed line tension."
    },
    {
        "id": "sfx_fishing_splash",
        "title": "Lure Water Splash",
        "category": "activities",
        "type": "sfx",
        "gain": 0.90,
        "duration": 1.2,
        "tags": ["fishing", "water"],
        "prompt": "Clean fishing bobber splashing into water, gentle ripple and bubbly aquatic plop."
    },
    {
        "id": "sfx_fishing_bite",
        "title": "Fish Strike Tension",
        "category": "activities",
        "type": "sfx",
        "gain": 0.90,
        "duration": 1.2,
        "tags": ["fishing", "bite"],
        "prompt": "Sharp fishing rod flex strain, water splash, subtle excitement chime."
    },
    {
        "id": "sfx_fishing_catch",
        "title": "Fish Caught Fanfare",
        "category": "activities",
        "type": "sfx",
        "gain": 0.95,
        "duration": 2.0,
        "tags": ["fishing", "catch"],
        "prompt": "Triumphant water splash with a glittering magical discovery chime, fish flopping on boat deck."
    },
    {
        "id": "sfx_arcade_coin_insert",
        "title": "Arcade Coin Insert",
        "category": "activities",
        "type": "ui",
        "gain": 0.90,
        "duration": 1.5,
        "tags": ["arcade", "coin"],
        "prompt": "1980s coin-op arcade coin drop, metallic coin sliding down slot and triggering electronic credit chirp."
    },
    {
        "id": "sfx_arcade_jump",
        "title": "8-Bit Jump Chirp",
        "category": "activities",
        "type": "ui",
        "gain": 0.90,
        "duration": 0.5,
        "tags": ["arcade", "8bit"],
        "prompt": "Retro 8-bit video game jump sound, classic arcade square wave upward pitch sweep."
    },
    {
        "id": "sfx_arcade_laser",
        "title": "8-Bit Arcade Laser",
        "category": "activities",
        "type": "ui",
        "gain": 0.90,
        "duration": 0.5,
        "tags": ["arcade", "8bit"],
        "prompt": "Authentic 1980s arcade space shooter laser zap, punchy 8-bit noise pulse."
    },
    {
        "id": "sfx_arcade_game_over",
        "title": "Arcade Death Jingle",
        "category": "activities",
        "type": "ui",
        "gain": 0.90,
        "duration": 2.5,
        "tags": ["arcade", "gameover"],
        "prompt": "Classic 1980s arcade game over jingle, descending 8-bit arpeggio with sad final noise burst."
    },
    {
        "id": "sfx_tinkering_ratchet",
        "title": "Socket Wrench Ratchet",
        "category": "activities",
        "type": "sfx",
        "gain": 0.90,
        "duration": 1.8,
        "tags": ["crafting", "tinkering"],
        "prompt": "Vintage mechanical socket wrench ratcheting back and forth, tight bolt squeak, workbench tool clink."
    },
    {
        "id": "sfx_cooking_sizzle",
        "title": "Cast Iron Pan Sizzle",
        "category": "activities",
        "type": "sfx",
        "gain": 0.90,
        "duration": 2.5,
        "tags": ["crafting", "cooking"],
        "prompt": "Hot cast iron pan sizzling with butter, sizzling stew bubbling, wooden spoon stirring delicious food."
    }
]

def update_audio_catalog():
    """Merges all SFX into audio_catalog.json cues array."""
    if not os.path.exists(CATALOG_PATH):
        print(f"Error: {CATALOG_PATH} not found.")
        return False

    with open(CATALOG_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    existing_cues = {c["id"]: c for c in data.get("cues", [])}
    for s in SFX_REGISTRY:
        existing_cues[s["id"]] = {
            "id": s["id"],
            "category": s["type"],
            "priority": 5,
            "gain": s["gain"],
            "fade_in_ms": 0,
            "fade_out_ms": 200,
            "duration_ms": int(s["duration"] * 1000)
        }
    data["cues"] = list(existing_cues.values())

    with open(CATALOG_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")

    print(f"Successfully synced {len(SFX_REGISTRY)} SFX cues into audio_catalog.json (Total cues: {len(data['cues'])})")
    return True

def generate_sfx_elevenlabs(api_key, sfx_info):
    """Calls ElevenLabs sound-generation API."""
    sfx_id = sfx_info["id"]
    output_file = os.path.join(RAW_AUDIO_DIR, f"{sfx_id}.mp3")

    print(f"\n[{sfx_info['category'].upper()}] -> {sfx_id} (\"{sfx_info['title']}\")")
    print(f"  Prompt: {sfx_info['prompt']}")

    url = "https://api.elevenlabs.io/v1/sound-generation"
    headers = {
        "xi-api-key": api_key,
        "Content-Type": "application/json"
    }
    payload = {
        "text": sfx_info["prompt"],
        "duration_seconds": sfx_info["duration"],
        "prompt_influence": 0.45
    }

    try:
        response = requests.post(url, json=payload, headers=headers, timeout=60)
        if response.status_code == 200:
            with open(output_file, "wb") as f:
                f.write(response.content)
            print(f"  [SUCCESS] -> {output_file} ({len(response.content)} bytes)")
            return True
        else:
            print(f"  [API ERROR {response.status_code}]: {response.text}")
            return False
    except Exception as e:
        print(f"  [EXCEPTION]: {e}")
        return False

def main():
    parser = argparse.ArgumentParser(description="Starborn ElevenLabs Sound Effects Generator")
    parser.add_argument("--api-key", default="", help="ElevenLabs API Key")
    parser.add_argument("--category", choices=["all", "ui", "world", "combat", "activities"], default="all", help="Category to generate")
    parser.add_argument("--cue", help="Specific cue ID to generate")
    parser.add_argument("--force", action="store_true", help="Force regenerate existing files")
    args = parser.parse_args()

    api_key = args.api_key.strip()
    if not api_key:
        api_key = os.getenv("ELEVENLABS_API_KEY", "").strip()
    if not api_key:
        key_file = os.path.join(ROOT_DIR, "elevenlabs_api_key.txt")
        if os.path.exists(key_file):
            with open(key_file, "r", encoding="utf-8") as f:
                api_key = f.read().strip()
                if api_key:
                    print(f"Loaded ElevenLabs API Key from {key_file} (prefix: {api_key[:6]}...)")

    if not api_key:
        print("Error: No ElevenLabs API Key found.")
        sys.exit(1)

    # Step 1: Sync Catalog
    print("=== STEP 1: SYNCING AUDIO CATALOG ===")
    update_audio_catalog()

    # Step 2: Select Targets
    if args.cue:
        targets = [s for s in SFX_REGISTRY if s["id"] == args.cue]
    elif args.category == "all":
        targets = SFX_REGISTRY
    else:
        targets = [s for s in SFX_REGISTRY if s["category"] == args.category]

    print(f"\n=== STEP 2: GENERATING {len(targets)} SFX WITH ELEVENLABS ===")
    os.makedirs(RAW_AUDIO_DIR, exist_ok=True)

    success_count = 0
    for s in targets:
        dest = os.path.join(RAW_AUDIO_DIR, f"{s['id']}.mp3")
        if os.path.exists(dest) and not args.force and not args.cue:
            print(f"  [EXISTS] {s['id']}.mp3 ({os.path.getsize(dest)} bytes)")
            success_count += 1
            continue
        if generate_sfx_elevenlabs(api_key, s):
            success_count += 1
        time.sleep(0.5)

    print(f"\n=== SUMMARY: {success_count}/{len(targets)} SFX GENERATED ===")

if __name__ == "__main__":
    main()
