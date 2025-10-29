# dialogue_box.py
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.boxlayout    import BoxLayout
from kivy.uix.image        import Image
from kivy.uix.label        import Label
from kivy.uix.widget       import Widget
from kivy.graphics         import Color, Rectangle, Line, RoundedRectangle
from kivy.clock            import Clock
from kivy.metrics          import dp
from font_manager          import fonts
from theme_manager         import ThemeManager
from kivy.properties       import NumericProperty, StringProperty


class DialogueBox(FloatLayout):
    """
    A bottom-anchored dialogue box that never covers the full screen.
    """
    # Exposed layout knobs for portrait/name
    portrait_size    = NumericProperty(dp(220))   # square portrait size in px
    portrait_margin_x = NumericProperty(dp(12))   # gap from box edge (left or right)
    portrait_offset_y = NumericProperty(dp(-16))    # vertical offset above box top
    name_gap          = NumericProperty(dp(8))    # gap between portrait and name
    name_offset_y     = NumericProperty(dp(0))    # vertical offset for name
    portrait_side     = StringProperty('left')    # 'left' or 'right'
    def __init__(self, **kwargs):
        super().__init__(**kwargs)

        # ── CRITICAL: only cover bottom slice ───────────────────────────────
        self.size_hint = (0.8, None)          # 80% of screen width
        self.pos_hint  = {"center_x": 0.5, "y": 0}    # Center horizontally
        # ────────────────────────────────────────────────────────────────────

        # ── State flags ────────────────────────────────────────────────────
        self.visible             = False
        self.on_dismiss          = None
        self._typewriter_event   = None
        self._waiting_for_input  = False
        self.text_is_complete    = False
        # start hidden
        self.hide()  # sets disabled=True, visible=False, opacity=0
        # ────────────────────────────────────────────────────────────────────

        # ── Theme defaults & manager ───────────────────────────────────────
        self._default_colors = {
            "bg":     [0.07, 0.07, 0.1, 0.9],
            "border": [0.5,  0.7,  1.0, 1.0],
        }
        self.themes = ThemeManager()
        # ────────────────────────────────────────────────────────────────────

        # ── Build the container ────────────────────────────────────────────
        main_container = BoxLayout(
            orientation="vertical",
            padding=dp(12),
            spacing=dp(5),
            size_hint=(1, None)
        )
        # initial guess, will grow on first text bind:
        main_container.height = dp(100)
        self.container = main_container
        self.add_widget(main_container)

        # ––– Floating header: portrait & name above dialogue box –––
        # portrait and name as free widgets
        self.portrait = Image(
            size_hint=(None, None),
            size=(self.portrait_size, self.portrait_size),
            allow_stretch=True,
            keep_ratio=True
        )
        self.name_lbl = Label(
            text="",
            font_name=fonts["dialogue_name"]["name"],
            font_size=fonts["dialogue_name"]["size"],
            size_hint=(None, None),
        )
        self.add_widget(self.portrait)
        self.add_widget(self.name_lbl)
        # re-position whenever box moves, resizes, or tuning knobs change
        self.bind(pos=self._update_header, height=self._update_header)
        self.bind(portrait_size=self._apply_portrait_size,
                  portrait_margin_x=self._update_header,
                  portrait_offset_y=self._update_header,
                  name_gap=self._update_header,
                  name_offset_y=self._update_header,
                  portrait_side=self._update_header)
        # –––––––––––––––––––––––––––––––––––––––––––––––––––––––––
        # set our widget height to match
        self.height = main_container.height
        # ────────────────────────────────────────────────────────────────────

        # ── Background & border (only behind this widget) ─────────────────
        self.corner_radius = dp(15)  # <-- Adjust this value to change the roundness
        with self.canvas.before:
            self.bg_color     = Color(rgba=self._default_colors["bg"])
            # *** Use RoundedRectangle for the background ***
            self.bg_rect      = RoundedRectangle(
                pos=self.pos, size=self.size, radius=[self.corner_radius]
            )
            self.border_color = Color(rgba=self._default_colors["border"])
            # *** Use rounded_rectangle for the border line ***
            self.border_line  = Line(
                width=dp(1.5),
                rounded_rectangle=(
                    self.x, self.y, self.width, self.height, self.corner_radius
                )
            )
        self.bind(pos=self._update_canvas, size=self._update_canvas)

        # ── Dialogue text (will drive height changes) ─────────────────────
        self.dialogue_lbl = Label(
            text="",
            font_name=fonts["dialogue_text"]["name"],
            font_size=fonts["dialogue_text"]["size"],
            markup=True,
            halign='left',
            valign='top',
            size_hint=(1, None),
            # *** FIX 1: Add horizontal padding to the label itself ***
            padding=(dp(48), 0, dp(0), 0)
        )
        
        # *** FIX 2: Adjust text_size to account for the new padding ***
        self.dialogue_lbl.bind(width=lambda lbl, w: setattr(lbl, 'text_size', (w - dp(48), None)))
        
        # bind wrapped-text size → resize container & self
        self.dialogue_lbl.bind(texture_size=self._update_text_size)
        main_container.add_widget(self.dialogue_lbl)

    def _update_canvas(self, *args):
        # keep bg & border in sync with our widget
        self.bg_rect.pos = self.pos
        self.bg_rect.size = self.size
        # *** CHANGE THIS LINE ***
        self.border_line.rounded_rectangle = (
            self.x, self.y, self.width, self.height, self.corner_radius
        )

    def _update_text_size(self, lbl, texture_size):
        # 1) grow the label
        lbl.height = texture_size[1]
        
        # 2) compute total height: top padding + text + bottom padding
        # *** CHANGE THIS LINE ***
        total = dp(12) + lbl.height + dp(12)
        
        self.container.height = total
        # 3) resize our widget to match
        self.height = total

    def _apply_theme(self):
        """Apply theme colors using the game's current ThemeManager.
        Falls back to our own ThemeManager if needed.
        """
        # Prefer the game's theme manager/state so popups match the room/hub
        try:
            if getattr(self.ui, 'themes', None):
                # Reuse the game's ThemeManager (already pointing to the right env)
                self.themes = self.ui.themes
                bg = getattr(self.ui, 'theme_bg', self.themes.col('bg'))
                fg = getattr(self.ui, 'theme_fg', self.themes.col('fg'))
                border = getattr(self.ui, 'theme_border', self.themes.col('border'))
            else:
                # Fallback: pick by room env or default
                env_key = getattr(self.ui, 'current_room', None)
                self.themes.use(env_key.env if env_key else 'default')
                bg = self.themes.col('bg'); fg = self.themes.col('fg'); border = self.themes.col('border')
        except Exception:
            # Extreme fallback
            bg = self._default_colors['bg']
            border = self._default_colors['border']
            fg = (1,1,1,1)

        self.bg_color.rgba     = [bg[0], bg[1], bg[2], 0.9]
        self.border_color.rgba = border
        self.name_lbl.color     = border
        self.dialogue_lbl.color = fg


    def show_dialogue(self, speaker_id, text, on_dismiss=None):
        from kivy.app import App

        # bring to front
        if self.parent:
            p = self.parent
            p.remove_widget(self)
            p.add_widget(self)

        self.ui = App.get_running_app().current_game
        self._apply_theme()

        # portrait & name
        if speaker_id:
            self.portrait.source = f"images/characters/{speaker_id}_portrait.png"
            self.portrait.opacity = 1
            self.name_lbl.text    = speaker_id.capitalize()
        else:
            self.portrait.opacity = 0
            self.name_lbl.text    = ""
            
        # *** FIX 2: Pre-calculate size and then start the typewriter ***
        self.full_text = text
        self.on_dismiss = on_dismiss
        
        # Set the full text to calculate the final size, but keep it transparent
        self.dialogue_lbl.text = self.full_text
        self.dialogue_lbl.color = (1, 1, 1, 0) # Make text invisible for now
        
        # Let the resize happen, then start the typewriter
        Clock.schedule_once(self._start_typewriter_after_resize, 0)

        # show widget
        self.disabled = False
        self.visible  = True
        self.opacity  = 1.0

    def _start_typewriter_after_resize(self, dt):
        """This function starts the typewriter effect *after* the box has been sized."""
        # Now that the box is the correct size, make the label visible again and clear its text
        self.dialogue_lbl.color = self.themes.col("fg")
        self.dialogue_lbl.text = ""
        
        # reset & start typewriter
        self.text_is_complete     = False
        self._waiting_for_input   = False
        if self._typewriter_event:
            self._typewriter_event.cancel()
        self._typewriter_event = Clock.schedule_interval(self._tick_typewriter, 0.03)


    def _tick_typewriter(self, dt):
        curr = len(self.dialogue_lbl.text)
        if curr < len(self.full_text):
            self.dialogue_lbl.text = self.full_text[:curr+1]
        else:
            if self._typewriter_event:
                self._typewriter_event.cancel()
                self._typewriter_event = None
            self.text_is_complete     = True
            self._waiting_for_input   = True

    def on_touch_down(self, touch):
        # Only advance/dismiss when the box is visible — no matter where you tap.
        if not self.visible:
            return False

        # Case 1: Typewriter effect is still running → skip to end.
        if not self.text_is_complete:
            if self._typewriter_event:
                self._typewriter_event.cancel()
                self._typewriter_event = None
            self.dialogue_lbl.text = self.full_text
            self.text_is_complete = True
            self._waiting_for_input = True
            return True  # consume

        # Case 2: Text fully shown and waiting for input → dismiss.
        if self._waiting_for_input:
            self._waiting_for_input = False
            cb = self.on_dismiss
            self.hide()            # hide box
            if cb:
                cb()               # callback to continue dialogue/gameplay
            return True  # consume

        # Fallback: consume any other tap so the game world behind doesn’t also get it.
        return True

    def hide(self):
        if self._typewriter_event:
            self._typewriter_event.cancel()
        self.disabled = True
        self.visible  = False
        self.opacity  = 0.0
        self.on_dismiss = None

    def _update_header(self, *args):
        """
        Align the bottom of portrait to the top of the box,
        and the name just to its right, vertically centered.
        """
        # Compute portrait anchor (bottom aligned to box top, with optional offsets)
        py = self.y + self.height + self.portrait_offset_y

        # Left or right anchoring
        if (self.portrait_side or '').lower() == 'right':
            px = self.right - self.portrait_margin_x - self.portrait.width
        else:
            px = self.x + self.portrait_margin_x

        self.portrait.pos = (px, py)

        # Size the label to its text and position it relative to the portrait
        w, h = self.name_lbl.texture_size
        self.name_lbl.size = (w, h)

        if (self.portrait_side or '').lower() == 'right':
            nx = px - self.name_gap - w
        else:
            nx = px + self.portrait.width + self.name_gap
        ny = py + self.name_offset_y
        self.name_lbl.pos = (nx, ny)

    def _apply_portrait_size(self, *args):
        # Keep portrait square and reflow header
        try:
            self.portrait.size = (self.portrait_size, self.portrait_size)
        except Exception:
            pass
        self._update_header()
