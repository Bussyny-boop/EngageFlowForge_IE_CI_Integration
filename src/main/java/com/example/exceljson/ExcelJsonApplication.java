package com.example.exceljson;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public class ExcelJsonApplication extends Application {

    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    
    // Icon paths for different sizes - JavaFX will automatically select the best size
    private static final String[] ICON_PATHS = {
        "/icon_16.png",
        "/icon_32.png",
        "/icon_48.png",
        "/icon_64.png",
        "/icon_128.png",
        "/icon_256.png",
        "/icon.png"
    };
    
    // Store reference to stage for window title updates
    private static Stage primaryStageRef;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStageRef = primaryStage;
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/exceljson/App.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Engage FlowForge 2.0");
        
        // Load application icon from resources - use multiple sizes for best quality
        // JavaFX will automatically select the most appropriate size for different contexts
        for (String iconPath : ICON_PATHS) {
            try (InputStream iconInputStream = getClass().getResourceAsStream(iconPath)) {
                if (iconInputStream != null) {
                    primaryStage.getIcons().add(new Image(iconInputStream));
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to load icon from " + iconPath + ": " + e.getMessage());
            }
        }
        
        if (primaryStage.getIcons().isEmpty()) {
            System.err.println("Warning: No icons loaded. Using default application icon.");
        }
        
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        // Load Vocera theme CSS
        try {
            var cssResource = getClass().getResource("/css/vocera-theme.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("Warning: vocera-theme.css not found in resources. Using default styling.");
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load vocera-theme.css: " + e.getMessage());
        }
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * Update the window title with the current file name
     * @param fileName the name of the loaded file, or null to reset to default
     */
    public static void updateWindowTitle(String fileName) {
        if (primaryStageRef != null) {
            if (fileName != null && !fileName.isEmpty()) {
                primaryStageRef.setTitle("Engage FlowForge 2.0 - [" + fileName + "]");
            } else {
                primaryStageRef.setTitle("Engage FlowForge 2.0");
            }
        }
    }
}
