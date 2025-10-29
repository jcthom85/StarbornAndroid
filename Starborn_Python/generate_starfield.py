import random
from kivy.uix.widget import Widget
from kivy.graphics import Color, Rectangle, Point, Line, InstructionGroup
from kivy.clock import Clock
from kivy.properties import NumericProperty, ObjectProperty, ListProperty
from theme_manager import ThemeManager
from kivy.metrics import dp

class ParallaxStarfield(Widget):
    """
    A widget that displays a multi-layered parallax starfield.
    Stars move at different speeds to create an illusion of depth.
    """
    num_stars_per_layer = NumericProperty(24)
    speed_scale = NumericProperty(1200)     # Default speed
    alpha = NumericProperty(1.0)
    color_tint = ListProperty([1.0, 1.0, 1.0])
    point_size_base = NumericProperty(1.5)

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self._layers = []
        self.bind(size=self._init_starfield, pos=self._init_starfield)
        self._update_event = None # Animation is now driven externally.
        self.bind(alpha=self._update_layer_colors, color_tint=self._update_layer_colors)
        # --- NEW: Add properties to store the last used theme ---
        self._theme_manager = None
        self._theme_name = None


    def apply_theme(self, theme_manager: ThemeManager, theme_name: str):
        """Applies colors from the theme to the star layers."""
        # --- MODIFIED: Store the theme for later use ---
        self._theme_manager = theme_manager
        self._theme_name = theme_name
        self._reapply_theme_colors()

    # --- NEW: Extracted the color application logic into its own method ---
    def _reapply_theme_colors(self):
        """Applies the stored theme colors to the current star layers."""
        if not self._theme_manager or not self._layers:
            return

        self._theme_manager.use(self._theme_name)
        fg_col = self._theme_manager.col('fg')
        accent_col = self._theme_manager.col('accent')
        border_col = self._theme_manager.col('border')

        layer_colors = [
            fg_col,       # Farthest stars are foreground color
            accent_col,   # Mid-ground stars are accent color
            border_col    # Closest stars are border color
        ]

        if self._layers:
            for i, layer in enumerate(self._layers):
                color = layer_colors[i % len(layer_colors)]
                layer['base_color'] = color
            self._update_layer_colors()

    def _update_layer_colors(self, *args):
        """
        This new method correctly applies both tint and global alpha to each star's
        individual Color instruction.
        """
        for layer in self._layers:
            base_color = layer.get('base_color', [1.0, 1.0, 1.0, 1.0])
            # Tint the base color
            final_rgb = [base_color[i] * self.color_tint[i] for i in range(3)]
            # Apply the widget's global alpha
            final_a = base_color[3] * self.alpha
            final_rgba = final_rgb + [final_a]

            for star in layer.get('stars', []):
                if 'color_inst' in star:
                    star['color_inst'].rgba = final_rgba

    def _init_starfield(self, *args):
        self.canvas.clear()
        self._layers = []
        if self.width <= 0 or self.height <= 0:
            return

        for i in range(3):  # Create 3 layers
            # Slower layers are farther away
            speed_multiplier = (i + 1) * 0.2

            stars = []
            for _ in range(self.num_stars_per_layer):
                # Group each star's Color and Line instructions for better management.
                group = InstructionGroup()
                color_inst = Color(1, 1, 1, 1)
                line_inst = Line(points=[0,0,0,0], width=dp(self.point_size_base) + i*0.5)
                group.add(color_inst)
                group.add(line_inst)
                self.canvas.add(group)
                stars.append({
                    'pos': [random.uniform(0, self.width), random.uniform(0, self.height)],
                    'line_inst': line_inst,
                    'color_inst': color_inst
                })

            layer = {
                "stars": stars,
                "speed": speed_multiplier,
                # The layer's 'color' is now just for storing the base theme color
                "base_color": [1, 1, 1, 1],
            }
            self._layers.append(layer)

        # --- NEW: Re-apply the theme after recreating the stars ---
        if self._theme_manager:
            self._reapply_theme_colors()


    def update(self, dt):
        """Called every frame to move the stars."""
        for layer in self._layers:
            speed_factor = dt * self.speed_scale
            vx = layer['speed'] * speed_factor
            vy = layer['speed'] * speed_factor

            for star in layer['stars']:
                x, y = star['pos']
                # Update coordinates
                x += vx
                y -= vy

                # If star goes off the bottom, wrap it to the top with a new random x
                if y < 0:
                    y = self.height # Reset y to top
                    x = random.uniform(0, self.width) # New random x
                elif x > self.width:
                    x = 0 # Reset x to left

                star['pos'] = [x, y]
                # Create a trail effect by drawing a line based on velocity
                trail_length = 8.0 # Increase this for even longer trails
                star['line_inst'].points = [x, y, x - vx * trail_length, y + vy * trail_length]