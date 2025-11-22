package com.example.exceljson;

import com.example.exceljson.util.VoiceGroupValidator;
import com.example.exceljson.util.VoiceGroupValidator.Segment;
import com.example.exceljson.util.VoiceGroupValidator.ValidationStatus;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class VoiceGroupValidationTest {

    @Test
    public void testValidation() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Code Blue Team");
        loadedGroups.add("Rapid Response");

        // Test Valid
        List<Segment> segments = VoiceGroupValidator.parseAndValidate("VGroup: Code Blue Team", loadedGroups);
        assertEquals(2, segments.size()); // Prefix, Name
        assertEquals("VGroup: ", segments.get(0).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(0).status);
        assertEquals("Code Blue Team", segments.get(1).text);
        assertEquals(ValidationStatus.VALID, segments.get(1).status);

        // Test Invalid
        segments = VoiceGroupValidator.parseAndValidate("VGroup: Unknown Team", loadedGroups);
        assertEquals(2, segments.size());
        assertEquals("Unknown Team", segments.get(1).text);
        assertEquals(ValidationStatus.INVALID, segments.get(1).status);

        // Test Mixed Content with Delimiter
        segments = VoiceGroupValidator.parseAndValidate("Start VGroup: Rapid Response, End", loadedGroups);
        assertEquals(4, segments.size()); // "Start ", "VGroup: ", "Rapid Response", ", End"
        assertEquals("Start ", segments.get(0).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(0).status);
        assertEquals("VGroup: ", segments.get(1).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(1).status);
        assertEquals("Rapid Response", segments.get(2).text);
        assertEquals(ValidationStatus.VALID, segments.get(2).status);
        assertEquals(", End", segments.get(3).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(3).status);
        
        // Test Empty
        segments = VoiceGroupValidator.parseAndValidate("", loadedGroups);
        assertTrue(segments.isEmpty());
    }

    @Test
    public void testMultiLineValidation() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Code Blue Team");
        loadedGroups.add("Rapid Response");
        loadedGroups.add("Stroke Team");

        // Test multi-line with valid and invalid groups
        String multiLineText = "VGroup: Code Blue Team\nVGroup: Unknown Team\nVGroup: Stroke Team";
        List<List<Segment>> allLines = VoiceGroupValidator.parseAndValidateMultiLine(multiLineText, loadedGroups);
        
        // Should have 3 lines
        assertEquals(3, allLines.size());
        
        // Line 1: Valid group
        List<Segment> line1 = allLines.get(0);
        assertEquals(2, line1.size());
        assertEquals("VGroup: ", line1.get(0).text);
        assertEquals(ValidationStatus.PLAIN, line1.get(0).status);
        assertEquals("Code Blue Team", line1.get(1).text);
        assertEquals(ValidationStatus.VALID, line1.get(1).status);
        
        // Line 2: Invalid group
        List<Segment> line2 = allLines.get(1);
        assertEquals(2, line2.size());
        assertEquals("VGroup: ", line2.get(0).text);
        assertEquals(ValidationStatus.PLAIN, line2.get(0).status);
        assertEquals("Unknown Team", line2.get(1).text);
        assertEquals(ValidationStatus.INVALID, line2.get(1).status);
        
        // Line 3: Valid group
        List<Segment> line3 = allLines.get(2);
        assertEquals(2, line3.size());
        assertEquals("VGroup: ", line3.get(0).text);
        assertEquals(ValidationStatus.PLAIN, line3.get(0).status);
        assertEquals("Stroke Team", line3.get(1).text);
        assertEquals(ValidationStatus.VALID, line3.get(1).status);
    }
    
    @Test
    public void testKeywordNotMarkedAsInvalid() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Emergency");
        
        // Test that keywords (VGroup:, Group:) are never marked as INVALID
        List<Segment> segments = VoiceGroupValidator.parseAndValidate("VGroup: Emergency", loadedGroups);
        
        // Check prefix is PLAIN, not INVALID
        assertEquals("VGroup: ", segments.get(0).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(0).status);
        
        // Check group name
        assertEquals("Emergency", segments.get(1).text);
        assertEquals(ValidationStatus.VALID, segments.get(1).status);
        
        // Test with invalid group - keyword still PLAIN
        segments = VoiceGroupValidator.parseAndValidate("Group: NonExistent", loadedGroups);
        assertEquals("Group: ", segments.get(0).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(0).status);
        assertEquals("NonExistent", segments.get(1).text);
        assertEquals(ValidationStatus.INVALID, segments.get(1).status);
    }
    
    @Test
    public void testTextWithoutVGroupNotProcessed() {
        Set<String> loadedGroups = new HashSet<>();
        loadedGroups.add("Code Blue Team");
        
        // Test text that contains "group" but not in "VGroup:" or "Group:" pattern
        List<Segment> segments = VoiceGroupValidator.parseAndValidate("This is a group discussion", loadedGroups);
        
        // Should return the text as a single PLAIN segment
        assertEquals(1, segments.size());
        assertEquals("This is a group discussion", segments.get(0).text);
        assertEquals(ValidationStatus.PLAIN, segments.get(0).status);
    }
}
