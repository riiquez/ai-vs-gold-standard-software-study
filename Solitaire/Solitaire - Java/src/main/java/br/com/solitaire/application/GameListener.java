package br.com.solitaire.application;

import br.com.solitaire.domain.model.GameSnapshot;

@FunctionalInterface
public interface GameListener {
    void onGameChanged(GameSnapshot snapshot);
}
