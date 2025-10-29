# dialogue_manager.py
import json
from pathlib import Path

class DialogueManager:
    def __init__(self, game):
        self.game = game
        self.dialogue_data = self._load_all_dialogue()

    def _load_all_dialogue(self):
        """Loads all dialogue entries from dialogue.json into a dictionary."""
        path = Path(__file__).parent / "dialogue.json"
        try:
            with open(path, "r", encoding="utf-8") as f:
                raw_list = json.load(f)
                return {entry["id"]: entry for entry in raw_list}
        except (FileNotFoundError, json.JSONDecodeError) as e:
            print(f"[DialogueManager] Error loading dialogue.json: {e}")
            return {}

    def get_dialogue_for_npc(self, npc_name: str) -> str | None:
        """
        Finds the highest-priority, valid dialogue ID for a given NPC.
        It checks conditions against the current game state.
        """
        possible_lines = []
        for key, entry in self.dialogue_data.items():
            if entry.get("speaker", "").lower() == npc_name.lower():
                possible_lines.append(entry)
        
        # In a more complex system, you could sort `possible_lines` by a "priority" key
        # to decide which dialogue takes precedence if multiple conditions are met.
        possible_lines.sort(key=lambda e: "condition" in e, reverse=True)

        for entry in possible_lines:
            if self._is_condition_met(entry.get("condition", "")):
                return entry["id"]
        
        return None # No suitable dialogue found

    def _is_condition_met(self, condition: str) -> bool:
        """Checks if a dialogue condition string is true."""
        if not condition:
            return True # No condition means it's always available

        # Conditions can be comma-separated for AND logic
        for cond in condition.split(','):
            cond = cond.strip()
            try:
                ctype, _, value = cond.partition(':')
                ctype = ctype.strip()
                value = value.strip()

                if ctype == "quest":
                    quest = None
                    qm = getattr(self.game, "quest_manager", None)
                    if qm and hasattr(qm, "get"):
                        quest = qm.get(value)
                    if quest is None:
                        quest = getattr(self.game, "quest_index", {}).get(value)
                    status = quest.get("status") if isinstance(quest, dict) else getattr(quest, "status", None)
                    if status not in ("active", "complete"):
                        return False
                elif ctype == "milestone":
                    if value not in self.game.milestones.completed:
                        return False
                elif ctype == "milestone_not_set":
                    if value in self.game.milestones.completed:
                        return False
                elif ctype == "item":
                    # --- THIS IS THE FIX ---
                    # The inventory is a dict {item_name: quantity}.
                    # We check for the key's existence, not with .find().
                    if value not in self.game.inventory:
                        return False
                else:
                    return False # Unrecognized condition type
            except Exception:
                return False # Malformed condition or other error during check
        
        return True # All conditions passed

    def play_dialogue(self, dialogue_id: str):
        """
        Shows a line of dialogue using the DialogueBox and handles its triggers.
        """
        if not dialogue_id or dialogue_id not in self.dialogue_data:
            print(f"[DialogueManager] Warning: Dialogue ID '{dialogue_id}' not found.")
            return

        entry = self.dialogue_data[dialogue_id]

        speaker_id = entry.get("speaker", "").lower()
        text = entry.get("text", "")

        def _after_dismiss():
            # Handle triggers
            trigger_str = entry.get("trigger", "")
            if trigger_str:
                for trig in trigger_str.split(','):
                    trig = trig.strip()
                    ttype, _, tval = trig.partition(':')
                    self.game.event_manager.dialogue_trigger(ttype.strip(), tval.strip())
            
            # Play next line if it exists
            next_id = entry.get("next", "")
            if next_id:
                from kivy.clock import Clock
                Clock.schedule_once(lambda dt: self.play_dialogue(next_id), 0.1)

        # Show the dialogue using the main UI component
        self.game.dialogue_box.show_dialogue(speaker_id, text, on_dismiss=_after_dismiss)
