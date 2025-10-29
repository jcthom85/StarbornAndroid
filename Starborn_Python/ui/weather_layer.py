# ui/weather_layer.py
from kivy.uix.widget import Widget
from kivy.clock import Clock
from kivy.animation import Animation
from kivy.graphics import Color, Ellipse, Rectangle, InstructionGroup
from kivy.metrics import dp
import random
import math

class WeatherLayer(Widget):
    def __init__(self, mode='none', intensity=0.8, **kw):
        super().__init__(**kw)
        self.mode = mode
        self.intensity = float(intensity)
        self._parts = []   # [ (shape, (vx,vy), life) ]
        self._instruction_group = InstructionGroup()
        self._fx_instruction_group = InstructionGroup() # For lightning
        self.canvas.add(self._instruction_group)
        self.canvas.add(self._fx_instruction_group)
        Clock.schedule_interval(self._tick, 1/60.0)

    def set_mode(self, mode: str):
        print(f"[DEBUG] WeatherLayer.set_mode called with: '{mode}'")
        self.mode = (mode or 'none').lower()
        # Clear only the instruction group, not the entire canvas
        self._instruction_group.clear()
        self._fx_instruction_group.clear()
        self._parts.clear()

    def _spawn_one(self):
        if not self.width or not self.height:
            return
        if self.mode == 'dust':
            # --- DUST REWORKED ---
            # Dust now blows horizontally across the screen from either side.
            r = dp(random.uniform(4, 8)) # Randomized size (2/3 smaller)
            side = random.choice([-1, 1]) # -1 for left, 1 for right
            x = -r if side == -1 else self.width + r
            y = random.uniform(0, self.height)
            # --- THIS IS THE FIX ---
            # Velocity should be opposite to the spawn side to move across the screen.
            vx = random.uniform(self.width * 0.05, self.width * 0.15) * -side # Much slower horizontal speed
            vy_base = random.uniform(-35, 35) # More vertical movement
            life = random.uniform(3.5, 5.5) # Longer lifetime
            color = Color(1.0, 0.93, 0.75, random.uniform(0.15, 0.35)) # Randomized alpha for depth
            shape = Ellipse(pos=(x, y), size=(r * 2.5, r)) # Wider motes
            # Add turbulence properties: [amplitude, frequency, phase]
            turbulence = [random.uniform(25, 50), random.uniform(0.5, 1.2), random.uniform(0, 2 * math.pi)]
            self._instruction_group.add(color)
            self._instruction_group.add(shape)
            self._parts.append([shape, (vx, vy_base), life, color, turbulence])

        elif self.mode in ('rain', 'storm'):
            # Rain is unchanged, as it already looks good.
            # Fast, straight-falling streaks.
            # --- THIS IS THE FIX: Increased rain drop size ---
            w, h = dp(4), dp(48)
            x  = random.randint(-int(self.width*0.1), int(self.width*1.1))
            y  = self.height + dp(10)
            vx = 0
            vy = -random.uniform(self.height*0.9, self.height*1.3)
            life = random.uniform(0.6, 1.2)
            color = Color(0.8, 0.9, 1.0, 0.25)
            shape = Rectangle(pos=(x, y), size=(w, h))
            self._instruction_group.add(color)
            self._instruction_group.add(shape)
            self._parts.append([shape, (vx, vy), life, color])

        elif self.mode == 'snow':
            # --- SNOW REWORKED ---
            # Snowflakes now have a gentle side-to-side swaying motion.
            r = dp(random.uniform(7, 11)) # Varied snowflake size
            x = random.randint(0, int(self.width))
            y = self.height + dp(10)
            vx_base = random.uniform(-25, 25) # More horizontal drift
            vy = -random.uniform(30, 55) # Varied fall speed
            life = random.uniform(3.0, 5.5)
            color = Color(1, 1, 1, 0.65)
            shape = Ellipse(pos=(x, y), size=(r, r))
            # Store sway properties: [amplitude, frequency, phase]
            sway = [random.uniform(20, 40), random.uniform(0.7, 1.8), random.uniform(0, 2 * math.pi)]
            self._instruction_group.add(color)
            self._instruction_group.add(shape)
            self._parts.append([shape, (vx_base, vy), life, color, sway]) # Add sway to part data
        
        elif self.mode == 'cave_drip':
            # A more realistic drip that hangs, grows, and then falls with acceleration.
            initial_w, initial_h = dp(2.5), dp(2.5)
            x = random.randint(0, int(self.width))
            y = self.height - initial_h # Start just at the top edge
            vx = 0
            vy = 0 # Starts with no vertical velocity
            life = random.uniform(3.5, 5.0)
            color = Color(0.6, 0.8, 1.0, 0.7) # Blue-tinted water
            shape = Rectangle(pos=(x, y), size=(initial_w, initial_h))
            
            # Add extra state for the drip's lifecycle
            # [shape, (vx,vy), life, color, state, hang_time, initial_size]
            state = 'hanging'
            hang_time = random.uniform(0.5, 1.5)
            self._instruction_group.add(color)
            self._instruction_group.add(shape)
            self._parts.append([shape, (vx, vy), life, color, state, hang_time, (initial_w, initial_h)])

        elif self.mode == 'starfall':
            # --- NEW EFFECT: STARFALL ---
            # Fast-moving, bright streaks for a "shooting stars" effect.
            w, h = dp(2.5), dp(60)
            x = random.uniform(0, self.width)
            y = self.height + h
            vx = 0
            vy = -random.uniform(self.height * 2, self.height * 3) # Very fast
            life = random.uniform(0.4, 0.8)
            # Bright, slightly blue-tinted white
            color = Color(0.9, 0.95, 1.0, 0.8)
            shape = Rectangle(pos=(x, y), size=(w, h))
            self._instruction_group.add(color)
            self._instruction_group.add(shape)
            self._parts.append([shape, (vx, vy), life, color])

    def _trigger_lightning(self):
        """Creates a full-screen, short-lived white flash."""
        if not self.size[0] or not self.size[1]:
            return

        flash_color = Color(1, 1, 1, 0)
        flash_rect = Rectangle(pos=self.pos, size=self.size)

        self._fx_instruction_group.add(flash_color)
        self._fx_instruction_group.add(flash_rect)

        # A quick, bright flash with a slight flicker, then a longer fade out
        anim = (Animation(a=0.8, duration=0.05) +
                Animation(a=0.4, duration=0.05) +
                Animation(a=0.9, duration=0.04) +
                Animation(duration=0.08) + # Hold briefly
                Animation(a=0, duration=0.6, t='out_quad'))

        def cleanup(*_):
            self._fx_instruction_group.remove(flash_color)
            self._fx_instruction_group.remove(flash_rect)
        anim.bind(on_complete=cleanup)
        anim.start(flash_color)

    def _tick(self, dt):
        # --- DUST POLISH: Increase particle count for a denser effect ---
        if self.mode == 'dust':
            target = int(5 * max(0.2, self.intensity) * (self.width / 360.0))
        elif self.mode == 'cave_drip':
            # Very infrequent drips
            target = int(2 * max(0.2, self.intensity) * (self.width / 360.0))
        else:
            target = int(12 * max(0.2, self.intensity) * (self.width / 360.0))
        for _ in range(max(0, target - len(self._parts))):
            self._spawn_one()

        # --- NEW: Lightning for storm mode ---
        if self.mode == 'storm':
            if random.random() < 0.005: # A small chance each frame
                self._trigger_lightning()

        alive_parts = []
        to_remove = []
        for part in self._parts:
            # --- THIS IS THE FIX ---
            # Safely unpack particle data. Dust/Rain/Starfall have 4 elements, Snow has 5.
            shape, (vx, vy), life, color = part[:4]
            life -= dt
            if life <= 0:
                to_remove.append(shape)
                to_remove.append(color)
                continue
            x, y = shape.pos

            # --- Apply mode-specific movement ---
            final_vx = vx
            final_vy = vy
            if self.mode == 'snow' and len(part) > 4:
                sway = part[4]
                amplitude, freq, phase = sway
                # Add a sine wave to the horizontal velocity for a swaying motion
                final_vx += amplitude * math.sin(freq * life + phase)
            elif self.mode == 'dust' and len(part) > 4:
                turbulence = part[4]
                amplitude, freq, phase = turbulence
                # Add a sine wave to the vertical velocity for a swirling motion
                final_vy += amplitude * math.sin(freq * life + phase)

            elif self.mode == 'cave_drip' and len(part) > 4:
                state, hang_time, initial_size = part[4], part[5], part[6]
                if state == 'hanging':
                    part[5] -= dt # Decrement hang_time
                    if part[5] <= 0:
                        part[4] = 'falling' # Switch to falling state
                    else:
                        # Grow while hanging
                        max_h = initial_size[1] * 4
                        progress = 1.0 - (part[5] / hang_time)
                        shape.size = (shape.size[0], initial_size[1] + (max_h - initial_size[1]) * progress)
                else: # 'falling'
                    # Apply gravity
                    gravity = -self.height * 2.5
                    final_vy += gravity * dt
                    # Update velocity in the particle data for next frame
                    part[1] = (vx, final_vy)
                    # Stretch the drip as it falls faster
                    speed = abs(final_vy)
                    shape.size = (shape.size[0], initial_size[1] + speed * 0.02)

            # Update position for all particle types
            shape.pos = (x + final_vx * dt, y + final_vy * dt)
            part[2] = life
            alive_parts.append(part)
        self._parts = alive_parts
        for instruction in to_remove:
            self._instruction_group.remove(instruction)