package com.example.exceljson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

/**
 * Test for visual flow diagram generation to ensure data is properly displayed
 */
public class VisualFlowDiagramTest {

    @Test
    public void testFlowRowDataPresence() throws Exception {
        // Load test data
        ExcelParserV5 parser = new ExcelParserV5();
        File testFile = new File("CDH_3S_Generated.xlsx");
        
        if (!testFile.exists()) {
            System.out.println("Test file not found, skipping test");
            return;
        }
        
        parser.load(testFile);
        
        // Check that we have nurse calls with recipients
        assertFalse(parser.nurseCalls.isEmpty(), "Should have nurse calls loaded");
        
        // Find a flow with recipients
        ExcelParserV5.FlowRow flowWithRecipients = null;
        for (ExcelParserV5.FlowRow flow : parser.nurseCalls) {
            if (flow.r1 != null && !flow.r1.trim().isEmpty()) {
                flowWithRecipients = flow;
                break;
            }
        }
        
        assertNotNull(flowWithRecipients, "Should have at least one flow with recipients");
        
        // Verify recipient data is present
        assertNotNull(flowWithRecipients.r1, "R1 should not be null");
        assertFalse(flowWithRecipients.r1.trim().isEmpty(), "R1 should not be empty");
        
        // Verify priority data is present
        assertNotNull(flowWithRecipients.priorityRaw, "Priority should not be null");
        
        // Verify alarm name is present
        assertNotNull(flowWithRecipients.alarmName, "Alarm name should not be null");
        assertFalse(flowWithRecipients.alarmName.trim().isEmpty(), "Alarm name should not be empty");
        
        // Print some sample data for verification
        System.out.println("Sample Flow Data:");
        System.out.println("  Alarm Name: " + flowWithRecipients.alarmName);
        System.out.println("  Priority: " + flowWithRecipients.priorityRaw);
        System.out.println("  Config Group: " + flowWithRecipients.configGroup);
        System.out.println("  Device A: " + flowWithRecipients.deviceA);
        System.out.println("  Device B: " + flowWithRecipients.deviceB);
        System.out.println("  R1: " + flowWithRecipients.r1);
        System.out.println("  R2: " + flowWithRecipients.r2);
        System.out.println("  R3: " + flowWithRecipients.r3);
        System.out.println("  T1: " + flowWithRecipients.t1);
        System.out.println("  T2: " + flowWithRecipients.t2);
    }
}
