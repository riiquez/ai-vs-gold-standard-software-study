package br.com.solitaire.domain.model;

import java.util.List;
import java.util.Objects;

/** Move uma ou mais cartas preservando sua ordem e identidade. */
public final class TransferCardsAction implements GameAction {
    private final CardPile source;
    private final CardPile destination;
    private final CardSequence sequence;

    public TransferCardsAction(CardPile source, CardPile destination, CardSequence sequence) {
        this.source = Objects.requireNonNull(source);
        this.destination = Objects.requireNonNull(destination);
        this.sequence = Objects.requireNonNull(sequence);
        if (source == destination) {
            throw new IllegalArgumentException("Origem e destino devem ser diferentes");
        }
    }

    @Override
    public void execute() {
        List<Card> removed = source.removeTop(sequence.size());
        if (!sameReferences(removed, sequence.cards())) {
            source.pushAll(removed);
            throw new IllegalStateException("A pilha de origem mudou antes da execução da ação");
        }
        destination.pushAll(removed);
    }

    @Override
    public void undo() {
        List<Card> removed = destination.removeTop(sequence.size());
        if (!sameReferences(removed, sequence.cards())) {
            destination.pushAll(removed);
            throw new IllegalStateException("A pilha de destino mudou antes de desfazer a ação");
        }
        source.pushAll(removed);
    }

    @Override
    public boolean isNoOp() {
        return false;
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
