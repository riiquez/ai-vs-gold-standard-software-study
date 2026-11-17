from __future__ import annotations

from dataclasses import dataclass
from typing import Callable

from PyQt5.QtCore import QPointF

from jetuml.domain.commands import (
    AddEdgeCommand,
    AddNodeCommand,
    CommandHistory,
    MoveNodesCommand,
    RemoveElementsCommand,
)
from jetuml.domain.services.diagram_service_factory import DiagramServiceFactory
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode


@dataclass
class DiagramControllerCallbacks:
    on_model_changed: Callable[[], None]
    on_notify: Callable[[str, bool], None]
    on_request_properties: Callable[[str, bool], None]


class DiagramController:
    def __init__(
        self,
        diagram: Diagram,
        history: CommandHistory,
        callbacks: DiagramControllerCallbacks,
    ) -> None:
        self.diagram = diagram
        self.history = history
        self._callbacks = callbacks
        self._service = DiagramServiceFactory.create(diagram)
        self.auto_edit_node = False

    def _flush_pending_nodes(self) -> None:
        for node in self._service.pop_pending_nodes():
            self.history.execute(AddNodeCommand(self.diagram, node))

    def create_node_at(self, pos: QPointF, kind: str, parent_id: str | None = None) -> None:
        result = self._service.create_node(kind, pos, parent_id)
        if not result.ok:
            if result.message:
                self._callbacks.on_notify(result.message, True)
            return
        assert result.node
        self.history.execute(AddNodeCommand(self.diagram, result.node))
        self._flush_pending_nodes()
        self._callbacks.on_model_changed()
        if self.auto_edit_node and kind != "note":
            self._callbacks.on_request_properties(result.node.id, False)

    def parent_package_at(self, pos: QPointF) -> str | None:
        return self._service.parent_package_at(pos)

    def try_create_edge(
        self, src_id: str, tgt_id: str, kind: str, target_point: QPointF | None = None
    ) -> None:
        result = self._service.create_edge(kind, src_id, tgt_id, target_point)
        if not result.ok:
            if result.silent:
                return
            if result.message:
                self._callbacks.on_notify(result.message, True)
            return
        assert result.edge
        self.history.execute(AddEdgeCommand(self.diagram, result.edge))
        self._flush_pending_nodes()
        self._callbacks.on_model_changed()

    def delete_elements(self, nodes: list[DiagramNode], edges: list[DiagramEdge]) -> None:
        if not nodes and not edges:
            return
        ids = {n.id for n in nodes}
        extra = [e for e in self.diagram.edges if e.source_id in ids or e.target_id in ids]
        merged = {e.id: e for e in edges + extra}
        self.history.execute(RemoveElementsCommand(self.diagram, nodes, list(merged.values())))
        self._callbacks.on_model_changed()

    def attach_detach_package(self, node_id: str, position: QPointF) -> None:
        self._service.attach_detach_package(node_id, position)
        self._callbacks.on_model_changed()

    def finish_move(
        self, before: dict[str, tuple[float, float]], after: dict[str, tuple[float, float]]
    ) -> None:
        if before != after:
            self.history.execute(MoveNodesCommand(self.diagram, before, after))
        self._callbacks.on_model_changed()