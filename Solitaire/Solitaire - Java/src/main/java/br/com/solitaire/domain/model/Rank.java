package br.com.solitaire.domain.model;

public enum Rank {
    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K");

    private final int value;
    private final String symbol;

    Rank(int value, String symbol) {
        this.value = value;
        this.symbol = symbol;
    }

    public int value() {
        return value;
    }

    public String symbol() {
        return symbol;
    }

    public boolean isImmediatelyAbove(Rank other) {
        return other != null && value == other.value + 1;
    }

    public boolean isImmediatelyBelow(Rank other) {
        return other != null && value + 1 == other.value;
    }
}
