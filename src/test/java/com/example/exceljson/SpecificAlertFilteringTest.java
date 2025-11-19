package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify specific alert types that should be filtered out.
 */
public class SpecificAlertFilteringTest {

    @Test
    public void testProblematicAlertsAreFiltered() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("XMLParser.xml");
        if (!xmlFile.exists()) {
            System.out.println("XMLParser.xml not found, skipping test");
            return;
        }
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Collect all alert types
        Set<String> processedAlertTypes = clinicals.stream()
            .map(flow -> flow.alarmName)
            .filter(name -> name != null && !name.isEmpty())
            .map(String::toUpperCase)
            .collect(Collectors.toSet());
        
        System.out.println("\nChecking for problematic alerts that should be filtered...\n");
        
        // These specific alert types should NOT be processed because they don't have
        // active CREATE DATAUPDATE rules (as per the problem statement)
        String[] shouldNotProcess = {
            "AFIB",
            "ST CHANGES",
            "PSVT",
            "ETCO2 HI",
            "ETCO2 LO",
            "ETCO2 LOW", 
            "ETCO2 HIGH",
            "PAUSE"
        };
        
        int foundCount = 0;
        for (String alertType : shouldNotProcess) {
            if (processedAlertTypes.contains(alertType)) {
                foundCount++;
                System.out.println("❌ FAIL: Alert type '" + alertType + "' is being processed but should be filtered out");
            } else {
                System.out.println("✅ PASS: Alert type '" + alertType + "' is correctly filtered out");
            }
        }
        
        if (foundCount > 0) {
            fail(foundCount + " alert types that should be filtered are still being processed");
        } else {
            System.out.println("\n✅ All problematic alerts are correctly filtered out!");
        }
    }
    
    @Test
    public void testExpectedAlertsAreProcessed() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("XMLParser.xml");
        if (!xmlFile.exists()) {
            System.out.println("XMLParser.xml not found, skipping test");
            return;
        }
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Collect all alert types
        Set<String> processedAlertTypes = clinicals.stream()
            .map(flow -> flow.alarmName)
            .filter(name -> name != null && !name.isEmpty())
            .collect(Collectors.toSet());
        
        System.out.println("\nChecking for expected alerts that should be processed...\n");
        
        // These alerts SHOULD be processed (they have active CREATE rules)
        String[] shouldProcess = {
            "Probe Disconnect",
            "SpO No Sensor",
            "SpO Sensor Off"
        };
        
        int missingCount = 0;
        for (String expectedAlert : shouldProcess) {
            boolean found = processedAlertTypes.stream()
                .anyMatch(processed -> processed.equalsIgnoreCase(expectedAlert));
            
            if (found) {
                System.out.println("✅ PASS: Alert type '" + expectedAlert + "' is correctly processed");
            } else {
                missingCount++;
                System.out.println("❌ FAIL: Alert type '" + expectedAlert + "' should be processed but is missing");
            }
        }
        
        if (missingCount > 0) {
            fail(missingCount + " expected alerts are missing");
        } else {
            System.out.println("\n✅ All expected alerts are being processed!");
        }
    }
}
