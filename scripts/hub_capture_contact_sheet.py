"""Make review sheets from emulator captures (no production art edits)."""
import sys
from pathlib import Path
from PIL import Image, ImageDraw
folder = Path(sys.argv[1])
mode = sys.argv[2] if len(sys.argv) > 2 else 'normal'
files = sorted(folder.glob(f'{mode}_*.png'))
sheet = Image.new('RGB', (1200, ((len(files)+3)//4)*690), '#182028')
draw = ImageDraw.Draw(sheet)
for i, path in enumerate(files):
    with Image.open(path) as picture:
        picture.thumbnail((300, 666))
        x, y = (i % 4)*300, (i//4)*690
        sheet.paste(picture, (x, y+24))
        draw.text((x+3, y+3), path.stem, fill='white')
sheet.save(folder / f'{mode}_sheet.jpg', quality=95)
