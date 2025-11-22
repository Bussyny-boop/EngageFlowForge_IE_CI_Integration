package com.example.exceljson;

import com.example.exceljson.util.VoiceGroupValidator;
import com.example.exceljson.util.VoiceGroupValidator.Segment;
import com.example.exceljson.util.VoiceGroupValidator.ValidationStatus;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases to verify voice group validation behavior,
 * particularly focusing on scenarios mentioned in the problem statement.
 */
public class VoiceGroupCellValidationTest {

    @Test
    public void testOnlyGroupNameTurnsRed_NotKeyword() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("ValidTeam");
        
        // Test with invalid group - only the name should be marked invalid, not the keyword
        List<Segment> segments = VoiceGroupValidator.parseAndValidate("VGroup: InvalidTeam", loadedGroups);
        
        assertEquals(2, segments.size(), "Should have 2 segments: keyword and group name");
        
        // Verify keyword is PLAIN (black), not INVALID (red)
        assertEquals("VGroup: ", segments.get(0).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(0).status, 
            "Keyword 'VGroup: ' should be PLAIN (black), not marked as invalid");
        
        // Verify group name is INVALID (red)
        assertEquals("InvalidTeam", segments.get(1).text);
        assertEquals(ValidationStatus.INVALID, segments.get(1).status,
            "Group name 'InvalidTeam' should be INVALID (red)");
    }
    
    @Test
    public void testMultipleGroups_OnlySomeInvalid() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("TeamA");
        loadedGroups.add("TeamB");
        
        // Test with mix of valid and invalid groups
        String text = "VGroup: TeamA, VGroup: InvalidTeam, VGroup: TeamB";
        List<Segment> segments = VoiceGroupValidator.parseAndValidate(text, loadedGroups);
        
        // Should have segments: "VGroup: ", "TeamA", ", ", "VGroup: ", "InvalidTeam", ", ", "VGroup: ", "TeamB"
        assertTrue(segments.size() >= 6, "Should have multiple segments");
        
        // Find the segments for each group
        boolean foundValidTeamA = false;
        boolean foundInvalidTeam = false;
        boolean foundValidTeamB = false;
        
        for (Segment seg : segments) {
            if (seg.text.equals("TeamA") && seg.status == ValidationStatus.VALID) {
                foundValidTeamA = true;
            }
            if (seg.text.equals("InvalidTeam") && seg.status == ValidationStatus.INVALID) {
                foundInvalidTeam = true;
            }
            if (seg.text.equals("TeamB") && seg.status == ValidationStatus.VALID) {
                foundValidTeamB = true;
            }
            // Verify all keywords are PLAIN
            if (seg.text.contains("VGroup:") || seg.text.contains("Group:")) {
                assertEquals(ValidationStatus.PLAIN, seg.status,
                    "Keywords should always be PLAIN, not marked as invalid");
            }
        }
        
        assertTrue(foundValidTeamA, "TeamA should be marked as VALID");
        assertTrue(foundInvalidTeam, "InvalidTeam should be marked as INVALID");
        assertTrue(foundValidTeamB, "TeamB should be marked as VALID");
    }
    
    @Test
    public void testCaseInsensitiveMatching() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Emergency");
        
        // Test case-insensitive matching - "emergency", "EMERGENCY", etc. should all be valid
        List<Segment> segments;
        
        // Lowercase
        segments = VoiceGroupValidator.parseAndValidate("vgroup: emergency", loadedGroups);
        assertEquals(2, segments.size());
        assertEquals(ValidationStatus.VALID, segments.get(1).status,
            "Lowercase 'emergency' should match 'Emergency'");
        
        // Uppercase
        segments = VoiceGroupValidator.parseAndValidate("VGROUP: EMERGENCY", loadedGroups);
        assertEquals(2, segments.size());
        assertEquals(ValidationStatus.VALID, segments.get(1).status,
            "Uppercase 'EMERGENCY' should match 'Emergency'");
        
        // Mixed case
        segments = VoiceGroupValidator.parseAndValidate("Group: EmErGeNcY", loadedGroups);
        assertEquals(2, segments.size());
        assertEquals(ValidationStatus.VALID, segments.get(1).status,
            "Mixed case 'EmErGeNcY' should match 'Emergency'");
    }
    
    @Test
    public void testAllGroupsInvalid_OnlyNamesRed() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("OnlyValidGroup");
        
        // Test scenario where ALL groups in text are invalid
        // This might be what the user was experiencing
        String text = "VGroup: Invalid1, VGroup: Invalid2, VGroup: Invalid3";
        List<Segment> segments = VoiceGroupValidator.parseAndValidate(text, loadedGroups);
        
        int invalidCount = 0;
        int keywordPlainCount = 0;
        
        for (Segment seg : segments) {
            if (seg.status == ValidationStatus.INVALID) {
                invalidCount++;
                // Verify it's a group name, not a keyword
                assertFalse(seg.text.contains("VGroup:") || seg.text.contains("Group:"),
                    "Keywords should never be marked INVALID");
            }
            if (seg.status == ValidationStatus.PLAIN && 
                (seg.text.contains("VGroup:") || seg.text.contains("Group:"))) {
                keywordPlainCount++;
            }
        }
        
        assertEquals(3, invalidCount, "All 3 invalid groups should be marked INVALID");
        assertEquals(3, keywordPlainCount, "All 3 keywords should be PLAIN");
    }
    
    @Test
    public void testNoGroupsLoaded_NoValidation() {
        Set<String> emptyGroups = new HashSet<>();
        
        // When no groups are loaded, validation should not occur
        // This is handled at the AppController level, but verify the validator handles it gracefully
        List<Segment> segments = VoiceGroupValidator.parseAndValidate("VGroup: AnyGroup", emptyGroups);
        
        assertEquals(2, segments.size());
        assertEquals("VGroup: ", segments.get(0).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(0).status);
        
        // With no groups loaded, the group name should be marked INVALID
        assertEquals("AnyGroup", segments.get(1).text);
        assertEquals(ValidationStatus.INVALID, segments.get(1).status);
    }
}
