package br.com.solitaire.application;

import br.com.solitaire.domain.model.Card;
import br.com.solitaire.domain.model.CardPile;
import br.com.solitaire.domain.model.CardSequence;
import br.com.solitaire.domain.model.CardSnapshot;
import br.com.solitaire.domain.model.CompositeAction;
import br.com.solitaire.domain.model.GameAction;
import br.com.solitaire.domain.model.GameLocation;
import br.com.solitaire.domain.model.GameSnapshot;
import br.com.solitaire.domain.model.LocationType;
import br.com.solitaire.domain.model.MoveSuggestion;
import br.com.solitaire.domain.model.NoOpAction;
import br.com.solitaire.domain.model.RecycleWasteAction;
import br.com.solitaire.domain.model.SetCardVisibilityAction;
import br.com.solitaire.domain.model.Suit;
import br.com.solitaire.domain.model.TransferCardsAction;
import br.com.solitaire.domain.rules.MoveRules;
import br.com.solitaire.domain.strategy.PriorityAutoMoveStrategy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.random.RandomGenerator;

/**
 * Fachada central do domínio: mantém o estado, valida jogadas, registra ações,
 * desfaz alterações e notifica os observadores.
 */
public final class SolitaireGame implements GameReadOnlyView {
    private static final int TABLEAU_COLUMNS = 7;

    private final RandomGenerator random;
    private final MoveRules moveRules = new MoveRules();
    private final SwitchableAutoMoveStrategy autoMoveStrategy;
    private final List<GameListener> listeners = new CopyOnWriteArrayList<>();
    private final Deque<GameAction> history = new ArrayDeque<>();

    private CardPile stock;
    private CardPile waste;
    private Map<Suit, CardPile> foundations;
    private List<CardPile> tableau;

    public SolitaireGame() {
        this(RandomGenerator.getDefault());
    }

    public SolitaireGame(RandomGenerator random) {
        this.random = Objects.requireNonNull(random);
        this.autoMoveStrategy = new SwitchableAutoMoveStrategy(new PriorityAutoMoveStrategy());
        newGame();
    }

    public void addListener(GameListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    /** Cria e distribui uma nova partida. */
    public void newGame() {
        stock = new CardPile();
        waste = new CardPile();
        foundations = new EnumMap<>(Suit.class);
        tableau = new ArrayList<>(TABLEAU_COLUMNS);
        history.clear();

        for (Suit suit : Suit.values()) {
            foundations.put(suit, new CardPile());
        }
        for (int columnIndex = 0; columnIndex < TABLEAU_COLUMNS; columnIndex++) {
            tableau.add(new CardPile());
        }

        List<Card> shuffledDeck = DeckFactory.shuffledDeck(random);
        stock.pushAll(shuffledDeck);

        for (int columnIndex = 0; columnIndex < TABLEAU_COLUMNS; columnIndex++) {
            CardPile column = tableau.get(columnIndex);
            for (int cardIndex = 0; cardIndex <= columnIndex; cardIndex++) {
                Card card = stock.pop();
                if (cardIndex == columnIndex) {
                    new SetCardVisibilityAction(card, true).execute();
                }
                column.push(card);
            }
        }

        notifyListeners();
    }

    /**
     * A ação do espaço do monte: compra, recicla o descarte ou inicia uma
     * nova partida quando não restam cartas nem no monte nem no descarte.
     */
    public GameAction buyOrRecycle() {
        if (!stock.isEmpty()) {
            return drawFromStock();
        }
        if (!waste.isEmpty()) {
            return perform(new RecycleWasteAction(stock, waste));
        }
        newGame();
        return NoOpAction.INSTANCE;
    }

    public GameAction drawFromStock() {
        if (stock.isEmpty()) {
            return NoOpAction.INSTANCE;
        }

        Card card = stock.top().orElseThrow();
        GameAction action = new CompositeAction(List.of(
                new TransferCardsAction(stock, waste, new CardSequence(List.of(card))),
                new SetCardVisibilityAction(card, true)));
        return perform(action);
    }

    /** Executa uma movimentação solicitada pelo jogador. */
    public GameAction moveCard(Card card, GameLocation destination) {
        Objects.requireNonNull(card);
        Objects.requireNonNull(destination);

        Optional<GameLocation> sourceOptional = findAccessibleCardLocation(card);
        if (sourceOptional.isEmpty()) {
            return NoOpAction.INSTANCE;
        }

        GameLocation source = sourceOptional.get();
        CardPile sourcePile = pileAt(source);
        List<Card> cardsToMove = source.type() == LocationType.TABLEAU
                ? sourcePile.sequenceFrom(card)
                : List.of(card);
        if (cardsToMove.isEmpty()) {
            return NoOpAction.INSTANCE;
        }

        CardSequence sequence = new CardSequence(cardsToMove);
        CardPile destinationPile = pileAt(destination);
        if (!moveRules.canMove(sequence, source, destination, destinationPile)) {
            return NoOpAction.INSTANCE;
        }

        List<GameAction> actions = new ArrayList<>();
        actions.add(new TransferCardsAction(sourcePile, destinationPile, sequence));

        if (source.type() == LocationType.TABLEAU) {
            List<Card> sourceCards = sourcePile.cards();
            int firstMovedIndex = sourceCards.size() - sequence.size();
            if (firstMovedIndex > 0) {
                Card cardToReveal = sourceCards.get(firstMovedIndex - 1);
                if (!cardToReveal.isFaceUp()) {
                    actions.add(new SetCardVisibilityAction(cardToReveal, true));
                }
            }
        }

        return perform(new CompositeAction(actions));
    }

    /** Verifica a legalidade sem alterar o estado. Usado pela interface durante o arraste. */
    @Override
    public boolean canMove(Card card, GameLocation destination) {
        if (card == null || destination == null) {
            return false;
        }
        Optional<GameLocation> sourceOptional = findAccessibleCardLocation(card);
        if (sourceOptional.isEmpty()) {
            return false;
        }

        GameLocation source = sourceOptional.get();
        CardPile sourcePile = pileAt(source);
        List<Card> cardsToMove = source.type() == LocationType.TABLEAU
                ? sourcePile.sequenceFrom(card)
                : List.of(card);
        if (cardsToMove.isEmpty()) {
            return false;
        }
        return moveRules.canMove(
                new CardSequence(cardsToMove), source, destination, pileAt(destination));
    }

    /** Local de origem em que a carta está acessível para movimentação. */
    @Override
    public Optional<GameLocation> findAccessibleCardLocation(Card card) {
        if (card == null) {
            return Optional.empty();
        }

        if (waste.top().orElse(null) == card) {
            return Optional.of(GameLocation.waste());
        }

        for (Suit suit : Suit.values()) {
            if (foundations.get(suit).top().orElse(null) == card) {
                return Optional.of(GameLocation.foundation(suit));
            }
        }

        for (int index = 0; index < tableau.size(); index++) {
            CardPile column = tableau.get(index);
            if (card.isFaceUp() && column.containsIdentity(card)) {
                return Optional.of(GameLocation.tableau(index));
            }
        }

        return Optional.empty();
    }

    /** Consulta a sequência que seria movida ao selecionar uma carta do tableau. */
    public Optional<CardSequence> sequenceFrom(Card card) {
        Optional<GameLocation> location = findAccessibleCardLocation(card);
        if (location.isEmpty() || location.get().type() != LocationType.TABLEAU) {
            return Optional.empty();
        }
        List<Card> sequence = pileAt(location.get()).sequenceFrom(card);
        return sequence.isEmpty() ? Optional.empty() : Optional.of(new CardSequence(sequence));
    }

    /** Indica se a carta é a primeira carta visível a partir da base da coluna. */
    public boolean isVisibleClosestToBase(Card card) {
        if (card == null || !card.isFaceUp()) {
            return false;
        }
        Optional<GameLocation> location = findAccessibleCardLocation(card);
        if (location.isEmpty() || location.get().type() != LocationType.TABLEAU) {
            return false;
        }
        List<Card> cards = pileAt(location.get()).cards();
        int cardIndex = indexOfIdentity(cards, card);
        if (cardIndex < 0) {
            return false;
        }
        for (int index = 0; index < cardIndex; index++) {
            if (cards.get(index).isFaceUp()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isVisibleKingAtColumnBase(int columnIndex) {
        CardPile column = tableau.get(columnIndex);
        if (column.isEmpty()) {
            return false;
        }
        Card baseCard = column.cards().getFirst();
        return baseCard.isFaceUp() && baseCard.rank() == br.com.solitaire.domain.model.Rank.KING;
    }

    /** Executa a sugestão após validar novamente sua legalidade no mecanismo central. */
    public GameAction executeSuggestion(MoveSuggestion suggestion) {
        Objects.requireNonNull(suggestion);
        return switch (suggestion) {
            case MoveSuggestion.DrawFromStock ignored -> drawFromStock();
            case MoveSuggestion.MoveCard move -> moveCard(move.card(), move.destination());
            case MoveSuggestion.NoOp ignored -> NoOpAction.INSTANCE;
        };
    }

    /** Executa uma única etapa automática. Retorna false quando nada foi alterado. */
    public boolean playAutomaticMove() {
        MoveSuggestion suggestion = autoMoveStrategy.suggest(this);
        return !executeSuggestion(suggestion).isNoOp();
    }

    public void setAutomaticStrategyEnabled(boolean enabled) {
        autoMoveStrategy.setEnabled(enabled);
    }

    public boolean isAutomaticStrategyEnabled() {
        return autoMoveStrategy.isEnabled();
    }

    public boolean undo() {
        if (history.isEmpty()) {
            return false;
        }
        GameAction action = history.removeLast();
        action.undo();
        notifyListeners();
        return true;
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public int score() {
        return foundations.values().stream().mapToInt(CardPile::size).sum();
    }

    public boolean isWon() {
        return score() == 52;
    }

    @Override
    public GameSnapshot snapshot() {
        EnumMap<Suit, List<CardSnapshot>> foundationSnapshots = new EnumMap<>(Suit.class);
        for (Suit suit : Suit.values()) {
            foundationSnapshots.put(suit, snapshotsOf(foundations.get(suit)));
        }

        List<List<CardSnapshot>> tableauSnapshots = tableau.stream()
                .map(this::snapshotsOf)
                .toList();

        return new GameSnapshot(
                snapshotsOf(stock),
                snapshotsOf(waste),
                foundationSnapshots,
                tableauSnapshots,
                history.size(),
                score(),
                isWon());
    }

    private GameAction perform(GameAction action) {
        if (action.isNoOp()) {
            return NoOpAction.INSTANCE;
        }
        action.execute();
        history.addLast(action);
        notifyListeners();
        return action;
    }

    private CardPile pileAt(GameLocation location) {
        Objects.requireNonNull(location);
        return switch (location.type()) {
            case STOCK -> stock;
            case WASTE -> waste;
            case FOUNDATION -> foundations.get(location.foundationSuit());
            case TABLEAU -> tableau.get(location.index());
        };
    }

    private List<CardSnapshot> snapshotsOf(CardPile pile) {
        return pile.cards().stream()
                .map(card -> new CardSnapshot(card, card.isFaceUp()))
                .toList();
    }

    private int indexOfIdentity(List<Card> cards, Card expected) {
        for (int index = 0; index < cards.size(); index++) {
            if (cards.get(index) == expected) {
                return index;
            }
        }
        return -1;
    }

    private void notifyListeners() {
        GameSnapshot snapshot = snapshot();
        for (GameListener listener : listeners) {
            listener.onGameChanged(snapshot);
        }
    }
}
