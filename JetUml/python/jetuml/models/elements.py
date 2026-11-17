from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from typing import Any


def new_id() -> str:
    return str(uuid.uuid4())


@dataclass
class DiagramNode:
    kind: str
    x: float
    y: float
    width: float = 140.0
    height: float = 80.0
    id: str = field(default_factory=new_id)
    parent_id: str | None = None
    properties: dict[str, Any] = field(default_factory=dict)
    note_anchor: tuple[float, float] | None = None  # fixo no canvas para nota

    def to_dict(self) -> dict[str, Any]:
        d: dict[str, Any] = {
            "id": self.id,
            "kind": self.kind,
            "x": self.x,
            "y": self.y,
            "width": self.width,
            "height": self.height,
            "parent_id": self.parent_id,
            "properties": self.properties,
        }
        if self.note_anchor is not None:
            d["note_anchor"] = list(self.note_anchor)
        return d

    @staticmethod
    def from_dict(data: dict[str, Any]) -> DiagramNode:
        anchor = data.get("note_anchor")
        return DiagramNode(
            id=data["id"],
            kind=data["kind"],
            x=float(data["x"]),
            y=float(data["y"]),
            width=float(data.get("width", 140)),
            height=float(data.get("height", 80)),
            parent_id=data.get("parent_id"),
            properties=dict(data.get("properties", {})),
            note_anchor=tuple(anchor) if anchor else None,
        )


@dataclass
class DiagramEdge:
    kind: str
    source_id: str
    target_id: str
    id: str = field(default_factory=new_id)
    properties: dict[str, Any] = field(default_factory=dict)
  # note: source pode ser anchor point para note connector
    source_point: tuple[float, float] | None = None
    target_point: tuple[float, float] | None = None

    def to_dict(self) -> dict[str, Any]:
        d: dict[str, Any] = {
            "id": self.id,
            "kind": self.kind,
            "source_id": self.source_id,
            "target_id": self.target_id,
            "properties": self.properties,
        }
        if self.source_point:
            d["source_point"] = list(self.source_point)
        if self.target_point:
            d["target_point"] = list(self.target_point)
        return d

    @staticmethod
    def from_dict(data: dict[str, Any]) -> DiagramEdge:
        sp = data.get("source_point")
        tp = data.get("target_point")
        return DiagramEdge(
            id=data["id"],
            kind=data["kind"],
            source_id=data["source_id"],
            target_id=data["target_id"],
            properties=dict(data.get("properties", {})),
            source_point=tuple(sp) if sp else None,
            target_point=tuple(tp) if tp else None,
        )


def default_node_properties(kind: str) -> dict[str, Any]:
    base = {"name": "", "text": ""}
    if kind == "class":
        return {"name": "Class", "attributes": "", "methods": ""}
    if kind == "interface":
        return {"name": "Interface", "methods": ""}
    if kind == "package":
        return {"name": "Package"}
    if kind == "package_description":
        return {"name": "Description", "content": ""}
    if kind == "note":
        return {"text": "Note"}
    if kind == "object":
        return {"name": "object:Class"}
    if kind == "field":
        return {"name": "field", "value": ""}
    if kind == "lifeline":
        return {"name": ":Object"}
    if kind == "implicit_parameter":
        return {"name": "params"}
    if kind == "activation":
        return {"name": "", "open_bottom": False}
    if kind == "state":
        return {"name": "State"}
    if kind == "actor":
        return {"name": "Actor"}
    if kind == "use_case":
        return {"name": "Use Case"}
    return base


def default_edge_properties(kind: str) -> dict[str, Any]:
    if kind in ("association", "aggregation", "composition"):
        return {
            "start_label": "",
            "middle_label": "",
            "end_label": "",
            "directionality": "unspecified",
            "aggregation_type": "composition" if kind == "composition" else "aggregation",
        }
    if kind in ("inheritance", "realization", "dependency"):
        return {
            "generalization_type": "inheritance" if kind == "inheritance" else "implementation",
            "directionality": "unidirectional",
        }
    if kind in ("call", "return", "collaboration", "transition"):
        return {"middle_label": ""}
    if kind in ("include", "extend"):
        return {"dependency_type": kind}
    if kind == "uc_association":
        return {"dependency_type": "none"}
    return {}