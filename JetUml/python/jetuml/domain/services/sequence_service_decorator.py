from __future__ import annotations

from PyQt5.QtCore import QPointF

from jetuml.domain.services.diagram_service import DiagramService, ServiceResult
from jetuml.domain.services.sequence_service import SequenceDiagramService
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode


class SequenceDiagramServiceDecorator:
    """Decorator (composição): especializa DiagramService para sequência."""

    def __init__(self, wrapped: DiagramService, sequence: SequenceDiagramService) -> None:
        self._wrapped = wrapped
        self._sequence = sequence
        self._pending_nodes: list[DiagramNode] = []

    @property
    def diagram(self) -> Diagram:
        return self._wrapped.diagram

    def refresh_validator(self) -> None:
        self._wrapped.refresh_validator()

    def parent_package_at(self, pos: QPointF) -> str | None:
        return self._wrapped.parent_package_at(pos)

    def attach_detach_package(self, node_id: str, position: QPointF) -> None:
        self._wrapped.attach_detach_package(node_id, position)

    def create_node(self, kind: str, pos: QPointF, parent_id: str | None = None) -> ServiceResult:
        result = self._wrapped.create_node(kind, pos, parent_id)
        if result.ok and result.node:
            self._pending_nodes.extend(self._sequence.after_node_created(result.node))
        return result

    def create_edge(
        self,
        kind: str,
        source_id: str,
        target_id: str,
        target_point: QPointF | None = None,
    ) -> ServiceResult:
        extras, preset = self._sequence.before_edge_created(source_id, target_id, kind)
        self._pending_nodes.extend(extras)
        if preset:
            if target_point:
                preset.target_point = (target_point.x(), target_point.y())
            return self._wrapped.register_edge(preset)
        return self._wrapped.create_edge(kind, source_id, target_id, target_point)

    def pop_pending_nodes(self) -> list[DiagramNode]:
        nodes, self._pending_nodes = self._pending_nodes, []
        return nodes

    def pop_pending_edge(self) -> DiagramEdge | None:
        return None