from __future__ import annotations

from jetuml.domain.validation.strategy_factory import ValidationStrategyFactory
from jetuml.domain.validation.chain import build_common_chain
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge
from jetuml.models.kinds import DiagramType, EdgeKind


class EdgeValidator:
    """Compõe Chain of Responsibility + Strategy por tipo de diagrama."""

    def __init__(self, diagram: Diagram) -> None:
        self._diagram = diagram
        self._chain = build_common_chain()
        self._strategy = ValidationStrategyFactory.create(diagram.diagram_type)

    def validate(self, edge: DiagramEdge) -> str | None:
        src = self._diagram.node_by_id(edge.source_id)
        tgt = self._diagram.node_by_id(edge.target_id) if edge.target_id else None
        if edge.kind == EdgeKind.NOTE_CONNECTOR.value:
            err = self._chain.validate(self._diagram, edge, src, tgt)
            if err:
                return err
            return None
        if edge.source_id == edge.target_id and src:
            err = self._chain.validate(self._diagram, edge, src, tgt)
            return err
        err = self._chain.validate(self._diagram, edge, src, tgt)
        if err:
            return err
        if src and tgt:
            return self._strategy.validate_edge(self._diagram, edge, src, tgt)
        return None


def can_paste_into(target: DiagramType, element_kinds: list[str]) -> str | None:
    if target == DiagramType.SEQUENCE:
        return "Colar em diagrama de sequência não é permitido."
    return None