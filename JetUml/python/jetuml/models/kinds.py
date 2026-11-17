from __future__ import annotations

from enum import Enum


class DiagramType(str, Enum):
    CLASS = "class"
    SEQUENCE = "sequence"
    STATE = "state"
    OBJECT = "object"
    USE_CASE = "use_case"

    @property
    def label(self) -> str:
        return {
            DiagramType.CLASS: "Class Diagram",
            DiagramType.SEQUENCE: "Sequence Diagram",
            DiagramType.STATE: "State Diagram",
            DiagramType.OBJECT: "Object Diagram",
            DiagramType.USE_CASE: "Use Case Diagram",
        }[self]


class NodeKind(str, Enum):
    # comum
    NOTE = "note"
    # class
    CLASS = "class"
    INTERFACE = "interface"
    PACKAGE = "package"
    PACKAGE_DESC = "package_description"
    # sequence
    LIFELINE = "lifeline"
    IMPLICIT_PARAM = "implicit_parameter"
    ACTIVATION = "activation"
    # state
    STATE = "state"
    INITIAL = "initial_state"
    FINAL = "final_state"
    # object
    OBJECT = "object"
    FIELD = "field"
    # use case
    ACTOR = "actor"
    USE_CASE = "use_case"


class EdgeKind(str, Enum):
    NOTE_CONNECTOR = "note_connector"
    # class
    DEPENDENCY = "dependency"
    INHERITANCE = "inheritance"
    REALIZATION = "realization"
    ASSOCIATION = "association"
    AGGREGATION = "aggregation"
    COMPOSITION = "composition"
    # sequence
    CALL = "call"
    RETURN = "return"
    # state
    TRANSITION = "transition"
    # object
    REFERENCE = "reference"
    COLLABORATION = "collaboration"
    # use case
    UC_ASSOCIATION = "uc_association"
    INCLUDE = "include"
    EXTEND = "extend"
    GENERALIZATION = "generalization"


TOOLS_BY_DIAGRAM: dict[DiagramType, list[tuple[str, str | None]]] = {
    DiagramType.CLASS: [
        ("Select", None),
        ("Class", NodeKind.CLASS.value),
        ("Interface", NodeKind.INTERFACE.value),
        ("Package", NodeKind.PACKAGE.value),
        ("Package Description", NodeKind.PACKAGE_DESC.value),
        ("Note", NodeKind.NOTE.value),
        ("Dependency", EdgeKind.DEPENDENCY.value),
        ("Inheritance", EdgeKind.INHERITANCE.value),
        ("Realization", EdgeKind.REALIZATION.value),
        ("Association", EdgeKind.ASSOCIATION.value),
        ("Aggregation", EdgeKind.AGGREGATION.value),
        ("Composition", EdgeKind.COMPOSITION.value),
        ("Note Connector", EdgeKind.NOTE_CONNECTOR.value),
        ("To Clipboard", "__clipboard__"),
    ],
    DiagramType.SEQUENCE: [
        ("Select", None),
        ("Implicit Parameter", NodeKind.IMPLICIT_PARAM.value),
        ("Note", NodeKind.NOTE.value),
        ("Call", EdgeKind.CALL.value),
        ("Return", EdgeKind.RETURN.value),
        ("Note Connector", EdgeKind.NOTE_CONNECTOR.value),
        ("To Clipboard", "__clipboard__"),
    ],
    DiagramType.STATE: [
        ("Select", None),
        ("State", NodeKind.STATE.value),
        ("Initial State", NodeKind.INITIAL.value),
        ("Final State", NodeKind.FINAL.value),
        ("Note", NodeKind.NOTE.value),
        ("Transition", EdgeKind.TRANSITION.value),
        ("Note Connector", EdgeKind.NOTE_CONNECTOR.value),
        ("To Clipboard", "__clipboard__"),
    ],
    DiagramType.OBJECT: [
        ("Select", None),
        ("Object", NodeKind.OBJECT.value),
        ("Field", NodeKind.FIELD.value),
        ("Note", NodeKind.NOTE.value),
        ("Reference", EdgeKind.REFERENCE.value),
        ("Collaboration", EdgeKind.COLLABORATION.value),
        ("Note Connector", EdgeKind.NOTE_CONNECTOR.value),
        ("To Clipboard", "__clipboard__"),
    ],
    DiagramType.USE_CASE: [
        ("Select", None),
        ("Actor", NodeKind.ACTOR.value),
        ("Use Case", NodeKind.USE_CASE.value),
        ("Note", NodeKind.NOTE.value),
        ("Association", EdgeKind.UC_ASSOCIATION.value),
        ("Include", EdgeKind.INCLUDE.value),
        ("Extend", EdgeKind.EXTEND.value),
        ("Generalization", EdgeKind.GENERALIZATION.value),
        ("Note Connector", EdgeKind.NOTE_CONNECTOR.value),
        ("To Clipboard", "__clipboard__"),
    ],
}

TOOL_SHORTCUT_KEYS = list("1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ")


VERBOSE_TOOLTIPS: dict[str, str] = {
    "Select": "Seleciona, move e redimensiona elementos do diagrama.",
    "Class": "Classe UML com compartimentos de atributos e métodos.",
    "Interface": "Interface com métodos; use «interface» no nome se desejar.",
    "Package": "Agrupa elementos; Shift para anexar nós existentes.",
    "Dependency": "Dependência temporária (linha tracejada).",
    "Inheritance": "Generalização — herança de classe.",
    "Association": "Relacionamento estrutural entre classes.",
    "Call": "Mensagem síncrona entre linhas de vida ou activations.",
    "Return": "Retorno de controle em diagrama de sequência.",
    "Transition": "Transição entre estados com evento opcional.",
    "Field": "Campo dentro de um objeto; referências saem do valor.",
    "Actor": "Ator externo que interage com casos de uso.",
}


def edge_kinds_for_diagram(dt: DiagramType) -> set[EdgeKind]:
    kinds: set[EdgeKind] = {EdgeKind.NOTE_CONNECTOR}
    if dt == DiagramType.CLASS:
        kinds.update(
            {
                EdgeKind.DEPENDENCY,
                EdgeKind.INHERITANCE,
                EdgeKind.REALIZATION,
                EdgeKind.ASSOCIATION,
                EdgeKind.AGGREGATION,
                EdgeKind.COMPOSITION,
            }
        )
    elif dt == DiagramType.SEQUENCE:
        kinds.update({EdgeKind.CALL, EdgeKind.RETURN})
    elif dt == DiagramType.STATE:
        kinds.add(EdgeKind.TRANSITION)
    elif dt == DiagramType.OBJECT:
        kinds.update({EdgeKind.REFERENCE, EdgeKind.COLLABORATION})
    elif dt == DiagramType.USE_CASE:
        kinds.update(
            {
                EdgeKind.UC_ASSOCIATION,
                EdgeKind.INCLUDE,
                EdgeKind.EXTEND,
                EdgeKind.GENERALIZATION,
            }
        )
    return kinds


def node_kinds_for_diagram(dt: DiagramType) -> set[NodeKind]:
    notes = {NodeKind.NOTE}
    if dt == DiagramType.CLASS:
        return notes | {
            NodeKind.CLASS,
            NodeKind.INTERFACE,
            NodeKind.PACKAGE,
            NodeKind.PACKAGE_DESC,
        }
    if dt == DiagramType.SEQUENCE:
        return notes | {NodeKind.LIFELINE, NodeKind.IMPLICIT_PARAM, NodeKind.ACTIVATION}
    if dt == DiagramType.STATE:
        return notes | {NodeKind.STATE, NodeKind.INITIAL, NodeKind.FINAL}
    if dt == DiagramType.OBJECT:
        return notes | {NodeKind.OBJECT, NodeKind.FIELD}
    if dt == DiagramType.USE_CASE:
        return notes | {NodeKind.ACTOR, NodeKind.USE_CASE}
    return notes