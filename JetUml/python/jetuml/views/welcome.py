from __future__ import annotations

from PyQt5.QtCore import Qt, pyqtSignal
from PyQt5.QtWidgets import QGridLayout, QLabel, QPushButton, QVBoxLayout, QWidget

from jetuml.models.kinds import DiagramType


class WelcomeWidget(QWidget):
    new_diagram = pyqtSignal(object)

    def __init__(self, parent=None) -> None:
        super().__init__(parent)
        layout = QVBoxLayout(self)
        title = QLabel("JetUML")
        title.setAlignment(Qt.AlignCenter)
        title.setStyleSheet("font-size: 28px; font-weight: bold;")
        layout.addWidget(title)
        subtitle = QLabel("Escolha o tipo de diagrama para começar")
        subtitle.setAlignment(Qt.AlignCenter)
        layout.addWidget(subtitle)
        grid = QGridLayout()
        types = list(DiagramType)
        for i, dt in enumerate(types):
            btn = QPushButton(dt.label)
            btn.clicked.connect(lambda checked=False, t=dt: self.new_diagram.emit(t))
            grid.addWidget(btn, i // 2, i % 2)
        layout.addLayout(grid)
        layout.addStretch()