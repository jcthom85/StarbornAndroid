from __future__ import annotations

from typing import Any, Callable, Dict, List, Optional

from kivy.app import App
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.graphics import Color, Line, RoundedRectangle
from kivy.metrics import dp
from kivy.uix.behaviors import ButtonBehavior
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label

from font_manager import fonts
from theme_manager import ThemeManager


HINT_CONTEXTS = {"overlay", "menu_overlay", "tinkering"}


class OverlayTutorialHint(ButtonBehavior, BoxLayout):
    """Lightweight hint bubble that floats above system overlays."""

    def __init__(self, **kwargs):
        super().__init__(orientation="vertical", **kwargs)
        self.size_hint = (None, None)
        self.padding = (dp(16), dp(14), dp(16), dp(18))
        self.spacing = dp(6)
        self.opacity = 0
        self.visible = False
        self.dismissible = True
        self._dismiss_cb: Optional[Callable[[], None]] = None
        self._window_bound = False
        self._anchor: str = "bottom"
        self._width_ratio: float = 0.54
        self._vertical_margin = dp(28)
        self._max_width = dp(420)
        self._min_width = dp(200)

        self._theme = ThemeManager()
        fg = self._theme.col("fg")
        bg = self._theme.col("bg")
        border = self._theme.col("border")

        with self.canvas.before:
            self._bg_color = Color(bg[0], bg[1], bg[2], 0.92)
            self._bg_rect = RoundedRectangle(radius=[dp(14)], pos=self.pos, size=self.size)
            self._border_color = Color(border[0], border[1], border[2], 0.85)
            self._border = Line(rounded_rectangle=(self.x, self.y, self.width, self.height, dp(14)), width=dp(1.4))

        self.bind(pos=self._update_canvas, size=self._update_canvas)

        base_font = fonts["dialogue_text"]["size"]
        self._body_label = Label(
            text="",
            markup=True,
            font_name=fonts["dialogue_text"]["name"],
            font_size=base_font * 0.5,
            halign="left",
            valign="top",
            size_hint=(1, None),
            color=fg,
        )
        self._body_label.bind(texture_size=lambda *_: self._reflow())
        self.add_widget(self._body_label)

        self._cta_label = Label(
            text="[i]Tap to continue[/i]",
            markup=True,
            font_name=fonts["dialogue_text"]["name"],
            font_size=base_font * 0.4,
            halign="center",
            valign="middle",
            size_hint=(1, None),
            color=(fg[0], fg[1], fg[2], 0.8),
        )
        self._cta_label.bind(texture_size=lambda *_: self._reflow())
        self.add_widget(self._cta_label)

    # ------------------------------------------------------------------ hint API
    def apply_theme(self, theme: Optional[ThemeManager]) -> None:
        mgr = theme or self._theme
        fg = mgr.col("fg")
        bg = mgr.col("bg")
        border = mgr.col("border")

        self._bg_color.rgba = (bg[0], bg[1], bg[2], 0.92)
        self._border_color.rgba = (border[0], border[1], border[2], 0.85)
        self._body_label.color = fg
        self._cta_label.color = (fg[0], fg[1], fg[2], 0.8)

    def set_text(self, text: str) -> None:
        self._body_label.text = text or ""
        Clock.schedule_once(lambda *_: self._reflow(), 0)

    def set_layout(self, *, anchor: Optional[str] = None, width_ratio: Optional[float] = None,
                   vertical_margin: Optional[float] = None) -> None:
        if anchor:
            anchor = anchor.lower()
            if anchor in {"top", "middle", "center", "bottom"}:
                self._anchor = "middle" if anchor in {"middle", "center"} else anchor
        if width_ratio:
            try:
                self._width_ratio = max(0.35, min(0.9, float(width_ratio)))
            except (TypeError, ValueError):
                pass
        if vertical_margin is not None:
            try:
                self._vertical_margin = float(vertical_margin)
            except (TypeError, ValueError):
                pass
        Clock.schedule_once(lambda *_: self._reflow(), 0)

    def set_dismissible(self, value: bool) -> None:
        self.dismissible = bool(value)
        self._cta_label.opacity = 1.0 if self.dismissible else 0.0
        self._cta_label.size_hint_y = None
        if self.dismissible:
            self._cta_label.text = "[i]Tap to continue[/i]"
        else:
            self._cta_label.text = ""
        Clock.schedule_once(lambda *_: self._reflow(), 0)

    def set_on_dismiss(self, callback: Optional[Callable[[], None]]) -> None:
        self._dismiss_cb = callback

    def show(self, parent) -> None:
        if self.parent is not parent:
            if self.parent:
                self.parent.remove_widget(self)
            parent.add_widget(self)
        if not self._window_bound:
            Window.bind(size=self._on_window_size)
            self._window_bound = True
        self.visible = True
        self.opacity = 1
        Clock.schedule_once(lambda *_: self._reflow(), 0)

    def hide(self) -> None:
        if self._window_bound:
            Window.unbind(size=self._on_window_size)
            self._window_bound = False
        self.visible = False
        self.opacity = 0
        if self.parent:
            self.parent.remove_widget(self)

    # ------------------------------------------------------------------ layout / touch
    def _on_window_size(self, *_):
        self._reflow()

    def _reflow(self, *_):
        width = Window.width * self._width_ratio
        width = min(width, self._max_width)
        width = max(width, self._min_width)
        self.width = width
        text_width = width - (self.padding[0] + self.padding[2])
        self._body_label.text_size = (text_width, None)
        self._body_label.texture_update()
        body_h = max(self._body_label.texture_size[1], 0)
        self._body_label.height = body_h

        if self.dismissible:
            self._cta_label.texture_update()
            cta_h = self._cta_label.texture_size[1]
        else:
            cta_h = 0
        self._cta_label.height = cta_h

        total = self.padding[1] + body_h + (self.spacing if self.dismissible and cta_h else 0) + cta_h + self.padding[3]
        self.height = total

        x = (Window.width - width) * 0.5
        margin = self._vertical_margin
        if self._anchor == "top":
            y = Window.height - total - margin
        elif self._anchor == "middle":
            y = (Window.height - total) * 0.5
        else:
            y = margin
        self.pos = (x, y)

    def _update_canvas(self, *_):
        self._bg_rect.pos = self.pos
        self._bg_rect.size = self.size
        self._border.rounded_rectangle = (self.x, self.y, self.width, self.height, dp(18))

    def on_touch_down(self, touch):
        if not self.visible or not self.dismissible:
            return super().on_touch_down(touch)
        if not self.collide_point(*touch.pos):
            return super().on_touch_down(touch)
        if self._dismiss_cb:
            cb, self._dismiss_cb = self._dismiss_cb, None
            cb()
        return True


class SystemTutorialRunner:
    """Drives dialogue-based tutorials over system UI screens."""

    def __init__(self, manager: "SystemTutorialManager",
                 steps: List[Dict[str, Any]],
                 context: Optional[str] = None):
        self.manager = manager
        self.game = manager.game
        self.steps = [s for s in steps if isinstance(s, dict) and s.get("type") != "scene_meta"]
        self.context = (context or "").lower()
        self._idx = 0
        self._wait_cb = None
        self._pending_wait: Optional[Dict[str, Any]] = None
        self._on_complete_actions: List[Dict[str, Any]] = []
        self._on_complete: Optional[Callable[[], None]] = None

    @property
    def _use_hint(self) -> bool:
        return self.manager.using_hint()

    # ------------------------------------------------------------------
    def start(self, on_complete: Callable[[], None] | None = None, on_complete_actions: List[Dict[str, Any]] | None = None):
        if self._use_hint:
            self.manager._prepare_hint_mode()
        else:
            self.manager._raise_dialogue_box()
        self._on_complete_actions = on_complete_actions or []
        self._on_complete = on_complete
        Clock.schedule_once(lambda *_: self._advance(), 0)

    def stop(self):
        self._clear_wait()
        if self._use_hint:
            self.manager._hide_hint()
        else:
            try:
                self.game.dialogue_box.hide()
            except Exception:
                pass
            self.manager._restore_dialogue_box()
        self._on_complete_actions = []
        self._on_complete = None

    # ------------------------------------------------------------------
    def _advance(self, *_):
        if self._idx >= len(self.steps):
            self._finish()
            return

        step = self.steps[self._idx]
        self._idx += 1
        typ = step.get("type")

        if typ in ("dialogue", "narration"):
            speaker = (step.get("actor") or "").lower()
            text = step.get("line") or step.get("text") or ""
            if self._use_hint:
                placement = step.get("placement") or step.get("position") or step.get("anchor") or "bottom"
                width_ratio = step.get("width_ratio") or step.get("hint_width_ratio")
                margin = step.get("margin") or step.get("vertical_margin") or step.get("hint_margin")
                self.manager._show_hint(
                    text,
                    dismissible=True,
                    on_dismiss=lambda: self._advance(),
                    placement=placement,
                    width_ratio=width_ratio,
                    vertical_margin=margin,
                )
            else:
                self.game.dialogue_box.show_dialogue(speaker, text, on_dismiss=lambda *_: self._advance())

        elif typ == "wait_for_player_action":
            instruct = step.get("line") or step.get("text") or step.get("hint") or "Follow the instructions."
            speaker = (step.get("actor") or "Jed").lower()
            self._pending_wait = step
            if self._use_hint:
                placement = step.get("placement") or step.get("position") or step.get("anchor") or "bottom"
                width_ratio = step.get("width_ratio") or step.get("hint_width_ratio")
                margin = step.get("margin") or step.get("vertical_margin") or step.get("hint_margin")
                self.manager._show_hint(
                    instruct,
                    dismissible=False,
                    placement=placement,
                    width_ratio=width_ratio,
                    vertical_margin=margin,
                )
                self._begin_wait()
            else:
                self.game.dialogue_box.show_dialogue(speaker, instruct, on_dismiss=lambda *_: self._begin_wait())

        elif typ == "set_milestone":
            milestone = step.get("milestone")
            if milestone:
                self.game.event_manager._action_set_milestone({"milestone": milestone})
            Clock.schedule_once(self._advance, 0)

        else:
            Clock.schedule_once(self._advance, 0)

    def _begin_wait(self):
        step = self._pending_wait or {}
        self._pending_wait = None

        expected_action = (step.get("action") or "").lower()
        expected_item = step.get("item_id")
        if isinstance(expected_item, str):
            expected_item = expected_item.lower()

        event_mgr = getattr(self.game, "event_manager", None)
        if not event_mgr or not hasattr(event_mgr, "subscribe"):
            if self._use_hint:
                self.manager._hide_hint()
            Clock.schedule_once(self._advance, 0)
            return

        def _handler(payload: Dict[str, Any] | None = None):
            data = payload or {}
            action = str(data.get("action", "")).lower()
            if expected_action and action != expected_action:
                return
            item_id = data.get("item_id") or data.get("item")
            if isinstance(item_id, str):
                item_id = item_id.lower()
            if expected_item and item_id != expected_item:
                return
            self._clear_wait()
            if self._use_hint:
                self.manager._hide_hint()
            Clock.schedule_once(self._advance, 0)

        self._wait_cb = _handler
        event_mgr.subscribe("player_action", _handler)

    def _clear_wait(self):
        if not self._wait_cb:
            return
        event_mgr = getattr(self.game, "event_manager", None)
        if event_mgr and hasattr(event_mgr, "unsubscribe"):
            try:
                event_mgr.unsubscribe("player_action", self._wait_cb)
            except Exception:
                pass
        self._wait_cb = None

    def _finish(self):
        self._clear_wait()
        if self._use_hint:
            self.manager._hide_hint()
        else:
            try:
                self.game.dialogue_box.hide()
            except Exception:
                pass
            self.manager._restore_dialogue_box()

        if self._on_complete_actions:
            if hasattr(self.manager.game, "event_manager") and hasattr(self.manager.game.event_manager, "_run_actions"):
                self.manager.game.event_manager._run_actions(self._on_complete_actions)

        if self._on_complete:
            cb, self._on_complete = self._on_complete, None
            cb()


class SystemTutorialManager:
    """Owns the active tutorial runner for system screens."""

    def __init__(self, game):
        self.game = game
        self.active: Optional[SystemTutorialRunner] = None
        self._dlg_state: Dict[str, Any] | None = None
        self._hint: Optional[OverlayTutorialHint] = None
        self.context: str = ""

    # ------------------------------------------------------------------
    def play(self, scene_id: str, *, context: str | None = None, on_complete_actions: List[Dict[str, Any]] | None = None):
        if not scene_id:
            return

        steps = self.game.cinematics.get_scene(scene_id)
        if not steps:
            return
        
        self.stop()
        self.context = (context or "").lower()
        runner = SystemTutorialRunner(self, steps, context=self.context)
        self.active = runner
        runner.start(
            on_complete=self._on_runner_complete,
            on_complete_actions=on_complete_actions
        )

    def stop(self):
        if self.active:
            self.active.stop()
            self.active = None
        self._hide_hint()

    def _on_runner_complete(self):
        self.active = None
        self._hide_hint()

    # ------------------------------------------------------------------ hint helpers
    def using_hint(self) -> bool:
        return self.context in HINT_CONTEXTS

    def _prepare_hint_mode(self):
        self._hide_hint()
        try:
            self.game.dialogue_box.hide()
        except Exception:
            pass

    def _ensure_hint(self) -> OverlayTutorialHint:
        if not self._hint:
            self._hint = OverlayTutorialHint()
        return self._hint

    def _show_hint(self, text: str, *, dismissible: bool,
                   on_dismiss: Optional[Callable[[], None]] = None,
                   placement: Optional[str] = None,
                   width_ratio: Optional[float] = None,
                   vertical_margin: Optional[float] = None):
        hint = self._ensure_hint()
        theme = getattr(self.game, "themes", None)
        if isinstance(theme, ThemeManager):
            hint.apply_theme(theme)
        hint.set_text(text)
        hint.set_dismissible(dismissible)
        hint.set_layout(anchor=placement, width_ratio=width_ratio, vertical_margin=vertical_margin)

        if dismissible and on_dismiss:
            hint.set_on_dismiss(lambda: self._on_hint_dismiss(on_dismiss))
        else:
            hint.set_on_dismiss(None)

        hint.show(Window)

    def _on_hint_dismiss(self, callback: Callable[[], None]):
        self._hide_hint()
        try:
            callback()
        except Exception:
            pass

    def _hide_hint(self):
        if self._hint:
            self._hint.hide()

    # ------------------------------------------------------------------ dialogue box helpers
    def _raise_dialogue_box(self):
        if self.using_hint():
            return
        if self._dlg_state is not None:
            return
        dlg = getattr(self.game, "dialogue_box", None)
        if not dlg:
            return

        parent = dlg.parent
        dlg_state = {
            "parent": parent,
            "index": None,
            "size_hint": dlg.size_hint,
            "pos_hint": getattr(dlg, "pos_hint", {}).copy() if getattr(dlg, "pos_hint", None) else None,
            "pos": dlg.pos,
            "width": dlg.width,
            "height": dlg.height,
        }
        if parent:
            try:
                dlg_state["index"] = parent.children.index(dlg)
            except ValueError:
                dlg_state["index"] = None
            parent.remove_widget(dlg)

        Window.add_widget(dlg)
        dlg.size_hint = (0.8, None)
        dlg.pos_hint = {"center_x": 0.5, "y": 0}
        dlg.width = Window.width * 0.8
        dlg.height = max(dlg_state.get("height") or 0, dlg.height)

        self._dlg_state = dlg_state

    def _restore_dialogue_box(self):
        dlg_state = self._dlg_state
        if not dlg_state:
            return
        dlg = getattr(self.game, "dialogue_box", None)
        if not dlg:
            self._dlg_state = None
            return

        if dlg.parent:
            dlg.parent.remove_widget(dlg)

        if dlg_state.get("size_hint") is not None:
            dlg.size_hint = dlg_state["size_hint"]
        if dlg_state.get("pos_hint") is not None:
            dlg.pos_hint = dlg_state["pos_hint"]
        else:
            dlg.pos_hint = None
        if dlg.pos_hint is None and dlg_state.get("pos") is not None:
            dlg.pos = dlg_state["pos"]
        if dlg_state.get("width") is not None:
            dlg.width = dlg_state["width"]
        if dlg_state.get("height") is not None:
            dlg.height = dlg_state["height"]

        parent = dlg_state.get("parent")
        if parent:
            try:
                index = dlg_state.get("index")
                if index is not None and 0 <= index <= len(parent.children):
                    parent.add_widget(dlg, index=index)
                else:
                    parent.add_widget(dlg)
            except Exception:
                Window.add_widget(dlg)
        else:
            try:
                self.game.add_widget(dlg)
            except Exception:
                Window.add_widget(dlg)

        self._dlg_state = None