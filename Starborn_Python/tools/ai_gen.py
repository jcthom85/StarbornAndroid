# tools/ai_gen.py
from __future__ import annotations

import io
import json
import os
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple, TypedDict, Union, Literal

from PIL import Image

# ---------- Types (kept simple to satisfy Pylance) ----------
# JSON-like types for hints only; avoid recursive/variable-in-type pitfalls
JSONScalar = Union[str, int, float, bool, None]
JSONLike = Union[JSONScalar, Dict[str, Any], List[Any]]

class ItemSpec(TypedDict, total=False):
    id: str
    name: str
    type: Literal["consumable", "equipment", "material", "quest", "key"]
    rarity: Literal["common", "uncommon", "rare", "epic", "legendary"]
    description: str
    value: int

# Optional light schema hint used by callers; keep fields Any to avoid Pylance issues
class SchemaHint(TypedDict, total=False):
    items: List[Dict[str, Any]]
    enemies: List[Dict[str, Any]]
    rooms: List[Dict[str, Any]]
    meta: Dict[str, Any]

# ---------- Defaults ----------
STARBORN_DEFAULT_PALETTE: List[Tuple[int, int, int]] = [
    (10, 12, 38),   # deep navy
    (22, 26, 66),
    (34, 46, 90),
    (52, 70, 120),
    (82, 102, 160),
    (120, 140, 200),
    (158, 170, 220),
    (196, 210, 245),
    (160, 120, 210),  # purple accent
    (220, 180, 250)   # neon-lavender
]

# ---------- OpenAI lazy import wrapper ----------
def _lazy_openai_import():
    try:
        import openai  # type: ignore
        return openai
    except Exception:
        return None

# ---------- Utility: ensure directory ----------
def _ensure_dir(p: Path) -> None:
    p.parent.mkdir(parents=True, exist_ok=True)

# ---------- AIGenerator ----------
class AIGenerator:
    """
    Centralized image/text helpers. Image generation relies on the OpenAI Images API.
    The class is 'lazy' — it won't crash if the SDK is missing until you call methods.
    """
    def __init__(self, project_root: Optional[str] = None):
        self.project_root = Path(project_root or os.getcwd())
        self._api_key: Optional[str] = os.environ.get("OPENAI_API_KEY") or None
        # Models can be overridden by env
        self._image_model = os.environ.get("OPENAI_IMAGE_MODEL", "gpt-image-1")
        self._text_model = os.environ.get("OPENAI_TEXT_MODEL", "gpt-5-mini")

    # ---- API Key plumbing ----
    def set_api_key(self, key: Optional[str]) -> None:
        self._api_key = (key or "").strip() or None
        if self._api_key:
            os.environ["OPENAI_API_KEY"] = self._api_key

    # ---- State/probe for UIs ----
    def ready_state(self) -> Dict[str, Any]:
        oi = _lazy_openai_import()
        return {
            "openai_installed": bool(oi),
            "has_api_key": bool(self._api_key or os.environ.get("OPENAI_API_KEY")),
            "image_model": self._image_model,
            "text_model": self._text_model,
        }

    # ---- Image generation (OpenAI Images API) ----
    def smart_image(
        self,
        prompt_core: str,
        style_hint: str,
        size_request: str,
        target_px: Tuple[int, int],
        palette: Optional[List[Tuple[int, int, int]]] = None,
        n: int = 1
    ) -> List[Image.Image]:
        """
        Compose final prompt and call OpenAI Images API. Then crop to exact target
        aspect and optionally quantize to a Starborn palette.
        """
        openai = _lazy_openai_import()
        if not openai:
            raise RuntimeError("openai package not installed. pip install openai")
        api_key = self._api_key or os.environ.get("OPENAI_API_KEY")
        if not api_key:
            raise RuntimeError("OPENAI_API_KEY not set")
        # new-style client
        try:
            client = openai.OpenAI(api_key=api_key)  # type: ignore[attr-defined]
        except Exception:
            # older SDKs
            openai.api_key = api_key  # type: ignore[attr-defined]
            client = openai  # type: ignore

        # Compose prompt
        full_prompt = f"{prompt_core.strip()}\n\nStyle: {style_hint.strip()}".strip()

        # Call Images API
        # Try to use 'client.images.generate', fallback to legacy 'Image.create'
        images: List[Image.Image] = []
        try:
            resp = client.images.generate(  # type: ignore[attr-defined]
                model=self._image_model,
                prompt=full_prompt,
                size=size_request,
                n=n
            )
            # new SDK returns base64 in resp.data[i].b64_json
            for d in getattr(resp, "data", [])[:n]:
                b64 = getattr(d, "b64_json", None)
                if not b64:
                    continue
                raw = io.BytesIO((b64 if isinstance(b64, bytes) else b64.encode("utf-8")))
                # if string, decode from base64
                import base64
                raw = io.BytesIO(base64.b64decode(b64))
                im = Image.open(raw).convert("RGBA")
                images.append(im)
        except Exception:
            # Legacy path
            try:
                resp = client.Image.create(  # type: ignore[attr-defined]
                    prompt=full_prompt,
                    size=size_request,
                    n=n,
                    response_format="b64_json"
                )
                for d in resp["data"][:n]:
                    import base64
                    raw = io.BytesIO(base64.b64decode(d["b64_json"]))
                    im = Image.open(raw).convert("RGBA")
                    images.append(im)
            except Exception as e:
                raise RuntimeError(f"OpenAI image generation failed: {e}")

        # Post-process: crop/fit & optional palette quantize
        out: List[Image.Image] = []
        for im in images:
            im2 = self._crop_to_target(im, target_px)
            if palette:
                im2 = self._quantize_palette(im2, palette)
            out.append(im2)
        return out

    # ---- Save utility ----
    def save_png(self, im: Image.Image, rel_path: str) -> str:
        """
        Save under project root. Returns the relative path actually written.
        """
        rp = rel_path.replace("\\", "/").lstrip("/")
        out_path = self.project_root / rp
        _ensure_dir(out_path)
        # Ensure PNG mode
        if im.mode not in ("RGB", "RGBA"):
            im = im.convert("RGBA")
        im.save(out_path, format="PNG")
        return rp

    # ---- Internal: crop & palette ----
    @staticmethod
    def _crop_to_target(im: Image.Image, target_px: Tuple[int, int]) -> Image.Image:
        tw, th = target_px
        if tw <= 0 or th <= 0:
            return im.copy()
        src_w, src_h = im.size
        target_ar = tw / float(th)
        src_ar = src_w / float(src_h)

        # letterbox crop to match target AR
        if src_ar > target_ar:
            # too wide -> crop left/right
            new_w = int(src_h * target_ar)
            x0 = (src_w - new_w) // 2
            box = (x0, 0, x0 + new_w, src_h)
        else:
            # too tall -> crop top/bottom
            new_h = int(src_w / target_ar)
            y0 = (src_h - new_h) // 2
            box = (0, y0, src_w, y0 + new_h)

        cropped = im.crop(box)
        if (cropped.size[0], cropped.size[1]) != (tw, th):
            cropped = cropped.resize((tw, th), Image.LANCZOS)
        return cropped

    @staticmethod
    def _quantize_palette(im: Image.Image, palette: List[Tuple[int, int, int]]) -> Image.Image:
        """
        Convert to an indexed image using the given palette, then back to RGBA for UI.
        """
        # build a 768-length palette (256 * 3)
        flat: List[int] = []
        for (r, g, b) in palette[:256]:
            flat.extend([int(r), int(g), int(b)])
        # pad to 768 if needed
        while len(flat) < 768:
            flat.extend([0, 0, 0])

        pal_img = Image.new("P", (1, 1))
        pal_img.putpalette(flat)

        # Convert source to RGB, then quantize using our palette
        base = im.convert("RGB")
        quant = base.quantize(palette=pal_img, dither=Image.FLOYDSTEINBERG)  # type: ignore[arg-type]
        return quant.convert("RGBA")
