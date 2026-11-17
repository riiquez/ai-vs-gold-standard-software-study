from __future__ import annotations

import re

from PyQt5.QtGui import QFont, QTextCharFormat


STEREO_PATTERN = re.compile(r"«([^»]*)»")
SEGMENT_PATTERN = re.compile(r"(/[^/]+/|_[^_]+_|[^/_]+)")


def insert_stereotype_delimiters(cursor_pos_fn):
    """Insere « » e retorna nova posição do cursor."""
    return "«»", cursor_pos_fn() - 1


def parse_segments(line: str) -> list[tuple[str, bool, bool]]:
    """Retorna lista (texto, italic, underline)."""
    result: list[tuple[str, bool, bool]] = []
    for m in SEGMENT_PATTERN.finditer(line):
        chunk = m.group(0)
        if chunk.startswith("/") and chunk.endswith("/") and len(chunk) > 2:
            result.append((chunk[1:-1], True, False))
        elif chunk.startswith("_") and chunk.endswith("_") and len(chunk) > 2:
            result.append((chunk[1:-1], False, True))
        else:
            result.append((chunk, False, False))
    return result


def apply_rich_line(format_fn, line: str, base_font: QFont) -> None:
    for text, italic, underline in parse_segments(line):
        fmt = QTextCharFormat()
        fmt.setFont(base_font)
        fmt.setFontItalic(italic)
        fmt.setFontUnderline(underline)
        format_fn(text, fmt)