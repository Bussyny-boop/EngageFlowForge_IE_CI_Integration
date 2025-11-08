package com.example.exceljson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify smooth tab transitions and icon support
 */
public class TabAnimationTest {

    @Test
    public void testIconResourcesExist() {
        // Verify SVG icon files exist
        var unitIcon = getClass().getResource("/icons/unit.svg");
        var nurseIcon = getClass().getResource("/icons/nurse.svg");
        var clinicalIcon = getClass().getResource("/icons/clinical.svg");
        var ordersIcon = getClass().getResource("/icons/orders.svg");
        
        assertNotNull(unitIcon, "unit.svg should exist in resources");
        assertNotNull(nurseIcon, "nurse.svg should exist in resources");
        assertNotNull(clinicalIcon, "clinical.svg should exist in resources");
        assertNotNull(ordersIcon, "orders.svg should exist in resources");
    }

    @Test
    public void testVoceraThemeHasTabStyles() {
        var cssResource = getClass().getResource("/css/vocera-theme.css");
        assertNotNull(cssResource, "vocera-theme.css should exist");
        
        try (var stream = cssResource.openStream()) {
            String cssContent = new String(stream.readAllBytes());
            
            // Verify tab-related styles are present
            assertTrue(cssContent.contains(".tab-pane"), "CSS should contain tab-pane styles");
            assertTrue(cssContent.contains(".tab-content-area"), "CSS should contain tab-content-area styles");
            assertTrue(cssContent.contains(".tab .graphic"), "CSS should contain tab graphic styles");
            assertTrue(cssContent.contains("dropshadow"), "CSS should contain header shadow effect");
            
        } catch (Exception e) {
            fail("Failed to read CSS file: " + e.getMessage());
        }
    }

    @Test
    public void testAppFxmlContainsNavigationReferences() {
        var fxmlResource = getClass().getResource("/com/example/exceljson/App.fxml");
        assertNotNull(fxmlResource, "App.fxml should exist");
        
        try (var stream = fxmlResource.openStream()) {
            String fxmlContent = new String(stream.readAllBytes());
            
            // Verify navigation has fx:id attributes (new layout uses ToggleButtons instead of TabPane)
            assertTrue(fxmlContent.contains("fx:id=\"navigationGroup\""), "FXML should contain navigationGroup");
            assertTrue(fxmlContent.contains("fx:id=\"navUnits\""), "FXML should contain navUnits");
            assertTrue(fxmlContent.contains("fx:id=\"navNurseCalls\""), "FXML should contain navNurseCalls");
            assertTrue(fxmlContent.contains("fx:id=\"navClinicals\""), "FXML should contain navClinicals");
            assertTrue(fxmlContent.contains("fx:id=\"navOrders\""), "FXML should contain navOrders");
            
            // Verify settings drawer exists
            assertTrue(fxmlContent.contains("fx:id=\"settingsDrawer\""), "FXML should contain settingsDrawer");
            
        } catch (Exception e) {
            fail("Failed to read FXML file: " + e.getMessage());
        }
    }
}
