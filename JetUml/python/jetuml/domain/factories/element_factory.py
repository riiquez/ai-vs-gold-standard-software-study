from __future__ import annotations

from PyQt5.QtCore import QPointF

from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode, default_edge_properties, default_node_properties
from jetuml.models.kinds import NodeKind


class ElementFactory:
    """Factory Method: cria nós e arestas com propriedades padrão."""

    _NODE_SIZES: dict[str, tuple[float, float]] = {
        NodeKind.NOTE.value: (120.0, 60.0),
        NodeKind.INITIAL.value: (24.0, 24.0),
        NodeKind.FINAL.value: (24.0, 24.0),
        NodeKind.PACKAGE.value: (200.0, 150.0),
    }

    @classmethod
    def create_node(
        cls,
        kind: str,
        position: QPointF,
        diagram: Diagram,
        parent_id: str | None = None,
    ) -> DiagramNode:
        w, h = cls._NODE_SIZES.get(kind, (140.0, 80.0))
        props = default_node_properties(kind)
        return DiagramNode(
            kind=kind,
            x=diagram.snap(position.x()),
            y=diagram.snap(position.y()),
            width=w,
            height=h,
            parent_id=parent_id,
            properties=props,
        )

    @staticmethod
    def create_edge(kind: str, source_id: str, target_id: str) -> DiagramEdge:
        return DiagramEdge(
            kind=kind,
            source_id=source_id,
            target_id=target_id,
            properties=default_edge_properties(kind),
        )