# StarBorn/ui/debug_panel.py
from kivy.app import App
from kivy.uix.screenmanager import Screen
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.scrollview import ScrollView
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.textinput import TextInput
from kivy.metrics import dp, sp
from font_manager import fonts
from functools import partial

def section_label(text):
    return Label(text=f"[b]{text}[/b]", markup=True, size_hint_y=None, height=dp(30),
                 font_name=fonts["medium_text"]["name"], font_size=fonts["medium_text"]["size"])

class DebugPanel(Screen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        # REMOVED: self.game = App.get_running_app().current_game
        # The game object will be retrieved inside the methods below when they are called.

        root = BoxLayout(orientation='vertical', padding=dp(10), spacing=dp(10))

        # Header
        header = BoxLayout(size_hint_y=None, height=dp(40))
        header.add_widget(Label(text="Debug Simulator", font_name=fonts["section_title"]["name"], font_size=fonts["section_title"]["size"]))
        close_btn = Button(text="Close", size_hint_x=None, width=dp(80))
        close_btn.bind(on_release=lambda *a: setattr(self.manager, 'current', 'explore'))
        header.add_widget(close_btn)
        root.add_widget(header)

        # Scrollable area for controls
        scroll = ScrollView(do_scroll_x=False)
        grid = GridLayout(cols=1, size_hint_y=None, spacing=dp(15), padding=dp(5))
        grid.bind(minimum_height=grid.setter('height'))
        scroll.add_widget(grid)
        root.add_widget(scroll)
        
        # --- Controls ---
        # Give Item
        grid.add_widget(section_label("Give Item"))
        item_input = TextInput(hint_text="item_id", multiline=False, size_hint_y=None, height=dp(40))
        give_item_btn = Button(text="Give Item", size_hint_y=None, height=dp(44))
        give_item_btn.bind(on_release=partial(self.give_item, item_input))
        grid.add_widget(item_input)
        grid.add_widget(give_item_btn)

        # Add Credits
        grid.add_widget(section_label("Add Credits"))
        credits_input = TextInput(hint_text="amount", multiline=False, size_hint_y=None, height=dp(40), input_filter='int')
        add_credits_btn = Button(text="Add Credits", size_hint_y=None, height=dp(44))
        add_credits_btn.bind(on_release=partial(self.add_credits, credits_input))
        grid.add_widget(credits_input)
        grid.add_widget(add_credits_btn)
        
        # Add AP
        grid.add_widget(section_label("Add Ability Points"))
        ap_input = TextInput(hint_text="amount", multiline=False, size_hint_y=None, height=dp(40), input_filter='int')
        add_ap_btn = Button(text="Add AP to Party", size_hint_y=None, height=dp(44))
        add_ap_btn.bind(on_release=partial(self.add_ap, ap_input))
        grid.add_widget(ap_input)
        grid.add_widget(add_ap_btn)

        # Warp to Room
        grid.add_widget(section_label("Warp to Room"))
        room_input = TextInput(hint_text="room_id", multiline=False, size_hint_y=None, height=dp(40))
        warp_btn = Button(text="Warp", size_hint_y=None, height=dp(44))
        warp_btn.bind(on_release=partial(self.warp_to_room, room_input))
        grid.add_widget(room_input)
        grid.add_widget(warp_btn)

        # Hot-Reload Data
        grid.add_widget(section_label("Game Data"))
        reload_btn = Button(text="Hot-Reload Game Data", size_hint_y=None, height=dp(44))
        reload_btn.bind(on_release=self.hot_reload)
        grid.add_widget(reload_btn)

        self.add_widget(root)

    def give_item(self, text_input, *args):
        game = App.get_running_app().current_game
        item_id = text_input.text.strip()
        item = game.all_items.find(item_id)
        if item:
            game.inventory[item.name] = game.inventory.get(item.name, 0) + 1
            game.say(f"DEBUG: Gave 1x {item.name}")
            game.update_inventory_display()
        else:
            game.say(f"DEBUG: Item '{item_id}' not found.")

    def add_credits(self, text_input, *args):
        game = App.get_running_app().current_game
        try:
            amount = int(text_input.text)
            game.credits += amount
            game.say(f"DEBUG: Added {amount} credits.")
        except ValueError:
            game.say("DEBUG: Invalid amount.")

    def add_ap(self, text_input, *args):
        game = App.get_running_app().current_game
        try:
            amount = int(text_input.text)
            for char in game.party:
                char.ability_points += amount
            game.say(f"DEBUG: Added {amount} AP to all party members.")
        except ValueError:
            game.say("DEBUG: Invalid amount.")

    def warp_to_room(self, text_input, *args):
        game = App.get_running_app().current_game
        room_id = text_input.text.strip()
        if room_id in game.rooms:
            game.current_room = game.rooms[room_id]
            game.update_room()
            game.say(f"DEBUG: Warped to {room_id}.")
        else:
            game.say(f"DEBUG: Room '{room_id}' not found.")

    def hot_reload(self, *args):
        game = App.get_running_app().current_game
        try:
            game.load_game_data()
            game.update_room()
            game.say("DEBUG: All game data hot-reloaded.")
        except Exception as e:
            game.say(f"DEBUG: Error reloading data: {e}")
            