package br.com.solitaire.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Vira todo o descarte para formar o monte. O topo do descarte passa a ser
 * a próxima carta comprada, como ocorre ao virar fisicamente a pilha.
 */
public final class RecycleWasteAction implements GameAction {
    private final CardPile stock;
    private final CardPile waste;
    private final List<Card> originalWasteOrder;

    public RecycleWasteAction(CardPile stock, CardPile waste) {
        this.stock = Objects.requireNonNull(stock);
        this.waste = Objects.requireNonNull(waste);
        if (!stock.isEmpty()) {
            throw new IllegalArgumentException("Só é possível reciclar quando o monte está vazio");
        }
        this.originalWasteOrder = waste.cards();
    }

    @Override
    public void execute() {
        List<Card> removed = waste.removeAll();
        if (!sameReferences(removed, originalWasteOrder)) {
            waste.pushAll(removed);
            throw new IllegalStateException("O descarte mudou antes da reciclagem");
        }

        List<Card> reversed = new ArrayList<>(removed);
        Collections.reverse(reversed);
        reversed.forEach(card -> card.setFaceUp(false));
        stock.pushAll(reversed);
    }

    @Override
    public void undo() {
        List<Card> removed = stock.removeTop(originalWasteOrder.size());
        List<Card> expected = new ArrayList<>(originalWasteOrder);
        Collections.reverse(expected);
        if (!sameReferences(removed, expected)) {
            stock.pushAll(removed);
            throw new IllegalStateException("O monte mudou antes de desfazer a reciclagem");
        }
        originalWasteOrder.forEach(card -> card.setFaceUp(true));
        waste.pushAll(originalWasteOrder);
    }

    @Override
    public boolean isNoOp() {
        return originalWasteOrder.isEmpty();
    }

    private boolean sameReferences(List<Card> first, List<Card> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            if (first.get(index) != second.get(index)) {
                return false;
            }
        }
        return true;
    }
}
