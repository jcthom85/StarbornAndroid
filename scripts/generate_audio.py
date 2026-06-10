#!/usr/bin/env python3
"""
Starborn Audio Generator
Automates generation of JRPG music, sound effects, and voice clips using the ElevenLabs API,
saving them directly into the Android raw resources folder.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
import urllib.request
import urllib.error

# Root folder and output paths
PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUTPUT_DIR = PROJECT_ROOT / "app" / "src" / "main" / "res" / "raw"

# Base ElevenLabs voices for characters (Public voice IDs or custom overrides)
CHARACTER_VOICE_MAP = {
    "nova": "EXAVITQu4vr4xnSDxMaL",     # Bella (Young, textured female)
    "zeke": "pNInz6obpgfrhhF2Ewqi",     # Giovanni (Rugged, warm male)
    "orion": "N2lVS1w7qi65JdC5uCcR",    # Brian (Deep, resonant male)
    "gh0st": "IKne3meq5aSn9XLyUdCD",    # Charlie (Clipped, low male)
    "thorne": "AZnzlk1XvdvUeBnXmlld",   # Dom (Corporate, professional female)
    "vale": "ErXwobaYiN019PkySvjV",     # Antoni (Melodic, smooth male)
}

# Pre-configured Music Prompts from Audio_Design_Guide.md
MUSIC_CATALOG = {
    "music_title_theme": {
        "prompt": "A pensive JRPG title theme, 75 BPM, solo acoustic fingerpicked guitar intro, slowly swelling into warm orchestral strings, ethereal choir pads, and distant echoing electric guitar harmonies, vast cathedral reverb. Mysterious, hopeful, and emotional. Loopable.",
        "duration_ms": 120000, # 2 minutes
    },
    "music_w1_homestead_explore": {
        "prompt": "Warm working-class acoustic blues, 80 BPM, slide resonator guitar, acoustic steel-string fingerpicking, dusty hand percussion, intimate cabin reverb. Nostalgic and determined mood. Featuring a prominent, slow 5-note ascending melodic hook on a steel-string guitar.",
        "duration_ms": 120000, # 2 minutes
    },
    "music_w1_logistics_explore": {
        "prompt": "Warm working-class acoustic blues, 85 BPM, slide resonator guitar, acoustic steel-string, slow industrial pipe rhythm, dusty room reverb. Pensive mood. Featuring a pensive, slow 5-note ascending melodic hook on a steel-string guitar.",
        "duration_ms": 120000,
    },
    "music_w1_combat": {
        "prompt": "Aggressive blues-rock, 130 BPM, overdriven slide electric guitar, gritty rhythm section, punchy drum kit, dry room acoustics. Energetic and defiant mood. Featuring a prominent, driving 5-note ascending melodic hook played on a distorted electric lead guitar.",
        "duration_ms": 90000, # 1.5 minutes
    },
    "music_crash_flight": {
        "prompt": "Epic cinematic electronic-orchestral fusion, 140 BPM, screaming string sections, rising alarm chords, rushing wind noise, extreme tension. High-stakes urgency. Short dramatic sequence.",
        "duration_ms": 45000, # 45 seconds
    },
    "music_w2_sector9_explore": {
        "prompt": "Ethereal forest JRPG music, 90 BPM, 12-string acoustic guitar, wood flutes, ambient synth pads, slow frame drums, soaring solo violin, wide lush reverb. Eerie, ancient jungle mood.",
        "duration_ms": 120000,
    },
    "music_w2_combat": {
        "prompt": "Tribal JRPG orchestral combat, 130 BPM, heavy tribal drums, double-bass grooves, sweeping string sections, woodwind flutes, open air echo. High-stakes battle mood.",
        "duration_ms": 90000,
    },
    "music_w3_spire_explore": {
        "prompt": "Cool jazz-rock, 95 BPM, slick electric fretless bass, saxophone, Rhodes electric piano, clean electric guitar chords, crisp hi-hat jazz drums. Late night city mood.",
        "duration_ms": 120000,
    },
    "music_w3_combat": {
        "prompt": "Infiltration funk-rock, 125 BPM, snapping slap bass, clean electric funk guitar strumming, aggressive organ stabs, crisp drum kit. Urgent high-energy chase.",
        "duration_ms": 90000,
    },
    "music_w4_foundry_explore": {
        "prompt": "Grinding industrial ambient rock, 75 BPM, industrial guitar drone, dripping metallic feedback, low machinery hum, cavernous metallic reverb. Oppressive heat.",
        "duration_ms": 120000,
    },
    "music_w4_combat": {
        "prompt": "Aggressive industrial metal, 145 BPM, chugging distorted electric rhythm guitars, heavy double-kick drums, grinding synth bass, wide industrial warehouse reverb. Furious and heavy mood. Featuring a prominent, driving 5-note ascending melodic hook played as a fast, screaming electric guitar solo.",
        "duration_ms": 90000,
    },
    "music_w5_void_explore": {
        "prompt": "Melancholic solo piano, 70 BPM, high-register classical piano keys, long sustain, vast empty sterile room reverb. Cold, lonely, and clinical mood. Featuring a chilling, slow 4-note descending chromatic melody played on the piano.",
        "duration_ms": 120000,
    },
    "music_w5_combat": {
        "prompt": "Epic neo-classical orchestral metal, 135 BPM, rapid church organ ostinatos, heavy chugging metal guitars, thunderous drums, sharp harpsichord runs, cold empty room acoustics. Intense, theatrical conflict. Featuring a chilling, slow 4-note descending chromatic melody played by the pipe organ and brass.",
        "duration_ms": 90000,
    },
    "music_w6_source_explore": {
        "prompt": "Surreal experimental ambient, 60 BPM, massive cathedral choir, pitch-shifted vocal pads, acoustic waveforms warping, sub-bass pressure, wet cavernous reverb. Haunting, cosmic resonance.",
        "duration_ms": 150000,
    },
    "music_w6_combat": {
        "prompt": "Epic JRPG symphonic rock, 120 BPM, soaring electric guitar lead, full orchestra, dramatic strings, grand brass section, church organ, massive cathedral reverb. High-stakes emotional climax. Featuring a prominent, driving 5-note ascending melodic hook played in unison by the lead guitar and violins.",
        "duration_ms": 120000,
    },
    "music_victory_theme": {
        "prompt": "A triumphant and energetic JRPG victory theme, 120 BPM, bright orchestral brass, upbeat rock drum kit, soaring electric guitar leads, positive and celebratory mood. Loopable.",
        "duration_ms": 60000,
    },
}

# Pre-configured SFX Prompts from Audio_Design_Guide.md
SFX_CATALOG = {
    "ui_title_start": {
        "text": "A bright, sparkling JRPG crystal chime rising in pitch, ending with a low, deep ocean wave whoosh, heavy sub-bass decay, magical transition sound",
        "duration": 2.5,
    },
    "ui_click": {
        "text": "A short, dry, high-pitched mechanical wood-and-metal click, transient spike, clean tail, user interface sound",
        "duration": 0.5,
    },
    "ui_confirm": {
        "text": "A clean acoustic wooden chime note, resonant frequency, warm, positive feedback, 300ms decay",
        "duration": 0.8,
    },
    "ui_error": {
        "text": "A dry, low-frequency double buzz, metallic buzzer sound, corporate security alarm, short tail",
        "duration": 0.6,
    },
    "ui_back": {
        "text": "A soft, downward acoustic wood click, clean decay, back button feedback",
        "duration": 0.5,
    },
    "battle_start": {
        "text": "A dramatic orchestral brass stab, rising energy, JRPG combat transition sound, low rumble impact",
        "duration": 1.5,
    },
    "shield_block": {
        "text": "A heavy metal plate slamming shut, high-impact clank, metallic resonance ringing out, solid barrier",
        "duration": 0.8,
    },
    "shield_break": {
        "text": "A violent shearing sound of metal buckling and breaking under immense pressure, sharp shrapnel scatter",
        "duration": 1.2,
    },
    "victory_fanfare": {
        "text": "A short, triumphant orchestral brass fanfare, JRPG victory sting, positive resolution, bright horn section",
        "duration": 3.0,
    },
    "defeat_sting": {
        "text": "A pensive, low-register solo piano chord decaying, sorrowful symphonic string pad, game over sting",
        "duration": 4.0,
    },
    "amb_vent": {
        "text": "A constant, low-frequency industrial ventilation hum, deep rhythmic white noise, air flowing through a hollow pipe, loopable ambient background noise",
        "duration": 30.0,
    },
    "amb_forest_birds": {
        "text": "Soft outdoor breeze blowing through tree leaves, gentle chirping of distant forest birds, peaceful loopable nature soundscape",
        "duration": 30.0,
    },
    "amb_cave_drips": {
        "text": "Hollow stone cavern echo, slow cold water droplets dripping onto wet stones, deep dark subterranean room acoustics, loopable background",
        "duration": 30.0,
    },
    "wpn_nova_laser": {
        "text": "A sharp sci-fi laser gun blast, high-tech energy projectile discharge, crisp futuristic weapon sound effect, short decay",
        "duration": 0.8,
    },
    "wpn_zeke_punch": {
        "text": "A powerful heavy physical impact punch, boxing glove hitting solid target, deep leather impact crunch, short tail",
        "duration": 0.6,
    },
    "wpn_orion_resonance": {
        "text": "A high-pitched magical crystal chime resonance, sparkling harmonic shimmer rising, ethereal energy pulse weapon sound",
        "duration": 1.0,
    },
    "wpn_gh0st_slash": {
        "text": "A quick sharp steel sword slash slicing through the air, crisp metallic blade swing swoosh sound effect",
        "duration": 0.7,
    },
}

# Voiceover scripts mapping
VOICE_CATALOG = {
    "shop_mechanic_vo": {
        "character": "zeke",
        "text": "Need something calibrated? Don't break it before we start, please.",
    },
    "shop_weapon_vo": {
        "character": "gh0st",
        "text": "Select your armaments. Lethal efficiency is highly recommended.",
    },
    "shop_armor_vo": {
        "character": "zeke",
        "text": "Keep your shields up. I'd rather patch your armor than your skin.",
    },
    "shop_accessory_vo": {
        "character": "orion",
        "text": "These Relic shards hum in resonance. Choose carefully, young spark.",
    },
    "shop_general_vo": {
        "character": "nova",
        "text": "Check your supplies. The road ahead won't offer any free handouts.",
    },
}


def get_api_key(args_key: str | None) -> str:
    """Resolves ElevenLabs API key from args, env, or local file."""
    if args_key:
        return args_key
    if os.environ.get("ELEVENLABS_API_KEY"):
        return os.environ["ELEVENLABS_API_KEY"]
    
    key_file = PROJECT_ROOT / "elevenlabs_api_key.txt"
    if key_file.exists():
        try:
            return key_file.read_text(encoding="utf-8").strip()
        except OSError:
            pass
            
    # Try parent directory / app data as fallback
    print("Error: ElevenLabs API key not found. Specify via --key, ELEVENLABS_API_KEY env, or elevenlabs_api_key.txt", file=sys.stderr)
    sys.exit(1)


def make_api_request(url: str, payload: dict, api_key: str) -> bytes:
    """Makes a POST request to ElevenLabs API and returns raw response bytes."""
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={
            "xi-api-key": api_key,
            "Content-Type": "application/json"
        },
        method="POST"
    )
    
    try:
        with urllib.request.urlopen(req) as response:
            return response.read()
    except urllib.error.HTTPError as e:
        error_body = e.read().decode("utf-8") if e.fp else ""
        print(f"API Error ({e.code}): {e.reason}\nResponse: {error_body}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"Network error occurred: {e}", file=sys.stderr)
        sys.exit(1)


def generate_music(track_id: str, api_key: str, output_dir: Path):
    """Generates music track using ElevenLabs /v1/music/stream."""
    if track_id not in MUSIC_CATALOG:
        print(f"Error: Music track '{track_id}' not found in catalog.", file=sys.stderr)
        sys.exit(1)
        
    config = MUSIC_CATALOG[track_id]
    print(f"Generating music '{track_id}'...")
    print(f"Prompt: {config['prompt']}")
    
    payload = {
        "prompt": config["prompt"],
        "music_length_ms": config["duration_ms"],
        "model_id": "music_v1",
        "force_instrumental": True
    }
    
    url = "https://api.elevenlabs.io/v1/music/stream"
    audio_bytes = make_api_request(url, payload, api_key)
    
    # Save file
    output_path = output_dir / f"{track_id}.mp3"
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path.write_bytes(audio_bytes)
    print(f"Successfully generated music: {output_path} ({len(audio_bytes)} bytes)")


def generate_sfx(cue_id: str, api_key: str, output_dir: Path):
    """Generates sound effect using ElevenLabs /v1/sound-generation."""
    if cue_id not in SFX_CATALOG:
        print(f"Error: SFX cue '{cue_id}' not found in catalog.", file=sys.stderr)
        sys.exit(1)
        
    config = SFX_CATALOG[cue_id]
    print(f"Generating SFX '{cue_id}'...")
    print(f"Prompt: {config['text']}")
    
    payload = {
        "text": config["text"],
        "duration_seconds": config["duration"],
        "prompt_influence": 0.3,
        "model_id": "eleven_text_to_sound_v2"
    }
    
    url = "https://api.elevenlabs.io/v1/sound-generation"
    audio_bytes = make_api_request(url, payload, api_key)
    
    output_path = output_dir / f"{cue_id}.mp3"
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path.write_bytes(audio_bytes)
    print(f"Successfully generated SFX: {output_path} ({len(audio_bytes)} bytes)")


def generate_voice(cue_id: str, api_key: str, output_dir: Path):
    """Generates voice clip using ElevenLabs /v1/text-to-speech/{voice_id}."""
    if cue_id not in VOICE_CATALOG:
        print(f"Error: Voice cue '{cue_id}' not found in catalog.", file=sys.stderr)
        sys.exit(1)
        
    config = VOICE_CATALOG[cue_id]
    character = config["character"]
    voice_id = CHARACTER_VOICE_MAP.get(character)
    
    if not voice_id:
        print(f"Error: No voice ID mapped for character '{character}'.", file=sys.stderr)
        sys.exit(1)
        
    print(f"Generating Voiceover '{cue_id}' ({character})...")
    print(f"Text: \"{config['text']}\"")
    
    payload = {
        "text": config["text"],
        "model_id": "eleven_monolingual_v1",
        "voice_settings": {
            "stability": 0.5,
            "similarity_boost": 0.8
        }
    }
    
    url = f"https://api.elevenlabs.io/v1/text-to-speech/{voice_id}"
    audio_bytes = make_api_request(url, payload, api_key)
    
    output_path = output_dir / f"{cue_id}.mp3"
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path.write_bytes(audio_bytes)
    print(f"Successfully generated Voiceover: {output_path} ({len(audio_bytes)} bytes)")


def list_catalog():
    """Prints all configured assets in the generator catalogs."""
    print("=== MUSIC CATALOG ===")
    for k, v in MUSIC_CATALOG.items():
        print(f"  * {k} (Length: {v['duration_ms'] / 1000}s)\n    Prompt: {v['prompt']}\n")
        
    print("=== SFX CATALOG ===")
    for k, v in SFX_CATALOG.items():
        print(f"  * {k} (Duration: {v['duration']}s)\n    Text: {v['text']}\n")
        
    print("=== VOICEOVER CATALOG ===")
    for k, v in VOICE_CATALOG.items():
        print(f"  * {k} ({v['character'].capitalize()})\n    Text: \"{v['text']}\"\n")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Starborn Audio Asset Generator (ElevenLabs API integration)."
    )
    parser.add_argument(
        "type",
        choices=["music", "sfx", "voice", "list"],
        help="Type of asset to generate, or 'list' to show configured items."
    )
    parser.add_argument(
        "name",
        nargs="?",
        help="ID of the asset to generate (e.g. 'music_w1_homestead_explore' or 'ui_click'). Ignored for 'list'."
    )
    parser.add_argument(
        "--key",
        help="ElevenLabs API Key override (can also be specified in ELEVENLABS_API_KEY env)."
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUTPUT_DIR,
        help=f"Directory to save generated audio files (default: {DEFAULT_OUTPUT_DIR})."
    )
    
    args = parser.parse_args()
    
    if args.type == "list":
        list_catalog()
        return 0
        
    if not args.name:
        print(f"Error: Asset ID required for generation type '{args.type}'. Use 'list' to see IDs.", file=sys.stderr)
        return 1
        
    api_key = get_api_key(args.key)
    output_dir = args.output_dir.resolve()
    
    if args.type == "music":
        generate_music(args.name, api_key, output_dir)
    elif args.type == "sfx":
        generate_sfx(args.name, api_key, output_dir)
    elif args.type == "voice":
        generate_voice(args.name, api_key, output_dir)
        
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
