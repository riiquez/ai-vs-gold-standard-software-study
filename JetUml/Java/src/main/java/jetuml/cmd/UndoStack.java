package jetuml.cmd;

import java.util.ArrayList;
import java.util.List;

/** Histórico linear. Desfazer chega até o estado inicial do diagrama (RF21). */
public final class UndoStack {
    private final List<Command> commands = new ArrayList<>();
    private int index;
    private int savedIndex;

    public void push(Command command) {
        push(command, true);
    }

    public void push(Command command, boolean execute) {
        while (commands.size() > index) {
            commands.remove(commands.size() - 1);
        }
        if (execute) {
            command.execute();
        }
        commands.add(command);
        index++;
    }

    public boolean canUndo() {
        return index > 0;
    }

    public boolean canRedo() {
        return index < commands.size();
    }

    public void undo() {
        if (canUndo()) {
            commands.get(--index).undo();
        }
    }

    public void redo() {
        if (canRedo()) {
            commands.get(index++).execute();
        }
    }

    public void markSaved() {
        savedIndex = index;
    }

    /** Um diagrama duplicado ainda não foi gravado, mesmo sem comandos. */
    public void markUnsaved() {
        savedIndex = -1;
    }

    public boolean isDirty() {
        return savedIndex != index;
    }
}
