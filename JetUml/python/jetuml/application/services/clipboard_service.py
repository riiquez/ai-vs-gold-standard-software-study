from __future__ import annotations

import copy
from typing import Callable

from jetuml.domain.commands import AddEdgeCommand, AddNodeCommand, CommandHistory
from jetuml.domain.validation import can_paste_into
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode, new_id


class ClipboardService:
    """Serviço de aplicação: recortar/copiar/colar elementos UML."""

    def __init__(self) -> None:
        self._buffer: list[tuple[str, dict]] = []

    def cut_or_copy(self, nodes: list[DiagramNode], edges: list[DiagramEdge], cut: bool) -> list:
        """Retorna elementos a remover se cut=True."""
        self._buffer.clear()
        for n in nodes:
            self._buffer.append(("node", copy.deepcopy(n).to_dict()))
        for e in edges:
            self._buffer.append(("edge", copy.deepcopy(e).to_dict()))
        return nodes + [] if not cut else nodes

    def paste(
        self,
        diagram: Diagram,
        history: CommandHistory,
        on_error: Callable[[str], None],
        on_done: Callable[[], None],
    ) -> None:
        if not self._buffer:
            return
        kinds = [x[1].get("kind") for x in self._buffer]
        err = can_paste_into(diagram.diagram_type, kinds)
        if err:
            on_error(err)
            return
        id_map: dict[str, str] = {}
        for kind, data in self._buffer:
            if kind == "node":
                old_id = data["id"]
                node = DiagramNode.from_dict(data)
                node.id = new_id()
                node.x += 20
                node.y += 20
                id_map[old_id] = node.id
                if node.parent_id and node.parent_id in id_map:
                    node.parent_id = id_map[node.parent_id]
                history.execute(AddNodeCommand(diagram, node))
            elif kind == "edge":
                edge = DiagramEdge.from_dict(data)
                edge.id = new_id()
                edge.source_id = id_map.get(edge.source_id, edge.source_id)
                edge.target_id = id_map.get(edge.target_id, edge.target_id)
                history.execute(AddEdgeCommand(diagram, edge))
        on_done()