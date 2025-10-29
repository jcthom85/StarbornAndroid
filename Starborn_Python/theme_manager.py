# theme_manager.py
import json, os

class ThemeManager:
    def __init__(self, path="themes.json"):
        fn = os.path.join(os.path.dirname(__file__), path)
        with open(fn, encoding="utf-8") as fp:
            self._schemes = json.load(fp)
        self._cur = self._schemes["default"]

    # ------------------------------------------------------------
    def use(self, env_name: str | None):
        """Switch to the given env scheme (or default if missing)."""
        # Use a fallback to 'default' if env_name is None or not found
        self._cur = self._schemes.get(env_name or "default", self._schemes["default"])

    def col(self, key: str):
        """Return a 4‑tuple color for 'bg' | 'fg' | 'border'."""
        return self._cur.get(key, (1, 1, 1, 1))

    def get_asset(self, key: str):
        """Return a string-based asset path, e.g., for an image."""
        return self._cur.get(key)
    