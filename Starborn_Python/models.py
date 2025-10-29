# models.py
from game_objects import Item # Import your new Item class

class NPC(Item):
    """
    Simple dialogue holder (no Ink required).
    `dialogue` is a dict keyed by the tags you use in talk():
        { "need_wrench": "...", "hint_key": "...", ... }
    """
    def __init__(self, name: str, dialogue: dict | str = None, *aliases: str):
        super().__init__(name, *aliases)
        # allow a plain string or a dict
        self.dialogue: dict[str, str] = \
            dialogue if isinstance(dialogue, dict) else {"default": dialogue or ""}
        self._override: str | None = None   # set via update_npc_dialogue

    # optional helper the rest of the code can call
    def say_line(self, key: str) -> str:
        if self._override:                # one‑shot override from an event
            line, self._override = self._override, None
            return line
        return self.dialogue.get(key, "(no line)")
    