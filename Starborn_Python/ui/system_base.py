# ui/system_base.py
from __future__ import annotations
from kivy.uix.screenmanager import Screen
from kivy.properties import StringProperty, ObjectProperty
from kivy.app import App
from ui.menu_overlay import MenuOverlay   # uses the sliding panel
from ui.menu_popup import InfoPopup       # generic popup (see below)

class SystemBase(Screen):
    """Common behavior for Shop/Cooking/Tinkering/Fishing."""
    # Override in subclasses:
    SYSTEM_KIND = "shop"           # one of: "shop", "cooking", "tinkering", "fishing"
    TITLE = "Menu"                 # "Let's Fish!", "Let's Tinker!", etc.
    NPC_NAME = ""                  # optional, used for top header
    NPC_PORTRAIT = ""              # optional image path

    overlay: MenuOverlay = ObjectProperty(None)
    _is_open = False

    def on_pre_enter(self, *args):
        """Play the blur+vignette transition and then slide the overlay up."""
        if self._is_open:
            return
        self._is_open = True
        app = App.get_running_app()
        # transition overlay -> open with our kind, then call _show_overlay
        # tx_mgr must call our callback *after* blur + vignette fade-in
        app.tx_mgr.open(self.SYSTEM_KIND, on_midway=self._show_overlay)

    def _show_overlay(self, *_):
        """Build the overlay (title, subtitle, portrait), then animate it in."""
        if self.overlay:
            return
        self.overlay = MenuOverlay(
            title=self.TITLE,
            subtitle=self.NPC_NAME,
            avatar_source=self.NPC_PORTRAIT
        )
        self.overlay.bind(on_request_close=lambda *_: self.close_system())
        self.add_widget(self.overlay)
        self.overlay.open()

    def notify(self, title: str, body: str):
        """Small helper for consistent popups."""
        InfoPopup.open_simple(title, body)

    def close_system(self):
        """Slide overlay down, then unblur and return to room."""
        if not self.overlay:
            return
        ov = self.overlay
        self.overlay = None

        def _after_overlay():
            app = App.get_running_app()
            app.tx_mgr.close(on_complete=self._finish_close)

        ov.close(on_complete=_after_overlay)

    def _finish_close(self, *_):
        self._is_open = False
        if self.parent and hasattr(self.parent, 'current'):
            # Let ScreenManager swap back if needed
            pass
