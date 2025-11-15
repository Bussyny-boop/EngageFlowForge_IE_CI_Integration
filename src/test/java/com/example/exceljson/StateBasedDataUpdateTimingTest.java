package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for state-based timing application in DataUpdate rules.
 * 
 * Requirements:
 * 1. When DataUpdate interface views don't have alert_type or unit.name,
 *    apply timing to ALL alerts and units based on the STATE in the condition
 * 2. State determines WHERE to apply timing:
 *    - state=Primary → T2 (time to 2nd recipient)
 *    - state=Secondary → T3 (time to 3rd recipient)
 *    - state=Tertiary → T4 (time to 4th recipient)
 * 3. Group state should be treated as Primary (normalized to Primary)
 * 4. When no role reference found, use destination from settings
 */
public class StateBasedDataUpdateTimingTest {

    @Test
    public void testSecondaryStateAppliesTimingToT3() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-state-based-timing-secondary.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // DataUpdate rule has state=Secondary in condition (no alert_type or unit.name)
        // This should apply timing to T3 (time to 3rd recipient)
        boolean foundFlow = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("APNEA".equals(flow.alarmName)) {
                foundFlow = true;
                assertEquals("VAssign:[Room] NURSE", flow.r1, "R1 should be VAssign:[Room] NURSE");
                assertEquals("Immediate", flow.t1, "T1 should be Immediate");
                assertEquals("VAssign:[Room] CHARGE NURSE", flow.r2, "R2 should be VAssign:[Room] CHARGE NURSE");
                assertTrue(flow.t2 == null || flow.t2.isEmpty(), "T2 should be empty (no Primary escalation)");
                assertEquals("VAssign:[Room] SUPERVISOR", flow.r3, "R3 should be VAssign:[Room] SUPERVISOR");
                assertEquals("90", flow.t3, "T3 should be 90 (from Secondary state DataUpdate)");
            }
        }
        
        assertTrue(foundFlow, "Should find APNEA flow");
    }
    
    @Test
    public void testPrimaryStateAppliesTimingToT2AllAlerts() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-state-based-timing-primary.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertEquals(2, clinicals.size(), "Should have 2 clinical flows (APNEA and BRADY)");
        
        // DataUpdate rule has state=Primary in condition (no alert_type or unit.name)
        // This should apply timing to T2 for ALL alerts
        boolean foundAPNEA = false;
        boolean foundBRADY = false;
        
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("APNEA".equals(flow.alarmName)) {
                foundAPNEA = true;
                assertEquals("VAssign:[Room] NURSE", flow.r1, "APNEA R1 should be VAssign:[Room] NURSE");
                assertEquals("Immediate", flow.t1, "APNEA T1 should be Immediate");
                assertEquals("VAssign:[Room] CHARGE NURSE", flow.r2, "APNEA R2 should be VAssign:[Room] CHARGE NURSE");
                assertEquals("60", flow.t2, "APNEA T2 should be 60 (from Primary state DataUpdate)");
            } else if ("BRADY".equals(flow.alarmName)) {
                foundBRADY = true;
                assertEquals("VAssign:[Room] NURSE", flow.r1, "BRADY R1 should be VAssign:[Room] NURSE");
                assertEquals("Immediate", flow.t1, "BRADY T1 should be Immediate");
                assertEquals("VAssign:[Room] CHARGE NURSE", flow.r2, "BRADY R2 should be VAssign:[Room] CHARGE NURSE");
                assertEquals("60", flow.t2, "BRADY T2 should be 60 (from Primary state DataUpdate)");
            }
        }
        
        assertTrue(foundAPNEA, "Should find APNEA flow");
        assertTrue(foundBRADY, "Should find BRADY flow");
    }
    
    @Test
    public void testGroupStateWithDestinationNoRole() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-state-based-timing-group.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // Group state rule with no role reference should use destination from settings
        boolean foundCodeBlue = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("Code Blue".equals(flow.alarmName)) {
                foundCodeBlue = true;
                assertEquals("VGroup g-adult_code_blue2xxx", flow.r1, 
                    "R1 should be destination from settings with VGroup prefix (no role found)");
                assertEquals("VMP", flow.deviceA, "Device should be VMP");
            }
        }
        
        assertTrue(foundCodeBlue, "Should find Code Blue flow");
    }
    
    @Test
    public void testMultipleStatesWithGlobalDataUpdate() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-state-based-timing-multiple.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // Test scenario with multiple DataUpdate rules at different states
        // Each should apply timing to the correct T position
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("APNEA".equals(flow.alarmName)) {
                assertEquals("VAssign:[Room] NURSE", flow.r1, "R1 should be VAssign:[Room] NURSE");
                assertEquals("Immediate", flow.t1, "T1 should be Immediate");
                assertEquals("VAssign:[Room] CHARGE NURSE", flow.r2, "R2 should be VAssign:[Room] CHARGE NURSE");
                assertEquals("60", flow.t2, "T2 should be 60 (from Primary state DataUpdate)");
                assertEquals("VAssign:[Room] SUPERVISOR", flow.r3, "R3 should be VAssign:[Room] SUPERVISOR");
                assertEquals("90", flow.t3, "T3 should be 90 (from Secondary state DataUpdate)");
            }
        }
    }
}
