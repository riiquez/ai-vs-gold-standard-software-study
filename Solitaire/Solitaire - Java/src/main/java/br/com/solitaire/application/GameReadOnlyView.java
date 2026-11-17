package br.com.solitaire.application;

import br.com.solitaire.domain.model.Card;
import br.com.solitaire.domain.model.GameLocation;
import br.com.solitaire.domain.model.GameSnapshot;

import java.util.Optional;

/** Consultas disponibilizadas à estratégia, sem operações de alteração de estado. */
public interface GameReadOnlyView {
    GameSnapshot snapshot();

    boolean canMove(Card card, GameLocation destination);

    Optional<GameLocation> findAccessibleCardLocation(Card card);

    boolean isVisibleKingAtColumnBase(int columnIndex);
}
