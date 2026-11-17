from __future__ import annotations

from PyQt5.QtCore import Qt
from PyQt5.QtGui import QBrush, QColor, QPainterPath, QPen

from jetuml.presentation.views.graphics.base_node_item import BaseNodeGraphicsItem, RectangularNodeItem


class NoteNodeItem(RectangularNodeItem):
    def display_text(self) -> str:
        return self.node.properties.get("text", "Note")


class UseCaseNodeItem(RectangularNodeItem):
    def draw_shape(self, painter) -> None:
        path = QPainterPath()
        path.addEllipse(self.rect())
        painter.setPen(self.pen())
        painter.setBrush(self.brush())
        painter.drawPath(path)


class ActorNodeItem(RectangularNodeItem):
    def __init__(self, node, font) -> None:
        super().__init__(node, font)
        self.setRect(0, 0, 50, 80)


class InitialStateNodeItem(BaseNodeGraphicsItem):
    def display_text(self) -> str:
        return ""

    def draw_shape(self, painter) -> None:
        pal = self._palette()
        fill = QColor(30, 30, 30) if pal.bg.lightness() > 128 else QColor(220, 220, 220)
        painter.setPen(QPen(Qt.NoPen))
        painter.setBrush(QBrush(fill))
        painter.drawEllipse(self.rect())


class FinalStateNodeItem(BaseNodeGraphicsItem):
    def display_text(self) -> str:
        return ""

    def draw_shape(self, painter) -> None:
        painter.setPen(self.pen())
        painter.setBrush(QBrush(Qt.NoBrush))
        painter.drawEllipse(self.rect())
        inner = self.rect().adjusted(6, 6, -6, -6)
        painter.setBrush(self.brush())
        painter.drawEllipse(inner)