from __future__ import annotations

from PyQt5.QtCore import QPointF, Qt
from PyQt5.QtGui import QBrush, QColor, QFont, QPen
from PyQt5.QtWidgets import QGraphicsItem, QGraphicsRectItem, QGraphicsSimpleTextItem

from jetuml.models.elements import DiagramNode
from jetuml.presentation.theme import theme_manager


class BaseNodeGraphicsItem(QGraphicsRectItem):
    """Template Method: paint() delega draw_shape() às subclasses."""

    def __init__(self, node: DiagramNode, font: QFont) -> None:
        super().__init__(0, 0, node.width, node.height)
        self.node = node
        self.font = font
        self.setPos(node.x, node.y)
        self.setFlags(
            QGraphicsItem.ItemIsSelectable
            | QGraphicsItem.ItemIsMovable
            | QGraphicsItem.ItemSendsGeometryChanges
        )
        self._create_label_widgets()
        self._apply_palette()
        self.refresh_label()

    def _create_label_widgets(self) -> None:
        self.label = QGraphicsSimpleTextItem(self)

    def _palette(self):
        return theme_manager().current().palette()

    def _apply_palette(self) -> None:
        pal = self._palette()
        self.setBrush(QBrush(pal.bg))
        self.setPen(QPen(pal.fg, 1.5))

    def refresh_label(self) -> None:
        pal = self._palette()
        self.label.setFont(self.font)
        self.label.setBrush(pal.fg)
        self.label.setText(self.display_text())
        self.label.setPos(8, 8)

    def display_text(self) -> str:
        p = self.node.properties
        return p.get("name", self.node.kind)

    def refresh(self) -> None:
        self.setRect(0, 0, self.node.width, self.node.height)
        self.setPos(self.node.x, self.node.y)
        self._apply_palette()
        self.refresh_label()
        self.prepareGeometryChange()

    def itemChange(self, change, value):
        if change == QGraphicsItem.ItemPositionHasChanged:
            self.node.x = self.pos().x()
            self.node.y = self.pos().y()
        return super().itemChange(change, value)

    def center_scene_pos(self) -> QPointF:
        return self.scenePos() + QPointF(self.rect().width() / 2, self.rect().height() / 2)

    def paint(self, painter, option, widget=None) -> None:
        self.draw_shape(painter)
        if self.display_text():
            pass  # label é filho; retângulo já desenhado

    def draw_shape(self, painter) -> None:
        painter.setPen(self.pen())
        painter.setBrush(self.brush())
        painter.drawRect(self.rect())


class RectangularNodeItem(BaseNodeGraphicsItem):
    pass


class PackageNodeItem(BaseNodeGraphicsItem):
    def _apply_palette(self) -> None:
        super()._apply_palette()
        pal = self._palette()
        tint = QColor(240, 248, 255) if pal.bg.lightness() > 128 else QColor(50, 55, 65)
        self.setBrush(QBrush(tint))