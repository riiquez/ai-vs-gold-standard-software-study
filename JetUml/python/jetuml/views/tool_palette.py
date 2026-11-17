from __future__ import annotations

from PyQt5.QtCore import QSize, Qt, pyqtSignal
from PyQt5.QtWidgets import QListWidget, QListWidgetItem, QWidget, QVBoxLayout

from jetuml.models.kinds import TOOL_SHORTCUT_KEYS, TOOLS_BY_DIAGRAM, VERBOSE_TOOLTIPS, DiagramType


class ToolPalette(QWidget):
    tool_selected = pyqtSignal(object)  # None for select, str kind, or __clipboard__

    def __init__(self, diagram_type: DiagramType, parent=None) -> None:
        super().__init__(parent)
        self._diagram_type = diagram_type
        self._show_hint = False
        self._verbose = False
        layout = QVBoxLayout(self)
        self.list = QListWidget()
        self.list.setMaximumWidth(200)
        layout.addWidget(self.list)
        self.list.currentRowChanged.connect(self._on_row)
        self.rebuild()

    def set_hints(self, show_hint: bool, verbose: bool) -> None:
        self._show_hint = show_hint
        self._verbose = verbose
        self.rebuild()

    def rebuild(self) -> None:
        self.list.clear()
        tools = TOOLS_BY_DIAGRAM[self._diagram_type]
        for i, (name, kind) in enumerate(tools):
            label = name
            if self._show_hint:
                key = TOOL_SHORTCUT_KEYS[i] if i < len(TOOL_SHORTCUT_KEYS) else "?"
                label = f"{key} {name}"
            item = QListWidgetItem(label)
            item.setData(Qt.UserRole, kind)
            tip = VERBOSE_TOOLTIPS.get(name, name) if self._verbose else name
            item.setToolTip(tip)
            self.list.addItem(item)
        self.list.setCurrentRow(0)

    def _on_row(self, row: int) -> None:
        if row < 0:
            return
        item = self.list.item(row)
        kind = item.data(Qt.UserRole)
        self.tool_selected.emit(kind)

    def activate_shortcut_index(self, index: int) -> None:
        if 0 <= index < self.list.count():
            self.list.setCurrentRow(index)

    def set_diagram_type(self, dt: DiagramType) -> None:
        self._diagram_type = dt
        self.rebuild()