package br.com.solitaire.domain.strategy;

import br.com.solitaire.application.GameReadOnlyView;
import br.com.solitaire.domain.model.MoveSuggestion;

@FunctionalInterface
public interface AutoMoveStrategy {
    MoveSuggestion suggest(GameReadOnlyView game);
}
