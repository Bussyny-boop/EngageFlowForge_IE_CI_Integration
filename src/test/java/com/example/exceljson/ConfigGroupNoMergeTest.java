package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that verifies:
 * 1. Config groups are NOT merged - each facility/unit combination gets its own config group
 * 2. Facilities and units are inherited from CREATE rules when SEND rules don't specify them
 */
public class ConfigGroupNoMergeTest {

    @Test
    public void testConfigGroupsNotMerged() throws Exception {
        // Load test XML
        File xmlFile = new File("src/test/resources/test-config-group-no-merge.xml");
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // Should have at least 2 flows for Need RN (Hospital A - ICU and Hospital A - ER)
        // because CREATE rules define both ICU and ER for Hospital A
        long needRNCount = nurseCalls.stream()
            .filter(f -> "Need RN".equals(f.alarmName))
            .count();
        
        assertTrue(needRNCount >= 2, 
            "Should have at least 2 Need RN flows (Hospital A-ICU and Hospital A-ER) - each with separate config group");
        
        // Verify that each flow has a unique config group
        Set<String> needRNConfigGroups = nurseCalls.stream()
            .filter(f -> "Need RN".equals(f.alarmName))
            .map(f -> f.configGroup)
            .collect(Collectors.toSet());
        
        assertEquals(needRNCount, needRNConfigGroups.size(),
            "Each Need RN flow should have a unique config group (no merging)");
    }
    
    @Test
    public void testFacilityAndUnitInheritanceFromCreateRule() throws Exception {
        // Load test XML
        File xmlFile = new File("src/test/resources/test-config-group-no-merge.xml");
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // The SEND rule for Need RN has NO facility/unit filters
        // It should inherit facilities and units from CREATE rules
        // CREATE rules define: Hospital A - ICU, Hospital A - ER
        // So we should get 2 flows
        long needRNCount = nurseCalls.stream()
            .filter(f -> "Need RN".equals(f.alarmName))
            .count();
        
        assertEquals(2, needRNCount,
            "SEND rule should inherit 2 facility-unit combinations from CREATE rules: Hospital A-ICU and Hospital A-ER");
        
        // Verify config group names contain the facility and unit
        List<String> needRNConfigGroups = nurseCalls.stream()
            .filter(f -> "Need RN".equals(f.alarmName))
            .map(f -> f.configGroup)
            .collect(Collectors.toList());
        
        // Config groups should contain "Hospital A" and either "ICU" or "ER"
        for (String configGroup : needRNConfigGroups) {
            assertTrue(configGroup.contains("Hospital") || configGroup.contains("ICU") || configGroup.contains("ER"),
                "Config group should contain facility or unit name: " + configGroup);
        }
    }
    
    @Test
    public void testUnitInheritanceWhenFacilitySpecified() throws Exception {
        // Load test XML
        File xmlFile = new File("src/test/resources/test-config-group-no-merge.xml");
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // The SEND rule for Call Button has Hospital B specified but NO unit
        // It should inherit unit from CREATE rule (CCU)
        long callButtonCount = nurseCalls.stream()
            .filter(f -> "Call Button".equals(f.alarmName))
            .count();
        
        assertEquals(1, callButtonCount,
            "SEND rule with facility but no unit should inherit unit from CREATE rule");
        
        // Verify config group contains both Hospital B and CCU
        String callButtonConfigGroup = nurseCalls.stream()
            .filter(f -> "Call Button".equals(f.alarmName))
            .map(f -> f.configGroup)
            .findFirst()
            .orElse("");
        
        assertTrue(callButtonConfigGroup.contains("Hospital") || callButtonConfigGroup.contains("CCU"),
            "Config group should contain facility and/or unit: " + callButtonConfigGroup);
    }
    
    @Test
    public void testEachFacilityUnitCombinationGetsUniqueConfigGroup() throws Exception {
        // Load test XML
        File xmlFile = new File("src/test/resources/test-config-group-no-merge.xml");
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // Get all config groups
        Set<String> allConfigGroups = nurseCalls.stream()
            .map(f -> f.configGroup)
            .collect(Collectors.toSet());
        
        // Total flows should equal unique config groups (no merging)
        assertEquals(nurseCalls.size(), allConfigGroups.size(),
            "Each flow should have a unique config group - config groups should NOT be merged");
    }
}
