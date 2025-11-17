#!/usr/bin/env python3
# tools/ai_service.py
from __future__ import annotations
import os, json, random, re
from typing import Any, Dict, Optional, Tuple, List

try:
    import openai  # type: ignore
    _HAS_OPENAI = True
except Exception:
    _HAS_OPENAI = False

# Default model can be overridden by env
DEFAULT_MODEL = os.environ.get("STARBRN_AI_MODEL", "gpt-5-mini")
OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY") or os.environ.get("OPENAI_API_KEY_STARBORN")


def _model_restricts_sampling(model: str) -> bool:
    """
    Returns True for models that disallow custom sampling params like `temperature`
    (e.g., o-series reasoning models and GPT-5 family).
    """
    if not model:
        return False
    m = model.lower()
    restricted_prefixes = ("o3", "o4", "gpt-5")
    return any(m.startswith(p) for p in restricted_prefixes)


# ---------- Chapter helpers ----------

_WORD_NUMS = {
    "zero": 0, "one": 1, "two": 2, "three": 3, "four": 4, "five": 5, "six": 6,
    "seven": 7, "eight": 8, "nine": 9, "ten": 10, "eleven": 11, "twelve": 12,
    "thirteen": 13, "fourteen": 14, "fifteen": 15, "sixteen": 16,
    "seventeen": 17, "eighteen": 18, "nineteen": 19, "twenty": 20,
}

_ROMAN_MAP = {"i":1,"ii":2,"iii":3,"iv":4,"v":5,"vi":6,"vii":7,"viii":8,"ix":9,"x":10,
              "xi":11,"xii":12,"xiii":13,"xiv":14,"xv":15,"xvi":16,"xvii":17,"xviii":18,"xix":19,"xx":20}

def _parse_chapter_num(text: str) -> Optional[int]:
    """Find a chapter number in user text like 'chapter 4', 'ch. iv', 'chapter six', etc."""
    q = text.lower()
    m = re.search(r'\bch(?:apter|\.)?\s*[:#-]?\s*(\d{1,3}|[ivxlcdm]+|[a-z]+)\b', q, re.IGNORECASE)
    if not m:
        return None
    tok = m.group(1).lower()
    if tok.isdigit():
        return int(tok)
    if tok in _ROMAN_MAP:
        return _ROMAN_MAP[tok]
    return _WORD_NUMS.get(tok)

def _title_has_chapter(title: str, target: int) -> bool:
    """True if title clearly refers to the target chapter number."""
    t = title.lower()
    # accept variants like "Chapter 04", "CHAPTER FOUR", "Act II – Chapter 4", "Ch.4"
    return re.search(rf'\bch(?:apter|\.)?\s*[:#-]?\s*0*{target}\b', t, re.IGNORECASE) is not None


class AIService:
    def __init__(self, use_openai: bool = False, model: str = DEFAULT_MODEL, temperature: float = 0.2):
        self.use_openai = bool(use_openai and OPENAI_API_KEY)
        self.model = model or DEFAULT_MODEL
        self.temperature = float(temperature)
        if self.use_openai and _HAS_OPENAI:
            openai.api_key = OPENAI_API_KEY

    # -------- Briefing section indexing & selection --------

    def _index_briefing_sections(self, briefing: str) -> List[Dict[str, Any]]:
        """
        Split briefing markdown into sections by ANY heading level (#{1,6}) OR lines that look like CHAPTER headings.
        Returns a list of dicts: {title, start, end, body, lower_title}
        """
        if not briefing:
            return []

        # Capture headings of any level: '#', '##', ..., '######'
        head_iter = list(re.finditer(r'(?m)^(#{1,6})\s+(.+?)\s*$', briefing))

        # Also capture all-caps chapter banner lines without '#'
        chap_iter = list(re.finditer(r'(?m)^\s*chapter\s+\w+.*$', briefing, re.IGNORECASE))

        marks = sorted(list({m.start() for m in head_iter} | {m.start() for m in chap_iter}))
        if not marks:
            # No headings; treat whole doc as one section
            return [{"title": "Document", "start": 0, "end": len(briefing),
                     "body": briefing, "lower_title": "document"}]

        sections: List[Dict[str, Any]] = []
        marks.append(len(briefing))
        for i in range(len(marks)-1):
            start, end = marks[i], marks[i+1]
            # extract the heading line as title
            line = briefing[start: briefing.find("\n", start) if briefing.find("\n", start) != -1 else end]
            title = re.sub(r'^#{1,6}\s+', '', line).strip()
            if not title:
                title = "Section"
            body = briefing[start:end].strip()
            sections.append({"title": title, "start": start, "end": end,
                             "body": body, "lower_title": title.lower()})
        return sections

    def _score_section(self, title: str, body: str, query: str) -> float:
        """
        Lightweight scoring: keyword hits in title*3 + body; chapter-number match gets a big boost.
        """
        q = query.lower(); t = title.lower(); b = body.lower()
        score = 0.0

        # Basic tokens
        tokens = re.findall(r"[a-z0-9]+", q)
        for tok in set(tokens):
            if tok in t: score += 3.0
            if tok in b: score += 1.0

        # Chapter boost
        chap_num = _parse_chapter_num(q)
        if chap_num is not None and _title_has_chapter(title, chap_num):
            score += 25.0  # strong boost if this is "the" chapter

        # System keywords
        if any(k in q for k in ["resonance","combat","status","skill","tinkering","cooking","fishing","accessibility"]):
            if any(k in t for k in ["system","combat","resonance","skill","tinkering","cooking","fishing","accessibility"]):
                score += 6.0

        # Character name boost
        if any(n in q for n in ["nova","zeke","orion","gh0st","ghost"]):
            if "character" in t or "at a glance" in t or any(n in t for n in ["nova","zeke","orion","gh0st","ghost"]):
                score += 5.0

        # Finale cues
        if any(k in q for k in ["relay nexus","final showdown","new dawn","finale","atraxis"]):
            if any(k in t for k in ["relay nexus","final showdown","new dawn"]):
                score += 8.0

        return score

    def _select_briefing_slices(
        self,
        briefing: str,
        query: str,
        max_sections: int = 3,
        per_section_cap: int = 12000,
        include_top_summary: bool = True,
        top_summary_cap: int = 6000,
    ) -> List[Tuple[str, str]]:
        """
        Choose Top Summary (if present) + best N sections for the query.
        If the query asks for a specific CHAPTER, force-include that chapter section (and neighbors).
        Returns list of (title, text) tuples, truncated per caps.
        """
        if not briefing:
            return []

        sections = self._index_briefing_sections(briefing)

        # Try to prepend Top Summary if present
        chosen: List[Tuple[str, str]] = []
        if include_top_summary:
            for sec in sections:
                if sec["lower_title"].startswith("top summary"):
                    chosen.append((sec["title"], sec["body"][:top_summary_cap]))
                    break

        # If the user asked for a specific chapter, force include it + neighbor chapters
        chap_num = _parse_chapter_num(query or "")
        is_chapter_query = chap_num is not None
        if is_chapter_query:
            # Prefer exact title matches first
            idx_matches = [i for i, s in enumerate(sections) if _title_has_chapter(s["title"], chap_num)]  # type: ignore
            if idx_matches:
                i = idx_matches[0]
                # Include prev/next if they look like chapter titles too
                pick_idx = {i}
                if i-1 >= 0: pick_idx.add(i-1)
                if i+1 < len(sections): pick_idx.add(i+1)
                for j in sorted(pick_idx):
                    sec = sections[j]
                    t, txt = sec["title"], sec["body"][:per_section_cap]
                    if not any(t == tt for (tt, _) in chosen):
                        chosen.append((t, txt))

        # Score all sections and fill remaining quota
        scored = []
        for sec in sections:
            score = self._score_section(sec["title"], sec["body"], query)
            scored.append((score, sec))
        scored.sort(key=lambda x: x[0], reverse=True)

        target_cap = (6 if is_chapter_query else max_sections)
        need_total = (1 + target_cap if include_top_summary else target_cap)

        for score, sec in scored:
            if len(chosen) >= need_total:
                break
            if any(sec["title"] == t for (t, _) in chosen):
                continue
            chosen.append((sec["title"], sec["body"][:per_section_cap]))

        # Fallback if nothing matched
        if not chosen and sections:
            first = sections[0]
            chosen = [(first["title"], first["body"][:per_section_cap])]

        # Cap total size around 60k to be safer with long contexts
        total_chars = 0
        final: List[Tuple[str, str]] = []
        for title, text in chosen:
            if any(title == t for (t, _) in final):
                continue
            if total_chars + len(text) > 60000:
                remain = max(0, 60000 - total_chars)
                if remain > 0:
                    final.append((title, text[:remain]))
                    total_chars += remain
                break
            final.append((title, text))
            total_chars += len(text)

        # Quick debug line (shows up in your console)
        try:
            print("[AI DEBUG] Briefing slices:", [t for (t, _) in final])
        except Exception:
            pass

        return final

    # ---------------------- public ----------------------
    def ask_text(self, question: str, context_pack: Dict[str, Any]) -> str:
        """Free-form text answer (non-JSON)."""
        if not self.use_openai:
            return self._offline_ask(question, context_pack)
        prompt = self._format_text_prompt(question, context_pack)
        try:
            return self._chat(prompt, expect_json=False)
        except Exception as ex:
            raise RuntimeError(f"OpenAI call failed in ask_text: {ex}") from ex

    def complete_json(
        self,
        system_goal: str,
        json_schema_hint: Dict[str, Any],
        task_instructions: str,
        context_pack: Dict[str, Any],
    ) -> Tuple[Optional[dict], str]:
        """JSON-constrained completion: returns (obj, raw_text)."""
        if not self.use_openai:
            out = self._offline_json(system_goal, json_schema_hint, task_instructions, context_pack)
            return out, json.dumps(out, ensure_ascii=False, indent=2)

        raw = ""
        prompt = self._format_json_prompt(system_goal, json_schema_hint, task_instructions, context_pack)
        try:
            raw = self._chat(prompt, expect_json=True)
            obj = self._parse_json_safe(raw) or self._parse_json_safe(self._json_repairish(raw))
            return obj, raw
        except Exception as ex:
            raise RuntimeError(f"OpenAI call failed in complete_json: {ex}\nRAW: {raw}") from ex

    # -------------------- internals ---------------------
    def _chat(self, prompt: str, expect_json: bool) -> str:
        """
        Single-turn call using Chat Completions.
        For models that disallow sampling overrides, omit `temperature`.
        """
        if _HAS_OPENAI:
            kwargs: Dict[str, Any] = dict(
                model=self.model,
                messages=[
                    {"role": "system", "content": "You are a helpful game content generator."},
                    {"role": "user", "content": prompt},
                ],
            )
            if not _model_restricts_sampling(self.model):
                kwargs["temperature"] = self.temperature

            if expect_json:
                try:
                    kwargs["response_format"] = {"type": "json_object"}
                except Exception:
                    pass

            rsp = openai.chat.completions.create(**kwargs)  # type: ignore[attr-defined]
            return rsp.choices[0].message.content or ""

        return "(openai client not installed) No-API mode."

    def _format_text_prompt(self, question: str, pack: Dict[str, Any]) -> str:
        """
        Text Q&A prompt: send Top Summary + top/forced relevant briefing sections,
        plus a minimized PACK for lookups.
        """
        briefing = (pack.get("assistant_briefing") or "").strip()
        slices = self._select_briefing_slices(briefing, question, max_sections=3)

        # Remove briefing from PACK to avoid duplication
        slim_pack_source = dict(pack)
        slim_pack_source.pop("assistant_briefing", None)
        slim_pack = json.dumps(self._min_pack(slim_pack_source), ensure_ascii=False)

        parts = []
        for title, text in slices:
            parts.append(f"### {title}\n{text}")
        sections = "\n\n".join(parts)

        return (
            "Answer using ONLY the facts in BRIEFING_SECTIONS and PACK. If unknown, say so.\n\n"
            f"BRIEFING_SECTIONS:\n{sections}\n\n"
            f"PACK (truncated):\n{slim_pack[:20000]}\n\n"
            f"QUESTION:\n{question}\n"
        )

    def _format_json_prompt(self, goal: str, schema: Dict[str, Any], task: str, pack: Dict[str, Any]) -> str:
        briefing = (pack.get("assistant_briefing") or "").strip()
        slices = self._select_briefing_slices(briefing, task, max_sections=2)

        slim_pack_source = dict(pack)
        slim_pack_source.pop("assistant_briefing", None)
        slim_pack = json.dumps(self._min_pack(slim_pack_source), ensure_ascii=False)

        parts = []
        for title, text in slices:
            parts.append(f"### {title}\n{text}")
        sections = "\n\n".join(parts)

        return (
            "Return ONLY strict JSON.\n"
            f"GOAL: {goal}\n"
            f"SCHEMA_HINT:\n{json.dumps(schema, ensure_ascii=False)}\n\n"
            f"BRIEFING_SECTIONS:\n{sections}\n\n"
            f"PACK (truncated):\n{slim_pack[:20000]}\n\n"
            f"TASK:\n{task}\n"
        )

    def _min_pack(self, pack: Dict[str, Any]) -> Dict[str, Any]:
        """Compact but useful PACK. Excludes 'assistant_briefing' (handled upstream)."""
        def trim(v):
            if isinstance(v, str) and len(v) > 1000:
                return v[:1000] + "…"
            return v

        out: Dict[str, Any] = {}
        for k, v in pack.items():
            if k == "assistant_briefing":
                continue
            if isinstance(v, list):
                out[k] = [trim(x) for x in v[:120]]
            elif isinstance(v, dict):
                items = list(v.items())[:120]
                out[k] = {kk: trim(vv) for kk, vv in items}
            else:
                out[k] = trim(v)
        out.setdefault("project", {"title": "Starborn"})
        return out

    def _parse_json_safe(self, s: str) -> Optional[dict]:
        try:
            return json.loads(s)
        except Exception:
            try:
                i, j = s.find("{"), s.rfind("}")
                if i >= 0 and j > i:
                    return json.loads(s[i : j + 1])
            except Exception:
                return None
        return None

    def _json_repairish(self, s: str) -> str:
        s = s.strip()
        if s.startswith("```"):
            s = s.strip("`").split("\n", 1)[-1]
        return s.replace(",}", "}").replace(",]", "]")

    def _offline_ask(self, question: str, pack: Dict[str, Any]) -> str:
        q = (question or "").lower()
        hits: List[str] = []

        def scan(name, val):
            if isinstance(val, dict):
                for kk, vv in val.items():
                    scan(f"{name}.{kk}", vv)
            elif isinstance(val, list):
                for i, vv in enumerate(val):
                    scan(f"{name}[{i}]", vv)
            else:
                s = str(val)
                if q and (q in s.lower()):
                    hits.append(f"- {name}: {s[:220] + ('…' if len(s)>220 else '')}")

        for k, v in pack.items():
            scan(k, v)
        if not hits:
            hits.append("No direct match in current files.")
        return "Relevant bits I found:\n" + "\n".join(hits[:12])

    def _offline_json(self, goal: str, schema: Dict[str, Any], task: str, pack: Dict[str, Any]) -> dict:
        seed = hash(json.dumps(pack, sort_keys=True) + task) & 0xFFFFFFFF
        rng = random.Random(seed)
        return {
            "note": "offline-stub",
            "goal": goal,
            "task_hint": task[:160],
            "seed": seed,
            "rand": rng.randint(1, 999999),
        }
