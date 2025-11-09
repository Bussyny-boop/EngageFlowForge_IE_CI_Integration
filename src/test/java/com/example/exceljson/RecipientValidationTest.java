package com.example.exceljson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for recipient field validation logic in Device-A column.
 * 
 * Valid keywords: VCS, Edge, XMPP, Vocera (case-insensitive)
 * Invalid keywords (removed from validation): Custom unit, Group, Assigned, CS
 * 
 * Blank cells should NOT be highlighted (considered valid).
 * Cells with invalid keywords should be highlighted.
 */
class RecipientValidationTest {

    private ExcelParserV5 parser;

    @BeforeEach
    void setUp() {
        parser = new ExcelParserV5();
    }

    // Test valid keywords (should return true)
    
    @Test
    void testValidKeyword_VCS() {
        assertTrue(parser.hasValidRecipientKeyword("VCS"));
        assertTrue(parser.hasValidRecipientKeyword("vcs"));
        assertTrue(parser.hasValidRecipientKeyword("VcS"));
    }

    @Test
    void testValidKeyword_Edge() {
        assertTrue(parser.hasValidRecipientKeyword("Edge"));
        assertTrue(parser.hasValidRecipientKeyword("edge"));
        assertTrue(parser.hasValidRecipientKeyword("EDGE"));
    }

    @Test
    void testValidKeyword_XMPP() {
        assertTrue(parser.hasValidRecipientKeyword("XMPP"));
        assertTrue(parser.hasValidRecipientKeyword("xmpp"));
        assertTrue(parser.hasValidRecipientKeyword("XmPp"));
    }

    @Test
    void testValidKeyword_Vocera() {
        assertTrue(parser.hasValidRecipientKeyword("Vocera"));
        assertTrue(parser.hasValidRecipientKeyword("vocera"));
        assertTrue(parser.hasValidRecipientKeyword("VOCERA"));
    }

    @Test
    void testValidKeyword_WithinText() {
        assertTrue(parser.hasValidRecipientKeyword("Using VCS interface"));
        assertTrue(parser.hasValidRecipientKeyword("Send to Edge"));
        assertTrue(parser.hasValidRecipientKeyword("XMPP enabled"));
        assertTrue(parser.hasValidRecipientKeyword("Vocera system"));
    }

    // Test blank/empty values (should return true - not highlighted)
    
    @Test
    void testBlankValue_Null() {
        assertTrue(parser.hasValidRecipientKeyword(null));
    }

    @Test
    void testBlankValue_Empty() {
        assertTrue(parser.hasValidRecipientKeyword(""));
    }

    @Test
    void testBlankValue_Whitespace() {
        assertTrue(parser.hasValidRecipientKeyword("   "));
        assertTrue(parser.hasValidRecipientKeyword("\t"));
        assertTrue(parser.hasValidRecipientKeyword("\n"));
    }

    // Test invalid keywords (should return false - will be highlighted)
    
    @Test
    void testInvalidKeyword_CustomUnit() {
        assertFalse(parser.hasValidRecipientKeyword("Custom unit"));
        assertFalse(parser.hasValidRecipientKeyword("custom unit"));
        assertFalse(parser.hasValidRecipientKeyword("CUSTOM UNIT"));
    }

    @Test
    void testInvalidKeyword_Group() {
        assertFalse(parser.hasValidRecipientKeyword("Group"));
        assertFalse(parser.hasValidRecipientKeyword("group"));
        assertFalse(parser.hasValidRecipientKeyword("GROUP"));
    }

    @Test
    void testInvalidKeyword_Assigned() {
        assertFalse(parser.hasValidRecipientKeyword("Assigned"));
        assertFalse(parser.hasValidRecipientKeyword("assigned"));
        assertFalse(parser.hasValidRecipientKeyword("ASSIGNED"));
    }

    @Test
    void testInvalidKeyword_CS() {
        assertFalse(parser.hasValidRecipientKeyword("CS"));
        assertFalse(parser.hasValidRecipientKeyword("cs"));
        assertFalse(parser.hasValidRecipientKeyword("Cs"));
    }

    @Test
    void testInvalidKeyword_RandomText() {
        assertFalse(parser.hasValidRecipientKeyword("Random text"));
        assertFalse(parser.hasValidRecipientKeyword("Invalid recipient"));
        assertFalse(parser.hasValidRecipientKeyword("Test 123"));
    }

    // Test combinations
    
    @Test
    void testValidAndInvalidCombination() {
        // Valid keyword present - should be valid
        assertTrue(parser.hasValidRecipientKeyword("Group using VCS"));
        assertTrue(parser.hasValidRecipientKeyword("Custom unit with Edge"));
        assertTrue(parser.hasValidRecipientKeyword("Assigned to XMPP"));
        assertTrue(parser.hasValidRecipientKeyword("CS group via Vocera"));
    }

    @Test
    void testMultipleValidKeywords() {
        assertTrue(parser.hasValidRecipientKeyword("VCS and Edge"));
        assertTrue(parser.hasValidRecipientKeyword("XMPP or Vocera"));
        assertTrue(parser.hasValidRecipientKeyword("Edge, VCS, XMPP"));
    }

    @Test
    void testOnlyInvalidKeywords() {
        // Only invalid keywords - should be invalid
        assertFalse(parser.hasValidRecipientKeyword("Group Assigned CS"));
        assertFalse(parser.hasValidRecipientKeyword("Custom unit Group"));
    }
}
