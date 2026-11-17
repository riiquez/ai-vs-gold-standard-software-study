package br.com.solitaire.application;

import br.com.solitaire.domain.model.MoveSuggestion;
import br.com.solitaire.domain.strategy.AutoMoveStrategy;

import java.util.Objects;

/** Permite habilitar/desabilitar a estratégia sem ramificações no restante do jogo. */
public final class SwitchableAutoMoveStrategy implements AutoMoveStrategy {
    private final AutoMoveStrategy delegate;
    private boolean enabled = true;

    public SwitchableAutoMoveStrategy(AutoMoveStrategy delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public MoveSuggestion suggest(GameReadOnlyView game) {
        return enabled ? delegate.suggest(game) : MoveSuggestion.noOp();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
