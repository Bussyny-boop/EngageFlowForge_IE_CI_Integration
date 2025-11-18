package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that config groups are separated by facility.
 * Even when alert types match, if facilities are different, separate rows should be created.
 */
public class ConfigGroupSeparationTest {

    @Test
    public void testConfigGroupSeparationByFacility() throws Exception {
        XmlParser parser = new XmlParser();
        
        // This test uses the test-single-level-group-facility.xml which has:
        // - A DataUpdate CREATE rule with facility: Grand Itasca
        // - A SEND rule for "Bed Exit" and "Safety Alarm" 
        File xmlFile = new File("src/test/resources/test-single-level-group-facility.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // Find all Bed Exit flows
        List<ExcelParserV5.FlowRow> bedExitFlows = clinicals.stream()
            .filter(f -> f.alarmName.equals("Bed Exit"))
            .collect(Collectors.toList());
        
        // Should have at least one Bed Exit flow
        assertFalse(bedExitFlows.isEmpty(), "Should have Bed Exit flows");
        
        // Verify config groups contain the facility name
        for (ExcelParserV5.FlowRow flow : bedExitFlows) {
            // Config groups should contain "Grand Itasca" or "GrandItasca"
            assertTrue(flow.configGroup.contains("Grand Itasca") || flow.configGroup.contains("GrandItasca"),
                "Config group should contain facility name: " + flow.configGroup);
            // Should not contain commas (no merged facilities)
            assertFalse(flow.configGroup.contains(","), 
                "Config group should not contain commas: " + flow.configGroup);
        }
    }
    
    @Test
    public void testGroupStateFacilityDuplicationStillWorks() throws Exception {
        XmlParser parser = new XmlParser();
        
        // This test ensures that facility-specific SEND rules still work correctly
        File xmlFile = new File("src/test/resources/test-group-state-facility-duplication.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // Count flows for "Adult Code Blue" - should only have ONE for Northland
        long adultCodeBlueCount = nurseCalls.stream()
            .filter(f -> f.alarmName.equals("Adult Code Blue"))
            .count();
        
        assertEquals(1, adultCodeBlueCount, 
            "Should have exactly 1 flow for Adult Code Blue (Northland only)");
        
        // Verify the config group is for Northland only
        ExcelParserV5.FlowRow adultCodeBlueFlow = nurseCalls.stream()
            .filter(f -> f.alarmName.equals("Adult Code Blue"))
            .findFirst()
            .orElseThrow();
        
        assertTrue(adultCodeBlueFlow.configGroup.contains("Northland"), 
            "Config group should contain Northland: " + adultCodeBlueFlow.configGroup);
        assertFalse(adultCodeBlueFlow.configGroup.contains("Lakes"), 
            "Config group should NOT contain Lakes: " + adultCodeBlueFlow.configGroup);
        assertFalse(adultCodeBlueFlow.configGroup.contains("GICH"), 
            "Config group should NOT contain GICH: " + adultCodeBlueFlow.configGroup);
    }
}
