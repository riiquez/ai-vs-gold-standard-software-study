package br.com.solitaire.domain.model;

import java.util.Objects;

/** Altera a visibilidade de uma carta e guarda o valor anterior para desfazer. */
public final class SetCardVisibilityAction implements GameAction {
    private final Card card;
    private final boolean newVisibility;
    private boolean previousVisibility;

    public SetCardVisibilityAction(Card card, boolean newVisibility) {
        this.card = Objects.requireNonNull(card);
        this.newVisibility = newVisibility;
        this.previousVisibility = card.isFaceUp();
    }

    @Override
    public void execute() {
        previousVisibility = card.isFaceUp();
        card.setFaceUp(newVisibility);
    }

    @Override
    public void undo() {
        card.setFaceUp(previousVisibility);
    }

    @Override
    public boolean isNoOp() {
        return card.isFaceUp() == newVisibility;
    }
}
