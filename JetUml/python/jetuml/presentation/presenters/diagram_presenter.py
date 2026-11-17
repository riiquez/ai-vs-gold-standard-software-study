from __future__ import annotations

from jetuml.application.controllers.diagram_controller import DiagramController, DiagramControllerCallbacks
from jetuml.domain.commands import CommandHistory
from jetuml.models.diagram import Diagram


class DiagramPresenter:
    """Adaptador entre View (Qt) e DiagramController — papel extra do MVC desktop."""

    def __init__(self, diagram: Diagram, scene) -> None:
        self.diagram = diagram
        self.scene = scene
        self.history = CommandHistory()
        callbacks = DiagramControllerCallbacks(
            on_model_changed=self._on_model_changed,
            on_notify=lambda msg, err: scene.notify.emit(msg, err),
            on_request_properties=lambda eid, is_edge: scene.request_properties.emit(eid, is_edge),
        )
        self.controller = DiagramController(diagram, self.history, callbacks)
        self.controller.auto_edit_node = getattr(scene, "auto_edit_node", False)

    def _on_model_changed(self) -> None:
        self.scene.rebuild()
        self.scene.model_changed.emit()

    def sync_auto_edit(self, enabled: bool) -> None:
        self.controller.auto_edit_node = enabled
        self.scene.auto_edit_node = enabled