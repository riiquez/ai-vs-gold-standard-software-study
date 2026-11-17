package br.com.solitaire.domain.model;

/** Contrato de uma ação reversível do jogo. */
public interface GameAction {
    void execute();

    void undo();

    boolean isNoOp();
}
