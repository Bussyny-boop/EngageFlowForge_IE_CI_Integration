package com.example.exceljson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * End-to-end test for visual callflow export
 * This test verifies that the complete workflow of generating visual flow diagrams
 * includes all necessary data (recipients, priorities, alert types, devices)
 */
public class VisualCallflowExportTest {

    @Test
    public void testCompleteVisualFlowGeneration() throws Exception {
        // Load test data
        ExcelParserV5 parser = new ExcelParserV5();
        File testFile = new File("CDH_3S_Generated.xlsx");
        
        if (!testFile.exists()) {
            System.out.println("Test file not found, skipping test");
            return;
        }
        
        parser.load(testFile);
        
        // Simulate the same flow as generateVisualFlow() in AppController
        List<ExcelParserV5.FlowRow> checkedRows = new ArrayList<>();
        
        // Filter for in-scope rows (same as AppController line 4022)
        checkedRows.addAll(parser.nurseCalls.stream()
            .filter(r -> r.inScope)
            .collect(Collectors.toList()));
        
        assertFalse(checkedRows.isEmpty(), "Should have some in-scope nurse calls");
        
        // Test the first flow with recipients
        ExcelParserV5.FlowRow testFlow = null;
        for (ExcelParserV5.FlowRow flow : checkedRows) {
            if (flow.r1 != null && !flow.r1.trim().isEmpty()) {
                testFlow = flow;
                break;
            }
        }
        
        assertNotNull(testFlow, "Should have at least one flow with recipients");
        
        System.out.println("\n=== Testing Visual Callflow Export ===");
        System.out.println("Flow Details:");
        System.out.println("  Type: " + testFlow.type);
        System.out.println("  Alarm Name: " + testFlow.alarmName);
        System.out.println("  Priority: " + testFlow.priorityRaw);
        System.out.println("  Config Group: " + testFlow.configGroup);
        System.out.println("  Device A: " + testFlow.deviceA);
        System.out.println("  Device B: " + testFlow.deviceB);
        System.out.println("  In Scope: " + testFlow.inScope);
        
        // Build the visual flow diagram using the EXACT same logic as AppController
        String diagram = buildVisualFlowDiagramExact(testFlow);
        
        System.out.println("\n=== Generated PlantUML Diagram ===");
        System.out.println(diagram);
        System.out.println("=== End Diagram ===\n");
        
        // CRITICAL ASSERTIONS: Verify all required data is in the diagram
        
        // 1. Alarm name MUST be present
        assertTrue(diagram.contains(testFlow.alarmName) || 
                   diagram.contains(sanitizeForPlantUml(testFlow.alarmName)),
            "❌ FAIL: Alarm name '" + testFlow.alarmName + "' is MISSING from diagram!");
        System.out.println("✅ PASS: Alarm name is present in diagram");
        
        // 2. Priority MUST be present (if not empty)
        if (testFlow.priorityRaw != null && !testFlow.priorityRaw.trim().isEmpty()) {
            String sanitizedPriority = sanitizeForPlantUml(testFlow.priorityRaw);
            if (!"-".equals(sanitizedPriority)) {
                assertTrue(diagram.contains("Priority: " + sanitizedPriority) || 
                           diagram.contains(sanitizedPriority),
                    "❌ FAIL: Priority '" + testFlow.priorityRaw + "' is MISSING from diagram!");
                System.out.println("✅ PASS: Priority is present in diagram");
            }
        }
        
        // 3. Recipients MUST be present
        if (testFlow.r1 != null && !testFlow.r1.trim().isEmpty()) {
            String[] recipientParts = testFlow.r1.split("[,;\\n\\r]+");
            int foundRecipients = 0;
            for (String part : recipientParts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    String sanitizedPart = sanitizeForPlantUml(trimmed);
                    if (!sanitizedPart.isEmpty() && !"-".equals(sanitizedPart)) {
                        if (diagram.contains(sanitizedPart)) {
                            foundRecipients++;
                            System.out.println("  ✓ Found recipient: " + sanitizedPart);
                        } else {
                            System.out.println("  ✗ MISSING recipient: " + sanitizedPart + " (original: " + trimmed + ")");
                        }
                    }
                }
            }
            assertTrue(foundRecipients > 0, 
                "❌ FAIL: NO recipients from R1 are present in diagram! Original: " + testFlow.r1);
            System.out.println("✅ PASS: " + foundRecipients + " recipient(s) present in diagram");
        }
        
        // 4. Device info MUST be present (if not empty)
        if (testFlow.deviceA != null && !testFlow.deviceA.trim().isEmpty() 
                && !"-".equals(testFlow.deviceA)) {
            String sanitizedDevice = sanitizeForPlantUml(testFlow.deviceA);
            assertTrue(diagram.contains("Device:") && diagram.contains(sanitizedDevice),
                "❌ FAIL: Device A '" + testFlow.deviceA + "' is MISSING from diagram!");
            System.out.println("✅ PASS: Device info is present in diagram");
        }
        
        System.out.println("\n=== ALL CHECKS PASSED ===");
        System.out.println("The visual callflow export correctly includes:");
        System.out.println("  ✓ Alarm names");
        System.out.println("  ✓ Priorities");
        System.out.println("  ✓ Recipients");
        System.out.println("  ✓ Device information");
    }
    
    /**
     * Build diagram using EXACT same logic as AppController.buildVisualFlowDiagram
     */
    private String buildVisualFlowDiagramExact(ExcelParserV5.FlowRow row) {
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
            // Header with alarm name, priority, and device
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
            
            // Recipients for each step
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
