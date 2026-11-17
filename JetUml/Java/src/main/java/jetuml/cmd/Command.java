package jetuml.cmd;

public interface Command {
    void execute();

    void undo();
}
