# Starborn/data/fishing_manager.py
import json
import random
from pathlib import Path

class FishingManager:
    def __init__(self):
        self.data = {}
        self._load_data()

    def _load_data(self):
        path = Path(__file__).parent / "recipes_fishing.json"
        try:
            with path.open("r", encoding="utf-8") as f:
                self.data = json.load(f)
        except Exception as e:
            print(f"[FishingManager] Failed to load data: {e}")
            self.data = {}

    def get_available_rods(self):
        return self.data.get("rods", [])

    def get_available_lures(self):
        return self.data.get("lures", [])

    def get_zone_items(self, zone_id):
        return self.data.get("zones", {}).get(zone_id, [])

    def get_minigame_settings(self, difficulty):
        return self.data.get("minigame_rules", {}).get(difficulty, {})

    def get_catch(self, zone_id, minigame_result, rod_power=1, lure_bonus=0):
        items = self.get_zone_items(zone_id)
        if not items:
            return None

        # Adjust weights based on rod and lure
        weighted_items = []
        for item in items:
            rarity_factor = self._rarity_factor(item["rarity"], rod_power, lure_bonus)
            weight = item["weight"] * rarity_factor
            weighted_items.append((item["item"], weight))

        chosen = self._weighted_choice(weighted_items)
        return CatchResult(name=chosen, rarity=self._get_rarity(chosen, items))

    def _rarity_factor(self, rarity, rod_power, lure_bonus):
        base = {
            "junk": 1.0,
            "common": 1.0,
            "uncommon": 1 + 0.05 * (rod_power + lure_bonus),
            "rare": 1 + 0.1 * (rod_power + lure_bonus),
            "epic": 1 + 0.15 * (rod_power + lure_bonus)
        }
        return base.get(rarity, 1.0)

    def _weighted_choice(self, weighted_items):
        total = sum(w for _, w in weighted_items)
        r = random.uniform(0, total)
        upto = 0
        for item, weight in weighted_items:
            if upto + weight >= r:
                return item
            upto += weight
        return weighted_items[-1][0]  # fallback

    def _get_rarity(self, name, item_list):
        for item in item_list:
            if item["item"] == name:
                return item["rarity"]
        return "common"


class CatchResult:
    def __init__(self, name, rarity):
        self.name = name
        self.rarity = rarity
        self.quantity = 1
        self.flavor_text = self._flavor_text()

    def _flavor_text(self):
        if self.rarity == "junk":
            return "Well, at least it's something."
        elif self.rarity == "common":
            return "A decent haul."
        elif self.rarity == "uncommon":
            return "Not bad!"
        elif self.rarity == "rare":
            return "That one's special."
        elif self.rarity == "epic":
            return "An incredible catch!"
        return "You caught something."
