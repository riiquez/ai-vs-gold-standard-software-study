package jetuml.controller;

import java.util.List;

import jetuml.app.ElementClipboard;
import jetuml.cmd.Commands;
import jetuml.model.Element;

/** Controle das ações de editar: desfazer, área de transferência e propriedades. */
public final class EditorController {
    public void undo(EditingSession session) {
        if (session.stack().canUndo()) {
            session.stack().undo();
            session.changed();
        }
    }

    public void redo(EditingSession session) {
        if (session.stack().canRedo()) {
            session.stack().redo();
            session.changed();
        }
    }

    public void cut(EditingSession session) {
        if (session.selection().isEmpty()) {
            return;
        }
        ElementClipboard.copy(session.diagram(), List.copyOf(session.selection()));
        session.stack().push(Commands.delete(session.diagram(), List.copyOf(session.selection())));
        session.clearSelection();
        session.changed();
    }

    public void copy(EditingSession session) {
        if (session.selection().isEmpty()) {
            return;
        }
        ElementClipboard.copy(session.diagram(), List.copyOf(session.selection()));
    }

    public void paste(EditingSession session) {
        ElementClipboard.Paste pasted = ElementClipboard.paste(session.diagram());
        if (!pasted.check().ok()) {
            if (pasted.check().kind() == jetuml.rules.Check.Kind.NOTIFY) {
                session.notifyUser(pasted.check().message(), true);
            }
            return;
        }
        session.stack().push(pasted.command());
        session.replaceSelection(pasted.elements());
        session.changed();
    }

    public void delete(EditingSession session) {
        if (session.selection().isEmpty()) {
            return;
        }
        session.stack().push(Commands.delete(session.diagram(), List.copyOf(session.selection())));
        session.clearSelection();
        session.changed();
    }

    public void editProperties(EditingSession session) {
        if (session.selection().size() != 1) {
            session.notifyUser("Select a single element to edit its properties.", true);
            return;
        }
        session.showProperties(session.selection().iterator().next());
    }
}
