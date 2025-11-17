package com.example.exceljson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.util.List;

/**
 * Test interface rule validation logic
 */
class InterfaceValidationTest {

    @Test
    void testInterfaceRuleValidation() throws Exception {
        XmlParser parser = new XmlParser();
        File xmlFile = new File("src/test/resources/test-interface-validation.xml");
        
        parser.load(xmlFile);
        
        List<FlowRow> flowRows = parser.getFlowRows();
        
        // Should only contain valid rules:
        // 1. Valid VMP rule (alert types covered by valid DataUpdate)
        // 2. Both DataUpdate rules (they are always processed)
        
        // Count rules by component and purpose
        long validVmpRules = flowRows.stream()
            .filter(row -> "VMP".equals(row.getComponent()))
            .filter(row -> row.getPurpose().contains("VALID RULE"))
            .count();
            
        long invalidVmpRules = flowRows.stream()
            .filter(row -> "VMP".equals(row.getComponent()))
            .filter(row -> row.getPurpose().contains("INVALID RULE"))
            .count();
            
        long dataUpdateRules = flowRows.stream()
            .filter(row -> "DataUpdate".equals(row.getComponent()))
            .count();
        
        // Assertions
        assertEquals(1, validVmpRules, "Should have 1 valid VMP rule");
        assertEquals(0, invalidVmpRules, "Should have 0 invalid VMP rules (they should be filtered out)");
        assertEquals(2, dataUpdateRules, "Should have 2 DataUpdate rules (they are always processed)");
        
        // Verify the valid VMP rule contains expected alert types
        FlowRow validVmpRow = flowRows.stream()
            .filter(row -> "VMP".equals(row.getComponent()))
            .filter(row -> row.getPurpose().contains("VALID RULE"))
            .findFirst()
            .orElse(null);
            
        assertNotNull(validVmpRow, "Valid VMP rule should exist");
        assertTrue(validVmpRow.getAlertTypes().contains("LHR"), "Should contain LHR alert type");
        assertTrue(validVmpRow.getAlertTypes().contains("HHR"), "Should contain HHR alert type");
        assertTrue(validVmpRow.getAlertTypes().contains("APNEA"), "Should contain APNEA alert type");
    }
    
    @Test
    void testDataUpdateInterfaceValidation() throws Exception {
        XmlParser parser = new XmlParser();
        File xmlFile = new File("src/test/resources/test-dataupdate-interface.xml");
        
        parser.load(xmlFile);
        
        List<FlowRow> flowRows = parser.getFlowRows();
        
        // All VMP rules in this file should be processed because they have
        // matching DataUpdate rules with valid logic
        long vmpRules = flowRows.stream()
            .filter(row -> "VMP".equals(row.getComponent()))
            .count();
            
        long dataUpdateRules = flowRows.stream()
            .filter(row -> "DataUpdate".equals(row.getComponent()))
            .count();
        
        assertEquals(2, vmpRules, "Should have 2 VMP rules (both valid)");
        assertEquals(1, dataUpdateRules, "Should have 1 DataUpdate rule");
        
        // Verify that VMP rules have the expected alert types from the view
        for (FlowRow row : flowRows) {
            if ("VMP".equals(row.getComponent())) {
                assertFalse(row.getAlertTypes().isEmpty(), 
                    "VMP rules should have alert types extracted from views");
                
                // Check that these alert types are covered by the DataUpdate rule
                assertTrue(row.getAlertTypes().contains("LHR") || 
                          row.getAlertTypes().contains("HHR") ||
                          row.getAlertTypes().contains("APNEA"),
                    "Alert types should match those covered by DataUpdate rule");
            }
        }
    }
    
    @Test
    void testNoDataUpdateRules() throws Exception {
        // Test with a file that has VMP rules but no DataUpdate rules
        // In this case, all VMP rules should be processed (legacy behavior)
        XmlParser parser = new XmlParser();
        File xmlFile = new File("src/test/resources/sample-engage.xml");
        
        parser.load(xmlFile);
        
        List<FlowRow> flowRows = parser.getFlowRows();
        
        // Should process all active VMP rules even without DataUpdate rules
        long vmpRules = flowRows.stream()
            .filter(row -> "VMP".equals(row.getComponent()))
            .count();
            
        assertTrue(vmpRules > 0, "Should process VMP rules when no DataUpdate rules exist");
    }
}