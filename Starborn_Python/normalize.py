import json

GRID_STEP = 1.0  # your rooms.pos are in WORLD UNITS (cells). If yours are pixels, divide first.

rooms = json.load(open("rooms.json","r",encoding="utf-8"))
rooms_by_id = {r["id"]: r for r in rooms if isinstance(r, dict) and "id" in r}

# index by integer cell positions
by_pos = {}
for r in rooms:
    if not isinstance(r, dict): continue
    x, y = r.get("pos", [0,0])
    cx, cy = int(round(float(x)/GRID_STEP)), int(round(float(y)/GRID_STEP))
    by_pos[(cx, cy)] = r

def neigh(x,y):
    return {
        "east":  by_pos.get((x+1, y)),
        "west":  by_pos.get((x-1, y)),
        "north": by_pos.get((x, y+1)),  # Y-up
        "south": by_pos.get((x, y-1)),
    }

# rebuild
for r in rooms:
    if not isinstance(r, dict): continue
    x, y = r.get("pos", [0,0])
    cx, cy = int(round(float(x)/GRID_STEP)), int(round(float(y)/GRID_STEP))
    ns = {}
    for d, nbr in neigh(cx, cy).items():
        if nbr: ns[d] = nbr["id"]
    r["connections"] = ns

json.dump(rooms, open("rooms.json","w",encoding="utf-8"), indent=2, ensure_ascii=False)
print("Rebuilt NSEW for", len(rooms), "rooms.")
