# game_objects.py

class Item:
    def __init__(self, name, *aliases):
        self.name = name
        # Ensure aliases are always a list and include the lowercase name for lookups
        self.aliases = list(aliases) if aliases else []
        if self.name.lower() not in self.aliases:
            self.aliases.append(self.name.lower())

    def __repr__(self):
        return f"Item({self.name})"

class Bag(list):
    def find(self, item_name):
        """Finds an item in the bag by its name or one of its aliases."""
        search_name = item_name.lower()
        for item in self:
            # This now checks the item's actual name as well as its aliases
            if item.name.lower() == search_name or search_name in item.aliases:
                return item
        return None
    
    def add(self, item):
        """Adds an item to the bag."""
        self.append(item)

class Room:
    def __init__(self, description=""):
        self.description = description
        self.items = Bag()
        self.npcs = Bag()
        self.enemies = Bag()
        self.exits = {}
        self.blocked_directions = {}

    def exit(self, direction):
        return self.exits.get(direction)

    def __str__(self):
        return self.description
