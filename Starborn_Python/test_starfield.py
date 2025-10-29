from kivy.app import App
from kivy.core.window import Window
from kivy.uix.floatlayout import FloatLayout
from kivy.graphics import Color, Rectangle
from generate_starfield import ParallaxStarfield

# ensure the window itself is black
Window.clearcolor = (0, 0, 0, 1)

class StarfieldTestApp(App):
    def build(self):
        root = FloatLayout()

        # in case clearcolor isn't applied on some platforms
        with root.canvas.before:
            Color(0, 0, 0, 1)
            Rectangle(pos=(0, 0), size=Window.size)

        # add the starfield, filling the screen
        sf = ParallaxStarfield(size_hint=(1, 1), pos_hint={'x': 0, 'y': 0})
        root.add_widget(sf)

        return root

if __name__ == "__main__":
    StarfieldTestApp().run()
