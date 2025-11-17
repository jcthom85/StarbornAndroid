# tools/ai_art_tool.py
from __future__ import annotations
import os, json, shutil
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QFormLayout, QLineEdit, QTextEdit,
    QPushButton, QComboBox, QSpinBox, QCheckBox, QFileDialog, QLabel, QMessageBox
)

from data_core import detect_project_root
from ai_gen import AIGenerator, STARBORN_DEFAULT_PALETTE

# Context + AI service used by ai_assistant.py
try:
    from context_index import ContextIndex
except Exception:
    ContextIndex = None  # Guard if missing

try:
    from ai_service import AIService
except Exception:
    class AIService:  # minimal stub so UI still runs
        def __init__(self, use_openai: bool = False, model: str = "gpt-5-mini"):
            self.use_openai = False
        def complete_json(self, tag: str, schema_hint: dict, task: str, pack: dict):
            return ({"subject": "", "negatives": ""}, "(AI disabled)")

def _load_json(p: Path):
    return json.loads(p.read_text("utf-8")) if p.exists() else None

def _save_json(p: Path, obj):
    p.write_text(json.dumps(obj, indent=2, ensure_ascii=False), encoding="utf-8")

def _index_by_id(arr: list) -> Dict[str, dict]:
    return {str(x.get("id") or x.get("name")).strip(): x for x in arr if isinstance(x, dict)}

def _asset_path(kind: str, ident: str, variant: Optional[int] = None) -> str:
    """
    Canonical path for an asset. If variant is provided, suffix _vN, otherwise plain id.
    """
    base = "assets"
    name = f"{ident}_v{variant}" if variant is not None else ident
    if kind == "hub_bg":   return f"{base}/backgrounds/hubs/{name}.png"
    if kind == "node_ic":  return f"{base}/icons/nodes/{name}.png"
    if kind == "room_bg":  return f"{base}/backgrounds/rooms/{name}.png"
    if kind == "npc_por":  return f"{base}/portraits/npcs/{name}.png"
    if kind == "enemy":    return f"{base}/enemies/{name}.png"
    if kind == "item_ic":  return f"{base}/icons/items/{name}.png"
    return f"{base}/misc/{name}.png"

# --- Starborn Visual Style Guide (baked-in) ---
STYLE_GUIDE_CORE = (
    # 1.0 Overall
    "Starborn visual identity: modern anime aesthetics with a charming chibi execution for characters/enemies. "
    "Simple, clean surfaces; vibrant palette with strong contrast; minimal but clear highlights/shadows. "
    "Consistent lighting: primary key light from UPPER-LEFT for highlights.\n"
    # 2.0 Characters
    "Chibi rules (sprites): exaggerated head and large expressive eyes, 2–3 heads tall, compact bodies. "
    "Thicker outlines for major forms; finer interior lines. One-pixel dark outline for in-game sprites to read at small scale. "
    "Tech details simplified and iconic for readability.\n"
    # 3.0 Environments
    "Environments: clean, semi-realistic aesthetic; layered backgrounds with optional parallax (fog, pipes, drifting dust/sparks, flickering neon). "
    "Maintain established world moods (e.g., Gritty Mining Colony, Bustling Outerworld Hub, Eerie Relay Lab).\n"
    # 4.0 Combat assets spec
    "Combat sprites: static chibi sprites with code-driven motion (slide/idle/shake/hit-flash). "
    "Portraits: realistically proportioned anime busts for dialogue; richer detail and emotion than chibi sprites.\n"
    # 5.0 UI
    "UI: portrait-mode mobile first; clean, thematic; room title in large pixel font; description in semi-transparent panel; "
    "interactive keywords light blue (#8ec9ff); minimal square icons; simple mini-map (ASCII/pixel grid)."
)

# Mode-specific clauses derived from the guide:
STYLE_MODE_HINTS: Dict[str, str] = {
    "Chibi Combat Sprite": (
        "OUTPUT: single full-body chibi sprite on transparent background. "
        "Proportions 2–3 heads tall; oversized head/eyes; thick outer outline; simplified iconic details; "
        "clear upper-left highlight; readable silhouette at small scale. "
        "No background scene; no UI; centered 3/4 front view."
    ),
    "Dialogue Portrait (Bust)": (
        "OUTPUT: bust portrait, realistically proportioned modern-anime style. "
        "Detailed facial expression; clean linework; more detail than sprites. "
        "Consistent upper-left key light. Neutral gradient/abstract backdrop only; no UI."
    ),
    "Environment Background (Stylized Anime)": (
        "OUTPUT: 2D layered background plate in clean, modern-anime style. "
        "Not semi-realistic: surfaces simplified but atmospheric; clear depth with parallax-friendly layers. "
        "Use world mood cues (gritty mining colony, eerie lab, neon hub, etc.). "
        "Reserve negative space for UI overlays. No characters; no text."
    ),
    "UI Element (Icon/Panel)": (
        "OUTPUT: crisp, minimal UI asset for portrait-mode game; geometric iconography; "
        "high contrast with accent color #8ec9ff. Transparent background. No photo textures."
    ),
}

# Optional, per-character visual accents from the guide (used to auto-nudge prompts if target matches)
CHAR_STYLE_HINTS: Dict[str, str] = {
    "nova": (
        "Nova: chibi rogue scavenger; bright purple eyes; sly smirk; freckles; soot-black spiky shoulder-length hair; "
        "charcoal-grey vest + burnt-orange shirt + tool belt; torn shimmering half-cape; slightly oversized boots with neon-green laces."
    ),
    "zeke": (
        "Zeke: chibi support; broad friendly grin; warm hazel eyes; blond undercut; cobalt-blue puffy bomber jacket with simplified patches; "
        "glowing smartwatch HUD on wrist; accents in sunshine yellow."
    ),
    "orion": (
        "Orion: chibi alien scientist; pale crystalline blue glowing eyes; snow-white shoulder-length hair; "
        "midnight-navy long lab coat with glowing circuit seams; bioluminescent freckles; glowing crystal pendant; optional small hovering drone."
    ),
    "gh0st": (
        "Gh0st: chibi ex-corporate assassin; icy grey eyes with digital hex pattern; short tight coils/dreads; "
        "matte-black hooded tactical cloak enveloping body; muted midnight-red glow lines on bodysuit; ominous single HUD eye when hood up."
    ),
}

NEGATIVE_PREFIX = "Avoid: "


class AIArtTool(QWidget):
    def __init__(self, project_root: Optional[str]=None):
        super().__init__()
        self.setWindowTitle("AI Art Lab")
        self.root = detect_project_root(project_root)
        self.gen = AIGenerator(str(self.root))  # lazy client; safe without key

        # Shared context + assistant service (mirrors ai_assistant.py)
        self.cx = ContextIndex(self.root) if ContextIndex else None
        self.ai = AIService(use_openai=True)

        # Domain data
        self.worlds = _load_json(self.root / "worlds.json") or []
        self.hubs   = _load_json(self.root / "hubs.json") or []
        self.nodes  = _load_json(self.root / "nodes.json") or []
        self.rooms  = _load_json(self.root / "rooms.json") or []
        self.npcs   = _load_json(self.root / "npcs.json") or []
        self.enemies= _load_json(self.root / "enemies.json") or []
        self.items  = _load_json(self.root / "items.json") or []

        self.hubs_by_id   = _index_by_id(self.hubs)
        self.nodes_by_id  = _index_by_id(self.nodes)
        self.rooms_by_id  = _index_by_id(self.rooms)
        self.npcs_by_id   = _index_by_id(self.npcs)
        self.enemies_by_id= _index_by_id(self.enemies)
        self.items_by_id  = _index_by_id(self.items)

        # State for Explain + Move/Assign quick-pick
        self._last_explain_payload: Optional[dict] = None
        self._last_saved_paths: List[str] = []   # relative paths under project root

        self._build_ui()
        self._update_ready_banner()

    # ---------- UI ----------
    def _build_ui(self):
        v = QVBoxLayout(self)

        # Ready/API row
        ready_row = QHBoxLayout()
        self.ready_lbl = QLabel("")
        self.api_key_edit = QLineEdit()
        self.api_key_edit.setEchoMode(QLineEdit.Password)
        self.api_key_edit.setPlaceholderText("API key (optional, session-only; or set OPENAI_API_KEY env var)")
        apply_btn = QPushButton("Apply")
        apply_btn.clicked.connect(self._apply_api_key)
        ready_row.addWidget(self.ready_lbl, 3)
        ready_row.addWidget(self.api_key_edit, 3)
        ready_row.addWidget(apply_btn, 0)
        v.addLayout(ready_row)

        # Target row
        target_row = QHBoxLayout()
        self.kind = QComboBox(); self.kind.addItems([
            "Room Background", "Hub Background", "Node Icon",
            "NPC Portrait", "Enemy Art", "Item Icon"
        ])
        self.ident = QComboBox(); self._reload_ident_choices()
        self.kind.currentIndexChanged.connect(self._reload_ident_choices)

        target_row.addWidget(QLabel("Target:"))
        target_row.addWidget(self.kind, 2)
        target_row.addWidget(QLabel("ID:"))
        target_row.addWidget(self.ident, 3)
        v.addLayout(target_row)

        # Prompt form
        form = QFormLayout()
        # Mode derived from the style guide (no arbitrary presets)
        self.mode = QComboBox()
        self.mode.addItems([
            "Chibi Combat Sprite",
            "Dialogue Portrait (Bust)",
            "Environment Background (Semi-Realistic)",
            "UI Element (Icon/Panel)",
        ])

        self.subject = QLineEdit()
        self.neg = QLineEdit()

        # Request size & final crop
        self.size = QComboBox(); self.size.addItems(["1024x1536", "1024x1024", "1536x1024", "2048x2048"])
        self.out_w = QSpinBox(); self.out_w.setRange(256, 8192); self.out_w.setValue(1080)
        self.out_h = QSpinBox(); self.out_h.setRange(256, 8192); self.out_h.setValue(2520)
        self.variants = QSpinBox(); self.variants.setRange(1, 4); self.variants.setValue(1)

        # Starborn palette quantization (optional but often nice for cohesion)
        self.palette_chk = QCheckBox("Enforce Starborn palette (blues/purples)")
        self.palette_chk.setChecked(True)

        # Context-aware prompting + style guide toggle
        self.use_ctx = QCheckBox("Use project briefing & PACK (auto-enrich subject)")
        self.use_ctx.setChecked(True)
        self.use_style_guide = QCheckBox("Apply Starborn Visual Style Guide")
        self.use_style_guide.setChecked(True)

        # Optional extra stylistic notes (appended after guide + mode)
        self.extra_style = QLineEdit()
        self.extra_style.setPlaceholderText("Optional: extra stylistic notes (e.g., 'dust motes, faint green hologram UI')")

        form.addRow("Asset Mode:", self.mode)
        form.addRow("Subject / Extra details:", self.subject)
        form.addRow("Negative terms:", self.neg)
        form.addRow("Model size (request):", self.size)
        form.addRow("Output W×H (final crop):", self._hstack(self.out_w, QLabel("×"), self.out_h))
        form.addRow("Variants:", self.variants)
        form.addRow("", self.palette_chk)
        form.addRow("", self.use_ctx)
        form.addRow("", self.use_style_guide)
        form.addRow("Extra stylistic notes:", self.extra_style)
        v.addLayout(form)

        # Buttons
        btns = QHBoxLayout()
        b_gen = QPushButton("Generate")
        b_attach = QPushButton("Generate & Attach")
        b_explain = QPushButton("Explain subject")
        b_move_assign = QPushButton("Move/Assign…")  # quick placement + JSON assignment
        b_open = QPushButton("Open Output Folder…")
        btns.addWidget(b_gen)
        btns.addWidget(b_attach)
        btns.addWidget(b_explain)
        btns.addWidget(b_move_assign)
        btns.addStretch(1)
        btns.addWidget(b_open)
        v.addLayout(btns)

        b_gen.clicked.connect(self.on_generate)
        b_attach.clicked.connect(self.on_generate_and_attach)
        b_open.clicked.connect(self.on_open_folder)
        b_explain.clicked.connect(self.on_explain_subject)
        b_move_assign.clicked.connect(self.on_move_assign)

        v.addStretch(1)

    def _hstack(self, *widgets):
        h = QHBoxLayout(); w = QWidget(); w.setLayout(h)
        for wd in widgets: h.addWidget(wd)
        return w

    def _apply_api_key(self):
        self.gen.set_api_key(self.api_key_edit.text())
        self._update_ready_banner()

    def _update_ready_banner(self):
        rs = self.gen.ready_state()
        msgs = []
        msgs.append("OpenAI: OK" if rs["openai_installed"] else "OpenAI: missing (pip install openai)")
        msgs.append("API key: set" if rs["has_api_key"] else "API key: not set")
        msgs.append(f"img={rs['image_model']} txt={rs['text_model']}")
        self.ready_lbl.setText("  |  ".join(msgs))

    def _reload_ident_choices(self):
        self.ident.clear()
        kind = self.kind.currentText()
        ids: List[str] = []
        if kind == "Hub Background":    ids = list(self.hubs_by_id.keys())
        elif kind == "Node Icon":       ids = list(self.nodes_by_id.keys())
        elif kind == "Room Background": ids = list(self.rooms_by_id.keys())
        elif kind == "NPC Portrait":    ids = list(self.npcs_by_id.keys())
        elif kind == "Enemy Art":       ids = list(self.enemies_by_id.keys())
        elif kind == "Item Icon":       ids = list(self.items_by_id.keys())
        self.ident.addItems(sorted(ids))

    # ---------- Context-aware subject + Explain ----------
    def _pack_debug_counts(self, pack: Dict[str, Any]) -> Dict[str, int]:
        keys = ["worlds", "hubs", "nodes", "rooms", "npcs", "enemies", "items", "quests", "skill_trees", "cinematics"]
        out = {}
        for k in keys:
            try:
                v = pack.get(k)
                out[k] = len(v) if isinstance(v, list) else (len(v) if isinstance(v, dict) else 0)
            except Exception:
                out[k] = 0
        return out

    def _contextual_subject(self, kind_key: str, ident: str, user_subject: str, style_hint: str) -> Tuple[str, str]:
        """
        Produce a short, image-model-ready SUBJECT and NEGATIVES grounded in
        BRIEFING_SECTIONS + PACK (same slice/min-pack logic as ai_assistant).
        Also caches a rich 'explain payload' for the Explain dialog, including PACK stats.
        """
        if not self.cx or not hasattr(self.cx, "pack_global"):
            self._last_explain_payload = {
                "reasoning": "ContextIndex unavailable. Using only user subject.",
                "briefing_titles": [],
                "pack_debug": {},
                "subject": user_subject.strip(),
                "negatives": ""
            }
            return user_subject.strip(), ""

        pack = self.cx.pack_global()
        target = {"kind": kind_key, "id": ident}
        try:
            pack = dict(pack)
            pack["target"] = target
        except Exception:
            pass
        pack_debug = self._pack_debug_counts(pack)

        # Primary: subject + negatives
        schema_subject = {"subject": "string", "negatives": "string"}
        task_subject = (
            "Create a single-sentence SUBJECT for an image of the Starborn asset, "
            "using ONLY BRIEFING_SECTIONS and PACK facts. If details are missing, keep it generic. "
            "Also provide a comma-separated NEGATIVES list to avoid unwanted elements. "
            "Return plain text values (no quotes).\n\n"
            f"TARGET_KIND: {kind_key}\nTARGET_ID: {ident}\n"
            f"USER_SUBJECT_HINT: {user_subject or '(none)'}\n"
            f"STYLE_HINT: {style_hint}\n"
        )

        subj, negs = user_subject.strip(), ""
        try:
            obj, _raw = self.ai.complete_json("image_prompt", schema_subject, task_subject, pack)
            subj = (obj or {}).get("subject") or subj
            negs = (obj or {}).get("negatives") or ""
        except Exception:
            pass

        # Secondary: explain payload
        schema_explain = {"reasoning": "string", "briefing_titles": ["string"]}
        task_explain = (
            "From the BRIEFING_SECTIONS included in the prompt you saw, list the titles of the sections "
            "that were most relevant to producing the SUBJECT above. Also write a short rationale in 2–4 "
            "sentences explaining how those sections informed the image description.\n"
            "Return concise values."
        )
        try:
            exp_obj, _raw2 = self.ai.complete_json("image_prompt_explain", schema_explain, task_explain, pack)
            self._last_explain_payload = {
                "reasoning": (exp_obj or {}).get("reasoning", ""),
                "briefing_titles": (exp_obj or {}).get("briefing_titles", []),
                "subject": subj,
                "negatives": negs,
                "target": target,
                "pack_debug": pack_debug
            }
        except Exception:
            self._last_explain_payload = {
                "reasoning": "Unable to retrieve explain details (AI disabled or error).",
                "briefing_titles": [],
                "subject": subj,
                "negatives": negs,
                "target": target,
                "pack_debug": pack_debug
            }

        return subj.strip(), negs.strip()

    def on_explain_subject(self):
        """
        Show which briefing sections were used + short rationale + PACK debug (counts/target).
        """
        payload = self._last_explain_payload or {}
        titles = payload.get("briefing_titles") or []
        reasoning = payload.get("reasoning") or "No details available."
        subj = payload.get("subject") or self.subject.text().strip()
        negs = payload.get("negatives") or self.neg.text().strip()
        target = payload.get("target") or {}
        pack_debug = payload.get("pack_debug") or {}

        msg = []
        if subj:
            msg.append(f"<b>Subject</b>: {self._escape(subj)}")
        if negs:
            msg.append(f"<b>Negatives</b>: {self._escape(negs)}")

        # Target info
        if target:
            msg.append("<b>Target</b>: "
                       f"{self._escape(str(target.get('kind')))} / {self._escape(str(target.get('id')))}")

        # Briefing titles
        if titles:
            msg.append("<b>Briefing sections referenced</b>:")
            for t in titles:
                msg.append(f" • {self._escape(str(t))}")

        # PACK debug counts
        if pack_debug:
            msg.append("<b>PACK</b>: " + ", ".join(
                [f"{self._escape(k)}={self._escape(str(v))}" for k, v in pack_debug.items()]
            ))

        msg.append(f"<b>Why</b>: {self._escape(reasoning)}")

        QMessageBox.information(self, "Explain subject", "<br/>".join(msg))

    @staticmethod
    def _escape(s: str) -> str:
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    # ---------- Prompt assembly / generation ----------
    def _compose_prompt(self) -> Tuple[str, Tuple[int,int], str, int, bool]:
        """
        Compose the final image prompt by layering:
        (1) Contextual SUBJECT (optional), (2) Style Guide core, (3) Mode-specific hint,
        (4) Optional character nudge, (5) User extras, (6) Negatives.
        """
        subject = self.subject.text().strip()
        neg = self.neg.text().strip()
        size_req = self.size.currentText()

        # 1) Contextual subject/negatives via PACK + briefing
        if self.use_ctx.isChecked():
            ident = self.ident.currentText().strip()
            kind_key = self._kind_key()
            ai_subject, ai_negs = self._contextual_subject(kind_key, ident, subject, "")
            subject = subject or ai_subject
            if ai_negs:
                neg = f"{ai_negs}, {neg}" if neg else ai_negs

        # 2) Style guide core + 3) Mode-specific hint
        style_chunks: List[str] = []
        if self.use_style_guide.isChecked():
            style_chunks.append(STYLE_GUIDE_CORE)
            style_chunks.append(STYLE_MODE_HINTS.get(self.mode.currentText(), ""))

        # 4) Character-specific nudges if target looks like a main cast id
        ident_lower = self.ident.currentText().strip().lower()
        for key, hint in CHAR_STYLE_HINTS.items():
            if key in ident_lower:
                style_chunks.append(hint)
                break

        # 5) Extra user style notes
        if self.extra_style.text().strip():
            style_chunks.append(self.extra_style.text().strip())

        # Final prompt text
        style_hint = "\n".join([s for s in style_chunks if s]).strip()
        if style_hint:
            full_prompt = f"{subject}\n\nStyle Guide:\n{style_hint}"
        else:
            full_prompt = subject

        # 6) Negatives appended
        if neg:
            full_prompt += f"\n{NEGATIVE_PREFIX}{neg}"

        # Output & variant controls
        out_w = int(self.out_w.value())
        out_h = int(self.out_h.value())
        n = int(self.variants.value())
        palette = self.palette_chk.isChecked()
        return (full_prompt, (out_w, out_h), size_req, n, palette)

    def on_generate(self):
        self._run_generate(attach=False)

    def on_generate_and_attach(self):
        self._run_generate(attach=True)

    def _run_generate(self, attach: bool):
        ident = self.ident.currentText().strip()
        if not ident:
            QMessageBox.warning(self, "AI Art", "Choose a target ID first.")
            return
        prompt, out_wh, size_req, n, palette = self._compose_prompt()
        try:
            imgs = self.gen.smart_image(
                prompt_core=prompt or "Stylized concept image",
                style_hint="",  # we now embed style in prompt itself
                size_request=size_req,
                target_px=out_wh,
                palette=STARBORN_DEFAULT_PALETTE if palette else None,
                n=n
            )
        except Exception as e:
            QMessageBox.critical(self, "OpenAI Error", str(e))
            return

        saved_paths: List[str] = []
        for idx, im in enumerate(imgs, start=1):
            rel = _asset_path(self._kind_key(), ident, variant=idx)
            saved_rel = self.gen.save_png(im, rel)
            saved_paths.append(saved_rel)

        # cache for Move/Assign quick-pick
        self._last_saved_paths = saved_paths[:]

        msg = "Saved:\n" + "\n".join(saved_paths)
        if attach:
            self._attach_to_json(ident, saved_paths[0])
            msg += f"\n\nAttached first variant to {self.kind.currentText()}."
        QMessageBox.information(self, "AI Art", msg)

    # ---------- Move/Assign ----------
    def on_move_assign(self):
        """
        Move/copy an existing image to its canonical path and assign it to the chosen object.
        If we have a recently generated image, offer to use it; otherwise ask user to pick a file.
        """
        ident = self.ident.currentText().strip()
        if not ident:
            QMessageBox.warning(self, "Move/Assign", "Choose a target ID first.")
            return

        # Offer last generated first
        src_abspath: Optional[Path] = None
        if self._last_saved_paths:
            first = self._last_saved_paths[0]
            full = (self.root / first)
            if full.exists():
                use_last = QMessageBox.question(
                    self,
                    "Move/Assign",
                    f"Use last generated image?\n\n{first}",
                    QMessageBox.Yes | QMessageBox.No,
                    QMessageBox.Yes
                )
                if use_last == QMessageBox.Yes:
                    src_abspath = full

        # If not chosen, ask user
        if src_abspath is None:
            fpath, _ = QFileDialog.getOpenFileName(
                self, "Choose image to move/assign", str(self.root),
                "Images (*.png *.jpg *.jpeg *.webp *.bmp)"
            )
            if not fpath:
                return
            src_abspath = Path(fpath)

        if not src_abspath.exists():
            QMessageBox.warning(self, "Move/Assign", "File does not exist anymore.")
            return

        kind_key = self._kind_key()

        # Destination: canonical non-variant filename (id.png)
        dest_rel = _asset_path(kind_key, ident, variant=None)
        dest_abs = (self.root / dest_rel)
        dest_abs.parent.mkdir(parents=True, exist_ok=True)

        # If src == dest, we still proceed to 'assign' step, but skip copying
        try:
            if src_abspath.resolve() != dest_abs.resolve():
                shutil.copyfile(str(src_abspath), str(dest_abs))
        except Exception as e:
            QMessageBox.critical(self, "Move/Assign", f"Failed to copy file:\n{e}")
            return

        # Assign in JSON where supported
        if self._assign_by_kind(kind_key, ident, dest_rel):
            QMessageBox.information(
                self, "Move/Assign",
                f"Placed at:\n{dest_rel}\n\nAssigned to {self.kind.currentText()}."
            )
        else:
            QMessageBox.information(
                self, "Move/Assign",
                f"Placed at:\n{dest_rel}\n\nNo matching JSON field to assign for this kind."
            )

    def _get_json_file_and_key(self, kind_key: str) -> Optional[Tuple[str, str, str]]:
        """Returns (filename, object_key, image_field_key)"""
        mapping = {
            "hub_bg":  ("hubs.json", "id", "background_image"),
            "node_ic": ("nodes.json", "id", "icon_image"),
            "room_bg": ("rooms.json", "id", "background_image"),
            "npc_por": ("npcs.json", "id", "portrait_image"),
            "enemy":   ("enemies.json", "id", "sprite_image"),
            "item_ic": ("items.json", "id", "icon_image"),
        }
        return mapping.get(kind_key)

    def _assign_by_kind(self, kind_key: str, ident: str, rel_path: str) -> bool:
        """
        Attach rel_path in the correct JSON field for supported kinds.
        Returns True if something was updated.
        """
        file_info = self._get_json_file_and_key(kind_key)
        if not file_info:
            return False

        filename, id_key, image_field = file_info
        json_path = self.root / filename
        data_list = _load_json(json_path) or []

        for item in data_list:
            if isinstance(item, dict) and (str(item.get(id_key)) == ident or str(item.get("name", "")).strip().lower() == ident.lower()):
                # For NPCs/Enemies/Items, be flexible with the image field name
                if kind_key in ("npc_por", "enemy", "item_ic"):
                    if image_field in item: item[image_field] = rel_path
                    elif "image" in item: item["image"] = rel_path # fallback
                    else: item[image_field] = rel_path # add preferred
                else:
                    item[image_field] = rel_path
                
                _save_json(json_path, data_list)
                return True
        return False

    # ---------- Attach helper (used by Generate & Attach) ----------
    def _kind_key(self) -> str:
        return {
            "Hub Background": "hub_bg",
            "Node Icon": "node_ic",
            "Room Background": "room_bg",
            "NPC Portrait": "npc_por",
            "Enemy Art": "enemy",
            "Item Icon": "item_ic",
        }[self.kind.currentText()]

    def _attach_to_json(self, ident: str, rel_path: str):
        kind = self._kind_key()
        if self._assign_by_kind(kind, ident, rel_path):
            return
        
        QMessageBox.warning(self, "Attach", f"ID '{ident}' not found in {self.kind.currentText()}.")

    def on_open_folder(self):
        base = Path(self.root) / "assets"
        base.mkdir(parents=True, exist_ok=True)
        QFileDialog.getExistingDirectory(self, "Open output folder", str(base))
