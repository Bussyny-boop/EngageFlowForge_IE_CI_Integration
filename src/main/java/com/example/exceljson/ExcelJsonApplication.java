package com.example.exceljson;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ExcelJsonApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Engage Rules Generator");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Failed to load main.fxml. Ensure it’s located under src/main/resources.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
