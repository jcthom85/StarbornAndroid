#!/usr/bin/env python3
"""
Starborn Room Editor – v4.7

New in this version:
• Color-coding by env (deterministic palette). Selected / connect-start rooms get a highlight border.
• Right-click on a room: Duplicate…, Delete, Center on this room.
• PNG Snapshot export of the map (mini-map and Large Map Designer). Choose width in pixels; height auto-matches.

Keeps from earlier:
• Large Map Designer (🗺️) with Add Mode, Connect Rooms, Fit, scope (All / Current Node).
• Hover summary card on room mouseover (title, description, items, enemies, NPCs).
• Background-image HOVER preview only on the Background Image field (robust paths from project root).
• Compact SQUARE nodes on the grid; drag (RMB), pan (LMB empty), zoom (wheel).
"""

from __future__ import annotations
import json, os, sys, copy
from typing import Dict, List, Optional, Set, Tuple

from PyQt5.QtCore import Qt, QPointF, QRectF, pyqtSignal, QEvent, QSize
from PyQt5.QtGui import (
    QPainter, QPen, QBrush, QColor, QCursor, QPixmap, QFontMetrics, QImage
)
from PyQt5.QtWidgets import (
    QApplication, QWidget, QHBoxLayout, QVBoxLayout, QTreeWidget, QTreeWidgetItem,
    QListWidget, QListWidgetItem, QLineEdit, QPushButton, QLabel, QGroupBox,
    QFormLayout, QPlainTextEdit, QScrollArea, QSpinBox, QComboBox,
    QInputDialog, QMessageBox, QDialog, QMenu, QAction, QFileDialog, QFrame, QCheckBox
)
from theme_kit import ThemeManager         # optional if you want per-editor theme flips
from data_core import detect_project_root, json_load, json_save, unique_id
from editor_undo import UndoManager
from ui_common import attach_status_bar, flash_status, attach_hotkeys, attach_list_context_menu, mark_invalid, clear_invalid
from editor_bus import goto as studio_goto, refresh_references as studio_refresh

# ---------- Visual tuning ----------
ROOM_SIZE = 56  # side length in px at zoom=1 (square)
GRID_STEP = ROOM_SIZE  # grid aligned to room size

# ---------- Env color palette ----------
# You can customize specific envs here; unknown envs get a stable HSL color via hashing.
ENV_COLORS_PRESET = {
    "forest": QColor(70, 140, 80),
    "cave": QColor(80, 80, 110),
    "desert": QColor(170, 130, 60),
    "city": QColor(80, 120, 160),
    "ice": QColor(120, 160, 200),
    "volcano": QColor(170, 70, 60),
    "space": QColor(110, 80, 150),
}

def _env_color(env: str) -> QColor:
    if not env:
        return QColor(70, 130, 180)
    key = (env or "").strip().lower()
    if key in ENV_COLORS_PRESET:
        return ENV_COLORS_PRESET[key]
    # Deterministic pseudo-random pastel via HSL
    h = (abs(hash(key)) % 360)
    s = 180  # 0..255
    l = 130  # 0..255
    c = QColor()
    c.setHsl(h, s, l)
    return c

# --------------------------------------------------------
# Utilities
# --------------------------------------------------------
def _safe_read_json(path: str, fallback):
    if not os.path.exists(path):
        return fallback
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
            return data
    except Exception as e:
        QMessageBox.critical(None, "Load Error", f"Failed to load {os.path.basename(path)}:\n{e}")
        return fallback

def _safe_write_json(path: str, data):
    try:
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=4, ensure_ascii=False)
        QMessageBox.information(None, "Saved", f"{os.path.basename(path)} saved successfully.")
        return True
    except Exception as e:
        QMessageBox.critical(None, "Save Error", f"Failed to save {os.path.basename(path)}:\n{e}")
        return False

def _node_room_key(node: dict) -> str:
    for k in ("rooms", "room_ids", "room_list"):
        if k in node and isinstance(node[k], list):
            return k
    return "rooms"

def _detect_project_root() -> str:
    if len(sys.argv) > 1 and os.path.isdir(sys.argv[1]):
        candidate = sys.argv[1]
        if os.path.exists(os.path.join(candidate, "rooms.json")):
            return candidate

    cwd = os.getcwd()
    if os.path.exists(os.path.join(cwd, "rooms.json")):
        return cwd

    here = os.path.dirname(os.path.abspath(__file__))
    parent = os.path.dirname(here)
    if os.path.exists(os.path.join(parent, "rooms.json")):
        return parent

    dlg = QFileDialog()
    dlg.setFileMode(QFileDialog.Directory)
    dlg.setOption(QFileDialog.ShowDirsOnly, True)
    dlg.setWindowTitle("Choose Starborn project root (contains rooms.json)")
    if dlg.exec_():
        picked = dlg.selectedFiles()[0]
        if os.path.exists(os.path.join(picked, "rooms.json")):
            return picked
    return cwd

# --------------------------------------------------------
# Hover helpers
# --------------------------------------------------------
class ImageHoverPreview(QFrame):
    def __init__(self, parent=None):
        super().__init__(parent, Qt.Tool | Qt.FramelessWindowHint | Qt.WindowStaysOnTopHint)
        self.setAttribute(Qt.WA_TransparentForMouseEvents, True)
        self.setStyleSheet("QFrame { background: rgba(0,0,0,200); border: 1px solid #333; }")
        self.label = QLabel(self)
        self.label.setAlignment(Qt.AlignCenter)
        self.label.setStyleSheet("QLabel { background: transparent; }")
        lay = QVBoxLayout(self)
        lay.setContentsMargins(6, 6, 6, 6)
        lay.addWidget(self.label)
        self.hide()

    def set_pixmap(self, pm: Optional[QPixmap]):
        if not pm or pm.isNull():
            self.hide()
            return
        max_w, max_h = 540, 320
        scaled = pm.scaled(max_w, max_h, Qt.KeepAspectRatio, Qt.SmoothTransformation)
        self.label.setPixmap(scaled)
        self.resize(scaled.width() + 12, scaled.height() + 12)

    def show_near(self, global_pos: QPointF):
        self.move(int(global_pos.x() + 16), int(global_pos.y() + 16))
        self.show()

class RoomHoverCard(QFrame):
    """Text hover card used when mousing over rooms on the map."""
    def __init__(self, parent=None):
        super().__init__(parent, Qt.Tool | Qt.FramelessWindowHint | Qt.WindowStaysOnTopHint)
        self.setAttribute(Qt.WA_TransparentForMouseEvents, True)
        self.setStyleSheet("""
            QFrame { background: rgba(12,12,12,230); border: 1px solid #333; border-radius: 6px; }
            QLabel { color: #e9e9e9; padding: 8px; }
        """)
        self.label = QLabel(self)
        self.label.setAlignment(Qt.AlignLeft | Qt.AlignTop)
        self.label.setWordWrap(True)
        self.label.setTextInteractionFlags(Qt.NoTextInteraction)
        lay = QVBoxLayout(self)
        lay.setContentsMargins(0, 0, 0, 0)
        lay.addWidget(self.label)
        self._max_w = 360
        self.hide()

    def set_text(self, text: str):
        self.label.setText(text)
        self.label.setFixedWidth(self._max_w)
        self.label.adjustSize()
        self.resize(self.label.sizeHint().width(), self.label.sizeHint().height())

    def show_near(self, global_pos: QPointF):
        self.move(int(global_pos.x() + 18), int(global_pos.y() + 18))
        self.show()

# --------------------------------------------------------
# MapView – draws rooms and handles interactions
# --------------------------------------------------------
class MapView(QWidget):
    connection_requested = pyqtSignal(str, str)   # emits (start_id, end_id)

    def __init__(self, rooms_by_id: Dict[str, dict], controller, parent=None):
        """
        controller must implement:
            - on_map_room_clicked(room_id: str)
            - on_room_pos_drag(room_id: str, x: float, y: float)
            - on_add_room_at(wx: float, wy: float)
            - on_duplicate_room(room_id: str)
            - on_delete_room(room_id: str)
            - export_snapshot(ids: Set[str])
        """
        super().__init__(parent)
        self.rooms_by_id = rooms_by_id
        self.controller = controller

        self.visible_room_ids: Set[str] = set(rooms_by_id.keys())
        self.selected_id: Optional[str] = None

        # view state
        self.zoom = 1.0
        self.offset = QPointF(0, 0)
        self._dragging = False
        self._drag_start = QPointF(0, 0)

        # drag a room (RMB)
        self._dragging_room_id: Optional[str] = None
        self._last_mouse = QPointF(0, 0)

        # connect mode
        self.in_connection_mode = False
        self.connection_start_id: Optional[str] = None
        self.live_connection_end_pos: Optional[QPointF] = None

        # add mode
        self.in_add_mode = False

        # hover card
        self._hover_id: Optional[str] = None
        self.hover_card = RoomHoverCard(self)

        self.setMinimumHeight(320)
        self.setMouseTracking(True)

    # --------------- external API ---------------
    def set_visible_ids(self, ids: Set[str]):
        self.visible_room_ids = set(ids) if ids else set()
        self.update()

    def set_connection_mode(self, enabled: bool):
        self.in_connection_mode = enabled
        self.connection_start_id = None
        self.live_connection_end_pos = None
        if enabled:
            self.setCursor(QCursor(Qt.CrossCursor))
        else:
            self.unsetCursor()
        self.update()

    def set_add_mode(self, enabled: bool):
        self.in_add_mode = enabled
        if enabled:
            self.setCursor(QCursor(Qt.PointingHandCursor))
        else:
            self.unsetCursor()
        self.update()

    def fit_to_ids(self, ids: Optional[Set[str]] = None, padding: float = 0.2):
        ids = ids if ids is not None else self.visible_room_ids
        pts = []
        for rid in ids:
            r = self.rooms_by_id.get(rid)
            if not r: continue
            pos = r.get("pos", [0, 0])
            pts.append((float(pos[0]), float(pos[1])))

        if not pts:
            return

        xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
        minx, maxx = min(xs), max(xs)
        miny, maxy = min(ys), max(ys)

        w_world = max(1.0, (maxx - minx) + 2.5)
        h_world = max(1.0, (maxy - miny) + 2.5)

        view_w = max(1, self.width()); view_h = max(1, self.height())
        z_x = (view_w * (1.0 - padding)) / (w_world * 100.0)
        z_y = (view_h * (1.0 - padding)) / (h_world * 100.0)
        self.zoom = max(0.1, min(5.0, min(z_x, z_y)))

        cx = (minx + maxx) * 0.5
        cy = (miny + maxy) * 0.5
        sx = cx * 100.0 * self.zoom
        sy = -cy * 100.0 * self.zoom
        self.offset = QPointF(-sx + self.width() * 0.5, -sy + self.height() * 0.5)
        self.update()

    # --------------- coordinate helpers ---------------
    def _get_room_center(self, room_id: str) -> Optional[QPointF]:
        r = self.rooms_by_id.get(room_id)
        if not r:
            return None
        pos = r.get("pos", [0, 0])
        cx = (pos[0] * 100) * self.zoom + self.offset.x() + self.width() * 0.5
        cy = (-pos[1] * 100) * self.zoom + self.offset.y() + self.height() * 0.5
        return QPointF(cx, cy)

    def screen_to_world(self, px: float, py: float) -> Tuple[float, float]:
        wx = (px - self.width() * 0.5 - self.offset.x()) / (100.0 * self.zoom)
        wy = -(py - self.height() * 0.5 - self.offset.y()) / (100.0 * self.zoom)
        return (wx, wy)

    def _screen_to_world_delta(self, dx: float, dy: float) -> Tuple[float, float]:
        if self.zoom <= 0: return 0.0, 0.0
        return dx / (100.0 * self.zoom), -dy / (100.0 * self.zoom)

    def _hit_test(self, pt) -> Optional[str]:
        size = ROOM_SIZE * self.zoom
        for rid in self.visible_room_ids:
            center = self._get_room_center(rid)
            if not center: continue
            rect = QRectF(center.x() - size/2, center.y() - size/2, size, size)
            if rect.contains(pt):
                return rid
        return None

    # --------------- painting ---------------
    def paintEvent(self, event):
        painter = QPainter(self)
        painter.setRenderHint(QPainter.Antialiasing)
        painter.fillRect(self.rect(), QColor(18, 18, 18))

        # Grid aligned to room size
        pen = QPen(QColor(40, 40, 40))
        painter.setPen(pen)
        step = GRID_STEP * self.zoom
        x = self.offset.x() % step
        y = self.offset.y() % step
        for gx in range(int(-step), self.width() + int(step), int(step)):
            painter.drawLine(int(gx + x), 0, int(gx + x), self.height())
        for gy in range(int(-step), self.height() + int(step), int(step)):
            painter.drawLine(0, int(gy + y), self.width(), int(gy + y))

        # Connections (draw each pair once)
        conn_pen = QPen(QColor(100, 100, 140, 150), 2, Qt.DashLine)
        painter.setPen(conn_pen)
        drawn = set()
        for rid in self.visible_room_ids:
            start_center = self._get_room_center(rid)
            if not start_center: continue
            room = self.rooms_by_id.get(rid, {})
            for dest_id in room.get("connections", {}).values():
                if dest_id not in self.visible_room_ids:
                    continue
                pair = tuple(sorted((rid, dest_id)))
                if pair in drawn: continue
                end_center = self._get_room_center(dest_id)
                if end_center:
                    painter.drawLine(start_center, end_center)
                    drawn.add(pair)

        # Rooms (squares) with env color
        size = ROOM_SIZE * self.zoom
        for rid in self.visible_room_ids:
            room = self.rooms_by_id.get(rid)
            if not room: continue
            center = self._get_room_center(rid)
            if not center: continue

            rect = QRectF(center.x() - size/2, center.y() - size/2, size, size)

            base_color = _env_color(room.get("env", ""))
            painter.setBrush(QBrush(base_color))
            painter.setPen(QPen(QColor(15, 15, 15), 2))
            painter.drawRoundedRect(rect, 6, 6)

            # Selection / connection-start highlight (border overlay)
            if self.in_connection_mode and rid == self.connection_start_id:
                painter.setBrush(Qt.NoBrush)
                painter.setPen(QPen(QColor(100, 220, 120), 3))
                painter.drawRoundedRect(rect.adjusted(1,1,-1,-1), 6, 6)
            elif rid == self.selected_id:
                painter.setBrush(Qt.NoBrush)
                painter.setPen(QPen(QColor(240, 200, 40), 3))
                painter.drawRoundedRect(rect.adjusted(1,1,-1,-1), 6, 6)

            # Centered, elided title or id for readability in small squares
            painter.setPen(Qt.white)
            title = (room.get("title") or rid or "").strip() or rid
            fm = QFontMetrics(painter.font())
            elided = fm.elidedText(title, Qt.ElideRight, int(size - 8))
            painter.drawText(rect, Qt.AlignCenter, elided)

        # Live connection line
        if self.in_connection_mode and self.connection_start_id and self.live_connection_end_pos:
            start_center = self._get_room_center(self.connection_start_id)
            if start_center:
                live_pen = QPen(QColor(120, 240, 140, 200), 3)
                painter.setPen(live_pen)
                painter.drawLine(start_center, self.live_connection_end_pos)

    # --------------- interactions & menus ---------------
    def _summary_for_room(self, rid: str) -> str:
        r = self.rooms_by_id.get(rid, {})
        title = r.get("title") or rid
        env = r.get("env") or ""
        desc = r.get("description", "")
        if not desc:
            for k, v in r.items():
                if k.startswith("description") and isinstance(v, str) and v:
                    desc = v
                    break
        desc = " ".join(str(desc).split())
        if len(desc) > 140:
            desc = desc[:137] + "…"

        # items
        items = r.get("items", [])
        if isinstance(items, list):
            shown_items = ", ".join([str(x) for x in items[:4]])
            if len(items) > 4:
                shown_items += f" (+{len(items)-4} more)"
        else:
            shown_items = str(items)

        # enemies
        enemies = r.get("enemies", [])
        enemy_strs = []
        if isinstance(enemies, list):
            for e in enemies[:4]:
                if isinstance(e, dict):
                    nm = e.get("name") or e.get("id") or "?"
                    cnt = e.get("count") or e.get("qty") or 1
                    enemy_strs.append(f"{nm}×{cnt}")
                else:
                    enemy_strs.append(str(e))
            if len(enemies) > 4:
                enemy_strs.append(f"+{len(enemies)-4} more")
        elif enemies:
            enemy_strs = [str(enemies)]
        shown_enemies = ", ".join(enemy_strs)

        # npcs
        npcs = r.get("npcs", [])
        shown_npcs = ""
        if isinstance(npcs, list) and npcs:
            shown_npcs = ", ".join([str(x) for x in npcs[:4]])
            if len(npcs) > 4:
                shown_npcs += f" (+{len(npcs)-4})"

        lines = [f"<b>{title}</b>" + (f" — <i>{env}</i>" if env else "")]
        if desc:
            lines.append(desc)
        if shown_items:
            lines.append(f"<b>Items:</b> {shown_items}")
        if shown_enemies:
            lines.append(f"<b>Enemies:</b> {shown_enemies}")
        if shown_npcs:
            lines.append(f"<b>NPCs:</b> {shown_npcs}")

        return "<br>".join(lines)

    def contextMenuEvent(self, event):
        self.hover_card.hide()
        pt = event.pos()
        rid = self._hit_test(pt)
        menu = QMenu(self)
        if rid:
            act_center = QAction("Center on this room", self)
            act_center.triggered.connect(lambda: self._center_on_room(rid))
            menu.addAction(act_center)

            act_dup = QAction("Duplicate…", self)
            act_dup.triggered.connect(lambda: self.controller.on_duplicate_room(rid))
            menu.addAction(act_dup)

            act_del = QAction("Delete", self)
            act_del.triggered.connect(lambda: self.controller.on_delete_room(rid))
            menu.addAction(act_del)
        else:
            act_add = QAction("Add Room Here…", self)
            wx, wy = self.screen_to_world(pt.x(), pt.y())
            act_add.triggered.connect(lambda: self.controller.on_add_room_at(wx, wy))
            menu.addAction(act_add)

            act_fit = QAction("Fit to Visible", self)
            act_fit.triggered.connect(lambda: self.fit_to_ids())
            menu.addAction(act_fit)

            act_snap = QAction("📸 Export PNG", self)
            act_snap.triggered.connect(lambda: self.controller.export_snapshot(self.visible_room_ids))
            menu.addAction(act_snap)

        menu.exec_(self.mapToGlobal(pt))

    def _center_on_room(self, rid: str):
        r = self.rooms_by_id.get(rid, {})
        pos = r.get("pos", [0, 0])
        wx, wy = float(pos[0]), float(pos[1])
        sx = wx * 100.0 * self.zoom
        sy = -wy * 100.0 * self.zoom
        self.offset = QPointF(-sx + self.width() * 0.5, -sy + self.height() * 0.5)
        self.update()

    def mousePressEvent(self, event):
        self.hover_card.hide()
        # Connection Mode
        if self.in_connection_mode and event.button() == Qt.LeftButton:
            clicked_room = self._hit_test(event.pos())
            if not clicked_room:
                return
            if not self.connection_start_id:
                self.connection_start_id = clicked_room
                self.live_connection_end_pos = event.pos()
            else:
                if clicked_room != self.connection_start_id:
                    self.connection_requested.emit(self.connection_start_id, clicked_room)
                self.connection_start_id = None
                self.live_connection_end_pos = None
            self.update()
            event.accept()
            return

        # Add Mode
        if self.in_add_mode and event.button() == Qt.LeftButton:
            clicked_room = self._hit_test(event.pos())
            if clicked_room:
                self.selected_id = clicked_room
                self.controller.on_map_room_clicked(clicked_room)
                self.update()
            else:
                wx, wy = self.screen_to_world(event.pos().x(), event.pos().y())
                self.controller.on_add_room_at(wx, wy)
            event.accept()
            return

        # Standard Mode
        if event.button() == Qt.LeftButton:
            clicked = self._hit_test(event.pos())
            if clicked:
                self.selected_id = clicked
                self.controller.on_map_room_clicked(clicked)
            else:
                self._dragging = True
                self._drag_start = event.pos()
            self.update()
            event.accept()
            return

        if event.button() == Qt.RightButton:
            clicked = self._hit_test(event.pos())
            if clicked:
                self._dragging_room_id = clicked
                self._last_mouse = event.pos()
            event.accept()
            return

    def mouseMoveEvent(self, event):
        # live connection line
        if self.in_connection_mode and self.connection_start_id:
            self.live_connection_end_pos = event.pos()
            self.update()
            event.accept()
            return

        # drag room (RMB)
        if self._dragging_room_id:
            dx = event.pos().x() - self._last_mouse.x()
            dy = event.pos().y() - self._last_mouse.y()
            wx, wy = self._screen_to_world_delta(dx, dy)
            room = self.rooms_by_id.get(self._dragging_room_id)
            if room:
                pos = room.get("pos", [0, 0])
                pos[0] += wx
                pos[1] += wy
                self.controller.on_room_pos_drag(self._dragging_room_id, pos[0], pos[1])
            self._last_mouse = event.pos()
            self.update()
            event.accept()
            return

        # pan map (LMB drag on empty)
        if self._dragging:
            delta = event.pos() - self._drag_start
            self.offset += delta
            self._drag_start = event.pos()
            self.update()
            event.accept()
            return

        # Hover summary
        rid = self._hit_test(event.pos())
        if rid and rid in self.visible_room_ids:
            if rid != self._hover_id:
                self._hover_id = rid
                text = self._summary_for_room(rid)
                self.hover_card.set_text(text)
            self.hover_card.show_near(event.globalPos())
        else:
            self._hover_id = None
            self.hover_card.hide()

    def leaveEvent(self, event):
        self._hover_id = None
        self.hover_card.hide()
        super().leaveEvent(event)

    def mouseReleaseEvent(self, event):
        if event.button() == Qt.RightButton and self._dragging_room_id:
            rid = self._dragging_room_id
            room = self.rooms_by_id.get(rid)
            if room:
                pos = room.get("pos", [0, 0])
                pos[0] = round(pos[0]); pos[1] = round(pos[1])
                self.controller.on_room_pos_drag(rid, pos[0], pos[1])
            self._dragging_room_id = None
            self.update()
            event.accept()
            return

        if event.button() == Qt.LeftButton and self._dragging:
            self._dragging = False
            event.accept()
            return

    def wheelEvent(self, event):
        angle = event.angleDelta().y()
        factor = 1.0 + (0.0015 * angle)
        new_zoom = max(0.1, min(self.zoom * factor, 8.0))

        mouse = event.pos()  # QPoint
        # scene point BEFORE zoom
        sx_before, sy_before = self.screen_to_world(mouse.x(), mouse.y())

        self.zoom = new_zoom

        # Keep same scene point under the cursor (mirror atlas_editor math)
        self.offset = QPointF(
            mouse.x() - sx_before * (100.0 * self.zoom),
            mouse.y() + sy_before * (100.0 * self.zoom)  # note your Y flips in screen_to_world
        )
        self.update()
        event.accept()

# --------------------------------------------------------
# Large Map Designer Dialog
# --------------------------------------------------------
class LargeMapDesignerDialog(QDialog):
    def __init__(self, editor: "RoomEditor"):
        super().__init__(editor)
        self.editor = editor
        self.setWindowTitle("Large Map Designer")
        self.resize(1200, 820)

        root = QVBoxLayout(self)

        # Controls
        ctrls = QHBoxLayout()
        self.scope_combo = QComboBox()
        self.scope_combo.addItems(["All Rooms", "Current Node"])
        self.scope_combo.currentTextChanged.connect(self._apply_scope)

        self.add_btn = QPushButton("➕ Add Mode")
        self.add_btn.setCheckable(True)
        self.add_btn.toggled.connect(self._toggle_add_mode)

        self.connect_btn = QPushButton("🔗 Connect Rooms")
        self.connect_btn.setCheckable(True)
        self.connect_btn.toggled.connect(self._toggle_connect_mode)

        self.fit_btn = QPushButton("◰ Fit")
        self.fit_btn.clicked.connect(lambda: self.map_view.fit_to_ids())

        self.snap_btn = QPushButton("📸 Export PNG")
        self.snap_btn.clicked.connect(self._export_scope_snapshot)

        ctrls.addWidget(QLabel("Scope:"))
        ctrls.addWidget(self.scope_combo)
        ctrls.addStretch(1)
        ctrls.addWidget(self.add_btn)
        ctrls.addWidget(self.connect_btn)
        ctrls.addWidget(self.fit_btn)
        ctrls.addWidget(self.snap_btn)
        root.addLayout(ctrls)

        # Map
        self.map_view = MapView(self.editor.rooms_by_id, controller=self.editor, parent=self)
        self.map_view.connection_requested.connect(self.editor._create_connection_dialog)
        root.addWidget(self.map_view, 1)

        # init scope
        self._apply_scope()
        self.map_view.fit_to_ids()

    def _apply_scope(self):
        mode = self.scope_combo.currentText()
        if mode == "All Rooms":
            ids = set(self.editor.rooms_by_id.keys())
        else:
            ids = set(self.editor._node_member_ids())
        self.map_view.set_visible_ids(ids)
        self.map_view.fit_to_ids(ids)

    def _toggle_add_mode(self, enabled: bool):
        if enabled and self.connect_btn.isChecked():
            self.connect_btn.setChecked(False)
        self.map_view.set_add_mode(enabled)

    def _toggle_connect_mode(self, enabled: bool):
        if enabled and self.add_btn.isChecked():
            self.add_btn.setChecked(False)
        self.map_view.set_connection_mode(enabled)

    def _export_scope_snapshot(self):
        self.editor.export_snapshot(self.map_view.visible_room_ids)

# --------------------------------------------------------
# Main Editor
# --------------------------------------------------------
class RoomEditor(QWidget):
    def __init__(self, root_dir: str):
        super().__init__()
        self.root = root_dir
        # resolved paths
        self.rooms_path  = os.path.join(self.root, "rooms.json")
        self.worlds_path = os.path.join(self.root, "worlds.json")
        self.hubs_path   = os.path.join(self.root, "hubs.json")
        self.nodes_path  = os.path.join(self.root, "nodes.json")

        self.setWindowTitle("Starborn Room Editor (World/Hub/Node)")
        self.resize(1420, 900)
        self.is_connection_mode = False

        # Data
        self.rooms_by_id: Dict[str, dict]  = {}
        self.worlds_by_id: Dict[str, dict] = {}
        self.hubs_by_id: Dict[str, dict]   = {}
        self.nodes_by_id: Dict[str, dict]  = {}

        self.current_world_id: Optional[str] = None
        self.current_hub_id: Optional[str]   = None
        self.current_node_id: Optional[str]  = None
        self.current_room_id: Optional[str]  = None

        self._desc_variants: Dict[str, str] = {}
        self._bg_cache: Dict[str, Optional[str]] = {}

        self._load_all()
        self._init_ui()
        self._rebuild_tree()
        self._refresh_room_list()

    # ---------- Path resolving for background images ----------
    def _resolve_bg_path(self, ref: str) -> Optional[str]:
        if not ref:
            return None
        ref = ref.strip().replace("\\", "/")
        cached = self._bg_cache.get(ref, "__miss__")
        if cached != "__miss__":
            return cached

        if os.path.isabs(ref) and os.path.exists(ref):
            self._bg_cache[ref] = ref
            return ref

        direct = os.path.join(self.root, ref)
        if os.path.exists(direct):
            self._bg_cache[ref] = direct
            return direct

        candidates = [
            os.path.join(self.root, "assets", ref),
            os.path.join(self.root, "assets", "backgrounds", ref),
            os.path.join(self.root, "images", ref),
            os.path.join(self.root, "images", "backgrounds", ref),
            os.path.join(self.root, "resources", ref),
            os.path.join(self.root, "resources", "backgrounds", ref),
            os.path.join(self.root, "art", ref),
            os.path.join(self.root, "art", "backgrounds", ref),
        ]
        for c in candidates:
            if os.path.exists(c):
                self._bg_cache[ref] = c
                return c

        base = os.path.basename(ref)
        for top in [self.root,
                    os.path.join(self.root, "assets"),
                    os.path.join(self.root, "images"),
                    os.path.join(self.root, "resources"),
                    os.path.join(self.root, "art")]:
            if not os.path.isdir(top):
                continue
            for dirpath, _, filenames in os.walk(top):
                if base in filenames:
                    found = os.path.join(dirpath, base)
                    self._bg_cache[ref] = found
                    return found

        self._bg_cache[ref] = None
        return None

    def _make_rel_to_root(self, path: str) -> str:
        try:
            rel = os.path.relpath(path, self.root)
            if rel.startswith(".."):
                return os.path.abspath(path)
            return rel.replace("\\", "/")
        except Exception:
            return os.path.abspath(path)

    # ---------- UI ----------
    def _init_ui(self):
        layout = QHBoxLayout(self)

        # Left: tree + refresh
        left_col = QVBoxLayout()
        self.tree = QTreeWidget()
        self.tree.setHeaderLabels(["Maps"])
        self.tree.itemClicked.connect(self._on_tree_clicked)
        left_col.addWidget(self.tree)

        refresh_tree_btn = QPushButton("Refresh Tree")
        refresh_tree_btn.clicked.connect(self._rebuild_tree)
        left_col.addWidget(refresh_tree_btn)

        layout.addLayout(left_col, 1)

        # Center column
        center_col = QVBoxLayout()

        # Map controls
        map_controls = QHBoxLayout()
        self.open_large_btn = QPushButton("🗺️ Open Large Map Designer")
        self.open_large_btn.clicked.connect(self._open_large_map)

        self.connect_btn = QPushButton("🔗 Connect Rooms")
        self.connect_btn.setCheckable(True)
        self.connect_btn.clicked.connect(self._toggle_connection_mode)

        self.fit_small_btn = QPushButton("◰ Fit")
        self.fit_small_btn.clicked.connect(lambda: self.map_view.fit_to_ids())

        self.snap_btn = QPushButton("📸 Export PNG")
        self.snap_btn.clicked.connect(lambda: self.export_snapshot(self.map_view.visible_room_ids))

        map_controls.addWidget(self.open_large_btn)
        map_controls.addStretch()
        map_controls.addWidget(self.connect_btn)
        map_controls.addWidget(self.fit_small_btn)
        map_controls.addWidget(self.snap_btn)
        center_col.addLayout(map_controls)

        # Mini Map
        self.map_view = MapView(self.rooms_by_id, controller=self, parent=self)
        self.map_view.connection_requested.connect(self._create_connection_dialog)
        center_col.addWidget(self.map_view, 3)

        # Node membership
        node_box = QGroupBox("Node Rooms")
        node_v = QVBoxLayout()
        self.node_rooms_list = QListWidget()
        node_v.addWidget(self.node_rooms_list, 1)

        mem_row = QHBoxLayout()
        self.available_rooms_search = QLineEdit()
        self.available_rooms_search.setPlaceholderText("Search all rooms…")
        self.available_rooms_list = QListWidget()
        self.available_rooms_search.textChanged.connect(self._refresh_available_rooms)

        mem_col = QVBoxLayout()
        mem_col.addWidget(self.available_rooms_search)
        mem_col.addWidget(self.available_rooms_list, 1)

        add_btn = QPushButton("⇧ Add to Node")
        rem_btn = QPushButton("⇩ Remove from Node")
        add_btn.clicked.connect(self._add_selected_to_node)
        rem_btn.clicked.connect(self._remove_selected_from_node)
        btns_col = QVBoxLayout()
        btns_col.addWidget(add_btn)
        btns_col.addWidget(rem_btn)
        btns_col.addStretch()

        mem_row.addWidget(self.node_rooms_list, 1)
        mem_row.addLayout(btns_col)
        mem_row.addLayout(mem_col, 1)
        node_v.addLayout(mem_row)

        auto_btn = QPushButton("Auto-Fill (from entry_room)")
        auto_btn.clicked.connect(self._autofill_node_from_entry)
        node_v.addWidget(auto_btn)

        save_node_btn = QPushButton("Save Node")
        save_node_btn.clicked.connect(self._save_current_node)
        node_v.addWidget(save_node_btn)
        node_box.setLayout(node_v)
        center_col.addWidget(node_box, 2)

        layout.addLayout(center_col, 3)

        # Right: inspector
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        insp = QWidget()
        scroll.setWidget(insp)
        form = QVBoxLayout(insp)

        # Room Identity
        id_group = QGroupBox("Room Identity")
        idf = QFormLayout()
        self.id_label = QLabel("—")
        self.title_edit = QLineEdit()
        self.env_edit = QLineEdit()

        # Background row with hover preview (only on hover)
        bg_row = QHBoxLayout()
        self.bg_edit = QLineEdit()
        self.bg_browse = QPushButton("Browse…")
        self.bg_browse.clicked.connect(self._browse_bg_image)
        bg_row.addWidget(self.bg_edit, 1)
        bg_row.addWidget(self.bg_browse, 0)

        # Hover preview widget
        self.bg_preview = ImageHoverPreview(self)
        self.bg_edit.installEventFilter(self)

        idf.addRow("ID:", self.id_label)
        idf.addRow("Title:", self.title_edit)
        idf.addRow("Env:", self.env_edit)
        idf.addRow("Background Image:", bg_row)
        id_group.setLayout(idf)
        form.addWidget(id_group)

        # Position
        pos_group = QGroupBox("Position")
        pos_row = QHBoxLayout()
        self.pos_x = QSpinBox(); self.pos_x.setRange(-2000, 2000)
        self.pos_y = QSpinBox(); self.pos_y.setRange(-2000, 2000)
        pos_row.addWidget(QLabel("X:")); pos_row.addWidget(self.pos_x)
        pos_row.addWidget(QLabel("Y:")); pos_row.addWidget(self.pos_y)
        pos_group.setLayout(pos_row)
        form.addWidget(pos_group)

        # Description variants
        desc_group = QGroupBox("Description Variants")
        desc_v = QVBoxLayout()
        self.desc_variant_combo = QComboBox()
        self.desc_variant_combo.currentTextChanged.connect(self._on_desc_variant_switched)
        self.desc_text = QPlainTextEdit()
        desc_btn_row = QHBoxLayout()
        add_var = QPushButton("Add Variant"); add_var.clicked.connect(self._add_variant)
        del_var = QPushButton("Remove Variant"); del_var.clicked.connect(self._remove_variant)
        desc_btn_row.addWidget(add_var); desc_btn_row.addWidget(del_var)
        desc_v.addWidget(self.desc_variant_combo)
        desc_v.addWidget(self.desc_text)
        desc_v.addLayout(desc_btn_row)
        desc_group.setLayout(desc_v)
        form.addWidget(desc_group)

        # Connections
        conn_group = QGroupBox("Connections")
        connf = QFormLayout()
        self.conn_north = QLineEdit()
        self.conn_south = QLineEdit()
        self.conn_east  = QLineEdit()
        self.conn_west  = QLineEdit()
        connf.addRow("North:", self.conn_north)
        connf.addRow("South:", self.conn_south)
        connf.addRow("East:",  self.conn_east)
        connf.addRow("West:",  self.conn_west)
        conn_group.setLayout(connf)
        form.addWidget(conn_group)

        # Items & NPCs
        in_group = QGroupBox("Items / NPCs (comma-separated)")
        inf = QFormLayout()
        self.items_edit = QLineEdit()
        self.npcs_edit  = QLineEdit()
        inf.addRow("Items:", self.items_edit)
        inf.addRow("NPCs:",  self.npcs_edit)
        in_group.setLayout(inf)
        form.addWidget(in_group)

        # Enemies JSON
        self.enemies_edit = QPlainTextEdit()
        enemies_group = QGroupBox("Enemies JSON")
        v = QVBoxLayout(); v.addWidget(self.enemies_edit); enemies_group.setLayout(v)
        form.addWidget(enemies_group)

        # Actions JSON
        self.actions_edit = QPlainTextEdit()
        actions_group = QGroupBox("Actions JSON")
        av = QVBoxLayout(); av.addWidget(self.actions_edit); actions_group.setLayout(av)
        form.addWidget(actions_group)

        # State JSON
        state_group = QGroupBox("State (JSON object)")
        self.state_edit = QPlainTextEdit()
        sv = QVBoxLayout(); sv.addWidget(self.state_edit); state_group.setLayout(sv)
        form.addWidget(state_group)

        # Save buttons
        save_room_btn = QPushButton("Save Room")
        save_room_btn.clicked.connect(self._save_current_room)
        save_all_rooms_btn = QPushButton("Save ALL Rooms")
        save_all_rooms_btn.clicked.connect(self._save_rooms)
        form.addWidget(save_room_btn)
        form.addWidget(save_all_rooms_btn)
        form.addStretch()

        layout.addWidget(scroll, 3)

        # Under map: room list + search
        bottom = QVBoxLayout()
        self.search_rooms = QLineEdit(); self.search_rooms.setPlaceholderText("Search rooms in node…")
        self.search_rooms.textChanged.connect(self._refresh_room_list)
        self.room_list = QListWidget()
        self.room_list.itemClicked.connect(self._on_room_selected)
        bottom.addWidget(self.search_rooms)
        bottom.addWidget(self.room_list)
        center_col.addLayout(bottom, 2)

    # ---------- Browse for background image ----------
    def _browse_bg_image(self):
        start_dir = self.root
        last_path = self._resolve_bg_path(self.bg_edit.text().strip())
        if last_path:
            start_dir = os.path.dirname(last_path)
        path, _ = QFileDialog.getOpenFileName(self, "Pick Background Image", start_dir,
                                              "Images (*.png *.jpg *.jpeg *.webp *.gif);;All Files (*)")
        if path:
            rel = self._make_rel_to_root(path)
            self.bg_edit.setText(rel)
            # Preview shows on hover only.

    # ---------- Hover image preview for bg_edit ----------
    def eventFilter(self, obj, event):
        if obj is self.bg_edit:
            if event.type() in (QEvent.Enter, QEvent.MouseMove):
                self._update_bg_preview(event.globalPos())
            elif event.type() in (QEvent.Leave, QEvent.FocusOut):
                self.bg_preview.hide()
        return super().eventFilter(obj, event)

    def _update_bg_preview(self, global_pos):
        ref = self.bg_edit.text().strip()
        abs_path = self._resolve_bg_path(ref)
        if abs_path and os.path.exists(abs_path):
            pm = QPixmap(abs_path)
            if not pm.isNull():
                self.bg_preview.set_pixmap(pm)
                self.bg_preview.show_near(global_pos)
                return
        self.bg_preview.hide()

    # ---------- Map Designer ----------
    def _open_large_map(self):
        dlg = LargeMapDesignerDialog(self)
        dlg.exec_()

    def _toggle_connection_mode(self):
        self.is_connection_mode = self.connect_btn.isChecked()
        self.map_view.set_connection_mode(self.is_connection_mode)
        if self.is_connection_mode:
            self.connect_btn.setText("❌ Cancel Connection")
        else:
            self.connect_btn.setText("🔗 Connect Rooms")

    def _create_connection_dialog(self, start_id: str, end_id: str):
        directions = ["north", "south", "east", "west"]

        forward_dir, ok1 = QInputDialog.getItem(
            self, "Create Connection",
            f"Direction from '{start_id}' to '{end_id}':",
            directions, 0, False
        )
        if not ok1:
            if self.connect_btn.isChecked():
                self.connect_btn.setChecked(False)
                self._toggle_connection_mode()
            return

        return_dir, ok2 = QInputDialog.getItem(
            self, "Create Return Connection",
            f"Return direction from '{end_id}' back to '{start_id}':\n(Optional - Cancel for one-way exit)",
            directions, 0, False
        )

        start_room = self.rooms_by_id[start_id]
        start_room.setdefault("connections", {})
        start_room["connections"][forward_dir] = end_id

        if ok2 and return_dir:
            end_room = self.rooms_by_id[end_id]
            end_room.setdefault("connections", {})
            end_room["connections"][return_dir] = start_id

        QMessageBox.information(self, "Success",
                                "Connection created. Remember to 'Save Room' or 'Save ALL Rooms' to persist changes.")
        self.map_view.update()
        self._load_room_into_inspector(start_id)

    # ---------- Duplicate / Delete ----------
    def on_duplicate_room(self, rid: str):
        if rid not in self.rooms_by_id:
            return
        new_id, ok = QInputDialog.getText(self, "Duplicate Room", f"New room ID (copy of {rid}):")
        if not ok or not new_id:
            return
        new_id = new_id.strip()
        if new_id in self.rooms_by_id:
            QMessageBox.warning(self, "Duplicate Room", f"Room '{new_id}' already exists.")
            return
        dup = copy.deepcopy(self.rooms_by_id[rid])
        dup["id"] = new_id
        # Offset position to see both
        pos = dup.get("pos", [0, 0])
        dup["pos"] = [int(round(pos[0])) + 1, int(round(pos[1])) + 1]
        # Clear connections by default to avoid accidental graph ties; ask user?
        # We'll keep connections but it's easy to clear if needed.
        self.rooms_by_id[new_id] = dup

        # If a node is selected, add duplicate into the node
        if self.current_node_id:
            node = self.nodes_by_id.get(self.current_node_id, {})
            key = _node_room_key(node)
            arr = list(node.get(key, []))
            if new_id not in arr:
                arr.append(new_id)
            node[key] = arr
            self._refresh_node_membership()

        self._refresh_room_list()
        self._refresh_map()
        self._load_room_into_inspector(new_id)
        self.map_view.selected_id = new_id
        self.map_view.update()

    def on_delete_room(self, rid: str):
        if rid not in self.rooms_by_id:
            return
        reply = QMessageBox.question(
            self, "Delete Room",
            f"Really delete room '{rid}'? This will also remove references in nodes and connections.",
            QMessageBox.Yes | QMessageBox.No, QMessageBox.No
        )
        if reply != QMessageBox.Yes:
            return

        # Remove connections pointing to and from rid
        for orid, r in list(self.rooms_by_id.items()):
            conns = r.get("connections", {})
            to_delete = [d for d, tgt in conns.items() if tgt == rid]
            for d in to_delete:
                del conns[d]
        # Remove the room
        self.rooms_by_id.pop(rid, None)

        # Remove from node memberships
        for node in self.nodes_by_id.values():
            key = _node_room_key(node)
            if key in node and isinstance(node[key], list):
                node[key] = [x for x in node[key] if x != rid]

        # UI cleanup
        if self.current_room_id == rid:
            self._clear_inspector()
        self._refresh_node_membership()
        self._refresh_room_list()
        self._refresh_map()
        self.map_view.selected_id = None
        self.map_view.update()

    # ---------- Export snapshot ----------
    def export_snapshot(self, ids: Set[str]):
        ids = set([i for i in ids if i in self.rooms_by_id])
        if not ids:
            QMessageBox.information(self, "Export PNG", "Nothing to export in the current scope.")
            return
        width, ok = QInputDialog.getInt(self, "Export PNG", "Image width (pixels):", 2048, 256, 8192, 64)
        if not ok:
            return
        # square export
        size_px = width
        path, _ = QFileDialog.getSaveFileName(self, "Save Map PNG", os.path.join(self.root, "map_snapshot.png"),
                                              "PNG Image (*.png)")
        if not path:
            return
        ok = self._render_snapshot_png(path, ids, size_px, size_px)
        if ok:
            QMessageBox.information(self, "Export PNG", f"Saved: {path}")

    def _render_snapshot_png(self, path: str, ids: Set[str], w_px: int, h_px: int, padding: float = 0.1) -> bool:
        # Compute bounds in world units
        pts = []
        for rid in ids:
            r = self.rooms_by_id.get(rid)
            if not r: continue
            pos = r.get("pos", [0, 0])
            pts.append((float(pos[0]), float(pos[1])))
        if not pts:
            return False
        xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
        minx, maxx = min(xs), max(xs)
        miny, maxy = min(ys), max(ys)

        w_world = max(1.0, (maxx - minx) + 2.5)
        h_world = max(1.0, (maxy - miny) + 2.5)

        z_x = (w_px * (1.0 - padding)) / (w_world * 100.0)
        z_y = (h_px * (1.0 - padding)) / (h_world * 100.0)
        zoom = max(0.05, min(z_x, z_y))

        cx = (minx + maxx) * 0.5
        cy = (miny + maxy) * 0.5
        # offset such that world (0,0) maps to center then adjusted to center (cx,cy)
        offset = QPointF(-cx * 100.0 * zoom + w_px * 0.5, -(-cy * 100.0 * zoom) + h_px * 0.5)

        img = QImage(QSize(w_px, h_px), QImage.Format_ARGB32_Premultiplied)
        img.fill(QColor(18, 18, 18))
        p = QPainter(img)
        p.setRenderHint(QPainter.Antialiasing, True)

        # Grid (same look)
        step = GRID_STEP * zoom
        pen = QPen(QColor(40, 40, 40))
        p.setPen(pen)
        # draw grid roughly covering image
        x0 = offset.x() % step
        y0 = offset.y() % step
        for gx in range(int(-step), w_px + int(step), int(step)):
            p.drawLine(int(gx + x0), 0, int(gx + x0), h_px)
        for gy in range(int(-step), h_px + int(step), int(step)):
            p.drawLine(0, int(gy + y0), w_px, int(gy + y0))

        # Helper: get screen pos from world pos
        def world_to_screen(wx: float, wy: float) -> QPointF:
            sx = wx * 100.0 * zoom + offset.x()
            sy = -wy * 100.0 * zoom + offset.y()
            return QPointF(sx, sy)

        # Connections (draw pair once)
        conn_pen = QPen(QColor(100, 100, 140, 150), 2, Qt.DashLine)
        p.setPen(conn_pen)
        drawn = set()
        for rid in ids:
            r = self.rooms_by_id.get(rid, {})
            start = world_to_screen(*r.get("pos", [0, 0]))
            for dest in r.get("connections", {}).values():
                if dest not in ids:  # export only visible scope
                    continue
                pair = tuple(sorted((rid, dest)))
                if pair in drawn:
                    continue
                rr = self.rooms_by_id.get(dest, {})
                end = world_to_screen(*rr.get("pos", [0, 0]))
                p.drawLine(start, end)
                drawn.add(pair)

        # Rooms
        size = ROOM_SIZE * zoom
        for rid in ids:
            r = self.rooms_by_id.get(rid, {})
            center = world_to_screen(*r.get("pos", [0, 0]))
            rect = QRectF(center.x() - size/2, center.y() - size/2, size, size)

            base_color = _env_color(r.get("env", ""))
            p.setBrush(QBrush(base_color))
            p.setPen(QPen(QColor(15, 15, 15), 2))
            p.drawRoundedRect(rect, 6, 6)

            p.setPen(Qt.white)
            title = (r.get("title") or rid or "").strip() or rid
            fm = QFontMetrics(p.font())
            elided = fm.elidedText(title, Qt.ElideRight, int(size - 8))
            p.drawText(rect, Qt.AlignCenter, elided)

        p.end()
        return img.save(path, "PNG")

    # ---------- Data load/save ----------
    def _load_all(self):
        raw_rooms = _safe_read_json(self.rooms_path, [])
        if isinstance(raw_rooms, list):
            self.rooms_by_id = {r.get("id"): r for r in raw_rooms if r.get("id")}
        elif isinstance(raw_rooms, dict):
            self.rooms_by_id = {k: v for k, v in raw_rooms.items() if k}
        else:
            self.rooms_by_id = {}

        self.worlds_by_id = {w.get("id"): w for w in _safe_read_json(self.worlds_path, []) if w.get("id")}
        self.hubs_by_id   = {h.get("id"): h for h in _safe_read_json(self.hubs_path, []) if h.get("id")}
        self.nodes_by_id  = {n.get("id"): n for n in _safe_read_json(self.nodes_path, []) if n.get("id")}

        for rid, r in self.rooms_by_id.items():
            r.setdefault("env", "")
            r.setdefault("title", "")
            r.setdefault("background_image", "")
            r.setdefault("description", "")
            r.setdefault("connections", {})
            r.setdefault("items", [])
            r.setdefault("npcs", [])
            r.setdefault("enemies", [])
            r.setdefault("state", {})
            r.setdefault("actions", [])
            r.setdefault("pos", [0, 0])

    def _save_rooms(self):
        out = list(self.rooms_by_id.values())
        return _safe_write_json(self.rooms_path, out)

    def _save_nodes(self):
        out = list(self.nodes_by_id.values())
        return _safe_write_json(self.nodes_path, out)

    # ---------- Tree / filtering ----------
    def _rebuild_tree(self):
        self.tree.clear()

        world_items: Dict[str, QTreeWidgetItem] = {}
        hub_items: Dict[str, QTreeWidgetItem]   = {}

        if self.worlds_by_id:
            for wid, w in sorted(self.worlds_by_id.items()):
                w_item = QTreeWidgetItem([w.get("title") or wid])
                w_item.setData(0, Qt.UserRole, ("world", wid))
                self.tree.addTopLevelItem(w_item)
                world_items[wid] = w_item
        else:
            all_item = QTreeWidgetItem(["All Worlds"])
            all_item.setData(0, Qt.UserRole, ("world", None))
            self.tree.addTopLevelItem(all_item)
            world_items[None] = all_item

        if self.hubs_by_id:
            for hid, h in sorted(self.hubs_by_id.items()):
                wid = h.get("world_id")
                parent = world_items.get(wid) or list(world_items.values())[0]
                h_item = QTreeWidgetItem([h.get("title") or hid])
                h_item.setData(0, Qt.UserRole, ("hub", hid))
                parent.addChild(h_item)
                hub_items[hid] = h_item
        else:
            h_item = QTreeWidgetItem(["All Hubs"])
            h_item.setData(0, Qt.UserRole, ("hub", None))
            list(world_items.values())[0].addChild(h_item)
            hub_items[None] = h_item

        if self.nodes_by_id:
            for nid, n in sorted(self.nodes_by_id.items()):
                hid = n.get("hub_id")
                parent = hub_items.get(hid) or list(hub_items.values())[0]
                n_item = QTreeWidgetItem([n.get("title") or nid])
                n_item.setData(0, Qt.UserRole, ("node", nid))
                parent.addChild(n_item)
        else:
            n_item = QTreeWidgetItem(["All Nodes"])
            n_item.setData(0, Qt.UserRole, ("node", None))
            list(hub_items.values())[0].addChild(n_item)

        self.tree.expandAll()

    def _on_tree_clicked(self, item: QTreeWidgetItem):
        role, value = item.data(0, Qt.UserRole)
        if role == "world":
            self.current_world_id = value
            self.current_hub_id = None
            self.current_node_id = None
        elif role == "hub":
            self.current_hub_id = value
            self.current_node_id = None
        elif role == "node":
            self.current_node_id = value
        self._refresh_room_list()
        self._refresh_node_membership()
        self._refresh_map()

    def _node_member_ids(self) -> List[str]:
        if not self.current_node_id:
            return sorted(self.rooms_by_id.keys())
        node = self.nodes_by_id.get(self.current_node_id, {})
        if not node: return []
        key = _node_room_key(node)
        return sorted([rid for rid in node.get(key, []) if rid in self.rooms_by_id])

    def _refresh_node_membership(self):
        self.node_rooms_list.clear()
        for rid in self._node_member_ids():
            self.node_rooms_list.addItem(QListWidgetItem(rid))
        self._refresh_available_rooms()

    def _refresh_available_rooms(self):
        filter_txt = (self.available_rooms_search.text() or "").lower()
        in_node = set(self._node_member_ids())
        self.available_rooms_list.clear()
        for rid in sorted(self.rooms_by_id.keys()):
            if rid in in_node: continue
            if filter_txt and filter_txt not in rid.lower(): continue
            self.available_rooms_list.addItem(QListWidgetItem(rid))

    def _add_selected_to_node(self):
        if not self.current_node_id: return
        node = self.nodes_by_id.get(self.current_node_id, {})
        key = _node_room_key(node)
        current = list(node.get(key, []))
        for item in self.available_rooms_list.selectedItems():
            rid = item.text()
            if rid not in current:
                current.append(rid)
        node[key] = current
        self._refresh_node_membership()
        self._refresh_room_list()
        self._refresh_map()

    def _remove_selected_from_node(self):
        if not self.current_node_id: return
        node = self.nodes_by_id.get(self.current_node_id, {})
        key = _node_room_key(node)
        current = list(node.get(key, []))
        remove = {i.text() for i in self.node_rooms_list.selectedItems()}
        node[key] = [rid for rid in current if rid not in remove]
        self._refresh_node_membership()
        self._refresh_room_list()
        self._refresh_map()

    def _autofill_node_from_entry(self, max_rooms: int = 200):
        if not self.current_node_id:
            QMessageBox.information(self, "Auto-Fill", "No node selected.")
            return

        node = self.nodes_by_id.get(self.current_node_id, {})
        entry = node.get("entry_room")
        if not entry or entry not in self.rooms_by_id:
            QMessageBox.warning(self, "Auto-Fill", "This node has no valid entry_room.")
            return

        other_entries = { n.get("entry_room") for nid, n in self.nodes_by_id.items()
                          if nid != self.current_node_id and n.get("entry_room") }
        visited: set[str] = set()
        queue: list[str] = [entry]

        while queue and len(visited) < max_rooms:
            rid = queue.pop(0)
            if rid in visited: continue
            visited.add(rid)
            conns = self.rooms_by_id.get(rid, {}).get("connections", {})
            for nbr in conns.values():
                if nbr in visited or nbr in other_entries: continue
                if nbr in self.rooms_by_id:
                    queue.append(nbr)

        key = _node_room_key(node)
        node[key] = sorted(visited)
        self._refresh_node_membership()
        self._refresh_room_list()
        self._refresh_map()
        QMessageBox.information(self, "Auto-Fill", f"Added {len(visited)} rooms to the node.")

    def _save_current_node(self):
        if not self.current_node_id:
            QMessageBox.information(self, "Node", "No node selected.")
            return
        self._save_nodes()

    def _visible_room_ids_for_current_node(self) -> Set[str]:
        return set(self._node_member_ids())

    def _refresh_room_list(self):
        self.room_list.clear()
        filter_txt = (self.search_rooms.text() or "").lower()
        ids = self._visible_room_ids_for_current_node()
        for rid in sorted(ids):
            if filter_txt and filter_txt not in rid.lower():
                continue
            self.room_list.addItem(QListWidgetItem(rid))
        if self.current_room_id and self.current_room_id not in ids:
            self.current_room_id = None
            self.map_view.selected_id = None
            self.map_view.update()

    def _refresh_map(self):
        self.map_view.set_visible_ids(self._visible_room_ids_for_current_node())

    # ---------- MapView callbacks ----------
    def on_map_room_clicked(self, rid: str):
        items = self.room_list.findItems(rid, Qt.MatchExactly)
        if items:
            self.room_list.setCurrentItem(items[0])
        self._load_room_into_inspector(rid)
        self.map_view.selected_id = rid
        self.map_view.update()

    def on_room_pos_drag(self, rid: str, x: float, y: float):
        if rid in self.rooms_by_id:
            self.rooms_by_id[rid]["pos"] = [x, y]
        if self.current_room_id == rid:
            self.pos_x.blockSignals(True); self.pos_y.blockSignals(True)
            try:
                self.pos_x.setValue(int(round(x)))
                self.pos_y.setValue(int(round(y)))
            finally:
                self.pos_x.blockSignals(False); self.pos_y.blockSignals(False)

    def on_add_room_at(self, wx: float, wy: float):
        x = int(round(wx)); y = int(round(wy))
        rid, ok = QInputDialog.getText(self, "Add Room", "New room ID:")
        if not ok or not rid:
            return
        rid = rid.strip()
        if rid in self.rooms_by_id:
            QMessageBox.warning(self, "Add Room", f"Room '{rid}' already exists.")
            return

        title, _ = QInputDialog.getText(self, "Room Title (optional)", "Title:")
        title = title.strip() if title else ""

        new_room = {
            "id": rid,
            "env": "",
            "title": title or rid,
            "background_image": "",
            "description": "",
            "connections": {},
            "items": [],
            "npcs": [],
            "enemies": [],
            "state": {},
            "actions": [],
            "pos": [x, y],
        }
        self.rooms_by_id[rid] = new_room

        if self.current_node_id:
            node = self.nodes_by_id.get(self.current_node_id, {})
            key = _node_room_key(node)
            arr = list(node.get(key, []))
            if rid not in arr:
                arr.append(rid)
            node[key] = arr
            self._refresh_node_membership()

        self._refresh_room_list()
        self._refresh_map()
        self._load_room_into_inspector(rid)
        self.map_view.selected_id = rid
        self.map_view.update()

    # ---------- Inspector ----------
    def _clear_inspector(self):
        self.current_room_id = None
        self.id_label.setText("—")
        self.title_edit.setText("")
        self.env_edit.setText("")
        self.bg_edit.setText("")
        self.pos_x.setValue(0); self.pos_y.setValue(0)
        self.desc_text.clear()
        self.desc_variant_combo.clear()
        self.conn_north.clear(); self.conn_south.clear(); self.conn_east.clear(); self.conn_west.clear()
        self.items_edit.clear(); self.npcs_edit.clear()
        self.enemies_edit.clear(); self.actions_edit.clear(); self.state_edit.clear()
        self._desc_variants.clear()
        self.bg_preview.hide()

    def _load_room_into_inspector(self, rid: str):
        self._clear_inspector()
        if rid not in self.rooms_by_id:
            return
        self.current_room_id = rid
        r = self.rooms_by_id[rid]

        self.id_label.setText(rid)
        self.title_edit.setText(r.get("title", ""))
        self.env_edit.setText(r.get("env", ""))
        self.bg_edit.setText(r.get("background_image", ""))  # no auto-preview here
        pos = r.get("pos", [0, 0])
        self.pos_x.setValue(int(pos[0] if isinstance(pos, list) and len(pos) > 0 else 0))
        self.pos_y.setValue(int(pos[1] if isinstance(pos, list) and len(pos) > 1 else 0))

        self._desc_variants = {k: v for k, v in r.items() if k.startswith("description")}
        if "description" not in self._desc_variants:
            self._desc_variants["description"] = r.get("description", "")

        self.desc_variant_combo.clear()
        self.desc_variant_combo.addItems(sorted(self._desc_variants.keys()))
        if self.desc_variant_combo.count() == 0:
            self.desc_variant_combo.addItem("description")
            self._desc_variants["description"] = r.get("description", "")

        self.desc_variant_combo.setCurrentText("description")
        self.desc_text.setPlainText(self._desc_variants.get("description", ""))

        conns = r.get("connections", {})
        self.conn_north.setText(conns.get("north", ""))
        self.conn_south.setText(conns.get("south", ""))
        self.conn_east.setText(conns.get("east", ""))
        self.conn_west.setText(conns.get("west", ""))

        self.items_edit.setText(",".join(r.get("items", [])))
        self.npcs_edit.setText(",".join(r.get("npcs", [])))

        try:
            self.enemies_edit.setPlainText(json.dumps(r.get("enemies", []), indent=2))
        except Exception:
            self.enemies_edit.setPlainText("[]")

        try:
            self.actions_edit.setPlainText(json.dumps(r.get("actions", []), indent=2))
        except Exception:
            self.actions_edit.setPlainText("[]")

        try:
            self.state_edit.setPlainText(json.dumps(r.get("state", {}), indent=2))
        except Exception:
            self.state_edit.setPlainText("{}")

    def _on_room_selected(self, item: QListWidgetItem):
        rid = item.text()
        self._load_room_into_inspector(rid)
        self.map_view.selected_id = rid
        self.map_view.update()

    def _on_desc_variant_switched(self, key: str):
        if key:
            self.desc_text.setPlainText(self._desc_variants.get(key, ""))

    def _add_variant(self):
        new_key, ok = QInputDialog.getText(self, "New Variant", "Variant key (e.g. description_off):")
        if not ok or not new_key: return
        if new_key in self._desc_variants:
            QMessageBox.warning(self, "Variant", "Variant key already exists.")
            return
        self._desc_variants[new_key] = ""
        self.desc_variant_combo.addItem(new_key)
        self.desc_variant_combo.setCurrentText(new_key)
        self.desc_text.clear()

    def _remove_variant(self):
        key = self.desc_variant_combo.currentText()
        if not key or key == "description":
            QMessageBox.warning(self, "Variant", "Cannot remove base 'description'.")
            return
        self._desc_variants.pop(key, None)
        idx = self.desc_variant_combo.findText(key)
        if idx >= 0: self.desc_variant_combo.removeItem(idx)
        self.desc_variant_combo.setCurrentText("description")
        self.desc_text.setPlainText(self._desc_variants.get("description", ""))

    def _save_current_room(self):
        if not self.current_room_id: return
        rid = self.current_room_id
        r = self.rooms_by_id[rid]

        r["title"] = self.title_edit.text().strip()
        r["env"]   = self.env_edit.text().strip()
        r["background_image"] = self.bg_edit.text().strip()
        r["pos"]   = [self.pos_x.value(), self.pos_y.value()]

        cur_key = self.desc_variant_combo.currentText()
        if cur_key:
            self._desc_variants[cur_key] = self.desc_text.toPlainText()

        for key in [k for k in list(r.keys()) if k.startswith("description")]:
            if key not in self._desc_variants:
                r.pop(key, None)
        for k, v in self._desc_variants.items():
            r[k] = v

        conns = {}
        if self.conn_north.text().strip(): conns["north"] = self.conn_north.text().strip()
        if self.conn_south.text().strip(): conns["south"] = self.conn_south.text().strip()
        if self.conn_east.text().strip():  conns["east"]  = self.conn_east.text().strip()
        if self.conn_west.text().strip():  conns["west"]  = self.conn_west.text().strip()
        r["connections"] = conns

        r["items"] = [s.strip() for s in self.items_edit.text().split(",") if s.strip()]
        r["npcs"]  = [s.strip() for s in self.npcs_edit.text().split(",") if s.strip()]

        for editor, key in [(self.enemies_edit, "enemies"), (self.actions_edit, "actions"), (self.state_edit, "state")]:
            try:
                r[key] = json.loads(editor.toPlainText() or ('[]' if key != 'state' else '{}'))
            except json.JSONDecodeError as e:
                QMessageBox.critical(self, "Invalid JSON", f"{key.capitalize()} JSON invalid:\n{e}")
                return

        if self._save_rooms():
            self._refresh_room_list()
            self._refresh_map()

# --------------------------------------------------------
# Entry
# --------------------------------------------------------
if __name__ == "__main__":
    app = QApplication(sys.argv)
    PROJECT_ROOT = _detect_project_root()
    editor = RoomEditor(PROJECT_ROOT)
    editor.show()
    sys.exit(app.exec_())
