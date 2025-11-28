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
 * Tests for the "No Caregiver Group" column validation feature.
 * 
 * This column should validate group names against loaded voice groups
 * regardless of whether the VGroup:/Group: keyword prefix is present.
 * 
 * Examples:
 * - "VGroup:Charge Nurse" -> validates "Charge Nurse"
 * - "Charge Nurse" (plain) -> validates "Charge Nurse"
 */
public class NoCaregiverGroupValidationTest {

    @Test
    public void testPlainGroupNameValidation_Valid() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Charge Nurse");
        loadedGroups.add("Code Blue Team");
        
        // Plain group name without VGroup: prefix should be validated
        List<Segment> segments = VoiceGroupValidator.parseAndValidateAlways("Charge Nurse", loadedGroups);
        
        assertEquals(1, segments.size(), "Should have 1 segment for plain group name");
        assertEquals("Charge Nurse", segments.get(0).text);
        assertEquals(ValidationStatus.VALID, segments.get(0).status, 
            "Plain group name 'Charge Nurse' should be VALID when it exists in loaded groups");
    }
    
    @Test
    public void testPlainGroupNameValidation_Invalid() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Charge Nurse");
        loadedGroups.add("Code Blue Team");
        
        // Plain group name that doesn't exist should be INVALID
        List<Segment> segments = VoiceGroupValidator.parseAndValidateAlways("Unknown Group", loadedGroups);
        
        assertEquals(1, segments.size(), "Should have 1 segment for plain group name");
        assertEquals("Unknown Group", segments.get(0).text);
        assertEquals(ValidationStatus.INVALID, segments.get(0).status, 
            "Plain group name 'Unknown Group' should be INVALID when it doesn't exist in loaded groups");
    }
    
    @Test
    public void testVGroupPrefixValidation_Valid() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Charge Nurse");
        
        // With VGroup: prefix - should work same as before
        List<Segment> segments = VoiceGroupValidator.parseAndValidateAlways("VGroup:Charge Nurse", loadedGroups);
        
        assertEquals(2, segments.size(), "Should have 2 segments: prefix and group name");
        assertEquals("VGroup:", segments.get(0).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(0).status, "Prefix should be PLAIN");
        assertEquals("Charge Nurse", segments.get(1).text);
        assertEquals(ValidationStatus.VALID, segments.get(1).status, 
            "Group name 'Charge Nurse' should be VALID when it exists in loaded groups");
    }
    
    @Test
    public void testVGroupPrefixValidation_Invalid() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Charge Nurse");
        
        // With VGroup: prefix - invalid group
        List<Segment> segments = VoiceGroupValidator.parseAndValidateAlways("VGroup:Unknown Group", loadedGroups);
        
        assertEquals(2, segments.size(), "Should have 2 segments: prefix and group name");
        assertEquals("VGroup:", segments.get(0).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(0).status, "Prefix should be PLAIN");
        assertEquals("Unknown Group", segments.get(1).text);
        assertEquals(ValidationStatus.INVALID, segments.get(1).status, 
            "Group name 'Unknown Group' should be INVALID when it doesn't exist in loaded groups");
    }
    
    @Test
    public void testGroupPrefixValidation() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Charge Nurse");
        
        // With Group: prefix (alternative to VGroup:)
        List<Segment> segments = VoiceGroupValidator.parseAndValidateAlways("Group: Charge Nurse", loadedGroups);
        
        assertEquals(2, segments.size(), "Should have 2 segments: prefix and group name");
        assertEquals("Group: ", segments.get(0).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(0).status, "Prefix should be PLAIN");
        assertEquals("Charge Nurse", segments.get(1).text);
        assertEquals(ValidationStatus.VALID, segments.get(1).status);
    }
    
    @Test
    public void testCaseInsensitiveValidation() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Charge Nurse");
        
        // Different case should still match (case-insensitive)
        List<Segment> segments = VoiceGroupValidator.parseAndValidateAlways("CHARGE NURSE", loadedGroups);
        
        assertEquals(1, segments.size());
        assertEquals("CHARGE NURSE", segments.get(0).text);
        assertEquals(ValidationStatus.VALID, segments.get(0).status, 
            "Case-insensitive match should be VALID");
        
        // Mixed case
        segments = VoiceGroupValidator.parseAndValidateAlways("charge NURSE", loadedGroups);
        assertEquals(1, segments.size());
        assertEquals("charge NURSE", segments.get(0).text);
        assertEquals(ValidationStatus.VALID, segments.get(0).status, 
            "Case-insensitive match should be VALID");
    }
    
    @Test
    public void testMultiLineValidation() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Charge Nurse");
        loadedGroups.add("Code Blue Team");
        
        // Multi-line with mix of valid and invalid plain group names
        String multiLine = "Charge Nurse\nUnknown Group\nCode Blue Team";
        List<List<Segment>> lineSegments = VoiceGroupValidator.parseAndValidateAlwaysMultiLine(multiLine, loadedGroups);
        
        assertEquals(3, lineSegments.size(), "Should have 3 lines");
        
        // Line 1: Charge Nurse - VALID
        List<Segment> line1 = lineSegments.get(0);
        assertEquals(1, line1.size());
        assertEquals("Charge Nurse", line1.get(0).text);
        assertEquals(ValidationStatus.VALID, line1.get(0).status);
        
        // Line 2: Unknown Group - INVALID
        List<Segment> line2 = lineSegments.get(1);
        assertEquals(1, line2.size());
        assertEquals("Unknown Group", line2.get(0).text);
        assertEquals(ValidationStatus.INVALID, line2.get(0).status);
        
        // Line 3: Code Blue Team - VALID
        List<Segment> line3 = lineSegments.get(2);
        assertEquals(1, line3.size());
        assertEquals("Code Blue Team", line3.get(0).text);
        assertEquals(ValidationStatus.VALID, line3.get(0).status);
    }
    
    @Test
    public void testMultiLineMixedFormat() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Charge Nurse");
        loadedGroups.add("Code Blue Team");
        
        // Multi-line with mix of VGroup prefix and plain group names
        String multiLine = "VGroup:Charge Nurse\nCode Blue Team\nUnknown Group";
        List<List<Segment>> lineSegments = VoiceGroupValidator.parseAndValidateAlwaysMultiLine(multiLine, loadedGroups);
        
        assertEquals(3, lineSegments.size(), "Should have 3 lines");
        
        // Line 1: VGroup:Charge Nurse - VALID
        List<Segment> line1 = lineSegments.get(0);
        assertEquals(2, line1.size());
        assertEquals("VGroup:", line1.get(0).text);
        assertEquals(ValidationStatus.PLAIN, line1.get(0).status);
        assertEquals("Charge Nurse", line1.get(1).text);
        assertEquals(ValidationStatus.VALID, line1.get(1).status);
        
        // Line 2: Code Blue Team (plain) - VALID
        List<Segment> line2 = lineSegments.get(1);
        assertEquals(1, line2.size());
        assertEquals("Code Blue Team", line2.get(0).text);
        assertEquals(ValidationStatus.VALID, line2.get(0).status);
        
        // Line 3: Unknown Group (plain) - INVALID
        List<Segment> line3 = lineSegments.get(2);
        assertEquals(1, line3.size());
        assertEquals("Unknown Group", line3.get(0).text);
        assertEquals(ValidationStatus.INVALID, line3.get(0).status);
    }
    
    @Test
    public void testEmptyAndWhitespace() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Charge Nurse");
        
        // Empty string
        List<Segment> segments = VoiceGroupValidator.parseAndValidateAlways("", loadedGroups);
        assertTrue(segments.isEmpty(), "Empty string should return empty segments");
        
        // Null
        segments = VoiceGroupValidator.parseAndValidateAlways(null, loadedGroups);
        assertTrue(segments.isEmpty(), "Null should return empty segments");
        
        // Whitespace only
        segments = VoiceGroupValidator.parseAndValidateAlways("   ", loadedGroups);
        assertEquals(1, segments.size());
        assertEquals(ValidationStatus.PLAIN, segments.get(0).status, 
            "Whitespace-only should be PLAIN (not validated)");
    }
    
    @Test
    public void testWhitespacePreservation() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Charge Nurse");
        
        // Group name with leading/trailing whitespace
        List<Segment> segments = VoiceGroupValidator.parseAndValidateAlways("  Charge Nurse  ", loadedGroups);
        
        // Should preserve whitespace as PLAIN segments
        assertEquals(3, segments.size(), "Should have leading space, group name, trailing space");
        assertEquals("  ", segments.get(0).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(0).status);
        assertEquals("Charge Nurse", segments.get(1).text);
        assertEquals(ValidationStatus.VALID, segments.get(1).status);
        assertEquals("  ", segments.get(2).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(2).status);
    }
    
    /**
     * Test the exact scenario from the problem statement:
     * - "VGroup:Charge Nurse" should validate "Charge Nurse"
     * - "Charge Nurse" (plain) should also validate "Charge Nurse"
     */
    @Test
    public void testProblemStatementScenario() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Charge Nurse");
        
        // Test 1: VGroup:Charge Nurse
        List<Segment> segments = VoiceGroupValidator.parseAndValidateAlways("VGroup:Charge Nurse", loadedGroups);
        assertEquals(2, segments.size());
        assertEquals("Charge Nurse", segments.get(1).text);
        assertEquals(ValidationStatus.VALID, segments.get(1).status,
            "VGroup:Charge Nurse should validate 'Charge Nurse' as VALID");
        
        // Test 2: Plain "Charge Nurse" 
        segments = VoiceGroupValidator.parseAndValidateAlways("Charge Nurse", loadedGroups);
        assertEquals(1, segments.size());
        assertEquals("Charge Nurse", segments.get(0).text);
        assertEquals(ValidationStatus.VALID, segments.get(0).status,
            "Plain 'Charge Nurse' should also validate as VALID");
        
        // Test 3: Invalid plain group name
        segments = VoiceGroupValidator.parseAndValidateAlways("Invalid Group", loadedGroups);
        assertEquals(1, segments.size());
        assertEquals("Invalid Group", segments.get(0).text);
        assertEquals(ValidationStatus.INVALID, segments.get(0).status,
            "Plain 'Invalid Group' should validate as INVALID (not in loaded groups)");
    }
}
