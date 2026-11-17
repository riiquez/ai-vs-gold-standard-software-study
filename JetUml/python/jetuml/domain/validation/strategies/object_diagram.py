from __future__ import annotations

from jetuml.domain.interfaces import IEdgeValidationStrategy
from jetuml.domain.validation.chain import count_edges_between
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode
from jetuml.models.kinds import EdgeKind, NodeKind


class ObjectDiagramValidationStrategy(IEdgeValidationStrategy):
    def validate_edge(
        self,
        diagram: Diagram,
        edge: DiagramEdge,
        source: DiagramNode,
        target: DiagramNode,
    ) -> str | None:
        if edge.kind == EdgeKind.COLLABORATION.value:
            for e in diagram.edges:
                if e.kind == EdgeKind.COLLABORATION.value:
                    if {e.source_id, e.target_id} == {source.id, target.id}:
                        return "Ciclo de colaboração entre dois objetos não é permitido."
        if edge.kind == EdgeKind.REFERENCE.value:
            if source.kind != NodeKind.FIELD.value or target.kind != NodeKind.OBJECT.value:
                return "Referência deve ir de um Campo até um Objeto."
        if count_edges_between(diagram, source.id, target.id, edge.kind) >= 1:
            return "Já existe uma aresta deste tipo entre estes nós."
        return None