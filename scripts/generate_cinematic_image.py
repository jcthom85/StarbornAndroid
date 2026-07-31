#!/usr/bin/env python3
"""Generate a Starborn cinematic still.

Follows docs/story/Starborn_Art_Production_Guide.md: gpt-image-2 at quality=low.
Generates portrait and resizes to 944x1665 to match the existing intro_* stills
(same 0.567 aspect as the guide's 1088x1920 portrait spec).

The API key is read from openai_api_key.txt or OPENAI_API_KEY and is never
printed, logged, or written anywhere.

Usage:
    python scripts/generate_cinematic_image.py --name intro_beast_glass_v1 --prompt-file <file>
"""

import argparse
import base64
import io
import os
import sys
from pathlib import Path

from openai import OpenAI
from PIL import Image

PROJECT_ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = PROJECT_ROOT / "world_assets/src/main/assets/images/cinematics"
TARGET = (944, 1665)
GEN_SIZE = "1024x1536"


def api_key() -> str:
    if os.environ.get("OPENAI_API_KEY"):
        return os.environ["OPENAI_API_KEY"]
    f = PROJECT_ROOT / "openai_api_key.txt"
    if f.exists():
        return f.read_text(encoding="utf-8").strip()
    sys.exit("No OpenAI API key found (OPENAI_API_KEY or openai_api_key.txt).")


def fit(img: Image.Image, size: tuple[int, int]) -> Image.Image:
    """Cover-fit: scale to fill, then centre-crop. Preserves composition centre."""
    tw, th = size
    sw, sh = img.size
    scale = max(tw / sw, th / sh)
    nw, nh = int(round(sw * scale)), int(round(sh * scale))
    img = img.resize((nw, nh), Image.LANCZOS)
    left, top = (nw - tw) // 2, (nh - th) // 2
    return img.crop((left, top, left + tw, top + th))


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--name", required=True, help="Output basename, no extension")
    ap.add_argument("--prompt-file", required=True, help="File containing the prompt")
    args = ap.parse_args()

    prompt = Path(args.prompt_file).read_text(encoding="utf-8").strip()
    client = OpenAI(api_key=api_key())

    print(f"Generating {args.name} ({GEN_SIZE}, quality=low)...")
    resp = client.images.generate(
        model="gpt-image-2",
        prompt=prompt,
        n=1,
        size=GEN_SIZE,
        quality="low",
    )

    data = resp.data[0]
    if not data.b64_json:
        sys.exit("No image data returned.")
    img = Image.open(io.BytesIO(base64.b64decode(data.b64_json))).convert("RGB")
    print(f"  received {img.size[0]}x{img.size[1]}")

    img = fit(img, TARGET)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    out = OUT_DIR / f"{args.name}.png"
    img.save(out, "PNG")
    print(f"  saved {out.relative_to(PROJECT_ROOT)} at {img.size[0]}x{img.size[1]}"
          f" ({out.stat().st_size / 1048576:.1f} MB)")


if __name__ == "__main__":
    main()
