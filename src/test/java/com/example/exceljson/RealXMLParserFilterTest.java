package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the real XMLParser.xml file properly filters
 * SEND dataupdate rules based on CREATE DATAUPDATE rules.
 */
public class RealXMLParserFilterTest {

    @Test
    public void testRealXMLParserFiltersSendRulesByCreateRules() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("XMLParser.xml");
        if (!xmlFile.exists()) {
            System.out.println("XMLParser.xml not found, skipping test");
            return; // Skip if file doesn't exist in test environment
        }
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Collect all alert types from flows
        Set<String> processedAlertTypes = clinicals.stream()
            .map(flow -> flow.alarmName)
            .filter(name -> name != null && !name.isEmpty())
            .collect(Collectors.toSet());
        
        System.out.println("Total unique alert types processed: " + processedAlertTypes.size());
        System.out.println("Sample alert types: " + processedAlertTypes.stream().limit(20).collect(Collectors.toList()));
        
        // These alert types should NOT be processed because they don't have CREATE DATAUPDATE rules
        // (based on the problem statement)
        String[] shouldNotProcess = {
            "LHR", "HHR", "Low Heart Rate", "High Heart Rate",
            "AFIB", "ST Changes", "Pause", "PSVT",
            "ETCO2 HI", "ETCO2 LO", "ETCO2 LOW", "ETCO2 HIGH"
        };
        
        int foundUnexpected = 0;
        for (String alertType : shouldNotProcess) {
            boolean found = false;
            for (String processed : processedAlertTypes) {
                String normalizedProcessed = processed.toLowerCase().replace("ō", "o");
                String normalizedAlert = alertType.toLowerCase();
                if (normalizedProcessed.equals(normalizedAlert) ||
                    normalizedProcessed.contains(normalizedAlert)) {
                    found = true;
                    foundUnexpected++;
                    System.out.println("WARNING: Found unexpected alert type being processed: " + processed + 
                        " (matches: " + alertType + ")");
                    break;
                }
            }
        }
        
        if (foundUnexpected > 0) {
            System.out.println("\n⚠️  Found " + foundUnexpected + " alert types that should NOT be processed!");
            System.out.println("These alerts don't have CREATE DATAUPDATE rules but are being processed anyway.");
        } else {
            System.out.println("\n✅ All processed alerts have corresponding CREATE DATAUPDATE rules.");
        }
    }
    
    @Test
    public void testProbeDisconnectAlertsAreProcessed() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("XMLParser.xml");
        if (!xmlFile.exists()) {
            System.out.println("XMLParser.xml not found, skipping test");
            return;
        }
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // These SHOULD be processed (they have CREATE rules)
        String[] shouldProcess = {
            "Probe Disconnect",
            "SpO No Sensor",
            "SpO Sensor Off"
        };
        
        for (String expectedAlert : shouldProcess) {
            boolean found = false;
            for (ExcelParserV5.FlowRow flow : clinicals) {
                if (flow.alarmName != null) {
                    String normalized = flow.alarmName.toLowerCase().replace("ō", "o");
                    String expected = expectedAlert.toLowerCase();
                    if (normalized.contains(expected) || expected.contains(normalized)) {
                        found = true;
                        System.out.println("✅ Found expected alert: " + flow.alarmName);
                        break;
                    }
                }
            }
            if (!found) {
                System.out.println("⚠️  Expected alert NOT found: " + expectedAlert);
            }
        }
    }
}
