package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for escalation flows with delayed primary state.
 * 
 * Requirements:
 * - When DataUpdate CREATE rule has defer-delivery-by=60 and sets state to Primary,
 *   the first recipient time (t1) should be "60" (or "60SEC"), not "Immediate"
 * - All escalation levels should show their recipients (Primary, Secondary, Tertiary, Quaternary)
 * - Example: "SET TO PRIMARY | 60SEC | PROBE DISCONNECT | ALARM ACTIVE | ALL UNIT | NORTHLAND"
 */
public class DelayedPrimaryEscalationTest {
    
    @Test
    public void testDelayedPrimaryEscalation_FirstRecipientTiming() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load XML with delayed primary escalation
        File xmlFile = new File("src/test/resources/test-escalation-delayed-primary.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        // Get clinical flows
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // Find the Probe Disconnect flow
        ExcelParserV5.FlowRow flow = null;
        for (ExcelParserV5.FlowRow f : clinicals) {
            if (f.alarmName != null && f.alarmName.contains("Probe Disconnect")) {
                flow = f;
                break;
            }
        }
        assertNotNull(flow, "Should find Probe Disconnect flow");
        
        // Verify the timing and recipients
        System.out.println("\n=== Probe Disconnect Escalation Flow ===");
        System.out.println("Alarm Name: " + flow.alarmName);
        System.out.println("Device A: " + flow.deviceA);
        System.out.println("Config Group: " + flow.configGroup);
        System.out.println("\nRecipients and Timing:");
        System.out.println("t1: " + flow.t1 + " | r1: " + flow.r1);
        System.out.println("t2: " + flow.t2 + " | r2: " + flow.r2);
        System.out.println("t3: " + flow.t3 + " | r3: " + flow.r3);
        System.out.println("t4: " + flow.t4 + " | r4: " + flow.r4);
        System.out.println("t5: " + flow.t5 + " | r5: " + flow.r5);
        
        // CRITICAL: First recipient time should be 60 seconds, not Immediate
        // This is because the DataUpdate CREATE rule has defer-delivery-by=60
        assertNotNull(flow.t1, "First recipient time should not be null");
        assertTrue(flow.t1.equals("60") || flow.t1.equals("60SEC") || flow.t1.equals("1MIN"),
            "First recipient time should be 60 seconds (was: " + flow.t1 + ")");
        
        // Verify all recipients are populated
        assertNotNull(flow.r1, "First recipient should not be null");
        assertFalse(flow.r1.isEmpty(), "First recipient should not be empty");
        assertTrue(flow.r1.contains("NURSE"), "First recipient should contain NURSE");
        
        assertNotNull(flow.r2, "Second recipient should not be null");
        assertFalse(flow.r2.isEmpty(), "Second recipient should not be empty");
        assertTrue(flow.r2.contains("CHARGE NURSE"), "Second recipient should contain CHARGE NURSE");
        
        assertNotNull(flow.r3, "Third recipient should not be null");
        assertFalse(flow.r3.isEmpty(), "Third recipient should not be empty");
        assertTrue(flow.r3.contains("MONITOR TECH"), "Third recipient should contain MONITOR TECH");
        
        assertNotNull(flow.r4, "Fourth recipient should not be null");
        assertFalse(flow.r4.isEmpty(), "Fourth recipient should not be empty");
        assertTrue(flow.r4.contains("UNIT MANAGER"), "Fourth recipient should contain UNIT MANAGER");
        
        // Verify escalation delays (t2, t3, t4 should be 60 seconds each)
        assertNotNull(flow.t2, "Second recipient time should not be null");
        assertTrue(flow.t2.equals("60") || flow.t2.equals("60SEC") || flow.t2.equals("1MIN"),
            "Second recipient time should be 60 seconds (was: " + flow.t2 + ")");
        
        assertNotNull(flow.t3, "Third recipient time should not be null");
        assertTrue(flow.t3.equals("60") || flow.t3.equals("60SEC") || flow.t3.equals("1MIN"),
            "Third recipient time should be 60 seconds (was: " + flow.t3 + ")");
        
        assertNotNull(flow.t4, "Fourth recipient time should not be null");
        assertTrue(flow.t4.equals("60") || flow.t4.equals("60SEC") || flow.t4.equals("1MIN"),
            "Fourth recipient time should be 60 seconds (was: " + flow.t4 + ")");
    }
}
