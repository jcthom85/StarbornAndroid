from __future__ import annotations

from typing import Optional

from kivy.clock import Clock
from kivy.metrics import dp
from kivy.properties import NumericProperty, StringProperty
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.label import Label
from kivy.uix.screenmanager import Screen

from . import config
from .galaga_game import GalagaGame

class PortraitStage(FloatLayout):
    """Letterboxes its child to maintain the game's 9:21 aspect ratio."""

    def __init__(self, aspect_w: float = config.ASPECT_WIDTH, aspect_h: float = config.ASPECT_HEIGHT, **kwargs):
        super().__init__(**kwargs)
        self.aspect_w = float(aspect_w)
        self.aspect_h = float(aspect_h)
        self.stage = FloatLayout(size_hint=(None, None))
        super().add_widget(self.stage)
        self.bind(size=self._layout_stage, pos=self._layout_stage)

    def add_widget(self, widget, *args, **kwargs):
        if widget is self.stage:
            return super().add_widget(widget, *args, **kwargs)
        self.stage.add_widget(widget, *args, **kwargs)

    def _layout_stage(self, *_args) -> None:
        if not self.width or not self.height:
            return
        target = self.aspect_w / self.aspect_h
        current = self.width / self.height
        if current > target:
            height = self.height
            width = height * target
            x = self.x + (self.width - width) * 0.5
            y = self.y
        else:
            width = self.width
            height = width / target
            x = self.x
            y = self.y + (self.height - height) * 0.5
        self.stage.size = (width, height)
        self.stage.pos = (x, y)

class GalagaHUD(FloatLayout):
    score = NumericProperty(0)
    high_score = NumericProperty(0)
    lives = NumericProperty(3)
    stage = NumericProperty(0)
    banner = StringProperty("")
    sub_banner = StringProperty("")

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.size_hint = (1, None)
        self.height = dp(140)
        self.padding = dp(16)

        font_size = dp(28)
        caption_size = dp(20)

        self._score_label = Label(text="SCORE 000000", halign="left", valign="middle", size_hint=(0.5, 1))
        self._hi_label = Label(text="HI 000000", halign="right", valign="middle", size_hint=(0.5, 1))
        self._lives_label = Label(text="Lives: 3", halign="left", valign="middle", size_hint=(0.5, 1))
        self._stage_label = Label(text="Stage 0", halign="right", valign="middle", size_hint=(0.5, 1))
        self._banner_label = Label(text="", halign="center", valign="middle", size_hint=(1, None))
        self._banner_label.height = dp(36)
        self._banner_label.pos_hint = {"x": 0, "top": 1}
        self._banner_label.font_size = font_size

        # Arrange using FloatLayout coordinates
        self._score_label.font_size = font_size
        self._hi_label.font_size = font_size
        self._lives_label.font_size = caption_size
        self._stage_label.font_size = caption_size

        self.add_widget(self._banner_label)
        self.add_widget(self._score_label)
        self.add_widget(self._hi_label)
        self.add_widget(self._lives_label)
        self.add_widget(self._stage_label)

        self.bind(score=self._refresh, high_score=self._refresh, lives=self._refresh, stage=self._refresh, banner=self._refresh, sub_banner=self._refresh)
        Clock.schedule_once(lambda _dt: self._refresh(), 0)

    def do_layout(self, *largs):  # type: ignore[override]
        super().do_layout(*largs)
        gutter = dp(18)
        mid_y = self.y + self.height * 0.4
        self._score_label.pos = (self.x + gutter, mid_y)
        self._score_label.size = (self.width * 0.5 - gutter, dp(36))
        self._hi_label.pos = (self.center_x, mid_y)
        self._hi_label.size = (self.width * 0.5 - gutter, dp(36))
        self._lives_label.pos = (self.x + gutter, self.y + dp(8))
        self._lives_label.size = (self.width * 0.5 - gutter, dp(30))
        self._stage_label.pos = (self.center_x, self.y + dp(8))
        self._stage_label.size = (self.width * 0.5 - gutter, dp(30))
        self._banner_label.pos = (self.x, self.top - self._banner_label.height)
        self._banner_label.width = self.width

    def _refresh(self, *_args) -> None:
        self._score_label.text = f"SCORE {self.score:06d}"
        self._hi_label.text = f"HI {self.high_score:06d}"
        self._lives_label.text = f"Lives: {self.lives}"
        self._stage_label.text = f"Stage {self.stage}"
        banner_text = self.banner or ""
        if self.sub_banner:
            banner_text = f"{banner_text}\n{self.sub_banner}" if banner_text else self.sub_banner
        self._banner_label.text = banner_text

class GalagaScreen(Screen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        root = FloatLayout()
        self._root = root
        self.stage = PortraitStage(size_hint=(1, 1))
        root.add_widget(self.stage)

        self.game = GalagaGame(size_hint=(1, 1))
        self.stage.add_widget(self.game)

        self.hud = GalagaHUD(pos_hint={"top": 1, "x": 0})
        root.add_widget(self.hud)
        self.add_widget(root)

        for prop in ("score", "high_score", "lives", "stage", "state_text", "sub_state_text", "challenge_hits"):
            self.game.bind(**{prop: self._on_game_update})

    def _on_game_update(self, *_args) -> None:
        self.hud.score = int(self.game.score)
        self.hud.high_score = int(self.game.high_score)
        self.hud.lives = int(self.game.lives)
        self.hud.stage = int(self.game.stage)
        banner = self.game.state_text
        sub_banner = self.game.sub_state_text
        target = getattr(self.game, "_challenge_target", 0)
        if self.game.stage % 4 == 0 and self.game.stage > 0 and target:
            sub_banner = f"{sub_banner}    (Hits {self.game.challenge_hits}/{target})".strip()
        self.hud.banner = banner
        self.hud.sub_banner = sub_banner

    def on_pre_enter(self):  # type: ignore[override]
        Clock.schedule_once(lambda _dt: self.game.start(), 0)

    def on_leave(self, *args):  # type: ignore[override]
        self.game.paused = True
        return super().on_leave(*args)

    def reset_game(self) -> None:
        self.game.reset()
        self._on_game_update()

    def toggle_pause(self) -> None:
        self.game.toggle_pause()
        self._on_game_update()


def build_galaga_screen(name: str = "galaga_arcade") -> GalagaScreen:
    """Convenience helper for ScreenManager integration."""
    return GalagaScreen(name=name)
