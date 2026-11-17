package jetuml.controller;

import jetuml.app.Tool;
import jetuml.cmd.UndoStack;
import jetuml.model.Diagram;
import jetuml.model.Element;
import jetuml.view.Layout;

/** O que o controlador pede à visão, sem depender de Swing. */
public interface CanvasHost {
    Tool tool();

    Diagram diagram();

    UndoStack stack();

    void changed();

    void notifyUser(String message, boolean error);

    void openProperties(Element element);

    boolean autoEdit();

    int modelWidth();

    int modelHeight();

    Layout geometry();

    void repaintCanvas();
}
