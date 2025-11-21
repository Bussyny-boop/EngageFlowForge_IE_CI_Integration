package com.example.exceljson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test edge cases for visual flow diagram generation
 */
public class VisualFlowEdgeCasesTest {

    @Test
    public void testEmptyRecipientsHandling() {
        // Create a flow with empty recipients
        ExcelParserV5.FlowRow flow = new ExcelParserV5.FlowRow();
        flow.alarmName = "Test Alarm";
        flow.priorityRaw = "High";
        flow.deviceA = "VMP";
        flow.r1 = "";  // Empty recipient
        flow.r2 = null;  // Null recipient
        flow.r3 = "   ";  // Whitespace only
        flow.r4 = "Valid Recipient";
        flow.t1 = "Immediate";
        flow.t2 = "";
        flow.t3 = "";
        flow.t4 = "2 minutes";
        
        // Build diagram using same logic as AppController
        StringBuilder diagram = new StringBuilder();
        String[] recipients = { flow.r1, flow.r2, flow.r3, flow.r4, flow.r5 };
        String[] times = { flow.t1, flow.t2, flow.t3, flow.t4, flow.t5 };
        
        // Find steps with non-empty recipients
        java.util.List<Integer> steps = new java.util.ArrayList<>();
        for (int i = 0; i < recipients.length; i++) {
            if (recipients[i] != null && !recipients[i].trim().isEmpty()) {
                steps.add(i);
            }
        }
        
        // Should only have 1 step (r4)
        assertEquals(1, steps.size(), "Should only have one non-empty recipient");
        assertEquals(3, steps.get(0), "Should be the 4th recipient (index 3)");
    }
    
    @Test
    public void testNullPriorityHandling() {
        ExcelParserV5.FlowRow flow = new ExcelParserV5.FlowRow();
        flow.alarmName = "Test Alarm";
        flow.priorityRaw = null;
        flow.r1 = "Nurse";
        flow.t1 = "Immediate";
        
        // Priority should be handled gracefully
        String priority = sanitizeForPlantUml(flow.priorityRaw);
        assertEquals("", priority, "Null priority should sanitize to empty string");
    }
    
    @Test
    public void testMultilineRecipientSplitting() {
        String recipients = "Nurse 1\nNurse 2\nNurse 3";
        String[] parts = recipients.split("[,;\\n\\r]+");
        
        assertEquals(3, parts.length, "Should split into 3 parts");
        assertEquals("Nurse 1", parts[0]);
        assertEquals("Nurse 2", parts[1]);
        assertEquals("Nurse 3", parts[2]);
    }
    
    @Test
    public void testMixedDelimiterRecipients() {
        String recipients = "Nurse 1, Nurse 2; Nurse 3\nNurse 4";
        String[] parts = recipients.split("[,;\\n\\r]+");
        
        assertEquals(4, parts.length, "Should handle mixed delimiters");
    }
    
    @Test
    public void testSpecialCharacterSanitization() {
        String input = "VAssign:[Room] Nurse";
        String sanitized = sanitizeForPlantUml(input);
        
        // [ and ] should be converted to ( and )
        assertEquals("VAssign:(Room) Nurse", sanitized, "Should sanitize brackets");
    }
    
    private static String sanitizeForPlantUml(String value) {
        if (value == null) {
            return "";
        }
        
        String cleaned = value.chars()
            .mapToObj(c -> {
                if (c < 32 || c == 127) {
                    return " ";
                }
                return String.valueOf((char) c);
            })
            .collect(java.util.stream.Collectors.joining());
        
        String normalized = cleaned
            .replace("[", "(")
            .replace("]", ")")
            .replace("{", "(")
            .replace("}", ")")
            .replace("<", "(")
            .replace(">", ")")
            .replace("|", "/")
            .replace("\\", "/")
            .replace("\"", "'")
            .replaceAll("\\s+", " ")
            .trim();
        
        return normalized.length() > 0 ? normalized : "-";
    }
}
