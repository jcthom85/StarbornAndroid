"""Validate portrait exports and make an art-review sheet; optionally install WebP copies."""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageDraw

parser = argparse.ArgumentParser()
parser.add_argument('--install', action='store_true')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
source = root / 'output/imagegen/hub-portrait-correction'
jobs = [json.loads(line) for line in (source / 'prompts.jsonl').read_text().splitlines()]
sheet = Image.new('RGB', (1088, 1536), '#111820')
draw = ImageDraw.Draw(sheet)
for index, job in enumerate(jobs):
    path = source / job['out']
    with Image.open(path) as picture:
        assert picture.size == (1088, 1920), (path, picture.size)
        assert picture.convert('RGBA').getextrema()[3] == (255, 255), path
        preview = picture.convert('RGB')
        preview.thumbnail((272, 480))
        x, y = (index % 4) * 272, (index // 4) * 512
        sheet.paste(preview, (x, y + 24))
        draw.text((x + 5, y + 5), path.stem.replace('_portrait_v2', ''), fill='white')
        if args.install:
            destination = root / 'world_assets/src/main/assets/images/hubs' / (path.stem + '.webp')
            picture.save(destination, 'WEBP', quality=90, method=6, exact=True)
        print(path.name, picture.size)
sheet.save(source / 'review-sheet.jpg', quality=90)
nodes = json.loads((root / 'app/src/main/assets/hub_nodes.json').read_text())
for node in nodes:
    if not node.get('icon_image'):
        continue
    path = root / 'world_assets/src/main/assets' / node['icon_image']
    with Image.open(path) as icon:
        assert icon.mode == 'RGBA', (node['id'], icon.mode)
        assert icon.getextrema()[3][0] == 0, node['id']
print('All authored destination icons exist and carry transparency.')
ship = source / 'astra-transparent.png'
if args.install and ship.exists():
    with Image.open(ship) as picture:
        assert picture.size == (1024, 1024) and picture.mode == 'RGBA'
        assert all(picture.getpixel(point)[3] == 0 for point in [(0, 0), (1023, 0), (0, 1023), (1023, 1023)])
        preview = Image.new('RGBA', picture.size, '#273441')
        preview.alpha_composite(picture)
        preview.convert('RGB').save(source / 'astra-alpha-review.jpg')
        picture.save(root / 'world_assets/src/main/assets/images/nodes/astra_ship_map_v2.webp',
                     'WEBP', quality=90, method=6, exact=True)
