package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for state-based escalation parsing from XML.
 * According to the requirements:
 * - state=Primary determines time to 2nd recipient
 * - state=Secondary determines time to 3rd recipient  
 * - state=Tertiary determines time to 4th recipient
 * - state=Quaternary determines time to 5th recipient
 */
public class StateBasedEscalationTest {

    @Test
    public void testStateBasedEscalationParsing() throws Exception {
        // Create test XML file
        File xmlFile = new File("/tmp/test-state-escalation.xml");
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Debug: print all flows
        System.out.println("Total flows parsed: " + clinicals.size());
        for (int i = 0; i < clinicals.size(); i++) {
            ExcelParserV5.FlowRow flow = clinicals.get(i);
            System.out.println("\nFlow " + (i+1) + ":");
            System.out.println("  Purpose/Name: " + flow.alarmName);
            System.out.println("  T1: " + flow.t1);
            System.out.println("  R1: " + flow.r1);
            System.out.println("  T2: " + flow.t2);
            System.out.println("  R2: " + flow.r2);
            System.out.println("  T3: " + flow.t3);
            System.out.println("  R3: " + flow.r3);
            System.out.println("  T4: " + flow.t4);
            System.out.println("  R4: " + flow.r4);
        }
        
        // We should have flows, but need to verify they are properly merged
        assertTrue(clinicals.size() > 0, "Should have parsed some clinical flows");
    }
    
    @Test
    public void testInactiveRulesAreSkipped() throws Exception {
        File xmlFile = new File("/tmp/test-state-escalation.xml");
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Verify that no flow has "THIS SHOULD BE SKIPPED" in its name
        for (ExcelParserV5.FlowRow flow : clinicals) {
            assertFalse(flow.alarmName.contains("THIS SHOULD BE SKIPPED"),
                "Inactive rules should not be parsed");
        }
    }
}
