package br.com.solitaire.domain.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Sequência ordenada da base para o topo. A cópia da lista preserva as
 * referências das cartas, em vez de criar cartas equivalentes.
 */
public record CardSequence(List<Card> cards) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public CardSequence {
        Objects.requireNonNull(cards, "cartas não podem ser nulas");
        cards = List.copyOf(cards);
        if (cards.isEmpty()) {
            throw new IllegalArgumentException("A sequência precisa conter ao menos uma carta");
        }
    }

    public Card first() {
        return cards.getFirst();
    }

    public Card last() {
        return cards.getLast();
    }

    public int size() {
        return cards.size();
    }
}
