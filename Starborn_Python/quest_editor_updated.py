#!/usr/bin/env python3
"""
Quest Editor for Starborn - v2.3
"""
import sys, os, json
from PyQt5.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QHBoxLayout, QListWidget,
    QLineEdit, QPushButton, QLabel, QGroupBox, QFormLayout,
    QComboBox, QTextEdit, QMessageBox, QListWidgetItem, QSpinBox,
    QInputDialog
)
# *** THIS IS THE FIX ***
# Import the 'Qt' core module which contains the MatchExactly constant.
from PyQt5.QtCore import Qt

class QuestEditor(QWidget):
    def __init__(self, root_dir):
        super().__init__()
        self.root = root_dir
        self.setWindowTitle("Starborn Quest Editor")
        self.quests = {}
        self.current_id = None
        self.loadQuests()
        self.initUI()

    def loadQuests(self):
        path = os.path.join(self.root, "quests.json")
        try:
            with open(path, "r", encoding="utf-8") as f:
                raw = json.load(f)
                self.quests = {q["id"]: q for q in raw}
        except Exception as e:
            QMessageBox.critical(self, "Load Error", f"Failed to load quests.json:\n{e}")
            self.quests = {}

    def saveQuests(self):
        path = os.path.join(self.root, "quests.json")
        try:
            with open(path, "w", encoding="utf-8") as f:
                out = list(self.quests.values())
                json.dump(out, f, indent=4)
            QMessageBox.information(self, "Saved", "quests.json saved successfully.")
        except Exception as e:
            QMessageBox.critical(self, "Save Error", f"Failed to save quests.json:\n{e}")

    def initUI(self):
        layout = QHBoxLayout(self)
        left = QVBoxLayout()
        self.search = QLineEdit()
        self.search.setPlaceholderText("Search quests...")
        self.search.textChanged.connect(self.refreshList)
        left.addWidget(self.search)
        self.quest_list = QListWidget()
        self.quest_list.itemClicked.connect(self.onSelect)
        left.addWidget(self.quest_list)
        btns = QHBoxLayout()
        add = QPushButton("Add Quest"); add.clicked.connect(self.onAdd)
        remove = QPushButton("Remove Quest"); remove.clicked.connect(self.onRemove)
        btns.addWidget(add); btns.addWidget(remove)
        left.addLayout(btns)
        layout.addLayout(left, 1)

        right = QVBoxLayout()
        form = QFormLayout()
        self.title_edit = QLineEdit()
        self.status_combo = QComboBox(); self.status_combo.addItems(["inactive", "active", "success", "failure"])
        self.type_combo = QComboBox(); self.type_combo.addItems(["", "fetch", "kill", "explore"])
        self.desc_edit = QTextEdit()
        self.giver_edit = QLineEdit()
        self.receiver_edit = QLineEdit()
        self.requires_edit = QLineEdit()
        self.required_item_edit = QLineEdit()
        self.reward_item_edit = QLineEdit()
        self.xp_reward_spin = QSpinBox(); self.xp_reward_spin.setRange(0, 10000)
        
        form.addRow("Title:", self.title_edit)
        form.addRow("Status:", self.status_combo)
        form.addRow("Type:", self.type_combo)
        form.addRow("Description:", self.desc_edit)
        form.addRow("Giver NPC ID:", self.giver_edit)
        form.addRow("Receiver NPC ID:", self.receiver_edit)
        form.addRow("Requires (Quest IDs):", self.requires_edit)
        form.addRow("Required Item:", self.required_item_edit)
        form.addRow("Reward Item:", self.reward_item_edit)
        form.addRow("XP Reward:", self.xp_reward_spin)

        right.addLayout(form)
        save = QPushButton("Save Changes"); save.clicked.connect(self.onSave)
        right.addWidget(save)
        layout.addLayout(right, 3)
        self.refreshList()

    def refreshList(self):
        self.quest_list.clear()
        ft = self.search.text().lower()
        for qid in sorted(self.quests.keys()):
            if ft in qid.lower():
                self.quest_list.addItem(QListWidgetItem(qid))

    def onSelect(self, item):
        self._save_form_to_memory()
        qid = item.text()
        self.current_id = qid
        q = self.quests[qid]
        self.title_edit.setText(q.get("title", ""))
        self.status_combo.setCurrentText(q.get("status", "inactive"))
        self.type_combo.setCurrentText(q.get("type", ""))
        self.desc_edit.setPlainText(q.get("description", ""))
        self.giver_edit.setText(q.get("giver", ""))
        self.receiver_edit.setText(q.get("receiver", ""))
        self.requires_edit.setText(",".join(q.get("requires", []) if isinstance(q.get("requires"), list) else [q.get("requires", "")]))
        self.required_item_edit.setText(q.get("required_item", ""))
        self.reward_item_edit.setText(q.get("reward_item", ""))
        self.xp_reward_spin.setValue(q.get("xp_reward", 0))

    def onAdd(self):
        new_id, ok = QInputDialog.getText(self, "Add Quest", "Enter unique quest ID:")
        if ok and new_id:
            if new_id in self.quests:
                QMessageBox.warning(self, "Error", "Quest ID already exists.")
                return
            self.quests[new_id] = {"id": new_id, "title": new_id, "status": "inactive"}
            self.refreshList()
            items = self.quest_list.findItems(new_id, Qt.MatchExactly)
            if items: self.quest_list.setCurrentItem(items[0]); self.onSelect(items[0])

    def onRemove(self):
        if not self.current_id: return
        if QMessageBox.question(self, "Delete?", f"Delete quest '{self.current_id}'?", QMessageBox.Yes | QMessageBox.No) == QMessageBox.Yes:
            del self.quests[self.current_id]
            self.current_id = None
            self.refreshList()
    
    def _save_form_to_memory(self):
        if not self.current_id: return
        q = self.quests[self.current_id]
        q["title"] = self.title_edit.text()
        q["status"] = self.status_combo.currentText()
        q["type"] = self.type_combo.currentText()
        q["description"] = self.desc_edit.toPlainText()
        q["giver"] = self.giver_edit.text()
        q["receiver"] = self.receiver_edit.text()
        requires = [r.strip() for r in self.requires_edit.text().split(",") if r.strip()]
        if len(requires) == 1: q["requires"] = requires[0]
        elif len(requires) > 1: q["requires"] = requires
        else: q.pop("requires", None)
        q["required_item"] = self.required_item_edit.text()
        q["reward_item"] = self.reward_item_edit.text()
        q["xp_reward"] = self.xp_reward_spin.value()

    def onSave(self):
        self._save_form_to_memory()
        self.saveQuests()

if __name__ == "__main__":
    app = QApplication(sys.argv)
    root_path = os.path.dirname(__file__)
    editor = QuestEditor(root_path)
    editor.resize(1000, 600)
    editor.show()
    sys.exit(app.exec_())