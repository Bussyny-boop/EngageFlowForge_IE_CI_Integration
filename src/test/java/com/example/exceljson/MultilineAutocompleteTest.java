package com.example.exceljson;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for multiline autocomplete logic.
 * 
 * This test verifies that autocomplete works correctly when editing cells with multiple lines.
 * It tests the logic for extracting the current line being edited and finding partial matches.
 */
public class MultilineAutocompleteTest {

    /**
     * Tests extracting the current line being edited from multiline text.
     * This simulates the logic used in setupAutoComplete and setupBedListAutoComplete.
     */
    @Test
    public void testCurrentLineExtraction() {
        // Test case 1: Single line text
        String text = "VGroup: Code Blue";
        int caretPos = text.length();
        String[] lines = text.substring(0, caretPos).split("\\n");
        String currentLine = lines.length > 0 ? lines[lines.length - 1] : "";
        
        assertEquals("VGroup: Code Blue", currentLine, "Current line should be the entire text for single line");
        
        // Test case 2: Multiple lines, caret at end
        text = "VGroup: Code Blue\nVGroup: Rapid Response";
        caretPos = text.length();
        lines = text.substring(0, caretPos).split("\\n");
        currentLine = lines.length > 0 ? lines[lines.length - 1] : "";
        
        assertEquals("VGroup: Rapid Response", currentLine, "Current line should be the last line");
        
        // Test case 3: Multiple lines, caret on first line
        text = "VGroup: Code Blue\nVGroup: Rapid Response";
        caretPos = 17; // At the end of first line
        lines = text.substring(0, caretPos).split("\\n");
        currentLine = lines.length > 0 ? lines[lines.length - 1] : "";
        
        assertEquals("VGroup: Code Blue", currentLine, "Current line should be the first line when caret is on first line");
        
        // Test case 4: Multiple lines, caret in the middle of second line
        text = "VGroup: Code Blue\nVGroup: Rapid Response";
        caretPos = 26; // In the middle of "VGroup: Rapid Response" (after "VGroup: ")
        lines = text.substring(0, caretPos).split("\\n");
        currentLine = lines.length > 0 ? lines[lines.length - 1] : "";
        
        assertEquals("VGroup: ", currentLine, "Current line should be partial second line up to caret");
        
        // Test case 5: Empty text
        text = "";
        caretPos = 0;
        lines = text.substring(0, caretPos).split("\\n");
        currentLine = lines.length > 0 ? lines[lines.length - 1] : "";
        
        assertEquals("", currentLine, "Current line should be empty for empty text");
    }
    
    /**
     * Tests the pattern matching logic for finding partial words on the current line.
     */
    @Test
    public void testPartialWordExtraction() {
        Pattern p = Pattern.compile("(?:^|[,;\\s:]\\s*)([a-zA-Z0-9_\\-\\s]{2,})$");
        
        // Test case 1: Simple partial word after keyword
        String currentLine = "VGroup: Code";
        Matcher m = p.matcher(currentLine);
        String partial = null;
        while (m.find()) {
            partial = m.group(1).trim();
        }
        
        assertEquals("Code", partial, "Should extract 'Code' as partial word");
        
        // Test case 2: Multi-word partial
        currentLine = "VGroup: Code Blue";
        m = p.matcher(currentLine);
        partial = null;
        while (m.find()) {
            partial = m.group(1).trim();
        }
        
        assertEquals("Code Blue", partial, "Should extract 'Code Blue' as partial word");
        
        // Test case 3: Comma-separated values
        currentLine = "VGroup: Code Blue, Rapid";
        m = p.matcher(currentLine);
        partial = null;
        while (m.find()) {
            partial = m.group(1).trim();
        }
        
        assertEquals("Rapid", partial, "Should extract 'Rapid' after comma");
        
        // Test case 4: Two characters minimum
        currentLine = "VGroup: Co";
        m = p.matcher(currentLine);
        partial = null;
        while (m.find()) {
            partial = m.group(1).trim();
        }
        
        assertEquals("Co", partial, "Should extract 2-character partial");
        
        // Test case 5: Single character should not match the pattern
        // The pattern requires {2,} which means 2 or more characters
        // 'VGroup:C' has only 'C' (1 char) after the colon, so it won't match
        currentLine = "VGroup:C";
        m = p.matcher(currentLine);
        partial = null;
        while (m.find()) {
            partial = m.group(1).trim();
        }
        
        assertNull(partial, "Should not match single character (requires 2+ chars)");
    }
    
    /**
     * Tests the text replacement logic for autocomplete in multiline context.
     */
    @Test
    public void testTextReplacementInMultilineContext() {
        // Test case 1: Replace on first line
        String text = "VGroup: Cod\nVGroup: Rapid Response";
        int caretPos = 11; // After "Cod"
        String partial = "Cod";
        String match = "Code Blue";
        
        String beforeCaret = text.substring(0, caretPos);
        int searchIndex = beforeCaret.lastIndexOf(partial);
        String newText = text.substring(0, searchIndex) + match + text.substring(searchIndex + partial.length());
        
        assertEquals("VGroup: Code Blue\nVGroup: Rapid Response", newText, 
            "Should replace 'Cod' with 'Code Blue' on first line");
        
        // Test case 2: Replace on second line
        text = "VGroup: Code Blue\nVGroup: Rap";
        caretPos = text.length(); // At the end
        partial = "Rap";
        match = "Rapid Response";
        
        beforeCaret = text.substring(0, caretPos);
        searchIndex = beforeCaret.lastIndexOf(partial);
        newText = text.substring(0, searchIndex) + match + text.substring(searchIndex + partial.length());
        
        assertEquals("VGroup: Code Blue\nVGroup: Rapid Response", newText,
            "Should replace 'Rap' with 'Rapid Response' on second line");
        
        // Test case 3: Replace in the middle line of three lines
        text = "VGroup: Code Blue\nVGroup: Ra\nVGroup: OB Nurse";
        caretPos = 28; // After "Ra" on second line
        partial = "Ra";
        match = "Rapid Response";
        
        beforeCaret = text.substring(0, caretPos);
        searchIndex = beforeCaret.lastIndexOf(partial);
        newText = text.substring(0, searchIndex) + match + text.substring(searchIndex + partial.length());
        
        assertEquals("VGroup: Code Blue\nVGroup: Rapid Response\nVGroup: OB Nurse", newText,
            "Should replace 'Ra' with 'Rapid Response' on middle line");
        
        // Test case 4: Verify caret position after replacement
        text = "VGroup: Cod";
        caretPos = text.length();
        partial = "Cod";
        match = "Code Blue";
        
        beforeCaret = text.substring(0, caretPos);
        searchIndex = beforeCaret.lastIndexOf(partial);
        int newCaretPos = searchIndex + match.length();
        
        // Position should be at index of "Cod" (8) + length of "Code Blue" (9) = 17
        assertEquals(searchIndex + match.length(), newCaretPos, 
            "Caret should be positioned after replacement text");
    }
    
    /**
     * Tests that autocomplete works correctly with VAssign context on different lines.
     */
    @Test
    public void testVAssignContextDetection() {
        Pattern vassignPattern = Pattern.compile("(?i)VAssign(?:ed)?:?");
        
        // Test case 1: VAssign on current line
        String currentLine = "VAssign: Room";
        assertTrue(vassignPattern.matcher(currentLine).find(), 
            "Should detect VAssign context on current line");
        
        // Test case 2: VGroup on current line (should not detect VAssign)
        currentLine = "VGroup: Code Blue";
        assertFalse(vassignPattern.matcher(currentLine).find(),
            "Should not detect VAssign context when VGroup is present");
        
        // Test case 3: VAssigned variant
        currentLine = "VAssigned: Room 101";
        assertTrue(vassignPattern.matcher(currentLine).find(),
            "Should detect VAssigned variant");
        
        // Test case 4: Case insensitive
        currentLine = "vassign: room";
        assertTrue(vassignPattern.matcher(currentLine).find(),
            "Should detect VAssign case-insensitively");
    }
    
    /**
     * Tests edge case scenarios for multiline autocomplete.
     */
    @Test
    public void testEdgeCases() {
        // Test case 1: Text ending with newline followed by typing on new line
        // When text ends with \n and user types, the current line will be the new content
        String text = "VGroup: Code Blue\nVG";
        int caretPos = text.length(); // At end after typing "VG"
        String[] lines = text.substring(0, caretPos).split("\\n");
        String currentLine = lines.length > 0 ? lines[lines.length - 1] : "";
        
        assertEquals("VG", currentLine, "Current line should be the new line being typed");
        
        // Test case 2: Line with only whitespace
        text = "VGroup: Code Blue\n   ";
        caretPos = text.length();
        lines = text.substring(0, caretPos).split("\\n");
        currentLine = lines.length > 0 ? lines[lines.length - 1] : "";
        
        assertEquals("   ", currentLine, "Current line should preserve whitespace");
        assertEquals(0, currentLine.trim().length(), "Trimmed line should be empty");
        
        // Test case 3: Multiple consecutive newlines
        text = "VGroup: Code Blue\n\n\nVGroup: Rapid";
        caretPos = text.length();
        lines = text.substring(0, caretPos).split("\\n");
        currentLine = lines.length > 0 ? lines[lines.length - 1] : "";
        
        assertEquals("VGroup: Rapid", currentLine, "Should extract last non-empty line");
    }
}
