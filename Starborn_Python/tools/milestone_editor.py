#!/usr/bin/env python3
"""
Milestone Editor for Starborn - v2.4
- Fix KeyError when switching selection if current_id is stale or was deleted
- Safer list selection using Qt.UserRole to store the true milestone id
- Guarded _save_form_to_memory when the id no longer exists
- Fixed Delete dialog string + clears selection/forms properly
- Silenced Qt layout warnings by setting layout once (no parent in ctor)
- Minor UX: search matches id or name
"""
import sys, os, json
from PyQt5.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QHBoxLayout, QListWidget,
    QLineEdit, QPushButton, QLabel, QFormLayout, QTextEdit,
    QMessageBox, QListWidgetItem, QInputDialog
)
from PyQt5.QtCore import Qt
from theme_kit import ThemeManager         # optional if you want per-editor theme flips
from data_core import detect_project_root, json_load, json_save, unique_id
from editor_undo import UndoManager
from ui_common import attach_status_bar, flash_status, attach_hotkeys, attach_list_context_menu, mark_invalid, clear_invalid
from editor_bus import goto as studio_goto, refresh_references as studio_refresh

class MilestoneEditor(QWidget):
    def __init__(self, root_dir):
        super().__init__()
        self.root = root_dir
        self.setWindowTitle("Milestone Editor")
        self.milestones = {}   # id -> milestone dict
        self.current_id = None

        self.loadMilestones()
        self.initUI()

    # ---------- Data IO ----------
    def loadMilestones(self):
        path = os.path.join(self.root, "milestones.json")
        try:
            with open(path, "r", encoding="utf-8") as f:
                raw = json.load(f)
                # normalize into id->dict
                self.milestones = {m["id"]: dict(m) for m in raw if "id" in m}
        except FileNotFoundError:
            # start empty if not present
            self.milestones = {}
        except Exception as e:
            QMessageBox.critical(self, "Load Error", f"Failed to load milestones.json:\n{e}")
            self.milestones = {}

    def saveMilestones(self):
        path = os.path.join(self.root, "milestones.json")
        try:
            with open(path, "w", encoding="utf-8") as f:
                out = list(self.milestones.values())
                json.dump(out, f, indent=4, ensure_ascii=False)
            QMessageBox.information(self, "Saved", "milestones.json saved successfully.")
        except Exception as e:
            QMessageBox.critical(self, "Save Error", f"Failed to save milestones.json:\n{e}")

    # ---------- UI ----------
    def initUI(self):
        # IMPORTANT: do NOT parent the top-level layout, then setLayout once.
        layout = QHBoxLayout()
        self.setLayout(layout)

        # Left column: search, list, buttons
        left = QVBoxLayout()
        self.search = QLineEdit()
        self.search.setPlaceholderText("Search milestones (id or name)…")
        self.search.textChanged.connect(self.refreshList)
        left.addWidget(self.search)

        self.ms_list = QListWidget()
        self.ms_list.itemClicked.connect(self.onSelect)
        left.addWidget(self.ms_list)

        btns = QHBoxLayout()
        add = QPushButton("Add");    add.clicked.connect(self.onAdd)
        remove = QPushButton("Remove"); remove.clicked.connect(self.onRemove)
        btns.addWidget(add); btns.addWidget(remove)
        left.addLayout(btns)
        layout.addLayout(left, 1)

        # Right column: form
        right = QVBoxLayout()
        form = QFormLayout()
        self.name_edit = QLineEdit()
        self.desc_edit = QTextEdit()
        self.trigger_edit = QTextEdit()
        self.effects_edit = QTextEdit()
        form.addRow("Name:", self.name_edit)
        form.addRow("Description:", self.desc_edit)
        form.addRow("Trigger (JSON Object):", self.trigger_edit)
        form.addRow("Effects (JSON Object):", self.effects_edit)
        right.addLayout(form)

        save_btn = QPushButton("Save Changes")
        save_btn.clicked.connect(self.onSave)
        right.addWidget(save_btn)
        layout.addLayout(right, 3)

        self.refreshList()

    # ---------- List / Selection ----------
    def refreshList(self):
        self.ms_list.clear()
        ft = (self.search.text() or "").lower().strip()

        # sort by id for stability
        for mid in sorted(self.milestones.keys()):
            m = self.milestones[mid]
            name = str(m.get("name", ""))

            if ft and (ft not in mid.lower() and ft not in name.lower()):
                continue

            item = QListWidgetItem(mid)
            # store the true id on the item to decouple from display text
            item.setData(Qt.UserRole, mid)
            self.ms_list.addItem(item)

        # try to keep selection if possible
        self._reselect_current_id_if_present()

    def _reselect_current_id_if_present(self):
        if not self.current_id:
            return
        for i in range(self.ms_list.count()):
            it = self.ms_list.item(i)
            if it.data(Qt.UserRole) == self.current_id:
                self.ms_list.setCurrentItem(it, Qt.NoItemFlags)
                break

    def onSelect(self, item: QListWidgetItem):
        # Safely save current form before switching
        if not self._save_form_to_memory():
            return  # invalid JSON warning already shown; stay on current

        mid = item.data(Qt.UserRole) or item.text()
        self.current_id = mid

        m = self.milestones.get(mid)
        if not m:
            # Selection references missing milestone (e.g., deleted externally)
            QMessageBox.warning(self, "Missing", f"Milestone '{mid}' no longer exists.")
            self.refreshList()
            return

        self._populate_form(m)

    # ---------- Add / Remove ----------
    def onAdd(self):
        new_id, ok = QInputDialog.getText(self, "Add Milestone", "Enter milestone ID:")
        if not ok or not new_id:
            return

        new_id = str(new_id).strip()
        if new_id in self.milestones:
            QMessageBox.warning(self, "Error", "Milestone ID already exists.")
            return

        # Create a new milestone with a complete, valid structure.
        self.milestones[new_id] = {
            "id": new_id,
            "name": new_id,
            "description": "",
            "trigger": {},
            "effects": {}
        }

        self.refreshList()
        # auto-select the new one
        self.current_id = new_id
        self._populate_form(self.milestones[new_id])

    def onRemove(self):
        if not self.current_id:
            QMessageBox.information(self, "No selection", "Select a milestone to delete.")
            return

        mid = self.current_id
        name = self.milestones.get(mid, {}).get("name", mid)

        if QMessageBox.question(
            self,
            "Delete?",
            f"Delete milestone '{name}' ({mid})?",
            QMessageBox.Yes | QMessageBox.No
        ) != QMessageBox.Yes:
            return

        # delete and clear form
        if mid in self.milestones:
            del self.milestones[mid]
        self.current_id = None
        self._clear_form()
        self.refreshList()
        # do not auto-save here; make user click Save to persist, or you can call self.saveMilestones()

    # ---------- Form helpers ----------
    def _populate_form(self, m: dict):
        self.name_edit.setText(str(m.get("name", "")))
        self.desc_edit.setPlainText(str(m.get("description", "")))
        self.trigger_edit.setPlainText(json.dumps(m.get("trigger", {}), indent=2))
        self.effects_edit.setPlainText(json.dumps(m.get("effects", {}), indent=2))

    def _clear_form(self):
        self.name_edit.clear()
        self.desc_edit.clear()
        self.trigger_edit.clear()
        self.effects_edit.clear()

    def _save_form_to_memory(self) -> bool:
        """
        Returns True if saved (or nothing to save), False if invalid JSON and we should
        abort navigation.
        """
        if not self.current_id or self.current_id not in self.milestones:
            return True

        m = self.milestones[self.current_id]
        m["name"] = self.name_edit.text()
        m["description"] = self.desc_edit.toPlainText()
        try:
            trig_txt = self.trigger_edit.toPlainText().strip()
            eff_txt = self.effects_edit.toPlainText().strip()
            m["trigger"] = json.loads(trig_txt) if trig_txt else {}
            m["effects"] = json.loads(eff_txt) if eff_txt else {}
            return True
        except json.JSONDecodeError as err:
            QMessageBox.warning(
                self,
                "Invalid JSON",
                f"Could not save. Fix JSON in Trigger or Effects:\n{err}"
            )
            return False

    # ---------- Save ----------
    def onSave(self):
        if self._save_form_to_memory():
            self.saveMilestones()


if __name__ == "__main__":
    app = QApplication(sys.argv)
    # Use the script directory as the default root (when run directly)
    root_path = os.path.dirname(__file__)
    editor = MilestoneEditor(root_path)
    editor.resize(1000, 600)
    editor.show()
    sys.exit(app.exec_())
