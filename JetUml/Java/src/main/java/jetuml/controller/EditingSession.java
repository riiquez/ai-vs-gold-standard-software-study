package jetuml.controller;

import java.util.Collection;
import java.util.Set;

import jetuml.cmd.UndoStack;
import jetuml.model.Diagram;
import jetuml.model.Element;

/** Sessão que o controlador de edição enxerga. A janela continua dona dos componentes. */
public interface EditingSession {
    Diagram diagram();

    UndoStack stack();

    Set<Element> selection();

    void replaceSelection(Collection<Element> elements);

    void clearSelection();

    void changed();

    void notifyUser(String message, boolean error);

    void showProperties(Element element);
}
