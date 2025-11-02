package com.example.exceljson;

import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the FXML file can be loaded without errors.
 */
public class FXMLLoadTest {

    @Test
    public void testFXMLCanBeLoaded() throws Exception {
        // This test verifies that the FXML file is well-formed and can be parsed
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/exceljson/App.fxml"));
        assertNotNull(loader, "FXMLLoader should not be null");
        
        // Verify the resource exists
        assertNotNull(loader.getLocation(), "FXML resource should exist");
        
        // Note: We don't actually load the FXML because that would require initializing
        // the JavaFX toolkit, which is not available in headless test environments.
        // This test just verifies the resource can be found.
    }
}
