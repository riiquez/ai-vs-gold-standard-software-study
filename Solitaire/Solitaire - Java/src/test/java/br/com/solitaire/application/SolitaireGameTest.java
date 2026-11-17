package br.com.solitaire.application;

import br.com.solitaire.domain.model.Card;
import br.com.solitaire.domain.model.GameSnapshot;
import br.com.solitaire.domain.model.Suit;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class SolitaireGameTest {

    @Test
    void shouldDealTheInitialKlondikeLayout() {
        SolitaireGame game = new SolitaireGame(RandomGenerator.of("L64X128MixRandom"));
        GameSnapshot state = game.snapshot();

        assertEquals(24, state.stock().size());
        assertTrue(state.waste().isEmpty());
        assertEquals(4, state.foundations().size());
        assertEquals(7, state.tableau().size());
        assertEquals(0, state.score());
        assertFalse(state.won());

        for (int column = 0; column < 7; column++) {
            assertEquals(column + 1, state.tableau().get(column).size());
            for (int cardIndex = 0; cardIndex <= column; cardIndex++) {
                assertEquals(cardIndex == column,
                        state.tableau().get(column).get(cardIndex).faceUp());
            }
        }

        assertEveryCardHasOneIdentityAndOneSuitRankCombination(state);
    }

    @Test
    void shouldDrawAndUndoTheExactSameCard() {
        SolitaireGame game = new SolitaireGame(RandomGenerator.getDefault());
        Card expected = game.snapshot().stockTop().orElseThrow().card();

        assertFalse(game.buyOrRecycle().isNoOp());
        GameSnapshot afterDraw = game.snapshot();
        assertSame(expected, afterDraw.wasteTop().orElseThrow().card());
        assertTrue(afterDraw.wasteTop().orElseThrow().faceUp());
        assertEquals(23, afterDraw.stock().size());

        assertTrue(game.undo());
        GameSnapshot afterUndo = game.snapshot();
        assertSame(expected, afterUndo.stockTop().orElseThrow().card());
        assertFalse(afterUndo.stockTop().orElseThrow().faceUp());
        assertTrue(afterUndo.waste().isEmpty());
        assertEquals(0, afterUndo.historySize());
    }

    @Test
    void shouldRecycleWasteInReverseOrderAndUndoItExactly() {
        SolitaireGame game = new SolitaireGame(RandomGenerator.getDefault());
        for (int draw = 0; draw < 24; draw++) {
            assertFalse(game.buyOrRecycle().isNoOp());
        }

        GameSnapshot beforeRecycle = game.snapshot();
        assertTrue(beforeRecycle.stock().isEmpty());
        assertEquals(24, beforeRecycle.waste().size());

        assertFalse(game.buyOrRecycle().isNoOp());
        GameSnapshot afterRecycle = game.snapshot();
        assertEquals(24, afterRecycle.stock().size());
        assertTrue(afterRecycle.waste().isEmpty());

        for (int index = 0; index < 24; index++) {
            Card originalWasteCard = beforeRecycle.waste().get(index).card();
            Card recycledStockCard = afterRecycle.stock().get(23 - index).card();
            assertSame(originalWasteCard, recycledStockCard);
            assertFalse(afterRecycle.stock().get(23 - index).faceUp());
        }

        Card nextCard = beforeRecycle.waste().getFirst().card();
        game.buyOrRecycle();
        assertSame(nextCard, game.snapshot().wasteTop().orElseThrow().card());
        assertTrue(game.undo());

        assertTrue(game.undo());
        GameSnapshot restored = game.snapshot();
        assertTrue(restored.stock().isEmpty());
        assertEquals(24, restored.waste().size());
        for (int index = 0; index < 24; index++) {
            assertSame(beforeRecycle.waste().get(index).card(), restored.waste().get(index).card());
            assertTrue(restored.waste().get(index).faceUp());
        }
    }

    @Test
    void shouldMoveACardBetweenTableauColumnsAndUndoToTheExactState() {
        SolitaireGame game = new SolitaireGame(new java.util.Random(0));
        GameSnapshot before = game.snapshot();
        Card movingCard = before.tableau().get(0).getFirst().card();
        var destination = br.com.solitaire.domain.model.GameLocation.tableau(4);

        assertTrue(game.canMove(movingCard, destination));
        assertFalse(game.moveCard(movingCard, destination).isNoOp());
        assertSame(movingCard, game.snapshot().tableau().get(4).getLast().card());
        assertTrue(game.snapshot().tableau().get(0).isEmpty());

        assertTrue(game.undo());
        assertEquals(before, game.snapshot());
    }

    @Test
    void shouldRevealTheNextCardAndHideItAgainWhenUndone() {
        SolitaireGame game = new SolitaireGame(new java.util.Random(0));
        GameSnapshot before = game.snapshot();
        var sourceColumn = before.tableau().get(3);
        Card movingCard = sourceColumn.getLast().card();
        Card cardToReveal = sourceColumn.get(sourceColumn.size() - 2).card();
        var destination = br.com.solitaire.domain.model.GameLocation.tableau(4);

        assertFalse(sourceColumn.get(sourceColumn.size() - 2).faceUp());
        assertTrue(game.canMove(movingCard, destination));
        game.moveCard(movingCard, destination);

        GameSnapshot afterMove = game.snapshot();
        assertSame(cardToReveal, afterMove.tableau().get(3).getLast().card());
        assertTrue(afterMove.tableau().get(3).getLast().faceUp());
        assertFalse(before.tableau().get(3).get(sourceColumn.size() - 2).faceUp(),
                "Uma fotografia anterior não deve ser alterada por uma jogada posterior");

        assertTrue(game.undo());
        GameSnapshot afterUndo = game.snapshot();
        assertSame(movingCard, afterUndo.tableau().get(3).getLast().card());
        assertFalse(afterUndo.tableau().get(3).get(sourceColumn.size() - 2).faceUp());
        assertEquals(before, afterUndo);
    }

    @Test
    void shouldUndoALongHistoryBackToTheExactInitialStateAndContinuePlaying() {
        SolitaireGame game = new SolitaireGame(new java.util.Random(17));
        GameSnapshot initial = game.snapshot();

        for (int draw = 0; draw < 24; draw++) {
            game.buyOrRecycle();
        }
        game.buyOrRecycle(); // recicla o descarte para o monte

        for (int action = 0; action < 25; action++) {
            assertTrue(game.undo());
        }

        assertEquals(initial, game.snapshot());
        assertTrue(game.playAutomaticMove());
        assertEquals(1, game.snapshot().waste().size());
    }

    @Test
    void shouldNotifyListenersAfterChangesAndUndo() {
        SolitaireGame game = new SolitaireGame(RandomGenerator.getDefault());
        int[] notifications = {0};
        game.addListener(snapshot -> notifications[0]++);

        game.buyOrRecycle();
        game.undo();

        assertEquals(2, notifications[0]);
    }

    @Test
    void disabledAutomaticStrategyShouldReturnWithoutChangingState() {
        SolitaireGame game = new SolitaireGame(RandomGenerator.getDefault());
        game.setAutomaticStrategyEnabled(false);
        GameSnapshot before = game.snapshot();

        assertFalse(game.playAutomaticMove());
        assertEquals(before, game.snapshot());
    }

    @Test
    void automaticStrategyShouldChooseTheFirstPriorityWhenWasteIsEmpty() {
        SolitaireGame game = new SolitaireGame(RandomGenerator.getDefault());
        assertTrue(game.playAutomaticMove());

        GameSnapshot state = game.snapshot();
        assertEquals(23, state.stock().size());
        assertEquals(1, state.waste().size());
        assertEquals(1, state.historySize());
    }

    @Test
    void newGameShouldResetTheBoardAndClearHistory() {
        SolitaireGame game = new SolitaireGame(RandomGenerator.getDefault());
        game.buyOrRecycle();
        game.newGame();

        GameSnapshot state = game.snapshot();
        assertEquals(24, state.stock().size());
        assertTrue(state.waste().isEmpty());
        assertEquals(0, state.historySize());
        assertEquals(0, state.score());
    }

    private void assertEveryCardHasOneIdentityAndOneSuitRankCombination(GameSnapshot state) {
        Set<Card> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> combinations = new java.util.HashSet<>();

        state.stock().forEach(card -> add(card.card(), identities, combinations));
        state.waste().forEach(card -> add(card.card(), identities, combinations));
        state.foundations().values().forEach(pile ->
                pile.forEach(card -> add(card.card(), identities, combinations)));
        state.tableau().forEach(column ->
                column.forEach(card -> add(card.card(), identities, combinations)));

        assertEquals(52, identities.size());
        assertEquals(52, combinations.size());
    }

    private void add(Card card, Set<Card> identities, Set<String> combinations) {
        assertTrue(identities.add(card), "A mesma instância de carta apareceu em mais de um local");
        assertTrue(combinations.add(card.suit() + "-" + card.rank()),
                "Combinação de naipe e rank duplicada");
    }
}
