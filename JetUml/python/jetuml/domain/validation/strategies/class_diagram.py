from __future__ import annotations

from jetuml.domain.interfaces import IEdgeValidationStrategy
from jetuml.domain.validation.chain import count_edges_between
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode
from jetuml.models.kinds import EdgeKind

CLASS_CYCLE_EDGES = {
    EdgeKind.INHERITANCE.value,
    EdgeKind.DEPENDENCY.value,
    EdgeKind.AGGREGATION.value,
    EdgeKind.ASSOCIATION.value,
}


class ClassDiagramValidationStrategy(IEdgeValidationStrategy):
    def validate_edge(
        self,
        diagram: Diagram,
        edge: DiagramEdge,
        source: DiagramNode,
        target: DiagramNode,
    ) -> str | None:
        k = edge.kind
        if k in CLASS_CYCLE_EDGES:
            for e in diagram.edges:
                if e.kind != k:
                    continue
                if e.source_id == target.id and e.target_id == source.id:
                    return f"Ciclo direto não permitido para {k}."
        if k in (EdgeKind.ASSOCIATION.value, EdgeKind.AGGREGATION.value):
            other = (
                EdgeKind.AGGREGATION.value
                if k == EdgeKind.ASSOCIATION.value
                else EdgeKind.ASSOCIATION.value
            )
            for e in diagram.edges:
                if e.kind == other and {e.source_id, e.target_id} == {source.id, target.id}:
                    return "Associação e agregação não podem coexistir entre as mesmas classes."
        if count_edges_between(diagram, source.id, target.id, k) >= 1:
            return "Já existe uma aresta deste tipo entre estes nós."
        return None