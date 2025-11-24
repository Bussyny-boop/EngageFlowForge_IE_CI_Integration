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
import javafx.scene.control.DialogPane;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;

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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/exceljson/App.fxml"));
        Parent root = loader.load();
        
        // Get the controller
        AppController controller = loader.getController();

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
        
        // Show role selection dialog AFTER app launches with theme applied
        UserProfile selectedProfile = showRoleSelectionDialog(scene, primaryStage);
        if (selectedProfile == null) {
            // User closed the dialog without selecting - exit application
            primaryStage.close();
            return;
        }
        
        // Set the user profile
        controller.setUserProfile(selectedProfile);
        
        // If CI mode, show the CI action selection dialog after role selection
        if (selectedProfile == UserProfile.CI) {
            controller.showCIActionDialog();
        }
    }
    
    /**
     * Shows the role selection dialog after application launches.
     * Dialog is styled with the app's theme and blocks access until a selection is made.
     * @param scene The application scene (for theme styling)
     * @param owner The owner stage (for modality)
     * @return The selected UserProfile, or null if dialog was closed without selection
     */
    private UserProfile showRoleSelectionDialog(Scene scene, Stage owner) {
        Alert roleDialog = new Alert(Alert.AlertType.NONE);
        roleDialog.setTitle("Engage FlowForge - Role Selection");
        roleDialog.setHeaderText("Engage FlowForge 2.0\n\nWhat is your Role?\n\nPlease select a profile to access the application.");
        roleDialog.initModality(Modality.APPLICATION_MODAL);
        roleDialog.initOwner(owner);
        
        ButtonType ieButton = new ButtonType("IE (Implementation Engineer)", ButtonBar.ButtonData.OK_DONE);
        ButtonType ciButton = new ButtonType("CI (Clinical Informatics)", ButtonBar.ButtonData.OK_DONE);
        
        roleDialog.getButtonTypes().setAll(ieButton, ciButton);
        
        // Get the dialog pane and add spacing between buttons
        DialogPane dialogPane = roleDialog.getDialogPane();
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);
        
        // Apply the app's theme to the dialog
        dialogPane.getStylesheets().addAll(scene.getStylesheets());
        
        // Add custom CSS to increase button spacing and improve appearance
        dialogPane.setStyle("-fx-padding: 30; -fx-spacing: 25;");
        
        // Add spacing to button bar
        if (dialogPane.lookup(".button-bar") != null) {
            dialogPane.lookup(".button-bar").setStyle("-fx-spacing: 40; -fx-padding: 15 0 0 0;");
        }
        
        // Make buttons larger and more prominent
        dialogPane.lookupButton(ieButton).setStyle("-fx-font-size: 14; -fx-padding: 12 30; -fx-min-width: 250;");
        dialogPane.lookupButton(ciButton).setStyle("-fx-font-size: 14; -fx-padding: 12 30; -fx-min-width: 250;");
        
        // Keep showing dialog until user makes a selection (no close without selection)
        UserProfile selectedProfile = null;
        while (selectedProfile == null) {
            Optional<ButtonType> result = roleDialog.showAndWait();
            
            if (result.isPresent()) {
                if (result.get() == ieButton) {
                    selectedProfile = UserProfile.IE;
                } else if (result.get() == ciButton) {
                    selectedProfile = UserProfile.CI;
                }
            } else {
                // User tried to close without selecting - show warning and re-display
                Alert warning = new Alert(Alert.AlertType.WARNING);
                warning.setTitle("Selection Required");
                warning.setHeaderText("Profile Selection Required");
                warning.setContentText("You must select a profile (IE or CI) to access the application.\n\nPlease choose your role to continue.");
                warning.initModality(Modality.APPLICATION_MODAL);
                warning.initOwner(owner);
                warning.getDialogPane().getStylesheets().addAll(scene.getStylesheets());
                warning.showAndWait();
            }
        }
        
        return selectedProfile;
    }
}
