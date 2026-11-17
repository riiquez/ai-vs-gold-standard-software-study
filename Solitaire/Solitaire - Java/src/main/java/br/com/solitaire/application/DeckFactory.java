package br.com.solitaire.application;

import br.com.solitaire.domain.model.Card;
import br.com.solitaire.domain.model.Rank;
import br.com.solitaire.domain.model.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class DeckFactory {
    private DeckFactory() {
    }

    public static List<Card> shuffledDeck(RandomGenerator random) {
        Objects.requireNonNull(random);
        List<Card> cards = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(cards, new java.util.Random(random.nextLong()));
        return List.copyOf(cards);
    }
}
