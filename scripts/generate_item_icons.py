#!/usr/bin/env python3
"""
Starborn Item Icon Production Pipeline.

Generates 1024x1024 item icons via OpenAI `gpt-image-2` (quality=low, canonical Starborn default),
removes the flat #00ff00 chroma-key background (both border-connected via scripts/remove_chroma_key.py
and enclosed pure-green pockets like trigger guards/rings/gaps), bleeds opaque RGB outward into
transparent pixels, centers into 256x256 transparent WebP (quality=90, method=6, exact=True),
installs into world_assets/src/main/assets/images/items/<id>.webp, and renders QA contact sheets.
"""
import argparse
import base64
import json
import subprocess
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

import numpy as np
import requests
from PIL import Image, ImageDraw

PROJECT_ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "items"
TMP_DIR = PROJECT_ROOT / "tmp" / "item_icons"
CHROMA_SCRIPT = PROJECT_ROOT / "scripts" / "remove_chroma_key.py"

STYLE_TAIL = (
    "Style/medium: Starborn 2D RPG item icon style — colorful chibi-anime/comic sci-fi adventure, "
    "thick crisp dark-navy/black ink outer outline, chunky slightly exaggerated iconic silhouette, "
    "glossy cel shading with controlled painted texture, low micro-detail so it reads clearly at 32-56dp on a phone. "
    "Composition/framing: Single isolated item centered with generous 14% empty padding on all sides, nothing cropped or touching edges. "
    "Constraints: Create the subject on a perfectly flat solid #00ff00 chroma-key green background for background removal. "
    "Uniform single-color background with no gradients, no floor plane, no cast shadow, no contact shadow, no glow halo bleeding onto the green, "
    "no frame, no UI border, no watermark, and no text or letters. Do NOT use pure bright green anywhere in the subject."
)


def clean_interior_chroma_green(png_path: Path, webp_path: Path) -> int:
    """Remove enclosed pure-#00ff00 pockets (e.g. trigger guards, handles, rings),
    despill adjacent edge pixels, bleed opaque RGB outward, and save centered 256x256 WebP."""
    im = Image.open(png_path).convert("RGBA")
    arr = np.array(im, dtype=np.int16)
    r, g, b, a = arr[..., 0], arr[..., 1], arr[..., 2], arr[..., 3]
    green_excess = g - np.maximum(r, b)
    pocket_core = (a > 0) & (g > 175) & (green_excess > 80)

    if pocket_core.any():
        h, w = a.shape
        mask = pocket_core.copy()
        dil1 = np.zeros_like(mask)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                ys = slice(max(0, dy), h + min(0, dy))
                xs = slice(max(0, dx), w + min(0, dx))
                yd = slice(max(0, -dy), h + min(0, -dy))
                xd = slice(max(0, -dx), w + min(0, -dx))
                dil1[yd, xd] |= mask[ys, xs]
        fringe = dil1 & ~mask & (a > 0) & (green_excess > 22)

        dil3 = dil1.copy()
        for _ in range(2):
            nxt = dil3.copy()
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    ys = slice(max(0, dy), h + min(0, dy))
                    xs = slice(max(0, dx), w + min(0, dx))
                    yd = slice(max(0, -dy), h + min(0, -dy))
                    xd = slice(max(0, -dx), w + min(0, -dx))
                    nxt[yd, xd] |= dil3[ys, xs]
            dil3 = nxt

        arr[mask, 3] = 0
        arr[fringe, 3] = np.clip(arr[fringe, 3] - (green_excess[fringe] * 2), 0, 255)
        despill = dil3 & (arr[..., 3] > 0) & (green_excess > 10)
        arr[despill, 1] = np.minimum(arr[despill, 1], np.maximum(arr[despill, 0], arr[despill, 2]) + 8)

        rgba = np.clip(arr, 0, 255).astype(np.uint8)
        for _ in range(4):
            trans = rgba[..., 3] == 0
            if not trans.any():
                break
            sum_rgb = np.zeros((h, w, 3), dtype=np.int32)
            cnt = np.zeros((h, w), dtype=np.int32)
            for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                ys = slice(max(0, dy), h + min(0, dy))
                xs = slice(max(0, dx), w + min(0, dx))
                yd = slice(max(0, -dy), h + min(0, -dy))
                xd = slice(max(0, -dx), w + min(0, -dx))
                src_op = rgba[ys, xs, 3] > 0
                sum_rgb[yd, xd] += np.where(src_op[..., None], rgba[ys, xs, :3], 0)
                cnt[yd, xd] += src_op.astype(np.int32)
            fill = trans & (cnt > 0)
            if not fill.any():
                break
            rgba[fill, :3] = (sum_rgb[fill] // cnt[fill, None]).astype(np.uint8)
        im = Image.fromarray(rgba)
        im.save(png_path)

    bbox = im.getbbox()
    if bbox:
        cropped = im.crop(bbox)
        cw, ch = cropped.size
        target_inner = 216
        scale = min(target_inner / cw, target_inner / ch)
        nw, nh = max(1, int(round(cw * scale))), max(1, int(round(ch * scale)))
        resized = cropped.resize((nw, nh), Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
        canvas.paste(resized, ((256 - nw) // 2, (256 - nh) // 2), resized)
        canvas.save(webp_path, "WEBP", quality=90, method=6, exact=True)
    else:
        im.resize((256, 256), Image.Resampling.LANCZOS).save(webp_path, "WEBP", quality=90, method=6, exact=True)
    return webp_path.stat().st_size


def generate_one(api_key: str, item_id: str, subject_desc: str, force: bool = False) -> tuple[str, str]:
    webp_path = OUT_DIR / f"{item_id}.webp"
    if webp_path.exists() and not force:
        return item_id, f"SKIP (exists, {webp_path.stat().st_size} B)"

    keyed_path = TMP_DIR / f"{item_id}_keyed.png"
    clean_png = TMP_DIR / f"{item_id}.png"
    prompt = (
        f"Use case: stylized-concept. Asset type: transparent mobile RPG item icon. "
        f"Subject: {subject_desc} "
        f"{STYLE_TAIL}"
    )

    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
    }
    last_err = None
    for attempt in range(3):
        try:
            r = requests.post(
                "https://api.openai.com/v1/images/generations",
                headers=headers,
                json={
                    "model": "gpt-image-2",
                    "prompt": prompt,
                    "size": "1024x1024",
                    "quality": "low",
                    "n": 1,
                },
                timeout=120,
            )
            if r.status_code == 200:
                data = r.json()["data"][0]
                if data.get("b64_json"):
                    keyed_path.write_bytes(base64.b64decode(data["b64_json"]))
                else:
                    keyed_path.write_bytes(requests.get(data["url"], timeout=60).content)
                break
            elif r.status_code in (429, 500, 502, 503, 504):
                last_err = f"HTTP {r.status_code}: {r.text[:180]}"
                time.sleep(2.5 * (attempt + 1))
            else:
                return item_id, f"ERROR HTTP {r.status_code}: {r.text[:220]}"
        except Exception as exc:
            last_err = str(exc)
            time.sleep(2.0 * (attempt + 1))
    else:
        return item_id, f"ERROR after retries: {last_err}"

    subprocess.run(
        [sys.executable, str(CHROMA_SCRIPT), "--input", str(keyed_path), "--out", str(clean_png)],
        check=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    size_bytes = clean_interior_chroma_green(clean_png, webp_path)
    return item_id, f"OK ({size_bytes} B)"


def render_contact_sheets(item_ids: list[str], sheet_dir: Path, prefix: str, cols: int = 5, per_sheet: int = 25) -> list[Path]:
    sheet_dir.mkdir(parents=True, exist_ok=True)
    paths: list[Path] = []
    for page in range(0, len(item_ids), per_sheet):
        chunk = item_ids[page:page + per_sheet]
        rows = (len(chunk) + cols - 1) // cols
        card_w, card_h = 204, 212
        pad = 16
        w = pad + cols * (card_w + pad)
        h = pad + rows * (card_h + pad)
        sheet = Image.new("RGB", (w, h), (14, 22, 34))
        draw = ImageDraw.Draw(sheet)
        for idx, item_id in enumerate(chunk):
            wp = OUT_DIR / f"{item_id}.webp"
            col, row = idx % cols, idx // cols
            x0 = pad + col * (card_w + pad)
            y0 = pad + row * (card_h + pad)
            draw.rounded_rectangle([x0, y0, x0 + card_w, y0 + card_h], radius=12, fill=(22, 34, 52), outline=(66, 118, 152), width=2)
            if wp.exists():
                icon = Image.open(wp).convert("RGBA")
                big = icon.resize((136, 136), Image.Resampling.LANCZOS)
                sheet.paste(big, (x0 + 10, y0 + 10), big)
                draw.rounded_rectangle([x0 + 150, y0 + 108, x0 + 194, y0 + 152], radius=8, fill=(11, 18, 30), outline=(90, 175, 220), width=1)
                small = icon.resize((36, 36), Image.Resampling.LANCZOS)
                sheet.paste(small, (x0 + 154, y0 + 112), small)
                sz = f"{wp.stat().st_size // 1024} KB"
            else:
                sz = "MISSING"
            draw.text((x0 + 10, y0 + 162), item_id[:26], fill=(230, 242, 255))
            draw.text((x0 + 10, y0 + 182), sz, fill=(135, 175, 205))
        sheet_path = sheet_dir / f"{prefix}_p{page // per_sheet + 1}.png"
        sheet.save(sheet_path)
        paths.append(sheet_path)
    return paths


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", required=True, help="JSON file mapping item_id -> subject description")
    parser.add_argument("--sheet-dir", required=True, help="Directory for QA contact sheets")
    parser.add_argument("--sheet-prefix", default="batch", help="Prefix for QA contact sheets")
    parser.add_argument("--workers", type=int, default=6)
    parser.add_argument("--only", nargs="*", help="Optional subset of item IDs to generate")
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args()

    api_key = (PROJECT_ROOT / "openai_api_key.txt").read_text(encoding="utf-8").strip()
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    TMP_DIR.mkdir(parents=True, exist_ok=True)

    manifest: dict[str, str] = json.loads(Path(args.manifest).read_text(encoding="utf-8"))
    if args.only:
        targets = [(k, manifest[k]) for k in args.only if k in manifest]
    else:
        targets = list(manifest.items())

    print(f"Processing {len(targets)} items with {args.workers} workers...")
    errors = []
    done = 0
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futs = {pool.submit(generate_one, api_key, iid, desc, args.force): iid for iid, desc in targets}
        for fut in as_completed(futs):
            iid, status = fut.result()
            done += 1
            print(f"[{done:3d}/{len(targets)}] {iid:34s} -> {status}", flush=True)
            if status.startswith("ERROR"):
                errors.append((iid, status))

    sheets = render_contact_sheets([k for k, _ in targets], Path(args.sheet_dir), args.sheet_prefix)
    for sp in sheets:
        print(f"CONTACT SHEET: {sp}")
    if errors:
        print(f"COMPLETED WITH {len(errors)} ERRORS:")
        for iid, st in errors:
            print(f"  {iid}: {st}")
        sys.exit(1)
    print("ALL ITEMS COMPLETED CLEANLY.")


if __name__ == "__main__":
    main()
