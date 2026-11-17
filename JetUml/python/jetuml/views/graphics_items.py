from __future__ import annotations

import math

from PyQt5.QtCore import QPointF, Qt
from PyQt5.QtGui import QBrush, QFont, QPainterPath, QPen
from PyQt5.QtWidgets import QGraphicsPathItem, QGraphicsRectItem, QGraphicsSimpleTextItem

from jetuml.presentation.views.graphics.factory import NodeGraphicsFactory
from jetuml.models.elements import DiagramEdge, DiagramNode
from jetuml.presentation.theme import theme_manager
from jetuml.presentation.views.graphics.base_node_item import BaseNodeGraphicsItem

# Compatibilidade: NodeItem é a base usada nas views
NodeItem = BaseNodeGraphicsItem


def set_dark_mode(enabled: bool) -> None:
    theme_manager().set_dark(enabled)


def create_node_item(node: DiagramNode, font: QFont) -> BaseNodeGraphicsItem:
    return NodeGraphicsFactory.create(node, font)


def _pal():
    return theme_manager().current().palette()


class EdgeItem(QGraphicsPathItem):
    def __init__(self, edge: DiagramEdge, src: BaseNodeGraphicsItem | None, tgt: BaseNodeGraphicsItem | None) -> None:
        super().__init__()
        self.edge = edge
        self.src_item = src
        self.tgt_item = tgt
        pal = _pal()
        self.setPen(QPen(pal.edge, 1.5))
        self.setZValue(-1)
        self.label = QGraphicsSimpleTextItem(self)
        self.label.setBrush(pal.fg)
        self._update_path()

    def _update_path(self) -> None:
        path = QPainterPath()
        if self.src_item and self.tgt_item:
            p1 = self.src_item.center_scene_pos()
            p2 = self.tgt_item.center_scene_pos()
            path = _orthogonal_path(p1, p2)
        elif self.edge.target_point and self.src_item:
            p1 = self.src_item.center_scene_pos()
            p2 = QPointF(*self.edge.target_point)
            path = _orthogonal_path(p1, p2)
        self.setPath(path)
        mid = path.pointAtPercent(0.5)
        self.label.setText(self.edge.properties.get("middle_label", ""))
        self.label.setPos(mid)

    def refresh(self) -> None:
        pal = _pal()
        self.setPen(QPen(pal.edge, 1.5))
        self.label.setBrush(pal.fg)
        self._update_path()

    def paint(self, painter, option, widget=None):
        super().paint(painter, option, widget)
        self._draw_arrow(painter)

    def _draw_arrow(self, painter) -> None:
        if not self.src_item or not self.tgt_item:
            return
        p1 = self.src_item.center_scene_pos()
        p2 = self.tgt_item.center_scene_pos()
        k = self.edge.kind
        pal = _pal()
        if k in ("inheritance", "realization", "generalization"):
            _draw_triangle(painter, p2, p1, hollow=k != "inheritance", pal=pal)
        elif k in ("composition", "aggregation"):
            _draw_diamond(painter, p1, p2, filled=k == "composition", pal=pal)
        elif k in ("dependency", "include", "extend"):
            _draw_arrow_head(painter, p1, p2, pal=pal, dashed=True)


def _orthogonal_path(p1: QPointF, p2: QPointF) -> QPainterPath:
    path = QPainterPath()
    path.moveTo(p1)
    mid_x = (p1.x() + p2.x()) / 2
    path.lineTo(mid_x, p1.y())
    path.lineTo(mid_x, p2.y())
    path.lineTo(p2)
    return path


def _draw_arrow_head(painter, p1: QPointF, p2: QPointF, pal, dashed: bool = False) -> None:
    angle = math.atan2(p2.y() - p1.y(), p2.x() - p1.x())
    sz = 10
    a1 = angle + math.pi / 6
    a2 = angle - math.pi / 6
    tip = p2
    p_a = QPointF(tip.x() - sz * math.cos(a1), tip.y() - sz * math.sin(a1))
    p_b = QPointF(tip.x() - sz * math.cos(a2), tip.y() - sz * math.sin(a2))
    pen = QPen(pal.edge, 1.5)
    if dashed:
        pen.setStyle(Qt.DashLine)
    painter.setPen(pen)
    painter.setBrush(QBrush(Qt.NoBrush))
    painter.drawLine(tip, p_a)
    painter.drawLine(tip, p_b)


def _draw_triangle(painter, tip: QPointF, from_pt: QPointF, hollow: bool, pal) -> None:
    angle = math.atan2(tip.y() - from_pt.y(), tip.x() - from_pt.x())
    sz = 14
    base = QPointF(tip.x() - sz * math.cos(angle), tip.y() - sz * math.sin(angle))
    perp = angle + math.pi / 2
    w = 7
    p1 = QPointF(base.x() + w * math.cos(perp), base.y() + w * math.sin(perp))
    p2 = QPointF(base.x() - w * math.cos(perp), base.y() - w * math.sin(perp))
    path = QPainterPath()
    path.moveTo(tip)
    path.lineTo(p1)
    path.lineTo(p2)
    path.closeSubpath()
    painter.setPen(QPen(pal.edge, 1.5))
    painter.setBrush(Qt.NoBrush if hollow else QBrush(pal.bg))
    painter.drawPath(path)


def _draw_diamond(painter, near: QPointF, far: QPointF, filled: bool, pal) -> None:
    angle = math.atan2(far.y() - near.y(), far.x() - near.x())
    sz = 12
    cx = near.x() + sz * math.cos(angle)
    cy = near.y() + sz * math.sin(angle)
    perp = angle + math.pi / 2
    w = 6
    p1 = QPointF(cx + w * math.cos(perp), cy + w * math.sin(perp))
    p2 = QPointF(cx - w * math.cos(perp), cy - w * math.sin(perp))
    p3 = QPointF(near.x(), near.y())
    p4 = QPointF(cx + (cx - near.x()), cy + (cy - near.y()))
    path = QPainterPath()
    path.moveTo(p3)
    path.lineTo(p1)
    path.lineTo(p4)
    path.lineTo(p2)
    path.closeSubpath()
    painter.setPen(QPen(pal.edge, 1.5))
    painter.setBrush(QBrush(pal.edge) if filled else Qt.NoBrush)
    painter.drawPath(path)


class GridItem(QGraphicsRectItem):
    def __init__(self, width: int, height: int, step: int, visible: bool) -> None:
        super().__init__(0, 0, width, height)
        self.step = step
        self.visible = visible
        self.setZValue(-100)
        pal = _pal()
        self.setBrush(QBrush(pal.bg))
        self.setPen(QPen(Qt.NoPen))

    def paint(self, painter, option, widget=None):
        super().paint(painter, option, widget)
        if not self.visible:
            return
        pal = _pal()
        painter.setPen(QPen(pal.grid, 0.5))
        r = self.rect()
        max_w, max_h = int(r.width()), int(r.height())
        x = 0
        while x <= max_w:
            painter.drawLine(x, 0, x, max_h)
            x += self.step
        y = 0
        while y <= max_h:
            painter.drawLine(0, y, max_w, y)
            y += self.step