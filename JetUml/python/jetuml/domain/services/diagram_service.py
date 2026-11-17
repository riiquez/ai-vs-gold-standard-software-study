from __future__ import annotations

from dataclasses import dataclass

from PyQt5.QtCore import QPointF, QRectF

from jetuml.domain.factories.element_factory import ElementFactory
from jetuml.domain.validation import EdgeValidator
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode
from jetuml.models.kinds import NodeKind


@dataclass
class ServiceResult:
    ok: bool
    message: str | None = None
    silent: bool = False
    node: DiagramNode | None = None
    edge: DiagramEdge | None = None


class DiagramService:
    """Lógica de domínio do diagrama (sem Qt, exceto QPointF como valor)."""

    def __init__(self, diagram: Diagram) -> None:
        self.diagram = diagram
        self._validator = EdgeValidator(diagram)

    def refresh_validator(self) -> None:
        self._validator = EdgeValidator(self.diagram)

    def parent_package_at(self, pos: QPointF) -> str | None:
        for n in reversed(self.diagram.nodes):
            if n.kind != NodeKind.PACKAGE.value:
                continue
            if QRectF(n.x, n.y, n.width, n.height).contains(pos):
                return n.id
        return None

    def create_node(self, kind: str, pos: QPointF, parent_id: str | None = None) -> ServiceResult:
        if kind == NodeKind.FIELD.value and not parent_id:
            return ServiceResult(ok=False, message="Campo deve pertencer a um objeto.")
        node = ElementFactory.create_node(kind, pos, self.diagram, parent_id)
        return ServiceResult(ok=True, node=node)

    def create_edge(
        self,
        kind: str,
        source_id: str,
        target_id: str,
        target_point: QPointF | None = None,
    ) -> ServiceResult:
        edge = ElementFactory.create_edge(kind, source_id, target_id or "")
        if target_point and not target_id:
            edge.target_point = (target_point.x(), target_point.y())
        return self.register_edge(edge)

    def register_edge(self, edge: DiagramEdge) -> ServiceResult:
        err = self._validator.validate(edge)
        if err == "__silent__":
            return ServiceResult(ok=False, silent=True)
        if err:
            return ServiceResult(ok=False, message=err)
        return ServiceResult(ok=True, edge=edge)

    def attach_detach_package(self, node_id: str, position: QPointF) -> None:
        node = self.diagram.node_by_id(node_id)
        if not node:
            return
        pid = self.parent_package_at(position)
        if pid == node_id:
            return
        if node.parent_id and not pid:
            node.parent_id = None
        elif pid:
            node.parent_id = pid
        self.diagram.dirty = True

    def pop_pending_nodes(self) -> list[DiagramNode]:
        return []

    def pop_pending_edge(self) -> DiagramEdge | None:
        return None