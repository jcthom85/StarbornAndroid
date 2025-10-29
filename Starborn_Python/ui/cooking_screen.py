# ui/cooking_screen.py
from __future__ import annotations
import json, random, math
from pathlib import Path
from functools import partial
from font_manager import fonts
from kivy.uix.image import Image
from kivy.app import App
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.graphics import Color, Line
from kivy.metrics import dp, sp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.modalview import ModalView
from kivy.uix.screenmanager import Screen
from kivy.uix.widget import Widget
from kivy.properties import NumericProperty, StringProperty, ListProperty
from kivy.animation import Animation
from kivy.uix.screenmanager import FadeTransition

from font_manager import fonts
from ui.menu_overlay import MenuOverlay
from ui.menu_popup import MenuPopup
from kivy.uix.gridlayout import GridLayout     # ← new
from kivy.uix.scrollview import ScrollView     # ← new

from ui.bordered_frame import BorderedFrame    # ← new, matches Tinkering screen

class CookingScreen(Screen):
    name = "cooking"
    MAX_SLOTS = 3

    def __init__(self, station_name="Cooking Station", subtitle="Makeshift Camp Stove",
                 background_image="images/ui/starborn_menu_bg.png", **kw):
        super().__init__(**kw)
        self.station_name, self.subtitle, self.background_image = station_name, subtitle, background_image
        self.game, self._overlay = None, None
        self.slots: list[str | None] = [None] * self.MAX_SLOTS
        self.all_recipes: list[dict] = []
        self._load_recipes()

    def on_pre_enter(self, *_):
        self.game = App.get_running_app().current_game
        self._build_overlay()
        self._refresh_slots()

    def on_leave(self, *_):
        if self._overlay:
            Window.remove_widget(self._overlay)
            self._overlay = None

        # NEW – close the fancy transition
        from kivy.app import App
        App.get_running_app().tx_mgr.close()

        if self.game:
            self.game.resume_enemy_timers()

    def _build_overlay(self):
        if self._overlay:
            return
        if self.game:
            self.game.pause_enemy_timers()

        # === ADD a theme manager helper instance for colors ===
        themes = self._room_themes()
        fg = themes.col("fg") if themes else (1,1,1,1)
        bg = themes.col("bg") if themes else (0,0,0,1)

        # ───────── overlay shell ─────────
        from kivy.app import App
        tex = App.get_running_app().last_room_tex
        self._overlay = MenuOverlay(
            default_tab=None,
            title="Let’s Cook!",
            background_tex=tex,
            show_background=False,
        )

        # ── Fade back to explore on close ────────────────────────────────
        from kivy.uix.screenmanager import FadeTransition
        from kivy.clock import Clock
        from kivy.app import App as _App

        def _return_to_explore(*_):
            # close the transition overlay first
            _App.get_running_app().tx_mgr.close()

            # fade the screen change to explore
            sm = self.manager
            if not sm:
                return
            old = sm.transition
            sm.transition = FadeTransition(duration=1.0)
            sm.current = "explore"
            Clock.schedule_once(lambda *_: setattr(sm, "transition", old), 0)

        self._overlay.bind(on_dismiss=_return_to_explore)

        # ───────── body layout ───────────
        content = BoxLayout(orientation="vertical",
                            padding=(dp(18), dp(18)),
                            spacing=dp(14))

        # === workspace (slots + preview) ===
        workspace = BoxLayout(spacing=dp(18), size_hint_y=0.65)

        # –– Ingredient slots ––
        slot_col = BoxLayout(orientation="vertical",
                            spacing=dp(8),
                            size_hint_x=0.5)
        self.slot_widgets: list[BorderedFrame] = []
        for idx in range(self.MAX_SLOTS):
            frm = BorderedFrame(padding=dp(6),
                                border_color=(0.5, 0.7, 1.0, 0.6))
            # --- MODIFIED BUTTON ---
            btn = Button(
                text="[color=777777]Empty[/color]",
                markup=True,
                font_name=fonts["popup_button"]["name"],
                font_size=fonts["popup_button"]["size"],
                background_normal='',
                background_color=(.22, .22, .25, 1), # A dark, neutral empty state
                color=(0.7,0.7,0.7,1)
            )
            btn.bind(on_release=partial(self._choose_ingredient, idx))
            frm.add_widget(btn)

            slot_col.add_widget(frm)
            self.slot_widgets.append(frm)

        workspace.add_widget(slot_col)

        # –– Result / flavour text ––
        self.result_label = Label(text="[i]Choose ingredients…[/i]",
            valign="top",
            halign="left",
            font_name=fonts["medium_text"]["name"],
            font_size=fonts["medium_text"]["size"],
            markup=True)
        self.result_label.bind(size=self.result_label.setter("text_size"))
        preview = BorderedFrame(padding=dp(10),
                                border_color=(0.5, 0.7, 1.0, .4),
                                size_hint_x=0.5)
        preview.add_widget(self.result_label)
        workspace.add_widget(preview)

        content.add_widget(workspace)

        # === bottom controls ===
        bottom = BoxLayout(orientation="vertical",
                        spacing=dp(10),
                        size_hint_y=None,
                        height=dp(120))

        self.message_label = Label(text="",
            font_name=fonts["small_text"]["name"],
            font_size=fonts["small_text"]["size"],
            halign="center",
            markup=True)

        bottom.add_widget(self.message_label)

        # --- MODIFIED COOK BUTTON (Primary Action) ---
        self.btn_cook = Button(text="[b]C O O K[/b]",
            markup=True,
            font_name=fonts["popup_button"]["name"],   # SourceSans3
            font_size=fonts["popup_button"]["size"],
                            size_hint_y=None,
                            height=dp(56),
                            on_release=self._attempt_cook,
                            background_normal='',
                            background_color=(0.35, 0.8, 0.35, 1), # Accent green
                            color=(1, 1, 1, 1)) # White text
        bottom.add_widget(self.btn_cook)

        # --- MODIFIED RECIPES BUTTON (Secondary Action) ---
        btn_recipes = Button(
            text="Recipe Book",
            font_name=fonts["popup_button"]["name"],
            font_size=fonts["popup_button"]["size"],
            size_hint_y=None,
            height=dp(46),
            background_normal='',
            background_color=fg,
            color=bg
        )
        btn_recipes.bind(on_release=lambda *_: self._open_recipe_book())
        bottom.add_widget(btn_recipes)
        content.add_widget(bottom)

        # === finish ===
        self._overlay.content_area.add_widget(content)
        Window.add_widget(self._overlay)

    def _room_themes(self):
        """Return a fresh ThemeManager locked to the current room's env."""
        from theme_manager import ThemeManager
        tm = ThemeManager()
        try:
            room   = getattr(self.game, "current_room", None)
            hub_id = self.game.world_manager.current_hub_id
            hub    = self.game.world_manager.hubs.get(hub_id, {}) if hub_id else {}
            env    = getattr(room, "env", None) or hub.get("theme", "default")
            tm.use(env)
        except Exception:
            pass
        return tm
    
    def _load_recipes(self):
        path = Path(__file__).parent.parent / "data" / "recipes_cooking.json"
        try:
            with path.open("r", encoding="utf8") as f: self.all_recipes = json.load(f)
        except Exception as e:
            print(f"[Cooking] Failed to load recipes: {e}"); self.all_recipes = []

    def _refresh_slots(self):
        themes = self._room_themes()
        fg = themes.col("fg") if themes else (1,1,1,1)
        bg = themes.col("bg") if themes else (0,0,0,1)

        for idx, frm in enumerate(self.slot_widgets):
            btn = frm.children[0]
            ingredient = self.slots[idx]
            if ingredient:
                btn.text = ingredient.title()
                btn.background_color = fg
                btn.color = bg
            else:
                btn.text = "[color=777777]Empty[/color]"
                btn.background_color = (.22, .22, .25, 1)
                btn.color = (0.7,0.7,0.7,1)
            btn.markup = True
        self.btn_cook.disabled = len([s for s in self.slots if s]) < 1 # Can cook with 1 ingredient now
        self.btn_cook.background_color = (0.35, 0.8, 0.35, 1) if not self.btn_cook.disabled else (.5, .5, .5, .5)

    def _choose_ingredient(self, slot_idx: int, *_):
        actions = [("- Clear Slot -", lambda *_: self._set_slot(slot_idx, None))]

        ingredients_in_slots = {item: self.slots.count(item)
                                for item in self.slots if item}

        for name, total_qty in sorted(self.game.inventory.items()):
            itm = self.game.all_items.find(name)
            if total_qty > 0 and getattr(itm, "type", "") != "equippable":
                available_qty = total_qty - ingredients_in_slots.get(name, 0)
                if available_qty > 0:
                    actions.append(
                        (f"{name} (x{available_qty})",          # ← stripped markup
                        lambda _n=name: self._set_slot(slot_idx, _n))
                    )

        if len(actions) == 1:                 # nothing to pick except “clear”
            actions.append(("— No ingredients —", lambda *_: None))  # ★ add this

        MenuPopup("Choose Ingredient", actions=actions, autoclose=True).open()


    def _set_slot(self, idx: int, ingredient: str | None):
        if 0 <= idx < self.MAX_SLOTS:
            self.slots[idx] = ingredient
        self._refresh_slots()

    # ─────────────────────────────────────────────────────────────
    #  COOK – instant version (no timing mini-game)
    # ─────────────────────────────────────────────────────────────
    def _attempt_cook(self, *_):
        """Validate, consume, and grant the success-tier item instantly."""
        actual = [s for s in self.slots if s]
        if len(actual) < 2:
            self.game.narrate("Add at least two ingredients."); return

        req = {n: actual.count(n) for n in actual}
        if any(self.game.inventory.get(n, 0) < c for n, c in req.items()):
            self.game.narrate("[i]You're missing ingredients.[/i]"); return

        # consume inventory
        for n, c in req.items():
            self.game.inventory[n] -= c
            if self.game.inventory[n] <= 0:
                del self.game.inventory[n]
        self.game.update_inventory_display()

        # give result (always 'success')
        recipe = self._match_recipe(req)
        if recipe:
            self._give_item(recipe["result"])
        else:
            self._show_failed_popup()

        self.slots = [None]*self.MAX_SLOTS
        self._refresh_slots()


    def _match_recipe(self, ingredients: dict[str,int]):
        return next((r for r in self.all_recipes if r["ingredients"] == ingredients), None)

    def _give_item(self, item_name: str | None, qty: int = 1):
        if not item_name:
            self.game.narrate("Nothing edible came out."); return
        self.game.inventory[item_name] = self.game.inventory.get(item_name, 0) + qty
        self.game.update_inventory_display()
        self._show_cooked_popup(item_name, qty)

    def _get_unlocked_recipes(self) -> list[dict]:
        """
        Return the list of recipes the player has discovered.
        Replace this stub with your real unlock logic later.
        """
        return self.all_recipes

    # ------------------------------------------------------------
    #  Recipe Book UI
    # ------------------------------------------------------------
    def _open_recipe_book(self, *_):
        print("▶ Recipe book opening…")      # ← make sure this appears in console
        actions = []
        for recipe in self._get_unlocked_recipes():
            actions.append(
                (recipe["name"], lambda r=recipe: self._open_quantity_picker(r))
            )
        if not actions:
            self.game.narrate("You don't know any recipes yet.")
            return
        MenuPopup("Recipes", actions=actions, size_hint=(0.9, 0.9)).open()

        # Starborn color scheme is baked into MenuPopup by default
        
    # ─────────────────────────────────────────────────────────────
    #  Quantity picker popup (Shop-style ± buttons)
    # ─────────────────────────────────────────────────────────────
    def _open_quantity_picker(self, recipe: dict):
        """
        Show a MenuPopup that lets the player pick how many dishes to cook
        (using – / + buttons) before calling _cook_recipe().
        """
        # ── How many can the player actually make? ──────────────────────
        max_craftable = min(
            self.game.inventory.get(ing, 0) // qty
            for ing, qty in recipe["ingredients"].items()
        )
        if max_craftable == 0:
            self.game.narrate("You don’t have the right ingredients.")
            return

        # ── Build the popup body ────────────────────────────────────────
        body = BoxLayout(orientation="vertical",
                        spacing=dp(12),
                        padding=dp(12))
        body.size_hint_y = None                       # ★ add this
        body.bind(minimum_height=lambda b, h: setattr(b, "height", h))  # ★ and this

        # 1) Scrollable ingredients list
        grid = GridLayout(cols=1, spacing=dp(4), size_hint_y=None)
        grid.bind(minimum_height=grid.setter("height"))
        for ing, qty in recipe["ingredients"].items():
            have = self.game.inventory.get(ing, 0)
            colour = "ffffff" if have >= qty else "ff8888"
            lbl = Label(
                text=f"{ing} × {qty}  [color={colour}](you: {have})[/color]",
                markup=True, font_size=sp(18),
                size_hint_y=None, height=dp(26),
                halign="left", valign="middle"
            )
            lbl.bind(size=lbl.setter("text_size"))
            grid.add_widget(lbl)

        sv = ScrollView(size_hint=(1, None), height=dp(110))
        sv.add_widget(grid)
        body.add_widget(sv)

        # 2) Quantity selector ( – | qty | + )
        qty_box = BoxLayout(size_hint_y=None, height=dp(66), spacing=dp(12))

        qty_label = Label(text="1", font_size=sp(44))

        def change_qty(delta):
            new_val = max(1, min(int(qty_label.text) + delta, max_craftable))
            qty_label.text = str(new_val)

        minus_btn = Button(text="–", font_size=sp(48),
                           on_release=lambda *_: change_qty(-1))
        plus_btn  = Button(text="+", font_size=sp(48),
                           on_release=lambda *_: change_qty(1))

        qty_box.add_widget(minus_btn)
        qty_box.add_widget(qty_label)
        qty_box.add_widget(plus_btn)
        body.add_widget(qty_box)

        # ── Action buttons (Cook / Cancel) ───────────────────────────────
        def do_cook():
            self._cook_recipe(recipe, int(qty_label.text))

        actions = [("Cook", do_cook), ("Cancel", None)]

        MenuPopup(
            recipe["name"],
            body=body,
            actions=actions,
            size_hint=(0.9, 0.75),
            theme_mgr=self.game.themes
        ).open()

    def _cook_multiple(self, recipe: dict, qty: int) -> None:
        """Consumes ingredients, grants the food, then shows ONE popup."""
        # Close any open modal
        for w in Window.children[:]:
            if isinstance(w, ModalView):
                w.dismiss()

        # Validate ingredient stock
        needs = {ing: need * qty for ing, need in recipe["ingredients"].items()}
        if any(self.game.inventory.get(n, 0) < c for n, c in needs.items()):
            self.game.narrate("You're out of ingredients for that many.")
            return

        # Build the container (this was missing before)
        box = BoxLayout(orientation="vertical", spacing=dp(12), padding=dp(12),
                        size_hint=(1, None))
        box.bind(minimum_height=lambda b, h: setattr(b, "height", h))

        # ── ingredient list ───────────────────────────────────────────
        grid = GridLayout(cols=1, spacing=dp(6), size_hint_y=None)
        grid.bind(minimum_height=grid.setter("height"))

        for ing, need in recipe["ingredients"].items():
            have = self.game.inventory.get(ing, 0)
            lbl = Label(
                text=f"{ing} × {need}  (you: {have})",
                font_size=sp(16),
                size_hint_y=None,
                height=dp(24),
                halign="left", valign="middle",
            )
            lbl.bind(size=lbl.setter("text_size"))
            grid.add_widget(lbl)

        scroll = ScrollView(size_hint=(1, None), height=dp(100), do_scroll_x=False)
        scroll.add_widget(grid)
        box.add_widget(scroll)  # ← fixed

        # Cook button
        cook_btn = Button(text=f"Cook ×{qty}",
                        size_hint_y=None,
                        height=dp(50),
                        on_release=lambda *_: (self._cook_recipe(recipe, qty) or popup.dismiss()))
        box.add_widget(cook_btn)

        popup = ModalView(size_hint=(0.85, None), height=dp(300),
                        auto_dismiss=True, background_color=(0, 0, 0, 0.8), background='')
        popup.add_widget(box)
        popup.open()

    def _show_recipe_popup(self, recipe: dict):
        box = BoxLayout(orientation="vertical", spacing=dp(14), padding=dp(18))
        title = Label(text=f"[b]{recipe['name']}[/b]",
                    font_size=sp(20), markup=True,
                    size_hint_y=None, height=dp(30))
        box.add_widget(title)

        # ── ingredient list ───────────────────────────────────────────
        grid = GridLayout(cols=1,
                        spacing=dp(6),
                        size_hint_y=None)
        grid.bind(minimum_height=grid.setter("height"))

        for ing, need in recipe["ingredients"].items():
            have = self.game.inventory.get(ing, 0)
            lbl = Label(
                text=f"{ing} × {need}  (you: {have})",
                font_size=sp(16),
                size_hint_y=None,
                height=dp(24),
                halign="left", valign="middle",
            )
            lbl.bind(size=lbl.setter("text_size"))
            grid.add_widget(lbl)

        scroll = ScrollView(size_hint=(1, None),   # ← create the ScrollView
                            height=dp(100),
                            do_scroll_x=False)
        scroll.add_widget(grid)                    # ← attach the GridLayout
        body.add_widget(scroll)                    # ← add to popup body

        # Cook button
        cook_btn = Button(text="Cook",
                        size_hint_y=None,
                        height=dp(50),
                        on_release=lambda *_: (self._cook_recipe(recipe) or popup.dismiss()))

        box.add_widget(cook_btn)

        popup = ModalView(size_hint=(0.85, None),
                        height=dp(300),
                        auto_dismiss=True,
                        background_color=(0, 0, 0, 0.8),
                        background='')
        popup.add_widget(box)
        popup.open()

    def _show_cooked_popup(self, item_name: str, qty: int = 1) -> None:
        body = BoxLayout(orientation="vertical",
                        spacing=dp(12),
                        padding=dp(16),
                        size_hint=(1, None))               # let the body grow
        body.bind(minimum_height=lambda b, h: setattr(b, "height", h))

        # build text ───────────────────────────────────────────
        line = (f"You cooked [b]{item_name} x{qty}![/b]"
                if qty > 1 else
                f"You cooked [b]{item_name}![/b]")

        lbl = Label(text=line,
                    markup=True,
                    font_size=sp(20),
                    halign="center", valign="middle",
                    size_hint=(1, None))                    # ↙ dynamic height
        lbl.bind(
            width=lambda l, w: setattr(l, "text_size", (w, None)),
            texture_size=lambda l, ts: setattr(l, "height", ts[1])
        )
        body.add_widget(lbl)

        MenuPopup(
            title="Bon Appétit!",
            body=body,
            actions=[("OK", None)],
            size_hint=(0.6, None),          # height now driven by content
            theme_mgr=self.game.themes
        ).open()

    # ─────────────────────────────────────────────────────────────
    #  FAILURE popup (unknown / bad combo)
    # ─────────────────────────────────────────────────────────────
    def _show_failed_popup(self) -> None:
        body = BoxLayout(orientation="vertical",
                        spacing=dp(12),
                        padding=dp(16),
                        size_hint=(1, None))          # let body grow
        body.bind(minimum_height=lambda b, h: setattr(b, "height", h))

        line = "The mixture fizzles into an inedible mess."

        lbl = Label(text=line,
                    font_size=sp(20),
                    halign="center", valign="middle",
                    size_hint=(1, None))               # dynamic height
        lbl.bind(
            width=lambda l, w: setattr(l, "text_size", (w, None)),
            texture_size=lambda l, ts: setattr(l, "height", ts[1])
        )
        body.add_widget(lbl)

        MenuPopup(
            title="Uh oh…",
            body=body,
            actions=[("OK", None)],
            size_hint=(0.6, None),     # height driven by content
            theme_mgr=self.game.themes
        ).open()

    # ─────────────────────────────────────────────────────────────
    #  COOK – instant version (supports quantity)
    # ─────────────────────────────────────────────────────────────
    def _cook_recipe(self, recipe: dict, quantity: int = 1):
        """
        Consume ingredients *quantity* times and grant the recipe's single-tier
        result. Works with the new JSON schema (uses recipe['result']).
        """
        # 1) Build dict of total ingredients needed
        needed = {ing: qty * quantity for ing, qty in recipe["ingredients"].items()}

        # 2) Validate inventory
        if any(self.game.inventory.get(ing, 0) < req for ing, req in needed.items()):
            self.game.narrate("You're missing ingredients.")
            return

        # 3) Consume
        for ing, req in needed.items():
            new_amt = self.game.inventory[ing] - req
            if new_amt > 0:
                self.game.inventory[ing] = new_amt
            else:
                self.game.inventory.pop(ing, None)
        self.game.update_inventory_display()

        # 4) Grant items
        result_name = recipe["result"]
        self.game.inventory[result_name] = self.game.inventory.get(result_name, 0) + quantity
        self.game.update_inventory_display()

        # 5) Show success popup (replaces text spam)
        self._show_cooked_popup(result_name, quantity)

        # 6) Reset slots
        self.slots = [None] * self.MAX_SLOTS
        self._refresh_slots()
