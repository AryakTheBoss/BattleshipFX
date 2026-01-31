package com.javamaster44;


import com.javamaster44.ui.GameUI;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        GameUI gameUI = new GameUI();
        Scene scene = new Scene(gameUI.getRoot(), 1000, 700);
        stage.setTitle("Battleship Remastered - Java 21");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
