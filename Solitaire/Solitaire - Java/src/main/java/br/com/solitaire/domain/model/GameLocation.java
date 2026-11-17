package br.com.solitaire.domain.model;

import java.util.Objects;

/** Identifica uma pilha sem expor a própria estrutura mutável. */
public record GameLocation(LocationType type, int index) {
    public GameLocation {
        Objects.requireNonNull(type, "tipo de local não pode ser nulo");
        switch (type) {
            case TABLEAU -> {
                if (index < 0 || index >= 7) {
                    throw new IllegalArgumentException("Índice de coluna inválido");
                }
            }
            case FOUNDATION -> {
                if (index < 0 || index >= Suit.values().length) {
                    throw new IllegalArgumentException("Índice de fundação inválido");
                }
            }
            case STOCK, WASTE -> {
                if (index != -1) {
                    throw new IllegalArgumentException("Monte e descarte não possuem índice");
                }
            }
        }
    }

    public static GameLocation stock() {
        return new GameLocation(LocationType.STOCK, -1);
    }

    public static GameLocation waste() {
        return new GameLocation(LocationType.WASTE, -1);
    }

    public static GameLocation foundation(Suit suit) {
        return new GameLocation(LocationType.FOUNDATION, Objects.requireNonNull(suit).ordinal());
    }

    public static GameLocation tableau(int columnIndex) {
        return new GameLocation(LocationType.TABLEAU, columnIndex);
    }

    public Suit foundationSuit() {
        if (type != LocationType.FOUNDATION) {
            throw new IllegalStateException("O local não é uma fundação");
        }
        return Suit.values()[index];
    }
}
