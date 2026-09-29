#!/usr/bin/env python3
import json
import re
from pathlib import Path

content = Path("app/src/main/java/com/example/starborn/feature/hub/ui/HubMapLayout.kt").read_text(encoding="utf-8")

layouts = {}

# Split file by '"hub_' to parse each hub definition block
parts = content.split('"hub_')
for part in parts[1:]:
    hub_id = "hub_" + part.split('"')[0]
    
    # Extract mapOf block
    if "mapOf(" in part:
        map_start = part.find("mapOf(") + len("mapOf(")
        # Find closing of mapOf
        depth = 1
        pos = map_start
        while pos < len(part) and depth > 0:
            if part[pos] == '(':
                depth += 1
            elif part[pos] == ')':
                depth -= 1
            pos += 1
        sites_body = part[map_start:pos-1]
    else:
        sites_body = ""

    sites = {}
    site_pattern = re.compile(r'"([a-zA-Z0-9_]+)"\s*to\s*site\((.*?)\)')
    for sm in site_pattern.finditer(sites_body):
        node_id = sm.group(1)
        args = [float(a.replace('f', '').strip()) for a in sm.group(2).split(',') if a.strip()]
        x = args[0] if len(args) > 0 else 0.5
        y = args[1] if len(args) > 1 else 0.5
        w = args[2] if len(args) > 2 else 0.25
        sites[node_id] = {"x": x, "y": y, "artwork_width": w}

    astra_dock = None
    astra_m = re.search(r'astraDock\s*=\s*site\((.*?)\)', part)
    if astra_m:
        args = [float(a.replace('f', '').strip()) for a in astra_m.group(1).split(',') if a.strip()]
        x = args[0] if len(args) > 0 else 0.5
        y = args[1] if len(args) > 1 else 0.5
        w = args[2] if len(args) > 2 else 0.25
        astra_dock = {"x": x, "y": y, "artwork_width": w}

    layouts[hub_id] = {
        "sites": sites,
        "astra_dock": astra_dock
    }

print(f"Extracted layouts for {len(layouts)} hubs:")
for hid, data in layouts.items():
    print(f"  {hid:<22}: {len(data['sites'])} sites, astra: {data['astra_dock']}")

out_file = Path("tools/hub_layouts.json")
out_file.write_text(json.dumps(layouts, indent=2), encoding="utf-8")
print(f"Saved to {out_file}")
