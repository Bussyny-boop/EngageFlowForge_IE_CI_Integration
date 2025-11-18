package com.example.exceljson;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that adapter rules (VMP, XMPP, etc.) are not processed when their corresponding
 * DataUpdate CREATE rules are inactive.
 */
public class InactiveDataUpdateRuleTest {

    @Test
    public void testAdapterRulesIgnoredWhenDataUpdateCreateRuleInactive() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load XMLParser.xml which has inactive DataUpdate CREATE rule for overtime alerts
        File xmlFile = new File("XMLParser.xml");
        if (!xmlFile.exists()) {
            // Try in root directory
            xmlFile = new File("/home/runner/work/FlowForge-with-Image-generator/FlowForge-with-Image-generator/XMLParser.xml");
        }
        assertTrue(xmlFile.exists(), "XMLParser.xml file should exist");
        
        parser.load(xmlFile);
        
        // Get nurse call flows
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // Check that NO overtime alert flows exist for Charge Nurse and Resource Nurse
        // The inactive DataUpdate CREATE rule means these alert types should not be processed
        // Alert types from the inactive rule: Toilet Assist OT, Needs Toilet OT, Patient OT, 
        // Routine Call OT, Needs Water OT, Toilet Request OT, Toilet OT, Pain OT
        
        long overtimeAlertCount = nurseCalls.stream()
            .filter(flow -> flow.alarmName != null && flow.alarmName.contains(" OT"))
            .filter(flow -> flow.r1 != null && 
                   (flow.r1.contains("Charge Nurse") || flow.r1.contains("Resource Nurse")))
            .count();
        
        // Since the DataUpdate CREATE rule for "OT" alerts is inactive,
        // there should be NO flows for these overtime alerts to Charge/Resource Nurse
        System.out.println("Found " + overtimeAlertCount + " overtime alert flows for Charge/Resource Nurse");
        
        // List all OT flows found for Charge/Resource Nurse
        nurseCalls.stream()
            .filter(flow -> flow.alarmName != null && flow.alarmName.contains(" OT"))
            .filter(flow -> flow.r1 != null && 
                   (flow.r1.contains("Charge Nurse") || flow.r1.contains("Resource Nurse")))
            .forEach(flow -> System.out.println("  - " + flow.alarmName + " -> " + flow.r1 + " (Config: " + flow.configGroup + ")"));
        
        // The test expectation: If the DataUpdate CREATE rule is inactive,
        // the corresponding SEND rules should also be ignored
        assertEquals(0, overtimeAlertCount, 
            "No overtime alert flows should exist for Charge/Resource Nurse when DataUpdate CREATE rule is inactive");
    }
    
    @Test
    public void testActiveDataUpdateCreateRuleAllowsAdapterRules() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load XMLParser.xml
        File xmlFile = new File("XMLParser.xml");
        if (!xmlFile.exists()) {
            xmlFile = new File("/home/runner/work/FlowForge-with-Image-generator/FlowForge-with-Image-generator/XMLParser.xml");
        }
        assertTrue(xmlFile.exists(), "XMLParser.xml file should exist");
        
        parser.load(xmlFile);
        
        // Get nurse call flows
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // Check that flows exist for alert types with ACTIVE DataUpdate CREATE rules
        // For example, "Code Blue" alerts have an active DataUpdate CREATE rule
        
        long codeBlueCount = nurseCalls.stream()
            .filter(flow -> flow.alarmName != null && 
                   (flow.alarmName.contains("Code Blue") || 
                    flow.alarmName.contains("CODE BLUE")))
            .count();
        
        System.out.println("Found " + codeBlueCount + " Code Blue alert flows");
        
        // We expect to find Code Blue flows since the DataUpdate CREATE rule is active
        assertTrue(codeBlueCount > 0, 
            "Should find Code Blue flows when DataUpdate CREATE rule is active");
    }
}
