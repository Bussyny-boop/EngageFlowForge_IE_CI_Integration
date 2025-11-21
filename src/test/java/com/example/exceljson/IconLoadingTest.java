package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that all required PNG icons can be loaded from resources
 */
public class IconLoadingTest {

    @Test
    public void testLoadDataIconsExist() {
        assertNotNull(getClass().getResourceAsStream("/icons/load-ndw.png"), "load-ndw.png should exist");
        assertNotNull(getClass().getResourceAsStream("/icons/load-xml.png"), "load-xml.png should exist");
        assertNotNull(getClass().getResourceAsStream("/icons/load-json.png"), "load-json.png should exist");
    }

    @Test
    public void testExportIconsExist() {
        assertNotNull(getClass().getResourceAsStream("/icons/export-nurse.png"), "export-nurse.png should exist");
        assertNotNull(getClass().getResourceAsStream("/icons/export-clinical.png"), "export-clinical.png should exist");
        assertNotNull(getClass().getResourceAsStream("/icons/export-orders.png"), "export-orders.png should exist");
    }

    @Test
    public void testUtilityIconsExist() {
        assertNotNull(getClass().getResourceAsStream("/icons/clear.png"), "clear.png should exist");
        assertNotNull(getClass().getResourceAsStream("/icons/preview.png"), "preview.png should exist");
        assertNotNull(getClass().getResourceAsStream("/icons/visual-flow.png"), "visual-flow.png should exist");
    }

    @Test
    public void testTabIconsExist() {
        assertNotNull(getClass().getResourceAsStream("/icons/unit.png"), "unit.png should exist");
        assertNotNull(getClass().getResourceAsStream("/icons/nurse.png"), "nurse.png should exist");
        assertNotNull(getClass().getResourceAsStream("/icons/clinical.png"), "clinical.png should exist");
        assertNotNull(getClass().getResourceAsStream("/icons/orders.png"), "orders.png should exist");
    }

    @Test
    public void testIconsAreValidPNG() throws Exception {
        String[] iconPaths = {
            "/icons/load-ndw.png",
            "/icons/load-xml.png",
            "/icons/load-json.png",
            "/icons/clear.png",
            "/icons/preview.png",
            "/icons/export-nurse.png",
            "/icons/export-clinical.png",
            "/icons/export-orders.png",
            "/icons/visual-flow.png"
        };

        for (String iconPath : iconPaths) {
            try (InputStream is = getClass().getResourceAsStream(iconPath)) {
                assertNotNull(is, iconPath + " should be loadable");
                
                // Read PNG signature (first 8 bytes)
                byte[] signature = new byte[8];
                int bytesRead = is.read(signature);
                assertEquals(8, bytesRead, iconPath + " should have at least 8 bytes");
                
                // Verify PNG signature: 137 80 78 71 13 10 26 10
                assertEquals((byte) 137, signature[0], iconPath + " should start with PNG signature");
                assertEquals((byte) 80, signature[1], iconPath + " should have valid PNG signature");
                assertEquals((byte) 78, signature[2], iconPath + " should have valid PNG signature");
                assertEquals((byte) 71, signature[3], iconPath + " should have valid PNG signature");
            }
        }
    }
}
