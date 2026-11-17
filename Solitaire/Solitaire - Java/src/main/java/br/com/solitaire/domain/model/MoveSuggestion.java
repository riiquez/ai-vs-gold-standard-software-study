package br.com.solitaire.domain.model;

import java.util.Objects;

/** Uma sugestão da estratégia; a execução e a validação pertencem ao jogo. */
public sealed interface MoveSuggestion
        permits MoveSuggestion.DrawFromStock, MoveSuggestion.MoveCard, MoveSuggestion.NoOp {

    record DrawFromStock() implements MoveSuggestion {
    }

    record MoveCard(Card card, GameLocation destination) implements MoveSuggestion {
        public MoveCard {
            Objects.requireNonNull(card, "carta não pode ser nula");
            Objects.requireNonNull(destination, "destino não pode ser nulo");
        }
    }

    record NoOp() implements MoveSuggestion {
    }

    static MoveSuggestion noOp() {
        return new NoOp();
    }
}
