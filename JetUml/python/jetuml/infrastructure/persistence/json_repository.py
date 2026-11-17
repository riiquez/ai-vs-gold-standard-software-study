from __future__ import annotations

import json
from pathlib import Path

from jetuml.domain.interfaces import IDiagramRepository
from jetuml.models.diagram import Diagram


class JsonDiagramRepository(IDiagramRepository):
    """Repository: persistência JSON (RNF02)."""

    def save(self, diagram: Diagram, path: str) -> None:
        data = diagram.to_dict()
        Path(path).write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
        diagram.file_path = path
        diagram.dirty = False

    def load(self, path: str) -> Diagram:
        raw = json.loads(Path(path).read_text(encoding="utf-8"))
        diagram = Diagram.from_dict(raw)
        diagram.file_path = path
        diagram.dirty = False
        diagram.ensure_fits_nodes()
        return diagram