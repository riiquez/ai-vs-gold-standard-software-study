from __future__ import annotations

from pathlib import Path

from PyQt5.QtCore import Qt
from PyQt5.QtWidgets import (
    QApplication,
    QDialog,
    QDialogButtonBox,
    QFileDialog,
    QFormLayout,
    QInputDialog,
    QMainWindow,
    QSpinBox,
    QTabWidget,
)

from jetuml.application.controllers import ApplicationController
from jetuml.application.tab_manager import TabManager
from jetuml.infrastructure.config import default_diagram_size
from jetuml.models.kinds import DiagramType
from jetuml.presentation.shell.menu_builder import MainMenuBuilder, MenuHandlers
from jetuml.views.diagram_tab import DiagramTab
from jetuml.views.export_utils import render_diagram_to_image, save_image
from jetuml.views.notifications import NotificationBar
from jetuml.views.welcome import WelcomeWidget


class MainWindow(QMainWindow):
    def __init__(self) -> None:
        super().__init__()
        self._app = ApplicationController()
        self.cfg = self._app.config
        self.setWindowTitle("JetUML")
        screen = QApplication.primaryScreen().availableGeometry()
        dw, dh = default_diagram_size(screen.width(), screen.height())
        if self.cfg.get("diagram_width", 0) < 250:
            self.cfg["diagram_width"], self.cfg["diagram_height"] = dw, dh

        self._tabs = QTabWidget()
        self._tabs.setTabsClosable(True)
        self._tabs.tabCloseRequested.connect(self._close_tab)
        self.setCentralWidget(self._tabs)

        self._welcome = WelcomeWidget()
        self._welcome.new_diagram.connect(self._new_diagram)
        self._tabs_mgr = TabManager(self._tabs, self._welcome)
        self._tabs_mgr.set_notify_handler(self._notify)
        self._tabs_mgr.ensure_welcome()

        self._notifier = NotificationBar()
        self._notifier._label.setParent(self)

        self._menus = MainMenuBuilder(self, self._menu_handlers())
        self._menus.build()
        if self._menus.dark_action:
            self._menus.dark_action.setChecked(self.cfg.get("dark_mode", False))
        self._refresh_recent()

        self.resize(1200, 800)
        self._apply_dark(self.cfg.get("dark_mode", False))

    def _menu_handlers(self) -> MenuHandlers:
        return MenuHandlers(
            new_diagram=self._show_new_type_dialog,
            open_file=self._open_file,
            save=lambda: self._save(False),
            save_as=lambda: self._save(True),
            close_tab=lambda: self._tabs.tabCloseRequested.emit(self._tabs.currentIndex()),
            duplicate=self._duplicate,
            export_image=self._export_image,
            copy_clipboard=self._copy_clipboard,
            exit_app=self.close,
            undo=self._undo,
            redo=self._redo,
            properties=self._properties,
            select_all=self._select_all,
            cut=lambda: self._clipboard("cut"),
            copy=lambda: self._clipboard("copy"),
            paste=lambda: self._clipboard("paste"),
            delete=self._delete,
            toggle_grid=self._toggle_grid,
            toggle_tool_hint=self._toggle_tool_hint,
            toggle_verbose=self._toggle_verbose,
            toggle_auto_edit=self._toggle_auto_edit,
            zoom_in=self._zoom_in,
            zoom_out=self._zoom_out,
            zoom_reset=self._zoom_reset,
            set_diagram_size=self._set_diagram_size,
            change_font=self._change_font,
            toggle_dark=self._toggle_dark,
            notification_duration=self._notification_duration,
        )

    def _current_tab(self) -> DiagramTab | None:
        return self._tabs_mgr.current_tab()

    def _new_diagram(self, dtype: DiagramType) -> None:
        tab = self._tabs_mgr.add_diagram(
            self._app.new_diagram(dtype),
            "Novo diagrama",
            self._make_tab,
        )
        self._apply_tab_prefs(tab)

    def _make_tab(self, diagram) -> DiagramTab:
        tab = DiagramTab(diagram)
        tab.title_changed.connect(lambda t, ti=tab: self._tabs_mgr.update_title(ti, t))
        return tab

    def _apply_tab_prefs(self, tab: DiagramTab) -> None:
        tab.apply_view_settings(
            None,
            self.cfg.get("font_family"),
            int(self.cfg.get("font_size", 10)),
            self.cfg.get("dark_mode"),
            self.cfg.get("auto_edit_node"),
            self.cfg.get("show_tool_hint", False),
            self.cfg.get("verbose_tooltips", False),
        )

    def _duplicate(self) -> None:
        tab = self._current_tab()
        if not tab:
            return
        dup = self._app.duplicate_diagram(tab.diagram)
        new_tab = self._tabs_mgr.add_diagram(dup, "Cópia", self._make_tab)
        self._apply_tab_prefs(new_tab)

    def _open_file(self) -> None:
        path, _ = QFileDialog.getOpenFileName(self, "Abrir", "", "JetUML JSON (*.json *.jetuml.json)")
        if not path:
            return
        try:
            d = self._app.open_diagram(path)
            tab = self._tabs_mgr.add_diagram(d, Path(path).name, self._make_tab)
            self._apply_tab_prefs(tab)
            self._refresh_recent()
        except Exception as ex:
            self._notify(f"Erro ao abrir: {ex}", True)

    def _save(self, save_as: bool = False) -> None:
        tab = self._current_tab()
        if not tab:
            return
        path = tab.diagram.file_path
        if save_as or not path:
            path, _ = QFileDialog.getSaveFileName(self, "Salvar", "", "JetUML JSON (*.json)")
            if not path:
                return
        try:
            self._app.save_diagram(tab.diagram, path)
            tab.title_changed.emit(path)
            self._refresh_recent()
            self._notify("Diagrama salvo.", False)
        except Exception as ex:
            self._notify(f"Erro ao salvar: {ex}", True)

    def _close_tab(self, index: int) -> None:
        def save_tab(t: DiagramTab) -> None:
            self._tabs.setCurrentWidget(t)
            self._save(False)

        if not self._tabs_mgr.close_index(index, save_tab):
            return

    def _export_image(self) -> None:
        tab = self._current_tab()
        if not tab:
            return
        path, _ = QFileDialog.getSaveFileName(self, "Exportar imagem", "", "Imagens (*.png *.jpg *.jpeg *.svg)")
        if not path:
            return
        img = render_diagram_to_image(tab.diagram, hide_grid=True, content_bounds_only=False)
        save_image(img, path, tab.diagram)
        self._notify("Imagem exportada.", False)

    def _notify(self, msg: str, error: bool) -> None:
        self._notifier.set_duration_seconds(int(self.cfg.get("notification_seconds", 5)))
        self._notifier.show_message(msg, error)

    def _refresh_recent(self) -> None:
        if not self._menus.recent_menu:
            return
        self._menus.recent_menu.clear()
        from PyQt5.QtWidgets import QAction

        for path in self.cfg.get("recent_files", []):
            act = QAction(path, self)
            act.triggered.connect(lambda checked=False, p=path: self._open_recent(p))
            self._menus.recent_menu.addAction(act)

    def _open_recent(self, path: str) -> None:
        if not Path(path).exists():
            self._notify("Arquivo não encontrado.", True)
            return
        d = self._app.open_diagram(path)
        tab = self._tabs_mgr.add_diagram(d, Path(path).name, self._make_tab)
        self._apply_tab_prefs(tab)

    def _show_new_type_dialog(self) -> None:
        items = [dt.label for dt in DiagramType]
        choice, ok = QInputDialog.getItem(self, "Novo diagrama", "Tipo:", items, 0, False)
        if ok and choice:
            for dt in DiagramType:
                if dt.label == choice:
                    self._new_diagram(dt)
                    break

    def _undo(self) -> None:
        tab = self._current_tab()
        if tab:
            tab.undo()

    def _redo(self) -> None:
        tab = self._current_tab()
        if tab:
            tab.redo()

    def _properties(self) -> None:
        tab = self._current_tab()
        if tab:
            tab.open_properties_selection()

    def _select_all(self) -> None:
        tab = self._current_tab()
        if tab:
            tab.select_all()

    def _clipboard(self, mode: str) -> None:
        tab = self._current_tab()
        if tab:
            tab.cut_copy_paste(mode)

    def _delete(self) -> None:
        tab = self._current_tab()
        if tab:
            tab.delete_selected()

    def _copy_clipboard(self) -> None:
        tab = self._current_tab()
        if tab:
            tab.copy_diagram_image()

    def _toggle_grid(self) -> None:
        tab = self._current_tab()
        if tab:
            vis = not tab.diagram.settings.grid_visible
            tab.apply_view_settings(vis, None, None, None, None, self.cfg["show_tool_hint"], self.cfg["verbose_tooltips"])

    def _toggle_tool_hint(self) -> None:
        self.cfg["show_tool_hint"] = not self.cfg.get("show_tool_hint", False)
        self._app.persist_config()
        tab = self._current_tab()
        if tab:
            tab.apply_view_settings(None, None, None, None, None, self.cfg["show_tool_hint"], self.cfg["verbose_tooltips"])

    def _toggle_verbose(self) -> None:
        self.cfg["verbose_tooltips"] = not self.cfg.get("verbose_tooltips", False)
        self._app.persist_config()
        tab = self._current_tab()
        if tab:
            tab.apply_view_settings(None, None, None, None, None, self.cfg["show_tool_hint"], self.cfg["verbose_tooltips"])

    def _toggle_auto_edit(self) -> None:
        self.cfg["auto_edit_node"] = not self.cfg.get("auto_edit_node", False)
        self._app.persist_config()
        tab = self._current_tab()
        if tab:
            tab.apply_view_settings(None, None, None, None, self.cfg["auto_edit_node"], self.cfg["show_tool_hint"], self.cfg["verbose_tooltips"])

    def _zoom_in(self) -> None:
        tab = self._current_tab()
        if tab:
            tab.canvas.zoom_in()

    def _zoom_out(self) -> None:
        tab = self._current_tab()
        if tab:
            tab.canvas.zoom_out()

    def _zoom_reset(self) -> None:
        tab = self._current_tab()
        if tab:
            tab.canvas.reset_zoom()

    def _set_diagram_size(self) -> None:
        dlg = QDialog(self)
        dlg.setWindowTitle("Tamanho do diagrama")
        form = QFormLayout(dlg)
        w = QSpinBox()
        w.setRange(250, 4000)
        w.setValue(self.cfg["diagram_width"])
        h = QSpinBox()
        h.setRange(250, 4000)
        h.setValue(self.cfg["diagram_height"])
        form.addRow("Largura", w)
        form.addRow("Altura", h)
        buttons = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.RestoreDefaults)
        form.addRow(buttons)

        def restore():
            sw, sh = default_diagram_size(
                QApplication.primaryScreen().availableGeometry().width(),
                QApplication.primaryScreen().availableGeometry().height(),
            )
            w.setValue(sw)
            h.setValue(sh)

        buttons.clicked.connect(lambda b: restore() if b == buttons.button(QDialogButtonBox.RestoreDefaults) else None)
        buttons.accepted.connect(dlg.accept)
        if dlg.exec_():
            self.cfg["diagram_width"] = w.value()
            self.cfg["diagram_height"] = h.value()
            self._app.persist_config()
            tab = self._current_tab()
            if tab:
                tab.diagram.width = w.value()
                tab.diagram.height = h.value()
                tab.scene.rebuild()

    def _change_font(self) -> None:
        from PyQt5.QtGui import QFontDialog

        font, ok = QFontDialog.getFont()
        if ok:
            self.cfg["font_family"] = font.family()
            self.cfg["font_size"] = font.pointSize()
            self._app.persist_config()
            tab = self._current_tab()
            if tab:
                tab.apply_view_settings(None, font.family(), font.pointSize(), None, None, self.cfg["show_tool_hint"], self.cfg["verbose_tooltips"])

    def _toggle_dark(self) -> None:
        self.cfg["dark_mode"] = not self.cfg.get("dark_mode", False)
        self._app.persist_config()
        self._apply_dark(self.cfg["dark_mode"])
        tab = self._current_tab()
        if tab:
            tab.apply_view_settings(None, None, None, self.cfg["dark_mode"], None, self.cfg["show_tool_hint"], self.cfg["verbose_tooltips"])

    def _apply_dark(self, enabled: bool) -> None:
        if enabled:
            self.setStyleSheet("QMainWindow { background: #2d2d30; color: #ddd; }")
        else:
            self.setStyleSheet("")

    def _notification_duration(self) -> None:
        sec, ok = QInputDialog.getInt(
            self, "Duração", "Segundos (0-10, 0 desativa):", int(self.cfg.get("notification_seconds", 5)), 0, 10
        )
        if ok:
            self.cfg["notification_seconds"] = sec
            self._app.persist_config()

    def keyPressEvent(self, event) -> None:
        text = event.text()
        if text and text.isalnum():
            tab = self._current_tab()
            if tab:
                tab.activate_tool_shortcut(text)
        super().keyPressEvent(event)

    def resizeEvent(self, event) -> None:
        super().resizeEvent(event)
        self._notifier._label.move(8, self.height() - 48)