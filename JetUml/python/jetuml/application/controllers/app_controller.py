from __future__ import annotations

from jetuml.infrastructure.config import add_recent, load_config, save_config
from jetuml.infrastructure.persistence.json_repository import JsonDiagramRepository
from jetuml.models.diagram import Diagram
from jetuml.models.kinds import DiagramType
from jetuml.models.diagram import DiagramSettings


class ApplicationController:
    def __init__(self, repository: JsonDiagramRepository | None = None) -> None:
        self.config = load_config()
        self._repo = repository or JsonDiagramRepository()

    def persist_config(self) -> None:
        save_config(self.config)

    def new_diagram(self, dtype: DiagramType) -> Diagram:
        return Diagram(
            diagram_type=dtype,
            width=int(self.config["diagram_width"]),
            height=int(self.config["diagram_height"]),
            settings=DiagramSettings(
                font_family=self.config.get("font_family", "Segoe UI"),
                font_size=int(self.config.get("font_size", 10)),
            ),
        )

    def open_diagram(self, path: str) -> Diagram:
        diagram = self._repo.load(path)
        diagram.width = max(diagram.width, int(self.config["diagram_width"]))
        diagram.height = max(diagram.height, int(self.config["diagram_height"]))
        diagram.ensure_fits_nodes()
        add_recent(path, self.config)
        self.persist_config()
        return diagram

    def save_diagram(self, diagram: Diagram, path: str) -> None:
        self._repo.save(diagram, path)
        add_recent(path, self.config)
        self.persist_config()

    def duplicate_diagram(self, source: Diagram) -> Diagram:
        dup = Diagram.from_dict(source.to_dict())
        dup.file_path = None
        dup.dirty = True
        return dup