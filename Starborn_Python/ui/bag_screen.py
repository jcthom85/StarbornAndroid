from kivy.app import App
from kivy.uix.screenmanager import Screen
import os
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.scrollview import ScrollView
from kivy.uix.carousel import Carousel
from kivy.uix.togglebutton import ToggleButton
from kivy.uix.button import Button
from kivy.uix.image import Image
from kivy.uix.label import Label
from kivy.uix.behaviors import ButtonBehavior
from kivy.metrics import dp, sp
from functools import partial
from kivy.graphics import Color, RoundedRectangle, Line
from ui.themed_button import ThemedToggleButton as TabButton
from kivy.clock import Clock
from font_manager import fonts
from ui.journal_screen import AspectStage # Import AspectStage
from ui.menu_popup import MenuPopup
from ui.bordered_frame import BorderedFrame

from kivy.uix.widget import Widget

def _get_icon_for_item(item_obj) -> str:
    """Determines the correct icon path for a given item object."""
    if not item_obj:
        return "images/ui/item_icon_generic.png"

    # Allow per-item override from JSON
    if hasattr(item_obj, 'icon'):
        return item_obj.icon

    icon_path = "images/ui/item_icon_generic.png"
    item_type = getattr(item_obj, 'type', 'junk')
    name_lc = item_obj.name.lower()

    if item_type == 'consumable':
        icon_path = "images/ui/item_icon_consumable.png"
        if any(k in name_lc for k in ('stew', 'salad', 'ramen', 'fish', 'meat')):
            icon_path = "images/ui/item_icon_food.png"
    elif item_type == 'ingredient':
        if getattr(item_obj, 'subtype', None) == 'fish':
            icon_path = "images/ui/item_icon_fish.png"
        else:
            icon_path = "images/ui/item_icon_ingredient.png"
            if any(k in name_lc for k in ('scrap', 'wiring', 'lens', 'weave')):
                icon_path = "images/ui/item_icon_material.png"
    elif item_type == 'fishing_rod':
        icon_path = "images/ui/item_icon_fishing.png"
    elif item_type == 'equippable':
        slot = getattr(item_obj, 'equipment', {}).get('slot', '')
        if slot == 'armor':
            icon_path = "images/ui/item_icon_armor.png"
            if 'gloves' in name_lc:
                icon_path = "images/ui/item_icon_gloves.png"
            elif 'pendant' in name_lc:
                icon_path = "images/ui/item_icon_pendant.png"
        elif slot == 'accessory':
            icon_path = "images/ui/item_icon_accessory.png"
        elif slot == 'weapon':
            icon_path = "images/ui/item_icon_sword.png"
            if any(k in name_lc for k in ('gun', 'pistol', 'rifle')):
                icon_path = "images/ui/item_icon_gun.png"
    return icon_path


class ItemEntry(ButtonBehavior, BoxLayout):
    """A composite widget for an inventory item (icon + label), clickable as a whole, with long-press support."""
    def __init__(self, item_obj, quantity, is_equipped=False, **kwargs):
        super().__init__(orientation='horizontal', **kwargs)
        self.register_event_type('on_long_press')
        self._long_press_ev = None
        self._long_pressed = False
        self.size_hint_y = None
        self.height = dp(36)

        # --- Icon Selection Logic ---
        icon = Image(source=_get_icon_for_item(item_obj), size_hint=(None, None), size=(dp(32), dp(32)))
        # --- End of Icon Logic ---

        # Label (no changes here)
        name = item_obj.name.title() if hasattr(item_obj, 'name') else str(item_obj)
        lbl = Label(text=f"{name} (x{quantity})",
                    font_name=fonts["medium_text"]["name"],
                    font_size=fonts["medium_text"]["size"],
                    halign="left", valign="middle",
                    size_hint_y=None, height=dp(32))
        lbl.color = (0,1,0,1) if is_equipped else (1,1,1,1)
        lbl.bind(size=lambda inst,val: setattr(inst, 'text_size', (inst.width, None)))
        self.add_widget(icon)
        self.add_widget(lbl)

    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos):
            self._long_press_ev = Clock.schedule_once(self._trigger_long_press, 0.6)
        return super().on_touch_down(touch)

    def on_touch_up(self, touch):
        if self._long_press_ev:
            self._long_press_ev.cancel()
            self._long_press_ev = None
        if self.collide_point(*touch.pos) and not self._long_pressed:
            self.dispatch('on_release')
            return True
        if self._long_pressed:
            self._long_pressed = False
            return True
        return super().on_touch_up(touch)
        
    def _trigger_long_press(self, dt):
        self._long_press_ev = None
        self._long_pressed = True
        self.dispatch('on_long_press')
        
    def on_long_press(self, *args):
        """Default handler for the long-press event."""
        pass
    # --- THIS IS THE FIX ---
    # This method was missing, which caused the crash.
    # -----------------------

class EquipmentSlot(ButtonBehavior, BoxLayout):
    """A clickable slot for displaying an equipped item, with its icon and stats."""
    def __init__(self, item_obj, theme_manager, **kwargs):
        super().__init__(orientation='horizontal', spacing=dp(10), size_hint_y=None, height=dp(56), **kwargs)
        self.padding = (dp(8), dp(8))
        self._tm = theme_manager

        # --- Background and Border ---
        with self.canvas.before:
            Color(0.1, 0.1, 0.12, 0.7) # Dark, semi-transparent background
            self.bg_rect = RoundedRectangle(pos=self.pos, size=self.size, radius=[dp(8)])
            self._original_border_rgba = self._tm.col("border") if self._tm else (0.5, 0.7, 1.0, 0.6)
            self.border_color = Color(rgba=self._original_border_rgba)
            self.border_line = Line(width=dp(1.5), rounded_rectangle=(self.x, self.y, self.width, self.height, dp(8)))
        self.bind(pos=self._update_canvas, size=self._update_canvas)

        # --- Content ---
        icon = Image(source=_get_icon_for_item(item_obj), size_hint=(None, 1), width=self.height - dp(16))
        self.add_widget(icon)

        info_box = BoxLayout(orientation='vertical', spacing=dp(2))
        if item_obj:
            name_text = f"[b]{item_obj.name}[/b]"
            stats = getattr(item_obj, 'equipment', {}).get('stats', {})
            stats_text = ", ".join([f"{k.upper()}: +{v}" for k, v in stats.items()])
            info_box.add_widget(Label(text=name_text, markup=True, font_name=fonts["medium_text"]["name"], font_size=fonts["medium_text"]["size"], halign='left', valign='bottom'))
            info_box.add_widget(Label(text=stats_text, font_name=fonts["small_text"]["name"], font_size=fonts["small_text"]["size"], color=(0.8, 0.9, 1, 1), halign='left', valign='top'))
        else:
            info_box.add_widget(Label(text="[i]Empty[/i]", markup=True, font_name=fonts["medium_text"]["name"], font_size=fonts["medium_text"]["size"], color=(0.7, 0.7, 0.7, 1)))

        for child in info_box.children:
            child.bind(size=lambda l, s: setattr(l, 'text_size', (s[0], None)))

        self.add_widget(info_box)

    def _update_canvas(self, *args):
        self.bg_rect.pos = self.pos
        self.bg_rect.size = self.size
        self.border_line.rounded_rectangle = (self.x, self.y, self.width, self.height, dp(8))

    def on_press(self):
        self.border_color.rgba = [c * 1.5 for c in self.border_color.rgba[:3]] + [1.0]
    def on_release(self):
        self.border_color.rgba = self._original_border_rgba

class BagScreen(Screen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self._built = False

    def _build_ui(self):
        if self._built: return

        # Use AspectStage to ensure consistent width with JournalScreen
        stage = AspectStage(9, 21)
        stage.stage.padding = [dp(-24), dp(18), dp(-24), dp(22)]
        stage.stage.spacing = dp(12)

        game = App.get_running_app().current_game
        if game:
            tm = game.themes
            fg, bg = tm.col('fg'), tm.col('bg')
            accent = tm.col('accent')

            # Define colors for the new TabButton
            tab_bg_color = tuple(accent[i] * 0.8 for i in range(3)) + (0.92,)
            tab_text_color = tuple(bg)

            self.btn_items = TabButton(text="[b]Items[/b]", markup=True, group="bag_tabs", state="down",
                                       font_name=fonts["popup_button"]["name"], font_size=fonts["popup_button"]["size"],
                                       bg_color=tab_bg_color, color=tab_text_color)
            self.btn_equip = TabButton(text="[b]Equipment[/b]", markup=True, group="bag_tabs",
                                       font_name=fonts["popup_button"]["name"], font_size=fonts["popup_button"]["size"],
                                       bg_color=tab_bg_color, color=tab_text_color)

            strip_radius = dp(26)
            self.btn_items.set_corner_radii([(strip_radius, strip_radius), (0, 0), (0,0), (0,0)])
            self.btn_equip.set_corner_radii([(0, 0), (strip_radius, strip_radius), (0,0), (0,0)])

            inner_tabs = BoxLayout(spacing=dp(2), size_hint_y=None, height=dp(44))
            inner_tabs.add_widget(self.btn_items)
            inner_tabs.add_widget(self.btn_equip)

            self.carousel = Carousel(direction="right", loop=False, scroll_timeout=200)
            self.carousel.bind(index=self._on_tab_switch)
            self.btn_items.bind(on_release=lambda *_: setattr(self.carousel, "index", 0))
            self.btn_equip.bind(on_release=lambda *_: setattr(self.carousel, "index", 1))

            items_slide = self._create_items_view()
            equip_slide = self._create_equipment_view()
            self.carousel.add_widget(items_slide)
            self.carousel.add_widget(equip_slide)

            stage.stage.add_widget(inner_tabs)
            stage.stage.add_widget(self.carousel)
            self.add_widget(stage)
        self._built = True

    def _on_tab_switch(self, carousel, index):
        self.btn_items.state = 'down' if index == 0 else 'normal'
        self.btn_equip.state = 'down' if index == 1 else 'normal'
        self.refresh()

    def on_pre_enter(self, *args):
        self._build_ui()
        self.refresh()

    def refresh(self):
        game = App.get_running_app().current_game
        if not game:
            return
        if not getattr(self, 'selected_character', None) and game.party:
            self.selected_character = game.party[0]
        self._populate_items_view(game.inventory)
        self._populate_party_list(game.party)
        self._populate_equipment_view()

    def _create_items_view(self):
        root = BoxLayout(orientation='vertical', spacing=dp(6), padding=[dp(6), 0, dp(12), 0])

        scroll = ScrollView(do_scroll_x=False, scroll_timeout=250, scroll_distance=dp(20), size_hint=(1, 1))
        self.items_container = BoxLayout(
            orientation='vertical',
            size_hint_y=None,
            spacing=dp(8),
            padding=[dp(5), dp(5)]
        )
        self.items_container.bind(minimum_height=self.items_container.setter('height'))
        scroll.add_widget(self.items_container)
        root.add_widget(scroll)

        game = App.get_running_app().current_game
        tm = getattr(game, "themes", None) if game else None
        border_rgba = tm.col("border") if tm else (0.55, 0.75, 1.0, 0.5)
        accent = tm.col("accent") if tm else (0.8, 0.9, 1.0, 0.9)
        fg = tm.col("fg") if tm else (1.0, 1.0, 1.0, 1.0)

        credits_frame = BorderedFrame(
            orientation='vertical',
            size_hint=(1, None),
            padding=(dp(16), dp(12)),
            border_color=border_rgba,
            radius=[dp(16)]
        )
        credits_frame.height = dp(76)

        caption_lbl = Label(
            text="[b]Credits[/b]",
            markup=True,
            font_name=fonts["small_text"]["name"],
            font_size=fonts["small_text"]["size"] * 1.05,
            color=(accent[0], accent[1], accent[2], 0.92),
            size_hint_y=None,
            halign="left",
            valign="bottom"
        )
        caption_lbl.bind(
            width=lambda inst, width: setattr(inst, "text_size", (width, None)),
            texture_size=lambda inst, size: setattr(inst, "height", max(size[1], dp(20)))
        )

        self.credits_value_label = Label(
            text="0 c",
            font_name=fonts["popup_title"]["name"],
            font_size=fonts["popup_title"]["size"] * 0.9,
            color=(fg[0], fg[1], fg[2], 0.96),
            size_hint_y=None,
            halign="left",
            valign="top"
        )
        self.credits_value_label.bind(
            width=lambda inst, width: setattr(inst, "text_size", (width, None)),
            texture_size=lambda inst, size: setattr(inst, "height", max(size[1], dp(28)))
        )

        credits_frame.add_widget(caption_lbl)
        credits_frame.add_widget(self.credits_value_label)
        root.add_widget(credits_frame)

        return root

    def _populate_items_view(self, inventory):
        self.items_container.clear_widgets()
        game = App.get_running_app().current_game
        if not game:
            return
        if hasattr(self, "credits_value_label"):
            credits_amount = getattr(game, "credits", 0)
            self.credits_value_label.text = f"{credits_amount:,} c"
        equipped = {it.name for c in game.party for it in c.equipment.values() if it}
        consumables, equippables, others = [], [], []
        for name, qty in inventory.items():
            item = game.all_items.find(name)
            if not item: continue
            t = getattr(item, 'type', None)
            if t=='consumable': consumables.append((item, qty))
            elif t=='equippable': equippables.append((item, qty))
            else: others.append((item, qty))
        for lst in (consumables, equippables, others): lst.sort(key=lambda x: x[0].name.lower())

        if consumables:
            self.items_container.add_widget(Label(
                text="[b]Consumables[/b]", markup=True,
                font_name=fonts["section_title"]["name"],
                font_size=fonts["section_title"]["size"],
                size_hint_y=None, height=dp(28)
            ))
            for item, qty in consumables:
                entry = ItemEntry(item, qty, is_equipped=False)
                entry.bind(on_release=partial(self._open_use_on_character_popup, item))
                entry.bind(on_long_press=lambda inst, it=item: self._show_item_details(it))
                self.items_container.add_widget(entry)

        if equippables:
            self.items_container.add_widget(Label(
                text="[b]Equipment[/b]", markup=True,
                font_name=fonts["section_title"]["name"],
                font_size=fonts["section_title"]["size"],
                size_hint_y=None, height=dp(28)
            ))
            for item, qty in equippables:
                is_eq = item.name in equipped
                entry = ItemEntry(item, qty, is_equipped=is_eq)
                entry.bind(on_release=partial(self._open_equip_character_popup, item))
                entry.bind(on_long_press=lambda inst, it=item: self._show_item_details(it))
                self.items_container.add_widget(entry)

        if others:
            self.items_container.add_widget(Label(
                text="[b]Other[/b]", markup=True,
                font_name=fonts["section_title"]["name"],
                font_size=fonts["section_title"]["size"],
                size_hint_y=None, height=dp(28)
            ))
            for item, qty in others:
                entry = ItemEntry(item, qty, is_equipped=False)
                entry.bind(on_release=lambda inst, it=item: self._show_item_details(it))
                entry.bind(on_long_press=lambda inst, it=item: self._show_item_details(it))
                self.items_container.add_widget(entry)

    def _create_equipment_view(self):
        # Tiny left padding prevents the Equipment slide's first border
        # from sitting exactly on the slide edge, which caused a 1px
        # orange sliver to be visible while on the Items tab.
        layout = BoxLayout(
            orientation='horizontal',
            spacing=dp(10),
            padding=[dp(3), 0, 0, 0],  # ← add this
        )

        game = App.get_running_app().current_game
        border_rgba = game.themes.col("border") if game and game.themes else (0.5, 0.7, 1.0, 0.4)

        # --- NEW: A vertical container to hold the party frame and a spacer ---
        party_container = BoxLayout(orientation='vertical', size_hint_x=0.4, padding=(0, dp(2), 0, 0))

        # The frame for the party list
        party_frame = BorderedFrame(
            size_hint=(1, None),  # Take full width of container, height from content
            padding=(dp(8), dp(12)),
            border_color=border_rgba,
            radius=[dp(12)]
        )
        # --- FIX: Bind the frame's height to its child's height PLUS its own padding ---
        def _update_frame_height(grid, height):
            # The height must account for the child, the padding, AND the border width itself.
            party_frame.height = height + (party_frame.padding[1] * 2) + party_frame.border_width

        self.party_list_layout = GridLayout(cols=1, size_hint_y=None, spacing=dp(8))
        self.party_list_layout.bind(minimum_height=self.party_list_layout.setter('height'))
        self.party_list_layout.bind(height=_update_frame_height)
        party_frame.add_widget(self.party_list_layout)

        party_container.add_widget(party_frame)
        # Add a spacer widget to push the party_frame to the top
        party_container.add_widget(Widget())

        self.equipment_layout = BoxLayout(orientation='vertical', size_hint_x=0.6, spacing=dp(10))
        layout.add_widget(party_container)
        layout.add_widget(self.equipment_layout)
        return layout

    def _populate_party_list(self, party):
        self.party_list_layout.clear_widgets()
        for char in party:
            game = App.get_running_app().current_game
            tm = game.themes
            fg, bg, accent = tm.col('fg'), tm.col('bg'), tm.col('accent')
            
            # Define colors for the party selection buttons
            btn_bg_down = tuple(bg[i] * 0.1 + fg[i] * 0.9 for i in range(3)) + (0.95,)
            btn_text_down = (bg[0], bg[1], bg[2], 0.98)
            btn_bg_normal = (bg[0] * 0.3, bg[1] * 0.3, bg[2] * 0.3, 0.85)
            btn_text_normal = (fg[0], fg[1], fg[2], 0.8)

            btn = TabButton(text=char.name, group='party_select',
                state='down' if getattr(self, 'selected_character', None)==char else 'normal',
                size_hint_y=None, height=dp(40),
                bg_color=btn_bg_down, color=btn_text_down)
            # Give these buttons a standard rounded corner look
            btn.set_corner_radii([dp(12)]*4)
            btn.bind(on_release=partial(self._on_character_select, char))
            self.party_list_layout.add_widget(btn)

    def _on_character_select(self, char, btn):
        if btn.state=='down':
            self.selected_character = char
            self._populate_equipment_view()

    def _populate_equipment_view(self):
        self.equipment_layout.clear_widgets()
        char = getattr(self, 'selected_character', None)
        if not char:
            self.equipment_layout.add_widget(Label(text="Select a character",
                                                   font_name=fonts["medium_text"]["name"],
                                                   font_size=fonts["medium_text"]["size"]))
            return
        
        # --- NEW: Header with character name and portrait ---
        header = BoxLayout(orientation='horizontal', size_hint_y=None, height=dp(80), spacing=dp(10))
        
        # Name label on the left
        name_label = Label(
            text=f"[b]{char.name}[/b]", markup=True,
            font_name=fonts["menu_button"]["name"],
            font_size=fonts["menu_button"]["size"],
            halign='left', valign='center',
            size_hint_x=1
        )
        name_label.bind(size=name_label.setter('text_size'))
        header.add_widget(name_label)

        # Portrait on the right
        img_path = f"images/characters/{getattr(char, 'id', '')}_portrait.png"
        if not os.path.exists(img_path):
            img_path = "images/ui/portrait_placeholder.png"
        portrait = Image(source=img_path, size_hint=(None, None), size=(dp(80), dp(80)), allow_stretch=True)
        header.add_widget(portrait)

        self.equipment_layout.add_widget(header)
        # --- END NEW ---
        
        for slot in ['weapon','armor','accessory']:
            self.equipment_layout.add_widget(Label(
                text=f"[b]{slot.upper()}[/b]", markup=True, size_hint_y=None, height=dp(20),
                font_name=fonts["section_title"]["name"], font_size=fonts["section_title"]["size"]-sp(2),
                color=(0.8, 0.9, 1.0, 0.8), halign='left', text_size=(self.equipment_layout.width - dp(20), None)
            ))
            equipped = char.equipment.get(slot)
            game = App.get_running_app().current_game
            slot_widget = EquipmentSlot(equipped, theme_manager=game.themes)
            slot_widget.bind(on_release=partial(self._open_equip_list_popup, slot))
            self.equipment_layout.add_widget(slot_widget)

        # Add a spacer to push everything to the top
        self.equipment_layout.add_widget(Widget())

    def _open_equip_list_popup(self, slot, *args):
        game = App.get_running_app().current_game
        if not game or not getattr(self, 'selected_character', None): return
        actions = []
        for name, qty in game.inventory.items():
            item = game.all_items.find(name)
            if item and getattr(item,'type',None)=='equippable' and item.equipment.get('slot')==slot:
                actions.append((item.name, partial(self._equip_item, item, slot)))
        if self.selected_character.equipment.get(slot):
            actions.insert(0, ("- Unequip -", partial(self._equip_item, None, slot)))
        popup = MenuPopup(title=f"Select {slot.capitalize()}", actions=actions, size_hint=(0.8,0.7))
        popup.open()

    def _perform_equip(self, char, slot, item):
        """Core logic for equipping/unequipping an item on a character."""
        game = App.get_running_app().current_game
        if not game: return

        # Unequip the new item from anyone else who has it.
        if item:
            for c in game.party:
                for s, it in c.equipment.items():
                    if it and it.name == item.name:
                        # If it was a weapon, clear the weapon_type from the old owner.
                        if s == 'weapon' and hasattr(c, 'weapon_type'):
                            del c.weapon_type
                        c.equipment[s] = None

        # Clear the old weapon_type if we're changing the weapon slot on the target character.
        if slot == 'weapon' and hasattr(char, 'weapon_type'):
            del char.weapon_type
        
        # Equip the new item (or unequip if item is None).
        char.equipment[slot] = item

        # Set the new weapon_type if the new item is a weapon.
        if item and slot == 'weapon':
            weapon_type = item.equipment.get('weapon_type')
            if weapon_type:
                char.weapon_type = weapon_type
        
        self.refresh()

    def _equip_item(self, item, slot):
        """Equips an item to the currently selected character."""
        self._perform_equip(self.selected_character, slot, item)

    def _set_equipment(self, char, slot, item):
        """Equips an item to a specified character."""
        self._perform_equip(char, slot, item)

    def _open_equip_character_popup(self, item, *args):
        game = App.get_running_app().current_game
        if not game: return
        slot = item.equipment.get('slot')
        if not slot: return
        actions = []
        for char in game.party:
            curr = char.equipment.get(slot)
            curr_name = curr.name if curr else "None"
            actions.append((f"{char.name} (Current: {curr_name})", partial(self._set_equipment, char, slot, item)))
        actions.append(("- Unequip from all -", partial(self._unequip_item_globally, item)))
        popup = MenuPopup(title=f"Equip {item.name}", actions=actions, size_hint=(0.8,0.7))
        popup.open()

    def _unequip_item_globally(self, item, *args):
        game = App.get_running_app().current_game
        if not game: return
        for char in game.party:
            for s,it in char.equipment.items():
                if it and it.name==item.name: 
                    # Also clear weapon_type if it's a weapon
                    if s == 'weapon' and hasattr(char, 'weapon_type'):
                        del char.weapon_type
                    char.equipment[s]=None
        self.refresh()

    def _open_use_on_character_popup(self, item, *args):
        game = App.get_running_app().current_game
        if not game: return
        def do_use(character):
            game.use_item(item, character)
            self.refresh()
        actions = [(char.name, partial(do_use, char)) for char in game.party]
        popup = MenuPopup(title=f"Use {item.name} on...", actions=actions, size_hint=(0.8,0.7))
        popup.open()

    def _show_item_details(self, item):
        description = getattr(item, "description", "No details available.")
        popup = MenuPopup(
            title=item.name,
            body=Label(
                text=description,
                font_name=fonts["medium_text"]["name"],
                font_size=fonts["medium_text"]["size"],
                halign="left", valign="top"
            ),
            size_hint=(0.7, 0.5)
        )
        popup.open()
