from __future__ import annotations

from PyQt5.QtCore import QPointF

from jetuml.domain.factories.element_factory import ElementFactory
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode
from jetuml.models.kinds import DiagramType, EdgeKind, NodeKind


class SequenceDiagramService:
    """Regras RF27–RF31 para diagramas de sequência."""

    def __init__(self, diagram: Diagram) -> None:
        self._diagram = diagram

    @property
    def active(self) -> bool:
        return self._diagram.diagram_type == DiagramType.SEQUENCE

    def after_node_created(self, node: DiagramNode) -> list[DiagramNode]:
        extra: list[DiagramNode] = []
        if not self.active:
            return extra
        if node.kind == NodeKind.IMPLICIT_PARAM.value:
            line = ElementFactory.create_node(
                NodeKind.LIFELINE.value,
                QPointF(node.x, node.y + node.height + 20),
                self._diagram,
            )
            line.width = 20.0
            line.height = max(120.0, self._diagram.height - line.y - 40)
            line.properties = {"name": node.properties.get("name", ":params")}
            extra.append(line)
        return extra

    def before_edge_created(
        self, source_id: str, target_id: str, kind: str
    ) -> tuple[list[DiagramNode], DiagramEdge | None]:
        """Cria activations e ajusta rótulos «create»."""
        if not self.active or kind != EdgeKind.CALL.value:
            return [], None
        src = self._diagram.node_by_id(source_id)
        tgt = self._diagram.node_by_id(target_id)
        if not src or not tgt:
            return [], None
        extras: list[DiagramNode] = []
        act_src = self._activation_for(src)
        act_tgt = self._activation_for(tgt)
        if act_src:
            extras.append(act_src)
        if act_tgt and (not act_src or act_tgt.x != act_src.x):
            extras.append(act_tgt)
        if tgt.kind == NodeKind.IMPLICIT_PARAM.value:
            props = {"middle_label": "«create»"}
            tgt.y = src.y + 40
            extras.append(self._activation_at(tgt.x + tgt.width / 2, tgt.y + tgt.height))
            edge = DiagramEdge(
                kind=kind,
                source_id=source_id,
                target_id=target_id,
                properties=props,
            )
            return extras, edge
        return extras, None

    def _activation_for(self, node: DiagramNode) -> DiagramNode | None:
        if node.kind == NodeKind.ACTIVATION.value:
            return None
        x = node.x + node.width / 2 - 8
        y = node.y + (node.height if node.kind != NodeKind.LIFELINE.value else 30)
        act = DiagramNode(
            kind=NodeKind.ACTIVATION.value,
            x=x,
            y=y,
            width=16,
            height=60,
            properties={"name": "", "open_bottom": False},
        )
        return act

    def _activation_at(self, cx: float, y: float) -> DiagramNode:
        return DiagramNode(
            kind=NodeKind.ACTIVATION.value,
            x=cx - 8,
            y=y,
            width=16,
            height=50,
            properties={"open_bottom": False},
        )