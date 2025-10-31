package com.example.exceljson;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public class ExcelJsonApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/exceljson/App.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("FlowForge V1.1");
        
        // Load application icon from resources
        try (InputStream iconStream = getClass().getResourceAsStream("/ICON.ico")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            } else {
                System.err.println("Warning: ICON.ico not found in resources. Using default application icon.");
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load ICON.ico: " + e.getMessage());
        }
        
        primaryStage.setScene(new Scene(root, 900, 700));
        primaryStage.show();
    }
}
