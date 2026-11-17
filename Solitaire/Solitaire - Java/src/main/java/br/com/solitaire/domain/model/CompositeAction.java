package br.com.solitaire.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Executa ações em ordem e as desfaz na ordem inversa. */
public final class CompositeAction implements GameAction {
    private final List<GameAction> actions;

    public CompositeAction(List<GameAction> actions) {
        this.actions = List.copyOf(Objects.requireNonNull(actions));
    }

    @Override
    public void execute() {
        List<GameAction> executed = new ArrayList<>();
        try {
            for (GameAction action : actions) {
                action.execute();
                executed.add(action);
            }
        } catch (RuntimeException exception) {
            for (int index = executed.size() - 1; index >= 0; index--) {
                executed.get(index).undo();
            }
            throw exception;
        }
    }

    @Override
    public void undo() {
        for (int index = actions.size() - 1; index >= 0; index--) {
            actions.get(index).undo();
        }
    }

    @Override
    public boolean isNoOp() {
        return actions.isEmpty() || actions.stream().allMatch(GameAction::isNoOp);
    }
}
