#!/usr/bin/env python3
"""
Build script for Starborn World Graph & Navigation Studio.
Generates tools/world_graph_studio.html with:
1. Bundled offline dataset (rooms, hub_nodes, hubs, worlds, hub_layouts)
2. Room background image display (Inspector preview + Canvas Art Mode toggle + Lightbox)
3. Interactive Hub Screen Layout with drag-and-drop node placement, sliders, crosshairs, and Astra ship dock
4. Strict NSEW swipe navigation validation and visual editor
"""

import json
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = BASE_DIR.parent
ASSETS_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets"
HUB_LAYOUTS_FILE = BASE_DIR / "hub_layouts.json"

def build():
    rooms = json.load(open(ASSETS_DIR / "rooms.json", encoding="utf-8"))
    hub_nodes = json.load(open(ASSETS_DIR / "hub_nodes.json", encoding="utf-8"))
    hubs = json.load(open(ASSETS_DIR / "hubs.json", encoding="utf-8"))
    worlds = json.load(open(ASSETS_DIR / "worlds.json", encoding="utf-8"))
    hub_layouts = json.load(open(HUB_LAYOUTS_FILE, encoding="utf-8")) if HUB_LAYOUTS_FILE.exists() else {}

    bundled_data = {
        "rooms": rooms,
        "hub_nodes": hub_nodes,
        "hubs": hubs,
        "worlds": worlds,
        "hub_layouts": hub_layouts
    }

    bundled_json_str = json.dumps(bundled_data, ensure_ascii=False)

    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Starborn // World Graph & Navigation Studio</title>
  <style>
    :root {{
      --bg: #090d16;
      --panel: #111726;
      --panel-elevated: #182238;
      --panel-hover: #1e2b45;
      --border: #23304a;
      --border-focus: #00e5ff;
      --text: #e2e8f0;
      --text-muted: #8492a6;
      --accent-cyan: #00e5ff;
      --accent-green: #10b981;
      --accent-amber: #f59e0b;
      --accent-red: #ef4444;
      --accent-purple: #a855f7;
      --accent-gold: #fbbf24;
      --font-mono: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    }}

    * {{ box-sizing: border-box; margin: 0; padding: 0; user-select: none; }}

    body {{
      background-color: var(--bg);
      color: var(--text);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      height: 100vh;
      width: 100vw;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }}

    /* HEADER */
    header {{
      background: var(--panel);
      border-bottom: 1px solid var(--border);
      height: 52px;
      padding: 0 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      z-index: 50;
      flex-shrink: 0;
    }}

    .brand {{
      display: flex;
      align-items: center;
      gap: 12px;
    }}
    .brand h1 {{
      font-size: 14px;
      font-weight: 700;
      letter-spacing: 1px;
      color: #fff;
      display: flex;
      align-items: center;
      gap: 8px;
    }}
    .brand span.tag {{
      background: rgba(0, 229, 255, 0.12);
      border: 1px solid rgba(0, 229, 255, 0.35);
      color: var(--accent-cyan);
      font-size: 10px;
      padding: 2px 7px;
      border-radius: 4px;
      font-family: var(--font-mono);
      font-weight: 600;
    }}

    .server-status {{
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 11px;
      font-family: var(--font-mono);
      background: rgba(0,0,0,0.3);
      padding: 3px 8px;
      border-radius: 6px;
      border: 1px solid var(--border);
    }}
    .status-dot {{
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--accent-green);
      box-shadow: 0 0 8px var(--accent-green);
    }}
    .status-dot.offline {{
      background: var(--accent-amber);
      box-shadow: 0 0 8px var(--accent-amber);
    }}

    .view-mode-tabs {{
      display: flex;
      background: rgba(0,0,0,0.35);
      padding: 3px;
      border-radius: 8px;
      border: 1px solid var(--border);
      gap: 3px;
    }}
    .view-mode-btn {{
      background: transparent;
      border: none;
      color: var(--text-muted);
      padding: 5px 12px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 700;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 6px;
      transition: all 0.15s ease;
    }}
    .view-mode-btn:hover {{
      color: #fff;
      background: rgba(255,255,255,0.06);
    }}
    .view-mode-btn.active {{
      background: var(--accent-cyan);
      color: #04101e;
      box-shadow: 0 0 10px rgba(0, 229, 255, 0.4);
    }}

    .header-actions {{
      display: flex;
      align-items: center;
      gap: 8px;
    }}

    .toggle-pill {{
      display: flex;
      align-items: center;
      gap: 6px;
      background: var(--panel-elevated);
      border: 1px solid var(--border);
      padding: 4px 10px;
      border-radius: 6px;
      font-size: 11px;
      cursor: pointer;
      color: var(--text-muted);
    }}
    .toggle-pill input {{ cursor: pointer; }}
    .toggle-pill:hover {{ color: var(--text); border-color: var(--border-focus); }}

    button.btn {{
      background: var(--panel-elevated);
      border: 1px solid var(--border);
      color: var(--text);
      padding: 5px 11px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 6px;
      transition: all 0.15s ease;
    }}
    button.btn:hover {{
      background: var(--panel-hover);
      border-color: var(--border-focus);
      color: #fff;
    }}
    button.btn-primary {{
      background: #0284c7;
      border-color: #38bdf8;
      color: #fff;
    }}
    button.btn-primary:hover {{
      background: #0369a1;
      box-shadow: 0 0 10px rgba(56, 189, 248, 0.4);
    }}
    button.btn-save {{
      background: rgba(16, 185, 129, 0.15);
      border-color: rgba(16, 185, 129, 0.4);
      color: #34d399;
    }}
    button.btn-save:hover {{
      background: #059669;
      border-color: #10b981;
      color: #fff;
      box-shadow: 0 0 12px rgba(16, 185, 129, 0.5);
    }}

    /* HIERARCHY FILTER BAR */
    .filter-bar {{
      background: #0d1322;
      border-bottom: 1px solid var(--border);
      padding: 8px 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      z-index: 40;
    }}

    .filter-row-top {{
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
    }}

    .world-tabs {{
      display: flex;
      gap: 3px;
      background: var(--panel);
      padding: 3px;
      border-radius: 8px;
      border: 1px solid var(--border);
      overflow-x: auto;
    }}
    .world-tab {{
      padding: 4px 10px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 600;
      cursor: pointer;
      color: var(--text-muted);
      white-space: nowrap;
      transition: all 0.15s ease;
    }}
    .world-tab:hover {{ color: var(--text); background: rgba(255,255,255,0.05); }}
    .world-tab.active {{
      background: var(--accent-cyan);
      color: #04101e;
      box-shadow: 0 0 8px rgba(0, 229, 255, 0.4);
    }}

    .filter-row-sub {{
      display: flex;
      align-items: center;
      gap: 14px;
      flex-wrap: wrap;
    }}

    .select-group {{
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 11px;
      color: var(--text-muted);
    }}
    .select-group strong {{ color: #cbd5e1; }}

    select.studio-select {{
      background: var(--panel-elevated);
      border: 1px solid var(--border);
      color: #fff;
      padding: 4px 10px;
      border-radius: 6px;
      font-size: 11px;
      cursor: pointer;
      outline: none;
    }}
    select.studio-select:focus {{
      border-color: var(--border-focus);
    }}

    .node-pills {{
      display: flex;
      align-items: center;
      gap: 4px;
      overflow-x: auto;
      max-width: calc(100vw - 480px);
      padding-bottom: 2px;
    }}
    .node-pill {{
      background: var(--panel-elevated);
      border: 1px solid var(--border);
      color: var(--text-muted);
      padding: 3px 8px;
      border-radius: 5px;
      font-size: 10px;
      font-weight: 600;
      cursor: pointer;
      white-space: nowrap;
      display: flex;
      align-items: center;
      gap: 5px;
      transition: all 0.15s ease;
    }}
    .node-pill:hover {{
      color: var(--text);
      border-color: #38bdf8;
      background: var(--panel-hover);
    }}
    .node-pill.active {{
      background: #0369a1;
      border-color: var(--accent-cyan);
      color: #fff;
      box-shadow: 0 0 8px rgba(0, 229, 255, 0.3);
    }}
    .node-pill .badge-count {{
      background: rgba(0,0,0,0.3);
      padding: 1px 4px;
      border-radius: 3px;
      font-size: 9px;
      font-family: var(--font-mono);
    }}

    .search-box {{
      display: flex;
      align-items: center;
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 6px;
      padding: 3px 8px;
      gap: 6px;
      margin-left: auto;
    }}
    .search-box svg {{ fill: var(--text-muted); }}
    .search-box input {{
      background: transparent;
      border: none;
      outline: none;
      color: #fff;
      font-size: 11px;
      width: 170px;
    }}

    /* KPI AUDIT RIBBON */
    .kpi-ribbon {{
      background: #0b101c;
      border-bottom: 1px solid var(--border);
      height: 32px;
      padding: 0 16px;
      display: flex;
      align-items: center;
      gap: 16px;
      font-size: 11px;
      z-index: 35;
      flex-shrink: 0;
    }}
    .kpi-scope {{
      font-family: var(--font-mono);
      font-size: 10px;
      color: var(--accent-cyan);
      background: rgba(0, 229, 255, 0.08);
      border: 1px solid rgba(0, 229, 255, 0.25);
      padding: 2px 7px;
      border-radius: 4px;
    }}
    .kpi-item {{
      display: flex;
      align-items: center;
      gap: 5px;
      cursor: pointer;
      padding: 2px 6px;
      border-radius: 4px;
      transition: background 0.15s;
    }}
    .kpi-item:hover {{
      background: rgba(255,255,255,0.06);
    }}
    .kpi-label {{ color: var(--text-muted); }}
    .kpi-value {{
      font-family: var(--font-mono);
      font-weight: 700;
      color: #fff;
    }}
    .kpi-value.good {{ color: var(--accent-green); }}
    .kpi-value.warn {{ color: var(--accent-amber); }}
    .kpi-value.error {{ color: var(--accent-red); }}

    /* WORKSPACE */
    .workspace {{
      flex: 1;
      display: flex;
      position: relative;
      overflow: hidden;
    }}

    /* CANVAS VIEWPORT */
    .canvas-viewport {{
      flex: 1;
      height: 100%;
      position: relative;
      background: radial-gradient(circle at 50% 50%, #101626 0%, #080c14 100%);
      cursor: grab;
      overflow: hidden;
    }}
    .canvas-viewport:active {{ cursor: grabbing; }}

    #gridCanvas {{
      position: absolute;
      top: 0;
      left: 0;
      pointer-events: none;
      z-index: 1;
    }}

    #linksSvg {{
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      pointer-events: none;
      z-index: 2;
    }}

    #islandsLayer {{
      position: absolute;
      top: 0;
      left: 0;
      pointer-events: none;
      z-index: 2;
    }}

    #nodesLayer {{
      position: absolute;
      top: 0;
      left: 0;
      transform-origin: 0 0;
      z-index: 3;
    }}

    /* ROOM NODE CARD */
    .room-node {{
      position: absolute;
      width: 160px;
      min-height: 84px;
      background: rgba(18, 25, 41, 0.94);
      backdrop-filter: blur(8px);
      border: 1px solid rgba(56, 189, 248, 0.3);
      border-radius: 8px;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.6);
      padding: 8px 10px;
      cursor: move;
      transition: border-color 0.15s, box-shadow 0.15s;
      user-select: none;
      overflow: hidden;
    }}
    .room-node:hover {{
      border-color: var(--accent-cyan);
      box-shadow: 0 0 16px rgba(0, 229, 255, 0.25);
    }}
    .room-node.selected {{
      border-color: var(--accent-cyan);
      box-shadow: 0 0 20px rgba(0, 229, 255, 0.5);
      background: rgba(22, 33, 56, 0.98);
    }}
    .room-node.is-entry {{
      border-color: var(--accent-gold);
      box-shadow: 0 0 16px rgba(251, 191, 36, 0.35);
    }}
    .room-node.has-violation {{
      border-color: var(--accent-red);
      box-shadow: 0 0 16px rgba(239, 68, 68, 0.4);
    }}
    .room-node.collision {{
      border: 2px dashed var(--accent-red);
      animation: pulse 1s infinite alternate;
    }}
    @keyframes pulse {{
      from {{ box-shadow: 0 0 6px var(--accent-red); }}
      to {{ box-shadow: 0 0 18px var(--accent-red); }}
    }}

    /* Card Art Banner in Visual Art Mode */
    .node-art-banner {{
      height: 48px;
      margin: -8px -10px 6px -10px;
      background-size: cover;
      background-position: center;
      position: relative;
      border-bottom: 1px solid rgba(255,255,255,0.1);
    }}
    .banner-gradient {{
      position: absolute;
      inset: 0;
      background: linear-gradient(to bottom, rgba(0,0,0,0.1) 0%, rgba(18,25,41,0.92) 100%);
    }}

    .node-header {{
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 4px;
      gap: 4px;
      position: relative;
      z-index: 2;
    }}
    .node-title {{
      font-size: 11px;
      font-weight: 700;
      color: #fff;
      line-height: 1.2;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1;
    }}
    .node-pos {{
      font-family: var(--font-mono);
      font-size: 9px;
      color: var(--text-muted);
      background: rgba(0,0,0,0.3);
      padding: 1px 4px;
      border-radius: 3px;
    }}

    .node-id {{
      font-family: var(--font-mono);
      font-size: 9px;
      color: var(--accent-cyan);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-bottom: 5px;
      position: relative;
      z-index: 2;
    }}

    .node-badges {{
      display: flex;
      flex-wrap: wrap;
      gap: 3px;
      font-size: 9px;
      position: relative;
      z-index: 2;
    }}
    .badge {{
      background: rgba(255,255,255,0.06);
      padding: 1px 4px;
      border-radius: 3px;
      font-size: 9px;
    }}
    .badge.entry {{ color: var(--accent-gold); border: 1px solid rgba(251, 191, 36, 0.4); font-weight: 700; }}
    .badge.combat {{ color: #f87171; border: 1px solid rgba(239, 68, 68, 0.3); }}
    .badge.action {{ color: #38bdf8; border: 1px solid rgba(56, 189, 248, 0.3); }}
    .badge.item {{ color: #34d399; border: 1px solid rgba(16, 185, 129, 0.3); }}
    .badge.npc {{ color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.3); }}

    /* CARDINAL CONNECTION PINS */
    .port-pin {{
      position: absolute;
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: #334155;
      border: 1px solid #64748b;
      cursor: crosshair;
      transition: all 0.15s;
      z-index: 10;
    }}
    .port-pin:hover {{
      background: var(--accent-cyan);
      box-shadow: 0 0 8px var(--accent-cyan);
      transform: scale(1.3);
    }}
    .port-pin.connected {{ background: var(--accent-green); border-color: #34d399; }}
    .port-pin.pin-n {{ top: -5px; left: calc(50% - 5px); }}
    .port-pin.pin-s {{ bottom: -5px; left: calc(50% - 5px); }}
    .port-pin.pin-e {{ right: -5px; top: calc(50% - 5px); }}
    .port-pin.pin-w {{ left: -5px; top: calc(50% - 5px); }}

    /* FLOATING CANVAS HUD */
    .canvas-hud {{
      position: absolute;
      bottom: 16px;
      left: 16px;
      display: flex;
      gap: 6px;
      z-index: 30;
      background: rgba(17, 23, 38, 0.88);
      backdrop-filter: blur(8px);
      padding: 6px;
      border-radius: 8px;
      border: 1px solid var(--border);
    }}

    /* HUB SCREEN VIEWPORT */
    .hub-screen-container {{
      flex: 1;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: radial-gradient(circle at 50% 50%, #131d33 0%, #060912 100%);
      position: relative;
      overflow: hidden;
    }}

    .hub-device-frame {{
      height: calc(100vh - 180px);
      aspect-ratio: 9 / 16;
      background: #000;
      border-radius: 24px;
      border: 3px solid #23304a;
      box-shadow: 0 0 40px rgba(0,0,0,0.8), 0 0 20px rgba(0, 229, 255, 0.15);
      position: relative;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }}

    .hub-bg-canvas {{
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
      pointer-events: none;
    }}

    .hub-top-scrim {{
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 110px;
      background: linear-gradient(to bottom, rgba(0,0,0,0.8) 0%, rgba(0,0,0,0.4) 60%, transparent 100%);
      pointer-events: none;
      z-index: 2;
    }}
    .hub-bottom-scrim {{
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      height: 120px;
      background: linear-gradient(to top, rgba(0,0,0,0.85) 0%, transparent 100%);
      pointer-events: none;
      z-index: 2;
    }}

    .hub-screen-header {{
      position: absolute;
      top: 18px;
      left: 18px;
      right: 18px;
      z-index: 3;
    }}
    .hub-screen-title {{
      font-size: 16px;
      font-weight: 800;
      color: #fff;
      letter-spacing: 0.5px;
      text-shadow: 0 2px 8px rgba(0,0,0,0.8);
    }}
    .hub-screen-desc {{
      font-size: 10px;
      color: #cbd5e1;
      margin-top: 3px;
      line-height: 1.3;
      text-shadow: 0 1px 4px rgba(0,0,0,0.9);
    }}

    .hub-sites-layer {{
      position: absolute;
      inset: 0;
      z-index: 4;
    }}

    .hub-site-anchor {{
      position: absolute;
      transform: translate(-50%, -50%);
      display: flex;
      flex-direction: column;
      align-items: center;
      cursor: grab;
      user-select: none;
      transition: transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1);
    }}
    .hub-site-anchor:hover {{
      transform: translate(-50%, -50%) scale(1.12);
      z-index: 15;
    }}
    .hub-site-anchor.selected {{
      transform: translate(-50%, -50%) scale(1.18);
      z-index: 18;
    }}
    .hub-site-anchor.dragging {{
      cursor: grabbing;
      transform: translate(-50%, -50%) scale(1.22);
      z-index: 25 !important;
      transition: none;
    }}

    .anchor-coord-tooltip {{
      position: absolute;
      bottom: -22px;
      background: rgba(3, 105, 161, 0.95);
      border: 1px solid var(--accent-cyan);
      color: #fff;
      font-family: var(--font-mono);
      font-size: 9px;
      padding: 1px 6px;
      border-radius: 4px;
      white-space: nowrap;
      pointer-events: none;
      box-shadow: 0 2px 8px rgba(0,0,0,0.7);
    }}

    /* Crosshairs on device frame during hover/drag */
    .hub-crosshair-x {{
      position: absolute;
      top: 0;
      bottom: 0;
      width: 1px;
      border-left: 1px dashed rgba(0, 229, 255, 0.4);
      pointer-events: none;
      z-index: 5;
      display: none;
    }}
    .hub-crosshair-y {{
      position: absolute;
      left: 0;
      right: 0;
      height: 1px;
      border-top: 1px dashed rgba(0, 229, 255, 0.4);
      pointer-events: none;
      z-index: 5;
      display: none;
    }}

    .hub-site-pin {{
      width: 58px;
      height: 58px;
      border-radius: 50%;
      background: rgba(15, 23, 42, 0.9);
      border: 2px solid var(--accent-cyan);
      box-shadow: 0 4px 16px rgba(0,0,0,0.7), 0 0 14px rgba(0, 229, 255, 0.35);
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
    }}
    .hub-site-anchor.selected .hub-site-pin {{
      border-color: #fff;
      box-shadow: 0 0 22px var(--accent-cyan);
    }}
    .hub-site-pin img {{
      width: 100%;
      height: 100%;
      object-fit: cover;
    }}

    .hub-site-badge {{
      background: rgba(15, 23, 42, 0.92);
      border: 1px solid var(--border);
      color: #fff;
      padding: 3px 8px;
      border-radius: 12px;
      font-size: 10px;
      font-weight: 700;
      margin-top: 4px;
      white-space: nowrap;
      box-shadow: 0 2px 8px rgba(0,0,0,0.6);
      display: flex;
      align-items: center;
      gap: 5px;
    }}
    .hub-site-anchor.selected .hub-site-badge {{
      border-color: var(--accent-cyan);
      color: var(--accent-cyan);
    }}

    /* Astra Dock special styling */
    .hub-site-anchor.astra-dock .hub-site-pin {{
      border-color: #38bdf8;
      box-shadow: 0 0 18px rgba(56, 189, 248, 0.6);
      background: radial-gradient(circle, #0369a1 0%, #0b1329 100%);
    }}
    .hub-site-anchor.astra-dock .hub-site-badge {{
      border-color: #38bdf8;
      color: #7dd3fc;
      background: rgba(12, 74, 110, 0.95);
    }}

    /* RIGHT INSPECTOR DRAWER */
    .inspector-drawer {{
      width: 360px;
      height: 100%;
      background: var(--panel);
      border-left: 1px solid var(--border);
      display: flex;
      flex-direction: column;
      z-index: 40;
      flex-shrink: 0;
      transition: transform 0.2s ease;
    }}
    .inspector-drawer.collapsed {{
      transform: translateX(100%);
      margin-left: -360px;
    }}

    .drawer-header {{
      padding: 12px 16px;
      border-bottom: 1px solid var(--border);
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: rgba(0,0,0,0.2);
    }}
    .drawer-title {{
      font-size: 13px;
      font-weight: 700;
      color: #fff;
      display: flex;
      align-items: center;
      gap: 6px;
    }}

    .drawer-content {{
      flex: 1;
      overflow-y: auto;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }}

    /* Room Background Art Preview Card */
    .room-art-card {{
      width: 100%;
      height: 130px;
      border-radius: 8px;
      border: 1px solid var(--border);
      position: relative;
      overflow: hidden;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(0,0,0,0.5);
    }}
    .room-art-card img {{
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s ease;
    }}
    .room-art-card:hover img {{
      transform: scale(1.05);
    }}
    .room-art-scrim {{
      position: absolute;
      inset: 0;
      background: linear-gradient(to top, rgba(0,0,0,0.85) 0%, transparent 60%);
      display: flex;
      align-items: flex-end;
      padding: 8px 10px;
      justify-content: space-between;
    }}
    .room-art-tags {{
      display: flex;
      gap: 4px;
      font-size: 9px;
    }}

    .form-group {{
      display: flex;
      flex-direction: column;
      gap: 5px;
    }}
    .form-label {{
      font-size: 11px;
      font-weight: 600;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }}
    .form-input, .form-textarea, .form-select {{
      background: var(--panel-elevated);
      border: 1px solid var(--border);
      color: #fff;
      padding: 7px 10px;
      border-radius: 6px;
      font-size: 12px;
      outline: none;
      width: 100%;
      font-family: inherit;
    }}
    .form-input:focus, .form-textarea:focus, .form-select:focus {{
      border-color: var(--border-focus);
      box-shadow: 0 0 8px rgba(0, 229, 255, 0.2);
    }}
    .form-textarea {{
      resize: vertical;
      min-height: 60px;
      line-height: 1.4;
    }}

    input[type=range].form-range {{
      -webkit-appearance: none;
      appearance: none;
      width: 100%;
      height: 6px;
      background: #1e293b;
      border-radius: 3px;
      outline: none;
      cursor: pointer;
    }}
    input[type=range].form-range::-webkit-slider-thumb {{
      -webkit-appearance: none;
      appearance: none;
      width: 16px;
      height: 16px;
      border-radius: 50%;
      background: var(--accent-cyan);
      cursor: pointer;
      box-shadow: 0 0 8px var(--accent-cyan);
      border: 2px solid #fff;
    }}

    .coords-row {{
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
    }}

    .connection-grid {{
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
    }}
    .conn-card {{
      background: var(--panel-elevated);
      border: 1px solid var(--border);
      padding: 8px;
      border-radius: 6px;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }}
    .conn-card-header {{
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 11px;
      font-weight: 700;
    }}
    .conn-status {{
      width: 6px;
      height: 6px;
      border-radius: 50%;
    }}
    .conn-status.reciprocal {{ background: var(--accent-green); box-shadow: 0 0 6px var(--accent-green); }}
    .conn-status.asymmetric {{ background: var(--accent-amber); box-shadow: 0 0 6px var(--accent-amber); }}
    .conn-status.none {{ background: #475569; }}

    .mold-breaker-alert {{
      background: rgba(239, 68, 68, 0.12);
      border: 1px solid rgba(239, 68, 68, 0.35);
      border-radius: 6px;
      padding: 8px;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }}
    .mold-breaker-title {{
      color: #f87171;
      font-weight: 700;
      font-size: 11px;
    }}
    .mold-breaker-desc {{
      color: var(--text);
      font-size: 11px;
      line-height: 1.3;
    }}

    /* AUDIT TRAY */
    .audit-tray {{
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      max-height: 280px;
      background: var(--panel);
      border-top: 1px solid var(--border);
      z-index: 45;
      display: flex;
      flex-direction: column;
      transform: translateY(100%);
      transition: transform 0.25s ease;
      box-shadow: 0 -8px 24px rgba(0,0,0,0.7);
    }}
    .audit-tray.open {{
      transform: translateY(0);
    }}
    .audit-tray-header {{
      padding: 8px 16px;
      background: rgba(0,0,0,0.3);
      border-bottom: 1px solid var(--border);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }}
    .audit-tray-content {{
      padding: 12px 16px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }}
    .audit-row {{
      background: var(--panel-elevated);
      border: 1px solid var(--border);
      padding: 8px 12px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 11px;
      cursor: pointer;
    }}
    .audit-row:hover {{
      border-color: var(--border-focus);
      background: var(--panel-hover);
    }}

    /* LIGHTBOX MODAL */
    .lightbox-modal {{
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.88);
      backdrop-filter: blur(8px);
      z-index: 200;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }}
    .lightbox-modal img {{
      max-width: 90vw;
      max-height: 80vh;
      border-radius: 12px;
      box-shadow: 0 0 40px rgba(0, 229, 255, 0.3);
      border: 1px solid var(--border-focus);
    }}
    .lightbox-caption {{
      margin-top: 12px;
      color: #fff;
      font-size: 13px;
      font-family: var(--font-mono);
    }}

    /* FLOATING TOAST */
    #toast {{
      position: fixed;
      bottom: 24px;
      right: 24px;
      background: #0284c7;
      color: #fff;
      padding: 10px 18px;
      border-radius: 8px;
      font-size: 12px;
      box-shadow: 0 8px 24px rgba(0,0,0,0.6);
      display: none;
      z-index: 100;
      animation: slide-in 0.2s ease;
    }}
    @keyframes slide-in {{
      from {{ transform: translateX(50px); opacity: 0; }}
      to {{ transform: translateX(0); opacity: 1; }}
    }}
  </style>
</head>
<body>

  <!-- TOP HEADER -->
  <header>
    <div class="brand">
      <h1>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--accent-cyan)" stroke-width="2">
          <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
          <polyline points="2 17 12 22 22 17"></polyline>
          <polyline points="2 12 12 17 22 12"></polyline>
        </svg>
        STARBORN
      </h1>
      <span class="tag">WORLD GRAPH &amp; NAVIGATION STUDIO</span>
      <div class="server-status">
        <div id="statusDot" class="status-dot"></div>
        <span id="statusText">Connecting...</span>
        <button id="btnReconnect" class="btn" style="padding: 2px 6px; font-size: 9px; display: none;">Reconnect</button>
      </div>
    </div>

    <!-- PRIMARY VIEW MODE SWITCHER -->
    <div class="view-mode-tabs">
      <button class="view-mode-btn active" id="btnModeRoomGrid">🗺️ Room Grid Map</button>
      <button class="view-mode-btn" id="btnModeHubScreen">🪐 Hub Screen Layout</button>
    </div>

    <div class="header-actions">
      <label class="toggle-pill" title="Automatically wires opposite cardinal exit on target room">
        <input type="checkbox" id="autoReciprocalCheck" checked>
        Auto-Wire Reciprocal Exits (NSEW)
      </label>
      <label class="toggle-pill" title="Snap dragged rooms to integer coordinates">
        <input type="checkbox" id="gridSnapCheck" checked>
        Snap to Grid
      </label>
      <button class="btn" id="btnUndo" title="Undo (Ctrl+Z)">↺ Undo</button>
      <button class="btn" id="btnExport" title="Download updated rooms.json">⬇ Export JSON</button>
      <button class="btn btn-save" id="btnSave" title="Save to Project (Ctrl+S)">💾 Save to Project</button>
    </div>
  </header>

  <!-- HIERARCHY FILTER BAR (World -> Hub -> Node) -->
  <div class="filter-bar">
    <!-- ROW 1: WORLD TABS -->
    <div class="filter-row-top">
      <div class="world-tabs" id="worldTabs">
        <div class="world-tab active" data-world="world_1">World 1: Mines</div>
        <div class="world-tab" data-world="world_2">World 2: Sector 9</div>
        <div class="world-tab" data-world="world_3">World 3: Spire</div>
        <div class="world-tab" data-world="world_4">World 4: Foundry</div>
        <div class="world-tab" data-world="world_5">World 5: Orbital</div>
        <div class="world-tab" data-world="world_6">World 6: Tear</div>
        <div class="world-tab" data-world="world_astra">Astra Station</div>
        <div class="world-tab" data-world="all">All Worlds</div>
      </div>

      <div class="search-box">
        <svg width="13" height="13" viewBox="0 0 24 24"><path d="M10 18a7.952 7.952 0 0 0 4.897-1.688l4.396 4.396 1.414-1.414-4.396-4.396A7.952 7.952 0 0 0 18 10c0-4.411-3.589-8-8-8s-8 3.589-8 8 3.589 8 8 8zm0-14c3.309 0 6 2.691 6 6s-2.691 6-6 6-6-2.691-6-6 2.691-6 6-6z"/></svg>
        <input type="text" id="searchInput" placeholder="Search room title or ID...">
      </div>
    </div>

    <!-- ROW 2: HUB SELECTOR & NODE PILLS -->
    <div class="filter-row-sub">
      <div class="select-group">
        <span>Hub:</span>
        <select class="studio-select" id="hubFilterSelect"></select>
      </div>

      <div class="select-group" id="nodeFilterGroup">
        <span>Node:</span>
        <select class="studio-select" id="nodeFilterSelect"></select>
      </div>

      <!-- Quick Node Switcher Pills -->
      <div class="node-pills" id="nodePills"></div>
    </div>
  </div>

  <!-- KPI AUDIT RIBBON -->
  <div class="kpi-ribbon">
    <div class="kpi-scope" id="kpiScope">Scope: World 1 &gt; Homestead &gt; The Pit</div>
    <div class="kpi-item" id="kpiTotalRoomsBtn">
      <span class="kpi-label">Rooms:</span>
      <span class="kpi-value" id="kpiTotalRooms">0</span>
    </div>
    <div class="kpi-item" id="kpiComplianceBtn" title="Click to view audit issues">
      <span class="kpi-label">Strict NSEW Compliance:</span>
      <span class="kpi-value good" id="kpiCompliance">100%</span>
    </div>
    <div class="kpi-item" id="kpiViolationsBtn" title="Click to view diagonal & non-NSEW exits">
      <span class="kpi-label">Diagonal Violations:</span>
      <span class="kpi-value warn" id="kpiViolations">0</span>
    </div>
    <div class="kpi-item" id="kpiAsymmetricBtn" title="Click to view asymmetric one-way drops">
      <span class="kpi-label">Asymmetric Exits:</span>
      <span class="kpi-value warn" id="kpiAsymmetric">0</span>
    </div>
    <div class="kpi-item" id="kpiCollisionsBtn" title="Rooms occupying the exact same [x,y] inside this node">
      <span class="kpi-label">In-Node Collisions:</span>
      <span class="kpi-value good" id="kpiCollisions">0</span>
    </div>
    <div class="kpi-item" id="kpiOrphansBtn" title="Rooms with 0 incoming links">
      <span class="kpi-label">Orphans:</span>
      <span class="kpi-value" id="kpiOrphans">0</span>
    </div>
    <div style="margin-left: auto;">
      <button class="btn" id="btnToggleAuditTray" style="padding: 2px 8px; font-size: 10px;">📋 Audit Drawer</button>
    </div>
  </div>

  <!-- MAIN WORKSPACE -->
  <div class="workspace">
    <!-- VIEW A: CANVAS VIEWPORT (Node Room Grid) -->
    <div class="canvas-viewport" id="canvasViewport">
      <canvas id="gridCanvas"></canvas>
      <svg id="linksSvg"></svg>
      <div id="islandsLayer"></div>
      <div id="nodesLayer"></div>

      <!-- FLOATING CANVAS HUD -->
      <div class="canvas-hud">
        <button class="btn" id="btnZoomIn" title="Zoom In (+)">➕</button>
        <button class="btn" id="btnZoomOut" title="Zoom Out (-)">➖</button>
        <button class="btn" id="btnResetView" title="Center &amp; Fit View">🎯 Fit</button>
        <button class="btn" id="btnToggleArtMode" title="Toggle Room Artwork Cards">🖼️ Art Mode: ON</button>
        <button class="btn" id="btnToggleLabels" title="Toggle Detail Badges">🏷️ Details</button>
        <button class="btn btn-primary" id="btnCreateRoom" title="Add New Room to Node">➕ Add Room</button>
      </div>
    </div>

    <!-- VIEW B: HUB SCREEN LAYOUT (Interactive Mobile Hub Scene) -->
    <div class="hub-screen-container" id="hubScreenContainer" style="display: none;">
      <div class="hub-device-frame" id="hubDeviceFrame">
        <img id="hubBgImage" class="hub-bg-canvas" src="" alt="Hub Background">
        <div class="hub-top-scrim"></div>
        <div class="hub-bottom-scrim"></div>
        <div class="hub-screen-header">
          <div class="hub-screen-title" id="hubScreenTitle">Homestead Quarter</div>
          <div class="hub-screen-desc" id="hubScreenDesc">A dense cluster of sleeping pods and industrial workshops.</div>
        </div>
        <div id="hubSitesLayer" class="hub-sites-layer"></div>
        <div id="hubCrosshairX" class="hub-crosshair-x"></div>
        <div id="hubCrosshairY" class="hub-crosshair-y"></div>
      </div>
    </div>

    <!-- RIGHT INSPECTOR DRAWER -->
    <div class="inspector-drawer collapsed" id="inspectorDrawer">
      <div class="drawer-header">
        <div class="drawer-title">
          <span>Inspector</span>
        </div>
        <button class="btn" id="btnCloseDrawer" style="padding: 2px 6px;">✕</button>
      </div>
      <div class="drawer-content" id="drawerContent">
        <!-- Dynamically populated -->
      </div>
    </div>

    <!-- BOTTOM AUDIT TRAY -->
    <div class="audit-tray" id="auditTray">
      <div class="audit-tray-header">
        <h3 style="font-size: 12px; font-weight: 700; color: #fff;">Navigation Integrity &amp; Strict NSEW Diagnostics</h3>
        <button class="btn" style="padding: 2px 6px;" id="btnCloseAuditTray">✕</button>
      </div>
      <div class="audit-tray-content" id="auditTrayContent">
        <!-- Dynamically populated -->
      </div>
    </div>
  </div>

  <!-- FLOATING IMAGE LIGHTBOX -->
  <div id="imageLightbox" class="lightbox-modal" style="display: none;" onclick="this.style.display='none'">
    <img id="lightboxImg" src="" alt="Room Artwork">
    <div id="lightboxCaption" class="lightbox-caption"></div>
  </div>

  <!-- FLOATING TOAST -->
  <div id="toast"></div>

  <!-- BUNDLED OFFLINE DATASET -->
  <script id="bundledData" type="application/json">
{bundled_json_str}
  </script>

  <!-- CORE STUDIO LOGIC SCRIPT -->
  <script>
    // Global Studio State
    const state = {{
      rooms: [],
      hubNodes: [],
      hubs: [],
      worlds: [],
      hubLayouts: {{}},
      roomMap: new Map(),
      nodeMap: new Map(),
      hubMap: new Map(),
      activeWorld: 'world_1',
      activeHub: 'hub_1_homestead',
      activeNode: 'pit',
      activeViewMode: 'room_grid', // 'room_grid' | 'hub_screen'
      selectedRoomId: null,
      selectedHubNodeId: null,
      zoom: 1.0,
      panX: 400,
      panY: 300,
      isPanning: false,
      panStartX: 0,
      panStartY: 0,
      draggedRoomId: null,
      dragOffsetX: 0,
      dragOffsetY: 0,
      isDraggingNode: false,
      isDraggingHubNode: false,
      draggedHubNodeId: null,
      connectingFrom: null,
      history: [],
      gridSpacing: 180,
      isServerLive: false,
      showArt: true,
      showDetails: true
    }};

    const oppositeDirections = {{
      north: 'south',
      south: 'north',
      east: 'west',
      west: 'east',
      up: 'down',
      down: 'up',
      northeast: 'southwest',
      southwest: 'northeast',
      northwest: 'southeast',
      southeast: 'northwest'
    }};

    // DOM Elements
    const canvasViewport = document.getElementById('canvasViewport');
    const hubScreenContainer = document.getElementById('hubScreenContainer');
    const nodesLayer = document.getElementById('nodesLayer');
    const islandsLayer = document.getElementById('islandsLayer');
    const linksSvg = document.getElementById('linksSvg');
    const gridCanvas = document.getElementById('gridCanvas');
    const ctx = gridCanvas.getContext('2d');
    const toast = document.getElementById('toast');
    const inspectorDrawer = document.getElementById('inspectorDrawer');
    const drawerContent = document.getElementById('drawerContent');
    const auditTray = document.getElementById('auditTray');
    const auditTrayContent = document.getElementById('auditTrayContent');

    function showToast(msg, duration = 3000) {{
      toast.textContent = msg;
      toast.style.display = 'block';
      setTimeout(() => {{ toast.style.display = 'none'; }}, duration);
    }}

    function getImageUrl(imgPath) {{
      if (!imgPath) return '';
      const clean = imgPath.replace(/^\/+/, '');
      if (state.isServerLive || window.location.protocol === 'http:' || window.location.protocol === 'https:') {{
        return '/' + clean;
      }}
      return '../world_assets/src/main/assets/' + clean;
    }}

    function openLightbox(url, title) {{
      const lb = document.getElementById('imageLightbox');
      const img = document.getElementById('lightboxImg');
      const cap = document.getElementById('lightboxCaption');
      img.src = url;
      cap.textContent = title || url;
      lb.style.display = 'flex';
    }}

    // Initialization
    async function init() {{
      resizeCanvas();
      window.addEventListener('resize', resizeCanvas);
      setupCanvasEvents();
      setupToolbarEvents();

      // Load bundled data first
      try {{
        const bundledText = document.getElementById('bundledData').textContent;
        const bundled = JSON.parse(bundledText);
        state.rooms = bundled.rooms || [];
        state.hubNodes = bundled.hubNodes || bundled.hub_nodes || [];
        state.hubs = bundled.hubs || [];
        state.worlds = bundled.worlds || [];
        state.hubLayouts = bundled.hub_layouts || bundled.hubLayouts || {{}};
      }} catch (e) {{
        console.error('Failed to parse bundled data:', e);
      }}

      // Check if local python server is live
      try {{
        const res = await fetch('/api/data', {{ cache: 'no-store' }});
        if (res.ok) {{
          const data = await res.json();
          state.rooms = data.rooms || state.rooms;
          state.hubNodes = data.hubNodes || data.hub_nodes || state.hubNodes;
          state.hubs = data.hubs || state.hubs;
          state.worlds = data.worlds || state.worlds;
          state.hubLayouts = data.hub_layouts || state.hubLayouts;
          state.isServerLive = true;
          document.getElementById('statusDot').classList.remove('offline');
          document.getElementById('statusText').textContent = 'Server Live: ' + state.rooms.length + ' rooms (Direct Save Active)';
          document.getElementById('btnReconnect').style.display = 'none';
        }} else {{
          throw new Error('Server returned ' + res.status);
        }}
      }} catch (err) {{
        state.isServerLive = false;
        document.getElementById('statusDot').classList.add('offline');
        document.getElementById('statusText').textContent = 'Offline File Mode (' + state.rooms.length + ' rooms loaded)';
        document.getElementById('btnReconnect').style.display = 'inline-flex';
      }}

      indexData();
      populateHubSelector();
      fitView();
      render();
    }}

    function indexData() {{
      state.roomMap.clear();
      state.rooms.forEach(r => state.roomMap.set(r.id, r));

      state.nodeMap.clear();
      state.hubNodes.forEach(n => state.nodeMap.set(n.id, n));

      state.hubMap.clear();
      state.hubs.forEach(h => state.hubMap.set(h.id, h));
    }}

    function pushHistory() {{
      state.history.push(JSON.stringify({{
        rooms: state.rooms,
        hubNodes: state.hubNodes,
        hubLayouts: state.hubLayouts
      }}));
      if (state.history.length > 30) state.history.shift();
    }}

    function undo() {{
      if (state.history.length > 0) {{
        const snap = JSON.parse(state.history.pop());
        state.rooms = snap.rooms || state.rooms;
        state.hubNodes = snap.hubNodes || state.hubNodes;
        state.hubLayouts = snap.hubLayouts || state.hubLayouts;
        indexData();
        if (state.activeViewMode === 'hub_screen') {{
          renderHubScreen();
          if (state.selectedHubNodeId) selectHubNode(state.selectedHubNodeId);
        }} else {{
          render();
        }}
        showToast('Undo executed.');
      }}
    }}

    // Hierarchy Lookups
    function getRoomNode(room) {{
      for (const node of state.hubNodes) {{
        if (node.rooms && node.rooms.includes(room.id)) return node;
        if (node.entry_room === room.id) return node;
      }}
      return null;
    }}

    function getRoomWorldId(room) {{
      const node = getRoomNode(room);
      if (node && node.world_id) return node.world_id;
      if (room.id.startsWith('pit_') || room.id.startsWith('trade_') || room.id.startsWith('medbay_') || room.id.startsWith('workshop_') || room.id.startsWith('checkpoint_') || room.id.startsWith('admin_') || room.id.startsWith('server_') || room.id.startsWith('mine_') || room.id.startsWith('echo_') || room.id.startsWith('launch_')) return 'world_1';
      if (room.id.startsWith('sector9_')) return 'world_2';
      if (room.id.startsWith('spire_')) return 'world_3';
      if (room.id.startsWith('foundry_')) return 'world_4';
      if (room.id.startsWith('orbital_') || room.id.startsWith('deep_')) return 'world_5';
      if (room.id.startsWith('source_')) return 'world_6';
      if (room.id.startsWith('astra_')) return 'world_astra';
      return 'world_1';
    }}

    function getRoomHubId(room) {{
      const node = getRoomNode(room);
      if (node && node.hub_id) return node.hub_id;
      return 'unknown';
    }}

    function getNodeOffset(nodeId) {{
      if (!nodeId || nodeId === 'all_nodes') return {{ x: 0, y: 0 }};
      const node = state.nodeMap.get(nodeId);
      if (!node) return {{ x: 0, y: 0 }};

      const hubNodes = state.hubNodes.filter(n => n.hub_id === node.hub_id);
      const index = hubNodes.findIndex(n => n.id === nodeId);
      if (index === -1) return {{ x: 0, y: 0 }};

      const cols = 3;
      const col = index % cols;
      const row = Math.floor(index / cols);
      return {{
        x: col * 12,
        y: -row * 12
      }};
    }}

    function getVisibleRooms() {{
      const q = document.getElementById('searchInput').value.trim().toLowerCase();

      if (q) {{
        return state.rooms.filter(r => {{
          const matchTitle = (r.title || '').toLowerCase().includes(q);
          const matchId = r.id.toLowerCase().includes(q);
          return matchTitle || matchId;
        }});
      }}

      if (state.activeNode !== 'all_nodes') {{
        const node = state.nodeMap.get(state.activeNode);
        if (node && node.rooms) {{
          const roomSet = new Set(node.rooms);
          if (node.entry_room) roomSet.add(node.entry_room);
          return state.rooms.filter(r => roomSet.has(r.id));
        }}
      }}

      return state.rooms.filter(r => {{
        const wId = getRoomWorldId(r);
        if (state.activeWorld !== 'all' && wId !== state.activeWorld) return false;
        if (state.activeHub !== 'all' && getRoomHubId(r) !== state.activeHub) return false;
        return true;
      }});
    }}

    // UI Population
    function populateHubSelector() {{
      const hubSelect = document.getElementById('hubFilterSelect');
      hubSelect.innerHTML = '';

      const relevantHubs = state.hubs.filter(h => state.activeWorld === 'all' || h.world_id === state.activeWorld);
      if (relevantHubs.length === 0) {{
        hubSelect.innerHTML = '<option value="all">All Hubs</option>';
        state.activeHub = 'all';
      }} else {{
        relevantHubs.forEach(h => {{
          const opt = document.createElement('option');
          opt.value = h.id;
          opt.textContent = h.title || h.id;
          hubSelect.appendChild(opt);
        }});
        if (!relevantHubs.some(h => h.id === state.activeHub)) {{
          state.activeHub = relevantHubs[0].id;
        }}
        hubSelect.value = state.activeHub;
      }}

      populateNodeSelector();
    }}

    function populateNodeSelector() {{
      const nodeSelect = document.getElementById('nodeFilterSelect');
      const pillsContainer = document.getElementById('nodePills');
      nodeSelect.innerHTML = '';
      pillsContainer.innerHTML = '';

      const relevantNodes = state.hubNodes.filter(n => {{
        if (state.activeWorld !== 'all' && n.world_id !== state.activeWorld) return false;
        if (state.activeHub !== 'all' && n.hub_id !== state.activeHub) return false;
        return true;
      }});

      const optAll = document.createElement('option');
      optAll.value = 'all_nodes';
      optAll.textContent = '🌐 All Nodes in Hub (Dispersed Islands)';
      nodeSelect.appendChild(optAll);

      const pillAll = document.createElement('div');
      pillAll.className = 'node-pill' + (state.activeNode === 'all_nodes' ? ' active' : '');
      pillAll.innerHTML = `<span>🌐 All Nodes</span>`;
      pillAll.addEventListener('click', () => selectNode('all_nodes'));
      pillsContainer.appendChild(pillAll);

      relevantNodes.forEach(node => {{
        const opt = document.createElement('option');
        opt.value = node.id;
        const roomCount = (node.rooms || []).length;
        opt.textContent = `${{node.title || node.id}} (${{roomCount}} rooms)`;
        nodeSelect.appendChild(opt);

        const pill = document.createElement('div');
        pill.className = 'node-pill' + (state.activeNode === node.id ? ' active' : '');
        pill.innerHTML = `
          <span>${{escapeHtml(node.title || node.id)}}</span>
          <span class="badge-count">${{roomCount}}</span>
        `;
        pill.addEventListener('click', () => selectNode(node.id));
        pillsContainer.appendChild(pill);
      }});

      if (relevantNodes.length > 0 && state.activeNode !== 'all_nodes') {{
        if (!relevantNodes.some(n => n.id === state.activeNode)) {{
          state.activeNode = relevantNodes[0].id;
        }}
      }} else if (relevantNodes.length === 0) {{
        state.activeNode = 'all_nodes';
      }}

      nodeSelect.value = state.activeNode;
    }}

    function selectNode(nodeId) {{
      state.activeNode = nodeId;
      const nodeSelect = document.getElementById('nodeFilterSelect');
      if (nodeSelect) nodeSelect.value = nodeId;

      populateNodeSelector();
      if (state.activeViewMode === 'hub_screen') {{
        switchViewMode('room_grid');
      }}
      fitView();
      render();
    }}

    function switchViewMode(mode) {{
      state.activeViewMode = mode;
      document.getElementById('btnModeRoomGrid').classList.toggle('active', mode === 'room_grid');
      document.getElementById('btnModeHubScreen').classList.toggle('active', mode === 'hub_screen');

      if (mode === 'room_grid') {{
        canvasViewport.style.display = 'block';
        hubScreenContainer.style.display = 'none';
        document.getElementById('nodeFilterGroup').style.display = 'flex';
        document.getElementById('nodePills').style.display = 'flex';
        fitView();
        render();
      }} else {{
        canvasViewport.style.display = 'none';
        hubScreenContainer.style.display = 'flex';
        document.getElementById('nodeFilterGroup').style.display = 'none';
        document.getElementById('nodePills').style.display = 'none';
        renderHubScreen();
      }}
    }}

    // Render Mobile Hub Screen Scene
    function renderHubScreen() {{
      const hub = state.hubMap.get(state.activeHub);
      if (!hub) return;

      document.getElementById('hubScreenTitle').textContent = hub.title || hub.id;
      document.getElementById('hubScreenDesc').textContent = hub.description || '';

      const bgImg = document.getElementById('hubBgImage');
      if (hub.background_image) {{
        bgImg.src = getImageUrl(hub.background_image);
      }}

      if (!state.hubLayouts[hub.id]) {{
        state.hubLayouts[hub.id] = {{ sites: {{}}, astra_dock: null }};
      }}
      const layout = state.hubLayouts[hub.id];
      const sitesLayer = document.getElementById('hubSitesLayer');
      sitesLayer.innerHTML = '';

      // Render Nodes
      const hubNodes = state.hubNodes.filter(n => n.hub_id === hub.id);
      hubNodes.forEach(node => {{
        if (!layout.sites[node.id]) {{
          const fallbackX = node.pos_hint ? (node.pos_hint.center_x || 0.5) : 0.5;
          const fallbackY = node.pos_hint ? (node.pos_hint.center_y || 0.5) : 0.5;
          layout.sites[node.id] = {{ x: fallbackX, y: fallbackY, artwork_width: 0.25 }};
        }}
        const site = layout.sites[node.id];

        const anchor = document.createElement('div');
        anchor.className = 'hub-site-anchor' + (state.selectedHubNodeId === node.id ? ' selected' : '');
        anchor.id = 'anchor_' + node.id;
        anchor.style.left = (site.x * 100) + '%';
        anchor.style.top = (site.y * 100) + '%';

        anchor.innerHTML = `
          <div class="hub-site-pin">
            <img src="${{getImageUrl(node.icon_image || 'images/nodes/pit_hub1_v3.webp')}}" onerror="this.src='${{getImageUrl('images/nodes/pit_hub1_v3.webp')}}'" />
          </div>
          <div class="hub-site-badge">
            <span>${{escapeHtml(node.title || node.id)}}</span>
            <span style="color:var(--accent-cyan); font-size:9px;">${{(node.rooms || []).length}}r</span>
          </div>
          <div class="anchor-coord-tooltip">${{Math.round(site.x * 100)}}%, ${{Math.round(site.y * 100)}}%</div>
        `;

        anchor.addEventListener('mousedown', (e) => {{
          if (e.button !== 0) return;
          e.stopPropagation();
          startHubNodeDrag(node.id, e);
        }});

        anchor.addEventListener('click', (e) => {{
          e.stopPropagation();
          selectHubNode(node.id);
        }});

        anchor.addEventListener('dblclick', (e) => {{
          e.stopPropagation();
          selectNode(node.id);
        }});

        sitesLayer.appendChild(anchor);
      }});

      // Render Astra Dock
      if (layout.astra_dock) {{
        const astraSite = layout.astra_dock;
        const astraAnchor = document.createElement('div');
        astraAnchor.className = 'hub-site-anchor astra-dock' + (state.selectedHubNodeId === 'astra_access' ? ' selected' : '');
        astraAnchor.id = 'anchor_astra_access';
        astraAnchor.style.left = (astraSite.x * 100) + '%';
        astraAnchor.style.top = (astraSite.y * 100) + '%';

        astraAnchor.innerHTML = `
          <div class="hub-site-pin">
            <img src="${{getImageUrl('images/nodes/astra_ship_map_v2.webp')}}" />
          </div>
          <div class="hub-site-badge">
            <span style="color:#7dd3fc;">The Astra</span>
            <span style="color:var(--accent-cyan); font-size:9px;">Ship</span>
          </div>
          <div class="anchor-coord-tooltip">${{Math.round(astraSite.x * 100)}}%, ${{Math.round(astraSite.y * 100)}}%</div>
        `;

        astraAnchor.addEventListener('mousedown', (e) => {{
          if (e.button !== 0) return;
          e.stopPropagation();
          startHubNodeDrag('astra_access', e);
        }});

        astraAnchor.addEventListener('click', (e) => {{
          e.stopPropagation();
          selectHubNode('astra_access');
        }});

        sitesLayer.appendChild(astraAnchor);
      }}

      // Scope Label
      document.getElementById('kpiScope').textContent = `Scope: Hub [${{hub.title || hub.id}}] Screen Layout`;
    }}

    // Interactive Hub Node Dragging Logic
    function startHubNodeDrag(nodeId, e) {{
      state.isDraggingHubNode = true;
      state.draggedHubNodeId = nodeId;
      selectHubNode(nodeId);
      pushHistory();

      const anchor = document.getElementById('anchor_' + nodeId);
      if (anchor) anchor.classList.add('dragging');

      const crossX = document.getElementById('hubCrosshairX');
      const crossY = document.getElementById('hubCrosshairY');
      if (crossX) crossX.style.display = 'block';
      if (crossY) crossY.style.display = 'block';
    }}

    function onHubNodeDrag(e) {{
      if (!state.isDraggingHubNode || !state.draggedHubNodeId) return;
      const frame = document.getElementById('hubDeviceFrame');
      if (!frame) return;
      const rect = frame.getBoundingClientRect();

      let normX = (e.clientX - rect.left) / rect.width;
      let normY = (e.clientY - rect.top) / rect.height;

      normX = Math.min(Math.max(normX, 0.05), 0.95);
      normY = Math.min(Math.max(normY, 0.05), 0.95);

      normX = Math.round(normX * 100) / 100;
      normY = Math.round(normY * 100) / 100;

      updateNodePosition(state.draggedHubNodeId, normX, normY);
    }}

    function stopHubNodeDrag() {{
      if (state.isDraggingHubNode) {{
        if (state.draggedHubNodeId) {{
          const anchor = document.getElementById('anchor_' + state.draggedHubNodeId);
          if (anchor) anchor.classList.remove('dragging');
        }}
        state.isDraggingHubNode = false;
        state.draggedHubNodeId = null;

        const crossX = document.getElementById('hubCrosshairX');
        const crossY = document.getElementById('hubCrosshairY');
        if (crossX) crossX.style.display = 'none';
        if (crossY) crossY.style.display = 'none';

        showToast('Updated node placement on Hub screen.');
      }}
    }}

    function updateNodePosition(nodeId, x, y) {{
      const hubId = state.activeHub;
      if (!state.hubLayouts[hubId]) state.hubLayouts[hubId] = {{ sites: {{}}, astra_dock: null }};
      const layout = state.hubLayouts[hubId];

      if (nodeId === 'astra_access') {{
        if (!layout.astra_dock) layout.astra_dock = {{ x: 0.5, y: 0.5, artwork_width: 0.25 }};
        layout.astra_dock.x = x;
        layout.astra_dock.y = y;
      }} else {{
        if (!layout.sites[nodeId]) layout.sites[nodeId] = {{ x: 0.5, y: 0.5, artwork_width: 0.25 }};
        layout.sites[nodeId].x = x;
        layout.sites[nodeId].y = y;

        // Sync in hubNodes
        const node = state.nodeMap.get(nodeId);
        if (node) {{
          if (!node.pos_hint) node.pos_hint = {{}};
          node.pos_hint.center_x = x;
          node.pos_hint.center_y = y;
        }}
      }}

      // Update Anchor Element
      const anchor = document.getElementById('anchor_' + nodeId);
      if (anchor) {{
        anchor.style.left = (x * 100) + '%';
        anchor.style.top = (y * 100) + '%';
        const tooltip = anchor.querySelector('.anchor-coord-tooltip');
        if (tooltip) tooltip.textContent = `${{Math.round(x * 100)}}%, ${{Math.round(y * 100)}}% (${{x.toFixed(2)}}, ${{y.toFixed(2)}})`;
      }}

      // Update crosshairs
      const crossX = document.getElementById('hubCrosshairX');
      const crossY = document.getElementById('hubCrosshairY');
      if (crossX) crossX.style.left = (x * 100) + '%';
      if (crossY) crossY.style.top = (y * 100) + '%';

      // Update Inspector inputs
      updateInspectorSiteInputs(nodeId, x, y);
    }}

    function updateInspectorSiteInputs(nodeId, x, y) {{
      const sxInput = document.getElementById('numSiteX');
      const syInput = document.getElementById('numSiteY');
      const rxSlider = document.getElementById('sliderSiteX');
      const rySlider = document.getElementById('sliderSiteY');
      const lx = document.getElementById('labelSiteX');
      const ly = document.getElementById('labelSiteY');
      const codeInput = document.getElementById('codeKotlinSite');

      if (sxInput) sxInput.value = x.toFixed(2);
      if (syInput) syInput.value = y.toFixed(2);
      if (rxSlider) rxSlider.value = x;
      if (rySlider) rySlider.value = y;
      if (lx) lx.textContent = `${{Math.round(x * 100)}}% (${{x.toFixed(2)}})`;
      if (ly) ly.textContent = `${{Math.round(y * 100)}}% (${{y.toFixed(2)}})`;

      if (codeInput) {{
        if (nodeId === 'astra_access') {{
          codeInput.value = `astraDock = site(${{x.toFixed(2)}}f, ${{y.toFixed(2)}}f)`;
        }} else {{
          codeInput.value = `"${{nodeId}}" to site(${{x.toFixed(2)}}f, ${{y.toFixed(2)}}f)`;
        }}
      }}
    }}

    window.onSiteSliderInput = function(nodeId, axis, val) {{
      const num = parseFloat(val);
      const hubId = state.activeHub;
      const layout = state.hubLayouts[hubId] || {{ sites: {{}}, astra_dock: null }};
      const current = (nodeId === 'astra_access' ? layout.astra_dock : layout.sites[nodeId]) || {{ x: 0.5, y: 0.5 }};

      if (axis === 'x') {{
        updateNodePosition(nodeId, num, current.y);
      }} else if (axis === 'y') {{
        updateNodePosition(nodeId, current.x, num);
      }} else if (axis === 'w') {{
        current.artwork_width = num;
        const lw = document.getElementById('labelSiteW');
        if (lw) lw.textContent = num.toFixed(2);
        const codeInput = document.getElementById('codeKotlinSite');
        if (codeInput) {{
          if (nodeId === 'astra_access') {{
            codeInput.value = `astraDock = site(${{current.x.toFixed(2)}}f, ${{current.y.toFixed(2)}}f, ${{num.toFixed(2)}}f)`;
          }} else {{
            codeInput.value = `"${{nodeId}}" to site(${{current.x.toFixed(2)}}f, ${{current.y.toFixed(2)}}f, ${{num.toFixed(2)}}f)`;
          }}
        }}
      }}
    }};

    window.onSiteNumberInput = function(nodeId, axis, val) {{
      const num = parseFloat(val);
      if (isNaN(num)) return;
      onSiteSliderInput(nodeId, axis, num);
    }};

    window.centerSiteX = function(nodeId) {{
      onSiteSliderInput(nodeId, 'x', 0.50);
    }};

    window.copyKotlinSnippet = function(nodeId) {{
      const codeInput = document.getElementById('codeKotlinSite');
      if (codeInput) {{
        navigator.clipboard.writeText(codeInput.value);
        showToast('Copied: ' + codeInput.value);
      }}
    }};

    function selectHubNode(nodeId) {{
      state.selectedHubNodeId = nodeId;
      inspectorDrawer.classList.remove('collapsed');

      const hub = state.hubMap.get(state.activeHub);
      const layout = (hub && state.hubLayouts[hub.id]) || {{ sites: {{}}, astra_dock: null }};
      const isAstra = (nodeId === 'astra_access');
      const site = isAstra ? (layout.astra_dock || {{ x: 0.5, y: 0.5, artwork_width: 0.25 }}) : (layout.sites[nodeId] || {{ x: 0.5, y: 0.5, artwork_width: 0.25 }});
      const kotlinSnippet = isAstra ? `astraDock = site(${{site.x.toFixed(2)}}f, ${{site.y.toFixed(2)}}f)` : `"${{nodeId}}" to site(${{site.x.toFixed(2)}}f, ${{site.y.toFixed(2)}}f)`;

      if (isAstra) {{
        drawerContent.innerHTML = `
          <div class="room-art-card" onclick="openLightbox('${{getImageUrl('images/nodes/astra_ship_map_v2.webp')}}', 'The Astra')">
            <img src="${{getImageUrl('images/nodes/astra_ship_map_v2.webp')}}" />
            <div class="room-art-scrim">
              <div class="room-art-tags">
                <span class="badge entry">🚀 PLAYER STARSHIP</span>
              </div>
              <span style="font-size:10px; color:#fff;">🔍 Expand</span>
            </div>
          </div>
          <div style="color:#fff; font-size:14px; font-weight:700;">The Astra (Landing Site)</div>
          <div style="font-size:11px; color:var(--text-muted); line-height:1.4;">
            Your personal interstellar corvette and operational mobile headquarters. Drag anywhere on the mobile hub screen or adjust coordinates below.
          </div>

          <div class="form-group" style="background:rgba(0,0,0,0.25); border:1px solid var(--border); padding:10px; border-radius:6px; margin-top:8px;">
            <label class="form-label">Hub Screen Placement Controls</label>
            <div style="display:flex; flex-direction:column; gap:8px; margin-top:4px;">
              <div>
                <div style="display:flex; justify-content:space-between; font-size:11px; margin-bottom:3px;">
                  <span style="color:#cbd5e1; font-weight:600;">Horizontal (X)</span>
                  <span id="labelSiteX" style="font-family:var(--font-mono); color:var(--accent-cyan);">${{Math.round(site.x * 100)}}% (${{site.x.toFixed(2)}})</span>
                </div>
                <input type="range" class="form-range" id="sliderSiteX" min="0.05" max="0.95" step="0.01" value="${{site.x}}" oninput="onSiteSliderInput('astra_access', 'x', this.value)">
                <div style="display:flex; gap:6px; margin-top:4px;">
                  <input type="number" class="form-input" id="numSiteX" min="0.05" max="0.95" step="0.01" value="${{site.x.toFixed(2)}}" onchange="onSiteNumberInput('astra_access', 'x', this.value)">
                  <button class="btn" style="padding:2px 8px; font-size:10px;" onclick="centerSiteX('astra_access')">Center (0.50)</button>
                </div>
              </div>

              <div>
                <div style="display:flex; justify-content:space-between; font-size:11px; margin-bottom:3px;">
                  <span style="color:#cbd5e1; font-weight:600;">Vertical (Y)</span>
                  <span id="labelSiteY" style="font-family:var(--font-mono); color:var(--accent-cyan);">${{Math.round(site.y * 100)}}% (${{site.y.toFixed(2)}})</span>
                </div>
                <input type="range" class="form-range" id="sliderSiteY" min="0.05" max="0.95" step="0.01" value="${{site.y}}" oninput="onSiteSliderInput('astra_access', 'y', this.value)">
                <input type="number" class="form-input" id="numSiteY" min="0.05" max="0.95" step="0.01" value="${{site.y.toFixed(2)}}" style="margin-top:4px;" onchange="onSiteNumberInput('astra_access', 'y', this.value)">
              </div>

              <div>
                <div style="display:flex; justify-content:space-between; font-size:11px; margin-bottom:3px;">
                  <span style="color:#cbd5e1; font-weight:600;">Scale / Width</span>
                  <span id="labelSiteW" style="font-family:var(--font-mono); color:var(--accent-cyan);">${{site.artwork_width || 0.25}}</span>
                </div>
                <input type="range" class="form-range" id="sliderSiteW" min="0.10" max="0.50" step="0.01" value="${{site.artwork_width || 0.25}}" oninput="onSiteSliderInput('astra_access', 'w', this.value)">
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Kotlin Source Code</label>
            <div style="display:flex; gap:6px; align-items:center;">
              <input type="text" class="form-input" id="codeKotlinSite" value="${{kotlinSnippet}}" readonly style="font-family:var(--font-mono); font-size:10px;">
              <button class="btn" onclick="copyKotlinSnippet('astra_access')">📋 Copy</button>
            </div>
          </div>

          <button class="btn btn-primary" style="margin-top:10px;" onclick="enterAstraNode()">
            🚀 Board The Astra (Common Room)
          </button>
        `;
        return;
      }}

      const node = state.nodeMap.get(nodeId);
      if (!node) return;

      const roomCount = (node.rooms || []).length;
      drawerContent.innerHTML = `
        <div class="room-art-card" onclick="openLightbox('${{getImageUrl(node.icon_image || '')}}', '${{escapeHtml(node.title)}}')">
          <img src="${{getImageUrl(node.icon_image || '')}}" onerror="this.src='${{getImageUrl('images/nodes/pit_hub1_v3.webp')}}'" />
          <div class="room-art-scrim">
            <div class="room-art-tags">
              <span class="badge entry">📍 HUB NODE</span>
              <span class="badge">${{roomCount}} Rooms</span>
            </div>
            <span style="font-size:10px; color:#fff;">🔍 Expand</span>
          </div>
        </div>

        <div style="color:#fff; font-size:14px; font-weight:700;">${{escapeHtml(node.title || node.id)}}</div>
        <div style="font-size:11px; color:var(--text-muted);">Node ID: <code style="color:var(--accent-cyan);">${{node.id}}</code></div>

        <div class="form-group" style="background:rgba(0,0,0,0.25); border:1px solid var(--border); padding:10px; border-radius:6px; margin-top:8px;">
          <label class="form-label">Hub Screen Placement Controls</label>
          <div style="font-size:10px; color:var(--text-muted); margin-bottom:6px;">
            Drag directly on the mobile hub screen or adjust precision coordinates below.
          </div>

          <div style="display:flex; flex-direction:column; gap:8px;">
            <div>
              <div style="display:flex; justify-content:space-between; font-size:11px; margin-bottom:3px;">
                <span style="color:#cbd5e1; font-weight:600;">Horizontal (X)</span>
                <span id="labelSiteX" style="font-family:var(--font-mono); color:var(--accent-cyan);">${{Math.round(site.x * 100)}}% (${{site.x.toFixed(2)}})</span>
              </div>
              <input type="range" class="form-range" id="sliderSiteX" min="0.05" max="0.95" step="0.01" value="${{site.x}}" oninput="onSiteSliderInput('${{node.id}}', 'x', this.value)">
              <div style="display:flex; gap:6px; margin-top:4px;">
                <input type="number" class="form-input" id="numSiteX" min="0.05" max="0.95" step="0.01" value="${{site.x.toFixed(2)}}" onchange="onSiteNumberInput('${{node.id}}', 'x', this.value)">
                <button class="btn" style="padding:2px 8px; font-size:10px;" onclick="centerSiteX('${{node.id}}')">Center (0.50)</button>
              </div>
            </div>

            <div>
              <div style="display:flex; justify-content:space-between; font-size:11px; margin-bottom:3px;">
                <span style="color:#cbd5e1; font-weight:600;">Vertical (Y)</span>
                <span id="labelSiteY" style="font-family:var(--font-mono); color:var(--accent-cyan);">${{Math.round(site.y * 100)}}% (${{site.y.toFixed(2)}})</span>
              </div>
              <input type="range" class="form-range" id="sliderSiteY" min="0.05" max="0.95" step="0.01" value="${{site.y}}" oninput="onSiteSliderInput('${{node.id}}', 'y', this.value)">
              <input type="number" class="form-input" id="numSiteY" min="0.05" max="0.95" step="0.01" value="${{site.y.toFixed(2)}}" style="margin-top:4px;" onchange="onSiteNumberInput('${{node.id}}', 'y', this.value)">
            </div>

            <div>
              <div style="display:flex; justify-content:space-between; font-size:11px; margin-bottom:3px;">
                <span style="color:#cbd5e1; font-weight:600;">Scale / Width</span>
                <span id="labelSiteW" style="font-family:var(--font-mono); color:var(--accent-cyan);">${{site.artwork_width || 0.25}}</span>
              </div>
              <input type="range" class="form-range" id="sliderSiteW" min="0.10" max="0.50" step="0.01" value="${{site.artwork_width || 0.25}}" oninput="onSiteSliderInput('${{node.id}}', 'w', this.value)">
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Kotlin Source Code</label>
          <div style="display:flex; gap:6px; align-items:center;">
            <input type="text" class="form-input" id="codeKotlinSite" value="${{kotlinSnippet}}" readonly style="font-family:var(--font-mono); font-size:10px;">
            <button class="btn" onclick="copyKotlinSnippet('${{node.id}}')">📋 Copy</button>
          </div>
        </div>

        <div class="form-group" style="margin-top:6px;">
          <label class="form-label">Entry Room</label>
          <input type="text" class="form-input" value="${{node.entry_room || ''}}" readonly>
        </div>

        <div class="form-group">
          <label class="form-label">Rooms in this Facility (${{roomCount}})</label>
          <div style="display:flex; flex-direction:column; gap:4px; max-height:140px; overflow-y:auto;">
            ${{(node.rooms || []).map(rId => {{
              const r = state.roomMap.get(rId);
              return `
                <div style="background:var(--panel-elevated); border:1px solid var(--border); padding:5px 8px; border-radius:4px; font-size:11px; display:flex; justify-content:space-between; align-items:center;">
                  <span style="color:#fff;">${{escapeHtml(r ? (r.title || r.id) : rId)}}</span>
                  <span style="color:var(--text-muted); font-size:9px;">${{r ? '[' + (r.pos || [0,0]).join(',') + ']' : ''}}</span>
                </div>
              `;
            }}).join('')}}
          </div>
        </div>

        <button class="btn btn-primary" style="margin-top:10px; padding:8px;" onclick="selectNode('${{node.id}}')">
          🗺️ Enter Node Room Map (4-Way Swipe Grid) ➡️
        </button>
      `;
    }}

    function enterAstraNode() {{
      const astraNode = state.hubNodes.find(n => n.id === 'astra_disembark' || n.id === 'astra_bridge_node');
      if (astraNode) {{
        state.activeWorld = 'world_astra';
        state.activeHub = 'hub_astra';
        populateHubSelector();
        selectNode(astraNode.id);
      }} else {{
        showToast('Astra facility selected.');
      }}
    }}

    // Canvas & Grid Renderers
    function resizeCanvas() {{
      const rect = canvasViewport.getBoundingClientRect();
      gridCanvas.width = rect.width || window.innerWidth;
      gridCanvas.height = rect.height || (window.innerHeight - 150);
      renderGrid();
    }}

    function renderGrid() {{
      ctx.clearRect(0, 0, gridCanvas.width, gridCanvas.height);
      const step = state.gridSpacing * state.zoom;
      if (step < 15) return;

      const offsetX = (state.panX % step + step) % step;
      const offsetY = (state.panY % step + step) % step;

      ctx.beginPath();
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.04)';
      ctx.lineWidth = 1;

      for (let x = offsetX; x < gridCanvas.width; x += step) {{
        ctx.moveTo(x, 0);
        ctx.lineTo(x, gridCanvas.height);
      }}
      for (let y = offsetY; y < gridCanvas.height; y += step) {{
        ctx.moveTo(0, y);
        ctx.lineTo(gridCanvas.width, y);
      }}
      ctx.stroke();
    }}

    function gridToScreen(gx, gy, nodeId = null) {{
      let effectiveX = gx;
      let effectiveY = gy;

      if (state.activeNode === 'all_nodes' && nodeId) {{
        const offset = getNodeOffset(nodeId);
        effectiveX += offset.x;
        effectiveY += offset.y;
      }}

      return {{
        x: state.panX + effectiveX * state.gridSpacing * state.zoom,
        y: state.panY - effectiveY * state.gridSpacing * state.zoom
      }};
    }}

    function screenToGrid(sx, sy, nodeId = null) {{
      let gx = Math.round((sx - state.panX) / (state.gridSpacing * state.zoom));
      let gy = Math.round(-(sy - state.panY) / (state.gridSpacing * state.zoom));

      if (state.activeNode === 'all_nodes' && nodeId) {{
        const offset = getNodeOffset(nodeId);
        gx -= offset.x;
        gy -= offset.y;
      }}

      return {{ x: gx, y: gy }};
    }}

    function fitView() {{
      const visibleRooms = getVisibleRooms();
      const vpWidth = canvasViewport.clientWidth || window.innerWidth - 360;
      const vpHeight = canvasViewport.clientHeight || window.innerHeight - 150;

      if (visibleRooms.length === 0) {{
        state.zoom = 1.0;
        state.panX = vpWidth / 2;
        state.panY = vpHeight / 2;
        render();
        return;
      }}

      let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
      visibleRooms.forEach(r => {{
        const pos = r.pos || [0, 0];
        const node = getRoomNode(r);
        const offset = (state.activeNode === 'all_nodes' && node) ? getNodeOffset(node.id) : {{ x: 0, y: 0 }};
        const rx = pos[0] + offset.x;
        const ry = pos[1] + offset.y;
        minX = Math.min(minX, rx);
        maxX = Math.max(maxX, rx);
        minY = Math.min(minY, ry);
        maxY = Math.max(maxY, ry);
      }});

      const centerX = (minX + maxX) / 2;
      const centerY = (minY + maxY) / 2;
      const spanX = Math.max(maxX - minX + 2.5, 3);
      const spanY = Math.max(maxY - minY + 2.5, 3);

      const targetZoomX = vpWidth / (spanX * state.gridSpacing);
      const targetZoomY = vpHeight / (spanY * state.gridSpacing);
      state.zoom = Math.min(Math.max(Math.min(targetZoomX, targetZoomY), 0.35), 1.2);

      state.panX = vpWidth / 2 - centerX * state.gridSpacing * state.zoom;
      state.panY = vpHeight / 2 + centerY * state.gridSpacing * state.zoom;
      render();
    }}

    // Main Render Pipeline
    function render() {{
      if (state.activeViewMode === 'hub_screen') {{
        renderHubScreen();
        return;
      }}

      renderGrid();
      nodesLayer.innerHTML = '';
      islandsLayer.innerHTML = '';
      linksSvg.innerHTML = '';

      const visibleRooms = getVisibleRooms();
      const visibleSet = new Set(visibleRooms.map(r => r.id));

      // Scope Label
      const scopeLabel = document.getElementById('kpiScope');
      if (state.activeNode !== 'all_nodes') {{
        const n = state.nodeMap.get(state.activeNode);
        scopeLabel.textContent = `Scope: Node [${{n ? (n.title || n.id) : state.activeNode}}]`;
      }} else if (state.activeHub !== 'all') {{
        const h = state.hubMap.get(state.activeHub);
        scopeLabel.textContent = `Scope: Hub [${{h ? (h.title || h.id) : state.activeHub}}] (All Nodes)`;
      }} else {{
        scopeLabel.textContent = `Scope: World [${{state.activeWorld}}]`;
      }}

      // Check In-Node Collisions
      const inNodeCoordCounts = new Map();
      visibleRooms.forEach(r => {{
        const node = getRoomNode(r);
        const nodeId = node ? node.id : 'unknown';
        const key = nodeId + ':' + (r.pos || [0, 0]).join(',');
        inNodeCoordCounts.set(key, (inNodeCoordCounts.get(key) || 0) + 1);
      }});

      let totalReciprocal = 0;
      let totalViolations = 0;
      let totalAsymmetric = 0;
      let inNodeCollisions = 0;
      const incomingCounts = new Map();

      // Render Island Outlines if in 'all_nodes' view
      if (state.activeNode === 'all_nodes') {{
        const hubNodes = state.hubNodes.filter(n => state.activeHub === 'all' || n.hub_id === state.activeHub);
        hubNodes.forEach(node => {{
          const nodeRooms = state.rooms.filter(r => (node.rooms || []).includes(r.id));
          if (nodeRooms.length === 0) return;

          let nMinX = Infinity, nMaxX = -Infinity, nMinY = Infinity, nMaxY = -Infinity;
          nodeRooms.forEach(r => {{
            const p = r.pos || [0, 0];
            nMinX = Math.min(nMinX, p[0]);
            nMaxX = Math.max(nMaxX, p[0]);
            nMinY = Math.min(nMinY, p[1]);
            nMaxY = Math.max(nMaxY, p[1]);
          }});

          const pTopLeft = gridToScreen(nMinX - 0.7, nMaxY + 0.7, node.id);
          const pBottomRight = gridToScreen(nMaxX + 0.7, nMinY - 0.7, node.id);

          const islandBox = document.createElement('div');
          islandBox.style.position = 'absolute';
          islandBox.style.left = pTopLeft.x + 'px';
          islandBox.style.top = pTopLeft.y + 'px';
          islandBox.style.width = (pBottomRight.x - pTopLeft.x) + 'px';
          islandBox.style.height = (pBottomRight.y - pTopLeft.y) + 'px';
          islandBox.style.border = '1px dashed rgba(56, 189, 248, 0.35)';
          islandBox.style.borderRadius = '12px';
          islandBox.style.background = 'rgba(15, 23, 42, 0.35)';
          islandBox.style.pointerEvents = 'auto';
          islandBox.style.cursor = 'pointer';

          const tag = document.createElement('div');
          tag.style.position = 'absolute';
          tag.style.top = '-12px';
          tag.style.left = '14px';
          tag.style.background = '#0284c7';
          tag.style.color = '#fff';
          tag.style.padding = '2px 8px';
          tag.style.borderRadius = '4px';
          tag.style.fontSize = '10px';
          tag.style.fontWeight = '700';
          tag.textContent = `${{node.title || node.id}} (${{nodeRooms.length}} rooms) 🔍 Focus`;
          tag.addEventListener('click', (e) => {{
            e.stopPropagation();
            selectNode(node.id);
          }});

          islandBox.appendChild(tag);
          islandBox.addEventListener('click', () => selectNode(node.id));
          islandsLayer.appendChild(islandBox);
        }});
      }}

      // Render Room Cards
      visibleRooms.forEach(room => {{
        const node = getRoomNode(room);
        const nodeId = node ? node.id : null;
        const pos = room.pos || [0, 0];
        const screenPos = gridToScreen(pos[0], pos[1], nodeId);

        const nodeEl = document.createElement('div');
        nodeEl.className = 'room-node';
        nodeEl.id = 'node_' + room.id;
        nodeEl.style.left = (screenPos.x - 80) + 'px';
        nodeEl.style.top = (screenPos.y - (state.showArt ? 64 : 42)) + 'px';
        nodeEl.style.transform = `scale(${{Math.min(Math.max(state.zoom, 0.7), 1.15)}})`;

        if (state.selectedRoomId === room.id) nodeEl.classList.add('selected');

        const collisionKey = (nodeId || 'unknown') + ':' + pos.join(',');
        if ((inNodeCoordCounts.get(collisionKey) || 0) > 1) {{
          nodeEl.classList.add('collision');
          inNodeCollisions++;
        }}

        const isEntry = node && (node.entry_room === room.id);
        if (isEntry) nodeEl.classList.add('is-entry');

        let roomHasViolation = false;
        const conns = room.connections || {{}};
        for (const [dir, target] of Object.entries(conns)) {{
          if (!['north', 'south', 'east', 'west'].includes(dir)) {{
            roomHasViolation = true;
            totalViolations++;
          }} else {{
            const opp = oppositeDirections[dir];
            const targetRoom = state.roomMap.get(target);
            if (!targetRoom || (targetRoom.connections || {{}})[opp] !== room.id) {{
              totalAsymmetric++;
            }} else {{
              totalReciprocal++;
            }}
          }}
          incomingCounts.set(target, (incomingCounts.get(target) || 0) + 1);
        }}
        if (roomHasViolation) nodeEl.classList.add('has-violation');

        // Art Banner (if Art Mode enabled)
        if (state.showArt && room.background_image) {{
          const bannerEl = document.createElement('div');
          bannerEl.className = 'node-art-banner';
          bannerEl.style.backgroundImage = `url('${{getImageUrl(room.background_image)}}')`;
          bannerEl.innerHTML = `<div class="banner-gradient"></div>`;
          nodeEl.appendChild(bannerEl);
        }}

        // Header
        const headerEl = document.createElement('div');
        headerEl.className = 'node-header';
        headerEl.innerHTML = `
          <div class="node-title" title="${{escapeHtml(room.title || room.id)}}">${{escapeHtml(room.title || 'Untitled')}}</div>
          <div class="node-pos">${{pos[0]}},${{pos[1]}}</div>
        `;

        const idEl = document.createElement('div');
        idEl.className = 'node-id';
        idEl.textContent = room.id;

        // Badges
        const badgesEl = document.createElement('div');
        badgesEl.className = 'node-badges';
        if (isEntry) badgesEl.innerHTML += `<span class="badge entry" title="Hub Node Entry Point">🚪 ENTRY</span>`;
        if (state.showDetails) {{
          if (room.enemies && room.enemies.length > 0) badgesEl.innerHTML += `<span class="badge combat" title="Enemies: ${{room.enemies.join(', ')}}">⚔️ ${{room.enemies.length}}</span>`;
          if (room.actions && room.actions.length > 0) badgesEl.innerHTML += `<span class="badge action" title="Actions: ${{room.actions.length}}">⚡ ${{room.actions.length}}</span>`;
          if (room.items && room.items.length > 0) badgesEl.innerHTML += `<span class="badge item" title="Items: ${{room.items.length}}">📦 ${{room.items.length}}</span>`;
          if (room.npcs && room.npcs.length > 0) badgesEl.innerHTML += `<span class="badge npc" title="NPCs: ${{room.npcs.join(', ')}}">👤</span>`;
        }}

        nodeEl.appendChild(headerEl);
        nodeEl.appendChild(idEl);
        nodeEl.appendChild(badgesEl);

        // Cardinal Connection Pins
        ['n', 's', 'e', 'w'].forEach(dirKey => {{
          const pin = document.createElement('div');
          const fullDir = {{ n: 'north', s: 'south', e: 'east', w: 'west' }}[dirKey];
          pin.className = `port-pin pin-${{dirKey}}`;
          if (conns[fullDir]) pin.classList.add('connected');
          pin.title = `Connect ${{fullDir.toUpperCase()}}`;
          pin.addEventListener('mousedown', (e) => {{
            e.stopPropagation();
            startConnection(room.id, fullDir);
          }});
          nodeEl.appendChild(pin);
        }});

        // Click & Drag Node Card
        nodeEl.addEventListener('mousedown', (e) => {{
          if (e.button !== 0) return;
          e.stopPropagation();
          selectRoom(room.id);
          startNodeDrag(room.id, e);
        }});

        nodesLayer.appendChild(nodeEl);
      }});

      // Render Connection Vectors
      const drawnLinks = new Set();
      visibleRooms.forEach(room => {{
        const fromNode = getRoomNode(room);
        const fromNodeId = fromNode ? fromNode.id : null;
        const fromPos = room.pos || [0, 0];
        const p1 = gridToScreen(fromPos[0], fromPos[1], fromNodeId);
        const conns = room.connections || {{}};

        for (const [dir, targetId] of Object.entries(conns)) {{
          const targetRoom = state.roomMap.get(targetId);
          if (!targetRoom) continue;

          const toNode = getRoomNode(targetRoom);
          const toNodeId = toNode ? toNode.id : null;
          const isCrossNode = fromNodeId && toNodeId && (fromNodeId !== toNodeId);

          if (!visibleSet.has(targetId)) {{
            drawOutboundExitIndicator(p1, dir, targetId, toNode ? toNode.title : 'External');
            continue;
          }}

          const toPos = targetRoom.pos || [0, 0];
          const p2 = gridToScreen(toPos[0], toPos[1], toNodeId);

          const linkKey = [room.id, targetId].sort().join('--');
          const isNSEW = ['north', 'south', 'east', 'west'].includes(dir);
          const opp = oppositeDirections[dir];
          const isReciprocal = opp && (targetRoom.connections || {{}})[opp] === room.id;

          if (isReciprocal && drawnLinks.has(linkKey)) continue;
          drawnLinks.add(linkKey);

          drawConnectionLine(p1, p2, dir, isNSEW, isReciprocal, isCrossNode);
        }}
      }});

      // Calculate KPI Values
      const orphanCount = visibleRooms.filter(r => (incomingCounts.get(r.id) || 0) === 0).length;
      const totalExits = totalReciprocal + totalViolations + totalAsymmetric;
      const compliancePercent = totalExits === 0 ? 100 : Math.round((totalReciprocal / totalExits) * 100);

      document.getElementById('kpiTotalRooms').textContent = visibleRooms.length;
      const compEl = document.getElementById('kpiCompliance');
      compEl.textContent = compliancePercent + '%';
      compEl.className = 'kpi-value ' + (compliancePercent >= 95 ? 'good' : compliancePercent >= 80 ? 'warn' : 'error');

      const violEl = document.getElementById('kpiViolations');
      violEl.textContent = totalViolations;
      violEl.className = 'kpi-value ' + (totalViolations === 0 ? 'good' : 'error');

      const asymEl = document.getElementById('kpiAsymmetric');
      asymEl.textContent = totalAsymmetric;
      asymEl.className = 'kpi-value ' + (totalAsymmetric === 0 ? 'good' : 'warn');

      const colEl = document.getElementById('kpiCollisions');
      colEl.textContent = inNodeCollisions;
      colEl.className = 'kpi-value ' + (inNodeCollisions === 0 ? 'good' : 'error');

      document.getElementById('kpiOrphans').textContent = orphanCount;
    }}

    function drawConnectionLine(p1, p2, dir, isNSEW, isReciprocal, isCrossNode) {{
      const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      line.setAttribute('x1', p1.x);
      line.setAttribute('y1', p1.y);
      line.setAttribute('x2', p2.x);
      line.setAttribute('y2', p2.y);

      if (!isNSEW) {{
        line.setAttribute('stroke', '#ef4444');
        line.setAttribute('stroke-width', '3');
        line.setAttribute('stroke-dasharray', '4,4');
      }} else if (isCrossNode) {{
        line.setAttribute('stroke', '#c084fc');
        line.setAttribute('stroke-width', '2.5');
        line.setAttribute('stroke-dasharray', '6,3');
      }} else if (isReciprocal) {{
        line.setAttribute('stroke', '#10b981');
        line.setAttribute('stroke-width', '2.5');
      }} else {{
        line.setAttribute('stroke', '#f59e0b');
        line.setAttribute('stroke-width', '2');
        line.setAttribute('stroke-dasharray', '5,5');
      }}
      linksSvg.appendChild(line);
    }}

    function drawOutboundExitIndicator(p, dir, targetRoomId, targetNodeName) {{
      const dirOffsets = {{
        north: {{ x: 0, y: -45 }},
        south: {{ x: 0, y: 45 }},
        east: {{ x: 45, y: 0 }},
        west: {{ x: -45, y: 0 }}
      }};
      const offset = dirOffsets[dir] || {{ x: 30, y: 30 }};
      const x2 = p.x + offset.x;
      const y2 = p.y + offset.y;

      const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      line.setAttribute('x1', p.x);
      line.setAttribute('y1', p.y);
      line.setAttribute('x2', x2);
      line.setAttribute('y2', y2);
      line.setAttribute('stroke', '#a855f7');
      line.setAttribute('stroke-width', '2');
      line.setAttribute('stroke-dasharray', '3,3');
      linksSvg.appendChild(line);

      const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      circle.setAttribute('cx', x2);
      circle.setAttribute('cy', y2);
      circle.setAttribute('r', '4');
      circle.setAttribute('fill', '#a855f7');
      linksSvg.appendChild(circle);
    }}

    // Room Dragging
    function startNodeDrag(roomId, e) {{
      state.isDraggingNode = true;
      state.draggedRoomId = roomId;
      const room = state.roomMap.get(roomId);
      const node = getRoomNode(room);
      const pos = room.pos || [0, 0];
      const screenPos = gridToScreen(pos[0], pos[1], node ? node.id : null);
      state.dragOffsetX = e.clientX - screenPos.x;
      state.dragOffsetY = e.clientY - screenPos.y;
      pushHistory();
    }}

    function onNodeDrag(e) {{
      if (!state.isDraggingNode || !state.draggedRoomId) return;
      const room = state.roomMap.get(state.draggedRoomId);
      if (!room) return;

      const node = getRoomNode(room);
      const mouseX = e.clientX - state.dragOffsetX;
      const mouseY = e.clientY - state.dragOffsetY;
      const gridPos = screenToGrid(mouseX, mouseY, node ? node.id : null);

      if (document.getElementById('gridSnapCheck').checked) {{
        if (!room.pos || room.pos[0] !== gridPos.x || room.pos[1] !== gridPos.y) {{
          room.pos = [gridPos.x, gridPos.y];
          render();
          updateInspectorCoords(gridPos.x, gridPos.y);
        }}
      }} else {{
        room.pos = [gridPos.x, gridPos.y];
        render();
      }}
    }}

    function stopNodeDrag() {{
      state.isDraggingNode = false;
      state.draggedRoomId = null;
    }}

    // Connection Wiring
    function startConnection(roomId, direction) {{
      state.connectingFrom = {{ roomId, direction }};
      showToast(`Select target room to connect from [${{roomId}}] (${{direction.toUpperCase()}})...`);
    }}

    function completeConnection(targetRoomId) {{
      if (!state.connectingFrom) return;
      const {{ roomId, direction }} = state.connectingFrom;
      state.connectingFrom = null;

      if (roomId === targetRoomId) {{
        showToast('Cannot connect a room to itself.');
        return;
      }}

      pushHistory();
      const sourceRoom = state.roomMap.get(roomId);
      const targetRoom = state.roomMap.get(targetRoomId);

      if (!sourceRoom.connections) sourceRoom.connections = {{}};
      sourceRoom.connections[direction] = targetRoomId;

      if (document.getElementById('autoReciprocalCheck').checked) {{
        const opp = oppositeDirections[direction];
        if (opp) {{
          if (!targetRoom.connections) targetRoom.connections = {{}};
          targetRoom.connections[opp] = roomId;
        }}
      }}

      showToast(`Connected ${{roomId}} (${{direction}}) -> ${{targetRoomId}}`);
      render();
      if (state.selectedRoomId === roomId || state.selectedRoomId === targetRoomId) {{
        selectRoom(state.selectedRoomId);
      }}
    }}

    // Canvas Events
    function setupCanvasEvents() {{
      canvasViewport.addEventListener('mousedown', (e) => {{
        if (e.target.closest('.room-node') || e.target.closest('.canvas-hud')) return;
        state.isPanning = true;
        state.panStartX = e.clientX - state.panX;
        state.panStartY = e.clientY - state.panY;
      }});

      window.addEventListener('mousemove', (e) => {{
        if (state.isDraggingHubNode) {{
          onHubNodeDrag(e);
        }} else if (state.isDraggingNode) {{
          onNodeDrag(e);
        }} else if (state.isPanning) {{
          state.panX = e.clientX - state.panStartX;
          state.panY = e.clientY - state.panStartY;
          render();
        }}
      }});

      window.addEventListener('mouseup', () => {{
        if (state.isDraggingHubNode) stopHubNodeDrag();
        if (state.isDraggingNode) stopNodeDrag();
        state.isPanning = false;
      }});

      canvasViewport.addEventListener('wheel', (e) => {{
        e.preventDefault();
        const zoomFactor = e.deltaY < 0 ? 1.12 : 0.89;
        const newZoom = Math.min(Math.max(state.zoom * zoomFactor, 0.25), 1.6);

        const mouseX = e.clientX;
        const mouseY = e.clientY;

        state.panX = mouseX - (mouseX - state.panX) * (newZoom / state.zoom);
        state.panY = mouseY - (mouseY - state.panY) * (newZoom / state.zoom);
        state.zoom = newZoom;
        render();
      }}, {{ passive: false }});

      // Canvas HUD
      document.getElementById('btnZoomIn').addEventListener('click', () => {{
        state.zoom = Math.min(state.zoom * 1.2, 1.6);
        render();
      }});
      document.getElementById('btnZoomOut').addEventListener('click', () => {{
        state.zoom = Math.max(state.zoom * 0.8, 0.25);
        render();
      }});
      document.getElementById('btnResetView').addEventListener('click', () => {{
        fitView();
      }});
      document.getElementById('btnToggleArtMode').addEventListener('click', (e) => {{
        state.showArt = !state.showArt;
        e.target.textContent = state.showArt ? '🖼️ Art Mode: ON' : '📐 Blueprint Mode';
        render();
      }});
      document.getElementById('btnToggleLabels').addEventListener('click', () => {{
        state.showDetails = !state.showDetails;
        render();
      }});
      document.getElementById('btnCreateRoom').addEventListener('click', createNewRoom);
    }}

    // Toolbar & Filter Events
    function setupToolbarEvents() {{
      // View Mode Switcher
      document.getElementById('btnModeRoomGrid').addEventListener('click', () => switchViewMode('room_grid'));
      document.getElementById('btnModeHubScreen').addEventListener('click', () => switchViewMode('hub_screen'));

      // World tabs
      document.querySelectorAll('.world-tab').forEach(tab => {{
        tab.addEventListener('click', () => {{
          document.querySelectorAll('.world-tab').forEach(t => t.classList.remove('active'));
          tab.classList.add('active');
          state.activeWorld = tab.dataset.world;
          populateHubSelector();
          if (state.activeViewMode === 'hub_screen') {{
            renderHubScreen();
          }} else {{
            fitView();
          }}
        }});
      }});

      // Hub dropdown
      document.getElementById('hubFilterSelect').addEventListener('change', (e) => {{
        state.activeHub = e.target.value;
        populateNodeSelector();
        if (state.activeViewMode === 'hub_screen') {{
          renderHubScreen();
        }} else {{
          fitView();
        }}
      }});

      // Node dropdown
      document.getElementById('nodeFilterSelect').addEventListener('change', (e) => {{
        selectNode(e.target.value);
      }});

      // Search
      document.getElementById('searchInput').addEventListener('input', () => {{
        render();
      }});

      // Undo / Redo
      document.getElementById('btnUndo').addEventListener('click', undo);
      window.addEventListener('keydown', (e) => {{
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {{
          e.preventDefault();
          undo();
        }}
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {{
          e.preventDefault();
          saveToProject();
        }}
      }});

      // Save & Export
      document.getElementById('btnSave').addEventListener('click', saveToProject);
      document.getElementById('btnExport').addEventListener('click', exportJson);
      document.getElementById('btnReconnect').addEventListener('click', () => init());

      // Audit Drawer Toggle
      document.getElementById('btnToggleAuditTray').addEventListener('click', toggleAuditTray);
      document.getElementById('btnCloseAuditTray').addEventListener('click', () => auditTray.classList.remove('open'));
      document.getElementById('kpiViolationsBtn').addEventListener('click', () => openAuditFilter('violations'));
      document.getElementById('kpiAsymmetricBtn').addEventListener('click', () => openAuditFilter('asymmetric'));
      document.getElementById('kpiCollisionsBtn').addEventListener('click', () => openAuditFilter('collisions'));
      document.getElementById('kpiOrphansBtn').addEventListener('click', () => openAuditFilter('orphans'));
      document.getElementById('btnCloseDrawer').addEventListener('click', () => {{
        inspectorDrawer.classList.add('collapsed');
      }});
    }}

    // Room Selection & Inspector
    function selectRoom(roomId) {{
      if (state.connectingFrom) {{
        completeConnection(roomId);
        return;
      }}

      state.selectedRoomId = roomId;
      inspectorDrawer.classList.remove('collapsed');
      const room = state.roomMap.get(roomId);
      if (!room) return;

      const node = getRoomNode(room);
      const pos = room.pos || [0, 0];
      const conns = room.connections || {{}};
      const isEntry = node && (node.entry_room === room.id);

      let nonNsewRows = '';
      for (const [dir, target] of Object.entries(conns)) {{
        if (!['north', 'south', 'east', 'west'].includes(dir)) {{
          nonNsewRows += `
            <div class="mold-breaker-alert">
              <div class="mold-breaker-title">🚨 Non-NSEW Exit: ${{dir.toUpperCase()}} -&gt; ${{target}}</div>
              <div class="mold-breaker-desc">This exit breaks mobile 4-way swipe. Convert it into a bolded keyword interactable.</div>
              <button class="btn btn-primary" style="font-size:10px; padding:3px 7px;" onclick="convertExitToAction('${{room.id}}', '${{dir}}', '${{target}}')">
                ⚡ Convert to Actionable Keyword Exit
              </button>
            </div>
          `;
        }}
      }}

      const bgUrl = getImageUrl(room.background_image);

      drawerContent.innerHTML = `
        ${{room.background_image ? `
          <div class="room-art-card" onclick="openLightbox('${{bgUrl}}', '${{escapeHtml(room.title)}}')">
            <img src="${{bgUrl}}" onerror="this.parentElement.style.display='none'" />
            <div class="room-art-scrim">
              <div class="room-art-tags">
                <span class="badge entry">🌍 ${{room.env || 'interior'}}</span>
                <span class="badge">${{room.weather || 'clear'}}</span>
              </div>
              <span style="font-size:10px; color:#fff;">🔍 Expand Artwork</span>
            </div>
          </div>
        ` : ''}}

        <div style="background: rgba(0,0,0,0.25); border: 1px solid var(--border); padding: 8px 10px; border-radius: 6px; font-size: 11px;">
          <div style="color: var(--accent-cyan); font-weight: 700; margin-bottom: 2px;">📍 ${{node ? (node.title || node.id) : 'Unassigned Node'}}</div>
          <div style="color: var(--text-muted); font-size: 10px;">ID: ${{room.id}} | World: ${{getRoomWorldId(room)}}</div>
          ${{isEntry ? '<div style="color: var(--accent-gold); font-weight:700; margin-top:4px;">🌟 Designated Node Entry Room</div>' : ''}}
        </div>

        <div class="form-group">
          <label class="form-label">Room Title</label>
          <input type="text" class="form-input" id="editRoomTitle" value="${{escapeHtml(room.title || '')}}">
        </div>

        <div class="coords-row">
          <div class="form-group">
            <label class="form-label">Grid X</label>
            <input type="number" class="form-input" id="editRoomX" value="${{pos[0]}}">
          </div>
          <div class="form-group">
            <label class="form-label">Grid Y</label>
            <input type="number" class="form-input" id="editRoomY" value="${{pos[1]}}">
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Description</label>
          <textarea class="form-textarea" id="editRoomDesc">${{escapeHtml(room.description || '')}}</textarea>
        </div>

        <div class="form-group">
          <label class="form-label">Strict Cardinal Exits (NSEW)</label>
          <div class="connection-grid">
            ${{renderExitCard(room, 'north', 'North (Swipe Up)')}}
            ${{renderExitCard(room, 'south', 'South (Swipe Down)')}}
            ${{renderExitCard(room, 'east', 'East (Swipe Right)')}}
            ${{renderExitCard(room, 'west', 'West (Swipe Left)')}}
          </div>
        </div>

        ${{nonNsewRows}}

        <div class="form-group">
          <label class="form-label">Background Image Path</label>
          <input type="text" class="form-input" id="editRoomBg" placeholder="images/rooms/..." value="${{escapeHtml(room.background_image || '')}}">
        </div>

        <div class="form-group">
          <label class="form-label">Environment &amp; Weather</label>
          <div class="coords-row">
            <input type="text" class="form-input" id="editRoomEnv" placeholder="env (e.g. mine, urban)" value="${{escapeHtml(room.env || '')}}">
            <input type="text" class="form-input" id="editRoomWeather" placeholder="weather" value="${{escapeHtml(room.weather || '')}}">
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Action Keywords / Transitions (${{(room.actions || []).length}})</label>
          <div style="font-size: 11px; color: var(--text-muted); line-height: 1.3;">
            Action keywords trigger bolded text interactions or elevator/chute warps.
          </div>
          <div id="actionKeywordsList" style="display: flex; flex-direction: column; gap: 4px; margin-top: 4px;">
            ${{(room.actions || []).map((a, i) => `
              <div style="background: var(--panel-elevated); border:1px solid var(--border); padding:6px; border-radius:4px; font-size:11px; display:flex; justify-content:space-between; align-items:center;">
                <div>
                  <strong style="color:var(--accent-cyan);">${{escapeHtml(a.name || 'unnamed')}}</strong>
                  <span style="color:var(--text-muted); font-size:9px;">(${{a.type || 'generic'}}${{a.target_room ? ' -&gt; ' + a.target_room : ''}})</span>
                </div>
                <button class="btn" style="padding:1px 5px; font-size:9px;" onclick="removeAction('${{room.id}}', ${{i}})">✕</button>
              </div>
            `).join('')}}
          </div>
          <button class="btn" style="margin-top: 6px; font-size:10px;" onclick="promptAddAction('${{room.id}}')">➕ Add Action Keyword</button>
        </div>
      `;

      document.getElementById('editRoomTitle').addEventListener('input', (e) => {{
        pushHistory();
        room.title = e.target.value;
        render();
      }});
      document.getElementById('editRoomX').addEventListener('change', (e) => {{
        pushHistory();
        room.pos = [parseInt(e.target.value) || 0, room.pos ? room.pos[1] : 0];
        render();
      }});
      document.getElementById('editRoomY').addEventListener('change', (e) => {{
        pushHistory();
        room.pos = [room.pos ? room.pos[0] : 0, parseInt(e.target.value) || 0];
        render();
      }});
      document.getElementById('editRoomDesc').addEventListener('input', (e) => {{
        room.description = e.target.value;
      }});
      document.getElementById('editRoomBg').addEventListener('change', (e) => {{
        room.background_image = e.target.value;
        selectRoom(roomId);
      }});
      document.getElementById('editRoomEnv').addEventListener('input', (e) => {{
        room.env = e.target.value;
      }});
      document.getElementById('editRoomWeather').addEventListener('input', (e) => {{
        room.weather = e.target.value;
      }});

      render();
    }}

    function renderExitCard(room, dir, label) {{
      const conns = room.connections || {{}};
      const target = conns[dir] || '';
      let statusClass = 'none';
      let statusTitle = 'Unconnected';

      if (target) {{
        const opp = oppositeDirections[dir];
        const targetRoom = state.roomMap.get(target);
        if (targetRoom && (targetRoom.connections || {{}})[opp] === room.id) {{
          statusClass = 'reciprocal';
          statusTitle = 'Strict Reciprocal (Matches mobile swipe back)';
        }} else {{
          statusClass = 'asymmetric';
          statusTitle = 'Asymmetric (No reciprocal swipe return)';
        }}
      }}

      return `
        <div class="conn-card">
          <div class="conn-card-header">
            <span style="color:#cbd5e1;">${{label}}</span>
            <div class="conn-status ${{statusClass}}" title="${{statusTitle}}"></div>
          </div>
          <input type="text" class="form-input" style="font-size:10px; padding:3px 6px;"
                 placeholder="target_room_id" value="${{escapeHtml(target)}}"
                 onchange="updateRoomExit('${{room.id}}', '${{dir}}', this.value)">
        </div>
      `;
    }}

    function updateRoomExit(roomId, direction, targetValue) {{
      pushHistory();
      const room = state.roomMap.get(roomId);
      if (!room) return;
      if (!room.connections) room.connections = {{}};

      const trimmed = targetValue.trim();
      if (trimmed) {{
        room.connections[direction] = trimmed;
        if (document.getElementById('autoReciprocalCheck').checked) {{
          const opp = oppositeDirections[direction];
          const targetRoom = state.roomMap.get(trimmed);
          if (targetRoom && opp) {{
            if (!targetRoom.connections) targetRoom.connections = {{}};
            targetRoom.connections[opp] = roomId;
          }}
        }}
      }} else {{
        delete room.connections[direction];
      }}

      render();
      selectRoom(roomId);
    }}

    function updateInspectorCoords(x, y) {{
      const xInput = document.getElementById('editRoomX');
      const yInput = document.getElementById('editRoomY');
      if (xInput) xInput.value = x;
      if (yInput) yInput.value = y;
    }}

    window.convertExitToAction = function(roomId, direction, targetRoomId) {{
      pushHistory();
      const room = state.roomMap.get(roomId);
      if (!room) return;

      if (room.connections && room.connections[direction]) {{
        delete room.connections[direction];
      }}

      if (!room.actions) room.actions = [];
      const actionName = direction === 'up' ? 'climb_ladder' :
                         direction === 'down' ? 'service_lift' :
                         `access_${{direction}}`;

      room.actions.push({{
        name: actionName,
        type: 'warp',
        target_room: targetRoomId,
        condition_unmet_message: `You access the ${{actionName.replace('_', ' ')}} leading to ${{targetRoomId}}.`
      }});

      showToast(`Converted ${{direction.toUpperCase()}} into actionable keyword: [${{actionName}}]`);
      render();
      selectRoom(roomId);
    }};

    window.promptAddAction = function(roomId) {{
      const name = prompt('Action Keyword Name (e.g. lift, terminal, crawlspace):');
      if (!name) return;
      const type = prompt('Type (warp / inspect / generic):', 'warp') || 'generic';
      let target_room = null;
      if (type === 'warp') {{
        target_room = prompt('Target Room ID for warp:');
      }}

      pushHistory();
      const room = state.roomMap.get(roomId);
      if (!room.actions) room.actions = [];
      const newAction = {{ name, type }};
      if (target_room) newAction.target_room = target_room;
      room.actions.push(newAction);

      render();
      selectRoom(roomId);
    }};

    window.removeAction = function(roomId, index) {{
      pushHistory();
      const room = state.roomMap.get(roomId);
      if (room && room.actions) {{
        room.actions.splice(index, 1);
        render();
        selectRoom(roomId);
      }}
    }};

    function createNewRoom() {{
      const id = prompt('Enter new unique Room ID:');
      if (!id) return;
      if (state.roomMap.has(id)) {{
        showToast('A room with that ID already exists!');
        return;
      }}

      const title = prompt('Enter Room Title:', 'New Chamber') || 'New Chamber';
      let pos = [0, 0];

      if (state.activeNode !== 'all_nodes') {{
        const node = state.nodeMap.get(state.activeNode);
        if (node) {{
          if (!node.rooms) node.rooms = [];
          node.rooms.push(id);
          const visible = getVisibleRooms();
          if (visible.length > 0) {{
            const last = visible[visible.length - 1];
            pos = [last.pos[0] + 1, last.pos[1]];
          }}
        }}
      }}

      const newRoom = {{
        id: id,
        title: title,
        description: 'A newly mapped chamber.',
        pos: pos,
        connections: {{}},
        actions: [],
        items: [],
        npcs: [],
        enemies: []
      }};

      pushHistory();
      state.rooms.push(newRoom);
      indexData();
      selectRoom(id);
      render();
      showToast(`Created room ${{id}}`);
    }}

    // Audit Drawer & Diagnostic Filters
    function toggleAuditTray() {{
      auditTray.classList.toggle('open');
      if (auditTray.classList.contains('open')) {{
        populateAuditIssues('all');
      }}
    }}

    function openAuditFilter(filterType) {{
      auditTray.classList.add('open');
      populateAuditIssues(filterType);
    }}

    function populateAuditIssues(filterType) {{
      auditTrayContent.innerHTML = '';
      const issues = [];
      const visibleRooms = getVisibleRooms();

      visibleRooms.forEach(room => {{
        const node = getRoomNode(room);
        const conns = room.connections || {{}};

        for (const [dir, target] of Object.entries(conns)) {{
          if (!['north', 'south', 'east', 'west'].includes(dir)) {{
            issues.push({{
              type: 'violations',
              room: room,
              node: node,
              label: `Diagonal / Vertical Exit: ${{dir.toUpperCase()}} -&gt; ${{target}}`,
              desc: 'Violates 4-way mobile swipe grid.'
            }});
          }} else {{
            const opp = oppositeDirections[dir];
            const targetRoom = state.roomMap.get(target);
            if (!targetRoom || (targetRoom.connections || {{}})[opp] !== room.id) {{
              issues.push({{
                type: 'asymmetric',
                room: room,
                node: node,
                label: `Asymmetric Exit: ${{dir.toUpperCase()}} -&gt; ${{target}}`,
                desc: 'Target room does not wire reciprocal return swipe.'
              }});
            }}
          }}
        }}
      }});

      const filtered = filterType === 'all' ? issues : issues.filter(i => i.type === filterType);

      if (filtered.length === 0) {{
        auditTrayContent.innerHTML = '<div style="color:var(--accent-green); font-size:12px; padding:8px;">✅ No issues detected in this scope!</div>';
        return;
      }}

      filtered.forEach(issue => {{
        const row = document.createElement('div');
        row.className = 'audit-row';
        row.innerHTML = `
          <div>
            <div style="font-weight:700; color:#fff;">${{escapeHtml(issue.room.title || issue.room.id)}} <span style="color:var(--accent-cyan); font-weight:normal;">(${{issue.room.id}})</span></div>
            <div style="color:var(--text-muted); font-size:10px;">${{issue.label}} — ${{issue.desc}}</div>
          </div>
          <div style="display:flex; gap:6px; align-items:center;">
            <span style="font-size:10px; background:rgba(0,0,0,0.3); padding:2px 6px; border-radius:4px;">Node: ${{issue.node ? issue.node.id : 'unknown'}}</span>
            <button class="btn btn-primary" style="font-size:10px; padding:3px 7px;">Focus Room 🔍</button>
          </div>
        `;
        row.addEventListener('click', () => {{
          if (issue.node && state.activeNode !== issue.node.id) {{
            selectNode(issue.node.id);
          }}
          selectRoom(issue.room.id);
          centerOnRoom(issue.room.id);
        }});
        auditTrayContent.appendChild(row);
      }});
    }}

    function centerOnRoom(roomId) {{
      const room = state.roomMap.get(roomId);
      if (!room) return;
      const node = getRoomNode(room);
      const pos = room.pos || [0, 0];
      const offset = (state.activeNode === 'all_nodes' && node) ? getNodeOffset(node.id) : {{ x: 0, y: 0 }};
      const rx = pos[0] + offset.x;
      const ry = pos[1] + offset.y;

      const vpWidth = canvasViewport.clientWidth || window.innerWidth - 360;
      const vpHeight = canvasViewport.clientHeight || window.innerHeight - 150;

      state.panX = vpWidth / 2 - rx * state.gridSpacing * state.zoom;
      state.panY = vpHeight / 2 + ry * state.gridSpacing * state.zoom;
      render();
    }}

    // Save & Export Operations
    async function saveToProject() {{
      if (!state.isServerLive) {{
        showToast('Server offline! Running in file mode. Use "Export JSON" or start serve_world_studio.py.');
        return;
      }}

      try {{
        showToast('Saving to project assets & Kotlin layouts...');
        const payload = {{
          rooms: state.rooms,
          hub_nodes: state.hubNodes,
          hub_layouts: state.hubLayouts
        }};

        const res = await fetch('/api/save', {{
          method: 'POST',
          headers: {{ 'Content-Type': 'application/json' }},
          body: JSON.stringify(payload)
        }});

        if (res.ok) {{
          const result = await res.json();
          showToast(`✅ ${{result.message}} (Backup: ${{result.backup}})`, 4000);
        }} else {{
          throw new Error('Server returned ' + res.status);
        }}
      }} catch (err) {{
        console.error('Save failed:', err);
        showToast('❌ Save failed: ' + err.message, 5000);
      }}
    }}

    function exportJson() {{
      const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(state.rooms, null, 2));
      const downloadAnchor = document.createElement('a');
      downloadAnchor.setAttribute('href', dataStr);
      downloadAnchor.setAttribute('download', 'rooms.json');
      document.body.appendChild(downloadAnchor);
      downloadAnchor.click();
      downloadAnchor.remove();
      showToast('Exported rooms.json');
    }}

    function escapeHtml(str) {{
      return String(str || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    }}

    // Boot
    window.addEventListener('DOMContentLoaded', init);
  </script>
</body>
</html>"""

    output_path = BASE_DIR / "world_graph_studio.html"
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(html_content)

    print(f"Successfully generated {output_path} ({len(html_content)} bytes)")

if __name__ == "__main__":
    build()
