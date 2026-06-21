import json
import subprocess

files = ["dialogue.json", "enemies.json", "npcs.json", "quests.json", "events.json"]

for fname in files:
    path = f"app/src/main/assets/{fname}"
    try:
        git_content = subprocess.check_output(["git", "show", f"HEAD:{path}"]).decode("utf-8")
        committed_data = json.loads(git_content)
    except Exception as e:
        committed_data = []

    try:
        with open(path, encoding="utf-8") as f:
            working_data = json.load(f)
    except Exception as e:
        working_data = []

    if isinstance(committed_data, list) and isinstance(working_data, list):
        committed_ids = {item.get("id") for item in committed_data if item.get("id")}
        working_ids = {item.get("id") for item in working_data if item.get("id")}
        
        new_ids = working_ids - committed_ids
        removed_ids = committed_ids - working_ids
        
        print(f"File: {fname}")
        print(f"  New IDs added ({len(new_ids)}): {list(new_ids)[:10]}...")
        print(f"  IDs removed ({len(removed_ids)}): {list(removed_ids)[:10]}...")
    elif isinstance(committed_data, dict) and isinstance(working_data, dict):
        committed_keys = set(committed_data.keys())
        working_keys = set(working_data.keys())
        new_keys = working_keys - committed_keys
        print(f"File: {fname}")
        print(f"  New keys added ({len(new_keys)}): {list(new_keys)[:10]}...")
