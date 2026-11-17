from __future__ import annotations

from jetuml.domain.interfaces import IEdgeValidationStrategy
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode
from jetuml.models.kinds import EdgeKind, NodeKind


class SequenceDiagramValidationStrategy(IEdgeValidationStrategy):
    def validate_edge(
        self,
        diagram: Diagram,
        edge: DiagramEdge,
        source: DiagramNode,
        target: DiagramNode,
    ) -> str | None:
        if edge.kind not in (EdgeKind.CALL.value, EdgeKind.RETURN.value):
            return None
        valid = {NodeKind.ACTIVATION.value, NodeKind.LIFELINE.value, NodeKind.IMPLICIT_PARAM.value}
        if source.kind not in valid or target.kind not in valid:
            return "__silent__"
        return None