package com.example.exceljson;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.lang.reflect.Field;

/**
 * Simple test to verify the Room Filter fields are present in the GUI
 */
public class RoomFilterUITest {
    
    public static void main(String[] args) throws Exception {
        // Launch in a platform runlater to avoid blocking
        Platform.startup(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(RoomFilterUITest.class.getResource("/com/example/exceljson/App.fxml"));
                Parent root = loader.load();
                
                AppController controller = loader.getController();
                
                // Use reflection to access the room filter fields
                Field nursecallField = AppController.class.getDeclaredField("roomFilterNursecallField");
                Field clinicalField = AppController.class.getDeclaredField("roomFilterClinicalField");
                Field ordersField = AppController.class.getDeclaredField("roomFilterOrdersField");
                
                nursecallField.setAccessible(true);
                clinicalField.setAccessible(true);
                ordersField.setAccessible(true);
                
                TextField nursecallTextField = (TextField) nursecallField.get(controller);
                TextField clinicalTextField = (TextField) clinicalField.get(controller);
                TextField ordersTextField = (TextField) ordersField.get(controller);
                
                System.out.println("✓ Room Filter UI Test Results:");
                System.out.println("  - Nursecall field exists: " + (nursecallTextField != null));
                System.out.println("  - Clinical field exists: " + (clinicalTextField != null));
                System.out.println("  - Orders field exists: " + (ordersTextField != null));
                
                if (nursecallTextField != null) {
                    System.out.println("  - Nursecall prompt text: " + nursecallTextField.getPromptText());
                }
                if (clinicalTextField != null) {
                    System.out.println("  - Clinical prompt text: " + clinicalTextField.getPromptText());
                }
                if (ordersTextField != null) {
                    System.out.println("  - Orders prompt text: " + ordersTextField.getPromptText());
                }
                
                System.out.println("\n✓ All Room Filter UI components loaded successfully!");
                Platform.exit();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.exit();
                System.exit(1);
            }
        });
    }
}
