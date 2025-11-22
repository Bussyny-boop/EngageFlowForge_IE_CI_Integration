package com.example.exceljson;

import com.example.exceljson.util.AssignmentRoleValidator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AssignmentRoleValidator
 */
public class AssignmentRoleValidatorTest {

    @Test
    public void testParseAndValidateWithValidRole() {
        Set<String> roles = new HashSet<>();
        roles.add("Nurse");
        roles.add("Doctor");
        roles.add("Charge Nurse");

        String text = "VAssign: Nurse";
        List<AssignmentRoleValidator.Segment> segments = AssignmentRoleValidator.parseAndValidate(text, roles);

        assertEquals(2, segments.size());
        assertEquals("VAssign: ", segments.get(0).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.PLAIN, segments.get(0).status);
        assertEquals("Nurse", segments.get(1).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.VALID, segments.get(1).status);
    }

    @Test
    public void testParseAndValidateWithInvalidRole() {
        Set<String> roles = new HashSet<>();
        roles.add("Nurse");
        roles.add("Doctor");

        String text = "VAssign: InvalidRole";
        List<AssignmentRoleValidator.Segment> segments = AssignmentRoleValidator.parseAndValidate(text, roles);

        assertEquals(2, segments.size());
        assertEquals("VAssign: ", segments.get(0).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.PLAIN, segments.get(0).status);
        assertEquals("InvalidRole", segments.get(1).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.INVALID, segments.get(1).status);
    }

    @Test
    public void testParseAndValidateWithNoRole() {
        Set<String> roles = new HashSet<>();
        roles.add("Nurse");

        String text = "VAssign:";
        List<AssignmentRoleValidator.Segment> segments = AssignmentRoleValidator.parseAndValidate(text, roles);

        assertEquals(1, segments.size());
        assertEquals("VAssign:", segments.get(0).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.PLAIN, segments.get(0).status);
    }

    @Test
    public void testParseAndValidateMultiLine() {
        Set<String> roles = new HashSet<>();
        roles.add("Nurse");
        roles.add("Doctor");

        String text = "VAssign: Nurse\nVAssign: Doctor\nVAssign: InvalidRole";
        List<List<AssignmentRoleValidator.Segment>> allLineSegments = 
            AssignmentRoleValidator.parseAndValidateMultiLine(text, roles);

        assertEquals(3, allLineSegments.size());
        
        // First line - valid
        List<AssignmentRoleValidator.Segment> line1 = allLineSegments.get(0);
        assertEquals(2, line1.size());
        assertEquals("Nurse", line1.get(1).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.VALID, line1.get(1).status);
        
        // Second line - valid
        List<AssignmentRoleValidator.Segment> line2 = allLineSegments.get(1);
        assertEquals(2, line2.size());
        assertEquals("Doctor", line2.get(1).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.VALID, line2.get(1).status);
        
        // Third line - invalid
        List<AssignmentRoleValidator.Segment> line3 = allLineSegments.get(2);
        assertEquals(2, line3.size());
        assertEquals("InvalidRole", line3.get(1).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.INVALID, line3.get(1).status);
    }

    @Test
    public void testCaseInsensitiveValidation() {
        Set<String> roles = new HashSet<>();
        roles.add("Nurse");

        String text = "vassign: nurse";
        List<AssignmentRoleValidator.Segment> segments = AssignmentRoleValidator.parseAndValidate(text, roles);

        assertEquals(2, segments.size());
        assertEquals("nurse", segments.get(1).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.VALID, segments.get(1).status);
    }

    @Test
    public void testMixedContentWithVAssign() {
        Set<String> roles = new HashSet<>();
        roles.add("Nurse");

        String text = "Some text before VAssign: Nurse and text after";
        List<AssignmentRoleValidator.Segment> segments = AssignmentRoleValidator.parseAndValidate(text, roles);

        // The pattern matches "VAssign: Nurse and text after" as one match
        // because [^,;\n]+ captures everything until comma, semicolon, or newline
        assertEquals(3, segments.size());
        assertEquals("Some text before ", segments.get(0).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.PLAIN, segments.get(0).status);
        assertEquals("VAssign: ", segments.get(1).text);
        // The role name captures "Nurse and text after" because there's no delimiter
        assertTrue(segments.get(2).text.contains("Nurse"));
    }
}
