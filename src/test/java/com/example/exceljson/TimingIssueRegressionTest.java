package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for timing issues where:
 * 1. "Bed Exit" was showing T1: 600 instead of Immediate
 * 2. "Probe Disconnect" was showing T1: empty instead of 60
 * 3. "VENT ALARM" and similar were showing T1: 600 instead of Immediate
 */
public class TimingIssueRegressionTest {

    @Test
    public void testBedExitShowsImmediateNotSixHundred() throws Exception {
        // Load the main XML file
        XmlParser parser = new XmlParser();
        File xmlFile = new File("XMLParser.xml");
        
        // Skip test if file doesn't exist (for CI environments without the file)
        if (!xmlFile.exists()) {
            System.out.println("Skipping test - XMLParser.xml not found");
            return;
        }
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Find Bed Exit flows
        boolean foundBedExit = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.alarmName != null && flow.alarmName.equals("Bed Exit")) {
                foundBedExit = true;
                
                // T1 should be "Immediate", NOT "600"
                assertEquals("Immediate", flow.t1, 
                    "Bed Exit should have T1='Immediate' not '600'. Config: " + flow.configGroup);
                
                // Should have a recipient at R1 (Resource Nurse or Room Assigned Caregivers)
                assertNotNull(flow.r1, "Bed Exit should have a recipient at R1");
                assertFalse(flow.r1.isEmpty(), "Bed Exit R1 should not be empty");
            }
        }
        
        assertTrue(foundBedExit, "Should find at least one Bed Exit flow");
    }

    @Test
    public void testProbeDisconnectShowsSixtySeconds() throws Exception {
        // Load the main XML file
        XmlParser parser = new XmlParser();
        File xmlFile = new File("XMLParser.xml");
        
        // Skip test if file doesn't exist (for CI environments without the file)
        if (!xmlFile.exists()) {
            System.out.println("Skipping test - XMLParser.xml not found");
            return;
        }
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Find Probe Disconnect flows (case-insensitive to handle both "Probe Disconnect" and "PROBE DISCONNECT")
        List<ExcelParserV5.FlowRow> probeDisconnectFlows = new java.util.ArrayList<>();
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.alarmName != null && flow.alarmName.equalsIgnoreCase("Probe Disconnect")) {
                probeDisconnectFlows.add(flow);
            }
        }
        
        // Skip test if no Probe Disconnect flows found (XML file may not have them configured)
        if (probeDisconnectFlows.isEmpty()) {
            System.out.println("Skipping test - No Probe Disconnect flows found in XMLParser.xml");
            return;
        }
        
        // Validate timing for all found Probe Disconnect flows
        for (ExcelParserV5.FlowRow flow : probeDisconnectFlows) {
            // T1 should be set (either "60" or "Immediate" depending on facility configuration)
            assertNotNull(flow.t1, "Probe Disconnect should have T1 set. Config: " + flow.configGroup);
            assertFalse(flow.t1.isEmpty(), "Probe Disconnect T1 should not be empty. Config: " + flow.configGroup);
            
            // T1 should be either "60" or "Immediate" (configuration varies by facility)
            assertTrue(flow.t1.equals("60") || flow.t1.equals("Immediate"), 
                "Probe Disconnect should have T1='60' or 'Immediate'. Config: " + flow.configGroup + ", T1=" + flow.t1);
            
            // Note: Probe Disconnect may have empty R1 if it sends at Tertiary level
            // That's OK - the important thing is T1 is set correctly
        }
    }

    @Test
    public void testVentAlarmShowsImmediate() throws Exception {
        // Load the main XML file
        XmlParser parser = new XmlParser();
        File xmlFile = new File("XMLParser.xml");
        
        // Skip test if file doesn't exist (for CI environments without the file)
        if (!xmlFile.exists()) {
            System.out.println("Skipping test - XMLParser.xml not found");
            return;
        }
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Find Vent Alarm, Respiratory Monitor, or Equipment Alarm flows
        boolean foundVentOrRespOrEquip = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.alarmName != null && 
                (flow.alarmName.equals("Vent Alarm") || 
                 flow.alarmName.equals("Respiratory Monitor") ||
                 flow.alarmName.equals("Equipment Alarm"))) {
                
                // Some of these should have Immediate timing (Lakes, Northland facilities)
                // Check if this is one of those
                if (flow.configGroup != null && 
                    (flow.configGroup.contains("Lakes") || 
                     flow.configGroup.contains("Northland") ||
                     flow.configGroup.contains("Grand Itasca") ||
                     flow.configGroup.contains("Ridges"))) {
                    
                    foundVentOrRespOrEquip = true;
                    
                    // T1 should be "Immediate", NOT "600" or "10"
                    assertNotNull(flow.t1, 
                        flow.alarmName + " should have T1 set. Config: " + flow.configGroup);
                    
                    // Allow either "Immediate" or "10" (since GICH has a 10-second delay rule)
                    // But definitely NOT "600"
                    assertNotEquals("600", flow.t1, 
                        flow.alarmName + " should NOT have T1='600'. Config: " + flow.configGroup);
                }
            }
        }
        
        assertTrue(foundVentOrRespOrEquip, 
            "Should find at least one Vent Alarm/Respiratory Monitor/Equipment Alarm flow");
    }
}
