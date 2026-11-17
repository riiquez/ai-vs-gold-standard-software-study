from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from .elements import DiagramEdge, DiagramNode
from .kinds import DiagramType


@dataclass
class DiagramSettings:
    grid_visible: bool = True
    font_family: str = "Segoe UI"
    font_size: int = 10
    snap_grid: int = 20

    def to_dict(self) -> dict[str, Any]:
        return {
            "grid_visible": self.grid_visible,
            "font_family": self.font_family,
            "font_size": self.font_size,
            "snap_grid": self.snap_grid,
        }

    @staticmethod
    def from_dict(data: dict[str, Any]) -> DiagramSettings:
        return DiagramSettings(
            grid_visible=bool(data.get("grid_visible", True)),
            font_family=str(data.get("font_family", "Segoe UI")),
            font_size=int(data.get("font_size", 10)),
            snap_grid=int(data.get("snap_grid", 20)),
        )


@dataclass
class Diagram:
    diagram_type: DiagramType
    width: int
    height: int
    nodes: list[DiagramNode] = field(default_factory=list)
    edges: list[DiagramEdge] = field(default_factory=list)
    settings: DiagramSettings = field(default_factory=DiagramSettings)
    file_path: str | None = None
    dirty: bool = False

    def node_by_id(self, nid: str) -> DiagramNode | None:
        for n in self.nodes:
            if n.id == nid:
                return n
        return None

    def snap(self, value: float) -> float:
        g = self.settings.snap_grid
        return round(value / g) * g

    def to_dict(self) -> dict[str, Any]:
        return {
            "format": "jetuml",
            "version": 1,
            "diagram_type": self.diagram_type.value,
            "width": self.width,
            "height": self.height,
            "settings": self.settings.to_dict(),
            "nodes": [n.to_dict() for n in self.nodes],
            "edges": [e.to_dict() for e in self.edges],
        }

    @staticmethod
    def from_dict(data: dict[str, Any]) -> Diagram:
        return Diagram(
            diagram_type=DiagramType(data["diagram_type"]),
            width=int(data["width"]),
            height=int(data["height"]),
            nodes=[DiagramNode.from_dict(n) for n in data.get("nodes", [])],
            edges=[DiagramEdge.from_dict(e) for e in data.get("edges", [])],
            settings=DiagramSettings.from_dict(data.get("settings", {})),
        )

    def content_bounds(self) -> tuple[float, float, float, float]:
        """min_x, min_y, max_x, max_y dos elementos."""
        if not self.nodes:
            return (0, 0, self.width, self.height)
        min_x = min(n.x for n in self.nodes)
        min_y = min(n.y for n in self.nodes)
        max_x = max(n.x + n.width for n in self.nodes)
        max_y = max(n.y + n.height for n in self.nodes)
        return min_x, min_y, max_x, max_y

    def ensure_fits_nodes(self) -> None:
        _, _, max_x, max_y = self.content_bounds()
        self.width = max(self.width, int(max_x) + 80)
        self.height = max(self.height, int(max_y) + 80)