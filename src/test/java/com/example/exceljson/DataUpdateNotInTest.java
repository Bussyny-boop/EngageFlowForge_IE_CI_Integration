package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DataUpdate NOT_IN filter relation support.
 * 
 * This test verifies that DataUpdate Create rules with "not_in" relation work correctly:
 * - Alerts NOT in the exclusion list should be processed
 * - Alerts IN the exclusion list should NOT be processed
 */
public class DataUpdateNotInTest {

    @Test
    public void testNotInFilterAllowsNonExcludedAlerts() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load the test XML file with NOT_IN filter
        File xmlFile = new File("src/test/resources/test-dataupdate-not-in.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        // Verify nurse calls were created
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        assertFalse(nurseCalls.isEmpty(), "Should have nurse call flows");
        
        // The DataUpdate Create rule uses NOT_IN with excluded codes:
        // "Staff Assist, Adult Code Blue, Code Blue, STAT C-Section, Infant Code Blue, Peds Code Blue"
        // 
        // The send rule is for "Need RN, Water Request, Toilet Request, Pain Medication"
        // which are NOT in the excluded list, so they should be processed
        
        boolean foundNeedRN = false;
        boolean foundWaterRequest = false;
        boolean foundToiletRequest = false;
        boolean foundPainMedication = false;
        
        for (ExcelParserV5.FlowRow flow : nurseCalls) {
            if (flow.alarmName.equals("Need RN")) {
                foundNeedRN = true;
                assertEquals("VMP", flow.deviceA, "Device should be VMP");
                assertEquals("Immediate", flow.t1, "T1 should be Immediate (defer-delivery-by=1)");
                assertEquals("Normal", flow.priorityRaw, "Priority should be Normal");
                assertEquals("10", flow.ttlValue, "TTL should be 10");
                assertEquals("VAssign:[Room] NURSE", flow.r1, "Recipient should be VAssign:[Room] NURSE");
            }
            if (flow.alarmName.equals("Water Request")) {
                foundWaterRequest = true;
                assertEquals("VMP", flow.deviceA, "Device should be VMP");
            }
            if (flow.alarmName.equals("Toilet Request")) {
                foundToiletRequest = true;
                assertEquals("VMP", flow.deviceA, "Device should be VMP");
            }
            if (flow.alarmName.equals("Pain Medication")) {
                foundPainMedication = true;
                assertEquals("VMP", flow.deviceA, "Device should be VMP");
            }
            
            // Verify that excluded alerts (Code Blue types) are NOT processed
            // These should NOT appear in the flow list
            assertFalse(flow.alarmName.contains("Code Blue"), 
                "Code Blue types should NOT be in flows (excluded by NOT_IN filter)");
            assertFalse(flow.alarmName.equals("Staff Assist"), 
                "Staff Assist should NOT be in flows (excluded by NOT_IN filter)");
            assertFalse(flow.alarmName.equals("STAT C-Section"), 
                "STAT C-Section should NOT be in flows (excluded by NOT_IN filter)");
        }
        
        // Verify that alerts NOT in the exclusion list were processed
        assertTrue(foundNeedRN, "Should find Need RN (not in exclusion list)");
        assertTrue(foundWaterRequest, "Should find Water Request (not in exclusion list)");
        assertTrue(foundToiletRequest, "Should find Toilet Request (not in exclusion list)");
        assertTrue(foundPainMedication, "Should find Pain Medication (not in exclusion list)");
        
        // Verify units were created correctly
        List<ExcelParserV5.UnitRow> units = parser.getUnits();
        assertFalse(units.isEmpty(), "Should have unit rows");
        
        boolean foundTestHospital = false;
        for (ExcelParserV5.UnitRow unit : units) {
            if (unit.facility.equals("Test Hospital")) {
                foundTestHospital = true;
            }
        }
        assertTrue(foundTestHospital, "Should find Test Hospital facility");
    }
    
    @Test
    public void testNotInFilterExcludesListedAlerts() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-not-in.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // Verify that alerts in the NOT_IN exclusion list do NOT appear
        // The exclusion list is: "Staff Assist, Adult Code Blue, Code Blue, STAT C-Section, Infant Code Blue, Peds Code Blue"
        for (ExcelParserV5.FlowRow flow : nurseCalls) {
            String alarmName = flow.alarmName;
            
            // None of these should appear in the flows
            assertNotEquals("Staff Assist", alarmName, "Staff Assist should be excluded");
            assertNotEquals("Adult Code Blue", alarmName, "Adult Code Blue should be excluded");
            assertNotEquals("Code Blue", alarmName, "Code Blue should be excluded");
            assertNotEquals("STAT C-Section", alarmName, "STAT C-Section should be excluded");
            assertNotEquals("Infant Code Blue", alarmName, "Infant Code Blue should be excluded");
            assertNotEquals("Peds Code Blue", alarmName, "Peds Code Blue should be excluded");
        }
    }
    
    @Test
    public void testLoadSummaryWithNotIn() throws Exception {
        XmlParser parser = new XmlParser();
        File xmlFile = new File("src/test/resources/test-dataupdate-not-in.xml");
        parser.load(xmlFile);
        
        String summary = parser.getLoadSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("XML Load Complete"), "Summary should indicate XML load");
        assertTrue(summary.contains("Nurse Call rows"), "Summary should mention nurse calls");
    }
}
