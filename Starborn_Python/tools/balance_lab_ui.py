#!/usr/bin/env python3
# tools/balance_lab_ui.py
from __future__ import annotations

import os, sys, json, csv, subprocess
from pathlib import Path
from typing import Optional, List

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QPushButton, QLabel, QTextEdit,
    QTableView, QMessageBox
)
from PyQt5.QtGui import QStandardItemModel, QStandardItem

DEFAULT_CFG = {
  "defaults": {
    "test_level": 5,
    "baseline_ttk_turns": 4.0,
    "baseline_survive_turns": 4.0,
    "rp_regen_per_turn": 6,
    "crit_chance": 0.05,
    "crit_mult": 1.5,
    "dodge_chance": 0.03,
    "assume_best_single_target_skill": True
  },
  "formulas": {
    "hp_per_vit": 10,
    "atk_per_str": 1.0,
    "spd_per_agi": 1.0,
    "def_per_vit": 0.5,
    "mitigation_per_def_point": 0.004,
    "mitigation_cap": 0.60,
    "base_attack_mult": 1.0,
    "skill_power_weight": {
      "damage_mult": 1.0,
      "heal_value": 0.6,
      "buff_flat_point": 0.15,
      "buff_mult_point": 1.0,
      "rp_cost_weight": 0.06
    }
  },
  "targets": {
    "enemy_group_size": 3,
    "boss_ttk_turns": 8.0,
    "minion_ttk_turns": 2.5
  },
  "tuning": {
    "max_global_scale_step": 0.15,
    "tolerance_turns": 0.25
  },
  "paths": {
    "characters": "characters.json",
    "enemies": "enemies.json",
    "skills": "skills.json",
    "skill_trees_dir": "skill_trees",
    "leveling": "data/leveling_data.json",
    "progression": "data/progression.json",
    "reports_dir": "reports"
  }
}

class BalanceLabPanel(QWidget):
    """Studio tab wrapper for Balance Lab CLI."""
    def __init__(self, project_root: Optional[Path]=None):
        super().__init__()
        self.project_root = Path(project_root or os.getcwd()).resolve()
        self.setObjectName("BalanceLabPanel")
        self.setWindowTitle("Balance Lab")

        self.tool_path = (self.project_root / "tools" / "balance_lab.py")
        self.cfg_path  = (self.project_root / "data" / "balance_targets.json")
        self.reports_dir = (self.project_root / "reports")

        self._ensure_config()
        self._ensure_reports_dir()

        self.lbl = QLabel(f"Project: {self.project_root}")
        self.lbl.setTextInteractionFlags(Qt.TextSelectableByMouse)

        self.btn_analyze = QPushButton("Analyze")
        self.btn_tune = QPushButton("Tune")
        self.btn_apply = QPushButton("Tune & Apply")
        self.btn_open_summary = QPushButton("Open SUMMARY.md")
        self.btn_open_folder = QPushButton("Open Reports Folder")
        self.btn_refresh = QPushButton("Refresh View")

        top = QHBoxLayout()
        for b in (self.btn_analyze, self.btn_tune, self.btn_apply, self.btn_open_summary, self.btn_open_folder, self.btn_refresh):
            top.addWidget(b)
        top.addStretch(1)

        self.summary = QTextEdit(); self.summary.setReadOnly(True)
        self.summary.setPlaceholderText("Run Analyze to generate reports…")

        self.table = QTableView()
        self.model = QStandardItemModel(self)
        self.table.setModel(self.model)
        self.table.horizontalHeader().setStretchLastSection(True)

        root = QVBoxLayout(self)
        root.addWidget(self.lbl)
        root.addLayout(top)
        root.addWidget(QLabel("SUMMARY.md"))
        root.addWidget(self.summary, 2)
        root.addWidget(QLabel("pairwise.csv"))
        root.addWidget(self.table, 3)
        self.setLayout(root)

        self.btn_analyze.clicked.connect(lambda: self._run("analyze"))
        self.btn_tune.clicked.connect(lambda: self._run("tune"))
        self.btn_apply.clicked.connect(lambda: self._run("tune", apply=True))
        self.btn_open_summary.clicked.connect(self._open_summary)
        self.btn_open_folder.clicked.connect(self._open_reports_folder)
        self.btn_refresh.clicked.connect(self._load_outputs)

        self._load_outputs()

    def _ensure_reports_dir(self):
        self.reports_dir.mkdir(parents=True, exist_ok=True)

    def _ensure_config(self):
        self.cfg_path.parent.mkdir(parents=True, exist_ok=True)
        if not self.cfg_path.exists():
            with open(self.cfg_path, "w", encoding="utf-8") as f:
                json.dump(DEFAULT_CFG, f, indent=2)

    def _python(self) -> str:
        return sys.executable or "python"

    def _run(self, cmd: str, apply: bool=False):
        if not self.tool_path.exists():
            QMessageBox.critical(self, "Missing tool", f"Not found: {self.tool_path}\n\nCreate tools/balance_lab.py first.")
            return
        args = [self._python(), str(self.tool_path), "--config", str(self.cfg_path), cmd]
        if apply and cmd == "tune": args.append("--apply")
        try:
            self.setEnabled(False)
            subprocess.run(args, cwd=str(self.project_root), check=False)
        finally:
            self.setEnabled(True)
        self._load_outputs()

    def _open_summary(self):
        md = self.reports_dir / "SUMMARY.md"
        if not md.exists():
            QMessageBox.information(self, "No Summary", "Run Analyze to generate SUMMARY.md")
            return
        try:
            if sys.platform.startswith("win"):
                os.startfile(str(md))
            elif sys.platform == "darwin":
                subprocess.run(["open", str(md)], check=False)
            else:
                subprocess.run(["xdg-open", str(md)], check=False)
        except Exception:
            self.summary.setPlainText(md.read_text(encoding="utf-8"))

    def _open_reports_folder(self):
        path = str(self.reports_dir)
        try:
            if sys.platform.startswith("win"):
                os.startfile(path)
            elif sys.platform == "darwin":
                subprocess.run(["open", path], check=False)
            else:
                subprocess.run(["xdg-open", path], check=False)
        except Exception:
            QMessageBox.information(self, "Open Folder", f"Reports in: {path}")

    def _load_outputs(self):
        md = self.reports_dir / "SUMMARY.md"
        if md.exists():
            try:
                self.summary.setPlainText(md.read_text(encoding="utf-8"))
            except Exception:
                self.summary.setPlainText("(Could not read SUMMARY.md)")
        else:
            self.summary.setPlainText("(No SUMMARY.md yet)")

        csv_path = self.reports_dir / "pairwise.csv"
        if csv_path.exists():
            rows: List[List[str]] = []
            with open(csv_path, "r", encoding="utf-8") as f:
                r = csv.reader(f)
                for i, row in enumerate(r):
                    if i == 0:
                        headers = row
                        self.model.setColumnCount(len(headers))
                        self.model.setHorizontalHeaderLabels(headers)
                        continue
                    rows.append(row)
            self.model.setRowCount(len(rows))
            for ri, row in enumerate(rows):
                for ci, val in enumerate(row):
                    self.model.setItem(ri, ci, QStandardItem(str(val)))
            self.table.resizeColumnsToContents()
        else:
            self.model.clear()
            self.model.setHorizontalHeaderLabels([])
