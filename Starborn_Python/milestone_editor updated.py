#!/usr/bin/env python3
"""
Milestone Editor for Starborn - v2.2
"""
import sys, os, json
from PyQt5.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QHBoxLayout, QListWidget,
    QLineEdit, QPushButton, QLabel, QFormLayout, QTextEdit,
    QMessageBox, QListWidgetItem, QInputDialog
)

class MilestoneEditor(QWidget):
    def __init__(self, root_dir):
        super().__init__()
        self.root = root_dir
        self.setWindowTitle("Starborn Milestone Editor")
        self.milestones = {}
        self.current_id = None
        self.loadMilestones()
        self.initUI()

    def loadMilestones(self):
        path = os.path.join(self.root, "milestones.json")
        try:
            with open(path, "r", encoding="utf-8") as f:
                raw = json.load(f)
                self.milestones = {m["id"]: m for m in raw}
        except Exception as e:
            QMessageBox.critical(self, "Load Error", f"Failed to load milestones.json:\n{e}")
            self.milestones = {}

    def saveMilestones(self):
        path = os.path.join(self.root, "milestones.json")
        try:
            with open(path, "w", encoding="utf-8") as f:
                out = list(self.milestones.values())
                json.dump(out, f, indent=4)
            QMessageBox.information(self, "Saved", "milestones.json saved successfully.")
        except Exception as e:
            QMessageBox.critical(self, "Save Error", f"Failed to save milestones.json:\n{e}")

    def initUI(self):
        layout = QHBoxLayout(self)
        left = QVBoxLayout()
        self.search = QLineEdit(); self.search.setPlaceholderText("Search milestones...")
        self.search.textChanged.connect(self.refreshList)
        left.addWidget(self.search)
        self.ms_list = QListWidget(); self.ms_list.itemClicked.connect(self.onSelect)
        left.addWidget(self.ms_list)
        btns = QHBoxLayout()
        add = QPushButton("Add"); add.clicked.connect(self.onAdd)
        remove = QPushButton("Remove"); remove.clicked.connect(self.onRemove)
        btns.addWidget(add); btns.addWidget(remove)
        left.addLayout(btns)
        layout.addLayout(left, 1)

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
        save = QPushButton("Save Changes"); save.clicked.connect(self.onSave)
        right.addWidget(save)
        layout.addLayout(right, 3)
        self.refreshList()

    def refreshList(self):
        self.ms_list.clear()
        ft = self.search.text().lower()
        for mid in sorted(self.milestones.keys()):
            if ft in mid.lower():
                self.ms_list.addItem(QListWidgetItem(mid))

    def onSelect(self, item):
        self._save_form_to_memory()
        mid = item.text()
        self.current_id = mid
        m = self.milestones[mid]
        self.name_edit.setText(m.get("name", ""))
        self.desc_edit.setPlainText(m.get("description", ""))
        self.trigger_edit.setPlainText(json.dumps(m.get("trigger", {}), indent=2))
        self.effects_edit.setPlainText(json.dumps(m.get("effects", {}), indent=2))

    def onAdd(self):
        # *** THIS IS THE FIX ***
        # Use QInputDialog to get text from the user.
        new_id, ok = QInputDialog.getText(self, "Add Milestone", "Enter milestone ID:")
        if ok and new_id:
            if new_id in self.milestones:
                QMessageBox.warning(self, "Error", "Milestone ID already exists.")
                return
            self.milestones[new_id] = {
                "id": new_id,
                "name": new_id,
                "description": "",
                "trigger": {},
                "effects": {}
            }
            self.refreshList()

    def onRemove(self):
        if not self.current_id: return
        if QMessageBox.question(self, "Delete?", f"Delete {self.current_id}?", QMessageBox.Yes | QMessageBox.No) == QMessageBox.Yes:
            del self.milestones[self.current_id]
            self.current_id = None
            self.refreshList()

    def _save_form_to_memory(self):
        if not self.current_id: return True
        m = self.milestones[self.current_id]
        m["name"] = self.name_edit.text()
        m["description"] = self.desc_edit.toPlainText()
        try:
            m["trigger"] = json.loads(self.trigger_edit.toPlainText() or "{}")
            m["effects"] = json.loads(self.effects_edit.toPlainText() or "{}")
            return True
        except json.JSONDecodeError as err:
            QMessageBox.warning(self, "Invalid JSON", f"Could not save, invalid JSON in Trigger or Effects:\n{err}")
            return False

    def onSave(self):
        if self._save_form_to_memory():
            self.saveMilestones()

if __name__ == "__main__":
    app = QApplication(sys.argv)
    root_path = os.path.dirname(__file__)
    editor = MilestoneEditor(root_path)
    editor.resize(1000, 600)
    editor.show()
    sys.exit(app.exec_())