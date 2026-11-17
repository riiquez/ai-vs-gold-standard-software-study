package br.com.solitaire.domain.model;

public enum Suit {
    HEARTS("Copas", "♥", CardColor.RED),
    DIAMONDS("Ouros", "♦", CardColor.RED),
    CLUBS("Paus", "♣", CardColor.BLACK),
    SPADES("Espadas", "♠", CardColor.BLACK);

    private final String displayName;
    private final String symbol;
    private final CardColor color;

    Suit(String displayName, String symbol, CardColor color) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public String symbol() {
        return symbol;
    }

    public CardColor color() {
        return color;
    }

    public boolean hasSameColorAs(Suit other) {
        return other != null && color == other.color;
    }
}
