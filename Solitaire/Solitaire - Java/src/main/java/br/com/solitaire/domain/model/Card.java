package br.com.solitaire.domain.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Uma carta é uma entidade: não sobrescreve equals/hashCode e, portanto,
 * mantém identidade por referência durante toda a partida.
 */
public final class Card implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final Suit suit;
    private final Rank rank;
    private boolean faceUp;

    public Card(Suit suit, Rank rank) {
        this(UUID.randomUUID().toString(), suit, rank, false);
    }

    private Card(String id, Suit suit, Rank rank, boolean faceUp) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.suit = Objects.requireNonNull(suit, "naipe não pode ser nulo");
        this.rank = Objects.requireNonNull(rank, "rank não pode ser nulo");
        this.faceUp = faceUp;
    }

    public String id() {
        return id;
    }

    public Suit suit() {
        return suit;
    }

    public Rank rank() {
        return rank;
    }

    public boolean isFaceUp() {
        return faceUp;
    }

    void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
    }

    @Override
    public String toString() {
        return rank.symbol() + suit.symbol();
    }
}
