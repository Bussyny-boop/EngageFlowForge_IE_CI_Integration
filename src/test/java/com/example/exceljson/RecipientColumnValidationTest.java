package com.example.exceljson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for recipient column validation logic (R1-R5).
 * 
 * R1 (1st recipient): Should be highlighted when blank/empty OR no valid keywords found
 * R2-R5 (2nd-5th recipients): Should be highlighted ONLY when no valid keywords found (NOT when empty)
 * 
 * Valid keywords (case-insensitive): VCS, Edge, XMPP, Vocera
 * Invalid keywords that trigger highlighting: Custom unit, Group, Assigned, CS, or any text without valid keywords
 */
class RecipientColumnValidationTest {

    private ExcelParserV5 parser;

    @BeforeEach
    void setUp() {
        parser = new ExcelParserV5();
    }

    // ========== Tests for R1 (1st recipient) ==========
    
    @Test
    void testR1_BlankValue_ShouldBeInvalid() {
        // R1 should be highlighted when blank
        assertFalse(parser.isValidFirstRecipient(null), "R1 null should be invalid");
        assertFalse(parser.isValidFirstRecipient(""), "R1 empty string should be invalid");
        assertFalse(parser.isValidFirstRecipient("   "), "R1 whitespace should be invalid");
        assertFalse(parser.isValidFirstRecipient("\t"), "R1 tab should be invalid");
        assertFalse(parser.isValidFirstRecipient("\n"), "R1 newline should be invalid");
    }

    @Test
    void testR1_ValidKeyword_VCS() {
        assertTrue(parser.isValidFirstRecipient("VCS"));
        assertTrue(parser.isValidFirstRecipient("vcs"));
        assertTrue(parser.isValidFirstRecipient("VcS"));
        assertTrue(parser.isValidFirstRecipient("Using VCS interface"));
    }

    @Test
    void testR1_ValidKeyword_Edge() {
        assertTrue(parser.isValidFirstRecipient("Edge"));
        assertTrue(parser.isValidFirstRecipient("edge"));
        assertTrue(parser.isValidFirstRecipient("EDGE"));
        assertTrue(parser.isValidFirstRecipient("Send to Edge"));
    }

    @Test
    void testR1_ValidKeyword_XMPP() {
        assertTrue(parser.isValidFirstRecipient("XMPP"));
        assertTrue(parser.isValidFirstRecipient("xmpp"));
        assertTrue(parser.isValidFirstRecipient("XmPp"));
        assertTrue(parser.isValidFirstRecipient("XMPP enabled"));
    }

    @Test
    void testR1_ValidKeyword_Vocera() {
        assertTrue(parser.isValidFirstRecipient("Vocera"));
        assertTrue(parser.isValidFirstRecipient("vocera"));
        assertTrue(parser.isValidFirstRecipient("VOCERA"));
        assertTrue(parser.isValidFirstRecipient("Vocera system"));
    }

    @Test
    void testR1_InvalidKeyword_CustomUnit() {
        assertFalse(parser.isValidFirstRecipient("Custom unit"));
        assertFalse(parser.isValidFirstRecipient("custom unit"));
        assertFalse(parser.isValidFirstRecipient("CUSTOM UNIT"));
    }

    @Test
    void testR1_InvalidKeyword_Group() {
        assertFalse(parser.isValidFirstRecipient("Group"));
        assertFalse(parser.isValidFirstRecipient("group"));
        assertFalse(parser.isValidFirstRecipient("GROUP"));
    }

    @Test
    void testR1_InvalidKeyword_Assigned() {
        assertFalse(parser.isValidFirstRecipient("Assigned"));
        assertFalse(parser.isValidFirstRecipient("assigned"));
        assertFalse(parser.isValidFirstRecipient("ASSIGNED"));
    }

    @Test
    void testR1_InvalidKeyword_CS() {
        assertFalse(parser.isValidFirstRecipient("CS"));
        assertFalse(parser.isValidFirstRecipient("cs"));
        assertFalse(parser.isValidFirstRecipient("Cs"));
    }

    @Test
    void testR1_InvalidKeyword_RandomText() {
        assertFalse(parser.isValidFirstRecipient("Random text"));
        assertFalse(parser.isValidFirstRecipient("Invalid recipient"));
        assertFalse(parser.isValidFirstRecipient("Test 123"));
    }

    @Test
    void testR1_ValidAndInvalidCombination() {
        // If any valid keyword is present, should be valid
        assertTrue(parser.isValidFirstRecipient("Group using VCS"));
        assertTrue(parser.isValidFirstRecipient("Custom unit with Edge"));
        assertTrue(parser.isValidFirstRecipient("Assigned to XMPP"));
        assertTrue(parser.isValidFirstRecipient("CS group via Vocera"));
    }

    @Test
    void testR1_MultipleValidKeywords() {
        assertTrue(parser.isValidFirstRecipient("VCS and Edge"));
        assertTrue(parser.isValidFirstRecipient("XMPP or Vocera"));
        assertTrue(parser.isValidFirstRecipient("Edge, VCS, XMPP"));
    }

    @Test
    void testR1_OnlyInvalidKeywords() {
        // Only invalid keywords - should be invalid
        assertFalse(parser.isValidFirstRecipient("Group Assigned CS"));
        assertFalse(parser.isValidFirstRecipient("Custom unit Group"));
    }

    // ========== Tests for R2-R5 (2nd-5th recipients) ==========
    
    @Test
    void testR2_R5_BlankValue_ShouldBeValid() {
        // R2-R5 should NOT be highlighted when blank
        assertTrue(parser.isValidOtherRecipient(null), "R2-R5 null should be valid");
        assertTrue(parser.isValidOtherRecipient(""), "R2-R5 empty string should be valid");
        assertTrue(parser.isValidOtherRecipient("   "), "R2-R5 whitespace should be valid");
        assertTrue(parser.isValidOtherRecipient("\t"), "R2-R5 tab should be valid");
        assertTrue(parser.isValidOtherRecipient("\n"), "R2-R5 newline should be valid");
    }

    @Test
    void testR2_R5_ValidKeyword_VCS() {
        assertTrue(parser.isValidOtherRecipient("VCS"));
        assertTrue(parser.isValidOtherRecipient("vcs"));
        assertTrue(parser.isValidOtherRecipient("VcS"));
        assertTrue(parser.isValidOtherRecipient("Using VCS interface"));
    }

    @Test
    void testR2_R5_ValidKeyword_Edge() {
        assertTrue(parser.isValidOtherRecipient("Edge"));
        assertTrue(parser.isValidOtherRecipient("edge"));
        assertTrue(parser.isValidOtherRecipient("EDGE"));
        assertTrue(parser.isValidOtherRecipient("Send to Edge"));
    }

    @Test
    void testR2_R5_ValidKeyword_XMPP() {
        assertTrue(parser.isValidOtherRecipient("XMPP"));
        assertTrue(parser.isValidOtherRecipient("xmpp"));
        assertTrue(parser.isValidOtherRecipient("XmPp"));
        assertTrue(parser.isValidOtherRecipient("XMPP enabled"));
    }

    @Test
    void testR2_R5_ValidKeyword_Vocera() {
        assertTrue(parser.isValidOtherRecipient("Vocera"));
        assertTrue(parser.isValidOtherRecipient("vocera"));
        assertTrue(parser.isValidOtherRecipient("VOCERA"));
        assertTrue(parser.isValidOtherRecipient("Vocera system"));
    }

    @Test
    void testR2_R5_InvalidKeyword_CustomUnit() {
        assertFalse(parser.isValidOtherRecipient("Custom unit"));
        assertFalse(parser.isValidOtherRecipient("custom unit"));
        assertFalse(parser.isValidOtherRecipient("CUSTOM UNIT"));
    }

    @Test
    void testR2_R5_InvalidKeyword_Group() {
        assertFalse(parser.isValidOtherRecipient("Group"));
        assertFalse(parser.isValidOtherRecipient("group"));
        assertFalse(parser.isValidOtherRecipient("GROUP"));
    }

    @Test
    void testR2_R5_InvalidKeyword_Assigned() {
        assertFalse(parser.isValidOtherRecipient("Assigned"));
        assertFalse(parser.isValidOtherRecipient("assigned"));
        assertFalse(parser.isValidOtherRecipient("ASSIGNED"));
    }

    @Test
    void testR2_R5_InvalidKeyword_CS() {
        assertFalse(parser.isValidOtherRecipient("CS"));
        assertFalse(parser.isValidOtherRecipient("cs"));
        assertFalse(parser.isValidOtherRecipient("Cs"));
    }

    @Test
    void testR2_R5_InvalidKeyword_RandomText() {
        assertFalse(parser.isValidOtherRecipient("Random text"));
        assertFalse(parser.isValidOtherRecipient("Invalid recipient"));
        assertFalse(parser.isValidOtherRecipient("Test 123"));
    }

    @Test
    void testR2_R5_ValidAndInvalidCombination() {
        // If any valid keyword is present, should be valid
        assertTrue(parser.isValidOtherRecipient("Group using VCS"));
        assertTrue(parser.isValidOtherRecipient("Custom unit with Edge"));
        assertTrue(parser.isValidOtherRecipient("Assigned to XMPP"));
        assertTrue(parser.isValidOtherRecipient("CS group via Vocera"));
    }

    @Test
    void testR2_R5_MultipleValidKeywords() {
        assertTrue(parser.isValidOtherRecipient("VCS and Edge"));
        assertTrue(parser.isValidOtherRecipient("XMPP or Vocera"));
        assertTrue(parser.isValidOtherRecipient("Edge, VCS, XMPP"));
    }

    @Test
    void testR2_R5_OnlyInvalidKeywords() {
        // Only invalid keywords - should be invalid
        assertFalse(parser.isValidOtherRecipient("Group Assigned CS"));
        assertFalse(parser.isValidOtherRecipient("Custom unit Group"));
    }

    // ========== Comparison tests to verify different behavior ==========
    
    @Test
    void testDifferenceBetweenR1AndR2_R5_ForBlankValues() {
        // R1 should reject blank values
        assertFalse(parser.isValidFirstRecipient(null), "R1 should reject null");
        assertFalse(parser.isValidFirstRecipient(""), "R1 should reject empty");
        assertFalse(parser.isValidFirstRecipient("   "), "R1 should reject whitespace");
        
        // R2-R5 should accept blank values
        assertTrue(parser.isValidOtherRecipient(null), "R2-R5 should accept null");
        assertTrue(parser.isValidOtherRecipient(""), "R2-R5 should accept empty");
        assertTrue(parser.isValidOtherRecipient("   "), "R2-R5 should accept whitespace");
    }

    @Test
    void testBothRejectInvalidKeywords() {
        // Both R1 and R2-R5 should reject invalid keywords
        String[] invalidTexts = {"Custom unit", "Group", "Assigned", "CS", "Random text"};
        
        for (String text : invalidTexts) {
            assertFalse(parser.isValidFirstRecipient(text), 
                "R1 should reject: " + text);
            assertFalse(parser.isValidOtherRecipient(text), 
                "R2-R5 should reject: " + text);
        }
    }

    @Test
    void testBothAcceptValidKeywords() {
        // Both R1 and R2-R5 should accept valid keywords
        String[] validTexts = {"VCS", "Edge", "XMPP", "Vocera", "Using VCS"};
        
        for (String text : validTexts) {
            assertTrue(parser.isValidFirstRecipient(text), 
                "R1 should accept: " + text);
            assertTrue(parser.isValidOtherRecipient(text), 
                "R2-R5 should accept: " + text);
        }
    }
}
