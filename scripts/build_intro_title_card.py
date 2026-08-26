#!/usr/bin/env python3
"""Composite the intro title card from the shipped Starborn logo.

The cold open cuts to black and lands on the title. Rather than generating
typography (unreliable, and it would drift from the brand), this reuses the
same wordmark the main menu uses: app/src/main/res/drawable-nodpi/title_logo_starborn.webp.

Output: world_assets/.../images/cinematics/intro_title_card_v1.webp at 944x1665,
matching the other intro stills.
"""

import math
from pathlib import Path
import random

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
LOGO = ROOT / "app/src/main/res/drawable-nodpi/title_logo_starborn.webp"
OUT = ROOT / "world_assets/src/main/assets/images/cinematics/intro_title_card_v1.webp"

W, H = 944, 1665
LOGO_WIDTH_FRAC = 0.73       # 73% width gives elegant cinematic golden-ratio margins
LOGO_CENTRE_FRAC = 0.48      # Centered perfectly at the visual centroid of the logo


def main() -> None:
    # 1. Base dark cosmic background
    bg = Image.new("RGB", (W, H), (3, 5, 9))

    # Radial cosmic nebula aura
    cx, cy = W // 2, int(H * LOGO_CENTRE_FRAC)
    nebula = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ndraw = ImageDraw.Draw(nebula)

    # Multi-layer radial glow
    # Outer deep teal/cyan glow
    for r in range(400, 50, -20):
        alpha = int(35 * (1.0 - r / 400.0) ** 1.5)
        ndraw.ellipse([cx - r * 1.2, cy - r * 0.7, cx + r * 1.2, cy + r * 0.7], fill=(0, 140, 220, alpha))

    # Mid cyan resonance bloom
    for r in range(220, 20, -15):
        alpha = int(55 * (1.0 - r / 220.0) ** 2)
        ndraw.ellipse([cx - r * 1.1, cy - r * 0.65, cx + r * 1.1, cy + r * 0.65], fill=(30, 200, 255, alpha))

    # Inner bright core
    for r in range(90, 5, -8):
        alpha = int(75 * (1.0 - r / 90.0) ** 2)
        ndraw.ellipse([cx - r, cy - r * 0.6, cx + r, cy + r * 0.6], fill=(160, 240, 255, alpha))

    nebula = nebula.filter(ImageFilter.GaussianBlur(30))
    bg.paste(Image.alpha_composite(Image.new("RGBA", (W, H), (3, 5, 9, 255)), nebula).convert("RGB"))

    # Subtle cosmic starfield
    rnd = random.Random(42)
    star_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(star_layer)
    for _ in range(120):
        sx = rnd.randint(20, W - 20)
        sy = rnd.randint(40, H - 40)
        dist = math.hypot(sx - cx, (sy - cy) * 1.5)
        size = rnd.choice([1, 1, 1, 2])
        brightness = rnd.randint(60, 200)
        if dist < 180:
            brightness = int(brightness * 0.3)
        sdraw.ellipse([sx, sy, sx + size, sy + size], fill=(brightness, brightness + rnd.randint(0, 40), 255, int(brightness * 0.8)))

    # Anamorphic horizontal streak through the star
    streak = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    kdraw = ImageDraw.Draw(streak)
    streak_w = int(W * 0.85)
    streak_h = 4
    kdraw.ellipse([cx - streak_w // 2, cy - streak_h // 2, cx + streak_w // 2, cy + streak_h // 2], fill=(120, 220, 255, 90))
    streak = streak.filter(ImageFilter.GaussianBlur(8))

    card = Image.alpha_composite(bg.convert("RGBA"), star_layer)
    card = Image.alpha_composite(card, streak)

    # 2. Resize and place Starborn Logo
    logo = Image.open(LOGO).convert("RGBA")
    target_w = int(W * LOGO_WIDTH_FRAC)
    scale = target_w / logo.width
    target_h = int(logo.height * scale)
    logo = logo.resize((target_w, target_h), Image.LANCZOS)

    # Soft logo outer glow / shadow for separation
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    lx = (W - target_w) // 2
    ly = int(H * LOGO_CENTRE_FRAC) - target_h // 2

    shadow.paste(logo, (lx, ly), logo)
    shadow = shadow.filter(ImageFilter.GaussianBlur(16))
    spx = shadow.load()
    for y in range(H):
        for x in range(W):
            a = spx[x, y][3]
            if a > 0:
                spx[x, y] = (0, 180, 255, int(a * 0.45))

    card = Image.alpha_composite(card, shadow)
    card.paste(logo, (lx, ly), logo)

    final_rgb = card.convert("RGB")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    final_rgb.save(OUT, "WEBP", quality=92, method=6)
    print(f"saved {OUT.relative_to(ROOT)} at {final_rgb.size[0]}x{final_rgb.size[1]}"
          f" ({OUT.stat().st_size / 1024:.1f} KB)")


if __name__ == "__main__":
    main()
