from __future__ import annotations

from PyQt5.QtCore import Qt
from PyQt5.QtGui import QFont
from PyQt5.QtWidgets import QGraphicsTextItem

from jetuml.util.text_format import parse_segments


class FormattedTextItem(QGraphicsTextItem):
    """RF24: /itálico/ e _sublinhado_ em rótulos UML."""

    def set_formatted_text(self, text: str, base_font: QFont, color) -> None:
        self.setDefaultTextColor(color)
        self.setHtml(self._multiline_html(text or "", base_font, color))

    def _multiline_html(self, text: str, base_font: QFont, color) -> str:
        lines = (text or "").splitlines()
        blocks = []
        for line in lines:
            parts = []
            for segment, italic, underline in parse_segments(line):
                safe = segment.replace("&", "&amp;")
                open_t = "<i>" if italic else ""
                close_t = "</i>" if italic else ""
                u1 = "<u>" if underline else ""
                u2 = "</u>" if underline else ""
                parts.append(f"{u1}{open_t}{safe}{close_t}{u2}")
            blocks.append("".join(parts))
        fam = base_font.family()
        size = base_font.pointSize()
        return (
            f'<div style="font-family:{fam}; font-size:{size}pt; color:{color.name()}">'
            + "<br/>".join(blocks)
            + "</div>"
        )