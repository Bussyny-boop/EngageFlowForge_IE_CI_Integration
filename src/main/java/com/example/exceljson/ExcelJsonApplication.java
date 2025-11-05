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
        // Try uppercase first (local override), then lowercase (tracked in git)
        String iconPath = getClass().getResource("/ICON.ico") != null ? "/ICON.ico" : "/icon.ico";
        
        try (InputStream iconInputStream = getClass().getResourceAsStream(iconPath)) {
            if (iconInputStream != null) {
                primaryStage.getIcons().add(new Image(iconInputStream));
            } else {
                System.err.println("Warning: icon.ico not found in resources. Using default application icon.");
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load icon: " + e.getMessage());
        }
        
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        // Load Stryker theme CSS
        try {
            var cssResource = getClass().getResource("/css/stryker-theme.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("Warning: stryker-theme.css not found in resources. Using default styling.");
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load stryker-theme.css: " + e.getMessage());
        }
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
