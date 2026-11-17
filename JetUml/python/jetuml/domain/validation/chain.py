from __future__ import annotations

from abc import ABC, abstractmethod

from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode
from jetuml.models.kinds import EdgeKind, NodeKind


class ValidationRule(ABC):
    """Chain of Responsibility: cada regra pode encadear a próxima."""

    def __init__(self) -> None:
        self._next: ValidationRule | None = None

    def set_next(self, rule: ValidationRule) -> ValidationRule:
        self._next = rule
        return rule

    def validate(
        self,
        diagram: Diagram,
        edge: DiagramEdge,
        source: DiagramNode | None,
        target: DiagramNode | None,
    ) -> str | None:
        err = self._check(diagram, edge, source, target)
        if err is not None:
            return err
        if self._next:
            return self._next.validate(diagram, edge, source, target)
        return None

    @abstractmethod
    def _check(
        self,
        diagram: Diagram,
        edge: DiagramEdge,
        source: DiagramNode | None,
        target: DiagramNode | None,
    ) -> str | None:
        pass


class EndpointsExistRule(ValidationRule):
    def _check(self, diagram, edge, source, target) -> str | None:
        if edge.kind == EdgeKind.NOTE_CONNECTOR.value:
            if not source:
                return "Elemento de origem ou destino inválido."
            return None
        if not source or not target:
            return "Elemento de origem ou destino inválido."
        return None


class NoteConnectorRule(ValidationRule):
    def _check(self, diagram, edge, source, target) -> str | None:
        if edge.kind != EdgeKind.NOTE_CONNECTOR.value:
            return None
        return None


class SelfEdgeRule(ValidationRule):
    def _check(self, diagram, edge, source, target) -> str | None:
        if not source or edge.source_id != edge.target_id:
            return None
        return validate_self_edge(diagram, edge, source)


def build_common_chain() -> ValidationRule:
    head = EndpointsExistRule()
    head.set_next(NoteConnectorRule()).set_next(SelfEdgeRule())
    return head


def validate_self_edge(diagram: Diagram, edge: DiagramEdge, node: DiagramNode) -> str | None:
    from jetuml.domain.validation.strategies import count_edges_between

    dt = diagram.diagram_type
    k = edge.kind
    from jetuml.models.kinds import DiagramType

    if dt == DiagramType.CLASS and k in (EdgeKind.INHERITANCE.value, EdgeKind.DEPENDENCY.value):
        return "Auto-herança e auto-dependência não são permitidas."
    if dt == DiagramType.USE_CASE and k in (
        EdgeKind.UC_ASSOCIATION.value,
        EdgeKind.GENERALIZATION.value,
        EdgeKind.DEPENDENCY.value,
    ):
        return "Auto-relacionamento não permitido para Ator/Caso de Uso."
    if dt == DiagramType.OBJECT and k == EdgeKind.COLLABORATION.value:
        return "Auto-colaboração não é permitida."
    if dt == DiagramType.STATE and k == EdgeKind.TRANSITION.value:
        count = count_edges_between(diagram, node.id, node.id, k, directed=True)
        if count >= 2:
            return "Máximo de duas autotransições por estado."
    return None


def count_edges_between(
    diagram: Diagram, a: str, b: str, kind: str, directed: bool = False
) -> int:
    c = 0
    for e in diagram.edges:
        if e.kind != kind:
            continue
        if directed:
            if e.source_id == a and e.target_id == b:
                c += 1
        elif (e.source_id == a and e.target_id == b) or (e.source_id == b and e.target_id == a):
            c += 1
    return c