package br.com.solitaire.domain.rules;

import br.com.solitaire.domain.model.Card;
import br.com.solitaire.domain.model.CardColor;
import br.com.solitaire.domain.model.CardPile;
import br.com.solitaire.domain.model.CardSequence;
import br.com.solitaire.domain.model.GameLocation;
import br.com.solitaire.domain.model.LocationType;
import br.com.solitaire.domain.model.Rank;
import br.com.solitaire.domain.model.Suit;

import java.util.List;
import java.util.Objects;

/** Regras únicas de legalidade para todas as formas de movimentação. */
public final class MoveRules {

    public boolean canMove(
            CardSequence sequence,
            GameLocation source,
            GameLocation destination,
            CardPile destinationPile) {

        Objects.requireNonNull(sequence);
        Objects.requireNonNull(source);
        Objects.requireNonNull(destination);
        Objects.requireNonNull(destinationPile);

        if (source.equals(destination) || !isValidFaceUpSequence(sequence)) {
            return false;
        }

        return switch (destination.type()) {
            case TABLEAU -> canPlaceOnTableau(sequence.first(), destinationPile);
            case FOUNDATION -> sequence.size() == 1
                    && canPlaceOnFoundation(sequence.first(), destination.foundationSuit(), destinationPile);
            case STOCK, WASTE -> false;
        };
    }

    public boolean canPlaceOnTableau(Card card, CardPile destinationPile) {
        Objects.requireNonNull(card);
        Objects.requireNonNull(destinationPile);

        if (destinationPile.isEmpty()) {
            return card.rank() == Rank.KING;
        }

        Card destinationTop = destinationPile.top().orElseThrow();
        return card.rank().isImmediatelyBelow(destinationTop.rank())
                && card.suit().color() != destinationTop.suit().color();
    }

    public boolean canPlaceOnFoundation(Card card, Suit foundationSuit, CardPile destinationPile) {
        Objects.requireNonNull(card);
        Objects.requireNonNull(foundationSuit);
        Objects.requireNonNull(destinationPile);

        if (card.suit() != foundationSuit) {
            return false;
        }

        if (destinationPile.isEmpty()) {
            return card.rank() == Rank.ACE;
        }

        Card destinationTop = destinationPile.top().orElseThrow();
        return card.suit() == destinationTop.suit()
                && card.rank().isImmediatelyAbove(destinationTop.rank());
    }

    private boolean isValidFaceUpSequence(CardSequence sequence) {
        List<Card> cards = sequence.cards();
        for (int index = 0; index < cards.size(); index++) {
            Card current = cards.get(index);
            if (!current.isFaceUp()) {
                return false;
            }

            if (index > 0) {
                Card below = cards.get(index - 1);
                if (!current.rank().isImmediatelyBelow(below.rank())
                        || current.suit().color() == below.suit().color()) {
                    return false;
                }
            }
        }
        return true;
    }
}
