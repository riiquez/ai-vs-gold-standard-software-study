from __future__ import annotations

from jetuml.domain.interfaces import IEdgeValidationStrategy
from jetuml.domain.validation.chain import count_edges_between
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode
from jetuml.models.kinds import EdgeKind


class UseCaseDiagramValidationStrategy(IEdgeValidationStrategy):
    def validate_edge(
        self,
        diagram: Diagram,
        edge: DiagramEdge,
        source: DiagramNode,
        target: DiagramNode,
    ) -> str | None:
        if edge.kind == EdgeKind.NOTE_CONNECTOR.value:
            return None
        if count_edges_between(diagram, source.id, target.id, edge.kind) >= 1:
            return "Já existe uma aresta deste tipo entre estes nós."
        return None