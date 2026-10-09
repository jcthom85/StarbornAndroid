"""Collect renderer PNG sequences into review sheets and GIFs; never writes game assets."""
from pathlib import Path
from PIL import Image, ImageDraw
import json
root=Path(__file__).resolve().parents[2]
folder=root/"desktopApp/build/reports/environmental-effects"
catalog=json.loads((root/"app/src/main/assets/environmental_effects.json").read_text())["presets"]
for preset,definition in catalog.items():
    for level in ["whisper","present","extreme"]:
        paths=[folder/f"{preset}_{level}-{frame}.png" for frame in range(12)]
        if not all(p.exists() for p in paths):continue
        frames=[Image.open(p).convert("RGB") for p in paths]
        frames[0].save(folder/f"{preset}_{level}.gif",save_all=True,append_images=frames[1:],loop=0,duration=200 if definition.get("burst_interval_seconds") else 500)
    sheet=Image.new("RGB",(900,510),"#071019");draw=ImageDraw.Draw(sheet)
    for index,level in enumerate(["whisper","present","extreme"]):
        path=folder/f"{preset}_{level}-0.png"
        if path.exists():sheet.paste(Image.open(path).convert("RGB"),(index*300,30));draw.text((index*300+8,9),f"{preset} / {level}",fill="white")
    sheet.save(folder/f"{preset}-intensities.jpg")
# Before/after images from the real Android renderer.
android=folder/"android-latest"
if android.exists():
    for family in ["dust","rain","storm","snow","cave_drip","starfall","steam","fog","gas","resonance","sparks","industrial"]:
        sheet=Image.new("RGB",(720,830),"#071019");draw=ImageDraw.Draw(sheet)
        for i,version in enumerate(["legacy","shared"]):
            p=android/f"{version}-{family}.png"
            if p.exists():
                im=Image.open(p).convert("RGB");im.thumbnail((360,800));sheet.paste(im,(i*360+(360-im.width)//2,30));draw.text((i*360+8,9),f"Android {version} / {family}",fill="white")
        sheet.save(folder/f"android-{family}-comparison.jpg")
print(f"Review gallery: {folder}")

import html
parts=['<!doctype html><meta charset="utf-8"><title>Starborn environmental effects review</title><style>body{background:#071019;color:#dfeaf0;font:16px system-ui;margin:32px}h1,h2{color:#65dff3}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,300px));gap:16px}img{max-width:100%;border-radius:8px}a{color:#65dff3}section{margin:36px 0}p{max-width:900px}</style><h1>Starborn environmental effects</h1><p>Production-renderer sampled motion at Whisper, Present, and Extreme. Burst effects begin near their first scheduled event. Campaign rooms use restrained authored values. These clips are review samples, not real-time frame-rate recordings.</p>']
for preset in catalog:
    parts.append('<section><h2>'+html.escape(preset)+'</h2><div class="grid">')
    for level in ['whisper','present','extreme']:parts.append(f'<div><p>{level}</p><a href="{preset}_{level}.gif"><img loading="lazy" src="{preset}_{level}.gif"></a></div>')
    parts.append('</div></section>')
parts.append('<h2>Native Android comparisons</h2><div class="grid">')
for family in ['dust','rain','storm','snow','cave_drip','starfall','steam','fog','gas','resonance','sparks','industrial']:parts.append(f'<a href="android-{family}-comparison.jpg"><img loading="lazy" src="android-{family}-comparison.jpg">{family}</a>')
parts.append('</div>')
(folder/'index.html').write_text('\n'.join(parts),encoding='utf-8')
