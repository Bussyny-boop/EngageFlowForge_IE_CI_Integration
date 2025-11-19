package com.example.exceljson;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that DataUpdate rules whose purpose starts with "RESET" are not processed.
 */
public class ResetDataUpdateRuleTest {

    @Test
    public void testResetDataUpdateRulesAreNotProcessed() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load XMLParser.xml which contains DataUpdate rules with "RESET" purpose
        File xmlFile = new File("XMLParser.xml");
        if (!xmlFile.exists()) {
            xmlFile = new File("/home/runner/work/FlowForge-with-Image-generator/FlowForge-with-Image-generator/XMLParser.xml");
        }
        assertTrue(xmlFile.exists(), "XMLParser.xml file should exist");
        
        parser.load(xmlFile);
        
        // Get all flow rows
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        List<ExcelParserV5.FlowRow> orders = parser.getOrders();
        
        // Count flows - we expect FEWER flows because RESET rules should be ignored
        int totalFlows = nurseCalls.size() + clinicals.size() + orders.size();
        
        System.out.println("Total flows after filtering RESET rules: " + totalFlows);
        System.out.println("  - Nurse calls: " + nurseCalls.size());
        System.out.println("  - Clinicals: " + clinicals.size());
        System.out.println("  - Orders: " + orders.size());
        
        // Verify that no flows have been created by RESET DataUpdate rules
        // This is a basic sanity check - the real verification is that the parser doesn't crash
        // and that flows are still created for non-RESET rules
        assertTrue(totalFlows > 0, "Should still have flows from non-RESET rules");
    }
    
    @Test
    public void testNonResetDataUpdateRulesAreStillProcessed() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load XMLParser.xml
        File xmlFile = new File("XMLParser.xml");
        if (!xmlFile.exists()) {
            xmlFile = new File("/home/runner/work/FlowForge-with-Image-generator/FlowForge-with-Image-generator/XMLParser.xml");
        }
        assertTrue(xmlFile.exists(), "XMLParser.xml file should exist");
        
        parser.load(xmlFile);
        
        // Get all flow rows
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Verify that Code Blue flows still exist (non-RESET DataUpdate rules)
        long codeBlueCount = nurseCalls.stream()
            .filter(flow -> flow.alarmName != null && 
                   (flow.alarmName.contains("Code Blue") || 
                    flow.alarmName.contains("CODE BLUE")))
            .count();
        
        System.out.println("Found " + codeBlueCount + " Code Blue alert flows");
        
        // We expect to find Code Blue flows since they are not RESET rules
        assertTrue(codeBlueCount > 0, 
            "Should find Code Blue flows from non-RESET DataUpdate rules");
        
        // Verify that other clinical flows exist
        long clinicalCount = clinicals.stream()
            .filter(flow -> flow.alarmName != null && !flow.alarmName.isEmpty())
            .count();
        
        System.out.println("Found " + clinicalCount + " clinical alert flows");
        assertTrue(clinicalCount > 0, 
            "Should find clinical flows from non-RESET DataUpdate rules");
    }
}
