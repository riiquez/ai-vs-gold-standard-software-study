from __future__ import annotations

from jetuml.infrastructure.persistence.json_repository import JsonDiagramRepository
from jetuml.models.diagram import Diagram

_repo = JsonDiagramRepository()


def save_diagram(diagram: Diagram, path: str) -> None:
    _repo.save(diagram, path)


def load_diagram(path: str) -> Diagram:
    return _repo.load(path)