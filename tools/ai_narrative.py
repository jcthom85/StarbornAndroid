#!/usr/bin/env python3
"""
Shared AI helper utilities for narrative tooling.

This module keeps model-facing logic (prompt construction, schema hints,
post-processing) decoupled from the individual PyQt tools so both the
AI Assistant and Narrative Builder can reuse the same entry points.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

try:
    from ai_service import AIService
except Exception:
    # Fallback stub mirrors the one inside ai_assistant.py so local usage
    # remains safe even when ai_service import fails.
    class AIService:  # type: ignore
        def __init__(self, use_openai: bool = False, model: str = "gpt-5-mini"):
            self.use_openai = False
            self.model = model

        def ask_text(self, question: str, pack: Dict[str, Any]) -> str:
            return "(AI disabled) " + question

        def complete_json(
            self,
            goal: str,
            schema_hint: Dict[str, Any],
            instructions: str,
            pack: Dict[str, Any],
        ) -> Tuple[Optional[dict], str]:
            return (
                {
                    "note": "offline-stub",
                    "goal": goal,
                    "instructions": instructions[:160],
                },
                "(AI disabled)",
            )


class NarrativeGenerationError(RuntimeError):
    """Raised when an AI-backed generation request cannot be fulfilled."""


@dataclass
class NarrativeContext:
    """
    Snapshot of the relevant narrative data around the current editing focus.

    All fields are optional; callers should supply whatever they have available
    so prompts can stay grounded in existing content.
    """

    project_root: Path
    quests: List[dict] = field(default_factory=list)
    quest: Optional[dict] = None
    stage: Optional[dict] = None
    focus_stage_index: Optional[int] = None
    stage_flow: List[dict] = field(default_factory=list)
    dialogues: Dict[str, dict] = field(default_factory=dict)
    events: Dict[str, dict] = field(default_factory=dict)
    cinematics: Dict[str, Any] = field(default_factory=dict)
    assistant_briefing: Optional[str] = None
    extras: Dict[str, Any] = field(default_factory=dict)

    def _load_briefing(self) -> str:
        if self.assistant_briefing is not None:
            return self.assistant_briefing
        try:
            path = self.project_root / "data" / "assistant_briefing.md"
            return path.read_text(encoding="utf-8")
        except Exception:
            return ""

    def to_pack(self) -> Dict[str, Any]:
        """
        Package context into a dict suitable for AIService.
        """
        pack = {
            "quests": self.quests,
            "quest_focus": self.quest or {},
            "stage_focus": self.stage or {},
            "stage_index": self.focus_stage_index,
            "stage_flow": self.stage_flow,
            "dialogue_index": self.dialogues,
            "event_index": self.events,
            "cinematics": self.cinematics,
        }
        pack.setdefault("assistant_briefing", self._load_briefing())
        if self.extras:
            pack.update(self.extras)
        return pack


@dataclass
class StageDraft:
    """
    Canonical result of a stage-focused AI request.
    """

    stage_index: int
    stage_title: Optional[str] = None
    stage_description: Optional[str] = None
    stage_notes: Optional[str] = None
    new_beats: List[dict] = field(default_factory=list)
    dialogue_updates: Dict[str, dict] = field(default_factory=dict)
    event_updates: Dict[str, dict] = field(default_factory=dict)
    cinematic_updates: Dict[str, Any] = field(default_factory=dict)
    notes: str = ""
    raw_response: Optional[Any] = None
    raw_text: str = ""


@dataclass
class QuestDraft:
    """
    Future placeholder for end-to-end quest generation.
    """

    quest_payload: Optional[dict] = None
    dialogue_updates: Dict[str, dict] = field(default_factory=dict)
    event_updates: Dict[str, dict] = field(default_factory=dict)
    cinematic_updates: Dict[str, Any] = field(default_factory=dict)
    flows: Dict[str, List[dict]] = field(default_factory=dict)
    notes: str = ""
    raw_response: Optional[Any] = None
    raw_text: str = ""


_STAGE_SCHEMA_HINT: Dict[str, Any] = {
    "type": "object",
    "properties": {
        "stage_title": {"type": "string"},
        "stage_description": {"type": "string"},
        "stage_notes": {"type": "string"},
        "beats": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "type": {"type": "string"},
                    "id": {"type": "string"},
                    "label": {"type": "string"},
                    "text": {"type": "string"},
                    "speaker": {"type": "string"},
                    "target": {"type": "string"},
                    "metadata": {"type": "object"},
                },
            },
        },
        "dialogue": {
            "type": "array",
            "items": {"type": "object"},
        },
        "events": {
            "type": "array",
            "items": {"type": "object"},
        },
        "cinematics": {
            "type": "array",
            "items": {"type": "object"},
        },
        "notes": {"type": "string"},
    },
}


def _coerce_stage_draft(
    payload: Optional[dict],
    raw_text: str,
    context: NarrativeContext,
    fallback_prompt: str,
) -> StageDraft:
    """
    Normalize the service payload into a StageDraft instance.
    """
    index = context.focus_stage_index if context.focus_stage_index is not None else -1
    draft = StageDraft(stage_index=index, raw_response=payload, raw_text=raw_text)

    if not isinstance(payload, dict):
        draft.notes = "AI response did not include JSON content."
        if fallback_prompt.strip():
            draft.new_beats.append({"type": "note", "text": fallback_prompt.strip()})
        return draft

    if payload.get("note") == "offline-stub":
        draft.notes = (
            "AI offline stub: configure OpenAI access to replace this placeholder."
        )
        if fallback_prompt.strip():
            draft.new_beats.append({"type": "note", "text": fallback_prompt.strip()})
        return draft

    draft.stage_title = payload.get("stage_title") or None
    draft.stage_description = payload.get("stage_description") or None
    draft.stage_notes = payload.get("stage_notes") or None
    draft.notes = payload.get("notes") or ""

    beats = payload.get("beats") or []
    if isinstance(beats, list):
        for beat in beats:
            if isinstance(beat, dict):
                beat_copy = dict(beat)
                if "type" not in beat_copy:
                    beat_copy["type"] = "note"
                draft.new_beats.append(beat_copy)

    for category, target in (
        ("dialogue", draft.dialogue_updates),
        ("events", draft.event_updates),
        ("cinematics", draft.cinematic_updates),
    ):
        data = payload.get(category)
        if isinstance(data, list):
            for row in data:
                if isinstance(row, dict):
                    ident = str(row.get("id") or "").strip()
                    if ident:
                        target[ident] = row

    if not draft.new_beats and fallback_prompt.strip():
        draft.new_beats.append({"type": "note", "text": fallback_prompt.strip()})

    return draft


def generate_stage(
    prompt: str,
    context: NarrativeContext,
    service: Optional[AIService] = None,
) -> StageDraft:
    """
    Produce a StageDraft for the current focus using the configured AI service.

    When OpenAI access is disabled, this falls back to an offline stub so the
    caller can still surface a useful note or placeholder beat.
    """
    prompt = (prompt or "").strip()
    if not prompt:
        raise NarrativeGenerationError("Prompt text is required.")
    if context.focus_stage_index is None:
        raise NarrativeGenerationError("NarrativeContext.focus_stage_index is missing.")

    svc = service or AIService(use_openai=True)
    pack = context.to_pack()

    instructions = (
        "Draft a short stage plan for the Starborn narrative tools. "
        "Return beats that reference existing IDs when appropriate. "
        "Use concise labels so they fit inside the UI."
    )

    try:
        payload, raw_text = svc.complete_json(
            "Plan a Starborn quest stage",
            _STAGE_SCHEMA_HINT,
            f"{instructions}\nPROMPT: {prompt}",
            pack,
        )
    except Exception as exc:
        raise NarrativeGenerationError(str(exc)) from exc

    return _coerce_stage_draft(payload, raw_text, context, prompt)


__all__ = [
    "NarrativeContext",
    "StageDraft",
    "QuestDraft",
    "NarrativeGenerationError",
    "generate_stage",
]
