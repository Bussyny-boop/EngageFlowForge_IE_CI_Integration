package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for single-level Group state escalation with facility filtering.
 * 
 * Requirements:
 * 1. When a CREATE DataUpdate rule has a facility filter (e.g., Facility_name_GICH)
 *    and a SET TO GROUP rule sets state="Group" with matching alert types,
 *    the config group should use the facility from the CREATE rule
 * 2. Since State=Group (normalized to Primary) with NO Secondary escalation,
 *    this is a single-level escalation
 * 3. Config group should be "FacilityName_AllUnits_Clinicals" not "All_Facilities_AllUnits_Clinicals"
 * 
 * Problem: "SET TO GROUP | IMMEDIATE | BED EXIT | SAFETY ALARM | NURSECALL EMDAN | GICH | NEW REQUEST"
 * isn't working as expected. It should have "GrandItasca_AllUnits" since it's tied to a facility.
 */
public class SingleLevelGroupFacilityTest {

    @Test
    public void testSingleLevelGroupStateUsesFacilityFromCreateRule() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-single-level-group-facility.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // Find the Bed Exit flow
        ExcelParserV5.FlowRow bedExitFlow = clinicals.stream()
            .filter(f -> "Bed Exit".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(bedExitFlow, "Should find Bed Exit flow");
        
        // Verify config group includes facility name from CREATE rule
        assertTrue(bedExitFlow.configGroup.contains("Grand Itasca") || bedExitFlow.configGroup.contains("GrandItasca"),
            "Config group should contain 'Grand Itasca' or 'GrandItasca', got: " + bedExitFlow.configGroup);
        
        // Config group should NOT be generic "All_Facilities"
        assertFalse(bedExitFlow.configGroup.contains("All_Facilities"),
            "Config group should NOT contain 'All_Facilities' when facility is specified in CREATE rule, got: " + bedExitFlow.configGroup);
        
        // Should have only one recipient (single-level escalation with Group state)
        assertNotNull(bedExitFlow.r1, "Should have R1 recipient");
        assertTrue(bedExitFlow.r2 == null || bedExitFlow.r2.isEmpty(), 
            "Should NOT have R2 recipient (single-level Group state), got: " + bedExitFlow.r2);
        
        // First recipient should be immediate (Group state is treated as Primary)
        assertEquals("Immediate", bedExitFlow.t1, "T1 should be Immediate for Group state");
        
        // Recipient should be either VGroup or VAssign (both are valid)
        assertTrue(bedExitFlow.r1.contains("NURSE") || bedExitFlow.r1.contains("Room") || bedExitFlow.r1.contains("VGroup"),
            "R1 should contain NURSE, Room assignment, or VGroup, got: " + bedExitFlow.r1);
        
        // Device should be VMP
        assertEquals("VMP", bedExitFlow.deviceA, "Device should be VMP");
        
        // With the fix, we should now have MULTIPLE Bed Exit flows (one for each SEND GROUP rule)
        long bedExitCount = clinicals.stream()
            .filter(f -> "Bed Exit".equals(f.alarmName))
            .count();
        assertEquals(2, bedExitCount, "Should have 2 Bed Exit flows (one for each SEND GROUP rule)");
    }
    
    @Test
    public void testSafetyAlarmAlsoUsesFacility() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-single-level-group-facility.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Find the Safety Alarm flow
        ExcelParserV5.FlowRow safetyAlarmFlow = clinicals.stream()
            .filter(f -> "Safety Alarm".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(safetyAlarmFlow, "Should find Safety Alarm flow");
        
        // Verify config group includes facility name
        assertTrue(safetyAlarmFlow.configGroup.contains("Grand Itasca") || safetyAlarmFlow.configGroup.contains("GrandItasca"),
            "Config group should contain 'Grand Itasca' or 'GrandItasca', got: " + safetyAlarmFlow.configGroup);
        
        assertFalse(safetyAlarmFlow.configGroup.contains("All_Facilities"),
            "Config group should NOT contain 'All_Facilities', got: " + safetyAlarmFlow.configGroup);
    }
    
    @Test
    public void testBothFlowsHaveSameConfigGroup() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-single-level-group-facility.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Both Bed Exit and Safety Alarm should have the same config group
        // since they share the same CREATE rule with facility filter
        ExcelParserV5.FlowRow bedExit = clinicals.stream()
            .filter(f -> "Bed Exit".equals(f.alarmName))
            .findFirst()
            .orElse(null);
            
        ExcelParserV5.FlowRow safetyAlarm = clinicals.stream()
            .filter(f -> "Safety Alarm".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(bedExit);
        assertNotNull(safetyAlarm);
        
        assertEquals(bedExit.configGroup, safetyAlarm.configGroup,
            "Both flows should have the same config group since they share the same CREATE rule");
    }
    
    @Test
    public void testMultipleSendGroupRulesCreateSeparateFlows() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-single-level-group-facility.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Count flows for "Bed Exit"
        long bedExitCount = clinicals.stream()
            .filter(f -> "Bed Exit".equals(f.alarmName))
            .count();
        
        // With the fix, we should now have 2 Bed Exit flows (one for each SEND GROUP rule)
        assertEquals(2, bedExitCount, "Should have 2 Bed Exit flows (one for each SEND GROUP rule)");
        
        // All Bed Exit flows should have facility in config group
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("Bed Exit".equals(flow.alarmName)) {
                assertTrue(flow.configGroup.contains("Grand Itasca") || flow.configGroup.contains("GrandItasca"),
                    "All Bed Exit flows should have Grand Itasca in config group, got: " + flow.configGroup);
            }
        }
    }
}
