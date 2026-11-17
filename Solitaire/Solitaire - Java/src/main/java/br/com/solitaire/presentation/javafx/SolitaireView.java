package br.com.solitaire.presentation.javafx;

import br.com.solitaire.domain.model.Card;
import br.com.solitaire.domain.model.CardSnapshot;
import br.com.solitaire.domain.model.GameLocation;
import br.com.solitaire.domain.model.GameSnapshot;
import br.com.solitaire.domain.model.Suit;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/** Monta a interface e encaminha interações ao mecanismo central do jogo. */
public final class SolitaireView {
    private final BorderPane root = new BorderPane();
    private final StackPane stockSlot = new StackPane();
    private final StackPane wasteSlot = new StackPane();
    private final Map<Suit, StackPane> foundationSlots = new EnumMap<>(Suit.class);
    private final List<Pane> tableauSlots = new ArrayList<>();
    private final Map<String, Card> cardsById = new HashMap<>();

    private final Label stockCount = new Label();
    private final Label wasteCount = new Label();
    private final Label statusLabel = new Label();
    private final Button undoButton = new Button("Desfazer");
    private final CheckBox automaticEnabled = new CheckBox("Estratégia habilitada");
    private final Button automaticStepButton = new Button("Próxima jogada automática");

    private Runnable onNewGame = () -> { };
    private Runnable onUndo = () -> { };
    private Runnable onBuyOrRecycle = () -> { };
    private Runnable onAutomaticStep = () -> { };
    private java.util.function.Consumer<Boolean> onAutomaticEnabledChanged = ignored -> { };

    public SolitaireView() {
        buildLayout();
    }

    public BorderPane root() {
        return root;
    }

    public void setOnNewGame(Runnable handler) {
        onNewGame = handler;
    }

    public void setOnUndo(Runnable handler) {
        onUndo = handler;
    }

    public void setOnBuyOrRecycle(Runnable handler) {
        onBuyOrRecycle = handler;
    }

    public void setOnAutomaticStep(Runnable handler) {
        onAutomaticStep = handler;
    }

    public void setOnAutomaticEnabledChanged(java.util.function.Consumer<Boolean> handler) {
        onAutomaticEnabledChanged = handler;
    }

    public void setAutomaticEnabled(boolean enabled) {
        automaticEnabled.setSelected(enabled);
        automaticStepButton.setDisable(!enabled);
    }

    public void render(
            GameSnapshot snapshot,
            BiPredicate<Card, GameLocation> canMove,
            BiPredicate<Card, GameLocation> moveCard) {
        cardsById.clear();
        registerCards(snapshot);

        renderStock(snapshot);
        renderWaste(snapshot);
        renderFoundations(snapshot, canMove, moveCard);
        renderTableau(snapshot, canMove, moveCard);

        stockCount.setText("Monte: " + snapshot.stock().size());
        wasteCount.setText("Descarte: " + snapshot.waste().size());
        undoButton.setDisable(snapshot.historySize() == 0);
        automaticStepButton.setDisable(!automaticEnabled.isSelected());
        statusLabel.setText(snapshot.won()
                ? "Você venceu! As 52 cartas estão nas fundações."
                : "Arraste uma carta (ou sequência) para um destino válido.");
    }

    private void buildLayout() {
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #176b4c, #0c513b);");

        Label title = new Label("Paciência");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 29px; -fx-font-weight: bold;");

        Button newGameButton = new Button("Nova partida");
        newGameButton.setOnAction(event -> onNewGame.run());
        stylePrimaryButton(newGameButton);

        undoButton.setOnAction(event -> onUndo.run());
        styleSecondaryButton(undoButton);

        automaticStepButton.setOnAction(event -> onAutomaticStep.run());
        styleSecondaryButton(automaticStepButton);

        automaticEnabled.setSelected(true);
        automaticEnabled.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        automaticEnabled.selectedProperty().addListener((observable, oldValue, newValue) -> {
            automaticStepButton.setDisable(!newValue);
            onAutomaticEnabledChanged.accept(newValue);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(12, title, spacer, automaticEnabled,
                automaticStepButton, undoButton, newGameButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        HBox topPiles = buildTopPiles();
        topPiles.setPadding(new Insets(24, 0, 25, 0));

        HBox columns = new HBox(12);
        columns.setAlignment(Pos.TOP_CENTER);
        columns.setPadding(new Insets(0, 0, 16, 0));
        for (int index = 0; index < 7; index++) {
            Pane column = new Pane();
            column.setMinSize(CardView.WIDTH, 430);
            column.setPrefSize(CardView.WIDTH, 430);
            column.setMaxSize(CardView.WIDTH, 430);
            styleEmptySlot(column);
            tableauSlots.add(column);
            columns.getChildren().add(column);
        }

        ScrollPane tableauScroll = new ScrollPane(columns);
        tableauScroll.setFitToHeight(true);
        tableauScroll.setFitToWidth(true);
        tableauScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tableauScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tableauScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        tableauScroll.setPadding(new Insets(0));
        VBox.setVgrow(tableauScroll, Priority.ALWAYS);

        statusLabel.setStyle("-fx-text-fill: #e9f6ef; -fx-font-size: 13px;");
        HBox footer = new HBox(statusLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(12, 0, 0, 0));

        VBox content = new VBox(8, toolbar, topPiles, tableauScroll, footer);
        VBox.setVgrow(tableauScroll, Priority.ALWAYS);
        root.setCenter(content);
    }

    private HBox buildTopPiles() {
        VBox stockContainer = new VBox(7, stockCount, stockSlot);
        stockContainer.setAlignment(Pos.TOP_CENTER);
        stockCount.setStyle("-fx-text-fill: #f0faf4; -fx-font-size: 12px;");
        stockSlot.setMinSize(CardView.WIDTH, CardView.HEIGHT);
        stockSlot.setPrefSize(CardView.WIDTH, CardView.HEIGHT);
        stockSlot.setMaxSize(CardView.WIDTH, CardView.HEIGHT);
        stockSlot.setOnMouseClicked(event -> onBuyOrRecycle.run());
        stockSlot.setOnMouseEntered(event -> stockSlot.setOpacity(0.86));
        stockSlot.setOnMouseExited(event -> stockSlot.setOpacity(1));
        stockSlot.setStyle("-fx-cursor: hand;");

        VBox wasteContainer = new VBox(7, wasteCount, wasteSlot);
        wasteContainer.setAlignment(Pos.TOP_CENTER);
        wasteCount.setStyle("-fx-text-fill: #f0faf4; -fx-font-size: 12px;");
        wasteSlot.setMinSize(CardView.WIDTH, CardView.HEIGHT);
        wasteSlot.setPrefSize(CardView.WIDTH, CardView.HEIGHT);
        wasteSlot.setMaxSize(CardView.WIDTH, CardView.HEIGHT);
        styleEmptySlot(wasteSlot);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox foundationsRow = new HBox(12);
        foundationsRow.setAlignment(Pos.TOP_CENTER);
        for (Suit suit : Suit.values()) {
            StackPane slot = new StackPane();
            slot.setMinSize(CardView.WIDTH, CardView.HEIGHT);
            slot.setPrefSize(CardView.WIDTH, CardView.HEIGHT);
            slot.setMaxSize(CardView.WIDTH, CardView.HEIGHT);
            styleEmptySlot(slot);
            foundationSlots.put(suit, slot);

            VBox foundationContainer = new VBox(7,
                    labelFor("Fundação " + suit.symbol()), slot);
            foundationContainer.setAlignment(Pos.TOP_CENTER);
            foundationsRow.getChildren().add(foundationContainer);
        }

        HBox top = new HBox(16, stockContainer, wasteContainer, spacer, foundationsRow);
        top.setAlignment(Pos.TOP_LEFT);
        return top;
    }

    private Label labelFor(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #f0faf4; -fx-font-size: 12px;");
        return label;
    }

    private void renderStock(GameSnapshot snapshot) {
        stockSlot.getChildren().clear();
        stockSlot.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        if (!snapshot.stock().isEmpty()) {
            CardSnapshot top = snapshot.stock().getLast();
            stockSlot.getChildren().add(new CardView(top));
        } else {
            Label emptyAction = new Label(snapshot.waste().isEmpty() ? "+" : "↶");
            emptyAction.setStyle("-fx-text-fill: #eaf6ef; -fx-font-size: 32px; -fx-font-weight: bold;");
            stockSlot.getChildren().add(emptyAction);
            styleEmptySlot(stockSlot);
        }
    }

    private void renderWaste(GameSnapshot snapshot) {
        wasteSlot.getChildren().clear();
        wasteSlot.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        if (snapshot.waste().isEmpty()) {
            styleEmptySlot(wasteSlot);
            return;
        }
        CardSnapshot top = snapshot.waste().getLast();
        CardView cardView = new CardView(top);
        configureDraggable(cardView);
        wasteSlot.getChildren().add(cardView);
    }

    private void renderFoundations(
            GameSnapshot snapshot,
            BiPredicate<Card, GameLocation> canMove,
            BiPredicate<Card, GameLocation> moveCard) {
        for (Suit suit : Suit.values()) {
            StackPane slot = foundationSlots.get(suit);
            slot.getChildren().clear();
            slot.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            List<CardSnapshot> pile = snapshot.foundations().get(suit);
            GameLocation destination = GameLocation.foundation(suit);
            styleDropTarget(slot, destination, canMove, moveCard, true);

            if (pile.isEmpty()) {
                Label symbol = new Label(suit.symbol());
                symbol.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 34px;");
                slot.getChildren().add(symbol);
            } else {
                CardView cardView = new CardView(pile.getLast());
                configureDraggable(cardView);
                styleDropTarget(cardView, destination, canMove, moveCard, true);
                slot.getChildren().add(cardView);
            }
        }
    }

    private void renderTableau(
            GameSnapshot snapshot,
            BiPredicate<Card, GameLocation> canMove,
            BiPredicate<Card, GameLocation> moveCard) {
        for (int columnIndex = 0; columnIndex < tableauSlots.size(); columnIndex++) {
            Pane columnPane = tableauSlots.get(columnIndex);
            columnPane.getChildren().clear();
            List<CardSnapshot> cards = snapshot.tableau().get(columnIndex);
            if (cards.isEmpty()) {
                styleEmptySlot(columnPane);
            } else {
                columnPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            }
            GameLocation destination = GameLocation.tableau(columnIndex);
            styleDropTarget(columnPane, destination, canMove, moveCard, true);

            double y = 0;
            for (CardSnapshot cardSnapshot : cards) {
                CardView cardView = new CardView(cardSnapshot);
                cardView.setLayoutX(0);
                cardView.setLayoutY(y);
                columnPane.getChildren().add(cardView);
                if (cardSnapshot.faceUp()) {
                    configureDraggable(cardView);
                }
                styleDropTarget(cardView, destination, canMove, moveCard, true);
                y += cardSnapshot.faceUp() ? 28 : 19;
            }
        }
    }

    private void registerCards(GameSnapshot snapshot) {
        snapshot.stock().forEach(card -> cardsById.put(card.id(), card.card()));
        snapshot.waste().forEach(card -> cardsById.put(card.id(), card.card()));
        snapshot.foundations().values().forEach(pile ->
                pile.forEach(card -> cardsById.put(card.id(), card.card())));
        snapshot.tableau().forEach(column ->
                column.forEach(card -> cardsById.put(card.id(), card.card())));
    }

    private void configureDraggable(CardView cardView) {
        if (!cardView.cardSnapshot().faceUp()) {
            return;
        }
        cardView.setOnDragDetected(event -> {
            Dragboard dragboard = cardView.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(cardView.cardSnapshot().id());
            dragboard.setContent(content);
            event.consume();
        });
    }

    private void styleDropTarget(
            Node node,
            GameLocation destination,
            BiPredicate<Card, GameLocation> canMove,
            BiPredicate<Card, GameLocation> moveCard,
            boolean enabled) {
        if (!enabled) {
            return;
        }
        node.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            Card draggedCard = dragboard.hasString() ? cardsById.get(dragboard.getString()) : null;
            boolean legal = draggedCard != null && canMove.test(draggedCard, destination);
            if (legal) {
                event.acceptTransferModes(TransferMode.MOVE);
                node.setOpacity(0.78);
            }
            event.consume();
        });
        node.setOnDragExited(event -> node.setOpacity(1));
        node.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            Card draggedCard = dragboard.hasString() ? cardsById.get(dragboard.getString()) : null;
            boolean moved = draggedCard != null && moveCard.test(draggedCard, destination);
            event.setDropCompleted(moved);
            event.consume();
        });
    }

    private void styleEmptySlot(Node node) {
        if (node instanceof Region region) {
            region.setStyle("""
                    -fx-background-color: rgba(5, 55, 40, 0.16);
                    -fx-background-radius: 9;
                    -fx-border-color: rgba(233, 248, 239, 0.36);
                    -fx-border-width: 1.4;
                    -fx-border-radius: 9;
                    -fx-border-style: dashed;
                    """);
        }
    }

    private void stylePrimaryButton(Button button) {
        button.setStyle("-fx-background-color: #f0c66b; -fx-text-fill: #24372f; " +
                "-fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 9 15 9 15;");
    }

    private void styleSecondaryButton(Button button) {
        button.setStyle("-fx-background-color: rgba(255,255,255,0.16); -fx-text-fill: white; " +
                "-fx-border-color: rgba(255,255,255,0.52); -fx-border-radius: 7; " +
                "-fx-background-radius: 7; -fx-padding: 8 13 8 13;");
    }
}
