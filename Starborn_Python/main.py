# main.py -- tiny launcher required by python-for-android


# --- Android / Pydroid + font bootstrap ---
from kivy.config import Config
Config.set('input', 'mtdev_%(name)s', '')
Config.set('kivy', 'window', 'sdl2')
Config.set('graphics', 'multisamples', '0')
Config.set('graphics', 'fullscreen', 'auto')
Config.set('graphics', 'resizable', '0')

# Tell Kivy where the fonts live BEFORE any KV is loaded
import os
from kivy.resources import resource_add_path, resource_find
APP_DIR = os.path.dirname(os.path.abspath(__file__))

# add project-local font dirs
resource_add_path(APP_DIR)
resource_add_path(os.path.join(APP_DIR, "fonts"))
resource_add_path(os.path.join(APP_DIR, "assets", "fonts"))

from game import StarbornApp          # import your real App subclass

if __name__ == "__main__":
    StarbornApp().run()