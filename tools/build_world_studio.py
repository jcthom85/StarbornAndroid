#!/usr/bin/env python3
"""
Build script for Starborn World Graph & Navigation Studio.
Generates tools/world_graph_studio.html with bundled data fallback,
node-level room map architecture, and live sync capabilities.
"""

import json
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = BASE_DIR.parent
ASSETS_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets"

def build():
    rooms = json.load(open(ASSETS_DIR / "rooms.json", encoding="utf-8"))
    hub_nodes = json.load(open(ASSETS_DIR / "hub_nodes.json", encoding="utf-8"))
    hubs = json.load(open(ASSETS_DIR / "hubs.json", encoding="utf-8"))
    worlds = json.load(open(ASSETS_DIR / "worlds.json", encoding="utf-8"))

    bundled_data = {
        "rooms": rooms,
        "hub_nodes": hub_nodes,
        "hubs": hubs,
        "worlds": worlds
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
      height: 50px;
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
      width: 154px;
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

    .node-header {{
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 4px;
      gap: 4px;
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
    }}

    .node-badges {{
      display: flex;
      flex-wrap: wrap;
      gap: 3px;
      font-size: 9px;
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
    .badge.external {{ color: #c084fc; border: 1px solid rgba(168, 85, 247, 0.4); }}

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

    /* FLOATING CANVAS CONTROLS */
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

      <div class="select-group">
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
    <!-- CANVAS VIEWPORT -->
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
        <button class="btn" id="btnToggleLabels" title="Toggle Detail Badges">🏷️ Details</button>
        <button class="btn btn-primary" id="btnCreateRoom" title="Add New Room to Node">➕ Add Room</button>
      </div>
    </div>

    <!-- RIGHT INSPECTOR DRAWER -->
    <div class="inspector-drawer collapsed" id="inspectorDrawer">
      <div class="drawer-header">
        <div class="drawer-title">
          <span>Room Inspector</span>
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

  <!-- FLOATING TOAST -->
  <div id="toast"></div>

  <!-- BUNDLED OFFLINE DATASET (Guarantees zero blank screen even when opened directly as file:///) -->
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
      roomMap: new Map(),
      nodeMap: new Map(),
      hubMap: new Map(),
      activeWorld: 'world_1',
      activeHub: 'hub_1_homestead',
      activeNode: 'pit', // node id OR 'all_nodes'
      selectedRoomId: null,
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
      connectingFrom: null, // {{ roomId, direction }}
      history: [],
      gridSpacing: 180,
      isServerLive: false,
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

    // Initialization
    async function init() {{
      resizeCanvas();
      window.addEventListener('resize', resizeCanvas);
      setupCanvasEvents();
      setupToolbarEvents();

      // Load bundled data first to guarantee immediate display
      try {{
        const bundledText = document.getElementById('bundledData').textContent;
        const bundled = JSON.parse(bundledText);
        state.rooms = bundled.rooms || [];
        state.hubNodes = bundled.hubNodes || bundled.hub_nodes || [];
        state.hubs = bundled.hubs || [];
        state.worlds = bundled.worlds || [];
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
      state.history.push(JSON.stringify(state.rooms));
      if (state.history.length > 30) state.history.shift();
    }}

    function undo() {{
      if (state.history.length > 0) {{
        state.rooms = JSON.parse(state.history.pop());
        indexData();
        render();
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

    // Node Spatial Offsets for "All Nodes (Dispersed)" view
    function getNodeOffset(nodeId) {{
      if (!nodeId || nodeId === 'all_nodes') return {{ x: 0, y: 0 }};
      const node = state.nodeMap.get(nodeId);
      if (!node) return {{ x: 0, y: 0 }};

      // Arrange nodes in the hub in a structured cluster
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

      // If search query is active, search across everything
      if (q) {{
        return state.rooms.filter(r => {{
          const matchTitle = (r.title || '').toLowerCase().includes(q);
          const matchId = r.id.toLowerCase().includes(q);
          return matchTitle || matchId;
        }});
      }}

      // Specific Node Selected (Focused Node Map View)
      if (state.activeNode !== 'all_nodes') {{
        const node = state.nodeMap.get(state.activeNode);
        if (node && node.rooms) {{
          const roomSet = new Set(node.rooms);
          if (node.entry_room) roomSet.add(node.entry_room);
          return state.rooms.filter(r => roomSet.has(r.id));
        }}
      }}

      // All Nodes in Hub or World Selected
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
        // Pick first hub if activeHub not in relevant list
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

      // Add "All Nodes (Dispersed View)" option
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

      document.querySelectorAll('.node-pill').forEach(pill => {{
        pill.classList.remove('active');
      }});
      populateNodeSelector();
      fitView();
      render();
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

    // Coordinate conversions
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

    // Fit View
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
      renderGrid();
      nodesLayer.innerHTML = '';
      islandsLayer.innerHTML = '';
      linksSvg.innerHTML = '';

      const visibleRooms = getVisibleRooms();
      const visibleSet = new Set(visibleRooms.map(r => r.id));

      // Update Scope Label
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

      // Audit Counters
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
          islandBox.title = `Click to focus on node: ${{node.title || node.id}}`;

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
        nodeEl.style.left = (screenPos.x - 77) + 'px';
        nodeEl.style.top = (screenPos.y - 42) + 'px';
        nodeEl.style.transform = `scale(${{Math.min(Math.max(state.zoom, 0.7), 1.15)}})`;

        if (state.selectedRoomId === room.id) nodeEl.classList.add('selected');

        const collisionKey = (nodeId || 'unknown') + ':' + pos.join(',');
        if ((inNodeCoordCounts.get(collisionKey) || 0) > 1) {{
          nodeEl.classList.add('collision');
          inNodeCollisions++;
        }}

        const isEntry = node && (node.entry_room === room.id);
        if (isEntry) nodeEl.classList.add('is-entry');

        // Check Room Violations
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

          // If target is outside visible set, render as an outbound exit marker
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

    // SVG Drawing Helpers
    function drawConnectionLine(p1, p2, dir, isNSEW, isReciprocal, isCrossNode) {{
      const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      line.setAttribute('x1', p1.x);
      line.setAttribute('y1', p1.y);
      line.setAttribute('x2', p2.x);
      line.setAttribute('y2', p2.y);

      if (!isNSEW) {{
        // Red: Non-Cardinal Violation
        line.setAttribute('stroke', '#ef4444');
        line.setAttribute('stroke-width', '3');
        line.setAttribute('stroke-dasharray', '4,4');
      }} else if (isCrossNode) {{
        // Purple: Cross-Node Inter-facility Transit
        line.setAttribute('stroke', '#c084fc');
        line.setAttribute('stroke-width', '2.5');
        line.setAttribute('stroke-dasharray', '6,3');
      }} else if (isReciprocal) {{
        // Green: Solid Strict Reciprocal Swipe
        line.setAttribute('stroke', '#10b981');
        line.setAttribute('stroke-width', '2.5');
      }} else {{
        // Amber: Asymmetric One-Way Drop
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

    // Node Dragging
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

    // Canvas Events (Pan & Zoom)
    function setupCanvasEvents() {{
      canvasViewport.addEventListener('mousedown', (e) => {{
        if (e.target.closest('.room-node') || e.target.closest('.canvas-hud')) return;
        state.isPanning = true;
        state.panStartX = e.clientX - state.panX;
        state.panStartY = e.clientY - state.panY;
      }});

      window.addEventListener('mousemove', (e) => {{
        if (state.isDraggingNode) {{
          onNodeDrag(e);
        }} else if (state.isPanning) {{
          state.panX = e.clientX - state.panStartX;
          state.panY = e.clientY - state.panStartY;
          render();
        }}
      }});

      window.addEventListener('mouseup', () => {{
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
      document.getElementById('btnToggleLabels').addEventListener('click', () => {{
        state.showDetails = !state.showDetails;
        render();
      }});
      document.getElementById('btnCreateRoom').addEventListener('click', createNewRoom);
    }}

    // Toolbar & Filter Events
    function setupToolbarEvents() {{
      // World tabs
      document.querySelectorAll('.world-tab').forEach(tab => {{
        tab.addEventListener('click', () => {{
          document.querySelectorAll('.world-tab').forEach(t => t.classList.remove('active'));
          tab.classList.add('active');
          state.activeWorld = tab.dataset.world;
          populateHubSelector();
          fitView();
        }});
      }});

      // Hub dropdown
      document.getElementById('hubFilterSelect').addEventListener('change', (e) => {{
        state.activeHub = e.target.value;
        populateNodeSelector();
        fitView();
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

      drawerContent.innerHTML = `
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

      // Attach dynamic listeners to inputs
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

    // Conversion of non-NSEW exits into bolded actionable keywords
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

      // Check within scope
      visibleRooms.forEach(room => {{
        const node = getRoomNode(room);
        const conns = room.connections || {{}};

        // Non-cardinal
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
        showToast('Saving to project assets...');
        const payload = {{
          rooms: state.rooms,
          hub_nodes: state.hubNodes
        }};

        const res = await fetch('/api/save', {{
          method: 'POST',
          headers: {{ 'Content-Type': 'application/json' }},
          body: JSON.stringify(payload)
        }});

        if (res.ok) {{
          const result = await res.json();
          showToast(`✅ Saved ${{state.rooms.length}} rooms! Backup created: ${{result.backup}}`, 4000);
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
