package com.example.exceljson;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        AppController controller = new AppController();
        BorderPane root = controller.buildUI();
        Scene scene = new Scene(root, 1200, 800);
        stage.setTitle("Engage Rules Generator");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
