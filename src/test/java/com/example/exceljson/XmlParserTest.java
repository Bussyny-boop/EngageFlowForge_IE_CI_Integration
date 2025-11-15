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
                assertEquals("Normal", flow.priorityRaw, "Priority should be Normal");
                assertEquals("10", flow.ttlValue, "TTL should be 10");
                assertEquals("VAssign:Nurse", flow.r1, "Recipient should be VAssign:Nurse (from role)");
            }
            if (flow.alarmName.contains("High Heart Rate")) {
                foundHighHR = true;
                assertEquals("VAssign:Nurse", flow.r1, "Recipient should be VAssign:Nurse");
            }
            if (flow.alarmName.contains("APNEA")) {
                foundAPNEA = true;
                assertEquals("VAssign:Nurse", flow.r1, "Recipient should be VAssign:Nurse");
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
                assertEquals("High", flow.priorityRaw, "Priority should be High");
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
    
    @Test
    public void testGroupDestination() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load XML file with group destination (g- prefix)
        File xmlFile = new File("src/test/resources/sample-engage-group-dest.xml");
        assertTrue(xmlFile.exists(), "Sample XML file with group destination should exist");
        
        parser.load(xmlFile);
        
        // Verify clinicals were created
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // Verify group destination handling
        boolean foundCodeBlue = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.alarmName.contains("Code Blue")) {
                foundCodeBlue = true;
                assertEquals("VMP", flow.deviceA, "Device should be VMP");
                assertEquals("5", flow.t1, "T1 should be 5");
                assertEquals("Urgent", flow.priorityRaw, "Priority should be Urgent");
                assertEquals("15", flow.ttlValue, "TTL should be 15");
                assertEquals("TRUE", flow.breakThroughDND, "Break through DND should be TRUE");
                // Recipient should be the group name (destination with g- prefix) with VAssign prefix
                assertEquals("VAssign:g-CodeBlueTeam", flow.r1, "Recipient should be VAssign:g-CodeBlueTeam (group destination)");
            }
        }
        assertTrue(foundCodeBlue, "Should find Code Blue alert");
        
        // Verify facility and unit
        List<ExcelParserV5.UnitRow> units = parser.getUnits();
        boolean foundMemorialHospital = false;
        for (ExcelParserV5.UnitRow unit : units) {
            if (unit.facility.equals("Memorial Hospital")) {
                foundMemorialHospital = true;
                assertTrue(unit.unitNames.contains("ER"), "Should have ER unit");
            }
        }
        assertTrue(foundMemorialHospital, "Should find Memorial Hospital facility");
    }
    
    @Test
    public void testDestinationOnlyRecipient() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load XML file with destination only (no role in views)
        File xmlFile = new File("src/test/resources/sample-engage-destination-only.xml");
        assertTrue(xmlFile.exists(), "Sample XML file with destination only should exist");
        
        parser.load(xmlFile);
        
        // Verify nurse calls were created
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        assertFalse(nurseCalls.isEmpty(), "Should have nurse call flows");
        
        // Verify destination-only recipient handling
        boolean foundBedExit = false;
        for (ExcelParserV5.FlowRow flow : nurseCalls) {
            if (flow.alarmName.contains("Bed Exit")) {
                foundBedExit = true;
                assertEquals("OutgoingWCTP", flow.deviceA, "Device should be OutgoingWCTP");
                assertEquals("Immediate", flow.t1, "T1 should be Immediate");
                assertEquals("Normal", flow.priorityRaw, "Priority should be Normal");
                assertEquals("10", flow.ttlValue, "TTL should be 10");
                // Recipient should be the exact destination value when no role is found
                assertEquals("#{bed.room.unit.first.users.devices.lines.number}", flow.r1, 
                    "Recipient should be exact destination value from settings when no role is found");
            }
        }
        assertTrue(foundBedExit, "Should find Bed Exit alert");
        
        // Verify facility and unit
        List<ExcelParserV5.UnitRow> units = parser.getUnits();
        boolean foundWestWing = false;
        for (ExcelParserV5.UnitRow unit : units) {
            if (unit.facility.equals("West Wing")) {
                foundWestWing = true;
                assertTrue(unit.unitNames.contains("ICU"), "Should have ICU unit");
            }
        }
        assertTrue(foundWestWing, "Should find West Wing facility");
    }
}
