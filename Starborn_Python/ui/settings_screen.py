# ui/settings_screen.py
from __future__ import annotations

from kivy.app import App
from kivy.metrics import dp, sp
from kivy.properties import DictProperty
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.screenmanager import Screen
from kivy.uix.switch import Switch
from kivy.uix.togglebutton import ToggleButton
from kivy.uix.widget import Widget

from font_manager import fonts


class SettingsScreen(Screen):
    settings = DictProperty({})

    def __init__(self, **kw):
        super().__init__(**kw)
        self._built = False

    def _build_ui(self):
        if self._built: return
        self.app = App.get_running_app()
        self.settings = getattr(self.app, "settings", {})

        root = BoxLayout(orientation='vertical', spacing=dp(10), padding=(dp(20), dp(10)))

        root.add_widget(Label(
            text="Settings",
            font_name=fonts["popup_title"]["name"],
            font_size=fonts["popup_title"]["size"],
            size_hint_y=None, height=dp(40)
        ))

        toggles_grid = GridLayout(cols=2, spacing=dp(10), size_hint_y=None, height=dp(150))
        self._toggles = {}
        for key, text in [
            ('screenshake', 'Disable Screenshake'),
            ('flashes',     'Disable Color Flashes'),
            ('haptics',     'Disable Haptics'),
        ]:
            toggles_grid.add_widget(Label(text=text, halign='left',
                                          font_name=fonts["medium_text"]["name"],
                                          font_size=fonts["medium_text"]["size"]))
            sw = Switch(active=self.settings.get(key, False))
            sw.bind(active=lambda inst, val, k=key: self._on_setting_change(k, val))
            self._toggles[key] = sw
            toggles_grid.add_widget(sw)
        root.add_widget(toggles_grid)

        buttons_col = BoxLayout(orientation='vertical', spacing=dp(10), size_hint_y=None, height=dp(100))
        if hasattr(self.app, "current_game") and self.app.current_game:
            save_btn = Button(text="Save Game", size_hint_y=None, height=dp(44),
                              font_name=fonts["popup_button"]["name"],
                              font_size=fonts["popup_button"]["size"])
            save_btn.bind(on_release=lambda *a: getattr(self.app.current_game, "prompt_save_slot", lambda: None)())
            buttons_col.add_widget(save_btn)

        load_btn = Button(text="Load Game", size_hint_y=None, height=dp(44),
                          font_name=fonts["popup_button"]["name"],
                          font_size=fonts["popup_button"]["size"])
        load_btn.bind(on_release=lambda *a: self.app.prompt_load_slot())
        buttons_col.add_widget(load_btn)
        root.add_widget(buttons_col)

        back_btn = Button(
            text="Back",
            size_hint_y=None, height=dp(44),
            font_name=fonts["popup_button"]["name"],
            font_size=fonts["popup_button"]["size"]
        )
        def _go_back(*_):
            if hasattr(self.app, "current_game") and self.app.current_game:
                # If in a game, just close the menu overlay
                if self.parent and hasattr(self.parent.parent, 'dismiss'):
                    self.parent.parent.dismiss()
            else:
                # If on main menu, go back to main menu screen
                if self.manager:
                    self.manager.current = "menu"
        back_btn.bind(on_release=_go_back)
        root.add_widget(back_btn)

        self.add_widget(root)
        self._built = True

    def on_pre_enter(self, *a):
        self._build_ui()
        # sync switches with current app settings
        for k, sw in self._toggles.items():
            sw.active = self.app.settings.get(k, False)

    def _on_setting_change(self, key, value):
        self.app.settings[key] = value
        # persist immediately
        if hasattr(self.app, "save_settings"):
            self.app.save_settings()
