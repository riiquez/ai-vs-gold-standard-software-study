from __future__ import annotations

from PyQt5.QtCore import QPoint, QPointF, QRect, Qt, pyqtSignal
from PyQt5.QtGui import QKeyEvent, QMouseEvent, QPainter, QPen, QWheelEvent
from PyQt5.QtWidgets import QGraphicsView, QRubberBand

from jetuml.models.kinds import EdgeKind, NodeKind
from jetuml.views.diagram_scene import DiagramScene
from jetuml.views.graphics_items import EdgeItem, NodeItem


class DiagramCanvas(QGraphicsView):
    copy_image_requested = pyqtSignal()

    ZOOM_STEPS = [0.5, 0.71, 1.0, 1.41, 2.0]
    _zoom_index = 2

    def __init__(self, scene: DiagramScene, parent=None) -> None:
        super().__init__(scene, parent)
        self.setRenderHint(QPainter.Antialiasing)
        self.setDragMode(QGraphicsView.RubberBandDrag)
        self.setTransformationAnchor(QGraphicsView.AnchorUnderMouse)
        self._scene = scene
        self._rubber = QRubberBand(QRubberBand.Rectangle, self)
        self._lasso_origin: QPoint | None = None
        self._move_before: dict[str, tuple[float, float]] | None = None
        self._edge_drag_src: str | None = None
        self._edge_drag_end: QPointF | None = None
        self.reset_zoom()

    def reset_zoom(self) -> None:
        self._zoom_index = 2
        self.setTransform(self.transform().__class__())
        self.scale(1.0, 1.0)

    def zoom_in(self) -> None:
        if self._zoom_index < len(self.ZOOM_STEPS) - 1:
            self._zoom_index += 1
            self._apply_zoom()

    def zoom_out(self) -> None:
        if self._zoom_index > 0:
            self._zoom_index -= 1
            self._apply_zoom()

    def _apply_zoom(self) -> None:
        s = self.ZOOM_STEPS[self._zoom_index]
        self.resetTransform()
        self.scale(s, s)

    def wheelEvent(self, event: QWheelEvent) -> None:
        if event.modifiers() & Qt.ControlModifier:
            if event.angleDelta().y() > 0:
                self.zoom_in()
            else:
                self.zoom_out()
            event.accept()
            return
        super().wheelEvent(event)

    def _scene_pos(self, event: QMouseEvent) -> QPointF:
        return self.mapToScene(event.pos())

    def _node_at(self, event: QMouseEvent) -> NodeItem | None:
        sp = self._scene_pos(event)
        item = self.itemAt(event.pos())
        if isinstance(item, NodeItem):
            return item
        if item and item.parentItem() and isinstance(item.parentItem(), NodeItem):
            return item.parentItem()
        for ni in self._scene._node_items.values():
            if ni.sceneBoundingRect().contains(sp):
                return ni
        return None

    def mousePressEvent(self, event: QMouseEvent) -> None:
        tool = self._scene.active_tool
        sp = self._scene_pos(event)

        if event.button() == Qt.RightButton:
            super().mousePressEvent(event)
            return

        if tool is None or tool == "select":
            if event.button() == Qt.LeftButton and not self.itemAt(event.pos()):
                self._lasso_origin = event.pos()
                self._rubber.setGeometry(QRect(event.pos(), event.pos()))
                self._rubber.show()
            item = self.itemAt(event.pos())
            if isinstance(item, NodeItem):
                self._move_before = {item.node.id: (item.node.x, item.node.y)}
            super().mousePressEvent(event)
            return

        if tool == "__clipboard__":
            self.copy_image_requested.emit()
            return

        if event.button() != Qt.LeftButton:
            super().mousePressEvent(event)
            return

        # criação de nó
        if tool in {k.value for k in NodeKind}:
            parent = self._scene._parent_package_at(sp)
            if tool == NodeKind.FIELD.value:
                for it in self._scene._node_items.values():
                    r = it.sceneBoundingRect()
                    if r.contains(sp) and it.node.kind == NodeKind.OBJECT.value:
                        parent = it.node.id
                        break
                if not parent:
                    self._scene.notify.emit("Campo deve ser criado dentro de um objeto.", True)
                    return
            self._scene.create_node_at(sp, tool, parent)
            return

        # criação de aresta
        if tool in {k.value for k in EdgeKind}:
            ni = self._node_at(event)
            if ni:
                self._edge_drag_src = ni.node.id
            return

        super().mousePressEvent(event)

    def mouseMoveEvent(self, event: QMouseEvent) -> None:
        if self._lasso_origin is not None:
            self._rubber.setGeometry(
                QRect(self._lasso_origin, event.pos()).normalized()
            )
        if self._edge_drag_src:
            self._edge_drag_end = self._scene_pos(event)
            self.viewport().update()
        super().mouseMoveEvent(event)

    def mouseDoubleClickEvent(self, event: QMouseEvent) -> None:
        item = self.itemAt(event.pos())
        from jetuml.views.graphics_items import EdgeItem, NodeItem

        if isinstance(item, NodeItem):
            self._scene.request_properties.emit(item.node.id, False)
        elif isinstance(item, EdgeItem):
            self._scene.request_properties.emit(item.edge.id, True)
        super().mouseDoubleClickEvent(event)

    def mouseReleaseEvent(self, event: QMouseEvent) -> None:
        if self._lasso_origin is not None:
            self._rubber.hide()
            self._lasso_origin = None
        sp = self._scene_pos(event)
        tool = self._scene.active_tool

        if self._move_before and (tool is None or tool == "select"):
            self._scene.finish_move(self._move_before)
            self._move_before = None

        if self._edge_drag_src and tool:
            tgt = self._node_at(event)
            tgt_id = tgt.node.id if tgt and tgt.node.id != self._edge_drag_src else None
            src_n = self._scene.diagram.node_by_id(self._edge_drag_src) if self._edge_drag_src else None
            if tool == EdgeKind.NOTE_CONNECTOR.value and src_n and src_n.kind == NodeKind.NOTE.value and not tgt_id:
                self._scene.try_create_edge(self._edge_drag_src, self._edge_drag_src, tool, target_point=sp)
            elif tgt_id:
                self._scene.try_create_edge(self._edge_drag_src, tgt_id, tool)
            elif not tgt_id and self._scene._node_items.get(self._edge_drag_src or ""):
                same = self._scene._node_items[self._edge_drag_src]
                if same.sceneBoundingRect().contains(sp):
                    self._scene.try_create_edge(self._edge_drag_src, self._edge_drag_src, tool)
            self._edge_drag_src = None
            self._edge_drag_end = None
            self.viewport().update()

        super().mouseReleaseEvent(event)

    def keyPressEvent(self, event: QKeyEvent) -> None:
        if event.key() == Qt.Key_Shift:
            for item in self._scene.selectedItems():
                if isinstance(item, NodeItem):
                    self._scene.attach_detach_package(item.node.id)
        super().keyPressEvent(event)

    def drawForeground(self, painter: QPainter, rect) -> None:
        super().drawForeground(painter, rect)
        if self._edge_drag_src and self._edge_drag_end and self._scene.active_tool:
            src = self._scene._node_items.get(self._edge_drag_src)
            if src:
                p1 = src.center_scene_pos()
                pen = QPen(Qt.darkGray, 1, Qt.DashLine)
                painter.setPen(pen)
                painter.drawLine(p1, self._edge_drag_end)