package com.example.exceljson;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public class ExcelJsonApplication extends Application {

    private static final int WINDOW_WIDTH = 1100;
    private static final int WINDOW_HEIGHT = 750;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/exceljson/App.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Engage FlowForge 2.0");
        
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
        
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(
            getClass().getResource("/css/stryker-theme.css").toExternalForm()
        );
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
