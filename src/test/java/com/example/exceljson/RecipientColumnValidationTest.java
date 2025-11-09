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
 * Valid keywords (case-insensitive): Custom unit, Group, Assigned, CS
 * Cells without these keywords will be highlighted
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
    void testR1_ValidKeyword_CustomUnit() {
        assertTrue(parser.isValidFirstRecipient("Custom unit"));
        assertTrue(parser.isValidFirstRecipient("custom unit"));
        assertTrue(parser.isValidFirstRecipient("CUSTOM UNIT"));
        assertTrue(parser.isValidFirstRecipient("Using Custom unit here"));
    }

    @Test
    void testR1_ValidKeyword_Group() {
        assertTrue(parser.isValidFirstRecipient("Group"));
        assertTrue(parser.isValidFirstRecipient("group"));
        assertTrue(parser.isValidFirstRecipient("GROUP"));
        assertTrue(parser.isValidFirstRecipient("Send to Group"));
    }

    @Test
    void testR1_ValidKeyword_Assigned() {
        assertTrue(parser.isValidFirstRecipient("Assigned"));
        assertTrue(parser.isValidFirstRecipient("assigned"));
        assertTrue(parser.isValidFirstRecipient("ASSIGNED"));
        assertTrue(parser.isValidFirstRecipient("Assigned to nurse"));
    }

    @Test
    void testR1_ValidKeyword_CS() {
        assertTrue(parser.isValidFirstRecipient("CS"));
        assertTrue(parser.isValidFirstRecipient("cs"));
        assertTrue(parser.isValidFirstRecipient("Cs"));
        assertTrue(parser.isValidFirstRecipient("CS enabled"));
    }

    @Test
    void testR1_InvalidKeyword_VCS() {
        assertFalse(parser.isValidFirstRecipient("VCS"));
        assertFalse(parser.isValidFirstRecipient("vcs"));
        assertFalse(parser.isValidFirstRecipient("VcS"));
    }

    @Test
    void testR1_InvalidKeyword_Edge() {
        assertFalse(parser.isValidFirstRecipient("Edge"));
        assertFalse(parser.isValidFirstRecipient("edge"));
        assertFalse(parser.isValidFirstRecipient("EDGE"));
    }

    @Test
    void testR1_InvalidKeyword_XMPP() {
        assertFalse(parser.isValidFirstRecipient("XMPP"));
        assertFalse(parser.isValidFirstRecipient("xmpp"));
        assertFalse(parser.isValidFirstRecipient("XmPp"));
    }

    @Test
    void testR1_InvalidKeyword_Vocera() {
        assertFalse(parser.isValidFirstRecipient("Vocera"));
        assertFalse(parser.isValidFirstRecipient("vocera"));
        assertFalse(parser.isValidFirstRecipient("VOCERA"));
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
        assertTrue(parser.isValidFirstRecipient("VCS using Group"));
        assertTrue(parser.isValidFirstRecipient("Edge with Custom unit"));
        assertTrue(parser.isValidFirstRecipient("XMPP to Assigned"));
        assertTrue(parser.isValidFirstRecipient("Vocera group via CS"));
    }

    @Test
    void testR1_MultipleValidKeywords() {
        assertTrue(parser.isValidFirstRecipient("Group and Assigned"));
        assertTrue(parser.isValidFirstRecipient("CS or Custom unit"));
        assertTrue(parser.isValidFirstRecipient("Assigned, Group, CS"));
    }

    @Test
    void testR1_OnlyInvalidKeywords() {
        // Only invalid keywords - should be invalid
        assertFalse(parser.isValidFirstRecipient("VCS Edge XMPP"));
        assertFalse(parser.isValidFirstRecipient("Vocera VCS"));
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
    void testR2_R5_ValidKeyword_CustomUnit() {
        assertTrue(parser.isValidOtherRecipient("Custom unit"));
        assertTrue(parser.isValidOtherRecipient("custom unit"));
        assertTrue(parser.isValidOtherRecipient("CUSTOM UNIT"));
        assertTrue(parser.isValidOtherRecipient("Using Custom unit here"));
    }

    @Test
    void testR2_R5_ValidKeyword_Group() {
        assertTrue(parser.isValidOtherRecipient("Group"));
        assertTrue(parser.isValidOtherRecipient("group"));
        assertTrue(parser.isValidOtherRecipient("GROUP"));
        assertTrue(parser.isValidOtherRecipient("Send to Group"));
    }

    @Test
    void testR2_R5_ValidKeyword_Assigned() {
        assertTrue(parser.isValidOtherRecipient("Assigned"));
        assertTrue(parser.isValidOtherRecipient("assigned"));
        assertTrue(parser.isValidOtherRecipient("ASSIGNED"));
        assertTrue(parser.isValidOtherRecipient("Assigned to nurse"));
    }

    @Test
    void testR2_R5_ValidKeyword_CS() {
        assertTrue(parser.isValidOtherRecipient("CS"));
        assertTrue(parser.isValidOtherRecipient("cs"));
        assertTrue(parser.isValidOtherRecipient("Cs"));
        assertTrue(parser.isValidOtherRecipient("CS enabled"));
    }

    @Test
    void testR2_R5_InvalidKeyword_VCS() {
        assertFalse(parser.isValidOtherRecipient("VCS"));
        assertFalse(parser.isValidOtherRecipient("vcs"));
        assertFalse(parser.isValidOtherRecipient("VcS"));
    }

    @Test
    void testR2_R5_InvalidKeyword_Edge() {
        assertFalse(parser.isValidOtherRecipient("Edge"));
        assertFalse(parser.isValidOtherRecipient("edge"));
        assertFalse(parser.isValidOtherRecipient("EDGE"));
    }

    @Test
    void testR2_R5_InvalidKeyword_XMPP() {
        assertFalse(parser.isValidOtherRecipient("XMPP"));
        assertFalse(parser.isValidOtherRecipient("xmpp"));
        assertFalse(parser.isValidOtherRecipient("XmPp"));
    }

    @Test
    void testR2_R5_InvalidKeyword_Vocera() {
        assertFalse(parser.isValidOtherRecipient("Vocera"));
        assertFalse(parser.isValidOtherRecipient("vocera"));
        assertFalse(parser.isValidOtherRecipient("VOCERA"));
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
        assertTrue(parser.isValidOtherRecipient("VCS using Group"));
        assertTrue(parser.isValidOtherRecipient("Edge with Custom unit"));
        assertTrue(parser.isValidOtherRecipient("XMPP to Assigned"));
        assertTrue(parser.isValidOtherRecipient("Vocera group via CS"));
    }

    @Test
    void testR2_R5_MultipleValidKeywords() {
        assertTrue(parser.isValidOtherRecipient("Group and Assigned"));
        assertTrue(parser.isValidOtherRecipient("CS or Custom unit"));
        assertTrue(parser.isValidOtherRecipient("Assigned, Group, CS"));
    }

    @Test
    void testR2_R5_OnlyInvalidKeywords() {
        // Only invalid keywords - should be invalid
        assertFalse(parser.isValidOtherRecipient("VCS Edge XMPP"));
        assertFalse(parser.isValidOtherRecipient("Vocera VCS"));
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
        // Both R1 and R2-R5 should reject invalid keywords (ones without valid keywords)
        String[] invalidTexts = {"VCS", "Edge", "XMPP", "Vocera", "Random text"};
        
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
        String[] validTexts = {"Custom unit", "Group", "Assigned", "CS", "Using Group"};
        
        for (String text : validTexts) {
            assertTrue(parser.isValidFirstRecipient(text), 
                "R1 should accept: " + text);
            assertTrue(parser.isValidOtherRecipient(text), 
                "R2-R5 should accept: " + text);
        }
    }
}
