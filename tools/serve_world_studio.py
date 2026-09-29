#!/usr/bin/env python3
"""
Starborn World Graph & Navigation Studio - Local Server
Serves the web studio and provides safe, atomic API endpoints for reading
and writing app/src/main/assets/rooms.json with automated timestamped backups.
"""

import http.server
import socketserver
import json
import os
import sys
import datetime
from pathlib import Path

PORT = 8765
BASE_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = BASE_DIR.parent
ASSETS_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets"
ROOMS_FILE = ASSETS_DIR / "rooms.json"
HUB_NODES_FILE = ASSETS_DIR / "hub_nodes.json"
HUBS_FILE = ASSETS_DIR / "hubs.json"
WORLDS_FILE = ASSETS_DIR / "worlds.json"

class StudioHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(BASE_DIR), **kwargs)

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()

    def do_GET(self):
        if self.path == "/" or self.path == "/index.html":
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            html_file = BASE_DIR / "world_graph_studio.html"
            if html_file.exists():
                with open(html_file, "rb") as f:
                    self.wfile.write(f.read())
            else:
                self.wfile.write(b"<h1>world_graph_studio.html not found</h1>")
            return

        if self.path == "/api/data":
            try:
                rooms = json.load(open(ROOMS_FILE, encoding="utf-8")) if ROOMS_FILE.exists() else []
                hub_nodes = json.load(open(HUB_NODES_FILE, encoding="utf-8")) if HUB_NODES_FILE.exists() else []
                hubs = json.load(open(HUBS_FILE, encoding="utf-8")) if HUBS_FILE.exists() else []
                worlds = json.load(open(WORLDS_FILE, encoding="utf-8")) if WORLDS_FILE.exists() else []

                payload = {
                    "status": "ok",
                    "timestamp": datetime.datetime.now().isoformat(),
                    "rooms": rooms,
                    "hub_nodes": hub_nodes,
                    "hubs": hubs,
                    "worlds": worlds,
                    "file_path": str(ROOMS_FILE)
                }
                self.send_response(200)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.end_headers()
                self.wfile.write(json.dumps(payload).encode("utf-8"))
            except Exception as e:
                self.send_response(500)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(json.dumps({"status": "error", "message": str(e)}).encode("utf-8"))
            return

        super().do_GET()

    def do_POST(self):
        if self.path == "/api/save":
            try:
                content_len = int(self.headers.get("Content-Length", 0))
                post_body = self.rfile.read(content_len)
                data = json.loads(post_body.decode("utf-8"))

                rooms = data.get("rooms")
                if not isinstance(rooms, list):
                    raise ValueError("Payload must contain a 'rooms' list.")

                # Create timestamped backup
                now_str = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
                backup_dir = BASE_DIR / "backups"
                backup_dir.mkdir(parents=True, exist_ok=True)
                backup_path = backup_dir / f"rooms_backup_{now_str}.json"

                if ROOMS_FILE.exists():
                    with open(ROOMS_FILE, "r", encoding="utf-8") as orig:
                        with open(backup_path, "w", encoding="utf-8") as bk:
                            bk.write(orig.read())

                # Write formatted JSON atomically
                temp_file = ROOMS_FILE.with_suffix(".tmp")
                with open(temp_file, "w", encoding="utf-8") as f:
                    json.dump(rooms, f, indent=2, ensure_ascii=False)
                    f.write("\n")

                temp_file.replace(ROOMS_FILE)

                hub_nodes = data.get("hub_nodes")
                if hub_nodes and isinstance(hub_nodes, list):
                    hub_backup_path = backup_dir / f"hub_nodes_backup_{now_str}.json"
                    if HUB_NODES_FILE.exists():
                        with open(HUB_NODES_FILE, "r", encoding="utf-8") as orig:
                            with open(hub_backup_path, "w", encoding="utf-8") as bk:
                                bk.write(orig.read())
                    temp_hub_file = HUB_NODES_FILE.with_suffix(".tmp")
                    with open(temp_hub_file, "w", encoding="utf-8") as f:
                        json.dump(hub_nodes, f, indent=2, ensure_ascii=False)
                        f.write("\n")
                    temp_hub_file.replace(HUB_NODES_FILE)

                resp = {
                    "status": "ok",
                    "message": f"Successfully saved {len(rooms)} rooms to {ROOMS_FILE.name}.",
                    "backup": backup_path.name,
                    "timestamp": now_str
                }
                self.send_response(200)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.end_headers()
                self.wfile.write(json.dumps(resp).encode("utf-8"))
            except Exception as e:
                self.send_response(500)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.end_headers()
                self.wfile.write(json.dumps({"status": "error", "message": str(e)}).encode("utf-8"))
            return

        self.send_response(404)
        self.end_headers()

def main():
    print("=" * 65)
    print("  Starborn World Graph & Navigation Studio Server")
    print("=" * 65)
    print(f"  Target File : {ROOMS_FILE}")
    print(f"  Studio URL  : http://localhost:{PORT}")
    print("=" * 65)
    print("  Press Ctrl+C to stop server.\n")

    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), StudioHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down server cleanly...")

if __name__ == "__main__":
    main()
