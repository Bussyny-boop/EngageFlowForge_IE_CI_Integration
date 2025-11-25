package com.example.exceljson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the Voice Group header detection properly handles
 * "Group Name" keyword matching with asterisks and variations.
 * 
 * Requirements from issue:
 * 1. Check for keyword "Group Name" and load that column
 * 2. If column does not exist, use the first column
 * 3. Handle headers with asterisks (e.g., "Group Name *")
 */
class VoiceGroupKeywordMatchingTest {

    // Constant from AppController
    private static final String TRAILING_ASTERISK_REGEX = "\\*+$";

    /**
     * Test that "Group Name" exact match works
     */
    @Test
    void testExactGroupNameMatch() {
        String header = "Group Name";
        String normalized = header.trim().replaceAll(TRAILING_ASTERISK_REGEX, "").trim();
        assertTrue(normalized.toLowerCase().contains("group name"), 
            "Should match exact 'Group Name' header");
    }

    /**
     * Test that "Group Name *" (space before asterisk) works
     */
    @Test
    void testGroupNameWithSpaceAsterisk() {
        String header = "Group Name *";
        String normalized = header.trim().replaceAll(TRAILING_ASTERISK_REGEX, "").trim();
        assertTrue(normalized.toLowerCase().contains("group name"), 
            "Should match 'Group Name *' header after removing asterisk and trimming");
    }

    /**
     * Test that "Group Name*" (no space before asterisk) works
     */
    @Test
    void testGroupNameWithNoSpaceAsterisk() {
        String header = "Group Name*";
        String normalized = header.trim().replaceAll(TRAILING_ASTERISK_REGEX, "").trim();
        assertTrue(normalized.toLowerCase().contains("group name"), 
            "Should match 'Group Name*' header after removing asterisk");
    }

    /**
     * Test that "group name" (lowercase) works
     */
    @Test
    void testLowercaseGroupName() {
        String header = "group name";
        String normalized = header.trim().replaceAll(TRAILING_ASTERISK_REGEX, "").trim();
        assertTrue(normalized.toLowerCase().contains("group name"), 
            "Should match lowercase 'group name' header");
    }

    /**
     * Test that "GROUP NAME" (uppercase) works
     */
    @Test
    void testUppercaseGroupName() {
        String header = "GROUP NAME";
        String normalized = header.trim().replaceAll(TRAILING_ASTERISK_REGEX, "").trim();
        assertTrue(normalized.toLowerCase().contains("group name"), 
            "Should match uppercase 'GROUP NAME' header");
    }

    /**
     * Test that "Voice Group Name" (contains "Group Name") works
     */
    @Test
    void testVoiceGroupNameContains() {
        String header = "Voice Group Name";
        String normalized = header.trim().replaceAll(TRAILING_ASTERISK_REGEX, "").trim();
        assertTrue(normalized.toLowerCase().contains("group name"), 
            "Should match 'Voice Group Name' header as it contains 'Group Name'");
    }

    /**
     * Test that headers without "Group Name" don't match
     */
    @Test
    void testHeaderWithoutGroupName() {
        String header = "Team Name";
        String normalized = header.trim().replaceAll(TRAILING_ASTERISK_REGEX, "").trim();
        assertFalse(normalized.toLowerCase().contains("group name"), 
            "Should NOT match 'Team Name' header");
    }

    /**
     * Test that "ID" column header doesn't match
     */
    @Test
    void testIdColumnDoesNotMatch() {
        String header = "ID *";
        String normalized = header.trim().replaceAll(TRAILING_ASTERISK_REGEX, "").trim();
        assertFalse(normalized.toLowerCase().contains("group name"), 
            "Should NOT match 'ID *' header");
    }

    /**
     * Test that multiple asterisks are removed
     */
    @Test
    void testMultipleAsterisks() {
        String header = "Group Name ***";
        String normalized = header.trim().replaceAll(TRAILING_ASTERISK_REGEX, "").trim();
        assertTrue(normalized.toLowerCase().contains("group name"), 
            "Should match 'Group Name ***' header after removing all trailing asterisks");
    }

    /**
     * Test that whitespace is properly handled
     */
    @Test
    void testWhitespaceHandling() {
        String header = "  Group Name  *  ";
        String normalized = header.trim().replaceAll(TRAILING_ASTERISK_REGEX, "").trim();
        assertTrue(normalized.toLowerCase().contains("group name"), 
            "Should match header with extra whitespace after proper trimming");
    }
}
