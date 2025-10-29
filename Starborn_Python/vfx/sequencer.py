from __future__ import annotations
"""Lightweight key‑frame sequencer for Starborn VFX & cinematics.

This module is **engine‑agnostic**: it only needs Kivy’s `Animation` and
`Clock`.  A `Timeline` is basically a scheduler that fires Animations and
callables at absolute times.  You can nest Timelines, re‑use clips, and await
completion from coroutines or plain callbacks.

Usage (runtime):

    tl = Timeline()
    tl.add_property(hero_sprite, "opacity", 0, 1, start=0.0, dur=0.6, ease="out_quad")
    tl.add_event(0.3, play_sfx, "whoosh")
    tl.play(on_complete=next_step)

The classes are tiny by design; anything fancy (Bezier paths, onion‑skinning
preview, visual graph) lives in the **Cinematics Editor**, which will emit a
JSON clip the game loads at runtime and pipes into this module.
"""
from dataclasses import dataclass, field
from typing import Any, Callable, List, Tuple
from kivy.clock import Clock
from kivy.animation import Animation

__all__ = [
    "Timeline",
    "Clip",
]

# ---------------------------------------------------------------------------
#  Core containers
# ---------------------------------------------------------------------------
@dataclass
class _AnimHandle:
    start_time: float
    anim: Animation
    target: Any

@dataclass
class _EventHandle:
    time: float
    callback: Callable
    args: Tuple
    kwargs: dict

@dataclass
class Clip:
    """Serializable definition of a reusable VFX sequence.

    The Cinematics Editor will save these as JSON so designers can drop a
    “cinematic slam” anywhere.  At runtime you feed the dict straight into
    `Timeline.from_dict()`.
    """
    duration: float = 0.0
    anims:   List[_AnimHandle]  = field(default_factory=list)
    events:  List[_EventHandle] = field(default_factory=list)

    def to_dict(self) -> dict:
        # Very minimal – enough for the editor to round‑trip.
        out = {
            "duration": self.duration,
            "anims": [
                {
                    "t": h.start_time,
                    "prop": list(h.anim._animated_properties.keys())[0],
                    "to": list(h.anim._animated_properties.values())[0].value,
                    "dur": h.anim._duration,
                    "ease": h.anim.t,
                }
                for h in self.anims
            ],
            "events": [
                {"t": e.time, "name": e.callback.__name__, "args": e.args}
                for e in self.events
            ],
        }
        return out

    @staticmethod
    def from_dict(d: dict, target: Any, event_registry: dict[str, Callable]):
        tl = Timeline()
        for a in d.get("anims", []):
            prop = a["prop"]
            tl.add_property(
                target,
                prop,
                start_value=getattr(target, prop),
                end_value=a["to"],
                start=a["t"],
                dur=a["dur"],
                ease=a.get("ease", "linear"),
            )
        for ev in d.get("events", []):
            fn = event_registry.get(ev["name"])
            if fn:
                tl.add_event(ev["t"], fn, *ev.get("args", []))
        return tl

# ---------------------------------------------------------------------------
#  Timeline – runtime engine
# ---------------------------------------------------------------------------
class Timeline:
    """Fire‑and‑forget mapper from absolute time → (Animation | callback)."""

    def __init__(self):
        self._anims: List[_AnimHandle] = []
        self._events: List[_EventHandle] = []
        self.duration: float = 0.0
        self._started = False

    # -------------------------------------------------------------------
    #  Builder helpers
    # -------------------------------------------------------------------
    def add_property(
        self,
        target: Any,
        prop: str,
        end_value: Any,
        *,
        start_value: Any | None = None,
        start: float = 0.0,
        dur: float = 1.0,
        ease: str = "linear",
    ) -> "Timeline":
        """Schedule an Animation of *one* property on *target*."""
        if start_value is not None:
            setattr(target, prop, start_value)
        anim = Animation(**{prop: end_value}, duration=dur, t=ease)
        self._anims.append(_AnimHandle(start, anim, target))
        self.duration = max(self.duration, start + dur)
        return self

    def add_event(self, time: float, callback: Callable, *args, **kwargs) -> "Timeline":
        """Call *callback* at *time*.  The function runs in the Kivy main thread."""
        self._events.append(_EventHandle(time, callback, args, kwargs))
        self.duration = max(self.duration, time)
        return self

    def extend(self, other: "Timeline", offset: float = 0.0) -> "Timeline":
        """Merge **other** into *this* timeline (with optional offset)."""
        for h in other._anims:
            self._anims.append(_AnimHandle(h.start_time + offset, h.anim, h.target))
        for e in other._events:
            self._events.append(_EventHandle(e.time + offset, e.callback, e.args, e.kwargs))
        self.duration = max(self.duration, offset + other.duration)
        return self

    # -------------------------------------------------------------------
    #  Playback
    # -------------------------------------------------------------------
    def play(self, *, on_complete: Callable | None = None):
        if self._started:
            return  # don’t double‑play
        self._started = True

        # Schedule Animations
        for h in self._anims:
            Clock.schedule_once(lambda _dt, hh=h: hh.anim.start(hh.target), h.start_time)
        # Schedule events
        for e in self._events:
            Clock.schedule_once(lambda _dt, ee=e: ee.callback(*ee.args, **ee.kwargs), e.time)
        # All done
        if on_complete:
            Clock.schedule_once(lambda _dt: on_complete(), self.duration)

    # Magic so you can `await Timeline().play_async()` in async coroutines.
    async def play_async(self):  # type: ignore[override]
        from asyncio import get_event_loop
        loop = get_event_loop()
        fut = loop.create_future()
        self.play(on_complete=lambda: fut.set_result(True))
        await fut
