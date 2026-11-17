from __future__ import annotations

import sys

from PyQt5.QtWidgets import QApplication

from jetuml.mainwindow import MainWindow


class JetUmlApplication:
    """Facade: ponto único de bootstrap da aplicação."""

    @staticmethod
    def run(argv: list[str] | None = None) -> int:
        args = argv if argv is not None else sys.argv
        app = QApplication(args)
        app.setApplicationName("JetUML")
        app.setOrganizationName("JetUML")
        window = MainWindow()
        window.show()
        return app.exec_()