from __future__ import annotations

from jetuml.domain.interfaces import IEdgeValidationStrategy
from jetuml.domain.validation.chain import count_edges_between
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode
from jetuml.models.kinds import EdgeKind, NodeKind


class StateDiagramValidationStrategy(IEdgeValidationStrategy):
    def validate_edge(
        self,
        diagram: Diagram,
        edge: DiagramEdge,
        source: DiagramNode,
        target: DiagramNode,
    ) -> str | None:
        if edge.kind != EdgeKind.TRANSITION.value:
            return None
        if source.kind == NodeKind.FINAL.value:
            return "Transição não pode partir de um estado final."
        if target.kind == NodeKind.INITIAL.value:
            return "Transição não pode chegar a um estado inicial."
        fwd = count_edges_between(diagram, source.id, target.id, EdgeKind.TRANSITION.value, directed=True)
        if fwd >= 2:
            return "No máximo duas transições na mesma direção entre estados."
        return None