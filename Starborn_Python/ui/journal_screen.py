# journal_screen.py
from __future__ import annotations
from kivy.app import App
from kivy.uix.screenmanager import Screen
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.carousel import Carousel
from kivy.uix.scrollview import ScrollView
from kivy.uix.label import Label
from kivy.metrics import dp
from kivy.graphics import Color, RoundedRectangle, Line
from font_manager import fonts
from ui.themed_button import ThemedToggleButton as TabButton
from ui.quest_widgets import QuestCard, QuestDetailOverlay

# Optional: same 9:21 stage used by Hub, kept light here.
from kivy.uix.floatlayout import FloatLayout
class AspectStage(FloatLayout):
    def __init__(self, aspect_w=9.0, aspect_h=21.0, **kw):
        super().__init__(**kw); self.aspect_w=aspect_w; self.aspect_h=aspect_h
        self.stage = BoxLayout(orientation='vertical', size_hint=(None, None))
        self.add_widget(self.stage); self.bind(size=self._relayout, pos=self._relayout)
    def _relayout(self, *_):
        W,H = self.size; target = self.aspect_w/self.aspect_h
        if W/H > target: h=H; w=h*target; x=self.x+(W-w)*0.5; y=self.y
        else: w=W; h=w/target; x=self.x; y=self.y+(H-h)*0.5
        self.stage.size=(w,h); self.stage.pos=(x,y)

class JournalScreen(Screen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self._built = False

    def _build_ui(self):
        if self._built: return
        app = App.get_running_app()
        game = app.current_game
        tm = game.themes
        fg, bg = tm.col('fg'), tm.col('bg')
        accent = tm.col('accent')

        # Define colors for the new TabButton
        tab_bg_color = tuple(accent[i] * 0.8 for i in range(3)) + (0.92,)
        tab_text_color = tuple(bg)

        # Tabs
        self.btn_active = TabButton(text="[b]Active[/b]", markup=True, group="journal_tabs", state="down",
                                    font_name=fonts["popup_button"]["name"], font_size=fonts["popup_button"]["size"],
                                    bg_color=tab_bg_color, color=tab_text_color)
        self.btn_done = TabButton(text="[b]Completed[/b]", markup=True, group="journal_tabs",
                                  font_name=fonts["popup_button"]["name"], font_size=fonts["popup_button"]["size"],
                                  bg_color=tab_bg_color, color=tab_text_color)

        tab_radius = dp(26)
        self.btn_active.set_corner_radii([(tab_radius, tab_radius), (0, 0), (0, 0), (0, 0)])
        self.btn_done.set_corner_radii([(0, 0), (tab_radius, tab_radius), (0, 0), (0, 0)])
        
        self.carousel = Carousel(direction="right", loop=False, scroll_timeout=220)
        self.carousel.bind(index=self._sync_tabs)
        self.btn_active.bind(on_release=lambda *_: setattr(self.carousel, "index", 0))
        self.btn_done.bind(on_release=lambda *_: setattr(self.carousel, "index", 1))
        
        # Simplified tab layout
        inner_tabs = BoxLayout(spacing=dp(2), size_hint_y=None, height=dp(44))
        inner_tabs.add_widget(self.btn_active)
        inner_tabs.add_widget(self.btn_done)

        # 9:21 stage
        stage = AspectStage(9,21)
        stage.stage.padding = [dp(-24), dp(18), dp(-24), dp(22)]
        stage.stage.spacing = dp(12)
        stage.stage.add_widget(inner_tabs)
        stage.stage.add_widget(self.carousel)
        self.add_widget(stage)
        self._built = True

    def on_pre_enter(self, *args):
        self._build_ui()
        self.refresh()

    def refresh(self):
        self.carousel.clear_widgets()
        self.carousel.add_widget(self._build_list(done=False))
        self.carousel.add_widget(self._build_list(done=True))

    def _build_list(self, *, done: bool):
        app = App.get_running_app()
        game = app.current_game
        qm = game.quest_manager
        quests = qm.list_completed() if done else qm.list_active()

        tm = game.themes if game else None
        accent = tm.col('accent') if tm else (0.56, 0.79, 1.0, 1)
        fg = tm.col('fg') if tm else (0.93, 0.94, 1.0, 1)

        container = BoxLayout(orientation='vertical', spacing=dp(10),
                              padding=[0, dp(6), 0, dp(12)])

        header_text = "[b]Active Quests[/b]" if not done else "[b]Completed Quests[/b]"
        header = Label(text=header_text, markup=True,
                       font_name=fonts["section_title"]["name"],
                       font_size=fonts["section_title"]["size"],
                       color=(accent[0], accent[1], accent[2], 0.96),
                       halign='left', valign='middle',
                       size_hint_y=None)
        header.bind(width=lambda inst, width: setattr(inst, "text_size", (width, None)))
        header.bind(texture_size=lambda inst, size: setattr(inst, "height", max(size[1], dp(28))))
        container.add_widget(header)

        scroll = ScrollView(do_scroll_x=False, scroll_distance=dp(20), bar_width=dp(4))
        vbox = BoxLayout(orientation='vertical', spacing=dp(10),
                         padding=[dp(5), dp(4), dp(5), dp(10)], size_hint_y=None)
        vbox.bind(minimum_height=vbox.setter("height"))

        if not quests:
            msg = "[i]No completed quests yet[/i]" if done else "[i]No active quests[/i]"
            empty = Label(text=msg, markup=True,
                          font_name=fonts["medium_text"]["name"],
                          font_size=fonts["medium_text"]["size"],
                          color=(fg[0], fg[1], fg[2], 0.72),
                          halign='center', valign='middle',
                          size_hint_y=None)
            empty.bind(width=lambda inst, width: setattr(inst, "text_size", (width, None)))
            empty.bind(texture_size=lambda inst, size: setattr(inst, "height", max(size[1], dp(60))))
            vbox.add_widget(empty)
        else:
            for q in quests:
                card = QuestCard(q, is_tracked=(qm.tracked_quest_id == q.id))

                def _open_detail(_instance, quest=q):
                    overlay = QuestDetailOverlay(quest)
                    overlay.present()

                card.bind(on_release=_open_detail)
                vbox.add_widget(card)

        scroll.add_widget(vbox)
        container.add_widget(scroll)
        return container

    def _sync_tabs(self, _, idx):
        self.btn_active.state = "down" if idx == 0 else "normal"
        self.btn_done.state = "down" if idx == 1 else "normal"
