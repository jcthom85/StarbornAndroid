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
    "amb_intro_containment_pressure": {
        "text": "Seamless loopable underground containment-lab ambience, deep ventilation pressure, distant stressed steel groans, low hydraulic vibration, faint electrical relay chatter, restrained and ominous, no impacts, no voices, no alarm, no music",
        "duration": 30.0,
    },
    "sfx_intro_door_buckle": {
        "text": "Massive reinforced pressure doors bow inward under one brutal impact, deep steel buckle, bolts strain and snap, dust falls, short concrete underground reverb, no creature vocalization, no music",
        "duration": 2.0,
    },
    "sfx_intro_chime_launch": {
        "text": "Heavy pneumatic emergency tube fires a compact brass device, clamp release, compressed-air blast, metal carrier accelerates down a pipe, one brief clean cyan-like resonant chime in the tail, no music",
        "duration": 2.5,
    },
    "sfx_intro_stasis_seal": {
        "text": "Industrial stasis pod closes and pressure-seals, segmented metal clamps lock in sequence, thick glass enclosure, hydraulic hiss, power relay drops to a low hum, no voice, no music",
        "duration": 2.2,
    },
    "sfx_intro_shift_buzzer": {
        "text": "Harsh old factory shift buzzer, one compact electromechanical blast through a cheap wall speaker, slight metal-room rattle, dry abrupt cutoff, no voice, no music",
        "duration": 1.5,
    },
    "ui_room_move": {
        "text": "Very short soft movement whoosh, quick rush of air with subtle cloth and light brushed-metal texture, grounded physical travel, restrained transient, clean short tail, no impact, no musical tone, no futuristic laser",
        "duration": 0.5,
    },
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
    "quest_new_stinger": {
        "text": "A short JRPG quest accepted stinger, warm acoustic chime over a dry metal tick, hopeful upward motion, dusty sci-fi interface, clean 1 second tail",
        "duration": 1.4,
    },
    "quest_complete_stinger": {
        "text": "A short JRPG quest complete stinger, warm acoustic guitar harmonic and soft brass swell, dry industrial room tone, satisfying resolution, clean 2 second tail",
        "duration": 2.2,
    },
    "quest_update_tick": {
        "text": "A compact quest log update tick, dry mechanical relay click followed by a small wooden chime, subtle positive UI feedback, very short tail",
        "duration": 0.7,
    },
    "sfx_bunk_light_on": {
        "text": "A dusty bunkroom light switch clicks on, tired fluorescent tube buzzes and warms up, small electrical pop, dry cramped metal room, no rain, no storm",
        "duration": 1.2,
    },
    "sfx_bunk_light_off": {
        "text": "A dusty bunkroom light switch clicks off, fluorescent buzz drops into silence, tiny electrical tick, dry cramped metal room, no rain, no storm",
        "duration": 0.9,
    },
    "sfx_terminal_boot": {
        "text": "A Dominion terminal boots under cold corporate power, dry relay clicks, soft CRT hum, tiny data chirps, sterile sci-fi console, no melody",
        "duration": 1.6,
    },
    "sfx_workshop_success": {
        "text": "A tinkering bench repair succeeds, small ratchet twist, solder pop, warm metal chime, dusty workshop acoustics, satisfying handcrafted sci-fi gadget sound",
        "duration": 1.4,
    },
    "sfx_security_lockdown": {
        "text": "A Dominion lockdown alarm sting, heavy blast door thud, red warning siren pulse, dry industrial hallway, urgent but short, no music",
        "duration": 2.0,
    },
    "sfx_warden_entry": {
        "text": "A massive security mech powers up, heavy servo rise, low metal footstep impact, hydraulic pressure release, intimidating industrial boss entrance, short tail",
        "duration": 2.3,
    },
    "sfx_chime_splice": {
        "text": "A relic chime splices into a sci-fi console, crystalline resonance overtakes dry circuitry, harmonic shimmer, deep sub pressure, clean magical technology sound",
        "duration": 2.0,
    },
    "sfx_pod_launch": {
        "text": "A mining escape pod launches from a rail, hydraulic clamps release, engine ignition roar, metal tunnel rush, urgent dry industrial sci-fi launch",
        "duration": 3.0,
    },
    "sfx_crash_impact": {
        "text": "A violent escape pod crash impact, metal hull buckles, glass-safe debris scatter, deep sub hit, abrupt air pressure drop, cinematic short tail",
        "duration": 2.0,
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
    "battle_weakness_resolve": {
        "text": "A clean acoustic harmonic resolution for a JRPG weakness hit, two glass chimes forming a perfect fifth, gentle cello harmonic bloom, bright satisfying resonance, no synth, no explosion",
        "duration": 0.9,
    },
    "battle_stability_shatter": {
        "text": "A glass singing bowl shatters under deep ocean pressure, crystalline bell fracture, sudden muffled underwater pressure drop, low sub swell, dramatic guard break sound, no melody",
        "duration": 1.3,
    },
    "battle_erosion_warning": {
        "text": "A subtle high tinnitus ring with detuned singing bowl dissonance, mental fatigue warning, thin glass resonance and faint crackle, restrained and uncomfortable, no alarm beep",
        "duration": 1.8,
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
    "amb_bunkroom_hum": {
        "text": "Very quiet loopable sci-fi sleeping alcove ambience, low ventilation hum behind metal walls, distant muffled colony machinery, occasional soft pipe creak, dry cramped bunkroom, no melody, no storm, no rain",
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
    "amb_colony_vent_crowd": {
        "text": "Loopable working-class sci-fi colony ambience, low ventilation rumble, distant muffled miners talking, soft metal creaks, dry dusty air, no melody",
        "duration": 30.0,
    },
    "amb_logistics_machinery": {
        "text": "Loopable industrial logistics ambience, heavy conveyor motors, distant cargo lifts, dry metal clanks, low electrical hum, corporate mine machinery, no melody",
        "duration": 30.0,
    },
    "amb_server_cold": {
        "text": "Loopable cold server room ambience, dense computer fan wash, frosty air vents, subtle electrical ticks, sterile sci-fi data center hum, no melody",
        "duration": 30.0,
    },
    "amb_launch_alarm": {
        "text": "Loopable emergency launch bay ambience, distant warning sirens, cargo rail power hum, hydraulic lift strain, metal vibration, urgent sci-fi industrial atmosphere, no melody",
        "duration": 30.0,
    },
    "amb_dust_storm": {
        "text": "Loopable Stellarium mine dust storm ambience, low rushing wind through hollow pipes, dry sandy grit scratching metal walls, distant industrial hum, no melody",
        "duration": 30.0,
    },
    "amb_w2_crash_jungle": {
        "text": "Loopable Sector 9 crash-site jungle ambience, humid bioluminescent forest at night, distant insects, soft alien birds, damaged escape pod cooling ticks, low ancient resonance under the trees, no melody",
        "duration": 30.0,
    },
    "amb_w2_glow_stream": {
        "text": "Loopable glowing stream ambience, gentle water flowing over glassy stones, crystalline water tinkles, soft harmonic chord in the current, warm jungle air, distant insects, no melody",
        "duration": 30.0,
    },
    "amb_w2_tideglass_shore": {
        "text": "Loopable Tideglass shore ambience, slow heavy waves on glass-like sand, crystalline retreating water, sea cave breath, soft wind through ruins, mysterious but calm, no melody",
        "duration": 30.0,
    },
    "amb_w2_canopy_resonance": {
        "text": "Loopable high jungle canopy ambience, leaves moving in warm wind, distant wildlife calls, hollow stone archways filtering wind into soft chords, faint Source resonance, no melody",
        "duration": 30.0,
    },
    "amb_w2_architect_facility": {
        "text": "Loopable ancient Architect facility ambience, cold stone halls, quiet cymatic hum, distant water drips, subtle electrical relic pulses, wide resonant reverb, no melody",
        "duration": 30.0,
    },
    "amb_w2_stasis_chamber": {
        "text": "Loopable stasis chamber ambience, deep suspended pod hum, slow ring rotation, cold air vents, soft harmonic pressure, ancient medical machinery asleep but alive, no melody",
        "duration": 30.0,
    },
    "amb_w2_hangar_power": {
        "text": "Loopable ruined hangar ambience, old ship bay power cycling weakly, distant hydraulic groans, loose cables sparking quietly, cavernous metal and stone space, no melody",
        "duration": 30.0,
    },
    "amb_astra_interior": {
        "text": "Loopable small starship interior ambience, steady life support hum, soft console pings, gentle hull creaks, warm enclosed cabin, repaired but old spacecraft, no melody",
        "duration": 30.0,
    },
    "amb_w3_sewers": {
        "text": "Loopable Spire sewer ambience, wet concrete tunnel, dirty runoff trickling, distant city machinery overhead, low ventilation rumble, occasional pipe knock, claustrophobic, no melody",
        "duration": 30.0,
    },
    "amb_w3_rain_alley": {
        "text": "Loopable neon city rain alley ambience, rain on metal awnings, distant traffic hum, buzzing signs, muffled voices through walls, lower-city grime, late night, no melody",
        "duration": 30.0,
    },
    "amb_w3_underrail_static": {
        "text": "Loopable abandoned underrail platform ambience, distant train thunder, electrical static, old speaker crackle, dripping ceiling, concrete tunnel air, tense but not musical",
        "duration": 30.0,
    },
    "amb_w3_night_market": {
        "text": "Loopable crowded night market ambience in rain, muffled vendors, sizzling food stall, lantern wires creaking, soft synth busker far away, human warmth under corporate surveillance",
        "duration": 30.0,
    },
    "amb_w3_checkpoint": {
        "text": "Loopable Dominion transit checkpoint ambience, scanner gates humming, security drones passing overhead, PA system static, orderly crowd murmur, cold corporate control, no melody",
        "duration": 30.0,
    },
    "amb_w3_upper_service": {
        "text": "Loopable upper-city service corridor ambience, polished ventilation, laundry machinery, quiet elevator motors, distant luxury crowd through thick walls, sterile expensive calm, no melody",
        "duration": 30.0,
    },
    "amb_w3_skypark": {
        "text": "Loopable upper-city skypark ambience, high-altitude wind against glass, artificial garden water, distant soft drones, luxury dome ventilation, beautiful but controlled, no melody",
        "duration": 30.0,
    },
    "amb_w3_corporate_archive": {
        "text": "Loopable corporate archive ambience, pristine glass gallery, quiet surveillance drones, soft data servers, polished room reverb, cold curated silence, no melody",
        "duration": 30.0,
    },
    "sfx_w2_chime_gate": {
        "text": "An ancient chime slots into a stone gate, crystalline harmonic lock turns, heavy stone breath opens, warm Source resonance blooms then settles, short magical technology sound",
        "duration": 2.0,
    },
    "sfx_w2_stasis_release": {
        "text": "Ancient stasis pod releases, deep pod seal unlatched, cold vapor, rotating rings slow to a stop, harmonic pressure dissolves, emotional sci-fi awakening sting",
        "duration": 2.5,
    },
    "sfx_w2_astra_reboot": {
        "text": "Old starship systems reboot, power conduits lock in, console relays wake, life support rises, warm engine core pulse, satisfying repaired spacecraft sound",
        "duration": 2.2,
    },
    "sfx_w2_astra_launch": {
        "text": "Small repaired starship launches from ruined hangar, clamps release, engines spool, metal bay resonance, rushing ascent through shield turbulence, cinematic but short",
        "duration": 3.0,
    },
    "sfx_w3_shield_anchor": {
        "text": "Improvised safehouse shield comes online, electric relay snap, soft protective resonance expands, rain muffles outside, warm barrier hum stabilizes, hopeful short sci-fi sting",
        "duration": 2.0,
    },
    "sfx_w3_elevator_rise": {
        "text": "Glass elevator rises from lower city to upper city, smooth magnetic lift, city rumble falling away, pressure change, elegant corporate ascent, short transition sound",
        "duration": 2.4,
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
    "wpn_nova_resonance_shot": {
        "text": "A compact acoustic resonance shot, plucked glass harp transient, small crystal bow snap, clean focused chime projectile, bright but physical, no laser, no synth",
        "duration": 0.7,
    },
    "wpn_zeke_body_impact": {
        "text": "A short percussive body blow with leather wrap, wooden staff thump, chesty breath impact, dry close room, physical martial hit, no gore",
        "duration": 0.6,
    },
    "wpn_orion_bowl_resonance": {
        "text": "A deep singing bowl weapon pulse, low brass breath, cello harmonic swell, resonant acoustic pressure wave, ancient and controlled, no electronic zap",
        "duration": 1.0,
    },
    "wpn_gh0st_wire_slash": {
        "text": "A clipped wire-and-blade slash, taut metal string pluck, whispery air cut, tiny glass scrape tail, stealthy acoustic strike, no sci-fi laser",
        "duration": 0.65,
    },
    "voice_murmur_nova_01": {
        "text": "A very short soft feminine nonverbal vocal murmur, melodic minor-third hum, warm breath, no words, no singing phrase",
        "duration": 0.5,
    },
    "voice_murmur_nova_02": {
        "text": "A tiny soft feminine nonverbal dialogue murmur, gentle rising hum, human breath, intimate and calm, no words",
        "duration": 0.5,
    },
    "voice_murmur_nova_03": {
        "text": "A very brief soft feminine nonverbal hm sound, minor-third color, close microphone, natural breath, no words",
        "duration": 0.5,
    },
    "voice_murmur_nova_04": {
        "text": "A short soft feminine nonverbal dialogue hum, warm melodic interval, quiet and expressive, no words",
        "duration": 0.5,
    },
    "voice_murmur_orion_01": {
        "text": "A very short low masculine nonverbal vocal hum, bass resonance, calm mentor tone, no words, no singing phrase",
        "duration": 0.5,
    },
    "voice_murmur_orion_02": {
        "text": "A tiny deep masculine dialogue murmur, resonant chest hum, slow and grounded, no words",
        "duration": 0.5,
    },
    "voice_murmur_orion_03": {
        "text": "A brief low masculine nonverbal hm, bass-frequency warmth, restrained, no words",
        "duration": 0.5,
    },
    "voice_murmur_orion_04": {
        "text": "A short deep masculine vocal murmur, soft resonant bass hum, thoughtful cadence, no words",
        "duration": 0.5,
    },
    "voice_murmur_zeke_01": {
        "text": "A very short masculine nonverbal dialogue grunt, percussive rhythmic breath, friendly rugged tone, no words",
        "duration": 0.5,
    },
    "voice_murmur_zeke_02": {
        "text": "A tiny masculine hm with clipped rhythm, warm rough breath, conversational, no words",
        "duration": 0.5,
    },
    "voice_murmur_zeke_03": {
        "text": "A brief percussive masculine vocal tick, rugged low murmur, no words",
        "duration": 0.5,
    },
    "voice_murmur_zeke_04": {
        "text": "A short masculine nonverbal dialogue murmur, rhythmic breath and throat tone, warm and practical, no words",
        "duration": 0.5,
    },
    "voice_murmur_gh0st_01": {
        "text": "A very short clipped masculine whisper murmur, dry breath, stealthy tone, no words, no electronic glitch",
        "duration": 0.5,
    },
    "voice_murmur_gh0st_02": {
        "text": "A tiny low whispery nonverbal hm, clipped and controlled, close microphone, no words",
        "duration": 0.5,
    },
    "voice_murmur_gh0st_03": {
        "text": "A brief dry whisper-breath dialogue murmur, tense and precise, no words",
        "duration": 0.5,
    },
    "voice_murmur_gh0st_04": {
        "text": "A short clipped masculine nonverbal murmur, airy whisper with faint throat tone, stealthy, no words",
        "duration": 0.5,
    },
}

# Voiceover scripts mapping
VOICE_CATALOG = {
    "shop_mechanic_greeting": {
        "character": "zeke",
        "text": "Hey there, Nova! I've got parts polished and ready to bolt on.",
    },
    "shop_mechanic_smalltalk": {
        "character": "zeke",
        "text": "Scavvers dragged in a crate of micro-cores, perfect for overclocking your drone.",
    },
    "shop_weapon_greeting": {
        "character": "gh0st",
        "text": "Looking to upgrade your bite? I've got fresh steel and plasma.",
    },
    "shop_armor_greeting": {
        "character": "zeke",
        "text": "Step in and suit up. No frontier scrape should catch you unshielded.",
    },
    "shop_general_greeting": {
        "character": "nova",
        "text": "Supplies, snacks, emergency fixes. If you need it, I stock it.",
    },
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
