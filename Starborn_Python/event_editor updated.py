#!/usr/bin/env python3
"""
Event Editor for Starborn - v2.2
"""
import sys, os, json
from PyQt5.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QHBoxLayout, QListWidget,
    QLineEdit, QPushButton, QLabel, QGroupBox, QFormLayout,
    QTextEdit, QMessageBox, QListWidgetItem, QCheckBox, QInputDialog
)

class EventEditor(QWidget):
    def __init__(self, root_dir):
        super().__init__()
        self.root = root_dir
        self.setWindowTitle("Starborn Event Editor")
        self.events = {}
        self.current_id = None
        self.loadEvents()
        self.initUI()

    def loadEvents(self):
        path = os.path.join(self.root, "events.json")
        try:
            with open(path, "r", encoding="utf-8") as f:
                raw = json.load(f)
                self.events = {e["id"]: e for e in raw}
        except Exception as e:
            QMessageBox.critical(self, "Load Error", f"Failed to load events.json:\n{e}")
            self.events = {}

    def saveEvents(self):
        path = os.path.join(self.root, "events.json")
        try:
            with open(path, "w", encoding="utf-8") as f:
                out = list(self.events.values())
                json.dump(out, f, indent=4)
            QMessageBox.information(self, "Saved", "events.json saved successfully.")
        except Exception as e:
            QMessageBox.critical(self, "Save Error", f"Failed to save events.json:\n{e}")

    def initUI(self):
        layout = QHBoxLayout(self)
        left = QVBoxLayout()
        self.search = QLineEdit(); self.search.setPlaceholderText("Search events...")
        self.search.textChanged.connect(self.refreshList)
        left.addWidget(self.search)
        self.event_list = QListWidget(); self.event_list.itemClicked.connect(self.onSelect)
        left.addWidget(self.event_list)
        btns = QHBoxLayout()
        add = QPushButton("Add Event"); add.clicked.connect(self.onAdd)
        remove = QPushButton("Remove Event"); remove.clicked.connect(self.onRemove)
        btns.addWidget(add); btns.addWidget(remove)
        left.addLayout(btns)
        layout.addLayout(left, 1)

        right = QVBoxLayout()
        form = QFormLayout()
        self.desc_edit = QTextEdit()
        self.trigger_edit = QTextEdit()
        self.actions_edit = QTextEdit()
        form.addRow("Description:", self.desc_edit)
        form.addRow("Trigger (JSON Object):", self.trigger_edit)
        form.addRow("Actions (JSON Array):", self.actions_edit)
        right.addLayout(form)
        save = QPushButton("Save Changes"); save.clicked.connect(self.onSave)
        right.addWidget(save)
        layout.addLayout(right, 3)
        self.refreshList()

    def refreshList(self):
        self.event_list.clear()
        ft = self.search.text().lower()
        for eid in sorted(self.events.keys()):
            if ft in eid.lower():
                self.event_list.addItem(QListWidgetItem(eid))

    def onSelect(self, item):
        self._save_form_to_memory()
        eid = item.text()
        self.current_id = eid
        e = self.events[eid]
        self.desc_edit.setPlainText(e.get("description", ""))
        self.trigger_edit.setPlainText(json.dumps(e.get("trigger", {}), indent=2))
        self.actions_edit.setPlainText(json.dumps(e.get("actions", []), indent=2))

    def onAdd(self):
        # *** THIS IS THE FIX ***
        # Use QInputDialog to get text from the user.
        new_id, ok = QInputDialog.getText(self, "Add Event", "Enter event ID:")
        if ok and new_id:
            if new_id in self.events:
                QMessageBox.warning(self, "Error", "Event ID already exists.")
                return
            self.events[new_id] = {"id": new_id, "description": "", "trigger": {}, "actions": []}
            self.refreshList()

    def onRemove(self):
        if not self.current_id: return
        if QMessageBox.question(self, "Delete?", f"Delete {self.current_id}?", QMessageBox.Yes | QMessageBox.No) == QMessageBox.Yes:
            del self.events[self.current_id]
            self.refreshList()
    
    def _save_form_to_memory(self):
        if not self.current_id: return True
        e = self.events[self.current_id]
        e["description"] = self.desc_edit.toPlainText()
        try:
            e["trigger"] = json.loads(self.trigger_edit.toPlainText() or "{}")
            e["actions"] = json.loads(self.actions_edit.toPlainText() or "[]")
            return True
        except json.JSONDecodeError as err:
            QMessageBox.warning(self, "Invalid JSON", f"Could not save, invalid JSON in Trigger or Actions:\n{err}")
            return False

    def onSave(self):
        if self._save_form_to_memory():
            self.saveEvents()

if __name__ == "__main__":
    app = QApplication(sys.argv)
    root_path = os.path.dirname(__file__)
    editor = EventEditor(root_path)
    editor.resize(1000, 600)
    editor.show()
    sys.exit(app.exec_())