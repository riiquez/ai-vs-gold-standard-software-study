from __future__ import annotations

from PyQt5.QtCore import Qt
from PyQt5.QtGui import QBrush, QFont, QPen
from PyQt5.QtWidgets import QGraphicsItem

from jetuml.models.kinds import NodeKind
from PyQt5.QtWidgets import QGraphicsSimpleTextItem

from jetuml.presentation.views.graphics.base_node_item import BaseNodeGraphicsItem, RectangularNodeItem
from jetuml.presentation.views.graphics.formatted_text import FormattedTextItem


class ClassNodeItem(BaseNodeGraphicsItem):
    """Notação UML de classe com compartimentos."""

    def _create_label_widgets(self) -> None:
        self.label = QGraphicsSimpleTextItem(self)
        self.label.hide()
        self._name = FormattedTextItem(self)
        self._attrs = FormattedTextItem(self)
        self._methods = FormattedTextItem(self)

    def refresh_label(self) -> None:
        pal = self._palette()
        p = self.node.properties
        self._name.set_formatted_text(p.get("name", "Class"), self.font, pal.fg)
        self._attrs.set_formatted_text(p.get("attributes", ""), self.font, pal.fg)
        self._methods.set_formatted_text(p.get("methods", ""), self.font, pal.fg)
        self._layout_labels()
        self._resize_from_content()

    def _layout_labels(self) -> None:
        y = 6.0
        self._name.setPos(8, y)
        y += self._name.boundingRect().height() + 4
        self._attrs.setPos(8, y)
        y += max(4, self._attrs.boundingRect().height())
        self._methods.setPos(8, y)

    def _resize_from_content(self) -> None:
        h = max(
            80.0,
            self._methods.pos().y() + self._methods.boundingRect().height() + 12,
        )
        w = max(160.0, self.node.width)
        self.node.height = h
        self.setRect(0, 0, w, h)

    def draw_shape(self, painter) -> None:
        r = self.rect()
        painter.setPen(self.pen())
        painter.setBrush(self.brush())
        painter.drawRect(r)
        pal = self._palette()
        pen = QPen(pal.fg, 1)
        painter.setPen(pen)
        y1 = self._name.pos().y() + self._name.boundingRect().height() + 4
        y2 = self._attrs.pos().y() + max(14, self._attrs.boundingRect().height())
        painter.drawLine(r.left(), y1, r.right(), y1)
        painter.drawLine(r.left(), y2, r.right(), y2)


class InterfaceNodeItem(ClassNodeItem):
    def draw_shape(self, painter) -> None:
        super().draw_shape(painter)
        r = self.rect()
        painter.drawText(int(r.left() + 6), int(r.top() + 12), "<<interface>>")


class LifelineNodeItem(BaseNodeGraphicsItem):
    def display_text(self) -> str:
        return self.node.properties.get("name", "")

    def refresh_label(self) -> None:
        super().refresh_label()
        self.label.setPos(4, 4)

    def draw_shape(self, painter) -> None:
        pal = self._palette()
        r = self.rect()
        head_h = 28.0
        painter.setPen(self.pen())
        painter.setBrush(self.brush())
        painter.drawRect(r.left(), r.top(), r.width(), head_h)
        cx = r.center().x()
        pen = QPen(pal.fg, 1, Qt.DashLine)
        painter.setPen(pen)
        painter.drawLine(cx, r.top() + head_h, cx, r.bottom())


class ActivationNodeItem(BaseNodeGraphicsItem):
    def display_text(self) -> str:
        return ""

    def draw_shape(self, painter) -> None:
        pal = self._palette()
        painter.setPen(QPen(pal.fg, 1.5))
        fill = QBrush(pal.bg)
        if self.node.properties.get("open_bottom"):
            painter.setBrush(QBrush(Qt.NoBrush))
        else:
            painter.setBrush(fill)
        painter.drawRect(self.rect())