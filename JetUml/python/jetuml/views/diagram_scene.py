from __future__ import annotations

from PyQt5.QtCore import QPointF, Qt, pyqtSignal
from PyQt5.QtGui import QFont
from PyQt5.QtWidgets import QGraphicsScene

from jetuml.presentation.presenters.diagram_presenter import DiagramPresenter
from jetuml.presentation.views.graphics.base_node_item import BaseNodeGraphicsItem
from jetuml.models.diagram import Diagram
from jetuml.views.graphics_items import EdgeItem, GridItem, create_node_item, set_dark_mode


class DiagramScene(QGraphicsScene):
    """View (MVC): renderiza o modelo e encaminha ações ao presenter/controller."""

    model_changed = pyqtSignal()
    notify = pyqtSignal(str, bool)
    request_properties = pyqtSignal(str, bool)

    def __init__(self, diagram: Diagram, parent=None) -> None:
        super().__init__(parent)
        self.diagram = diagram
        self.presenter = DiagramPresenter(diagram, self)
        self._node_items: dict[str, BaseNodeGraphicsItem] = {}
        self._edge_items: dict[str, EdgeItem] = {}
        self._grid: GridItem | None = None
        self.active_tool: str | None = None
        self.auto_edit_node = False
        self._font = QFont(diagram.settings.font_family, diagram.settings.font_size)
        self.rebuild()

    @property
    def history(self):
        return self.presenter.history

    @property
    def controller(self):
        return self.presenter.controller

    def set_font(self, family: str, size: int) -> None:
        self._font = QFont(family, size)
        self.rebuild()

    def set_dark(self, enabled: bool) -> None:
        set_dark_mode(enabled)
        self.rebuild()

    def rebuild(self) -> None:
        self.clear()
        self._node_items.clear()
        self._edge_items.clear()
        self._grid = GridItem(
            self.diagram.width,
            self.diagram.height,
            self.diagram.settings.snap_grid,
            self.diagram.settings.grid_visible,
        )
        self.addItem(self._grid)
        self.setSceneRect(0, 0, self.diagram.width, self.diagram.height)
        for node in self.diagram.nodes:
            if node.parent_id and not self.diagram.node_by_id(node.parent_id):
                node.parent_id = None
            item = create_node_item(node, self._font)
            self.addItem(item)
            self._node_items[node.id] = item
        for edge in self.diagram.edges:
            src = self._node_items.get(edge.source_id)
            tgt = self._node_items.get(edge.target_id)
            ei = EdgeItem(edge, src, tgt)
            self.addItem(ei)
            self._edge_items[edge.id] = ei
        self._refresh_edges()

    def _refresh_edges(self) -> None:
        for ei in self._edge_items.values():
            ei.refresh()

    def snap_point(self, p: QPointF) -> QPointF:
        return QPointF(self.diagram.snap(p.x()), self.diagram.snap(p.y()))

    def create_node_at(self, pos: QPointF, kind: str, parent_id: str | None = None) -> None:
        self.controller.create_node_at(pos, kind, parent_id)

    def _parent_package_at(self, pos: QPointF) -> str | None:
        return self.controller.parent_package_at(pos)

    def try_create_edge(
        self, src_id: str, tgt_id: str, kind: str, target_point: QPointF | None = None
    ) -> None:
        self.controller.try_create_edge(src_id, tgt_id, kind, target_point)

    def delete_selection(self) -> None:
        from jetuml.views.graphics_items import EdgeItem as EI

        selected_nodes = []
        selected_edges = []
        for item in self.selectedItems():
            if isinstance(item, BaseNodeGraphicsItem):
                selected_nodes.append(item.node)
            elif isinstance(item, EI):
                selected_edges.append(item.edge)
        self.controller.delete_elements(selected_nodes, selected_edges)

    def select_all(self) -> None:
        for item in self._node_items.values():
            item.setSelected(True)
        for item in self._edge_items.values():
            item.setSelected(True)

    def attach_detach_package(self, node_id: str) -> None:
        item = self._node_items.get(node_id)
        if not item:
            return
        self.controller.attach_detach_package(node_id, item.pos())

    def finish_move(self, before: dict[str, tuple[float, float]]) -> None:
        after = {nid: (it.node.x, it.node.y) for nid, it in self._node_items.items() if nid in before}
        self.controller.finish_move(before, after)
        self._refresh_edges()