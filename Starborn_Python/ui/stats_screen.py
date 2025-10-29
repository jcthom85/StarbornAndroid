# stats_screen.py

import os, json
from functools import partial

from kivy.app import App
from kivy.metrics import dp, sp
from kivy.uix.screenmanager import Screen
from kivy.uix.scrollview import ScrollView
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.anchorlayout import AnchorLayout
from kivy.uix.image import Image
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.uix.carousel import Carousel
from kivy.uix.gridlayout import GridLayout
from kivy.uix.progressbar import ProgressBar
from kivy.uix.widget import Widget
from kivy.clock import Clock
from kivy.core.window import Window

from kivy.graphics import Color, Rectangle # only for the HP/XP progress fills

from font_manager import fonts
from theme_manager import ThemeManager
from ui.themed_button import ThemedButton
from skill_tree_manager import SkillTreeManager as STM


class StatsScreen(Screen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self._built = False

    def _build_ui(self):
        if self._built: return
        self._scroll = ScrollView(do_scroll_x=False, bar_width=0)
        self._root = BoxLayout(orientation="vertical", spacing=dp(20),
                               padding=[dp(8), dp(8)], size_hint_y=None)
        self._root.bind(minimum_height=self._root.setter("height"))
        self._scroll.add_widget(self._root)
        self.add_widget(self._scroll)
        self._built = True

    def on_pre_enter(self, *_):
        self._build_ui()
        self.refresh()

    def refresh(self):
        self._root.clear_widgets()
        app = App.get_running_app()
        if not hasattr(app, "current_game") or not app.current_game or not app.current_game.party:
            return
        for ch in app.current_game.party:
            self._root.add_widget(self._build_character_panel(ch))

    # --- theme ---
    def _get_starborn_theme_manager(self):
        tm = ThemeManager()
        tm.use("starborn")
        return tm

    # --- character panel ---
    def _build_character_panel(self, ch):
        leveling_manager = App.get_running_app().current_game.leveling_manager

        panel = BoxLayout(orientation="vertical", spacing=dp(8), size_hint=(1, None))
        row = BoxLayout(orientation="horizontal", spacing=dp(10), size_hint_y=None, height=dp(80))

        img_path = f"images/characters/{getattr(ch, 'id', '')}_portrait.png"
        if not os.path.exists(img_path):
            img_path = "images/ui/portrait_placeholder.png"
        portrait = Image(source=img_path, size_hint=(None, None), size=(dp(80), dp(80)), allow_stretch=True)
        row.add_widget(portrait)

        stats_box = BoxLayout(orientation="vertical", spacing=dp(2))
        stats_box.add_widget(Label(text=f"[b]{ch.name}[/b]", markup=True,
                                   font_name=fonts["medium_text"]["name"], font_size=fonts["medium_text"]["size"],
                                   halign="left", size_hint_y=None, height=dp(22)))
        stats_box.add_widget(Label(text=f"Lv {ch.level}",
                                   font_name=fonts["small_text"]["name"], font_size=fonts["small_text"]["size"],
                                   halign="left", size_hint_y=None, height=dp(18)))
        stats_box.add_widget(Label(text=f"HP {ch.hp}/{ch.total_max_hp}",
                                   font_name=fonts["small_text"]["name"], font_size=fonts["small_text"]["size"],
                                   halign="left", size_hint_y=None, height=dp(18)))

        start_xp, next_level_xp = leveling_manager.get_level_bounds(ch.level)
        bar_thickness = dp(4)

        if next_level_xp is not None:
            xp_needed_for_level = next_level_xp - start_xp
            xp_progress_in_level = ch.xp - start_xp
            xp_text = f"{xp_progress_in_level} / {xp_needed_for_level} XP to next Lv"
            hp_bar = ProgressBar(max=ch.total_max_hp, value=ch.hp, size_hint_y=None, height=bar_thickness)
            xp_bar = ProgressBar(max=xp_needed_for_level, value=xp_progress_in_level, size_hint_y=None, height=bar_thickness)
        else:
            xp_text = "Max Level Reached"
            hp_bar = ProgressBar(max=ch.total_max_hp, value=ch.hp, size_hint_y=None, height=bar_thickness)
            xp_bar = ProgressBar(max=1, value=1, size_hint_y=None, height=bar_thickness)

        # simple fills for the bars (kept)
        with hp_bar.canvas.after:
            Color(0.2, 0.6, 1, 1)
            hp_rect = Rectangle(pos=hp_bar.pos, size=(0, hp_bar.height))

        def _update_hp_rect(*_):
            ratio = (hp_bar.value / hp_bar.max) if hp_bar.max > 0 else 0
            hp_rect.pos = hp_bar.pos
            hp_rect.size = (hp_bar.width * ratio, hp_bar.height)

        hp_bar.bind(value=_update_hp_rect, size=_update_hp_rect, pos=_update_hp_rect)
        _update_hp_rect()

        with xp_bar.canvas.after:
            Color(0.6, 0.2, 0.8, 1)
            xp_rect = Rectangle(pos=xp_bar.pos, size=(0, xp_bar.height))

        def _update_xp_rect(*_):
            ratio = (xp_bar.value / xp_bar.max) if xp_bar.max > 0 else 1.0
            xp_rect.pos = xp_bar.pos
            xp_rect.size = (xp_bar.width * ratio, xp_bar.height)

        xp_bar.bind(value=_update_xp_rect, size=_update_xp_rect, pos=_update_xp_rect)
        _update_xp_rect()

        stats_box.add_widget(Label(text=xp_text,
                                   font_name=fonts["small_text"]["name"], font_size=fonts["small_text"]["size"],
                                   halign="left", size_hint_y=None, height=dp(18)))

        stats_box.add_widget(hp_bar)
        stats_box.add_widget(xp_bar)
        row.add_widget(stats_box)
        panel.add_widget(row)

        app = App.get_running_app()
        game = app.current_game
        tm = game.themes
        fg, bg = tm.col('fg'), tm.col('bg')

        btns = BoxLayout(size_hint_y=None, height=dp(44), spacing=dp(8))
        btns.add_widget(ThemedButton(
            text="Details",
            on_release=lambda *_: self._show_details(ch),
            font_name=fonts["popup_button"]["name"],
            font_size=fonts["popup_button"]["size"], bg_color=fg, color=bg
        ))
        btns.add_widget(ThemedButton(
            text="Skills",
            on_release=lambda *_: self._show_skills(ch),
            font_name=fonts["popup_button"]["name"],
            font_size=fonts["popup_button"]["size"], bg_color=fg, color=bg
        ))
        panel.add_widget(btns)

        panel.height = row.height + btns.height + dp(12)
        return panel

    def _show_details(self, ch):
        """Shows a popup with the character's detailed stats."""
        tm = self._get_starborn_theme_manager()
        fg = tm.col("fg")

        # Scrollable body for potentially long stat lists
        body_scroll = ScrollView(do_scroll_x=False, bar_width=dp(4))
        content = GridLayout(cols=2, spacing=dp(10), padding=dp(12), size_hint_y=None)
        content.bind(minimum_height=content.setter('height'))
        body_scroll.add_widget(content)

        def add_stat(name: str, value):
            """Helper to add a stat row to the grid."""
            content.add_widget(Label(
                text=f"{name}:", halign='right', color=fg,
                font_name=fonts["medium_text"]["name"], font_size=fonts["medium_text"]["size"],
                size_hint_y=None, height=dp(24)
            ))
            content.add_widget(Label(
                text=str(value), halign='left', color=fg,
                font_name=fonts["medium_text"]["name"], font_size=fonts["medium_text"]["size"],
                size_hint_y=None, height=dp(24)
            ))

        # --- Primary Attributes ---
        add_stat("Strength", ch.total_strength)
        add_stat("Vitality", ch.total_vitality)
        add_stat("Agility", ch.total_agility)
        add_stat("Focus", ch.total_focus)
        add_stat("Luck", ch.total_luck)

        # --- Derived Stats ---
        content.add_widget(Widget(size_hint_y=None, height=dp(10))) # Spacer
        content.add_widget(Widget(size_hint_y=None, height=dp(10))) # Spacer
        add_stat("Attack", ch.total_atk)
        add_stat("Defense", ch.total_def)
        add_stat("Speed", f"{ch.total_spd:.1f}")
        add_stat("Accuracy", f"{ch.total_accuracy:.1f}%")
        add_stat("Evasion", f"{ch.total_evasion:.1f}%")
        add_stat("Crit Rate", f"{ch.total_crit_rate:.1f}%")

        # Local import to break circular dependency
        from ui.menu_overlay import MenuOverlay
        # Use MenuOverlay for a slide-up panel instead of a centered popup.
        overlay = MenuOverlay(
            default_tab=None, # This creates a simple, non-tabbed overlay
            title=f"{ch.name} - Details",
            show_background=True
        )
        # Add the scrollable stat list to the overlay's content area.
        overlay.content_area.add_widget(body_scroll)
        # Add the overlay to the main window to display it.
        Window.add_widget(overlay)

    # --- skills popup ---
    def _show_skills(self, ch):
        tm = self._get_starborn_theme_manager()
        tree_path = os.path.join("skill_trees", f"{ch.id}.json")
        try:
            with open(tree_path, encoding="utf8") as fp:
                tree = json.load(fp)
        except Exception:
            # If the file is missing or invalid, proceed with an empty tree.
            tree = {}

        NODE_SIZE = dp(88)
        GRID_SPACING = dp(8)
        grid_height = 6 * NODE_SIZE + 5 * GRID_SPACING
        carousel_height = grid_height + dp(70)

        carousel = Carousel(direction="right", loop=True, size_hint=(1, None),
                            height=carousel_height, scroll_timeout=200)

        app = App.get_running_app()
        if not hasattr(app, "current_game") or not app.current_game:
            return # Can't build skills without a game
        stm = STM(app.current_game)

        for branch_name, nodes in tree.get("branches", {}).items():
            slide = self._build_branch_slide(branch_name, nodes, ch, stm)
            carousel.add_widget(slide)

        # Local import to break circular dependency
        from ui.menu_overlay import MenuOverlay
        # Use MenuOverlay for the skills screen as well.
        overlay = MenuOverlay(
            default_tab=None,
            title=f"{ch.name} – Skills",
            show_background=True
        )
        overlay.content_area.add_widget(carousel)
        Window.add_widget(overlay)

    def _build_branch_slide(self, title, nodes, ch, stm):
        tm = self._get_starborn_theme_manager()

        slide = BoxLayout(orientation="vertical", spacing=dp(10), padding=[dp(6), dp(6)])
        slide.add_widget(Label(
            text=title,
            font_name=fonts["popup_title"]["name"],
            font_size=fonts["popup_title"]["size"],
            size_hint_y=None, height=sp(32),
            color=tm.col("fg")
        ))

        NODE_SIZE = dp(88)
        GRID_SPACING = dp(8)

        grid = GridLayout(rows=6, cols=3, spacing=GRID_SPACING, size_hint=(None, None))
        grid.width = 3 * NODE_SIZE + 2 * GRID_SPACING
        grid.height = 6 * NODE_SIZE + 5 * GRID_SPACING
        grid._nodes = nodes # stash for details

        self._populate_skill_grid(grid, ch, stm)

        grid_container = AnchorLayout(anchor_x="center")
        grid_container.add_widget(grid)
        slide.add_widget(grid_container)
        return slide

    # placeholder detection (kept so blanks are truly blank)
    @staticmethod
    def _is_placeholder_node(node: dict) -> bool:
        if node is None:
            return True
        if node.get("hidden") or node.get("placeholder"):
            return True
        node_id = node.get("id")
        if node_id in (None, "", "-"):
            return True
        name = (node.get("name") or "").strip()
        if not name:
            return True
        return False

    # fill the 6x3 grid (no custom art)
    def _populate_skill_grid(self, grid, ch, stm):
        grid.clear_widgets()
        grid.canvas.after.clear() # make sure nothing leftover draws

        pos_map = {(n["pos"][1], n["pos"][0]): n for n in grid._nodes}
        node_widgets = {}
        available_ids = {n["id"] for n in stm.available_nodes(ch)}

        NODE_SIZE = dp(88)
        dim_text = (0.6, 0.6, 0.6, 1)

        for r in range(6):
            for c in range(3):
                node = pos_map.get((r, c))
                if self._is_placeholder_node(node):
                    grid.add_widget(Widget(size_hint=(None, None), size=(NODE_SIZE, NODE_SIZE)))
                    continue

                btn = Button(
                    size_hint=(None, None), size=(NODE_SIZE, NODE_SIZE),
                    font_name=fonts["popup_button"]["name"], font_size=sp(11),
                    text_size=(NODE_SIZE - dp(10), None), halign="center", valign="center",
                    markup=True,
                    # transparent; no themed background either
                    background_normal="", background_down="", background_color=(0, 0, 0, 0)
                )

                cost = node.get("cost_ap", 0)
                sub_fs = int(sp(9))
                if node["id"] in ch.unlocked_abilities:
                    btn.text = f"[b]{node['name']}[/b]\n[size={sub_fs}](Learned)[/size]"
                elif node["id"] in available_ids:
                    btn.text = f"[b]{node['name']}[/b]\n[size={sub_fs}]Cost: {cost} AP[/size]"
                else:
                    btn.text = f"{node['name']}\n[size={sub_fs}]Cost: {cost} AP[/size]"
                    btn.color = dim_text

                btn.bind(on_release=lambda _b, n=node: self._show_skill_details(ch, grid, stm, n))
                node_widgets[node["id"]] = btn
                grid.add_widget(btn)

        # NOTE: We intentionally do NOT draw prerequisite lines anymore.

    # --- skill details popup ---
    def _show_skill_details(self, ch, grid, stm, node):
        tm = self._get_starborn_theme_manager()

        def format_effect(effect):
            etype = effect.get("type", "N/A").title()
            if etype == "Buff":
                bt = effect.get("buff_type", "").replace("_", " ").title()
                return f"Effect: +{effect.get('value', 0)} {bt}."
            if etype == "Damage":
                mult = effect.get("mult", 0)
                return f"Effect: Deals damage with a x{mult} multiplier."
            if etype == "Utility":
                return f"Effect: Grants the '{effect.get('subtype', 'utility').title()}' ability."
            if etype == "Heal":
                return f"Effect: Restores {effect.get('value', 0)} HP."
            return "Effect: Special ability."

        # Scrollable body that resizes to fit its content, with a max height.
        body_scroll = ScrollView(do_scroll_x=False, do_scroll_y=True, bar_width=0, size_hint=(1, None))

        PAD_X = dp(16)
        content = BoxLayout(orientation="vertical", spacing=dp(8),
                            padding=[PAD_X, dp(12), PAD_X, dp(12)], size_hint_y=None)
        content.bind(minimum_height=content.setter("height"))

        def set_scroll_height(instance, height):
            # Set the ScrollView's height to be the content height, but no more
            # than 60% of the total window height.
            max_h = Window.height * 0.6
            body_scroll.height = min(height, max_h)

        # Bind our new capping function to the content's height property.
        content.bind(height=set_scroll_height)

        body_scroll.add_widget(content)
        
        fg = tm.col("fg")

        # A simplified and robust function to create word-wrapping labels
        def make_wrapped(text, font_key, *, bold=False, align="left"):
            lbl = Label(
                text=f"[b]{text}[/b]" if bold else text,
                markup=True,
                font_name=fonts[font_key]["name"],
                font_size=fonts[font_key]["size"],
                color=fg,
                halign=align,
                valign="top",
                size_hint=(1, None),
            )

            # These two bindings are the standard Kivy way to make a label
            # auto-wrap and resize its height to fit its content.
            lbl.bind(width=lambda instance, width: setattr(instance, 'text_size', (width, None)))
            lbl.bind(texture_size=lambda instance, size: setattr(instance, 'height', size[1]))
            return lbl

        # Cost
        content.add_widget(make_wrapped(f"Cost: {node.get('cost_ap', 0)} Ability Points", "medium_text"))

        # Requirements
        reqs = node.get("requires", [])
        if reqs:
            all_nodes = {n["id"]: n["name"] for n in grid._nodes}
            req_names = ", ".join(all_nodes.get(r, r.replace("_", " ").title()) for r in reqs)
            content.add_widget(make_wrapped(f"Requires: {req_names}", "small_text"))

        # Effect
        content.add_widget(make_wrapped(format_effect(node.get("effect", {})), "medium_text"))

        # Actions
        actions = []
        if node["id"] in {n["id"] for n in stm.available_nodes(ch)}:
            actions.append(("Purchase", lambda *_: (popup.dismiss(), self._purchase_skill(ch, grid, stm, node))))

        # Use MenuPopup here for the smaller details view, as it's a sub-popup.
        from ui.menu_popup import MenuPopup
        popup = MenuPopup(title=node["name"], body=body_scroll, actions=actions,
                        size_hint=(0.6, None), theme_mgr=tm)
        popup.open()

    def _purchase_skill(self, ch, grid, stm, node):
        if stm.unlock(ch, node["id"]):
            carousel = grid.parent.parent.parent
            for slide in carousel.slides:
                grid_widget = slide.children[0].children[0]
                self._populate_skill_grid(grid_widget, ch, stm)
        else:
            pass