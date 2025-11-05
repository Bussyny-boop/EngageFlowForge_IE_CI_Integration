package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that icon resources are available and loadable.
 */
public class IconResourceTest {

    @Test
    public void testPngIconExists() {
        // Test that the PNG icon file exists and can be loaded
        InputStream pngStream = getClass().getResourceAsStream("/icon.png");
        assertNotNull(pngStream, "PNG icon should be available in resources");
        
        try {
            pngStream.close();
        } catch (Exception e) {
            fail("Failed to close PNG icon stream: " + e.getMessage());
        }
    }

    @Test
    public void testIcoIconExists() {
        // Test that the ICO icon file exists (used by jpackage for Windows installer)
        InputStream icoStream = getClass().getResourceAsStream("/icon.ico");
        assertNotNull(icoStream, "ICO icon should be available in resources");
        
        try {
            icoStream.close();
        } catch (Exception e) {
            fail("Failed to close ICO icon stream: " + e.getMessage());
        }
    }
}
