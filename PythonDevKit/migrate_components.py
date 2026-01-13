# tools/migrate_components.py
"""
Promote tinkering inputs to type 'component' in items.json,
leaving cooking inputs as type 'ingredient'.

Logic:
- Load items.json
- Load recipes_engineering.json (or legacy recipes_tinkering.json if present)
- Any item name that appears as a recipe 'base' or in 'components' => type = 'component'
- Everything else is left as-is (so cooking ingredients stay 'ingredient')
- Writes a backup: items.backup.json
"""

import json, os, sys

# This path resolution is correct and robust. No changes needed here.
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEMS = os.path.join(ROOT, "items.json")

# --- FIX 1: Corrected the candidate paths ---
# The script will now look for these files in the correct order and location.
CANDIDATE_RECIPE_PATHS = [
    os.path.join(ROOT, "data", "recipes_engineering.json"), # Preferred name
    os.path.join(ROOT, "data", "recipes_tinkering.json"),   # Legacy/fallback name
]

# --- FIX 2: Improved error handling ---
# This will now tell you exactly why a file failed to load.
def _load_json(path):
    if not os.path.exists(path): # Check if the file exists first
        return None
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        # Instead of failing silently, print the error.
        print(f"[migrate] Error loading {path}: {e}")
        return None

def main():
    items = _load_json(ITEMS)
    if not isinstance(items, list):
        print("items.json must be a JSON array or could not be loaded.")
        sys.exit(1)

    recipes = None
    found_path = ""
    for p in CANDIDATE_RECIPE_PATHS:
        recipes = _load_json(p)
        if recipes:
            found_path = p
            print(f"[migrate] Using recipes from: {found_path}")
            break
            
    if not recipes:
        print("[migrate] No engineering/tinkering recipes found in candidate paths. Nothing to do.")
        sys.exit(0)

    # Collect every item name used in 'base' or 'components'
    names_for_components = set()
    for r in recipes:
        b = r.get("base")
        if b: names_for_components.add(b)
        for c in r.get("components", []):
            if c: names_for_components.add(c)

    changed = 0
    for it in items:
        nm = it.get("name")
        if nm in names_for_components and it.get("type") != "component":
            print(f"  -> Changing type of '{nm}' to 'component'")
            it["type"] = "component"
            changed += 1

    if not changed:
        print("[migrate] No changes needed. All component types are already set correctly.")
        return

    # backup + write
    backup = os.path.join(ROOT, "items.backup.json")
    # Using items_data variable for backup to avoid confusion.
    with open(ITEMS, "r", encoding="utf-8") as f:
        items_data_for_backup = json.load(f)
    with open(backup, "w", encoding="utf-8") as f:
        json.dump(items_data_for_backup, f, ensure_ascii=False, indent=4)

    with open(ITEMS, "w", encoding="utf-8") as f:
        json.dump(items, f, ensure_ascii=False, indent=4)
        
    print(f"\n[migrate] Success! Updated {changed} items to type 'component'.")
    print(f"Original file backed up to: {backup}")

if __name__ == "__main__":
    main()