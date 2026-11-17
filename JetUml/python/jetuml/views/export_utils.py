from __future__ import annotations

import io

from PyQt5.QtCore import QRectF, Qt
from PyQt5.QtGui import QImage, QPainter
from PyQt5.QtWidgets import QApplication, QGraphicsScene

from jetuml.models.diagram import Diagram
from jetuml.views.diagram_scene import DiagramScene


def render_diagram_to_image(
    diagram: Diagram, hide_grid: bool = False, content_bounds_only: bool = False
) -> QImage:
    prev_vis = diagram.settings.grid_visible
    if hide_grid:
        diagram.settings.grid_visible = False
    scene = DiagramScene(diagram)
    if content_bounds_only and diagram.nodes:
        min_x, min_y, max_x, max_y = diagram.content_bounds()
        margin = 20
        rect = QRectF(min_x - margin, min_y - margin, max_x - min_x + 2 * margin, max_y - min_y + 2 * margin)
    else:
        rect = QRectF(0, 0, diagram.width, diagram.height)
    img = QImage(int(rect.width()), int(rect.height()), QImage.Format_ARGB32)
    img.fill(Qt.white)
    painter = QPainter(img)
    scene.render(painter, target=QRectF(0, 0, rect.width(), rect.height()), source=rect)
    painter.end()
    diagram.settings.grid_visible = prev_vis
    return img


def image_to_clipboard(img: QImage) -> None:
    QApplication.clipboard().setImage(img)


def save_image(img: QImage, path: str, diagram: Diagram | None = None) -> None:
    lower = path.lower()
    if lower.endswith(".svg") and diagram is not None:
        from PyQt5.QtSvg import QSvgGenerator

        gen = QSvgGenerator()
        gen.setFileName(path)
        gen.setSize(img.size())
        gen.setViewBox(QRectF(0, 0, img.width(), img.height()))
        scene = DiagramScene(diagram)
        painter = QPainter(gen)
        scene.render(painter)
        painter.end()
        return
    fmt = "PNG"
    if lower.endswith(".jpg") or lower.endswith(".jpeg"):
        fmt = "JPEG"
    img.save(path, fmt)