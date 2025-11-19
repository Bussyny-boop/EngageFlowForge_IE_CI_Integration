package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that SEND rules only process alert types that exist in CREATE rule views.
 * 
 * Problem statement:
 * - CREATE rule has: VENT ALARM, RESPIRATORY MONITOR, EQUIPMENT, NURSECALL EMDAN, GICH, NEW REQUEST
 * - SEND rule has: HIGH CALLBACK, VENT OUT, RESP OUT, EQUIP OUT, ROOM NURSE
 * - None of the alerts in SEND rule match the alerts in CREATE rule
 * - Therefore, the SEND rule should NOT be processed
 */
public class CreateViewAlertFilteringTest {

    @Test
    public void testSendRuleNotProcessedWhenNoAlertsMatchCreate() throws Exception {
        XmlParser parser = new XmlParser();
        File xmlFile = new File("src/test/resources/test-create-view-alert-filtering.xml");
        
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> flows = parser.getNurseCalls();
        
        // The SEND rule should NOT create any flows because none of its alert types
        // (HIGH CALLBACK, VENT OUT, RESP OUT, EQUIP OUT, ROOM NURSE)
        // match any of the CREATE rule's alert types
        // (VENT ALARM, RESPIRATORY MONITOR, EQUIPMENT, NURSECALL EMDAN, GICH, NEW REQUEST)
        assertEquals(0, flows.size(), 
            "SEND rule should not be processed when its alert types don't match CREATE rule views");
    }
    
    @Test
    public void testSendRuleFilteredWhenSomeAlertsMatchCreate() throws Exception {
        // This test verifies that when a SEND rule has SOME alerts that match CREATE rule
        // and SOME that don't, only the matching alerts are processed
        // This is the ACTUAL behavior we want to test based on the problem statement
        
        XmlParser parser = new XmlParser();
        File xmlFile = new File("src/test/resources/test-create-view-partial-match.xml");
        
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> flows = parser.getNurseCalls();
        
        // The CREATE rule has: VENT ALARM, EQUIPMENT
        // The SEND rule has: VENT ALARM, VENT OUT, EQUIPMENT
        // Only VENT ALARM and EQUIPMENT should be processed (2 flows)
        // VENT OUT should NOT be processed
        assertEquals(2, flows.size(), 
            "SEND rule should only process alerts that are in CREATE rule");
        
        // Verify the correct alerts were processed
        Set<String> processedAlerts = flows.stream()
            .map(f -> f.alarmName)
            .collect(java.util.stream.Collectors.toSet());
        
        assertTrue(processedAlerts.contains("VENT ALARM"), "VENT ALARM should be processed");
        assertTrue(processedAlerts.contains("EQUIPMENT"), "EQUIPMENT should be processed");
        assertFalse(processedAlerts.contains("VENT OUT"), "VENT OUT should NOT be processed");
    }
}
