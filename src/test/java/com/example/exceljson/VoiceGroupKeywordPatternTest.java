package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the VGROUP_KEYWORD_PATTERN correctly identifies
 * VGroup: and Group: patterns without false positives.
 * This addresses the issue where any text containing "group" would
 * incorrectly trigger voice group validation.
 */
public class VoiceGroupKeywordPatternTest {
    
    // This is the same pattern used in AppController
    private static final Pattern VGROUP_KEYWORD_PATTERN = Pattern.compile("(?i)(?:VGroup|Group):");
    
    @Test
    public void testPatternMatchesValidKeywords() {
        // Should match VGroup: and Group: patterns
        assertTrue(VGROUP_KEYWORD_PATTERN.matcher("VGroup: Code Blue Team").find());
        assertTrue(VGROUP_KEYWORD_PATTERN.matcher("Group: Emergency").find());
        assertTrue(VGROUP_KEYWORD_PATTERN.matcher("vgroup: test").find()); // case insensitive
        assertTrue(VGROUP_KEYWORD_PATTERN.matcher("group: test").find()); // case insensitive
        assertTrue(VGROUP_KEYWORD_PATTERN.matcher("VGROUP: TEST").find()); // uppercase
        assertTrue(VGROUP_KEYWORD_PATTERN.matcher("GROUP: TEST").find()); // uppercase
        
        // Should match even when embedded in other text
        assertTrue(VGROUP_KEYWORD_PATTERN.matcher("Start VGroup: Rapid Response, End").find());
        assertTrue(VGROUP_KEYWORD_PATTERN.matcher("Prefix Group: Name Suffix").find());
    }
    
    @Test
    public void testPatternDoesNotMatchFalsePositives() {
        // Should NOT match text that just contains the word "group" or "vgroup"
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("This is a group discussion").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("group discussion").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("vgroup").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("VGroup").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("Group").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("grouping").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("regroup").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("group chat").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("The group met today").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("Configure group settings").find());
        
        // Should NOT match patterns without the colon
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("VGroup Test").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("Group Test").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("VGroup-Test").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("Group_Test").find());
    }
    
    @Test
    public void testPatternRequiresColon() {
        // The pattern should require a colon after VGroup or Group
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("VGroup Name").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("Group Name").find());
        assertTrue(VGROUP_KEYWORD_PATTERN.matcher("VGroup: Name").find());
        assertTrue(VGROUP_KEYWORD_PATTERN.matcher("Group: Name").find());
        
        // Even with whitespace before colon, it should not match
        // (pattern requires immediate colon)
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("VGroup : Name").find());
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("Group : Name").find());
    }
    
    @Test
    public void testEmptyAndNullStrings() {
        // Should not match empty or null strings
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher("").find());
        // Null would throw NPE, so we don't test it directly
    }
    
    @Test
    public void testMultilineText() {
        // Should find pattern in multiline text
        String multiline = "Line 1: Some text\nVGroup: Code Blue\nLine 3: More text";
        assertTrue(VGROUP_KEYWORD_PATTERN.matcher(multiline).find());
        
        String multilineNoPattern = "Line 1: group discussion\nLine 2: More groups\nLine 3: End";
        assertFalse(VGROUP_KEYWORD_PATTERN.matcher(multilineNoPattern).find());
    }
}
