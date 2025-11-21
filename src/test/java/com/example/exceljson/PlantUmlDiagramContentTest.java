package com.example.exceljson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Test to verify that PlantUML diagram contains expected content
 */
public class PlantUmlDiagramContentTest {

    @Test
    public void testDiagramContainsRecipients() throws Exception {
        // Load test data
        ExcelParserV5 parser = new ExcelParserV5();
        File testFile = new File("CDH_3S_Generated.xlsx");
        
        if (!testFile.exists()) {
            System.out.println("Test file not found, skipping test");
            return;
        }
        
        parser.load(testFile);
        
        // Find a flow with recipients
        ExcelParserV5.FlowRow flowWithRecipients = null;
        for (ExcelParserV5.FlowRow flow : parser.nurseCalls) {
            if (flow.r1 != null && !flow.r1.trim().isEmpty() && flow.inScope) {
                flowWithRecipients = flow;
                break;
            }
        }
        
        assertNotNull(flowWithRecipients, "Should have at least one flow with recipients");
        
        // Create a minimal diagram using the same logic as AppController
        String diagram = buildTestDiagram(flowWithRecipients);
        
        System.out.println("=== Generated PlantUML Diagram ===");
        System.out.println(diagram);
        System.out.println("=== End Diagram ===");
        
        // Verify diagram contains expected elements
        assertTrue(diagram.contains("@startuml"), "Diagram should start with @startuml");
        assertTrue(diagram.contains("@enduml"), "Diagram should end with @enduml");
        
        // Verify alarm name is present
        String alarmName = flowWithRecipients.alarmName;
        String sanitizedAlarmName = sanitizeForPlantUml(alarmName);
        assertTrue(diagram.contains(sanitizedAlarmName), 
            "Diagram should contain alarm name: " + sanitizedAlarmName);
        
        // Verify priority is present if not empty
        if (flowWithRecipients.priorityRaw != null && !flowWithRecipients.priorityRaw.trim().isEmpty()) {
            String sanitizedPriority = sanitizeForPlantUml(flowWithRecipients.priorityRaw);
            if (!"-".equals(sanitizedPriority)) {
                assertTrue(diagram.contains("Priority: " + sanitizedPriority), 
                    "Diagram should contain priority: " + sanitizedPriority);
            }
        }
        
        // Verify recipients are present
        String r1 = flowWithRecipients.r1;
        if (r1 != null && !r1.trim().isEmpty()) {
            // Split recipients same way as AppController does
            String[] recipientParts = r1.split("[,;\\n\\r]+");
            for (String part : recipientParts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    String sanitizedPart = sanitizeForPlantUml(trimmed);
                    if (!sanitizedPart.isEmpty() && !"-".equals(sanitizedPart)) {
                        assertTrue(diagram.contains(sanitizedPart), 
                            "Diagram should contain recipient: " + sanitizedPart + 
                            " (original: " + trimmed + ")");
                    }
                }
            }
        }
        
        // Verify device info is present if not empty
        if (flowWithRecipients.deviceA != null && !flowWithRecipients.deviceA.trim().isEmpty() 
                && !"-".equals(flowWithRecipients.deviceA)) {
            String sanitizedDevice = sanitizeForPlantUml(flowWithRecipients.deviceA);
            assertTrue(diagram.contains("Device: " + sanitizedDevice) || diagram.contains(sanitizedDevice), 
                "Diagram should contain device A: " + sanitizedDevice);
        }
    }
    
    // Helper method to build a test diagram using same logic as AppController
    private String buildTestDiagram(ExcelParserV5.FlowRow row) {
        StringBuilder plantuml = new StringBuilder();
        plantuml.append("@startuml\n");
        
        String[] recipients = { row.r1, row.r2, row.r3, row.r4, row.r5 };
        String[] times = { row.t1, row.t2, row.t3, row.t4, row.t5 };
        
        List<Integer> steps = new ArrayList<>();
        for (int i = 0; i < recipients.length; i++) {
            if (recipients[i] != null && !recipients[i].trim().isEmpty()) {
                steps.add(i);
            }
        }
        
        if (!steps.isEmpty()) {
            List<String> headerLines = new ArrayList<>();
            headerLines.add("🔔 " + sanitizeForPlantUml(row.alarmName));
            String priority = sanitizeForPlantUml(row.priorityRaw);
            if (!"-".equals(priority) && !priority.isEmpty()) {
                headerLines.add("⚡ Priority: " + priority);
            }
            String deviceInfo = "";
            if (row.deviceA != null && !row.deviceA.trim().isEmpty() && !"-".equals(row.deviceA)) {
                deviceInfo = sanitizeForPlantUml(row.deviceA);
            }
            if (row.deviceB != null && !row.deviceB.trim().isEmpty() && !"-".equals(row.deviceB)) {
                if (!deviceInfo.isEmpty()) deviceInfo += ", ";
                deviceInfo += sanitizeForPlantUml(row.deviceB);
            }
            if (!deviceInfo.isEmpty()) {
                headerLines.add("📱 Device: " + deviceInfo);
            }
            String headerLabel = String.join("\\n", headerLines);
            plantuml.append("rectangle \"").append(headerLabel).append("\" as FlowHeader <<FlowHeader>>\n");
            
            for (int i = 0; i < steps.size(); i++) {
                int idx = steps.get(i);
                List<String> stageLabelLines = new ArrayList<>();
                stageLabelLines.add("🛑 Stop " + (i + 1) + " of " + steps.size());
                
                String rawRecipient = recipients[idx];
                if (rawRecipient != null && !rawRecipient.isEmpty()) {
                    String[] recipientParts = rawRecipient.split("[,;\\n\\r]+");
                    for (String part : recipientParts) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) {
                            String sanitizedPart = sanitizeForPlantUml(trimmed);
                            if (!sanitizedPart.isEmpty() && !"-".equals(sanitizedPart)) {
                                stageLabelLines.add("👤 " + sanitizedPart);
                            }
                        }
                    }
                }
                
                String stageLabel = String.join("\\n", stageLabelLines);
                plantuml.append("rectangle \"").append(stageLabel).append("\" as Stop_").append(i + 1).append("\n");
            }
        }
        
        plantuml.append("@enduml\n");
        return plantuml.toString();
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
            .collect(Collectors.joining());
        
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
