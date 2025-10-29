# c:/Starborn/test_weather.py

from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.floatlayout import FloatLayout
from kivy.core.window import Window
from kivy.graphics import Color, Rectangle

# Make sure the weather_layer can be imported
import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from ui.weather_layer import WeatherLayer

class WeatherTestApp(App):
    def build(self):
        # Root layout
        root = FloatLayout()

        # Black background to see particles
        with root.canvas.before:
            Color(0, 0, 0, 1)
            self.bg = Rectangle(size=Window.size, pos=(0,0))
        Window.bind(size=lambda *_: setattr(self.bg, 'size', Window.size))

        # Weather layer
        self.weather = WeatherLayer(size_hint=(1, 1))
        root.add_widget(self.weather)

        # Controls
        controls = BoxLayout(size_hint=(1, None), height='48dp', pos_hint={'top': 1})
        
        btn_rain = Button(text='Rain')
        btn_rain.bind(on_release=lambda *_: self.weather.set_mode('rain'))
        
        btn_snow = Button(text='Snow')
        btn_snow.bind(on_release=lambda *_: self.weather.set_mode('snow'))

        btn_dust = Button(text='Dust')
        btn_dust.bind(on_release=lambda *_: self.weather.set_mode('dust'))

        btn_starfall = Button(text='Starfall')
        btn_starfall.bind(on_release=lambda *_: self.weather.set_mode('starfall'))

        btn_storm = Button(text='Storm')
        btn_storm.bind(on_release=lambda *_: self.weather.set_mode('storm'))

        btn_cave_drip = Button(text='Cave Drip')
        btn_cave_drip.bind(on_release=lambda *_: self.weather.set_mode('cave_drip'))

        btn_none = Button(text='None')
        btn_none.bind(on_release=lambda *_: self.weather.set_mode('none'))

        for btn in [btn_rain, btn_snow, btn_dust, btn_starfall, btn_storm, btn_cave_drip, btn_none]:
            controls.add_widget(btn)
        
        root.add_widget(controls)
        return root

if __name__ == '__main__':
    WeatherTestApp().run()