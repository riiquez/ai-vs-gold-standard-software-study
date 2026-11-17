from __future__ import annotations

from PyQt5.QtCore import Qt, QTimer
from PyQt5.QtWidgets import QLabel, QWidget


class NotificationBar(QWidget):
    def __init__(self, parent=None) -> None:
        super().__init__(parent)
        self.setFixedHeight(36)
        self._label = QLabel(self)
        self._label.setAlignment(Qt.AlignCenter)
        self._label.setFixedHeight(32)
        self._timer = QTimer(self)
        self._timer.setSingleShot(True)
        self._timer.timeout.connect(self._hide)
        self._duration_ms = 5000
        self.hide()
        self._label.setParent(parent if parent else self)

    def set_duration_seconds(self, seconds: int) -> None:
        self._duration_ms = max(0, seconds) * 1000

    def show_message(self, text: str, error: bool = False) -> None:
        if self._duration_ms == 0:
            return
        self._label.setText(text)
        color = "#8b0000" if error else "#006400"
        bg = "#ffdddd" if error else "#ddffdd"
        self._label.setStyleSheet(
            f"background:{bg}; color:{color}; padding:6px 12px; border-radius:4px;"
        )
        if self.parentWidget():
            self._label.move(8, self.parentWidget().height() - 48)
            self._label.resize(min(500, self.parentWidget().width() - 16), 32)
        self._label.show()
        self._timer.start(self._duration_ms)

    def _hide(self) -> None:
        self._label.hide()