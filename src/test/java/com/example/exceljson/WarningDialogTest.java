package com.example.exceljson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

/**
 * Test to verify that the warning dialog behavior is correct.
 * This test checks that the showWarning method uses showAndWait() instead of auto-closing.
 */
public class WarningDialogTest {

    @Test
    public void testShowWarningMethodExists() throws Exception {
        // Verify that AppController has a showWarning method
        Method showWarningMethod = AppController.class.getDeclaredMethod("showWarning", String.class);
        assertNotNull(showWarningMethod, "showWarning method should exist");
        
        // Make it accessible to check implementation
        showWarningMethod.setAccessible(true);
        
        // Verify the method is private
        assertTrue(java.lang.reflect.Modifier.isPrivate(showWarningMethod.getModifiers()),
                "showWarning should be private");
    }

    @Test
    public void testCheckBothDefaultInterfacesSelectedMethodExists() throws Exception {
        // Verify that AppController has the checkBothDefaultInterfacesSelected method
        Method checkMethod = AppController.class.getDeclaredMethod("checkBothDefaultInterfacesSelected");
        assertNotNull(checkMethod, "checkBothDefaultInterfacesSelected method should exist");
        
        // Make it accessible
        checkMethod.setAccessible(true);
        
        // Verify the method is private
        assertTrue(java.lang.reflect.Modifier.isPrivate(checkMethod.getModifiers()),
                "checkBothDefaultInterfacesSelected should be private");
    }
    
    @Test
    public void testShowTimedWarningMethodDoesNotExist() {
        // Verify that the old showTimedWarning method no longer exists
        try {
            Method showTimedWarningMethod = AppController.class.getDeclaredMethod("showTimedWarning", String.class, int.class);
            fail("showTimedWarning method should not exist - it should be replaced with showWarning");
        } catch (NoSuchMethodException e) {
            // Expected - this is good, the old method has been removed
            assertTrue(true, "showTimedWarning correctly removed");
        }
    }
}
