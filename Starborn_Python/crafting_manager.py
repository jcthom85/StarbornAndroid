import json
from pathlib import Path
import random

class CraftingManager:
    def __init__(self, game):
        self.game = game
        self.tinkering_recipes = {}
        self.components = {}
        self.load_data()

    def load_data(self):
        """Loads all crafting data from JSON files."""
        p_root = Path(__file__).parent / "data"
        try:
            with open(p_root / "recipes_tinkering.json", "r", encoding="utf8") as f:
                self.tinkering_recipes = {r["id"]: r for r in json.load(f)}
        except (FileNotFoundError, json.JSONDecodeError) as e:
            print(f"[CraftingManager] Error loading tinkering recipes: {e}")
            self.tinkering_recipes = {}

        try:
            with open(p_root / "components.json", "r", encoding="utf8") as f:
                self.components = {c["id"]: c for c in json.load(f)}
        except (FileNotFoundError, json.JSONDecodeError) as e:
            print(f"[CraftingManager] Error loading components: {e}")
            self.components = {}


    def get_mods_for_slot(self, slot_type: str) -> list[dict]:
        """Returns a list of all known mod recipes for a given slot type."""
        return [r for r in self.tinkering_recipes.values() if r.get("mod_type") == slot_type]

    def can_craft(self, recipe_id: str) -> bool:
        """Checks if the player has the required components for a recipe."""
        recipe = self.tinkering_recipes.get(recipe_id)
        if not recipe:
            return False
        for comp_id, needed in recipe.get("cost", {}).items():
            # Find the component's proper name to check against the inventory dict
            item_to_check = None
            # The "item" could be a component or another item (like a base mod for an upgrade)
            comp_obj = self.components.get(comp_id)
            if comp_obj:
                item_to_check = comp_obj.get('name')
            else: # Fallback for items that aren't in components.json (e.g. mod upgrades)
                 item_def = self.game.all_items.find(comp_id)
                 if item_def:
                     item_to_check = item_def.name
                 else: # Or it could be a recipe ID for an upgrade
                      recipe_def = self.tinkering_recipes.get(comp_id)
                      if recipe_def:
                          item_to_check = recipe_def.get('name')

            if not item_to_check or self.game.inventory.get(item_to_check, 0) < needed:
                return False
        return True

    def craft_mod(self, recipe_id: str):
        """Consumes components and returns the crafted mod's recipe dict."""
        if not self.can_craft(recipe_id):
            NarrativePopup.show("[i]Not enough components.[/i]", theme_mgr=self.game.themes)
            return None

        recipe = self.tinkering_recipes[recipe_id]
        
        # This logic correctly handles component consumption
        for comp_id, needed in recipe.get("cost", {}).items():
            # This logic needs to be as robust as the check logic above
            item_to_consume = None
            comp_obj = self.components.get(comp_id)
            if comp_obj:
                item_to_consume = comp_obj.get('name')
            else:
                 item_def = self.game.all_items.find(comp_id)
                 if item_def: item_to_consume = item_def.name
                 else:
                      recipe_def = self.tinkering_recipes.get(comp_id)
                      if recipe_def: item_to_consume = recipe_def.get('name')
            
            if item_to_consume:
                self.game.inventory[item_to_consume] -= needed
                if self.game.inventory[item_to_consume] <= 0:
                    del self.game.inventory[item_to_consume]

        NarrativePopup.show(f"Crafted [b]{recipe['name']}[/b].", theme_mgr=self.game.themes)
        self.game.update_inventory_display()
        
        return recipe

    def scrap_item(self, item_name: str):
        """Destroys an item and returns its defined components."""
        item = self.game.all_items.find(item_name)
        if not item or not hasattr(item, 'scrap_yield'):
            NarrativePopup.show("This item cannot be scrapped.", theme_mgr=self.game.themes)
            return

        yields = getattr(item, 'scrap_yield', {})
        if not yields:
            NarrativePopup.show(f"Scrapping {item.name} yielded nothing.", theme_mgr=self.game.themes)
            # Still need to remove the item from inventory even if it yields nothing
            if item.name in self.game.inventory:
                self.game.inventory[item.name] -= 1
                if self.game.inventory[item.name] <= 0:
                    del self.game.inventory[item.name]
            self.game.update_inventory_display()
            if self.game.manager.current == 'tinkering':
                self.game.manager.get_screen('tinkering').on_pre_enter()
            return
            
        # *** THIS IS THE FIX ***
        # Decrement the count by 1 instead of popping the whole stack.
        if item.name in self.game.inventory:
            self.game.inventory[item.name] -= 1
            if self.game.inventory[item.name] <= 0:
                del self.game.inventory[item.name]
        # *** END OF FIX ***

        yield_texts = []
        for comp_id, amount in yields.items():
            comp_name = next((c['name'] for c in self.components.values() if c['id'] == comp_id), comp_id)
            current_amount = self.game.inventory.get(comp_name, 0)
            self.game.inventory[comp_name] = current_amount + amount
            yield_texts.append(f"{amount}x {comp_name}")

        NarrativePopup.show(f"Scrapped {item.name} and recovered: {', '.join(yield_texts)}.", theme_mgr=self.game.themes)
        self.game.update_inventory_display()
        
        # Refresh the tinkering screen to remove the scrapped item
        if self.game.manager.current == 'tinkering':
            self.game.manager.get_screen('tinkering').on_pre_enter()