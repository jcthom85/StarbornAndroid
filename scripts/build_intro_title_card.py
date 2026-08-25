#!/usr/bin/env python3
"""Composite the intro title card from the shipped Starborn logo.

The cold open cuts to black and lands on the title. Rather than generating
typography (unreliable, and it would drift from the brand), this reuses the
same wordmark the main menu uses: app/src/main/res/drawable/title_logo_starborn.png.

The card is deliberately near-black rather than the busy main-menu background,
so it reads as a hard cut out of the breach and into the title.

Output: world_assets/.../images/cinematics/intro_title_card_v1.webp at 944x1665,
matching the other intro stills.
"""

from pathlib import Path

from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
LOGO = ROOT / "app/src/main/res/drawable/title_logo_starborn.png"
OUT = ROOT / "world_assets/src/main/assets/images/cinematics/intro_title_card_v1.webp"

W, H = 944, 1665
LOGO_WIDTH_FRAC = 0.86      # logo width as a fraction of the card
LOGO_CENTRE_FRAC = 0.46     # slightly above centre reads better than dead centre


def main() -> None:
    card = Image.new("RGB", (W, H), (4, 6, 11))

    # Subtle cyan vertical falloff behind the wordmark so it does not sit on flat black.
    glow = Image.new("RGB", (W, H), (4, 6, 11))
    gpx = glow.load()
    cx, cy = W // 2, int(H * LOGO_CENTRE_FRAC)
    for y in range(H):
        d = abs(y - cy) / (H * 0.5)
        f = max(0.0, 1.0 - d) ** 3
        r = int(4 + 10 * f)
        g = int(6 + 30 * f)
        b = int(11 + 44 * f)
        for x in range(W):
            gpx[x, y] = (r, g, b)
    glow = glow.filter(ImageFilter.GaussianBlur(40))
    card = Image.blend(card, glow, 0.85)

    logo = Image.open(LOGO).convert("RGBA")
    target_w = int(W * LOGO_WIDTH_FRAC)
    scale = target_w / logo.width
    logo = logo.resize((target_w, int(logo.height * scale)), Image.LANCZOS)

    x = (W - logo.width) // 2
    y = int(H * LOGO_CENTRE_FRAC) - logo.height // 2
    card.paste(logo, (x, y), logo)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    # The asset pack ships WebP; emitting PNG here would orphan the cinematics.json path.
    card.save(OUT, "WEBP", quality=90, method=6)
    print(f"saved {OUT.relative_to(ROOT)} at {card.size[0]}x{card.size[1]}"
          f" ({OUT.stat().st_size / 1048576:.2f} MB)")


if __name__ == "__main__":
    main()
