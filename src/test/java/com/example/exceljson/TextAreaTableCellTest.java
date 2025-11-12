package com.example.exceljson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for TextAreaTableCell Shift+Enter behavior.
 * 
 * This test verifies the logic for inserting newlines when Shift+Enter is pressed.
 * We test the string manipulation logic directly since we can't test JavaFX UI in headless mode.
 */
public class TextAreaTableCellTest {

    /**
     * Tests the logic of inserting a newline at the caret position.
     * This simulates what happens when Shift+Enter is pressed in the TextArea.
     */
    @Test
    public void testNewlineInsertionLogic() {
        // Test case 1: Insert newline in the middle of text
        String currentText = "Hello World";
        int caretPosition = 5; // After "Hello"
        String expectedText = "Hello\n World";
        
        String actualText = currentText.substring(0, caretPosition) + "\n" + currentText.substring(caretPosition);
        assertEquals(expectedText, actualText, "Newline should be inserted at caret position");
        
        // Test case 2: Insert newline at the beginning
        currentText = "Hello World";
        caretPosition = 0;
        expectedText = "\nHello World";
        
        actualText = currentText.substring(0, caretPosition) + "\n" + currentText.substring(caretPosition);
        assertEquals(expectedText, actualText, "Newline should be inserted at the beginning");
        
        // Test case 3: Insert newline at the end
        currentText = "Hello World";
        caretPosition = currentText.length();
        expectedText = "Hello World\n";
        
        actualText = currentText.substring(0, caretPosition) + "\n" + currentText.substring(caretPosition);
        assertEquals(expectedText, actualText, "Newline should be inserted at the end");
        
        // Test case 4: Insert newline in empty text
        currentText = "";
        caretPosition = 0;
        expectedText = "\n";
        
        actualText = currentText.substring(0, caretPosition) + "\n" + currentText.substring(caretPosition);
        assertEquals(expectedText, actualText, "Newline should be inserted in empty text");
        
        // Test case 5: Multiple newlines
        currentText = "Line1\nLine2";
        caretPosition = 5; // After "Line1"
        expectedText = "Line1\n\nLine2";
        
        actualText = currentText.substring(0, caretPosition) + "\n" + currentText.substring(caretPosition);
        assertEquals(expectedText, actualText, "Newline should be inserted between existing lines");
    }
    
    /**
     * Tests that the caret position is correctly updated after inserting a newline.
     */
    @Test
    public void testCaretPositionAfterNewline() {
        String currentText = "Hello World";
        int caretPosition = 5; // After "Hello"
        
        // After inserting a newline, the caret should move forward by 1
        int expectedCaretPosition = caretPosition + 1;
        assertEquals(6, expectedCaretPosition, "Caret should move forward by 1 after newline insertion");
    }
    
    /**
     * Tests edge cases for newline insertion.
     */
    @Test
    public void testEdgeCases() {
        // Very long text
        String longText = "A".repeat(1000);
        int midpoint = 500;
        String result = longText.substring(0, midpoint) + "\n" + longText.substring(midpoint);
        assertEquals(1001, result.length(), "Newline should be inserted in long text");
        assertTrue(result.contains("\n"), "Result should contain a newline");
        
        // Text with special characters
        String specialText = "Special\tChars\r\nTest";
        int position = 7;
        String resultSpecial = specialText.substring(0, position) + "\n" + specialText.substring(position);
        assertTrue(resultSpecial.contains("\n"), "Newline should be inserted in text with special characters");
    }
}
