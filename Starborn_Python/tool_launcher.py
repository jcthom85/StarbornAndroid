import json
import os
from game_objects import Room, Bag, Item

class WorldManager:
    """
    Manages the loading and state of the game world, including worlds, hubs, nodes, and rooms.
    """
    def __init__(self, game_instance):
        self.game = game_instance
        self.worlds = {}
        self.hubs = {}
        self.nodes = {}
        self.all_rooms = {}

        self.current_world_id = None
        self.current_hub_id = None
        self.current_node_id = None

        self._load_all_data()

    def _load_json(self, filename):
        """Safely loads a JSON file from the project root."""
        path = os.path.join(os.path.dirname(__file__), filename)
        if not os.path.exists(path):
            print(f"Warning: Data file not found at {path}")
            return []
        try:
            with open(path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except json.JSONDecodeError as e:
            print(f"Error decoding JSON from {filename}: {e}")
            return []

    def _load_all_data(self):
        """
        Loads every JSON data file (worlds, hubs, nodes, rooms, enemies) and
        wires the results together.
        """
        # ── A. high-level structures ───────────────────────────────────
        self.worlds = {w["id"]: w for w in self._load_json("worlds.json")}
        self.hubs   = {h["id"]: h for h in self._load_json("hubs.json")}
        self.nodes  = {n["id"]: n for n in self._load_json("nodes.json")}

        # ── B. enemies (Bag + flavour text) ────────────────────────────
        self.game.enemy_flavor = {}
        self.game.all_enemies  = Bag()
        for en in self._load_json("enemies.json"):
            self.game.enemy_flavor[en["id"]] = en.get(
                "flavor", en.get("description", "")
            )
            # NOTE: We now use your custom Item class from game_objects.py
            obj = Item(en["name"], en["id"])
            obj.enemy_id    = en["id"]
            obj.behavior    = en.get("behavior", "passive")
            obj.alert_delay = en.get("alert_delay", 3)
            obj.party       = en.get("party", [en["id"]])
            self.game.all_enemies.add(obj)

        # ── C. build Room objects ──────────────────────────────────────
        self.all_rooms = {}
        room_defs = self._load_json("rooms.json")

        for rd in room_defs:
            rid = rd["id"]
            # Create a Room object from your custom class
            room = Room(rd.get("description", ""))
            
            # Copy all key-value pairs from the JSON data onto the room object
            for key, value in rd.items():
                setattr(room, key, value)
            
            # Ensure essential attributes are set
            room.room_id = rid
            room.state = dict(rd.get("state", {}))
            
            # Attach items, npcs, and enemies using your custom Bag and Item classes
            room.items = self.game.all_items.bag_from_names(rd.get("items", []))
            room.npcs  = self.game.all_npcs.bag_from_names(rd.get("npcs",  []))
            room.enemies = Bag() # This part of your code was already correct
            for ed in rd.get("enemies", []):
                if isinstance(ed, str):
                    enemy_id, overrides = ed, {}
                else:  # dict
                    enemy_id = ed.get("id") or (ed.get("party") or [None])[0]
                    overrides = ed
                if not enemy_id:
                    continue
                base_enemy = self.game.all_enemies.find(enemy_id)
                if base_enemy:
                    # shallow copy is fine for now; use overrides if/when needed
                    inst = Item(base_enemy.name, *base_enemy.aliases)
                    inst.enemy_id   = base_enemy.enemy_id
                    inst.behavior   = overrides.get(
                        "behavior", getattr(base_enemy, "behavior", "passive")
                    )
                    inst.alert_delay = overrides.get(
                        "alert_delay", getattr(base_enemy, "alert_delay", 3)
                    )
                    inst.party = overrides.get("party", [enemy_id])
                    room.enemies.add(inst)
            room.actions = rd.get("actions", [])
            self.all_rooms[rid] = room

        # ── D. connect exits (THIS IS THE CRUCIAL FIX) ────────────────
        for rd in room_defs:
            src_room = self.all_rooms.get(rd["id"])
            if src_room:
                for direction, dest_id in rd.get("connections", {}).items():
                    dest_room = self.all_rooms.get(dest_id)
                    if dest_room:
                        # This line populates the `exits` dictionary
                        src_room.exits[direction.lower()] = dest_room
                        
    def set_location(self, world_id, hub_id, node_id, room_id):
        self.current_world_id = world_id
        self.current_hub_id = hub_id
        self.current_node_id = node_id
        self.game.current_room = self.get_room(room_id)
        
        if self.game.current_room:
            current_hub = self.hubs.get(hub_id, {})
            env = self.game.current_room.env or current_hub.get("theme", "default")
            self.game.themes.use(env)

    def get_room(self, room_id):
        return self.all_rooms.get(room_id)

    def get_rooms_for_node(self, node_id):
        node = self.nodes.get(node_id)
        if not node:
            return {}
        
        node_rooms = {room_id: self.all_rooms[room_id] for room_id in node.get('rooms', []) if room_id in self.all_rooms}
        return node_rooms

    def get_nodes_for_hub(self, hub_id):
        """Returns a list of all node definitions for a given hub_id."""
        return [node for node in self.nodes.values() if node.get("hub_id") == hub_id]

