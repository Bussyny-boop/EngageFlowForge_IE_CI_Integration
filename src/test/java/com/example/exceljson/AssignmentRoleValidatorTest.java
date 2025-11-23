package com.example.exceljson;

import com.example.exceljson.util.AssignmentRoleValidator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AssignmentRoleValidator
 */
public class AssignmentRoleValidatorTest {

    // Helper pattern to extract role name from bracket notation (e.g., "[Room] CNA" -> "CNA")
    private static final Pattern BRACKET_EXTRACTION_PATTERN = Pattern.compile("^\\[\\w+\\]\\s*");
    
    /**
     * Helper method to extract the actual role name from text that may contain bracket notation
     */
    private String extractRoleFromBracketNotation(String text) {
        return text.replaceAll(BRACKET_EXTRACTION_PATTERN.pattern(), "").trim();
    }

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
    public void testVAssignedKeyword() {
        Set<String> roles = new HashSet<>();
        roles.add("Room 101");
        roles.add("ICU Pod A");

        String text = "VAssigned: Room 101";
        List<AssignmentRoleValidator.Segment> segments = AssignmentRoleValidator.parseAndValidate(text, roles);

        assertEquals(2, segments.size());
        assertEquals("VAssigned: ", segments.get(0).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.PLAIN, segments.get(0).status);
        assertEquals("Room 101", segments.get(1).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.VALID, segments.get(1).status);
    }

    @Test
    public void testVAssignedCaseInsensitive() {
        Set<String> roles = new HashSet<>();
        roles.add("Nurse");

        String text = "vassigned: nurse";
        List<AssignmentRoleValidator.Segment> segments = AssignmentRoleValidator.parseAndValidate(text, roles);

        assertEquals(2, segments.size());
        assertEquals("vassigned: ", segments.get(0).text);
        assertEquals("nurse", segments.get(1).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.VALID, segments.get(1).status);
    }

    @Test
    public void testVAssignedWithInvalidRole() {
        Set<String> roles = new HashSet<>();
        roles.add("Nurse");

        String text = "VAssigned: Doctor";
        List<AssignmentRoleValidator.Segment> segments = AssignmentRoleValidator.parseAndValidate(text, roles);

        assertEquals(2, segments.size());
        assertEquals("VAssigned: ", segments.get(0).text);
        assertEquals("Doctor", segments.get(1).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.INVALID, segments.get(1).status);
    }

    @Test
    public void testMixedVAssignAndVAssigned() {
        Set<String> roles = new HashSet<>();
        roles.add("Nurse");
        roles.add("Doctor");

        String text = "VAssign: Nurse\nVAssigned: Doctor";
        List<List<AssignmentRoleValidator.Segment>> allLineSegments = 
            AssignmentRoleValidator.parseAndValidateMultiLine(text, roles);

        assertEquals(2, allLineSegments.size());
        
        // First line with VAssign - valid
        List<AssignmentRoleValidator.Segment> line1 = allLineSegments.get(0);
        assertEquals(2, line1.size());
        assertEquals("VAssign: ", line1.get(0).text);
        assertEquals("Nurse", line1.get(1).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.VALID, line1.get(1).status);
        
        // Second line with VAssigned - valid
        List<AssignmentRoleValidator.Segment> line2 = allLineSegments.get(1);
        assertEquals(2, line2.size());
        assertEquals("VAssigned: ", line2.get(0).text);
        assertEquals("Doctor", line2.get(1).text);
        assertEquals(AssignmentRoleValidator.ValidationStatus.VALID, line2.get(1).status);
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

    @Test
    public void testVAssignWithBracketNotation() {
        Set<String> roles = new HashSet<>();
        roles.add("CNA");
        roles.add("RN");
        roles.add("PCT");

        // Test: "VAssign: [Room] CNA" - should only validate "CNA", not "[Room] CNA"
        String text = "VAssign: [Room] CNA";
        List<AssignmentRoleValidator.Segment> segments = AssignmentRoleValidator.parseAndValidate(text, roles);

        // Should find "CNA" as a valid role
        boolean foundValidCNA = false;
        for (AssignmentRoleValidator.Segment segment : segments) {
            if (segment.status == AssignmentRoleValidator.ValidationStatus.VALID) {
                // The text should be just "CNA" after extracting from "[Room] CNA"
                String extractedRole = extractRoleFromBracketNotation(segment.text);
                if (extractedRole.equals("CNA")) {
                    foundValidCNA = true;
                    break;
                }
            }
        }
        
        assertTrue(foundValidCNA, "CNA should be validated as a valid role after removing [Room] prefix");
        
        System.out.println("✅ Test: VAssign with [Room] bracket notation");
        System.out.println("   Segments:");
        for (AssignmentRoleValidator.Segment segment : segments) {
            System.out.println("     - '" + segment.text + "' -> " + segment.status);
        }
    }

    @Test
    public void testVAssignWithPodBracketNotation() {
        Set<String> roles = new HashSet<>();
        roles.add("RN");

        String text = "VAssign: [Pod] RN";
        List<AssignmentRoleValidator.Segment> segments = AssignmentRoleValidator.parseAndValidate(text, roles);

        boolean foundValidRN = false;
        for (AssignmentRoleValidator.Segment segment : segments) {
            if (segment.status == AssignmentRoleValidator.ValidationStatus.VALID) {
                String extractedRole = extractRoleFromBracketNotation(segment.text);
                if (extractedRole.equals("RN")) {
                    foundValidRN = true;
                    break;
                }
            }
        }
        
        assertTrue(foundValidRN, "RN should be validated as a valid role after removing [Pod] prefix");
    }
}
