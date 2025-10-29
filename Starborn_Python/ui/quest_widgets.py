# ui/quest_widgets.py

from __future__ import annotations

from typing import Optional

from kivy.app import App
from kivy.metrics import dp
from kivy.uix.behaviors import ButtonBehavior
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.anchorlayout import AnchorLayout
from kivy.uix.scrollview import ScrollView
from kivy.uix.widget import Widget

from kivy.graphics import Color, RoundedRectangle, Line

from font_manager import fonts
from ui.menu_overlay import MenuOverlay
from ui.themed_button import ThemedButton
from kivy.core.window import Window
from theme_manager import ThemeManager


_DEFAULT_THEME = ThemeManager()


def _current_theme():
    try:
        app = App.get_running_app()
    except Exception:
        return None
    if not app:
        return None
    game = getattr(app, "current_game", None)
    return getattr(game, "themes", None)


def _palette():
    tm = _current_theme()
    if tm:
        base = tm.col("bg")
        border = tm.col("border")
        fg = tm.col("fg")
        accent = tm.col("accent")
        return tm, base, border, fg, accent

    base = _DEFAULT_THEME.col("bg")
    border = _DEFAULT_THEME.col("border")
    fg = _DEFAULT_THEME.col("fg")
    accent = _DEFAULT_THEME.col("border")
    return _DEFAULT_THEME, base, border, fg, accent



def _tidy_item_id(item_id: str | None) -> str:
    if not item_id:
        return "Unknown item"
    parts = item_id.replace('-', ' ').replace('_', ' ').split()
    if not parts:
        return "Unknown item"
    return ' '.join(p.capitalize() for p in parts)

def _item_display_name(item_id: str | None) -> str:
    if not item_id:
        return _tidy_item_id(item_id)
    try:
        app = App.get_running_app()
    except Exception:
        app = None
    game = getattr(app, 'current_game', None) if app else None
    lookup = None
    if game and hasattr(game, 'all_items'):
        try:
            lookup = game.all_items.find(item_id)
        except Exception:
            lookup = None
    if lookup:
        name = getattr(lookup, 'display_name', None) or getattr(lookup, 'name', None)
        if name:
            return str(name)
    return _tidy_item_id(item_id)

def _mix_rgb(a, b, weight):
    return tuple(a[i] * (1 - weight) + b[i] * weight for i in range(3))


def _make_wrapped_label(text, font_key, color, *, markup=False, halign="left", valign="top", min_height=dp(22)):
    lbl = Label(
        text=text,
        markup=markup,
        font_name=fonts[font_key]["name"],
        font_size=fonts[font_key]["size"],
        color=color,
        halign=halign,
        valign=valign,
        size_hint_y=None,
    )
    lbl.bind(width=lambda inst, width: setattr(inst, "text_size", (width, None)))
    lbl.bind(texture_size=lambda inst, size: setattr(inst, "height", max(size[1], min_height)))
    return lbl


def _style_panel(widget: Widget, bg_rgba, border_rgba, radius):
    with widget.canvas.before:
        widget._bg_color = Color(*bg_rgba)
        widget._bg_rect = RoundedRectangle(pos=widget.pos, size=widget.size,
                                           radius=[(radius, radius)] * 4)
        widget._border_color = Color(*border_rgba)
        widget._border_line = Line(width=dp(1.6),
                                   rounded_rectangle=(widget.x, widget.y, widget.width, widget.height, radius))

    def _sync(*_):
        widget._bg_rect.pos = widget.pos
        widget._bg_rect.size = widget.size
        widget._border_line.rounded_rectangle = (widget.x, widget.y, widget.width, widget.height, radius)

    widget.bind(pos=_sync, size=_sync)


class _TaskIndicator(Widget):
    def __init__(self, done, border_rgba, accent_rgba, **kwargs):
        super().__init__(**kwargs)
        self.size_hint = (None, 1) # Take full vertical space of its parent
        self.width = dp(24)
        self._radius = dp(6)
        self._done = bool(done)
        self._accent_rgba = accent_rgba
        border = (border_rgba[0], border_rgba[1], border_rgba[2], 0.9)

        with self.canvas.before:
            self._fill_color = Color(*self._fill_rgba())
            self._fill_rect = RoundedRectangle(
                                               radius=[(self._radius, self._radius)] * 4)
            self._border_color = Color(*border)
            self._border_line = Line(width=dp(1.2),
                                     rounded_rectangle=(0,0,0,0, self._radius))
            if self._done:
                self._check_color = Color(accent_rgba[0], accent_rgba[1], accent_rgba[2], 1.0)
                self._check_line = Line(width=dp(2))
            else:
                self._check_color = None
                self._check_line = None

        self.bind(pos=self._update_canvas, size=self._update_canvas)
        self._update_canvas()

    def _fill_rgba(self):
        return (
            self._accent_rgba[0],
            self._accent_rgba[1],
            self._accent_rgba[2],
            0.55 if self._done else 0.1,
        )

    def _update_canvas(self, *_):
        # Center the drawn elements within the widget's area
        box_size = dp(22)
        cx, cy = self.center_x, self.center_y
        bx, by = cx - box_size / 2, cy - box_size / 2

        self._fill_rect.pos = (bx, by)
        self._fill_rect.size = (box_size, box_size)
        self._border_line.rounded_rectangle = (bx, by, box_size, box_size, self._radius)

        if self._done and self._check_line:
            x0 = bx + box_size * 0.22
            y0 = by + box_size * 0.45
            x1 = bx + box_size * 0.45
            y1 = by + box_size * 0.2
            x2 = bx + box_size * 0.78
            y2 = by + box_size * 0.75
            self._check_line.points = [x0, y0, x1, y1, x2, y2]


class BorderedCard(BoxLayout):
    def __init__(self, *, bg_rgba, border_rgba, radius=dp(18), padding=None, spacing=None, **kw):
        if padding is None:
            padding = [dp(12), dp(12), dp(12), dp(12)]
        if spacing is None:
            spacing = dp(8)
        super().__init__(orientation="vertical", padding=padding, spacing=spacing, **kw)
        self.size_hint_y = None
        self.height = dp(84)
        self.bind(minimum_height=self._sync_height)
        self._radius = radius

        with self.canvas.before:
            self._bg_color = Color(*bg_rgba)
            self._bg_rect = RoundedRectangle(pos=self.pos, size=self.size,
                                             radius=[(radius, radius)] * 4)
            self._border_color = Color(*border_rgba)
            self._border_line = Line(width=dp(1.6),
                                     rounded_rectangle=(self.x, self.y, self.width, self.height, radius))

        self.bind(pos=self._update_canvas, size=self._update_canvas)

    def _sync_height(self, _inst, value):
        self.height = max(value, dp(84))

    def _update_canvas(self, *_):
        self._bg_rect.pos = self.pos
        self._bg_rect.size = self.size
        self._border_line.rounded_rectangle = (
            self.x, self.y, self.width, self.height, self._radius)


class QuestCard(ButtonBehavior, BorderedCard):
    """Compact quest listing card styled to match the core menu look."""

    def __init__(self, quest, is_tracked=False, **kw):
        tm, base, border, fg, accent = _palette()
        bg_mix = _mix_rgb(base, fg, 0.4)
        bg_rgba = (bg_mix[0], bg_mix[1], bg_mix[2], 0.88)
        border_mix = _mix_rgb(border, accent, 0.35)
        border_rgba = (border_mix[0], border_mix[1], border_mix[2], 0.88)
        super().__init__(bg_rgba=bg_rgba, border_rgba=border_rgba, **kw)
        self.padding = [dp(14), dp(14), dp(14), dp(14)]
        self.spacing = dp(10)

        self.quest = quest
        self._fg = fg
        self._accent = (accent[0], accent[1], accent[2], 1.0)
        self._border_default = border_rgba
        self._set_tracked(is_tracked)

        title = quest.title if hasattr(quest, "title") else quest.get("title", "Quest")
        progress = quest.progress_text() if hasattr(quest, "progress_text") else quest.get("progress", "")

        top_row = BoxLayout(orientation="horizontal", spacing=dp(8), size_hint_y=None)

        title_lbl = _make_wrapped_label(f"[b]{title}[/b]", "medium_text",
                                        (fg[0], fg[1], fg[2], 0.98), markup=True,
                                        valign="middle", min_height=dp(28))
        title_lbl.size_hint_x = 0.7

        progress_lbl = _make_wrapped_label(progress, "small_text",
                                           (accent[0], accent[1], accent[2], 0.9),
                                           halign="right", valign="middle", min_height=dp(22))
        progress_lbl.size_hint_x = 0.3

        top_row.add_widget(title_lbl)
        top_row.add_widget(progress_lbl)

        def _sync_top_height(*_):
            top_row.height = max(title_lbl.height, progress_lbl.height)

        title_lbl.bind(height=_sync_top_height)
        progress_lbl.bind(height=_sync_top_height)
        _sync_top_height()

        self.add_widget(top_row)

        stage = quest.current_stage() if hasattr(quest, "current_stage") else None
        if stage and getattr(stage, "title", None):
            stage_lbl = _make_wrapped_label(f"[b]{stage.title}[/b]", "small_text",
                                            (accent[0], accent[1], accent[2], 0.92),
                                            markup=True, valign="middle", min_height=dp(20))
            self.add_widget(stage_lbl)

        blurb = getattr(quest, "summary", "") or getattr(quest, "description", "")
        if blurb:
            summary_lbl = _make_wrapped_label(blurb, "small_text",
                                              (fg[0], fg[1], fg[2], 0.78))
            self.add_widget(summary_lbl)

        if is_tracked:
            tracked_lbl = Label(text="[b]TRACKED[/b]", markup=True,
                                font_name=fonts["small_text"]["name"],
                                font_size=fonts["small_text"]["size"],
                                color=self._accent,
                                size_hint_y=None, height=dp(20),
                                halign="left", valign="middle")
            tracked_lbl.bind(width=lambda inst, width: setattr(inst, "text_size", (width, None)))
            self.add_widget(tracked_lbl)

    def _set_tracked(self, tracked: bool):
        self._border_color.rgba = self._accent if tracked else self._border_default



class QuestDetailOverlay(MenuOverlay):
    '''Sliding overlay variant of the quest detail view.'''

    def __init__(self, quest_state, **kw):
        self.quest = quest_state
        tm, base, border, fg, accent = _palette()
        default_kwargs = dict(default_tab=None, title=quest_state.title, show_background=True, dim_alpha=0.55)
        default_kwargs.update(kw)
        super().__init__(**default_kwargs)

        body = self._build_body(base, border, fg, accent)
        scroll = ScrollView(do_scroll_x=False, bar_width=dp(4), size_hint=(1, 1))
        scroll.add_widget(body)

        fg_col = self.tm.col('fg')
        bg_col = self.tm.col('bg')
        accent_col = self.tm.col('accent')

        self._track_button = ThemedButton(
            text=self._track_label(),
            font_name=fonts['popup_button']['name'],
            font_size=fonts['popup_button']['size'],
            bg_color=(accent_col[0], accent_col[1], accent_col[2], 0.92),
            color=(bg_col[0], bg_col[1], bg_col[2], 0.95),
            size_hint=(1, None),
            height=dp(48)
        )
        self._track_button.bind(on_release=lambda *_: self._toggle_track())

        btn_box = BoxLayout(orientation='vertical', spacing=dp(8), size_hint_y=None, padding=[0, dp(4), 0, 0])
        btn_box.add_widget(self._track_button)
        btn_box.height = self._track_button.height + btn_box.padding[1] + btn_box.padding[3]

        wrapper = BoxLayout(orientation='vertical', spacing=dp(12), padding=[0, 0, 0, 0])
        wrapper.add_widget(scroll)
        wrapper.add_widget(btn_box)

        self.content_area.clear_widgets()
        if hasattr(self.content_area, 'padding'):
            self.content_area.padding = [0, 0, 0, dp(12)]
        if hasattr(self.content_area, 'spacing'):
            self.content_area.spacing = dp(12)
        self.content_area.add_widget(wrapper)

    def _track_label(self) -> str:
        return 'Untrack' if self._is_tracked() else 'Track'

    def _quest_manager(self):
        try:
            app = App.get_running_app()
        except Exception:
            return None
        game = getattr(app, 'current_game', None)
        return getattr(game, 'quest_manager', None)

    def _is_tracked(self) -> bool:
        qm = self._quest_manager()
        return bool(qm and qm.tracked_quest_id == self.quest.id)

    def _toggle_track(self):
        qm = self._quest_manager()
        if not qm:
            return
        new = None if qm.tracked_quest_id == self.quest.id else self.quest.id
        qm.set_tracked(new)
        self._track_button.text = self._track_label()

    def present(self):
        self._track_button.text = self._track_label()
        Window.add_widget(self)

    def _build_body(self, base, border, fg, accent):
        body = BoxLayout(orientation='vertical', spacing=dp(14),
                         padding=[dp(6), dp(4), dp(6), dp(12)],
                         size_hint_y=None)

        def _update_height(*_):
            children = list(body.children)
            spacing_total = body.spacing * (len(children) - 1) if len(children) > 1 else 0
            total = body.padding[1] + body.padding[3] + spacing_total
            total += sum(getattr(child, 'height', 0) for child in children)
            if not children:
                total += dp(40)
            body.height = total

        def _watch(widget):
            if hasattr(widget, 'bind'):
                widget.bind(height=_update_height)
                if hasattr(widget, 'minimum_height'):
                    widget.bind(minimum_height=_update_height)

        sections_added = 0

        if getattr(self.quest, 'summary', None) or getattr(self.quest, 'flavor', None):
            summary_panel, summary_box = self._make_scrolling_section(
                border, base, fg, accent, heading=None, height=dp(170))
            if self.quest.summary:
                summary_box.add_widget(_make_wrapped_label(
                    self.quest.summary, 'medium_text',
                    (fg[0], fg[1], fg[2], 0.98)))
            if self.quest.flavor:
                summary_box.add_widget(_make_wrapped_label(
                    f"[i]{self.quest.flavor}[/i]", 'small_text',
                    (fg[0], fg[1], fg[2], 0.8), markup=True))
            body.add_widget(summary_panel)
            _watch(summary_box)
            sections_added += 1

        stage = self.quest.current_stage() if hasattr(self.quest, 'current_stage') else None
        if stage:
            stage_title = getattr(stage, 'title', '') or getattr(stage, 'name', '')
            stage_panel, stage_box = self._make_scrolling_section(
                border, base, fg, accent, heading=stage_title, height=dp(300))
            description = getattr(stage, 'description', '')
            if description:
                stage_box.add_widget(_make_wrapped_label(
                    description, 'medium_text',
                    (fg[0], fg[1], fg[2], 0.92)))

            tasks = getattr(stage, 'tasks', None)
            if tasks:
                tasks_box = BoxLayout(orientation='vertical', spacing=dp(-16), padding=[0, dp(4), 0, dp(4)],
                                      size_hint_y=None)
                tasks_box.bind(minimum_height=tasks_box.setter('height'))
                for task in tasks:
                    row = BoxLayout(orientation='horizontal', spacing=dp(0),
                                    padding=[0, dp(2), 0, dp(2)], size_hint_y=None)

                    indicator = _TaskIndicator(getattr(task, 'done', False), border, accent)
                    indicator.size_hint = (None, None)
                    indicator_anchor = AnchorLayout(anchor_x='left', anchor_y='top', size_hint=(None, None))
                    indicator_anchor.width = indicator.width + dp(6)
                    indicator_anchor.bind(height=row.setter('height'))
                    indicator_anchor.add_widget(indicator)
                    row.add_widget(indicator_anchor)

                    task_lbl = _make_wrapped_label(task_text(task), 'medium_text',
                                                   (fg[0], fg[1], fg[2], 0.95),
                                                   valign='top', min_height=indicator.height)
                    task_lbl.size_hint_x = 1
                    row.add_widget(task_lbl)

                    def _sync_row_height(*_):
                        # The label's texture_size[1] is the ground truth for wrapped height.
                        content_height = max(task_lbl.texture_size[1], indicator.height)
                        # Add some padding to prevent text collision
                        row.height = content_height + dp(8)

                    task_lbl.bind(texture_size=lambda *_: _sync_row_height())
                    _sync_row_height()
                    tasks_box.add_widget(row)
                stage_box.add_widget(tasks_box)
                _watch(tasks_box)
            else:
                stage_box.add_widget(_make_wrapped_label(
                    'No tasks for this stage.', 'small_text',
                    (fg[0], fg[1], fg[2], 0.75)))

            body.add_widget(stage_panel)
            _watch(stage_box)
            sections_added += 1

        rewards = getattr(self.quest, 'rewards', None)
        if rewards:
            rewards_panel, rewards_box = self._make_scrolling_section(
                border, base, fg, accent, heading='Rewards', height=dp(150))
            for reward in rewards:
                rewards_box.add_widget(_make_wrapped_label(
                    f'- {pretty_reward(reward)}', 'small_text',
                    (fg[0], fg[1], fg[2], 0.9), valign='middle'))
            body.add_widget(rewards_panel)
            _watch(rewards_box)
            sections_added += 1

        if not sections_added:
            fallback = Label(
                text='[i]No quest details available.[/i]',
                markup=True,
                font_name=fonts['medium_text']['name'],
                font_size=fonts['medium_text']['size'],
                color=(fg[0], fg[1], fg[2], 0.8),
                halign='center',
                valign='middle',
                size_hint_y=None,
                height=dp(150),
            )
            fallback.bind(size=lambda inst, val: setattr(inst, 'text_size', (inst.width, None)))
            body.add_widget(fallback)
            _watch(fallback)

        _update_height()
        body.bind(children=lambda *_: _update_height())
        return body

    def _make_scrolling_section(self, border, base, fg, accent, heading: Optional[str], height: float):
        panel = self._make_panel(border, base, fg, accent, heading=heading, fixed_height=height)
        panel.size_hint_y = None
        panel.height = height

        scroll = ScrollView(do_scroll_x=False, bar_width=dp(4), size_hint=(1, 1))
        content = BoxLayout(orientation='vertical', spacing=dp(8),
                             padding=[0, 0, 0, dp(6)], size_hint_y=None)
        content.bind(minimum_height=content.setter('height'))
        scroll.add_widget(content)
        panel.add_widget(scroll)
        return panel, content

    def _make_panel(self, border, base, fg, accent, heading: Optional[str] = None, fixed_height: float | None = None):
        panel = BoxLayout(orientation='vertical', spacing=dp(10),
                           padding=[dp(14), dp(14), dp(14), dp(14)],
                           size_hint_y=None)
        if fixed_height is None:
            panel.bind(minimum_height=lambda inst, h: setattr(inst, 'height', h))
        else:
            panel.height = fixed_height
        bg_mix = _mix_rgb(base, fg, 0.45)
        bg_rgba = (bg_mix[0], bg_mix[1], bg_mix[2], 0.88)
        border_mix = _mix_rgb(border, accent, 0.6)
        border_rgba = (border_mix[0], border_mix[1], border_mix[2], 0.92)
        _style_panel(panel, bg_rgba, border_rgba, dp(18))

        if heading:
            panel.add_widget(_make_wrapped_label(
                f"[b]{heading}[/b]", 'section_title',
                (accent[0], accent[1], accent[2], 0.96),
                markup=True, valign='middle', min_height=dp(26)))

        return panel


def task_text(t) -> str:
    return getattr(t, "text", "") if t else ""


def pretty_reward(r: dict) -> str:
    typ = r.get("type", "item")
    if typ == "xp":
        return f"{r.get('amount', 0)} XP"
    if typ == "item":
        item_id = r.get("id")
        qty = r.get("qty", 1)
        name = _item_display_name(item_id)
        return f"{name} x{qty}" if qty and qty != 1 else name
    return f"{typ}: {r}"
