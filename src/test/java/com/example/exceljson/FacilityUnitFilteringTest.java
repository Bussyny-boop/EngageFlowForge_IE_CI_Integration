package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that verifies facility and unit filtering based on CREATE rules.
 * SEND rules should only process facilities and units that are defined in CREATE rules.
 */
public class FacilityUnitFilteringTest {

    @Test
    public void testFacilityAndUnitFilteringWithPositiveFilters() throws Exception {
        // Load test XML with facility and unit filtering scenarios
        File xmlFile = new File("src/test/resources/test-facility-unit-filtering.xml");
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // Expected: Only 3 flows should be created
        // 1. Need RN - Hospital A - ICU (valid)
        // 2. Call Button - Hospital A - ICU (valid, NOT in Psych)
        // 3. Call Button - Hospital A - ER (valid, NOT in Psych)
        
        // Should NOT create:
        // - Need RN - Hospital B - ICU (invalid facility - Hospital B not in CREATE)
        // - Need RN - Hospital A - ER (invalid unit - ER not in CREATE for Need RN)
        
        assertEquals(3, nurseCalls.size(), 
            "Should only process 3 flows: Need RN/Hospital A/ICU + Call Button/Hospital A/ICU + Call Button/Hospital A/ER");
        
        // Verify Need RN flow exists (should be only 1)
        long needRNCount = nurseCalls.stream()
            .filter(f -> "Need RN".equals(f.alarmName))
            .count();
        assertEquals(1, needRNCount, "Should find exactly 1 Need RN flow (Hospital A, ICU only)");
        
        // Verify Call Button flows exist for Hospital A with ICU and ER (but not Psych)
        long callButtonCount = nurseCalls.stream()
            .filter(f -> "Call Button".equals(f.alarmName))
            .count();
        assertEquals(2, callButtonCount, 
            "Should find 2 Call Button flows (Hospital A/ICU and Hospital A/ER)");
    }
    
    @Test
    public void testNotInRelationForUnits() throws Exception {
        // Load test XML
        File xmlFile = new File("src/test/resources/test-facility-unit-filtering.xml");
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // Verify Call Button flows use NOT_IN relation correctly
        // CREATE rule uses NOT_IN for Psych, so ICU and ER should be allowed
        long callButtonFlows = nurseCalls.stream()
            .filter(f -> "Call Button".equals(f.alarmName))
            .count();
        
        assertEquals(2, callButtonFlows,
            "NOT_IN relation should allow ICU and ER (all units except Psych)");
    }
    
    @Test
    public void testInvalidFacilityFiltered() throws Exception {
        // Load test XML
        File xmlFile = new File("src/test/resources/test-facility-unit-filtering.xml");
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // The SEND rule for "Need RN | Hospital B | ICU" should be filtered out
        // because Hospital B is not in the CREATE rule (only Hospital A is)
        long needRNCount = nurseCalls.stream()
            .filter(f -> "Need RN".equals(f.alarmName))
            .count();
        
        // Should be only 1 (Hospital A, ICU), not 2
        assertEquals(1, needRNCount, 
            "Need RN flow for Hospital B should be filtered out");
    }
    
    @Test
    public void testInvalidUnitFiltered() throws Exception {
        // Load test XML
        File xmlFile = new File("src/test/resources/test-facility-unit-filtering.xml");
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // The SEND rule for "Need RN | Hospital A | ER" should be filtered out
        // because ER is not in the CREATE rule for Need RN (only ICU is)
        long needRNCount = nurseCalls.stream()
            .filter(f -> "Need RN".equals(f.alarmName))
            .count();
        
        // Should be only 1 (Hospital A, ICU), not 2
        assertEquals(1, needRNCount, 
            "Need RN flow for ER unit should be filtered out");
    }
}
