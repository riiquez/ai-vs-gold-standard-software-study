package br.com.solitaire.domain.rules;

import br.com.solitaire.domain.model.Card;
import br.com.solitaire.domain.model.CardPile;
import br.com.solitaire.domain.model.CardSequence;
import br.com.solitaire.domain.model.GameLocation;
import br.com.solitaire.domain.model.Rank;
import br.com.solitaire.domain.model.SetCardVisibilityAction;
import br.com.solitaire.domain.model.Suit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveRulesTest {
    private final MoveRules rules = new MoveRules();

    @Test
    void shouldAcceptDescendingAlternatingSequenceOnTableau() {
        Card eightHearts = visible(Suit.HEARTS, Rank.EIGHT);
        Card sevenSpades = visible(Suit.SPADES, Rank.SEVEN);
        Card sixDiamonds = visible(Suit.DIAMONDS, Rank.SIX);
        Card nineClubs = visible(Suit.CLUBS, Rank.NINE);
        CardPile destination = new CardPile();
        destination.push(nineClubs);

        assertTrue(rules.canMove(
                new CardSequence(List.of(eightHearts, sevenSpades, sixDiamonds)),
                GameLocation.tableau(0),
                GameLocation.tableau(1),
                destination));
    }

    @Test
    void shouldRejectSequenceWithSameColorNeighbors() {
        Card eightHearts = visible(Suit.HEARTS, Rank.EIGHT);
        Card sevenDiamonds = visible(Suit.DIAMONDS, Rank.SEVEN);
        CardPile destination = new CardPile();
        destination.push(visible(Suit.CLUBS, Rank.NINE));

        assertFalse(rules.canMove(
                new CardSequence(List.of(eightHearts, sevenDiamonds)),
                GameLocation.tableau(0),
                GameLocation.tableau(1),
                destination));
    }

    @Test
    void emptyTableauShouldAcceptOnlyKing() {
        Card king = visible(Suit.HEARTS, Rank.KING);
        Card queen = visible(Suit.HEARTS, Rank.QUEEN);

        assertTrue(rules.canPlaceOnTableau(king, new CardPile()));
        assertFalse(rules.canPlaceOnTableau(queen, new CardPile()));
    }

    @Test
    void emptyFoundationShouldAcceptOnlyAceOfItsSuit() {
        Card aceHearts = visible(Suit.HEARTS, Rank.ACE);
        Card aceSpades = visible(Suit.SPADES, Rank.ACE);
        CardPile emptyFoundation = new CardPile();

        assertTrue(rules.canMove(new CardSequence(List.of(aceHearts)),
                GameLocation.tableau(0), GameLocation.foundation(Suit.HEARTS), emptyFoundation));
        assertFalse(rules.canMove(new CardSequence(List.of(aceSpades)),
                GameLocation.tableau(0), GameLocation.foundation(Suit.HEARTS), emptyFoundation));
    }

    private Card visible(Suit suit, Rank rank) {
        Card card = new Card(suit, rank);
        new SetCardVisibilityAction(card, true).execute();
        return card;
    }
}
