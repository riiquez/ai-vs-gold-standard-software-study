"""Compatibilidade — use jetuml.domain.validation."""

from jetuml.domain.validation import EdgeValidator, can_paste_into
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge


def validate_new_edge(diagram: Diagram, edge: DiagramEdge) -> str | None:
    return EdgeValidator(diagram).validate(edge)


__all__ = ["validate_new_edge", "can_paste_into", "EdgeValidator"]