from __future__ import annotations

from jetuml.domain.services.diagram_service import DiagramService
from jetuml.domain.services.sequence_service import SequenceDiagramService
from jetuml.domain.services.sequence_service_decorator import SequenceDiagramServiceDecorator
from jetuml.models.diagram import Diagram
from jetuml.models.kinds import DiagramType


class DiagramServiceFactory:
    """Factory + Decorator: serviço de domínio conforme tipo de diagrama."""

    @staticmethod
    def create(diagram: Diagram) -> DiagramService:
        core = DiagramService(diagram)
        if diagram.diagram_type == DiagramType.SEQUENCE:
            seq = SequenceDiagramService(diagram)
            return SequenceDiagramServiceDecorator(core, seq)
        return core