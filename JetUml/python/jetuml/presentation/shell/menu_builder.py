from __future__ import annotations

from dataclasses import dataclass
from typing import Callable

from PyQt5.QtGui import QKeySequence
from PyQt5.QtWidgets import QAction, QMenu, QWidget


@dataclass
class MenuHandlers:
    new_diagram: Callable[[], None]
    open_file: Callable[[], None]
    save: Callable[[], None]
    save_as: Callable[[], None]
    close_tab: Callable[[], None]
    duplicate: Callable[[], None]
    export_image: Callable[[], None]
    copy_clipboard: Callable[[], None]
    exit_app: Callable[[], None]
    undo: Callable[[], None]
    redo: Callable[[], None]
    properties: Callable[[], None]
    select_all: Callable[[], None]
    cut: Callable[[], None]
    copy: Callable[[], None]
    paste: Callable[[], None]
    delete: Callable[[], None]
    toggle_grid: Callable[[], None]
    toggle_tool_hint: Callable[[], None]
    toggle_verbose: Callable[[], None]
    toggle_auto_edit: Callable[[], None]
    zoom_in: Callable[[], None]
    zoom_out: Callable[[], None]
    zoom_reset: Callable[[], None]
    set_diagram_size: Callable[[], None]
    change_font: Callable[[], None]
    toggle_dark: Callable[[], None]
    notification_duration: Callable[[], None]


class MainMenuBuilder:
    """Builder: monta menus sem acoplar lógica à MainWindow."""

    def __init__(self, parent: QWidget, handlers: MenuHandlers) -> None:
        self._parent = parent
        self._h = handlers
        self.recent_menu: QMenu | None = None
        self.grid_action: QAction | None = None
        self.dark_action: QAction | None = None

    def build(self) -> None:
        from PyQt5.QtWidgets import QMainWindow

        win = self._parent
        if not isinstance(win, QMainWindow):
            return
        mb = win.menuBar()
        file_m = mb.addMenu("&File")
        self._act(file_m, "New", QKeySequence.New, self._h.new_diagram)
        self._act(file_m, "Open...", QKeySequence.Open, self._h.open_file)
        self.recent_menu = file_m.addMenu("Recent Files")
        self._act(file_m, "Save", QKeySequence.Save, self._h.save)
        self._act(file_m, "Save As...", QKeySequence.SaveAs, self._h.save_as)
        self._act(file_m, "Close", QKeySequence("Ctrl+W"), self._h.close_tab)
        self._act(file_m, "Duplicate", QKeySequence("Ctrl+D"), self._h.duplicate)
        self._act(file_m, "Export Image...", None, self._h.export_image)
        self._act(file_m, "Copy to Clipboard", QKeySequence("Ctrl+B"), self._h.copy_clipboard)
        self._act(file_m, "Exit", QKeySequence("Ctrl+Q"), self._h.exit_app)

        edit_m = mb.addMenu("&Edit")
        self._act(edit_m, "Undo", QKeySequence.Undo, self._h.undo)
        self._act(edit_m, "Redo", QKeySequence.Redo, self._h.redo)
        self._act(edit_m, "Properties", QKeySequence("Ctrl+Return"), self._h.properties)
        self._act(edit_m, "Select All", QKeySequence.SelectAll, self._h.select_all)
        self._act(edit_m, "Cut", QKeySequence.Cut, self._h.cut)
        self._act(edit_m, "Copy", QKeySequence.Copy, self._h.copy)
        self._act(edit_m, "Paste", QKeySequence.Paste, self._h.paste)
        self._act(edit_m, "Delete", QKeySequence.Delete, self._h.delete)

        view_m = mb.addMenu("&View")
        self.grid_action = self._act(view_m, "Show Grid", None, self._h.toggle_grid, checkable=True, checked=True)
        self._act(view_m, "Show Tool Hint", None, self._h.toggle_tool_hint, checkable=True)
        self._act(view_m, "Verbose Tooltips", None, self._h.toggle_verbose, checkable=True)
        self._act(view_m, "Auto Edit Node", None, self._h.toggle_auto_edit, checkable=True)
        self._act(view_m, "Zoom In", QKeySequence.ZoomIn, self._h.zoom_in)
        self._act(view_m, "Zoom Out", QKeySequence.ZoomOut, self._h.zoom_out)
        self._act(view_m, "Reset Zoom", QKeySequence("Ctrl+0"), self._h.zoom_reset)
        self._act(view_m, "Set Diagram Size...", None, self._h.set_diagram_size)
        self._act(view_m, "Change Font...", None, self._h.change_font)
        self.dark_action = self._act(
            view_m, "Dark Mode", None, self._h.toggle_dark, checkable=True, checked=False
        )
        self._act(view_m, "Notification Duration...", None, self._h.notification_duration)

    def _act(self, menu, text, shortcut, slot, checkable=False, checked=False) -> QAction:
        a = QAction(text, self._parent)
        if shortcut:
            a.setShortcut(shortcut)
        a.setCheckable(checkable)
        if checkable:
            a.setChecked(checked)
        a.triggered.connect(slot)
        menu.addAction(a)
        return a