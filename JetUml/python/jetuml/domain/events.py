from __future__ import annotations

from typing import Callable, Protocol


class DiagramObserver(Protocol):
    def on_diagram_changed(self) -> None: ...


class DiagramSubject:
    """Observer: notifica interessados em mudanças do documento."""

    def __init__(self) -> None:
        self._observers: list[Callable[[], None]] = []

    def attach(self, callback: Callable[[], None]) -> None:
        if callback not in self._observers:
            self._observers.append(callback)

    def detach(self, callback: Callable[[], None]) -> None:
        if callback in self._observers:
            self._observers.remove(callback)

    def notify_observers(self) -> None:
        for cb in self._observers:
            cb()