# ui/quest_popup.py
from __future__ import annotations

from typing import Any, Callable, Iterable, Sequence

from kivy.animation import Animation
from kivy.app import App
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.graphics import Color, Line, RoundedRectangle
from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.label import Label
from kivy.uix.popup import Popup

from font_manager import fonts
from theme_manager import ThemeManager as DefaultThemeManager
from ui.themed_button import ThemedButton


def _mix_rgb(a: Sequence[float], b: Sequence[float], weight: float) -> tuple[float, float, float]:
    return tuple(a[i] * (1.0 - weight) + b[i] * weight for i in range(3))


class QuestPopup(Popup):
    """
    Rounded quest notification card that mirrors the journal styling.
    Displays a headline (“New Quest”/“Quest Complete”), quest title, flavor text,
    and optional bullet lines. Provides quick links to the journal.
    """

    def __init__(
        self,
        *,
        headline: str,
        quest_title: str,
        summary: str = "",
        lines: Iterable[str] | None = None,
        theme_mgr=None,
        auto_dismiss: float = 0.0,
        on_open_journal: Callable[[], None] | None = None,
        journal_text: str = "Journal",
    ):
        super().__init__(title="", separator_height=0, background="", auto_dismiss=False)

        self.background_color = (0, 0, 0, 0)
        self._tm = theme_mgr if theme_mgr else DefaultThemeManager()
        self._headline = headline
        self._quest_title = quest_title
        self._summary = summary
        self._lines = list(lines or [])
        self._auto_delay = max(0.0, float(auto_dismiss))
        self._auto_evt = None
        self._closing = False
        self._on_open_journal = on_open_journal
        self._journal_btn = None
        self._journal_btn_height = dp(46)
        self._journal_btn_margin = dp(52)

        self._build_ui(journal_text)
        self._bind_layout()

    # ------------------------------------------------------------------ build
    def _build_ui(self, journal_text: str):
        tm = self._tm
        bg = tm.col("bg")
        fg = tm.col("fg")
        accent = tm.col("accent")
        border = tm.col("border")

        radius = dp(22)
        base_rgb = _mix_rgb(bg, fg, 0.38)
        border_rgb = _mix_rgb(border, accent, 0.4)

        root = FloatLayout(size_hint=(1, 1))
        self.add_widget(root)

        with root.canvas.before:
            self._bg_color = Color(base_rgb[0], base_rgb[1], base_rgb[2], 0.95)
            self._bg_rect = RoundedRectangle(radius=[(radius, radius)] * 4)
            self._accent_color = Color(accent[0], accent[1], accent[2], 0.55)
            self._accent_rect = RoundedRectangle(radius=[(radius, radius), (radius, radius), (0, 0), (0, 0)])
            self._border_color = Color(border_rgb[0], border_rgb[1], border_rgb[2], 0.9)
            self._border = Line(rounded_rectangle=(0, 0, 0, 0, radius), width=dp(1.6))

        btn_overlap = self._journal_btn_height / 2 if self._on_open_journal else 0
        pad = [dp(22), dp(0), dp(22), dp(22) + btn_overlap]
        layout = BoxLayout(
            orientation="vertical",
            spacing=dp(16),
            padding=pad,
            size_hint=(1, None),
            pos_hint={"center_x": 0.5, "top": 1},
        )
        layout.bind(minimum_height=lambda inst, value: setattr(inst, "height", value))
        root.add_widget(layout)
        self._layout = layout

        headline_lbl = Label(
            text=f"[b]{self._headline}[/b]",
            markup=True,
            font_name=fonts["popup_button"]["name"],
            font_size=fonts["popup_button"]["size"] * 1.05,
            color=(accent[0], accent[1], accent[2], 1.0),
            size_hint_y=None,
            halign="left",
            valign="middle",
        )
        headline_lbl.bind(
            width=lambda inst, width: setattr(inst, "text_size", (width, None)),
            texture_size=lambda inst, size: setattr(inst, "height", max(size[1], dp(28))),
        )
        layout.add_widget(headline_lbl)

        title_lbl = Label(
            text=self._quest_title,
            font_name=fonts["popup_title"]["name"],
            font_size=fonts["popup_title"]["size"] * 1.05,
            color=(fg[0], fg[1], fg[2], 0.98),
            size_hint_y=None,
            halign="left",
            valign="middle",
        )
        title_lbl.bind(
            width=lambda inst, width: setattr(inst, "text_size", (width, None)),
            texture_size=lambda inst, size: setattr(inst, "height", max(size[1], dp(32))),
        )
        layout.add_widget(title_lbl)

        if self._summary:
            summary_lbl = Label(
                text=self._summary,
                font_name=fonts["medium_text"]["name"],
                font_size=fonts["medium_text"]["size"],
                color=(fg[0], fg[1], fg[2], 0.84),
                size_hint_y=None,
                halign="left",
                valign="top",
            )
            summary_lbl.bind(
                width=lambda inst, width: setattr(inst, "text_size", (width, None)),
                texture_size=lambda inst, size: setattr(inst, "height", max(size[1], dp(48))),
            )
            layout.add_widget(summary_lbl)

        for line in self._lines:
            bullet_lbl = Label(
                text=f"- {line}",
                font_name=fonts["small_text"]["name"],
                font_size=fonts["small_text"]["size"] * 1.02,
                color=(fg[0], fg[1], fg[2], 0.78),
                size_hint_y=None,
                halign="left",
                valign="middle",
            )
            bullet_lbl.bind(
                width=lambda inst, width: setattr(inst, "text_size", (width, None)),
                texture_size=lambda inst, size: setattr(inst, "height", max(size[1], dp(24))),
            )
            layout.add_widget(bullet_lbl)

        layout.add_widget(BoxLayout(size_hint_y=None, height=dp(6)))  # spacer

        if self._on_open_journal:
            journal_btn = ThemedButton(
                text=journal_text,
                font_name=fonts["popup_button"]["name"],
                font_size=fonts["popup_button"]["size"],
                bg_color=(accent[0], accent[1], accent[2], 1.0),
                color=(bg[0], bg[1], bg[2], 0.96),
                size_hint=(None, None),
                height=self._journal_btn_height,
            )
            journal_btn.corner_radius = self._journal_btn_height / 2.0
            journal_btn.bind(height=lambda inst, h: setattr(inst, "corner_radius", max(1.0, h / 2.0)))
            journal_btn.bind(on_release=lambda *_: self._handle_open_journal())
            root.add_widget(journal_btn)
            self._journal_btn = journal_btn

    def _bind_layout(self):
        self.size_hint = (None, None)
        self.opacity = 0.0
        self.bind(pos=self._sync_canvas, size=self._sync_canvas)
        self._layout.bind(minimum_height=lambda *_: Clock.schedule_once(lambda *_: self._reflow(), 0))
        Window.bind(on_resize=lambda *_: Clock.schedule_once(lambda *_: self._reflow(), 0))
        Clock.schedule_once(lambda *_: self._reflow(), 0)

    # ------------------------------------------------------------------ layout helpers
    def _reflow(self):
        width = min(Window.width * 0.84, dp(640))
        content_h = getattr(self._layout, "minimum_height", self._layout.height)
        height = min(max(content_h, dp(200)), Window.height * 0.72)
        self.width = width
        self.height = height
        self.x = (Window.width - self.width) / 2.0
        self.y = (Window.height - self.height) * 0.55
        self._sync_canvas()

    def _sync_canvas(self, *_) -> None:
        radius = dp(22)
        self._bg_rect.pos = self.pos
        self._bg_rect.size = self.size
        accent_h = dp(10)
        inset = dp(6)
        accent_w = max(dp(40), self.width - inset * 2)
        self._accent_rect.pos = (self.x + inset, self.top - accent_h)
        self._accent_rect.size = (accent_w, accent_h)
        self._accent_rect.radius = [
            (radius, radius),
            (radius, radius),
            (0, 0),
            (0, 0),
        ]
        self._border.rounded_rectangle = (self.x, self.y, self.width, self.height, radius)

        if self._journal_btn:
            min_width = dp(160)
            available = max(self.width - dp(20), min_width)
            width = self.width - self._journal_btn_margin * 2
            width = min(max(width, min_width), available)
            self._journal_btn.size = (width, self._journal_btn_height)
            self._journal_btn.center_x = self.x + (self.width / 2)
            self._journal_btn.center_y = self.y

    # ------------------------------------------------------------------ actions
    def _handle_open_journal(self):
        cb = self._on_open_journal
        if cb:
            Clock.schedule_once(lambda *_: cb(), 0)
        self.dismiss()

    def open(self, *largs, **kwargs):
        super().open(*largs, **kwargs)
        Animation.cancel_all(self, "opacity")
        Animation(opacity=1.0, duration=0.18, t="out_quad").start(self)
        if self._auto_delay > 0:
            self._auto_evt = Clock.schedule_once(lambda *_: self.dismiss(), self._auto_delay)

    def dismiss(self, *largs, **kwargs):
        if self._closing:
            return
        self._closing = True
        if self._auto_evt is not None:
            self._auto_evt.cancel()
            self._auto_evt = None

        def _finish(*_):
            self._closing = False
            super(QuestPopup, self).dismiss(*largs, **kwargs)

        Animation.cancel_all(self, "opacity")
        anim = Animation(opacity=0.0, duration=0.15, t="in_quad")
        anim.bind(on_complete=_finish)
        anim.start(self)

    def on_touch_down(self, touch):
        if not self.collide_point(*touch.pos):
            self.dismiss()
            return True
        return super().on_touch_down(touch)


class QuestPopupManager:
    """
    Centralised controller that subscribes to quest start/complete events,
    queues popups so they never overlap, and forwards journal navigation.
    """

    def __init__(self, game, event_manager):
        self.game = game
        self.event_manager = event_manager
        self._queue: list[tuple[str, str, dict[str, Any] | None]] = []
        self._current: QuestPopup | None = None
        self._last_stage_index: dict[str, int] = {}

        if event_manager:
            event_manager.subscribe("quest_started", self._on_quest_started)
            event_manager.subscribe("quest_completed", self._on_quest_completed)
            event_manager.subscribe("quest_stage_changed", self._on_quest_stage_changed)

    # ------------------------------------------------------------------ event handlers
    def _get_live_quest(self, quest_id: str):
        qm = getattr(self.game, "quest_manager", None)
        if qm and callable(getattr(qm, "get", None)):
            return qm.get(quest_id)
        return None

    def _on_quest_started(self, payload: dict):
        quest_id = payload.get("quest_id") if isinstance(payload, dict) else None
        if not quest_id:
            return
        quest = self._get_live_quest(quest_id)
        if quest is not None:
            try:
                stage_index = int(getattr(quest, "stage_index", 0) or 0)
            except (TypeError, ValueError):
                stage_index = 0
            self._last_stage_index[quest_id] = stage_index
        else:
            self._last_stage_index.setdefault(quest_id, 0)
        self._queue_popup(("new", quest_id, None))

    def _on_quest_completed(self, payload: dict):
        quest_id = payload.get("quest_id") if isinstance(payload, dict) else None
        if not quest_id:
            return
        self._last_stage_index.pop(quest_id, None)
        self._queue_popup(("complete", quest_id, None))

    def _on_quest_stage_changed(self, payload: dict | None):
        quest_id = payload.get("quest_id") if isinstance(payload, dict) else None
        if not quest_id:
            return
        quest = self._get_live_quest(quest_id)
        if quest is None:
            return
        try:
            new_index = int(getattr(quest, "stage_index", None))
        except (TypeError, ValueError):
            new_index = None
        prev_seen = self._last_stage_index.get(quest_id)
        if new_index is None:
            return
        self._last_stage_index[quest_id] = new_index
        if new_index <= 0:
            return
        if prev_seen is not None and new_index <= prev_seen:
            return
        extra: dict[str, Any] = {
            "new_index": new_index,
            "previous_index": prev_seen,
        }
        if isinstance(payload, dict):
            extra["stage_id"] = payload.get("stage_id")
        self._queue_popup(("stage", quest_id, extra))

    # ------------------------------------------------------------------ queue logic
    def _queue_popup(self, item: tuple[str, str, dict[str, Any] | None]):
        self._queue.append(item)
        self._maybe_show_next()

    def _maybe_show_next(self):
        if self._current is not None:
            return
        while self._queue:
            kind, quest_id, extra = self._queue.pop(0)
            quest = self._get_live_quest(quest_id)
            if not quest:
                continue
            popup = self._build_popup(kind, quest, extra)
            if popup:
                popup.bind(on_dismiss=lambda *_: self._on_popup_dismissed())
                self._current = popup
                popup.open()
                break

    def _on_popup_dismissed(self):
        self._current = None
        Clock.schedule_once(lambda *_: self._maybe_show_next(), 0.1)

    # ------------------------------------------------------------------ popup content helpers
    def _build_popup(self, kind: str, quest, extra: dict[str, Any] | None = None) -> QuestPopup | None:
        tm = getattr(self.game, "themes", None)
        summary = getattr(quest, "summary", "") or getattr(quest, "description", "")
        lines: list[str] = []
        quest_id = getattr(quest, "id", "quest")
        auto = 0.0

        stage = quest.current_stage() if hasattr(quest, "current_stage") else None
        if kind == "new":
            headline = "New Quest"
            if not summary and stage and getattr(stage, "description", ""):
                summary = stage.description
            if stage and getattr(stage, "title", ""):
                lines.append(stage.title)
            if stage and getattr(stage, "tasks", None):
                for task in stage.tasks[:2]:
                    text = getattr(task, "text", "") or getattr(task, "id", "")
                    if text:
                        lines.append(text)
        elif kind == "stage":
            headline = "Quest Updated"
            if stage and getattr(stage, "description", ""):
                summary = stage.description
            new_index = None
            if extra:
                new_index = extra.get("new_index", getattr(quest, "stage_index", None))
            if new_index is None:
                new_index = getattr(quest, "stage_index", None)
            try:
                new_index_int = int(new_index)
            except (TypeError, ValueError):
                new_index_int = None
            prev_index = extra.get("previous_index") if extra else None
            if prev_index is None and isinstance(new_index_int, int):
                prev_index = new_index_int - 1
            stages = getattr(quest, "stages", []) or []
            if (
                isinstance(prev_index, int)
                and prev_index >= 0
                and prev_index < len(stages)
            ):
                prev_stage = stages[prev_index]
                prev_title = getattr(prev_stage, "title", "") or getattr(prev_stage, "id", "")
                lines.append(f"Finished: Stage {prev_index + 1} - {prev_title}")
            if stage and getattr(stage, "title", ""):
                if isinstance(new_index_int, int):
                    stage_label = f"Stage {new_index_int + 1} - {stage.title}"
                else:
                    stage_label = stage.title
                lines.append(f"Now: {stage_label}")
            if stage and getattr(stage, "tasks", None):
                for task in stage.tasks[:3]:
                    text = getattr(task, "text", "") or getattr(task, "id", "")
                    if text:
                        lines.append(text)
        else:
            headline = "Quest Complete"
            rewards = getattr(quest, "rewards", []) or []
            for reward in rewards[:3]:
                name = reward.get("name") or reward.get("item") or reward.get("type")
                qty = reward.get("quantity") or reward.get("amount")
                if name and qty:
                    lines.append(f"{qty} A- {name}")
                elif name:
                    lines.append(name)
            auto = 0.0

        if not summary and hasattr(quest, "flavor") and quest.flavor:
            summary = quest.flavor

        return QuestPopup(
            headline=headline,
            quest_title=getattr(quest, "title", quest_id),
            summary=summary,
            lines=lines,
            theme_mgr=tm,
            auto_dismiss=auto,
            on_open_journal=self._goto_journal,
        )

    def _goto_journal(self):
        app = App.get_running_app()
        if not app or not hasattr(app, "open_menu"):
            return

        def _open_journal(*_):
            try:
                app.open_menu("journal")
                refresh = getattr(app, "refresh_ui_screens", None)
                if callable(refresh):
                    refresh()
            except Exception as exc:  # pragma: no cover - defensive logging
                print(f"[QuestPopupManager] Failed opening journal: {exc}")

        # Delay slightly so the popup can finish its close animation first.
        Clock.schedule_once(_open_journal, 0.2)
