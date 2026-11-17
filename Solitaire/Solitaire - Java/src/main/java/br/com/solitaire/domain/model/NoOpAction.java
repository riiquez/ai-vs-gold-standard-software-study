package br.com.solitaire.domain.model;

/** Representa uma ação sem efeito, sem exigir tratamento especial no chamador. */
public final class NoOpAction implements GameAction {
    public static final NoOpAction INSTANCE = new NoOpAction();

    private NoOpAction() {
    }

    @Override
    public void execute() {
        // Intencionalmente vazio.
    }

    @Override
    public void undo() {
        // Intencionalmente vazio.
    }

    @Override
    public boolean isNoOp() {
        return true;
    }
}
