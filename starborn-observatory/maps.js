const ASSET_ROOT = '../app/src/main/assets/';
const IMAGE_ROOT = '../world_assets/src/main/assets/';
const DIRECTIONS = {
  north: 'N', south: 'S', east: 'E', west: 'W',
  northeast: 'NE', northwest: 'NW', southeast: 'SE', southwest: 'SW',
  up: 'UP', down: 'DOWN'
};
const OPPOSITES = {
  north: 'south', south: 'north', east: 'west', west: 'east',
  northeast: 'southwest', southwest: 'northeast',
  northwest: 'southeast', southeast: 'northwest', up: 'down', down: 'up'
};
const HUB_DRAFT_KEY = 'starborn-observatory.hub-node-positions.v1';
const app = document.querySelector('#app');
const data = { worlds: [], hubs: [], nodes: [], rooms: [] };
const selected = { world: null, hub: null, node: null, room: null, viewMode: 'rooms' };
let draftPositions = readHubDraft();

function readHubDraft() {
  try {
    const value = JSON.parse(localStorage.getItem(HUB_DRAFT_KEY) || '{}');
    return value && typeof value === 'object' && !Array.isArray(value) ? value : {};
  } catch {
    return {};
  }
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[character]);
}

function directionName(value) {
  const normalized = String(value ?? '').toLowerCase();
  return DIRECTIONS[normalized] || normalized.toUpperCase();
}

function isCardinal(value) {
  return ['north', 'south', 'east', 'west'].includes(String(value ?? '').toLowerCase());
}

function selectControl(label, key, entries, current) {
  const options = entries.map(entry =>
    `<option value="${escapeHtml(entry.id)}" ${entry.id === current ? 'selected' : ''}>${escapeHtml(entry.title || entry.id)}</option>`
  ).join('');
  return `<label>${label}<select data-select="${key}">${options}</select></label>`;
}

function clampPosition(value) {
  const number = Number(value);
  return Number.isFinite(number) ? Math.max(0, Math.min(1, number)) : 0.5;
}

function hubNodePosition(node) {
  const saved = draftPositions[node.id];
  const source = saved || node.pos_hint || {};
  return { x: clampPosition(source.center_x ?? source.x), y: clampPosition(source.center_y ?? source.y) };
}

function changedHubPositions(nodes = data.nodes) {
  return nodes.filter(node => {
    const saved = draftPositions[node.id];
    if (!saved) return false;
    const original = node.pos_hint || {};
    return Math.abs(clampPosition(saved.x) - clampPosition(original.center_x)) > 0.0001 ||
      Math.abs(clampPosition(saved.y) - clampPosition(original.center_y)) > 0.0001;
  });
}

function hubScreenEditor(hub, nodes) {
  if (!hub) return '<div class="map-empty">Select a hub to view its screen.</div>';
  const changedCount = changedHubPositions(nodes).length;
  const totalChangedCount = changedHubPositions().length;
  const background = hub.background_image ? IMAGE_ROOT + hub.background_image : '';
  const markers = nodes.map(node => {
    const position = hubNodePosition(node);
    const icon = node.icon_image
      ? `<img src="${escapeHtml(IMAGE_ROOT + node.icon_image)}" alt="" draggable="false">`
      : '<span class="hub-node-fallback" aria-hidden="true"></span>';
    return `<button class="hub-node-marker ${node.id === selected.node ? 'is-selected' : ''}" type="button"
      data-editor-node="${escapeHtml(node.id)}" data-x="${position.x}" data-y="${position.y}"
      style="left:${position.x * 100}%;top:${position.y * 100}%" aria-label="Drag ${escapeHtml(node.title || node.id)} to adjust placement">
      ${icon}<span class="hub-node-label">${escapeHtml(node.title || node.id)}</span>
    </button>`;
  }).join('');
  return `<div class="hub-editor">
    <div class="hub-editor-heading">
      <div><p class="eyebrow">HUB SCREEN</p><h2>${escapeHtml(hub.title)}</h2><p>${nodes.length} nodes · drag a node to adjust its ground position</p></div>
      <div class="hub-editor-actions"><button type="button" class="map-action" data-reset-draft ${totalChangedCount ? '' : 'disabled'}>Reset draft</button>
        <button type="button" class="map-action primary" data-export-json ${totalChangedCount ? '' : 'disabled'}>Download updated JSON</button></div>
    </div>
    <div class="hub-screen-stage"><div class="hub-screen-canvas" data-hub-canvas>
      <img class="hub-screen-background" src="${escapeHtml(background)}" alt="${escapeHtml(hub.title)} hub screen">
      <div class="hub-node-layer">${markers}</div>
    </div></div>
    <p class="hub-draft-status" data-draft-status>${changedCount} adjusted on this screen · ${totalChangedCount} total · draft saved in this browser</p>
    <p class="map-muted">Positions start from the current Android layout. Asset-backed destinations can be adjusted here; special travel shortcuts keep their fixed runtime positions. Download the updated <code>hub_nodes.json</code> to apply this draft to the game data.</p>
  </div>`;
}

function installHubEditorHandlers() {
  const canvas = app.querySelector('[data-hub-canvas]');
  if (!canvas) return;

  let drag = null;
  const markers = [...canvas.querySelectorAll('[data-editor-node]')];
  const persist = () => {
    try { localStorage.setItem(HUB_DRAFT_KEY, JSON.stringify(draftPositions)); } catch { /* Browser storage can be unavailable. */ }
  };
  const updateDraftStatus = () => {
    const changedCount = changedHubPositions(data.nodes.filter(node => node.hub_id === selected.hub)).length;
    const totalChangedCount = changedHubPositions().length;
    const status = app.querySelector('[data-draft-status]');
    if (status) status.textContent = `${changedCount} adjusted on this screen · ${totalChangedCount} total · draft saved in this browser`;
    app.querySelector('[data-reset-draft]')?.toggleAttribute('disabled', totalChangedCount === 0);
    app.querySelector('[data-export-json]')?.toggleAttribute('disabled', totalChangedCount === 0);
  };

  markers.forEach(marker => {
    marker.addEventListener('pointerdown', event => {
      if (event.button !== 0) return;
      marker.dataset.didDrag = 'false';
      const rect = canvas.getBoundingClientRect();
      const pointerX = (event.clientX - rect.left) / rect.width;
      const pointerY = (event.clientY - rect.top) / rect.height;
      drag = {
        marker,
        pointerId: event.pointerId,
        rect,
        offsetX: pointerX - Number(marker.dataset.x),
        offsetY: pointerY - Number(marker.dataset.y),
        x: Number(marker.dataset.x),
        y: Number(marker.dataset.y),
        startX: Number(marker.dataset.x),
        startY: Number(marker.dataset.y),
        moved: false
      };
      marker.setPointerCapture(event.pointerId);
      marker.classList.add('is-dragging');
      event.preventDefault();
    });
    marker.addEventListener('pointermove', event => {
      if (!drag || drag.marker !== marker || drag.pointerId !== event.pointerId) return;
      const rawX = (event.clientX - drag.rect.left) / drag.rect.width - drag.offsetX;
      const rawY = (event.clientY - drag.rect.top) / drag.rect.height - drag.offsetY;
      const x = clampPosition(rawX);
      const y = clampPosition(rawY);
      drag.moved ||= Math.abs(x - drag.startX) + Math.abs(y - drag.startY) > 0.002;
      drag.x = x;
      drag.y = y;
      marker.dataset.x = String(x);
      marker.dataset.y = String(y);
      marker.style.left = `${x * 100}%`;
      marker.style.top = `${y * 100}%`;
    });
    const finishDrag = event => {
      if (!drag || drag.marker !== marker || drag.pointerId !== event.pointerId) return;
      marker.classList.remove('is-dragging');
      marker.dataset.didDrag = drag.moved ? 'true' : 'false';
      if (drag.moved) {
        draftPositions[marker.dataset.editorNode] = { x: drag.x, y: drag.y };
        persist();
        updateDraftStatus();
      }
      drag = null;
    };
    marker.addEventListener('pointerup', finishDrag);
    marker.addEventListener('pointercancel', finishDrag);
    marker.addEventListener('click', event => {
      if (marker.dataset.didDrag === 'true') {
        marker.dataset.didDrag = 'false';
        event.preventDefault();
        return;
      }
      selected.node = marker.dataset.editorNode;
      markers.forEach(item => item.classList.toggle('is-selected', item === marker));
      app.querySelectorAll('[data-node]').forEach(item => item.classList.toggle('is-selected', item.dataset.node === selected.node));
    });
  });
}

function downloadHubNodes() {
  const exported = data.nodes.map(node => {
    const position = hubNodePosition(node);
    return { ...node, pos_hint: { ...(node.pos_hint || {}), center_x: position.x, center_y: position.y } };
  });
  const blob = new Blob([`${JSON.stringify(exported, null, 2)}\n`], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'hub_nodes.json';
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function connectionEdges(rooms) {
  const roomIds = new Set(rooms.map(room => room.id));
  const edgesByPair = new Map();
  const external = [];

  for (const room of rooms) {
    for (const [direction, destinationId] of Object.entries(room.connections || {})) {
      const destination = data.roomsById.get(destinationId);
      const actionLabel = room.special_exits?.[direction] || room.special_exits?.[direction.toLowerCase()] || '';
      if (!roomIds.has(destinationId)) {
        external.push({ room, direction, destination, destinationId, actionLabel, isNonCardinal: !isCardinal(direction) });
        continue;
      }

      const key = [room.id, destinationId].sort().join('::');
      if (!edgesByPair.has(key)) edgesByPair.set(key, { rooms: [room, destination], entries: [] });
      edgesByPair.get(key).entries.push({ room, destinationId, direction, actionLabel });
    }
  }

  const edges = [...edgesByPair.values()].map(edge => {
    const entry = edge.entries[0];
    const reverseDirection = OPPOSITES[String(entry.direction).toLowerCase()];
    const reverseEntry = edge.entries.find(candidate =>
      candidate.room.id === entry.destinationId && candidate.direction.toLowerCase() === reverseDirection
    );
    const isReciprocal = Boolean(reverseEntry);
    const specialLabels = [...new Set(edge.entries.map(candidate => candidate.actionLabel).filter(Boolean))];
    const directions = isReciprocal
      ? `${directionName(entry.direction)} ↔ ${directionName(reverseEntry.direction)}`
      : directionName(entry.direction);
    return {
      ...edge,
      from: entry.room,
      to: data.roomsById.get(entry.destinationId),
      directions,
      isReciprocal,
      isSpecial: specialLabels.length > 0,
      isNonCardinal: edge.entries.some(candidate => !isCardinal(candidate.direction)),
      isUnlabeledNonCardinal: edge.entries.some(candidate => !isCardinal(candidate.direction)) && specialLabels.length === 0,
      actionLabel: specialLabels.join(' · ')
    };
  });

  return { edges, external };
}

function layoutFor(rooms) {
  const positioned = rooms.map(room => ({
    room,
    x: Number(room.pos?.[0]) || 0,
    y: Number(room.pos?.[1]) || 0
  }));
  if (!positioned.length) return { svg: '<div class="map-empty">This node has no room records.</div>', external: [] };

  const stepX = 218;
  const stepY = 154;
  const tileWidth = 174;
  const tileHeight = 74;
  const paddingX = 112;
  const paddingY = 76;
  const minX = Math.min(...positioned.map(item => item.x));
  const maxX = Math.max(...positioned.map(item => item.x));
  const minY = Math.min(...positioned.map(item => item.y));
  const maxY = Math.max(...positioned.map(item => item.y));
  const svgWidth = Math.max(440, paddingX * 2 + (maxX - minX) * stepX);
  const svgHeight = Math.max(250, paddingY * 2 + (maxY - minY) * stepY);
  const centers = new Map(positioned.map(({ room, x, y }) => [room.id, {
    x: paddingX + (x - minX) * stepX,
    y: paddingY + (maxY - y) * stepY
  }]));
  const { edges, external } = connectionEdges(rooms);

  const linkMarkup = edges.map(edge => {
    const from = centers.get(edge.from.id);
    const to = centers.get(edge.to.id);
    const midX = (from.x + to.x) / 2;
    const midY = (from.y + to.y) / 2;
    const directionLabel = escapeHtml(edge.directions);
    const linkTitle = edge.actionLabel ? `${edge.directions}: ${edge.actionLabel}` : edge.directions;
    const marker = edge.isReciprocal ? '' : ' marker-end="url(#room-arrow)"';
    return `<g class="layout-link ${edge.isSpecial ? 'is-special' : ''} ${edge.isUnlabeledNonCardinal ? 'is-unlabeled' : ''}" aria-label="${escapeHtml(linkTitle)}">
      <title>${escapeHtml(linkTitle)}</title>
      <line x1="${from.x}" y1="${from.y}" x2="${to.x}" y2="${to.y}"${marker}></line>
      <rect class="link-label-bg" x="${midX - 28}" y="${midY - 10}" width="56" height="20" rx="6"></rect>
      <text class="link-label" x="${midX}" y="${midY + 4}" text-anchor="middle">${directionLabel}</text>
    </g>`;
  }).join('');

  const roomMarkup = positioned.map(({ room, x, y }) => {
    const center = centers.get(room.id);
    const title = room.title || room.id;
    const shownTitle = title.length > 23 ? `${title.slice(0, 21)}…` : title;
    const isSelected = room.id === selected.room;
    return `<g class="map-room ${isSelected ? 'is-selected' : ''}" data-room="${escapeHtml(room.id)}" role="button" tabindex="0" aria-label="Select ${escapeHtml(title)}">
      <title>${escapeHtml(title)} (${x}, ${y})</title>
      <rect x="${center.x - tileWidth / 2}" y="${center.y - tileHeight / 2}" width="${tileWidth}" height="${tileHeight}" rx="8"></rect>
      <text class="room-title" x="${center.x}" y="${center.y - 3}" text-anchor="middle">${escapeHtml(shownTitle)}</text>
      <text class="room-position" x="${center.x}" y="${center.y + 19}" text-anchor="middle">${x}, ${y}</text>
    </g>`;
  }).join('');

  const svg = `<div class="layout-scroll"><svg class="room-layout" viewBox="0 0 ${svgWidth} ${svgHeight}" role="group" aria-label="Room layout for ${escapeHtml(data.nodesById.get(selected.node)?.title || 'selected node')}">
    <defs><marker id="room-arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" class="arrow-head"></path></marker></defs>
    <g class="layout-links">${linkMarkup}</g>
    <g class="layout-rooms">${roomMarkup}</g>
  </svg></div>`;
  return { svg, external };
}

function roomDetails(room, roomsInNode) {
  if (!room) return '<div class="card map-message">Select a room to inspect its exits and details.</div>';
  const roomIds = new Set(roomsInNode.map(item => item.id));
  const connections = Object.entries(room.connections || {});
  const connectionList = connections.length ? connections.map(([direction, destinationId]) => {
    const destination = data.roomsById.get(destinationId);
    const actionLabel = room.special_exits?.[direction] || room.special_exits?.[direction.toLowerCase()] || '';
    const isExternal = !roomIds.has(destinationId);
    const isNonCardinal = !isCardinal(direction);
    const isSpecial = Boolean(actionLabel) || isNonCardinal;
    return `<li class="connection-row ${isSpecial ? 'is-special' : ''} ${isExternal ? 'is-external' : ''}">
      <span class="connection-direction">${escapeHtml(directionName(direction))}</span>
      <span class="connection-copy"><strong>${escapeHtml(destination?.title || destinationId)}</strong>
        ${isExternal ? '<small>Leads to another node or area</small>' : ''}
        ${actionLabel ? `<small class="path-action">${escapeHtml(actionLabel)}</small>` : ''}
        ${isNonCardinal && !actionLabel ? '<small class="warning">Non-cardinal exit has no special path label</small>' : ''}
        ${!destination ? '<small class="warning">Destination ID not found in room data</small>' : ''}
      </span>
    </li>`;
  }).join('') : '<li class="map-muted">No authored exits.</li>';
  const image = room.background_image
    ? `<img class="room-art" src="${escapeHtml(IMAGE_ROOT + room.background_image)}" alt="${escapeHtml(room.title || room.id)} artwork">`
    : '<div class="room-art-placeholder">No room artwork</div>';
  const list = (values) => Array.isArray(values) && values.length
    ? values.map(value => `<span class="detail-chip">${escapeHtml(value)}</span>`).join(' ')
    : '<span class="map-muted">None</span>';

  return `<div class="card room-details">
    <div class="room-detail-heading">${image}<div class="room-detail-intro"><p class="eyebrow">SELECTED ROOM</p><h2>${escapeHtml(room.title || room.id)}</h2><p class="room-id">${escapeHtml(room.id)}</p><p class="room-description">${escapeHtml(room.description || 'No description authored.')}</p></div></div>
    <div class="room-detail-copy">
      <h3>Connections</h3><ul class="connection-list">${connectionList}</ul>
      <div class="room-content-grid">
        <div><h3>NPCs</h3><div class="detail-chips">${list(room.npcs)}</div></div>
        <div><h3>Items</h3><div class="detail-chips">${list(room.items)}</div></div>
        <div><h3>Enemies</h3><div class="detail-chips">${list(room.enemies)}</div></div>
      </div>
    </div>
  </div>`;
}

function render() {
  const hubs = data.hubs.filter(hub => hub.world_id === selected.world);
  selected.hub = hubs.some(hub => hub.id === selected.hub) ? selected.hub : hubs[0]?.id || null;
  const nodes = data.nodes.filter(node => node.hub_id === selected.hub);
  selected.node = nodes.some(node => node.id === selected.node) ? selected.node : nodes[0]?.id || null;
  const node = data.nodesById.get(selected.node);
  const nodeRooms = (node?.rooms || []).map(id => data.roomsById.get(id)).filter(Boolean);
  selected.room = nodeRooms.some(room => room.id === selected.room) ? selected.room : nodeRooms[0]?.id || null;
  const room = data.roomsById.get(selected.room);
  const hub = data.hubsById.get(selected.hub);
  const { svg, external } = layoutFor(nodeRooms);
  const externalMarkup = external.length ? `<section class="external-exits">
    <div class="external-heading"><div><p class="eyebrow">NODE TRANSITIONS</p><h3>Paths to other nodes</h3></div><span>${external.length} exits</span></div>
    <div class="external-list">${external.map(exit => `<button class="external-exit ${exit.actionLabel ? 'is-special' : ''} ${exit.isNonCardinal && !exit.actionLabel ? 'is-unlabeled' : ''}" data-room="${escapeHtml(exit.room.id)}">
      <span class="connection-direction">${escapeHtml(directionName(exit.direction))}</span>
      <span><strong>${escapeHtml(exit.room.title || exit.room.id)}</strong> → ${escapeHtml(exit.destination?.title || exit.destinationId)}
      ${exit.actionLabel ? `<small>${escapeHtml(exit.actionLabel)}</small>` : ''}</span>
      ${exit.isNonCardinal && !exit.actionLabel ? '<small class="warning">No special path label</small>' : ''}
    </button>`).join('')}</div>
  </section>` : '';

  const hubScreenMode = selected.viewMode === 'hub';
  app.innerHTML = `<div class="maps-workspace">
    <section class="card maps-controls ${hubScreenMode ? 'is-hub-mode' : ''}" aria-label="Map filters">
      ${selectControl('WORLD', 'world', data.worlds, selected.world)}
      ${selectControl('HUB', 'hub', hubs, selected.hub)}
      ${hubScreenMode ? '' : `${selectControl('NODE', 'node', nodes, selected.node)}${selectControl('ROOM', 'room', nodeRooms, selected.room)}`}
    </section>
    <div class="maps-content">
      <aside class="card maps-sidebar">
        ${hub ? `<img class="map-art" src="${escapeHtml(IMAGE_ROOT + (hub.background_image || ''))}" alt="${escapeHtml(hub.title)} artwork"><h2>${escapeHtml(hub.title)}</h2><p class="hub-description">${escapeHtml(hub.description || '')}</p>` : '<h2>No hub data</h2>'}
        <div class="node-list-heading"><h3>Nodes in this hub</h3><span>${nodes.length}</span></div>
        <div class="node-map">${nodes.map(item => `<button class="node-button ${item.id === selected.node ? 'is-selected' : ''}" data-node="${escapeHtml(item.id)}"><strong>${escapeHtml(item.title || item.id)}</strong><small>${(item.rooms || []).length} rooms</small></button>`).join('') || '<p class="map-muted">No nodes in this hub.</p>'}</div>
      </aside>
      <div class="maps-main">
        <section class="card layout-card">
          <div class="map-mode-bar"><div><p class="eyebrow">MAP VIEW</p><h2>${hubScreenMode ? 'Hub screen' : 'Room layout'}</h2></div>
            <div class="map-mode-toggle" role="group" aria-label="Choose map view">
              <button type="button" data-view-mode="rooms" class="${hubScreenMode ? '' : 'is-active'}" aria-pressed="${!hubScreenMode}">Room layout</button>
              <button type="button" data-view-mode="hub" class="${hubScreenMode ? 'is-active' : ''}" aria-pressed="${hubScreenMode}">Hub screen</button>
            </div>
          </div>
          ${hubScreenMode ? hubScreenEditor(hub, nodes) : `<div class="layout-heading"><div><p class="eyebrow">ROOM LAYOUT</p><h2>${escapeHtml(node?.title || 'Select a node')}</h2><p>${nodeRooms.length} rooms · authored room positions · north is up</p></div>
            <div class="map-legend"><span><i class="legend-line"></i>Standard connection</span><span><i class="legend-line special"></i>Named special path</span><span><i class="legend-line unlabeled"></i>Unlabeled non-cardinal</span><span><i class="legend-dot"></i>Selected room</span></div>
          </div>${svg}${externalMarkup}`}
        </section>
        ${hubScreenMode ? '' : roomDetails(room, nodeRooms)}
      </div>
    </div>
  </div>`;

  app.querySelectorAll('[data-select]').forEach(control => control.addEventListener('change', event => {
    const key = event.currentTarget.dataset.select;
    selected[key] = event.currentTarget.value || null;
    if (key === 'world') selected.hub = selected.node = selected.room = null;
    if (key === 'hub') selected.node = selected.room = null;
    if (key === 'node') selected.room = null;
    render();
  }));
  app.querySelectorAll('[data-node]').forEach(button => button.addEventListener('click', () => {
    selected.node = button.dataset.node;
    selected.room = null;
    render();
  }));
  app.querySelectorAll('[data-view-mode]').forEach(button => button.addEventListener('click', () => {
    selected.viewMode = button.dataset.viewMode;
    render();
  }));
  app.querySelector('[data-reset-draft]')?.addEventListener('click', () => {
    draftPositions = {};
    try { localStorage.removeItem(HUB_DRAFT_KEY); } catch { /* Browser storage can be unavailable. */ }
    render();
  });
  app.querySelector('[data-export-json]')?.addEventListener('click', downloadHubNodes);
  installHubEditorHandlers();
  app.querySelectorAll('[data-room]').forEach(button => {
    const chooseRoom = () => {
      selected.room = button.dataset.room;
      render();
    };
    button.addEventListener('click', chooseRoom);
    if (button.matches('.map-room')) button.addEventListener('keydown', event => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        chooseRoom();
      }
    });
  });
}

async function load() {
  try {
    const files = ['worlds', 'hubs', 'hub_nodes', 'rooms'];
    const values = await Promise.all(files.map(async file => {
      const response = await fetch(`${ASSET_ROOT}${file}.json`);
      if (!response.ok) throw new Error(`Could not load ${file}.json (${response.status})`);
      return response.json();
    }));
    files.forEach((file, index) => { data[file] = values[index]; });
    data.roomsById = new Map(data.rooms.map(room => [room.id, room]));
    data.nodesById = new Map(data.nodes.map(node => [node.id, node]));
    data.hubsById = new Map(data.hubs.map(hub => [hub.id, hub]));
    selected.world = data.worlds[0]?.id || null;
    render();
  } catch (error) {
    app.innerHTML = `<div class="card map-message map-error"><h2>Map data could not be loaded</h2><p>${escapeHtml(error.message)}</p><p>Run the local web server from the repository root so the asset paths resolve.</p></div>`;
  }
}

load();
