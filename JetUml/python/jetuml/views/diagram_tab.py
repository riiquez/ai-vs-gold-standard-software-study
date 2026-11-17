from __future__ import annotations

from PyQt5.QtCore import Qt, pyqtSignal
from PyQt5.QtWidgets import (
    QHBoxLayout,
    QMenu,
    QWidget,
)

from jetuml.application.services.clipboard_service import ClipboardService
from jetuml.models.diagram import Diagram
from jetuml.models.kinds import TOOL_SHORTCUT_KEYS
from jetuml.views.diagram_canvas import DiagramCanvas
from jetuml.views.diagram_scene import DiagramScene
from jetuml.views.export_utils import image_to_clipboard, render_diagram_to_image
from jetuml.views.properties_dialog import PropertiesDialog
from jetuml.views.tool_palette import ToolPalette


class DiagramTab(QWidget):
    title_changed = pyqtSignal(str)
    notify = pyqtSignal(str, bool)

    def __init__(self, diagram: Diagram, parent=None) -> None:
        super().__init__(parent)
        self.diagram = diagram
        self._clipboard = ClipboardService()
        self.scene = DiagramScene(diagram)
        self.presenter = self.scene.presenter
        self.canvas = DiagramCanvas(self.scene)
        self.palette = ToolPalette(diagram.diagram_type)
        layout = QHBoxLayout(self)
        layout.addWidget(self.palette)
        layout.addWidget(self.canvas, 1)
        self.setLayout(layout)
        self._wire()

    def _wire(self) -> None:
        self.palette.tool_selected.connect(self._on_tool)
        self.scene.notify.connect(self.notify.emit)
        self.scene.request_properties.connect(self._open_properties)
        self.scene.model_changed.connect(self._on_dirty)
        self.canvas.copy_image_requested.connect(self.copy_diagram_image)
        self.canvas.setContextMenuPolicy(Qt.CustomContextMenu)
        self.canvas.customContextMenuRequested.connect(self._context_menu)

    def _on_tool(self, kind) -> None:
        if kind is None:
            self.scene.active_tool = None
        elif kind == "__clipboard__":
            self.copy_diagram_image()
        else:
            self.scene.active_tool = kind
        self.canvas.setDragMode(
            QGraphicsView.RubberBandDrag if kind is None else QGraphicsView.NoDrag
        )

    def _on_dirty(self) -> None:
        self.diagram.dirty = True
        name = self.diagram.file_path or f"Sem título ({self.diagram.diagram_type.label})"
        suffix = " *" if self.diagram.dirty else ""
        self.title_changed.emit(name + suffix)

    def _open_properties(self, eid: str, is_edge: bool) -> None:
        dlg = PropertiesDialog(self.diagram, self.scene.history, eid, is_edge, self)
        if dlg.exec_():
            self.scene.rebuild()

    def open_properties_selection(self) -> None:
        for item in self.scene.selectedItems():
            from jetuml.views.graphics_items import EdgeItem, NodeItem

            if isinstance(item, NodeItem):
                self._open_properties(item.node.id, False)
                return
            if isinstance(item, EdgeItem):
                self._open_properties(item.edge.id, True)
                return

    def _context_menu(self, pos) -> None:
        menu = QMenu(self)
        for i in range(self.palette.list.count()):
            item = self.palette.list.item(i)
            act = menu.addAction(item.text())
            act.triggered.connect(lambda checked=False, row=i: self.palette.list.setCurrentRow(row))
        menu.exec_(self.canvas.mapToGlobal(pos))

    def copy_diagram_image(self) -> None:
        img = render_diagram_to_image(self.diagram, hide_grid=True, content_bounds_only=True)
        image_to_clipboard(img)
        self.notify.emit("Diagrama copiado para a área de transferência.", False)

    def undo(self) -> None:
        if self.scene.history.undo():
            self.scene.rebuild()
            self._on_dirty()

    def redo(self) -> None:
        if self.scene.history.redo():
            self.scene.rebuild()
            self._on_dirty()

    def delete_selected(self) -> None:
        self.scene.delete_selection()

    def select_all(self) -> None:
        self.scene.select_all()

    def cut_copy_paste(self, mode: str) -> None:
        from jetuml.views.graphics_items import EdgeItem, NodeItem

        if mode in ("copy", "cut"):
            nodes, edges = [], []
            for item in self.scene.selectedItems():
                if isinstance(item, NodeItem):
                    nodes.append(item.node)
                elif isinstance(item, EdgeItem):
                    edges.append(item.edge)
            self._clipboard.cut_or_copy(nodes, edges, cut=(mode == "cut"))
            if mode == "cut":
                self.scene.delete_selection()

        if mode == "paste":
            self._clipboard.paste(
                self.diagram,
                self.scene.history,
                lambda err: self.notify.emit(err, True),
                lambda: (self.scene.rebuild(), self._on_dirty()),
            )

    def activate_tool_shortcut(self, ch: str) -> None:
        ch = ch.upper()
        if ch in TOOL_SHORTCUT_KEYS:
            self.palette.activate_shortcut_index(TOOL_SHORTCUT_KEYS.index(ch))

    def apply_view_settings(
        self,
        grid: bool | None,
        font_family: str | None,
        font_size: int | None,
        dark: bool | None,
        auto_edit: bool | None,
        show_hint: bool,
        verbose: bool,
    ) -> None:
        if grid is not None:
            self.diagram.settings.grid_visible = grid
            self.scene.rebuild()
        if font_family and font_size:
            self.diagram.settings.font_family = font_family
            self.diagram.settings.font_size = font_size
            self.scene.set_font(font_family, font_size)
        if dark is not None:
            self.scene.set_dark(dark)
        if auto_edit is not None:
            self.scene.presenter.sync_auto_edit(auto_edit)
        self.palette.set_hints(show_hint, verbose)


from PyQt5.QtWidgets import QGraphicsView