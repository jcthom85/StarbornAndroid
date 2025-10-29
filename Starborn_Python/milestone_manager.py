# milestone_manager.py
import json
from pathlib import Path

class MilestoneManager:
    def __init__(self, game):
        self.game = game
        self.milestones: dict[str, dict] = {}          # id ➜ data
        self.completed: set[str] = set()
        self.load()

    def load(self, path: Path | str = "milestones.json"):
        try:
            with open(path, "r", encoding="utf8") as f:
                self.milestones = {m["id"]: m for m in json.load(f)}
        except (FileNotFoundError, json.JSONDecodeError):
            self.milestones = {}

    def on_battle_end(self, battle_id: str):
        self._check_triggers("battle", battle_id)

    def on_event_complete(self, event_id: str):
        self._check_triggers("event", event_id)

    def on_quest_complete(self, quest_id: str):
        self._check_triggers("quest", quest_id)

    def _check_triggers(self, trigger_type: str, trigger_id: str):
        for mid, data in self.milestones.items():
            if mid in self.completed:
                continue
            
            # --- THIS IS THE FIX ---
            # Safely get the trigger object. If it doesn't exist, skip this milestone.
            trig = data.get("trigger")
            if not trig:
                continue
            
            # Now check if the trigger type and ID match.
            if trig.get("type") == trigger_type and trig.get(f"{trigger_type}_id") == trigger_id:
                self._unlock(mid, data)

    def _unlock(self, mid: str, data: dict):
        self.completed.add(mid)
        effects = data.get("effects", {})
        
        # 1. unlock abilities
        for ab in effects.get("unlock_abilities", []):
            for ch in self.game.party:
                if ab.startswith(ch.id):
                    ch.unlocked_abilities.add(ab)
        
        # 2. unlock areas
        for area_id in effects.get("unlock_areas", []):
            if area_id in self.game.rooms:
                self.game.rooms[area_id].locked = False
        
        # 3. fire a toast / popup
        if hasattr(self.game, 'narrate'):
            self.game.narrate(f"[i]Milestone achieved: {data['name']}[/i]")