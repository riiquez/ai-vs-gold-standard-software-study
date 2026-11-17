from __future__ import annotations

from abc import ABC, abstractmethod

from PyQt5.QtGui import QColor


class ThemePalette:
    def __init__(self, bg: QColor, fg: QColor, edge: QColor, grid: QColor) -> None:
        self.bg = bg
        self.fg = fg
        self.edge = edge
        self.grid = grid


class ITheme(ABC):
    @abstractmethod
    def palette(self) -> ThemePalette:
        pass


class LightTheme(ITheme):
    def palette(self) -> ThemePalette:
        return ThemePalette(
            QColor(255, 255, 255),
            QColor(30, 30, 30),
            QColor(40, 40, 40),
            QColor(200, 200, 200),
        )


class DarkThemeDecorator(ITheme):
    """Decorator: envolve tema claro e ajusta cores para modo escuro."""

    def __init__(self, wrapped: ITheme) -> None:
        self._wrapped = wrapped

    def palette(self) -> ThemePalette:
        return ThemePalette(
            QColor(45, 45, 48),
            QColor(220, 220, 220),
            QColor(200, 200, 200),
            QColor(70, 70, 70),
        )


class ThemeManager:
    def __init__(self) -> None:
        self._dark = False

    def set_dark(self, enabled: bool) -> None:
        self._dark = enabled

    def current(self) -> ITheme:
        base = LightTheme()
        return DarkThemeDecorator(base) if self._dark else base


_theme_manager = ThemeManager()


def theme_manager() -> ThemeManager:
    return _theme_manager