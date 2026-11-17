from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Protocol

from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode


class ICommand(ABC):
    @abstractmethod
    def undo(self) -> None:
        pass

    @abstractmethod
    def redo(self) -> None:
        pass


class IEdgeValidationStrategy(ABC):
    """Strategy: regras específicas por tipo de diagrama (RF18–RF19)."""

    @abstractmethod
    def validate_edge(
        self,
        diagram: Diagram,
        edge: DiagramEdge,
        source: DiagramNode,
        target: DiagramNode,
    ) -> str | None:
        pass


class IDiagramRepository(Protocol):
    def save(self, diagram: Diagram, path: str) -> None: ...

    def load(self, path: str) -> Diagram: ...


class OperationResult(Protocol):
    ok: bool
    message: str | None