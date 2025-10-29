# /tools/shop_editor.py
# Starborn Shop Editor (Catalog model: no per-item stock, no merchant credits)
from __future__ import annotations

import json, os, sys, re
from pathlib import Path
from typing import Optional, Dict, Any
from collections import defaultdict

from PyQt5.QtWidgets import (
    QApplication, QMainWindow, QWidget, QListWidget, QListWidgetItem,
    QSplitter, QVBoxLayout, QHBoxLayout, QLabel, QLineEdit, QPushButton,
    QFormLayout, QFileDialog, QMessageBox, QGroupBox, QComboBox,
    QTableWidget, QTableWidgetItem, QHeaderView, QTabWidget, QDialog,
    QDialogButtonBox, QDoubleSpinBox
)
from PyQt5.QtCore import Qt

# ---- Project root detection (aligns with Studio) --------------------------
try:
    from data_core import detect_project_root
except Exception:
    def detect_project_root(hint: Optional[Path] = None) -> Path:
        here = Path(__file__).resolve()
        return here.parent.parent  # .../Starborn

# ----------------- IO helpers -----------------
def atomic_write_json(path: Path, data: dict | list):
    tmp = path.with_suffix(path.suffix + ".tmp")
    path.parent.mkdir(parents=True, exist_ok=True)
    with tmp.open("w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    tmp.replace(path)

def load_json(path: Path, default):
    if not path.exists():
        return default
    try:
        with path.open("r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return default

def norm_name(s: str) -> str:
    if not s:
        return ""
    s = s.strip()
    s = re.sub(r"^(a|an|the)\s+", "", s, flags=re.I)
    return s.lower()

# ----------------- Item index -----------------
class ItemIndex:
    def __init__(self, items_list):
        # canonical_key -> item dict
        self.by_key: Dict[str, Dict[str, Any]] = {}
        self.display_order: list[str] = []
        for it in items_list:
            key = it.get("id") or it.get("name")
            if not key:
                continue
            self.by_key[norm_name(key)] = it
            self.display_order.append(key)

    def keys(self):
        return list(self.display_order)

    def find(self, key_or_name: str | None):
        if not key_or_name:
            return None
        it = self.by_key.get(norm_name(key_or_name))
        if it:
            return it
        # Alias support if you add it later
        return None

    def price_for_shop_sell(self, item: dict, sell_markup: float) -> int:
        # Prefer explicit buy_price; fallback to value*2, else 0
        bp = item.get("buy_price")
        if bp is None:
            val = item.get("value", 0)
            bp = val * 2
        return int(round(bp * sell_markup))

    def price_for_shop_buy(self, item: dict, buy_markdown: float) -> int:
        sp = item.get("sell_price")
        if sp is None:
            val = item.get("value", 0)
            sp = val * buy_markdown
        return int(round(sp))

# ----------------- Legacy conversion -----------------
def is_legacy_shops(obj: dict) -> bool:
    # Legacy had per-item "inventory" entries (with optional stock/unlock_milestone)
    return isinstance(obj, dict) and any(isinstance(v, dict) and "inventory" in v for v in obj.values())

def to_catalog(legacy: dict) -> dict:
    """Convert legacy shops.json (with per-item inventory/stock) into catalog schema."""
    out: Dict[str, Any] = {}
    for shop_id, sdata in legacy.items():
        name = sdata.get("name", shop_id)
        portrait = sdata.get("shopkeeper_portrait") or sdata.get("portrait") or ""
        sells_items: list[str] = []
        gates: Dict[str, Any] = {}
        for entry in sdata.get("inventory", []):
            iid = entry.get("item_id")
            if not iid:
                continue
            if iid not in sells_items:
                sells_items.append(iid)
            if "unlock_milestone" in entry:
                gates[iid] = {"milestones": [entry["unlock_milestone"]]}
        out[shop_id] = {
            "name": name,
            "portrait": portrait,
            "pricing": {"sell_markup": 1.20, "buy_markdown": 0.35},
            "sells": {"items": sells_items, "rules": {"types_in": [], "subtypes_in": []}, "gates": gates},
            "buys": {"accept_types": ["consumable","weapon","armor","ingredient","component"], "blacklist": []}
        }
    return out

# ----------------- Main editor -----------------
class ShopEditor(QMainWindow):
    def __init__(self, project_root: Optional[Path] = None):
        super().__init__()
        self.root = detect_project_root(project_root)
        self.setWindowTitle("Starborn – Shop Editor (Catalog)")
        self.resize(1200, 750)

        # Data paths
        self.items_path = self.root / "items.json"
        self.npcs_path  = self.root / "npcs.json"
        self.rooms_path = self.root / "rooms.json"
        self.shops_path = self.root / "shops.json"

        # Load data
        self.items = load_json(self.items_path, [])
        self.items_index = ItemIndex(self.items)
        self.npcs = load_json(self.npcs_path, [])
        self.rooms = load_json(self.rooms_path, [])
        self.shops: Dict[str, Any] = load_json(self.shops_path, {})

        # Convert legacy if present
        if is_legacy_shops(self.shops):
            resp = QMessageBox.question(
                self, "Convert legacy shops.json?",
                "Detected legacy shops.json (with per-item stock).\n\n"
                "Convert to catalog format now?\n• No stock\n• No merchant credits\n• Keeps milestone gates\n\n"
                "A backup will be saved as shops.legacy.json."
            )
            if resp == QMessageBox.StandardButton.Yes:
                backup = self.shops_path.with_name("shops.legacy.json")
                atomic_write_json(backup, self.shops)
                self.shops = to_catalog(self.shops)
                atomic_write_json(self.shops_path, self.shops)

        self._build_ui()
        self._reload_list()

    # ---------- Studio hooks ----------
    def save(self) -> bool:
        """Called by Studio 'Save All'."""
        # Commit current form (if any) before writing file
        if self.shop_list.currentItem():
            new_id, payload = self._gather_form()
            old_id = self.shop_list.currentItem().text()
            if new_id != old_id and new_id in self.shops:
                QMessageBox.warning(self, "Duplicate ID", f"Shop id '{new_id}' already exists.")
                return False
            if new_id != old_id:
                self.shops.pop(old_id, None)
                # Update the UI list entry text
                self.shop_list.currentItem().setText(new_id)
            self.shops[new_id] = payload
        atomic_write_json(self.shops_path, self.shops)
        return True

    def refresh_refs(self):
        """Called by Studio 'Refresh Refs' to reload lookups."""
        self.items = load_json(self.items_path, [])
        self.items_index = ItemIndex(self.items)
        # no need to touch shops; visible on preview refresh

    def select_id(self, ident: str):
        """Called by Studio 'goto' to focus a shop by id."""
        matches = self.shop_list.findItems(ident, Qt.MatchFlag.MatchExactly)
        if matches:
            self.shop_list.setCurrentItem(matches[0])

    # ---------- UI ----------
    def _build_ui(self):
        splitter = QSplitter(self)
        self.setCentralWidget(splitter)

        # Left: shop list
        left = QWidget()
        left_v = QVBoxLayout(left)
        self.shop_list = QListWidget()
        left_v.addWidget(self.shop_list)

        btns = QHBoxLayout()
        self.btn_add = QPushButton("New Shop")
        self.btn_del = QPushButton("Delete Shop")
        btns.addWidget(self.btn_add)
        btns.addWidget(self.btn_del)
        left_v.addLayout(btns)

        splitter.addWidget(left)

        # Right: tabs
        right = QWidget()
        right_v = QVBoxLayout(right)
        self.tabs = QTabWidget()
        right_v.addWidget(self.tabs)

        # Basics
        basics = QWidget()
        form = QFormLayout(basics)

        self.inp_id = QLineEdit()
        self.inp_name = QLineEdit()

        self.inp_portrait = QLineEdit()
        self.btn_portrait = QPushButton("…")
        pr = QHBoxLayout(); pr.addWidget(self.inp_portrait); pr.addWidget(self.btn_portrait)

        self.inp_sell_markup  = _fspin(1.20, 0.0, 10.0)
        self.inp_buy_markdown = _fspin(0.35, 0.0, 10.0)

        form.addRow("Shop ID", self.inp_id)
        form.addRow("Name", self.inp_name)
        form.addRow("Portrait", pr)
        form.addRow("Sell Markup", self.inp_sell_markup)
        form.addRow("Buy Markdown", self.inp_buy_markdown)
        self.tabs.addTab(basics, "Basics")

        # Sells
        sells = QWidget()
        s_v = QVBoxLayout(sells)

        grp_items = QGroupBox("Manual Items (explicit)")
        gli = QVBoxLayout(grp_items)

        self.tbl_items = QTableWidget(0, 2)
        self.tbl_items.setHorizontalHeaderLabels(["Item (id or name)", "Milestone gate (optional CSV)"])
        self.tbl_items.horizontalHeader().setSectionResizeMode(0, QHeaderView.ResizeMode.Stretch)
        self.tbl_items.horizontalHeader().setSectionResizeMode(1, QHeaderView.ResizeMode.Stretch)
        gli.addWidget(self.tbl_items)

        irow = QHBoxLayout()
        self.btn_add_item = QPushButton("+ Add Item")
        self.btn_del_item = QPushButton("– Remove Selected")
        irow.addWidget(self.btn_add_item)
        irow.addWidget(self.btn_del_item)
        gli.addLayout(irow)

        grp_rules = QGroupBox("Rule Filters (optional)")
        glr = QFormLayout(grp_rules)
        self.inp_types_in = QLineEdit()
        self.inp_subtypes_in = QLineEdit()
        glr.addRow("types_in (CSV)", self.inp_types_in)
        glr.addRow("subtypes_in (CSV)", self.inp_subtypes_in)

        s_v.addWidget(grp_items)
        s_v.addWidget(grp_rules)
        self.tabs.addTab(sells, "Sells")

        # Buys
        buys = QWidget()
        bform = QFormLayout(buys)
        self.inp_accept_types = QLineEdit()
        self.inp_blacklist = QLineEdit()
        bform.addRow("accept_types (CSV)", self.inp_accept_types)
        bform.addRow("blacklist item ids (CSV)", self.inp_blacklist)
        self.tabs.addTab(buys, "Buys")

        # Preview
        preview = QWidget()
        pv = QVBoxLayout(preview)
        self.btn_preview = QPushButton("Refresh Preview")
        self.tbl_preview = QTableWidget(0, 4)
        self.tbl_preview.setHorizontalHeaderLabels(["Item", "Type", "Sell Price", "Buy Price"])
        self.tbl_preview.horizontalHeader().setSectionResizeMode(0, QHeaderView.ResizeMode.Stretch)
        self.tbl_preview.horizontalHeader().setSectionResizeMode(1, QHeaderView.ResizeMode.ResizeToContents)
        self.tbl_preview.horizontalHeader().setSectionResizeMode(2, QHeaderView.ResizeMode.ResizeToContents)
        self.tbl_preview.horizontalHeader().setSectionResizeMode(3, QHeaderView.ResizeMode.ResizeToContents)
        pv.addWidget(self.btn_preview)
        pv.addWidget(self.tbl_preview)
        self.tabs.addTab(preview, "Preview")

        # Bottom bar
        bar = QHBoxLayout()
        self.btn_save = QPushButton("Save")
        self.btn_revert = QPushButton("Revert")
        bar.addStretch(1)
        bar.addWidget(self.btn_revert)
        bar.addWidget(self.btn_save)
        right_v.addLayout(bar)

        splitter.addWidget(right)
        splitter.setStretchFactor(1, 1)

        # Signals
        self.shop_list.currentItemChanged.connect(self._on_shop_selected)
        self.btn_add.clicked.connect(self._add_shop)
        self.btn_del.clicked.connect(self._delete_shop)
        self.btn_portrait.clicked.connect(self._pick_portrait)

        self.btn_add_item.clicked.connect(self._add_item_row)
        self.btn_del_item.clicked.connect(self._delete_item_rows)
        self.btn_preview.clicked.connect(self._refresh_preview)

        self.btn_save.clicked.connect(lambda: self.save() and QMessageBox.information(self, "Saved", "shops.json updated."))
        self.btn_revert.clicked.connect(self._reload_list)

    # ---------- CRUD/list ----------
    def _reload_list(self):
        cur_id = self.shop_list.currentItem().text() if self.shop_list.currentItem() else None
        self.shop_list.clear()
        for sid in sorted(self.shops.keys()):
            self.shop_list.addItem(QListWidgetItem(sid))
        if self.shop_list.count():
            # best effort keep selection
            row = 0
            if cur_id:
                hits = self.shop_list.findItems(cur_id, Qt.MatchFlag.MatchExactly)
                if hits:
                    row = self.shop_list.row(hits[0])
            self.shop_list.setCurrentRow(row)

    def _on_shop_selected(self, cur: QListWidgetItem, prev: QListWidgetItem):
        if not cur:
            self._clear_form()
            return
        self._load_form(cur.text())

    def _add_shop(self):
        base = "shop"
        sid = base
        i = 1
        while sid in self.shops:
            sid = f"{base}_{i}"; i += 1
        self.shops[sid] = {
            "name": sid,
            "portrait": "",
            "pricing": {"sell_markup": 1.20, "buy_markdown": 0.35},
            "sells": {"items": [], "rules": {"types_in": [], "subtypes_in": []}, "gates": {}},
            "buys": {"accept_types": ["consumable","weapon","armor","ingredient","component"], "blacklist": []}
        }
        self._reload_list()
        hits = self.shop_list.findItems(sid, Qt.MatchFlag.MatchExactly)
        if hits:
            self.shop_list.setCurrentItem(hits[0])

    def _delete_shop(self):
        cur = self.shop_list.currentItem()
        if not cur: return
        sid = cur.text()
        if QMessageBox.question(self, "Delete Shop", f"Delete '{sid}'?") == QMessageBox.StandardButton.Yes:
            self.shops.pop(sid, None)
            self._reload_list()

    # ---------- Form IO ----------
    def _clear_form(self):
        self.inp_id.setText("")
        self.inp_name.setText("")
        self.inp_portrait.setText("")
        self.inp_sell_markup.setValue(1.20)
        self.inp_buy_markdown.setValue(0.35)
        self.tbl_items.setRowCount(0)
        self.inp_types_in.setText("")
        self.inp_subtypes_in.setText("")
        self.inp_accept_types.setText("")
        self.inp_blacklist.setText("")
        self.tbl_preview.setRowCount(0)

    def _load_form(self, sid: str):
        self._clear_form()
        data = self.shops.get(sid, {})
        self.inp_id.setText(sid)
        self.inp_name.setText(data.get("name","") or sid)
        self.inp_portrait.setText(data.get("portrait","") or data.get("shopkeeper_portrait",""))
        pricing = data.get("pricing", {})
        self.inp_sell_markup.setValue(float(pricing.get("sell_markup", 1.20)))
        self.inp_buy_markdown.setValue(float(pricing.get("buy_markdown", 0.35)))

        sells = data.get("sells", {})
        items = sells.get("items", [])
        gates = sells.get("gates", {})
        self.tbl_items.setRowCount(0)
        for iid in items:
            self._append_item_row(iid, ",".join(gates.get(iid, {}).get("milestones", [])))

        rules = sells.get("rules", {})
        self.inp_types_in.setText(",".join(rules.get("types_in", [])))
        self.inp_subtypes_in.setText(",".join(rules.get("subtypes_in", [])))

        buys = data.get("buys", {})
        self.inp_accept_types.setText(",".join(buys.get("accept_types", [])))
        self.inp_blacklist.setText(",".join(buys.get("blacklist", [])))

    def _gather_form(self):
        sid = self.inp_id.text().strip()
        name = self.inp_name.text().strip() or sid
        portrait = self.inp_portrait.text().strip()
        sell_markup = float(self.inp_sell_markup.value())
        buy_markdown = float(self.inp_buy_markdown.value())

        # manual items with gates
        items: list[str] = []
        gates: Dict[str, Any] = {}
        for r in range(self.tbl_items.rowCount()):
            iid = (self.tbl_items.item(r, 0).text() if self.tbl_items.item(r, 0) else "").strip()
            gate = (self.tbl_items.item(r, 1).text() if self.tbl_items.item(r, 1) else "").strip()
            if not iid:
                continue
            items.append(iid)
            if gate:
                gates[iid] = {"milestones": [g.strip() for g in gate.split(",") if g.strip()]}

        types_in = [t.strip() for t in self.inp_types_in.text().split(",") if t.strip()]
        subtypes_in = [t.strip() for t in self.inp_subtypes_in.text().split(",") if t.strip()]
        accept_types = [t.strip() for t in self.inp_accept_types.text().split(",") if t.strip()]
        blacklist = [t.strip() for t in self.inp_blacklist.text().split(",") if t.strip()]

        payload = {
            "name": name,
            "portrait": portrait,
            "pricing": {"sell_markup": sell_markup, "buy_markdown": buy_markdown},
            "sells": {"items": items, "rules": {"types_in": types_in, "subtypes_in": subtypes_in}, "gates": gates},
            "buys": {"accept_types": accept_types, "blacklist": blacklist}
        }
        return sid, payload

    # ---------- Items table helpers ----------
    def _append_item_row(self, item_key: str, milestone_csv: str = ""):
        r = self.tbl_items.rowCount()
        self.tbl_items.insertRow(r)
        self.tbl_items.setItem(r, 0, QTableWidgetItem(item_key))
        self.tbl_items.setItem(r, 1, QTableWidgetItem(milestone_csv))

    def _add_item_row(self):
        # Quick picker dialog of known items
        dlg = QDialog(self)
        dlg.setWindowTitle("Add Item")
        v = QVBoxLayout(dlg)
        v.addWidget(QLabel("Double-click an item to add"))
        table = QTableWidget(0, 3)
        table.setHorizontalHeaderLabels(["Key", "Type", "Base price"])
        table.horizontalHeader().setSectionResizeMode(0, QHeaderView.ResizeMode.Stretch)
        table.horizontalHeader().setSectionResizeMode(1, QHeaderView.ResizeMode.ResizeToContents)
        table.horizontalHeader().setSectionResizeMode(2, QHeaderView.ResizeMode.ResizeToContents)
        v.addWidget(table)

        for key in self.items_index.keys():
            it = self.items_index.find(key)
            if not it: continue
            r = table.rowCount()
            table.insertRow(r)
            table.setItem(r, 0, QTableWidgetItem(str(it.get("id") or it.get("name"))))
            table.setItem(r, 1, QTableWidgetItem(str(it.get("type",""))))
            base_price = it.get("buy_price", it.get("value",""))
            table.setItem(r, 2, QTableWidgetItem(str(base_price)))

        table.itemDoubleClicked.connect(lambda _it: self._pick_item_from_dialog(dlg, table))
        v.addWidget(QDialogButtonBox(QDialogButtonBox.StandardButton.Close, parent=dlg))
        dlg.resize(700, 500)
        dlg.exec()

    def _pick_item_from_dialog(self, dlg, table):
        r = table.currentRow()
        key = table.item(r, 0).text()
        self._append_item_row(key, "")
        dlg.accept()

    def _delete_item_rows(self):
        rows = sorted({i.row() for i in self.tbl_items.selectedIndexes()}, reverse=True)
        for r in rows:
            self.tbl_items.removeRow(r)

    # ---------- Preview ----------
    def _refresh_preview(self):
        sid, data = self._gather_form()
        sells = data["sells"]
        chosen = set(sells["items"])
        types_in = set(sells["rules"]["types_in"])
        subtypes_in = set(sells["rules"]["subtypes_in"])

        # expand by rule filters
        if types_in or subtypes_in:
            for key in self.items_index.keys():
                it = self.items_index.find(key)
                if not it: continue
                if types_in and it.get("type") not in types_in:
                    continue
                if subtypes_in and it.get("subtype","") not in subtypes_in:
                    continue
                chosen.add(it.get("id") or it.get("name"))

        markup = float(data["pricing"]["sell_markup"])
        markdown = float(data["pricing"]["buy_markdown"])
        self.tbl_preview.setRowCount(0)
        for key in sorted(chosen):
            it = self.items_index.find(key)
            if not it: continue
            r = self.tbl_preview.rowCount()
            self.tbl_preview.insertRow(r)
            self.tbl_preview.setItem(r, 0, QTableWidgetItem(str(it.get("id") or it.get("name"))))
            self.tbl_preview.setItem(r, 1, QTableWidgetItem(str(it.get("type",""))))
            self.tbl_preview.setItem(r, 2, QTableWidgetItem(str(self.items_index.price_for_shop_sell(it, markup))))
            self.tbl_preview.setItem(r, 3, QTableWidgetItem(str(self.items_index.price_for_shop_buy(it, markdown))))

    # ---------- Misc ----------
    def _pick_portrait(self):
        fn, _ = QFileDialog.getOpenFileName(self, "Choose portrait image", str(self.root), "Images (*.png *.jpg *.jpeg)")
        if fn:
            rel = os.path.relpath(fn, self.root)
            self.inp_portrait.setText(rel.replace("\\","/"))

# ---- small helpers ----
def _fspin(value: float, lo: float, hi: float):
    w = QDoubleSpinBox()
    w.setDecimals(2)
    w.setRange(lo, hi)
    w.setValue(value)
    return w

# ---- Entry point ----
def main():
    app = QApplication(sys.argv)
    win = ShopEditor()
    win.show()
    sys.exit(app.exec_() if hasattr(app, "exec_") else app.exec())

if __name__ == "__main__":
    main()
