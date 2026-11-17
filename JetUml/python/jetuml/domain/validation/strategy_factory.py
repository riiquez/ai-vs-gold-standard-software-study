from __future__ import annotations

from jetuml.domain.interfaces import IEdgeValidationStrategy
from jetuml.domain.validation.strategies import (
    ClassDiagramValidationStrategy,
    ObjectDiagramValidationStrategy,
    SequenceDiagramValidationStrategy,
    StateDiagramValidationStrategy,
    UseCaseDiagramValidationStrategy,
)
from jetuml.models.kinds import DiagramType


class ValidationStrategyFactory:
    @staticmethod
    def create(diagram_type: DiagramType) -> IEdgeValidationStrategy:
        mapping: dict[DiagramType, type[IEdgeValidationStrategy]] = {
            DiagramType.CLASS: ClassDiagramValidationStrategy,
            DiagramType.USE_CASE: UseCaseDiagramValidationStrategy,
            DiagramType.OBJECT: ObjectDiagramValidationStrategy,
            DiagramType.STATE: StateDiagramValidationStrategy,
            DiagramType.SEQUENCE: SequenceDiagramValidationStrategy,
        }
        cls = mapping.get(diagram_type, ClassDiagramValidationStrategy)
        return cls()