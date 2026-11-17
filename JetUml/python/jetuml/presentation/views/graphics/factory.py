from __future__ import annotations

from PyQt5.QtGui import QFont

from jetuml.models.elements import DiagramNode
from jetuml.models.kinds import NodeKind
from jetuml.presentation.views.graphics.base_node_item import BaseNodeGraphicsItem, PackageNodeItem, RectangularNodeItem
from jetuml.presentation.views.graphics.specialized_nodes import (
    ActorNodeItem,
    FinalStateNodeItem,
    InitialStateNodeItem,
    NoteNodeItem,
    UseCaseNodeItem,
)
from jetuml.presentation.views.graphics.uml_nodes import (
    ActivationNodeItem,
    ClassNodeItem,
    InterfaceNodeItem,
    LifelineNodeItem,
)


class NodeGraphicsFactory:
    _MAP: dict[str, type[BaseNodeGraphicsItem]] = {
        NodeKind.PACKAGE.value: PackageNodeItem,
        NodeKind.CLASS.value: ClassNodeItem,
        NodeKind.INTERFACE.value: InterfaceNodeItem,
        NodeKind.USE_CASE.value: UseCaseNodeItem,
        NodeKind.ACTOR.value: ActorNodeItem,
        NodeKind.INITIAL.value: InitialStateNodeItem,
        NodeKind.FINAL.value: FinalStateNodeItem,
        NodeKind.NOTE.value: NoteNodeItem,
        NodeKind.LIFELINE.value: LifelineNodeItem,
        NodeKind.ACTIVATION.value: ActivationNodeItem,
    }

    @classmethod
    def create(cls, node: DiagramNode, font: QFont) -> BaseNodeGraphicsItem:
        kind = node.kind
        if kind == NodeKind.FIELD.value:

            class FieldNodeItem(RectangularNodeItem):
                def display_text(self) -> str:
                    p = self.node.properties
                    return f"{p.get('name', 'field')} = {p.get('value', '')}"

            return FieldNodeItem(node, font)
        item_cls = cls._MAP.get(kind, RectangularNodeItem)
        return item_cls(node, font)