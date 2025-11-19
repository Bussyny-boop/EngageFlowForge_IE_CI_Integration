package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the fix for the XML Parser role extraction issue with
 * north_western_cdh_test.xml file.
 * 
 * Specifically tests that:
 * 1. Battery Low rule for CDH facility has PCT as first recipient
 * 2. Desat rule for CDH facility has "Group 1" as first recipient
 */
public class NorthWesternCDHRecipientTest {

    @Test
    public void testBatteryLowAndDesatRecipients() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load the north_western_cdh_test.xml file
        File xmlFile = new File("north_western_cdh_test.xml");
        if (!xmlFile.exists()) {
            // Try alternative path
            xmlFile = new File("../north_western_cdh_test.xml");
        }
        assertTrue(xmlFile.exists(), "north_western_cdh_test.xml file should exist");
        
        parser.load(xmlFile);
        
        // Get clinicals
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        System.out.println("\n=== Analyzing Battery Low rules for CDH_2A ===");
        // Find Battery Low rules for CDH facility with unit 2A
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.alarmName.contains("Battery Low") && 
                flow.configGroup.equals("CDH_2A_Clinicals")) {
                System.out.println("\nBattery Low Rule:");
                System.out.println("  Config Group: " + flow.configGroup);
                System.out.println("  Alarm Name: " + flow.alarmName);
                System.out.println("  R1: " + (flow.r1 != null ? flow.r1 : "NULL"));
                System.out.println("  T1: " + (flow.t1 != null ? flow.t1 : "NULL"));
                System.out.println("  R2: " + (flow.r2 != null ? flow.r2 : "NULL"));
                System.out.println("  T2: " + (flow.t2 != null ? flow.t2 : "NULL"));
                System.out.println("  R3: " + (flow.r3 != null ? flow.r3 : "NULL"));
                System.out.println("  T3: " + (flow.t3 != null ? flow.t3 : "NULL"));
                
                // Verify first recipient is PCT
                assertNotNull(flow.r1, "R1 should not be null");
                assertFalse(flow.r1.isEmpty(), "R1 should not be empty");
                assertTrue(flow.r1.contains("VAssign"), 
                    "First recipient should be VAssign (role-based), got: " + flow.r1);
                assertTrue(flow.r1.contains("PCT") || flow.r1.contains("TECH"), 
                    "First recipient should be PCT or TECH, got: " + flow.r1);
                
                // Verify second recipient is RN
                assertNotNull(flow.r2, "R2 should not be null");
                assertFalse(flow.r2.isEmpty(), "R2 should not be empty");
                assertTrue(flow.r2.contains("RN") || flow.r2.contains("Nurse"), 
                    "Second recipient should be RN or Nurse, got: " + flow.r2);
                
                // Verify third recipient is Charge Nurse
                assertNotNull(flow.r3, "R3 should not be null");
                assertFalse(flow.r3.isEmpty(), "R3 should not be empty");
                assertTrue(flow.r3.contains("Charge"), 
                    "Third recipient should be Charge Nurse, got: " + flow.r3);
            }
        }
        
        System.out.println("\n=== Analyzing Desat rules for CDH_2A ===");
        // Find Desat rules for CDH facility with unit 2A
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.alarmName.contains("Desat") && 
                flow.configGroup.equals("CDH_2A_Clinicals")) {
                System.out.println("\nDesat Rule:");
                System.out.println("  Config Group: " + flow.configGroup);
                System.out.println("  Alarm Name: " + flow.alarmName);
                System.out.println("  R1: " + (flow.r1 != null ? flow.r1 : "NULL"));
                System.out.println("  T1: " + (flow.t1 != null ? flow.t1 : "NULL"));
                System.out.println("  R2: " + (flow.r2 != null ? flow.r2 : "NULL"));
                System.out.println("  T2: " + (flow.t2 != null ? flow.t2 : "NULL"));
                System.out.println("  R3: " + (flow.r3 != null ? flow.r3 : "NULL"));
                System.out.println("  T3: " + (flow.t3 != null ? flow.t3 : "NULL"));
                
                // Verify first recipient is Group 1
                assertNotNull(flow.r1, "R1 should not be null");
                assertFalse(flow.r1.isEmpty(), "R1 should not be empty");
                assertTrue(flow.r1.contains("VGroup") || flow.r1.contains("Group"), 
                    "First recipient should be VGroup (group-based), got: " + flow.r1);
                assertTrue(flow.r1.contains("1") || flow.r1.contains("groups"), 
                    "First recipient should reference Group 1, got: " + flow.r1);
                
                // Verify second recipient is RN
                assertNotNull(flow.r2, "R2 should not be null");
                assertFalse(flow.r2.isEmpty(), "R2 should not be empty");
                assertTrue(flow.r2.contains("RN") || flow.r2.contains("Nurse"), 
                    "Second recipient should be RN or Nurse, got: " + flow.r2);
            }
        }
    }
}
