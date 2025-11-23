package com.example.exceljson;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.stage.Modality;

import java.io.InputStream;
import java.util.prefs.Preferences;
import java.util.Optional;

public class ExcelJsonApplication extends Application {

    private static final int WINDOW_WIDTH = 1100;
    private static final int WINDOW_HEIGHT = 750;
    
    // Icon paths for different sizes - JavaFX will automatically select the best size
    private static final String[] ICON_PATHS = {
        "/icon_16.png",
        "/icon_32.png",
        "/icon_48.png",
        "/icon_64.png",
        "/icon_128.png",
        "/icon.png"
    };

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Show role selection dialog first
        UserProfile selectedProfile = showRoleSelectionDialog();
        if (selectedProfile == null) {
            // User closed the dialog without selecting - exit application
            return;
        }
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/exceljson/App.fxml"));
        Parent root = loader.load();
        
        // Get the controller and set the user profile
        AppController controller = loader.getController();
        controller.setUserProfile(selectedProfile);

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
        
        // Load theme based on user preference
        Preferences prefs = Preferences.userNodeForPackage(AppController.class);
        boolean isDarkMode = prefs.getBoolean("darkMode", false);
        
        String themePath = isDarkMode ? "/css/dark-theme.css" : "/css/vocera-theme.css";
        
        try {
            var cssResource = getClass().getResource(themePath);
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("Warning: " + themePath + " not found in resources. Using default styling.");
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load " + themePath + ": " + e.getMessage());
        }
        
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // If CI mode, show the CI action selection dialog after main window is shown
        if (selectedProfile == UserProfile.CI) {
            controller.showCIActionDialog();
        }
    }
    
    /**
     * Shows the role selection dialog at application startup.
     * @return The selected UserProfile, or null if dialog was closed without selection
     */
    private UserProfile showRoleSelectionDialog() {
        Alert roleDialog = new Alert(Alert.AlertType.NONE);
        roleDialog.setTitle("Role Selection");
        roleDialog.setHeaderText("What is your Role?");
        roleDialog.initModality(Modality.APPLICATION_MODAL);
        
        ButtonType ieButton = new ButtonType("IE", ButtonBar.ButtonData.OK_DONE);
        ButtonType ciButton = new ButtonType("CI", ButtonBar.ButtonData.OK_DONE);
        
        roleDialog.getButtonTypes().setAll(ieButton, ciButton);
        
        Optional<ButtonType> result = roleDialog.showAndWait();
        
        if (result.isPresent()) {
            if (result.get() == ieButton) {
                return UserProfile.IE;
            } else if (result.get() == ciButton) {
                return UserProfile.CI;
            }
        }
        
        return null; // Dialog closed without selection
    }
}
