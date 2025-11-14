package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for XmlParser functionality.
 */
public class XmlParserTest {

    @Test
    public void testLoadSampleXml() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load the sample XML file
        File xmlFile = new File("src/test/resources/sample-engage.xml");
        assertTrue(xmlFile.exists(), "Sample XML file should exist");
        
        parser.load(xmlFile);
        
        // Verify units were created
        List<ExcelParserV5.UnitRow> units = parser.getUnits();
        assertFalse(units.isEmpty(), "Should have at least one unit");
        
        // Verify clinicals were created
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // Check that alert types were extracted correctly
        boolean foundLowHR = false;
        boolean foundHighHR = false;
        boolean foundAPNEA = false;
        
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.alarmName.contains("Low Heart Rate")) {
                foundLowHR = true;
                assertEquals("VMP", flow.deviceA, "Device should be VMP");
                assertEquals("30", flow.t1, "T1 should be 30");
                assertEquals("2", flow.priorityRaw, "Priority should be 2");
                assertEquals("10", flow.ttlValue, "TTL should be 10");
            }
            if (flow.alarmName.contains("High Heart Rate")) {
                foundHighHR = true;
            }
            if (flow.alarmName.contains("APNEA")) {
                foundAPNEA = true;
            }
        }
        
        assertTrue(foundLowHR, "Should find Low Heart Rate alarm");
        assertTrue(foundHighHR, "Should find High Heart Rate alarm");
        assertTrue(foundAPNEA, "Should find APNEA alarm");
        
        // Verify unit has correct facility
        boolean foundEastBank = false;
        for (ExcelParserV5.UnitRow unit : units) {
            if (unit.facility.equals("East Bank")) {
                foundEastBank = true;
                assertTrue(unit.unitNames.contains("CVICU"), "Should have CVICU unit");
            }
        }
        assertTrue(foundEastBank, "Should find East Bank facility");
    }
    
    @Test
    public void testLoadSummary() throws Exception {
        XmlParser parser = new XmlParser();
        File xmlFile = new File("src/test/resources/sample-engage.xml");
        parser.load(xmlFile);
        
        String summary = parser.getLoadSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("XML Load Complete"), "Summary should indicate XML load");
        assertTrue(summary.contains("Unit rows"), "Summary should mention units");
        assertTrue(summary.contains("Clinical rows"), "Summary should mention clinicals");
    }
    
    @Test
    public void testDifferentVersionNumbers() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load XML file with different version-major and version-minor (5.12)
        File xmlFile = new File("src/test/resources/sample-engage-v5-12.xml");
        assertTrue(xmlFile.exists(), "Sample XML file with different version should exist");
        
        // Should parse successfully regardless of version numbers
        parser.load(xmlFile);
        
        // Verify data was parsed
        List<ExcelParserV5.UnitRow> units = parser.getUnits();
        assertFalse(units.isEmpty(), "Should have at least one unit");
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // Verify the test alert was parsed
        boolean foundTestAlert = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.alarmName.contains("Test Alert")) {
                foundTestAlert = true;
                assertEquals("VMP", flow.deviceA, "Device should be VMP");
                assertEquals("15", flow.t1, "T1 should be 15");
                assertEquals("1", flow.priorityRaw, "Priority should be 1");
                assertEquals("5", flow.ttlValue, "TTL should be 5");
            }
        }
        assertTrue(foundTestAlert, "Should find Test Alert");
        
        // Verify facility and unit
        boolean foundTestHospital = false;
        for (ExcelParserV5.UnitRow unit : units) {
            if (unit.facility.equals("Test Hospital")) {
                foundTestHospital = true;
                assertTrue(unit.unitNames.contains("ICU"), "Should have ICU unit");
            }
        }
        assertTrue(foundTestHospital, "Should find Test Hospital facility");
    }
}
