package br.com.solitaire.domain.strategy;

import br.com.solitaire.application.GameReadOnlyView;
import br.com.solitaire.domain.model.CardSnapshot;
import br.com.solitaire.domain.model.GameLocation;
import br.com.solitaire.domain.model.GameSnapshot;
import br.com.solitaire.domain.model.MoveSuggestion;
import br.com.solitaire.domain.model.Suit;

import java.util.List;
import java.util.Optional;

/** Implementa a ordem de prioridade definida nos requisitos funcionais. */
public final class PriorityAutoMoveStrategy implements AutoMoveStrategy {

    @Override
    public MoveSuggestion suggest(GameReadOnlyView game) {
        GameSnapshot state = game.snapshot();

        // Prioridade 1: comprar se o descarte está vazio.
        if (state.waste().isEmpty() && !state.stock().isEmpty()) {
            return new MoveSuggestion.DrawFromStock();
        }

        // Prioridade 2: descarte para fundação.
        Optional<CardSnapshot> wasteTop = state.wasteTop();
        if (wasteTop.isPresent()) {
            for (Suit suit : Suit.values()) {
                GameLocation foundation = GameLocation.foundation(suit);
                if (game.canMove(wasteTop.get().card(), foundation)) {
                    return new MoveSuggestion.MoveCard(wasteTop.get().card(), foundation);
                }
            }
        }

        // Prioridade 3: descarte para tableau.
        if (wasteTop.isPresent()) {
            for (int column = 0; column < state.tableau().size(); column++) {
                GameLocation destination = GameLocation.tableau(column);
                if (game.canMove(wasteTop.get().card(), destination)) {
                    return new MoveSuggestion.MoveCard(wasteTop.get().card(), destination);
                }
            }
        }

        // Prioridade 4: topo de uma coluna do tableau para fundação.
        for (List<CardSnapshot> column : state.tableau()) {
            if (column.isEmpty()) {
                continue;
            }
            CardSnapshot top = column.getLast();
            for (Suit suit : Suit.values()) {
                GameLocation foundation = GameLocation.foundation(suit);
                if (game.canMove(top.card(), foundation)) {
                    return new MoveSuggestion.MoveCard(top.card(), foundation);
                }
            }
        }

        // Prioridade 5a: mover uma sequência que revele uma carta oculta.
        for (int sourceIndex = 0; sourceIndex < state.tableau().size(); sourceIndex++) {
            List<CardSnapshot> source = state.tableau().get(sourceIndex);
            for (int cardIndex = 1; cardIndex < source.size(); cardIndex++) {
                boolean wouldReveal = !source.get(cardIndex - 1).faceUp()
                        && source.get(cardIndex).faceUp();
                if (!wouldReveal) {
                    continue;
                }
                CardSnapshot firstMoved = source.get(cardIndex);
                MoveSuggestion suggestion = firstLegalTableauMove(
                        game, state, firstMoved, sourceIndex);
                if (suggestion != null) {
                    return suggestion;
                }
            }
        }

        // Prioridade 5b: mover uma coluna inteira quando isso realmente a esvazia.
        // Um rei que já está na base não é deslocado para outra coluna vazia.
        for (int sourceIndex = 0; sourceIndex < state.tableau().size(); sourceIndex++) {
            List<CardSnapshot> source = state.tableau().get(sourceIndex);
            if (source.isEmpty() || !source.getFirst().faceUp()) {
                continue;
            }
            CardSnapshot firstMoved = source.getFirst();
            MoveSuggestion suggestion = firstLegalTableauMove(game, state, firstMoved, sourceIndex);
            if (suggestion != null) {
                return suggestion;
            }
        }

        // Prioridade 6: comprar quando nenhuma jogada anterior for possível.
        if (!state.stock().isEmpty()) {
            return new MoveSuggestion.DrawFromStock();
        }

        return MoveSuggestion.noOp();
    }

    private MoveSuggestion firstLegalTableauMove(
            GameReadOnlyView game,
            GameSnapshot state,
            CardSnapshot firstMoved,
            int sourceIndex) {
        for (int destinationIndex = 0; destinationIndex < state.tableau().size(); destinationIndex++) {
            if (destinationIndex == sourceIndex) {
                continue;
            }
            boolean destinationIsEmpty = state.tableau().get(destinationIndex).isEmpty();
            if (destinationIsEmpty && game.isVisibleKingAtColumnBase(sourceIndex)) {
                continue;
            }
            GameLocation destination = GameLocation.tableau(destinationIndex);
            if (game.canMove(firstMoved.card(), destination)) {
                return new MoveSuggestion.MoveCard(firstMoved.card(), destination);
            }
        }
        return null;
    }
}
