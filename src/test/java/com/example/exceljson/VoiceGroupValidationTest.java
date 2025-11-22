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
}
