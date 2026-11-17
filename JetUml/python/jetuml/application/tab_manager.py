from __future__ import annotations

from pathlib import Path
from typing import Callable

from PyQt5.QtWidgets import QMessageBox, QTabWidget

from jetuml.models.diagram import Diagram
from jetuml.views.diagram_tab import DiagramTab
from jetuml.views.welcome import WelcomeWidget


class TabManager:
    """Composição: gerencia abas de diagrama e tela de boas-vindas."""

    def __init__(self, tabs: QTabWidget, welcome: WelcomeWidget) -> None:
        self._tabs = tabs
        self._welcome = welcome
        self._on_notify: Callable[[str, bool], None] | None = None

    def set_notify_handler(self, handler: Callable[[str, bool], None]) -> None:
        self._on_notify = handler

    def ensure_welcome(self) -> None:
        if self._tabs.count() == 0:
            self._tabs.addTab(self._welcome, "Bem-vindo")

    def current_tab(self) -> DiagramTab | None:
        w = self._tabs.currentWidget()
        return w if isinstance(w, DiagramTab) else None

    def add_diagram(self, diagram: Diagram, title: str, factory: Callable[[Diagram], DiagramTab]) -> DiagramTab:
        if isinstance(self._tabs.currentWidget(), WelcomeWidget):
            self._tabs.removeTab(0)
        tab = factory(diagram)
        if self._on_notify:
            tab.notify.connect(self._on_notify)
        idx = self._tabs.addTab(tab, title[:40])
        self._tabs.setCurrentIndex(idx)
        return tab

    def update_title(self, tab: DiagramTab, title: str) -> None:
        idx = self._tabs.indexOf(tab)
        if idx < 0:
            return
        name = Path(title).name if ("/" in title or "\\" in title) and not title.endswith(" *") else title
        self._tabs.setTabText(idx, name[:40])

    def close_index(self, index: int, save_prompt: Callable[[DiagramTab], bool]) -> bool:
        w = self._tabs.widget(index)
        if isinstance(w, DiagramTab) and w.diagram.dirty:
            r = QMessageBox.question(
                None,
                "Fechar",
                "Salvar alterações?",
                QMessageBox.Yes | QMessageBox.No | QMessageBox.Cancel,
            )
            if r == QMessageBox.Cancel:
                return False
            if r == QMessageBox.Yes:
                self._tabs.setCurrentIndex(index)
                save_prompt(w)
        self._tabs.removeTab(index)
        if self._tabs.count() == 0:
            self.ensure_welcome()
        return True