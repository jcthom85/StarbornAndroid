# map_utils.py
"""
Shared ASCII-map builder for both minimap and full map.

Call `build_ascii_grid(...)` – pass `radius=2` for a 5×5 minimap,
or `radius=None` for the whole bounding box.

NEW  · 2025-06-03
  * `current_symbol` lets callers inject a dynamic glyph (e.g. an [img] tag)
"""

from typing import List, Dict, Tuple, Set, Optional

# --------------------------------------------------------------------------- #
DEFAULT_GLYPHS = {
    "current": "@",        # fallback if caller doesn't supply current_symbol
    "seen":    "O",
    "blank":   "·",
}

THIN_SPACE = "\u2009"      # keeps fixed grid width when using [img] tags
# --------------------------------------------------------------------------- #
def build_ascii_grid(
    discovered:      Set[Tuple[int, int]],
    room_positions:  Dict[str, Tuple[int, int]],
    current_room_id: str,
    *,
    radius: Optional[int] = None,
    current_symbol: Optional[str] = None,   # ← NEW
) -> List[str]:
    """
    Returns a list[str] ready for '\n'.join(...).

    • If `radius` is None we show the full bounding box.
    • If `current_symbol` is None we fall back to DEFAULT_GLYPHS["current"].
    """
    cur_pos = room_positions.get(current_room_id)
    if cur_pos is None:
        raise ValueError("current_room_id not found in room_positions")

    cur_glyph = current_symbol or DEFAULT_GLYPHS["current"]

    # Decide viewport bounds
    if radius is None:
        xs = [p[0] for p in discovered] + [cur_pos[0]]
        ys = [p[1] for p in discovered] + [cur_pos[1]]
        min_x, max_x = min(xs), max(xs)
        min_y, max_y = min(ys), max(ys)
    else:
        cx, cy       = cur_pos
        min_x, max_x = cx - radius, cx + radius
        min_y, max_y = cy - radius, cy + radius

    # Build rows top→bottom
    rows = []
    for gy in range(max_y, min_y - 1, -1):
        chars = []
        for gx in range(min_x, max_x + 1):
            pos = (gx, gy)
            if pos == cur_pos:
                char = cur_glyph
            elif pos in discovered:
                char = DEFAULT_GLYPHS["seen"]
            else:
                char = DEFAULT_GLYPHS["blank"]
            chars.append(char)
        rows.append(THIN_SPACE.join(chars))
    return rows
