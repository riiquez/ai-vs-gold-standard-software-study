from __future__ import annotations

from PyQt5.QtCore import Qt
from PyQt5.QtGui import QKeyEvent
from PyQt5.QtWidgets import (
    QCheckBox,
    QComboBox,
    QDialog,
    QFormLayout,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QPushButton,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)

from jetuml.commands.history import SetPropertiesCommand
from jetuml.models.diagram import Diagram
from jetuml.models.elements import DiagramEdge, DiagramNode


class PropertiesDialog(QDialog):
    def __init__(self, diagram: Diagram, history, target_id: str, is_edge: bool, parent=None) -> None:
        super().__init__(parent)
        self.diagram = diagram
        self.history = history
        self.target_id = target_id
        self.is_edge = is_edge
        self._before: dict = {}
        self._fields: list[QWidget] = []
        self.setWindowTitle("Propriedades")
        self.setModal(True)
        layout = QVBoxLayout(self)
        self._form = QFormLayout()
        layout.addLayout(self._form)
        btn = QPushButton("OK")
        btn.clicked.connect(self._apply)
        layout.addWidget(btn)
        self._build_fields()
        self.resize(400, 300)

    def _target(self) -> DiagramNode | DiagramEdge | None:
        if self.is_edge:
            for e in self.diagram.edges:
                if e.id == self.target_id:
                    return e
        else:
            return self.diagram.node_by_id(self.target_id)
        return None

    def _build_fields(self) -> None:
        t = self._target()
        if not t:
            return
        self._before = dict(t.properties)
        p = t.properties
        kind = t.kind if isinstance(t, DiagramNode) else t.kind

        def add_line(key: str, label: str, value: str) -> None:
            w = QLineEdit(value)
            w.setObjectName(key)
            self._form.addRow(label, w)
            self._fields.append(w)

        def add_text(key: str, label: str, value: str) -> None:
            w = QTextEdit(value)
            w.setObjectName(key)
            self._form.addRow(label, w)
            self._fields.append(w)

        if isinstance(t, DiagramNode):
            if kind == "note":
                add_text("text", "Texto", p.get("text", ""))
            elif kind == "class":
                add_line("name", "Nome", p.get("name", ""))
                add_text("attributes", "Atributos", p.get("attributes", ""))
                add_text("methods", "Métodos", p.get("methods", ""))
            elif kind == "interface":
                add_line("name", "Nome", p.get("name", ""))
                add_text("methods", "Métodos", p.get("methods", ""))
            elif kind == "package":
                add_line("name", "Nome", p.get("name", ""))
            elif kind == "package_description":
                add_line("name", "Nome", p.get("name", ""))
                add_text("content", "Conteúdo", p.get("content", ""))
            elif kind == "object":
                add_line("name", "Nome", p.get("name", ""))
            elif kind == "field":
                add_line("name", "Nome", p.get("name", ""))
                add_line("value", "Valor", p.get("value", ""))
            elif kind == "actor":
                add_line("name", "Nome", p.get("name", ""))
            elif kind == "use_case":
                add_line("name", "Nome", p.get("name", ""))
            elif kind == "state":
                add_line("name", "Nome", p.get("name", ""))
            elif kind == "implicit_parameter":
                add_line("name", "Nome", p.get("name", ""))
            elif kind == "activation":
                cb = QCheckBox("Open Bottom")
                cb.setChecked(bool(p.get("open_bottom", False)))
                cb.setObjectName("open_bottom")
                self._form.addRow("Sequência", cb)
                self._fields.append(cb)
            else:
                add_line("name", "Nome", p.get("name", ""))
        else:
            if kind in ("association", "aggregation", "composition"):
                add_line("start_label", "Start Label", p.get("start_label", ""))
                add_line("middle_label", "Middle Label", p.get("middle_label", ""))
                add_line("end_label", "End Label", p.get("end_label", ""))
                combo = QComboBox()
                combo.addItems(["unspecified", "unidirectional", "bidirectional"])
                combo.setCurrentText(p.get("directionality", "unspecified"))
                combo.setObjectName("directionality")
                self._form.addRow("Directionality", combo)
                self._fields.append(combo)
                agg = QComboBox()
                agg.addItems(["aggregation", "composition"])
                agg.setCurrentText(p.get("aggregation_type", "aggregation"))
                agg.setObjectName("aggregation_type")
                self._form.addRow("Aggregation Type", agg)
                self._fields.append(agg)
            elif kind in ("inheritance", "realization", "dependency"):
                gt = QComboBox()
                gt.addItems(["inheritance", "implementation"])
                gt.setCurrentText(p.get("generalization_type", "inheritance"))
                gt.setObjectName("generalization_type")
                self._form.addRow("Generalization Type", gt)
                self._fields.append(gt)
                d = QComboBox()
                d.addItems(["unidirectional", "bidirectional"])
                d.setCurrentText(p.get("directionality", "unidirectional"))
                d.setObjectName("directionality")
                self._form.addRow("Directionality", d)
                self._fields.append(d)
            elif kind in ("call", "return", "collaboration", "transition"):
                add_line("middle_label", "Middle Label", p.get("middle_label", ""))
            elif kind in ("include", "extend", "uc_association"):
                dep = QComboBox()
                dep.addItems(["none", "include", "extend"])
                dt = p.get("dependency_type", "none")
                if kind in ("include", "extend"):
                    dt = kind
                dep.setCurrentText(dt)
                dep.setObjectName("dependency_type")
                self._form.addRow("Dependency Type", dep)
                self._fields.append(dep)

    def _apply(self) -> None:
        t = self._target()
        if not t:
            return
        after = dict(t.properties)
        for w in self._fields:
            key = w.objectName()
            if isinstance(w, QLineEdit):
                after[key] = w.text()
            elif isinstance(w, QTextEdit):
                after[key] = w.toPlainText()
            elif isinstance(w, QCheckBox):
                after[key] = w.isChecked()
            elif isinstance(w, QComboBox):
                after[key] = w.currentText()
        self.history.execute(
            SetPropertiesCommand(self.diagram, self.target_id, self.is_edge, self._before, after)
        )
        self.accept()

    def keyPressEvent(self, event: QKeyEvent) -> None:
        if event.key() == Qt.Key_Escape:
            self.reject()
            return
        if event.modifiers() & Qt.ControlModifier and event.key() == Qt.Key_Q:
            for w in self._fields:
                if isinstance(w, QLineEdit):
                    pos = w.cursorPosition()
                    text = w.text()
                    w.setText(text[:pos] + "«»" + text[pos:])
                    w.setCursorPosition(pos + 1)
                    return
        super().keyPressEvent(event)