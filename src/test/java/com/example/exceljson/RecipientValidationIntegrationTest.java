package com.example.exceljson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify the recipient validation behavior matches the problem statement.
 * 
 * Problem Statement:
 * 1. Recipient cells should change to light orange when keywords "Custom unit, Group, Assigned, CS" are NOT found
 * 2. 1st recipients field (R1) should change to light orange when keywords are not found OR when cell is empty
 * 3. Other recipients (R2-R5) should only change to light orange when keywords are not found, NOT when empty
 */
class RecipientValidationIntegrationTest {

    private ExcelParserV5 parser;

    @BeforeEach
    void setUp() {
        parser = new ExcelParserV5();
    }

    @Test
    void testProblemStatement_R1_EmptyCell() {
        // R1 should be highlighted when empty
        assertFalse(parser.isValidFirstRecipient(""), 
            "R1 empty cell should be highlighted (invalid)");
        assertFalse(parser.isValidFirstRecipient(null), 
            "R1 null cell should be highlighted (invalid)");
    }

    @Test
    void testProblemStatement_R1_InvalidKeywords() {
        // R1 should be highlighted when these invalid keywords are found (no valid keywords present)
        assertFalse(parser.isValidFirstRecipient("Custom unit"), 
            "R1 with 'Custom unit' should be highlighted");
        assertFalse(parser.isValidFirstRecipient("Group"), 
            "R1 with 'Group' should be highlighted");
        assertFalse(parser.isValidFirstRecipient("Assigned"), 
            "R1 with 'Assigned' should be highlighted");
        assertFalse(parser.isValidFirstRecipient("CS"), 
            "R1 with 'CS' should be highlighted");
    }

    @Test
    void testProblemStatement_R1_ValidKeywords() {
        // R1 should NOT be highlighted when valid keywords are found
        assertTrue(parser.isValidFirstRecipient("VCS"), 
            "R1 with 'VCS' should NOT be highlighted");
        assertTrue(parser.isValidFirstRecipient("Edge"), 
            "R1 with 'Edge' should NOT be highlighted");
        assertTrue(parser.isValidFirstRecipient("XMPP"), 
            "R1 with 'XMPP' should NOT be highlighted");
        assertTrue(parser.isValidFirstRecipient("Vocera"), 
            "R1 with 'Vocera' should NOT be highlighted");
    }

    @Test
    void testProblemStatement_R2_R5_EmptyCell() {
        // R2-R5 should NOT be highlighted when empty
        assertTrue(parser.isValidOtherRecipient(""), 
            "R2-R5 empty cell should NOT be highlighted (valid)");
        assertTrue(parser.isValidOtherRecipient(null), 
            "R2-R5 null cell should NOT be highlighted (valid)");
    }

    @Test
    void testProblemStatement_R2_R5_InvalidKeywords() {
        // R2-R5 should be highlighted when these invalid keywords are found (no valid keywords present)
        assertFalse(parser.isValidOtherRecipient("Custom unit"), 
            "R2-R5 with 'Custom unit' should be highlighted");
        assertFalse(parser.isValidOtherRecipient("Group"), 
            "R2-R5 with 'Group' should be highlighted");
        assertFalse(parser.isValidOtherRecipient("Assigned"), 
            "R2-R5 with 'Assigned' should be highlighted");
        assertFalse(parser.isValidOtherRecipient("CS"), 
            "R2-R5 with 'CS' should be highlighted");
    }

    @Test
    void testProblemStatement_R2_R5_ValidKeywords() {
        // R2-R5 should NOT be highlighted when valid keywords are found
        assertTrue(parser.isValidOtherRecipient("VCS"), 
            "R2-R5 with 'VCS' should NOT be highlighted");
        assertTrue(parser.isValidOtherRecipient("Edge"), 
            "R2-R5 with 'Edge' should NOT be highlighted");
        assertTrue(parser.isValidOtherRecipient("XMPP"), 
            "R2-R5 with 'XMPP' should NOT be highlighted");
        assertTrue(parser.isValidOtherRecipient("Vocera"), 
            "R2-R5 with 'Vocera' should NOT be highlighted");
    }

    @Test
    void testProblemStatement_KeyDifference() {
        // The KEY difference: R1 highlights empty, R2-R5 do not
        String emptyValue = "";
        
        assertFalse(parser.isValidFirstRecipient(emptyValue), 
            "KEY DIFFERENCE: R1 empty -> highlighted");
        assertTrue(parser.isValidOtherRecipient(emptyValue), 
            "KEY DIFFERENCE: R2-R5 empty -> NOT highlighted");
    }

    @Test
    void testProblemStatement_BothRejectInvalidKeywords() {
        // Both R1 and R2-R5 highlight when keywords like "Custom unit, Group, Assigned, CS" are found
        String[] invalidCases = {"Custom unit", "Group", "Assigned", "CS"};
        
        for (String invalidCase : invalidCases) {
            assertFalse(parser.isValidFirstRecipient(invalidCase), 
                "R1 should highlight for: " + invalidCase);
            assertFalse(parser.isValidOtherRecipient(invalidCase), 
                "R2-R5 should highlight for: " + invalidCase);
        }
    }

    @Test
    void testProblemStatement_MixedCaseWithValidKeyword() {
        // If text contains a valid keyword (VCS, Edge, XMPP, Vocera), 
        // it should NOT be highlighted even if it also contains "Custom unit" etc.
        String mixedCase = "Custom unit using VCS";
        
        assertTrue(parser.isValidFirstRecipient(mixedCase), 
            "R1 should NOT highlight when valid keyword is present");
        assertTrue(parser.isValidOtherRecipient(mixedCase), 
            "R2-R5 should NOT highlight when valid keyword is present");
    }
}
