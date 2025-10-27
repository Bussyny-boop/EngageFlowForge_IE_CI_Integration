package com.example.exceljson;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ExcelJsonApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/exceljson/App.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Engage Rules Generator");
        primaryStage.setScene(new Scene(root, 900, 700));
        primaryStage.show();
    }
}
