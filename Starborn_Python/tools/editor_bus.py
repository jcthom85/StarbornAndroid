#!/usr/bin/env python3
from __future__ import annotations
from typing import Callable, Dict, List

class _Bus:
    def __init__(self):
        self._subs: Dict[str, List[Callable]] = {}
    def subscribe(self, topic: str, cb: Callable):
        self._subs.setdefault(topic, []).append(cb)
    def publish(self, topic: str, *args, **kwargs):
        for cb in self._subs.get(topic, []):
            try: cb(*args, **kwargs)
            except Exception: pass

EditorBus = _Bus()

def goto(target_type: str, ident: str):
    EditorBus.publish("goto", target_type, ident)

def refresh_references():
    EditorBus.publish("refresh_refs")
