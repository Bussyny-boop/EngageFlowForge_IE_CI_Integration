package com.example.exceljson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify settings layout improvements for better visibility
 * and organization of the Vocera Badge Alert Interface section.
 */
public class SettingsLayoutTest {

    @Test
    public void testSettingsHasScrollPane() {
        var fxmlResource = getClass().getResource("/com/example/exceljson/App.fxml");
        assertNotNull(fxmlResource, "App.fxml should exist");
        
        try (var stream = fxmlResource.openStream()) {
            String fxmlContent = new String(stream.readAllBytes());
            
            // Verify settings drawer has a ScrollPane for scrollability
            assertTrue(fxmlContent.contains("fx:id=\"settingsDrawer\""), 
                "FXML should contain settingsDrawer");
            assertTrue(fxmlContent.contains("<ScrollPane fitToWidth=\"true\" hbarPolicy=\"NEVER\" vbarPolicy=\"AS_NEEDED\""), 
                "Settings drawer should have a ScrollPane with vertical scrolling");
            
        } catch (Exception e) {
            fail("Failed to read FXML file: " + e.getMessage());
        }
    }

    @Test
    public void testAdapterAndVoceraInterfaceLayout() {
        var fxmlResource = getClass().getResource("/com/example/exceljson/App.fxml");
        assertNotNull(fxmlResource, "App.fxml should exist");
        
        try (var stream = fxmlResource.openStream()) {
            String fxmlContent = new String(stream.readAllBytes());
            
            // Verify all three sections exist
            assertTrue(fxmlContent.contains("Adapter Reference Names"), 
                "FXML should contain Adapter Reference Names label");
            assertTrue(fxmlContent.contains("Vocera Badges Alert Interface"), 
                "FXML should contain Vocera Badges Alert Interface label");
            assertTrue(fxmlContent.contains("Room Filters"), 
                "FXML should contain Room Filters label");
            
            // Find the positions of all three sections
            int adapterPos = fxmlContent.indexOf("Adapter Reference Names");
            int voceraPos = fxmlContent.indexOf("Vocera Badges Alert Interface");
            int roomFiltersPos = fxmlContent.indexOf("Room Filters");
            
            // Verify they are in an HBox layout (side by side)
            // Find the HBox that contains all three sections
            int hboxStart = fxmlContent.lastIndexOf("<HBox", adapterPos);
            int hboxEnd = fxmlContent.indexOf("</HBox>", roomFiltersPos);
            
            assertTrue(hboxStart > 0 && hboxEnd > roomFiltersPos, 
                "Adapter, Vocera, and Room Filters sections should be in a horizontal layout (HBox)");
            
            // Verify the HBox contains all three sections
            String hboxContent = fxmlContent.substring(hboxStart, hboxEnd);
            assertTrue(hboxContent.contains("Adapter Reference Names") && 
                       hboxContent.contains("Vocera Badges Alert Interface") &&
                       hboxContent.contains("Room Filters"), 
                "All three sections should be in the same HBox for side-by-side layout");
            
        } catch (Exception e) {
            fail("Failed to read FXML file: " + e.getMessage());
        }
    }

    @Test
    public void testVoceraInterfaceCheckboxesExist() {
        var fxmlResource = getClass().getResource("/com/example/exceljson/App.fxml");
        assertNotNull(fxmlResource, "App.fxml should exist");
        
        try (var stream = fxmlResource.openStream()) {
            String fxmlContent = new String(stream.readAllBytes());
            
            // Verify all Vocera interface checkboxes exist
            assertTrue(fxmlContent.contains("fx:id=\"defaultEdgeCheckbox\""), 
                "FXML should contain defaultEdgeCheckbox");
            assertTrue(fxmlContent.contains("fx:id=\"defaultVmpCheckbox\""), 
                "FXML should contain defaultVmpCheckbox");
            assertTrue(fxmlContent.contains("fx:id=\"defaultVoceraCheckbox\""), 
                "FXML should contain defaultVoceraCheckbox");
            assertTrue(fxmlContent.contains("fx:id=\"defaultXmppCheckbox\""), 
                "FXML should contain defaultXmppCheckbox");
            
            // Verify checkbox labels
            assertTrue(fxmlContent.contains("Via Edge"), "FXML should contain 'Via Edge' checkbox");
            assertTrue(fxmlContent.contains("Via VMP"), "FXML should contain 'Via VMP' checkbox");
            assertTrue(fxmlContent.contains("Via Vocera"), "FXML should contain 'Via Vocera' checkbox");
            assertTrue(fxmlContent.contains("Via XMPP"), "FXML should contain 'Via XMPP' checkbox");
            
        } catch (Exception e) {
            fail("Failed to read FXML file: " + e.getMessage());
        }
    }
}
