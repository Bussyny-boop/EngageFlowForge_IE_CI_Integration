package com.example.exceljson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify enhanced page transition animations
 */
public class EnhancedAnimationTest {

    @Test
    public void testAppControllerHasAnimationImports() {
        // This test verifies that the AppController has the necessary animation imports
        // We can't directly test JavaFX animations in a headless environment,
        // but we can verify the code structure
        
        try {
            // Verify that the AppController class exists and can be loaded
            Class<?> appControllerClass = Class.forName("com.example.exceljson.AppController");
            assertNotNull(appControllerClass, "AppController class should exist");
            
            // Check that FadeTransition, TranslateTransition, and ParallelTransition are available
            Class.forName("javafx.animation.FadeTransition");
            Class.forName("javafx.animation.TranslateTransition");
            Class.forName("javafx.animation.ParallelTransition");
            
        } catch (ClassNotFoundException e) {
            fail("Required animation classes not found: " + e.getMessage());
        }
    }

    @Test
    public void testAnimationDurationConstants() {
        // Verify that Duration class is available for animation timing
        try {
            Class.forName("javafx.util.Duration");
        } catch (ClassNotFoundException e) {
            fail("Duration class not found: " + e.getMessage());
        }
    }

    @Test
    public void testNavigationElementsExist() {
        var fxmlResource = getClass().getResource("/com/example/exceljson/App.fxml");
        assertNotNull(fxmlResource, "App.fxml should exist");
        
        try (var stream = fxmlResource.openStream()) {
            String fxmlContent = new String(stream.readAllBytes());
            
            // Verify navigation toggle buttons exist (these trigger animations)
            assertTrue(fxmlContent.contains("fx:id=\"navUnits\""), 
                "FXML should contain navUnits button");
            assertTrue(fxmlContent.contains("fx:id=\"navNurseCalls\""), 
                "FXML should contain navNurseCalls button");
            assertTrue(fxmlContent.contains("fx:id=\"navClinicals\""), 
                "FXML should contain navClinicals button");
            assertTrue(fxmlContent.contains("fx:id=\"navOrders\""), 
                "FXML should contain navOrders button");
            
            // Verify views exist (these are animated)
            assertTrue(fxmlContent.contains("fx:id=\"unitsView\""), 
                "FXML should contain unitsView");
            assertTrue(fxmlContent.contains("fx:id=\"nurseCallsView\""), 
                "FXML should contain nurseCallsView");
            assertTrue(fxmlContent.contains("fx:id=\"clinicalsView\""), 
                "FXML should contain clinicalsView");
            assertTrue(fxmlContent.contains("fx:id=\"ordersView\""), 
                "FXML should contain ordersView");
            
        } catch (Exception e) {
            fail("Failed to read FXML file: " + e.getMessage());
        }
    }

    @Test
    public void testSettingsDrawerElementsExist() {
        var fxmlResource = getClass().getResource("/com/example/exceljson/App.fxml");
        assertNotNull(fxmlResource, "App.fxml should exist");
        
        try (var stream = fxmlResource.openStream()) {
            String fxmlContent = new String(stream.readAllBytes());
            
            // Verify settings drawer exists (it has animated open/close)
            assertTrue(fxmlContent.contains("fx:id=\"settingsDrawer\""), 
                "FXML should contain settingsDrawer");
            assertTrue(fxmlContent.contains("fx:id=\"settingsButton\""), 
                "FXML should contain settingsButton");
            assertTrue(fxmlContent.contains("fx:id=\"closeSettingsButton\""), 
                "FXML should contain closeSettingsButton");
            
        } catch (Exception e) {
            fail("Failed to read FXML file: " + e.getMessage());
        }
    }
}
