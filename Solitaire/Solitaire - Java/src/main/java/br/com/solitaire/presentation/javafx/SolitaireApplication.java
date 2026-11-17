package br.com.solitaire.presentation.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class SolitaireApplication extends Application {
    @Override
    public void start(Stage stage) {
        SolitaireController controller = new SolitaireController();
        Scene scene = new Scene(controller.view().root(), 1180, 780);

        stage.setTitle("Paciência - Solitaire");
        stage.setMinWidth(1020);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
