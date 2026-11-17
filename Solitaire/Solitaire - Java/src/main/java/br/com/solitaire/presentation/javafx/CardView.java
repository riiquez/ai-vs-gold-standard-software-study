package br.com.solitaire.presentation.javafx;

import br.com.solitaire.domain.model.CardSnapshot;
import br.com.solitaire.domain.model.CardColor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Representação visual de uma carta. Não contém regras de movimentação. */
final class CardView extends StackPane {
    static final double WIDTH = 96;
    static final double HEIGHT = 132;

    private final CardSnapshot cardSnapshot;

    CardView(CardSnapshot cardSnapshot) {
        this.cardSnapshot = cardSnapshot;
        setMinSize(WIDTH, HEIGHT);
        setPrefSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(7));
        setCardAppearance();
    }

    CardSnapshot cardSnapshot() {
        return cardSnapshot;
    }

    private void setCardAppearance() {
        if (!cardSnapshot.faceUp()) {
            setStyle("""
                    -fx-background-color: linear-gradient(to bottom right, #244c86, #17365e);
                    -fx-background-radius: 9;
                    -fx-border-color: #d6e1ed;
                    -fx-border-width: 2;
                    -fx-border-radius: 9;
                    """);
            Label backPattern = new Label("✦  ✦  ✦\n✦  ♠  ✦\n✦  ✦  ✦");
            backPattern.setAlignment(Pos.CENTER);
            backPattern.setStyle("-fx-text-fill: #d5e2f3; -fx-font-size: 17px; -fx-font-weight: bold;");
            getChildren().add(backPattern);
            return;
        }

        String color = cardSnapshot.suit().color() == CardColor.RED ? "#c7394d" : "#202936";
        setStyle("""
                -fx-background-color: #fffefa;
                -fx-background-radius: 9;
                -fx-border-color: #d4d8dc;
                -fx-border-width: 1.2;
                -fx-border-radius: 9;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 5, 0.18, 1, 2);
                """);

        Label rankAndSuit = new Label(cardSnapshot.rank().symbol() + " " + cardSnapshot.suit().symbol());
        rankAndSuit.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 17px; -fx-font-weight: bold;");

        Label largeSuit = new Label(cardSnapshot.suit().symbol());
        largeSuit.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 43px;");

        VBox content = new VBox(13, rankAndSuit, largeSuit);
        content.setAlignment(Pos.CENTER);
        getChildren().add(content);
    }
}
