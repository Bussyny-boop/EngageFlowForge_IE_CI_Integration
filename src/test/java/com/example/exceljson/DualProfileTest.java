package com.example.exceljson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the dual-profile system (IE and CI modes).
 */
class DualProfileTest {

    @Test
    void testUserProfileEnum() {
        // Test that enum values exist
        assertNotNull(UserProfile.IE);
        assertNotNull(UserProfile.CI);
        
        // Test display names
        assertEquals("Implementation Engineer", UserProfile.IE.getDisplayName());
        assertEquals("Clinical Informatics", UserProfile.CI.getDisplayName());
    }
    
    @Test
    void testUserProfileEnumValues() {
        // Test that we have exactly 2 profiles
        UserProfile[] profiles = UserProfile.values();
        assertEquals(2, profiles.length);
        
        // Test that we can find each profile by name
        assertEquals(UserProfile.IE, UserProfile.valueOf("IE"));
        assertEquals(UserProfile.CI, UserProfile.valueOf("CI"));
    }
    
    @Test
    void testUserProfileDefaults() {
        // Verify the enum has the expected values
        UserProfile ie = UserProfile.IE;
        UserProfile ci = UserProfile.CI;
        
        assertNotEquals(ie, ci);
        assertEquals("IE", ie.name());
        assertEquals("CI", ci.name());
    }
}
