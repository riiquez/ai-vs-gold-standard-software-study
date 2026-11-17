package br.com.solitaire.presentation.javafx;

import br.com.solitaire.application.SolitaireGame;
import br.com.solitaire.domain.model.Card;
import br.com.solitaire.domain.model.GameLocation;
import br.com.solitaire.domain.model.GameSnapshot;

/** Liga eventos da interface aos casos de uso do jogo. */
public final class SolitaireController {
    private final SolitaireGame game;
    private final SolitaireView view;

    public SolitaireController() {
        game = new SolitaireGame();
        view = new SolitaireView();

        view.setOnNewGame(game::newGame);
        view.setOnUndo(game::undo);
        view.setOnBuyOrRecycle(game::buyOrRecycle);
        view.setOnAutomaticStep(game::playAutomaticMove);
        view.setOnAutomaticEnabledChanged(game::setAutomaticStrategyEnabled);
        view.setAutomaticEnabled(game.isAutomaticStrategyEnabled());

        game.addListener(this::render);
        render(game.snapshot());
    }

    public SolitaireView view() {
        return view;
    }

    private void render(GameSnapshot snapshot) {
        view.render(snapshot, game::canMove, this::moveCard);
    }

    private boolean moveCard(Card card, GameLocation destination) {
        return !game.moveCard(card, destination).isNoOp();
    }
}
