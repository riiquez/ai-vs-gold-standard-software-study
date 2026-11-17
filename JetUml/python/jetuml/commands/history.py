from __future__ import annotations

from abc import abstractmethod

from jetuml.domain.interfaces import ICommand
from copy import deepcopy

from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode


class Command(ICommand):
    @abstractmethod
    def undo(self) -> None:
        pass

    @abstractmethod
    def redo(self) -> None:
        pass


class CommandHistory:
    def __init__(self) -> None:
        self._undo: list[Command] = []
        self._redo: list[Command] = []

    def execute(self, cmd: Command) -> None:
        cmd.redo()
        self._undo.append(cmd)
        self._redo.clear()

    def undo(self) -> bool:
        if not self._undo:
            return False
        cmd = self._undo.pop()
        cmd.undo()
        self._redo.append(cmd)
        return True

    def redo(self) -> bool:
        if not self._redo:
            return False
        cmd = self._redo.pop()
        cmd.redo()
        self._undo.append(cmd)
        return True

    def clear(self) -> None:
        self._undo.clear()
        self._redo.clear()


class AddNodeCommand(Command):
    def __init__(self, diagram: Diagram, node: DiagramNode) -> None:
        self.diagram = diagram
        self.node = node

    def undo(self) -> None:
        self.diagram.nodes = [n for n in self.diagram.nodes if n.id != self.node.id]

    def redo(self) -> None:
        self.diagram.nodes.append(self.node)
        self.diagram.dirty = True


class RemoveElementsCommand(Command):
    def __init__(
        self,
        diagram: Diagram,
        nodes: list[DiagramNode],
        edges: list[DiagramEdge],
    ) -> None:
        self.diagram = diagram
        self.nodes = deepcopy(nodes)
        self.edges = deepcopy(edges)

    def undo(self) -> None:
        self.diagram.nodes.extend(deepcopy(self.nodes))
        self.diagram.edges.extend(deepcopy(self.edges))

    def redo(self) -> None:
        ids = {n.id for n in self.nodes} | {e.id for e in self.edges}
        self.diagram.nodes = [n for n in self.diagram.nodes if n.id not in ids]
        self.diagram.edges = [e for e in self.diagram.edges if e.id not in ids]
        self.diagram.dirty = True


class AddEdgeCommand(Command):
    def __init__(self, diagram: Diagram, edge: DiagramEdge) -> None:
        self.diagram = diagram
        self.edge = edge

    def undo(self) -> None:
        self.diagram.edges = [e for e in self.diagram.edges if e.id != self.edge.id]

    def redo(self) -> None:
        self.diagram.edges.append(self.edge)
        self.diagram.dirty = True


class MoveNodesCommand(Command):
    def __init__(
        self,
        diagram: Diagram,
        before: dict[str, tuple[float, float]],
        after: dict[str, tuple[float, float]],
    ) -> None:
        self.diagram = diagram
        self.before = before
        self.after = after

    def undo(self) -> None:
        for nid, (x, y) in self.before.items():
            n = self.diagram.node_by_id(nid)
            if n:
                n.x, n.y = x, y

    def redo(self) -> None:
        for nid, (x, y) in self.after.items():
            n = self.diagram.node_by_id(nid)
            if n:
                n.x, n.y = x, y
        self.diagram.dirty = True


class SetPropertiesCommand(Command):
    def __init__(
        self,
        diagram: Diagram,
        target_id: str,
        is_edge: bool,
        before: dict,
        after: dict,
    ) -> None:
        self.diagram = diagram
        self.target_id = target_id
        self.is_edge = is_edge
        self.before = deepcopy(before)
        self.after = deepcopy(after)

    def _props(self, data: dict) -> None:
        if self.is_edge:
            for e in self.diagram.edges:
                if e.id == self.target_id:
                    e.properties = deepcopy(data)
                    break
        else:
            n = self.diagram.node_by_id(self.target_id)
            if n:
                n.properties = deepcopy(data)

    def undo(self) -> None:
        self._props(self.before)

    def redo(self) -> None:
        self._props(self.after)
        self.diagram.dirty = True