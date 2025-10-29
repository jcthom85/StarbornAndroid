# data/leveling_manager.py
import json
from pathlib import Path

class LevelingManager:
    def __init__(self):
        self._level_curve = {}
        self._xp_sources = {}
        self._max_level = 0
        self._load_data()

    def _load_data(self):
        path = Path(__file__).parent / "leveling_data.json"
        try:
            with path.open("r", encoding="utf-8") as f:
                data = json.load(f)
                # Convert string keys from JSON to integer keys for easy sorting
                self._level_curve = {int(k): v for k, v in data.get("level_curve", {}).items()}
                self._xp_sources = data.get("xp_sources", {})
                if self._level_curve:
                    self._max_level = max(self._level_curve.keys())
        except Exception as e:
            print(f"[LevelingManager] Failed to load data: {e}")

    def get_level_for_xp(self, total_xp: int) -> int:
        """Calculates a character's level based on their total XP."""
        current_level = 1
        # Iterate through sorted levels (1, 2, 3...)
        for level, required_xp in sorted(self._level_curve.items()):
            if total_xp >= required_xp:
                current_level = level
            else:
                break
        return current_level

    def get_xp_for_rarity(self, source: str, rarity: str) -> int:
        """Gets the XP value for a given activity and rarity."""
        return self._xp_sources.get(source, {}).get(rarity, 0)

    def get_level_bounds(self, level: int) -> tuple[int, int | None]:
        """Returns the total XP needed for the start of a level and the start of the next."""
        if level >= self._max_level:
            return (self._level_curve.get(level, 0), None)
        
        start_xp = self._level_curve.get(level, 0)
        next_level_xp = self._level_curve.get(level + 1)
        return (start_xp, next_level_xp)