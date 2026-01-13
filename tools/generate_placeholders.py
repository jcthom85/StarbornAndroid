import os
from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

# Configuration
ASSET_ROOT = Path("app/src/main/assets") # Moving to Android project assets folder
if not ASSET_ROOT.exists():
    # Fallback to local assets folder if android structure isn't standard or we are in a subfolder
    ASSET_ROOT = Path("assets")

ASSET_ROOT.mkdir(parents=True, exist_ok=True)

# Asset Definitions
ASSETS = {
    "icons/items": [
        {"name": "relic_tuning_fork", "color": (150, 50, 250), "text": "Tuning Fork"},
        {"name": "relic_bridge", "color": (50, 200, 100), "text": "Bridge"},
        {"name": "relic_lens", "color": (50, 100, 250), "text": "Lens"},
        {"name": "relic_anvil", "color": (250, 100, 50), "text": "Anvil"},
        {"name": "relic_anchor", "color": (20, 20, 50), "text": "Anchor"},
        {"name": "relic_key", "color": (250, 250, 200), "text": "Key"},
        {"name": "item_chime_cell", "color": (100, 250, 250), "text": "Chime Cell"},
        {"name": "item_mining_pass", "color": (200, 200, 200), "text": "Mining Pass"},
    ],
    "portraits/npcs": [
        {"name": "nova", "color": (200, 100, 100), "text": "Nova\n(Protagonist)"},
        {"name": "zeke", "color": (100, 150, 250), "text": "Zeke\n(Support)"},
        {"name": "orion", "color": (200, 250, 250), "text": "Orion\n(Healer)"},
        {"name": "gh0st", "color": (50, 50, 50), "text": "Gh0st\n(Tank)"},
        {"name": "jed", "color": (150, 100, 50), "text": "Jed\n(Mentor)"},
        {"name": "warden", "color": (100, 0, 0), "text": "The Warden\n(Boss)"},
        {"name": "mara_thorne", "color": (200, 200, 250), "text": "Mara Thorne\n(CEO)"},
        {"name": "arden_vale", "color": (100, 100, 150), "text": "Arden Vale\n(Lt)"},
        {"name": "scrapper", "color": (100, 100, 100), "text": "Scrapper\n(Fence)"},
        {"name": "doc", "color": (250, 250, 250), "text": "Doc\n(Medic)"},
        {"name": "foreman_bogs", "color": (150, 150, 0), "text": "Bogs\n(Foreman)"},
    ],
    "backgrounds/hubs": [
        {"name": "hub_1_homestead", "color": (50, 30, 30), "text": "The Pit\n(Homestead)"},
        {"name": "hub_2_logistics", "color": (30, 30, 50), "text": "Logistics\nSector"},
    ],
    "backgrounds/rooms": [
        {"name": "jeds_workshop", "color": (80, 60, 40), "text": "Jed's Workshop"},
        {"name": "trade_row", "color": (60, 40, 40), "text": "Trade Row"},
        {"name": "med_bay", "color": (200, 200, 200), "text": "Med-Bay"},
        {"name": "shadow_alley", "color": (20, 20, 20), "text": "Shadow Alley"},
        {"name": "admin_concourse", "color": (100, 100, 120), "text": "Admin Concourse"},
        {"name": "deep_elevator", "color": (40, 20, 20), "text": "Deep Elevator"},
        {"name": "old_tunnels", "color": (30, 10, 10), "text": "Old Tunnels"},
        {"name": "echo_chamber", "color": (50, 0, 50), "text": "Echo Chamber\n(Relic Room)"},
        {"name": "launch_bay", "color": (0, 0, 20), "text": "Launch Bay\n(Boss Arena)"},
        {"name": "ship_bridge", "color": (50, 80, 100), "text": "Astra Bridge"},
        {"name": "ship_common", "color": (100, 80, 60), "text": "Astra Common"},
        {"name": "ship_workshop", "color": (60, 60, 80), "text": "Astra Workshop"},
        {"name": "ship_quarters", "color": (80, 80, 100), "text": "Astra Quarters"},
    ]
}

def generate_image(path, name, color, text):
    # Dimensions
    if "icons" in str(path):
        w, h = 256, 256
    elif "portraits" in str(path):
        w, h = 512, 768
    else: # backgrounds
        w, h = 1080, 1920

    img = Image.new('RGB', (w, h), color)
    draw = ImageDraw.Draw(img)
    
    # Border
    draw.rectangle([(10, 10), (w-10, h-10)], outline=(255, 255, 255), width=5)
    
    # Text
    # Try to load a font, fallback to default
    try:
        font = ImageFont.truetype("arial.ttf", size=40)
    except IOError:
        font = ImageFont.load_default()

    # Calculate text position (centered)
    # Pillow's default font doesn't support getbbox well for size, so we'll just approximate center
    # For a real font we'd use textbbox
    
    draw.text((w//2, h//2), text, fill=(255, 255, 255), font=font, anchor="mm", align="center")
    
    # Save
    full_path = path / f"{name}.png"
    img.save(full_path)
    print(f"Generated: {full_path}")

def main():
    for category, items in ASSETS.items():
        folder = ASSET_ROOT / category
        folder.mkdir(parents=True, exist_ok=True)
        for item in items:
            generate_image(folder, item["name"], item["color"], item["text"])

if __name__ == "__main__":
    main()
