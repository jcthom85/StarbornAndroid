#!/usr/bin/env python3
"""Lift a flat #00ff00 chroma-key background off a generated portrait.

`scripts/batch_generate_npcs.py` used to shell out to a copy of this living in a
per-machine Codex skills directory, which is not present on every workstation. It lives
in the repo now so portrait generation is reproducible.

The green is removed in three passes:

1. **Matte.** Distance from the key colour in a chroma plane (not plain RGB distance,
   which eats green in hair and cloth) drives alpha, with a soft band between the
   transparent and opaque thresholds so edges stay anti-aliased instead of stair-stepping.
2. **Flood-fill guard.** Only green connected to the border is cut. A green prop or a
   green highlight inside the subject survives, which plain thresholding would punch a
   hole through.
3. **Colour bleed.** The RGB *under* fully transparent pixels is still key-green, and
   bilinear filtering samples it: scaled down to a 60dp dialogue portrait, thin features
   pick up a green fringe. The opaque colours are extended outward into the cut so there
   is no green left to sample. This is why the pack saves alpha images with `exact=True`
   -- it preserves exactly this padding.

4. **Despill.** Green that bled onto the subject's edge is neutralised there. This is
   deliberately confined to a narrow band around the cut: applied to the whole image it
   does real damage, because Starborn subjects are legitimately green (moss, vines,
   foliage) and -- worse -- cyan resonance light has a high green channel, so a global
   clamp turns the game's signature cyan into flat blue.
"""

from __future__ import annotations

import argparse
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image


def build_matte(rgb: np.ndarray, key: np.ndarray, transparent: float, opaque: float) -> np.ndarray:
    """Alpha in 0..1 from distance to the key colour, with a soft ramp between thresholds."""
    diff = rgb.astype(np.float32) - key.astype(np.float32)
    # Weight green heavily: the key is green, and a plain euclidean distance would treat a
    # green-tinted skin shadow as background.
    distance = np.sqrt(
        (diff[..., 0] ** 2) * 1.0 + (diff[..., 1] ** 2) * 2.0 + (diff[..., 2] ** 2) * 1.0
    )
    if opaque <= transparent:
        opaque = transparent + 1.0
    alpha = (distance - transparent) / (opaque - transparent)
    return np.clip(alpha, 0.0, 1.0)


def border_connected(mask: np.ndarray) -> np.ndarray:
    """Mask of key-coloured pixels reachable from the image border (4-connected flood fill)."""
    height, width = mask.shape
    seen = np.zeros_like(mask, dtype=bool)
    queue: deque[tuple[int, int]] = deque()

    for x in range(width):
        for y in (0, height - 1):
            if mask[y, x] and not seen[y, x]:
                seen[y, x] = True
                queue.append((y, x))
    for y in range(height):
        for x in (0, width - 1):
            if mask[y, x] and not seen[y, x]:
                seen[y, x] = True
                queue.append((y, x))

    while queue:
        y, x = queue.popleft()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < height and 0 <= nx < width and mask[ny, nx] and not seen[ny, nx]:
                seen[ny, nx] = True
                queue.append((ny, nx))
    return seen


def dilate(mask: np.ndarray, radius: int) -> np.ndarray:
    """Binary dilation by `radius` using shifts -- avoids a scipy dependency."""
    out = mask.copy()
    for _ in range(radius):
        grown = out.copy()
        grown[1:, :] |= out[:-1, :]
        grown[:-1, :] |= out[1:, :]
        grown[:, 1:] |= out[:, :-1]
        grown[:, :-1] |= out[:, 1:]
        out = grown
    return out


def despill(rgb: np.ndarray, band: np.ndarray) -> np.ndarray:
    """Neutralise green spill, but only inside `band` (the pixels near the cut).

    Applying this globally is destructive: it flattens genuinely green subject matter and
    drags cyan -- which has a high green channel -- toward blue. Confining it to the edge
    band fixes the lime rim without touching the palette.
    """
    out = rgb.astype(np.float32)
    r, g, b = out[..., 0], out[..., 1], out[..., 2]
    limit = np.maximum(r, b)
    spill = band & (g > limit)
    out[..., 1] = np.where(spill, limit, g)
    return out


def bleed_rgb_outward(rgb: np.ndarray, opaque: np.ndarray, passes: int) -> np.ndarray:
    """Extend opaque colours into the transparent region so filtering has no key left to sample."""
    out = rgb.astype(np.float32)
    known = opaque.copy()
    for _ in range(passes):
        if known.all():
            break
        total = np.zeros_like(out)
        count = np.zeros(known.shape, dtype=np.float32)
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            shifted_rgb = np.roll(out, (dy, dx), axis=(0, 1))
            shifted_known = np.roll(known, (dy, dx), axis=(0, 1))
            total += shifted_rgb * shifted_known[..., None]
            count += shifted_known
        fillable = (~known) & (count > 0)
        safe = np.where(count > 0, count, 1.0)
        averaged = total / safe[..., None]
        out = np.where(fillable[..., None], averaged, out)
        known = known | fillable
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description="Remove a flat chroma-key background.")
    parser.add_argument("--input", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--key", default="#00ff00", help="Key colour (default: #00ff00)")
    parser.add_argument(
        "--transparent-threshold",
        type=float,
        default=40.0,
        help="Distance at or below which a pixel is fully transparent.",
    )
    parser.add_argument(
        "--opaque-threshold",
        type=float,
        default=190.0,
        help="Distance at or above which a pixel is fully opaque.",
    )
    parser.add_argument("--no-despill", action="store_true")
    parser.add_argument(
        "--bleed-passes",
        type=int,
        default=8,
        help="How far to extend opaque colour into the cut, in pixels (default: 8).",
    )
    parser.add_argument(
        "--despill-radius",
        type=int,
        default=3,
        help="How many pixels in from the cut to despill (default: 3). Keep this small.",
    )
    args = parser.parse_args()

    key_hex = args.key.lstrip("#")
    key = np.array([int(key_hex[i : i + 2], 16) for i in (0, 2, 4)], dtype=np.uint8)

    image = Image.open(args.input).convert("RGB")
    rgb = np.array(image)

    alpha = build_matte(rgb, key, args.transparent_threshold, args.opaque_threshold)

    # Only cut background that touches the border, so green inside the subject survives.
    reachable = border_connected(alpha < 1.0)
    alpha = np.where(reachable, alpha, 1.0)

    if args.no_despill:
        out_rgb = rgb
    else:
        band = dilate(alpha < 0.99, args.despill_radius) & (alpha > 0.0)
        out_rgb = despill(rgb, band)

    out_rgb = bleed_rgb_outward(out_rgb, alpha > 0.5, args.bleed_passes)

    rgba = np.dstack([out_rgb, alpha * 255.0]).astype(np.uint8)
    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(rgba, mode="RGBA").save(out_path)

    cut = float((alpha < 0.5).mean()) * 100.0
    print(f"saved {out_path} ({cut:.1f}% of the canvas made transparent)")


if __name__ == "__main__":
    main()
