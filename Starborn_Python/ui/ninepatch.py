# ui/ninepatch.py
from kivy.uix.widget import Widget
from kivy.graphics import BorderImage, Color
class NinePatchFrame(Widget):
    def __init__(self, src, border=(16,16,16,16), **kw):
        super().__init__(**kw)
        with self.canvas:
            Color(1,1,1,1)
            self.img = BorderImage(source=src, border=border,
                                    pos=self.pos, size=self.size)
        self.bind(pos=self._u, size=self._u)
    def _u(self,*_):
        self.img.pos = self.pos; self.img.size = self.size