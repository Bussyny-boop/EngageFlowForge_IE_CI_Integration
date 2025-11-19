package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the issue where SEND dataupdate rules process alert types
 * that don't have corresponding CREATE DATAUPDATE rules.
 * 
 * Problem: The SEND PRIMARY rule includes many alert types (LHR, HHR, APNEA, etc.),
 * but only 4 of them have CREATE DATAUPDATE rules:
 * - Probe Disconnect
 * - SpO No Sensor
 * - SpO Sensor Off
 * - SpO No Pulse
 * 
 * Expected: The SEND rule should ONLY process these 4 alert types.
 * All other alert types should be filtered out.
 */
public class DataUpdateFilterIssueTest {

    @Test
    public void testSendRuleOnlyProcessesAlertsWithCreateRules() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-filter-issue.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // Collect all alert types from the flows
        Set<String> processedAlertTypes = clinicals.stream()
            .map(flow -> flow.alarmName)
            .filter(name -> name != null && !name.isEmpty())
            .collect(Collectors.toSet());
        
        System.out.println("Processed alert types: " + processedAlertTypes);
        
        // These are the ONLY alert types that should be processed
        // (they have corresponding CREATE DATAUPDATE rules)
        Set<String> expectedAlertTypes = Set.of(
            "PROBE DISCONNECT",
            "SPO NO SENSOR",
            "SPO SENSOR OFF",
            "SPO NO PULSE"
        );
        
        // Normalize for comparison
        Set<String> normalizedProcessed = processedAlertTypes.stream()
            .map(String::toUpperCase)
            .map(s -> s.replace("Ō", "O")) // Handle unicode
            .collect(Collectors.toSet());
        
        Set<String> normalizedExpected = expectedAlertTypes.stream()
            .map(String::toUpperCase)
            .collect(Collectors.toSet());
        
        // These alert types should NOT be processed (no CREATE rules for them)
        String[] shouldNotProcess = {
            "LHR", "HHR", "APNEA", "AFIB", "ST CHANGES", "PAUSE", "PSVT",
            "ETCO2 HI", "ETCO2 LO", "ETCO2 LOW", "ETCO2 HIGH",
            "MAP HIGH", "MAP LOW", "SPO2 HIGH", "SPO2 LOW"
        };
        
        for (String alertType : shouldNotProcess) {
            String normalized = alertType.toUpperCase();
            assertFalse(normalizedProcessed.contains(normalized),
                "Alert type '" + alertType + "' should NOT be processed because it has no CREATE DATAUPDATE rule");
        }
        
        // Verify that all processed alerts are in the expected set
        for (String processed : normalizedProcessed) {
            // Normalize both for comparison (remove spaces, handle unicode, etc.)
            String processedNormalized = processed.replace(" ", "").replace("Ō", "O").toUpperCase();
            boolean found = false;
            for (String expected : normalizedExpected) {
                String expectedNormalized = expected.replace(" ", "").toUpperCase();
                if (processedNormalized.equals(expectedNormalized) || 
                    processedNormalized.contains(expectedNormalized) ||
                    expectedNormalized.contains(processedNormalized)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, 
                "Alert type '" + processed + "' is being processed but it shouldn't be. " +
                "Only these should be processed: " + normalizedExpected);
        }
    }
    
    @Test
    public void testOnlyFourAlertTypesAreProcessed() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-filter-issue.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Count unique alert types
        Set<String> uniqueAlertTypes = clinicals.stream()
            .map(flow -> flow.alarmName)
            .filter(name -> name != null && !name.isEmpty())
            .collect(Collectors.toSet());
        
        System.out.println("Unique alert types processed: " + uniqueAlertTypes);
        System.out.println("Count: " + uniqueAlertTypes.size());
        
        // Should be at most 4 alert types (the ones with CREATE rules)
        // Note: SpO No Pulse might not appear if it's not in the SEND rule's view
        assertTrue(uniqueAlertTypes.size() <= 4,
            "Should process at most 4 alert types (Probe Disconnect, SpO No Sensor, SpO Sensor Off, SpO No Pulse). " +
            "Found: " + uniqueAlertTypes);
        
        // All processed alert types should be in the expected set
        Set<String> expectedLowercase = Set.of(
            "probe disconnect",
            "spo no sensor",
            "spo sensor off",
            "spo no pulse"
        );
        
        for (String alertType : uniqueAlertTypes) {
            String lowercase = alertType.toLowerCase();
            assertTrue(expectedLowercase.contains(lowercase),
                "Alert type '" + alertType + "' should not be processed. " +
                "Only Probe Disconnect, SpO No Sensor, SpO Sensor Off, SpO No Pulse should be processed.");
        }
    }
}
