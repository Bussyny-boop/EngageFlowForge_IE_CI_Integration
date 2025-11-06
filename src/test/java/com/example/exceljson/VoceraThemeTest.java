package com.example.exceljson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the Vocera theme exists and is loadable
 */
public class VoceraThemeTest {

    @Test
    public void testVoceraThemeCssExists() {
        var cssResource = getClass().getResource("/css/vocera-theme.css");
        assertNotNull(cssResource, "vocera-theme.css should exist in resources");
        
        String cssPath = cssResource.toString();
        assertTrue(cssPath.endsWith("vocera-theme.css"), 
                  "CSS resource path should end with vocera-theme.css");
    }

    @Test
    public void testAppFxmlExists() {
        var fxmlResource = getClass().getResource("/com/example/exceljson/App.fxml");
        assertNotNull(fxmlResource, "App.fxml should exist in resources");
    }
}
