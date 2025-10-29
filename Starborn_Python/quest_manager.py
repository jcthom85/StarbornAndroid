# quest_manager.py
from __future__ import annotations
from typing import Dict, List, Optional, Callable, Any
from dataclasses import dataclass, field
import time

# ──────────────────────────────────────────────────────────────────────────────
# Data types
# ──────────────────────────────────────────────────────────────────────────────
@dataclass
class QuestTask:
    id: str
    text: str
    done: bool = False
    # Optional: wire tutorials/minor events to individual tasks
    tutorial_id: Optional[str] = None

@dataclass
class QuestStage:
    id: str
    title: str
    description: str = ""
    tasks: List[QuestTask] = field(default_factory=list)

@dataclass
class QuestState:
    id: str
    title: str
    summary: str = ""
    description: str = ""
    flavor: str = ""
    giver: Optional[str] = None
    hub_id: Optional[str] = None
    node_id: Optional[str] = None
    rewards: List[Dict[str, Any]] = field(default_factory=list)

    # runtime state
    status: str = "inactive"  # inactive | active | complete | failed
    stage_index: int = 0
    stages: List[QuestStage] = field(default_factory=list)
    log: List[Dict[str, Any]] = field(default_factory=list)

    def current_stage(self) -> Optional[QuestStage]:
        if 0 <= self.stage_index < len(self.stages):
            return self.stages[self.stage_index]
        return None

    def progress_text(self) -> str:
        if not self.stages:
            return "No stages"
        s = self.current_stage()
        if not s or not s.tasks:
            return f"Stage {self.stage_index+1}/{len(self.stages)}"
        done = sum(1 for t in s.tasks if t.done)
        total = len(s.tasks)
        return f"{done} of {total} complete"


# ──────────────────────────────────────────────────────────────────────────────
# Manager
# ──────────────────────────────────────────────────────────────────────────────
class QuestManager:
    """
    Runtime-first quest system:
    - Define quest blueprints at runtime (from tools, json, or code)
    - Start/advance/complete with events emitted to EventManager (if present)
    - Minimal persistence hooks (to_dict / load_dict)
    - 'Tracked' quest support for HUD pinning
    """
    def __init__(self, game):
        self.game = game
        self._defs: Dict[str, Dict[str, Any]] = {}    # canonical definitions
        self._by_id: Dict[str, QuestState] = {}       # live state
        self.tracked_quest_id: Optional[str] = None

        # Back-compat for older code that iterated a flat list
        self.quests: List[Dict[str, Any]] = []

    # ── public API ────────────────────────────────────────────────────────────
    def register_defs(self, quest_defs: List[Dict[str, Any]]):
        """Register/replace a batch of quest definitions (safe to call repeatedly)."""
        for qd in quest_defs:
            qid = qd.get("id")
            if not qid:
                continue
            self._defs[qid] = qd

    def ensure_quest(self, quest_id: str) -> QuestState:
        """Create live state from def (if needed) and return it."""
        if quest_id in self._by_id:
            return self._by_id[quest_id]
        qd = self._defs.get(quest_id, {"id": quest_id, "title": quest_id})
        q = QuestState(
            id=qd["id"],
            title=qd.get("title", qd["id"]),
            summary=qd.get("summary", ""),
            description=qd.get("description", ""),
            flavor=qd.get("flavor", ""),
            giver=qd.get("giver"),
            hub_id=qd.get("hub_id"),
            node_id=qd.get("node_id"),
            rewards=qd.get("rewards", []),
            status="inactive",
            stage_index=0,
            stages=self._build_stages(qd.get("stages", [])),
            log=[]
        )
        self._by_id[q.id] = q
        self._sync_flat_list()
        return q

    def start(self, quest_id: str):
        q = self.ensure_quest(quest_id)
        if q.status in ("active", "complete", "failed"):
            return
        q.status = "active"
        self._log(q, "Quest started.")
        self._emit("quest_started", {"quest_id": q.id})
        self._sync_flat_list()

    def set_tracked(self, quest_id: Optional[str]):
        self.tracked_quest_id = quest_id
        self._emit("quest_tracked", {"quest_id": quest_id})

    def set_stage(self, quest_id: str, stage_id: str):
        q = self.ensure_quest(quest_id)
        for i, st in enumerate(q.stages):
            if st.id == stage_id:
                q.stage_index = i
                self._log(q, f"Advanced to stage: {st.title}")
                self._emit("quest_stage_changed", {"quest_id": quest_id, "stage_id": stage_id})
                self._sync_flat_list()
                return

    def next_stage(self, quest_id: str):
        q = self.ensure_quest(quest_id)
        if q.stage_index + 1 < len(q.stages):
            q.stage_index += 1
            st = q.current_stage()
            self._log(q, f"Advanced to stage: {st.title if st else q.stage_index+1}")
            self._emit("quest_stage_changed", {"quest_id": quest_id, "stage_id": st.id if st else None})
            self._sync_flat_list()
        else:
            self.complete(quest_id)

    def set_task_done(self, quest_id: str, task_id: str, done: bool=True):
        q = self.ensure_quest(quest_id)
        st = q.current_stage()
        if not st:
            return
        for t in st.tasks:
            if t.id == task_id:
                t.done = bool(done)
                self._emit("quest_task_updated", {"quest_id": quest_id, "task_id": task_id, "done": done})
                self._log(q, f"Updated task: {t.text} → {'Done' if done else 'Not done'}")
                self._sync_flat_list()
                break

    def complete(self, quest_id: str):
        q = self.ensure_quest(quest_id)
        if q.status == "complete":
            return
        q.status = "complete"
        self._log(q, "Quest complete.")
        self._emit("quest_completed", {"quest_id": quest_id, "rewards": q.rewards})
        self._sync_flat_list()

    def fail(self, quest_id: str):
        q = self.ensure_quest(quest_id)
        if q.status == "failed":
            return
        q.status = "failed"
        self._log(q, "Quest failed.")
        self._emit("quest_failed", {"quest_id": quest_id})
        self._sync_flat_list()

    # Query helpers for UI
    def list_active(self) -> List[QuestState]:
        return [q for q in self._by_id.values() if q.status == "active"]

    def list_completed(self) -> List[QuestState]:
        return [q for q in self._by_id.values() if q.status == "complete"]

    def get(self, quest_id: str) -> Optional[QuestState]:
        return self._by_id.get(quest_id)

    # Persistence
    def to_dict(self) -> Dict[str, Any]:
        return {
            "tracked": self.tracked_quest_id,
            "quests": [self._serialize(q) for q in self._by_id.values()]
        }

    def load_dict(self, data: Dict[str, Any]):
        self._by_id.clear()
        self.tracked_quest_id = data.get("tracked")
        for qd in data.get("quests", []):
            q = self._deserialize(qd)
            self._by_id[q.id] = q
        self._sync_flat_list()

    # ── internal helpers ──────────────────────────────────────────────
    def _log(self, q: QuestState, text: str):
        q.log.append({"t": time.time(), "text": text})

    def _emit(self, event: str, payload: Dict[str, Any]):
        # Be forgiving: support either .emit(...) or .post(...)
        em = getattr(self.game, "event_manager", None)
        try:
            if em and hasattr(em, "emit"):
                em.emit(event, payload)
            elif em and hasattr(em, "post"):
                em.post(event, payload)
        except Exception:
            pass

    def _build_stages(self, stage_defs: List[Dict[str, Any]]) -> List[QuestStage]:
        stages = []
        for sd in stage_defs:
            tasks = [QuestTask(**td) if not isinstance(td, QuestTask) else td for td in sd.get("tasks", [])]
            stages.append(QuestStage(
                id=sd.get("id", f"stage{len(stages)+1}"),
                title=sd.get("title", f"Stage {len(stages)+1}"),
                description=sd.get("description", ""),
                tasks=tasks
            ))
        return stages

    def _serialize(self, q: QuestState) -> Dict[str, Any]:
        return {
            "id": q.id,
            "title": q.title,
            "summary": q.summary,
            "description": q.description,
            "flavor": q.flavor,
            "giver": q.giver,
            "hub_id": q.hub_id,
            "node_id": q.node_id,
            "rewards": q.rewards,
            "status": q.status,
            "stage_index": q.stage_index,
            "stages": [
                {
                    "id": s.id, "title": s.title, "description": s.description,
                    "tasks": [{"id": t.id, "text": t.text, "done": t.done, "tutorial_id": t.tutorial_id} for t in s.tasks]
                } for s in q.stages
            ],
            "log": q.log[:],
        }

    def _deserialize(self, data: Dict[str, Any]) -> QuestState:
        stages: List[QuestStage] = []
        for sd in data.get("stages", []):
            stages.append(QuestStage(
                id=sd.get("id"),
                title=sd.get("title", ""),
                description=sd.get("description", ""),
                tasks=[QuestTask(**td) for td in sd.get("tasks", [])]
            ))
        return QuestState(
            id=data["id"], title=data.get("title", data["id"]),
            summary=data.get("summary", ""), description=data.get("description", ""),
            flavor=data.get("flavor", ""), giver=data.get("giver"),
            hub_id=data.get("hub_id"), node_id=data.get("node_id"),
            rewards=data.get("rewards", []), status=data.get("status", "inactive"),
            stage_index=int(data.get("stage_index", 0)), stages=stages, log=data.get("log", [])
        )

    def _sync_flat_list(self):
        """Refresh a simple list-of-dicts for existing UI code that expects self.quests."""
        flat = []
        for q in self._by_id.values():
            flat.append({
                "id": q.id,
                "title": q.title,
                "status": q.status,
                "progress": q.progress_text(),
                "giver": q.giver,
                "summary": q.summary
            })
        # keep stable order: active first (by insertion), then complete
        flat.sort(key=lambda e: (0 if e["status"]=="active" else 1, e["title"].lower()))
        self.quests = flat
