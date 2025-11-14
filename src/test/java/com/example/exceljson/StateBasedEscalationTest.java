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
        // Create test XML file from resources
        File xmlFile = new File("src/test/resources/test-state-escalation.xml");
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // We should have 2 flows (APNEA and ASYSTOLE), merged with escalation
        assertEquals(2, clinicals.size(), "Should have 2 merged clinical flows");
        
        // Check first flow (APNEA)
        ExcelParserV5.FlowRow apneaFlow = clinicals.stream()
            .filter(f -> "APNEA".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(apneaFlow, "APNEA flow should exist");
        
        // Verify the complete escalation chain
        assertEquals("Immediate", apneaFlow.t1, "T1 should be Immediate for Primary state");
        assertEquals("Primary Caregiver", apneaFlow.r1, "R1 should be Primary Caregiver");
        
        assertEquals("30", apneaFlow.t2, "T2 should be 30 (time to 2nd recipient, from Primary state escalation)");
        assertEquals("Secondary Caregiver", apneaFlow.r2, "R2 should be Secondary Caregiver");
        
        assertEquals("60", apneaFlow.t3, "T3 should be 60 (time to 3rd recipient, from Secondary state escalation)");
        assertEquals("Tertiary Caregiver", apneaFlow.r3, "R3 should be Tertiary Caregiver");
        
        assertEquals("90", apneaFlow.t4, "T4 should be 90 (time to 4th recipient, from Tertiary state escalation)");
        assertEquals("Quaternary Caregiver", apneaFlow.r4, "R4 should be Quaternary Caregiver");
        
        // Check second flow (ASYSTOLE) has same structure
        ExcelParserV5.FlowRow asystoleFlow = clinicals.stream()
            .filter(f -> "ASYSTOLE".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(asystoleFlow, "ASYSTOLE flow should exist");
        assertEquals("Immediate", asystoleFlow.t1);
        assertEquals("Primary Caregiver", asystoleFlow.r1);
        assertEquals("30", asystoleFlow.t2);
        assertEquals("Secondary Caregiver", asystoleFlow.r2);
        assertEquals("60", asystoleFlow.t3);
        assertEquals("Tertiary Caregiver", asystoleFlow.r3);
        assertEquals("90", asystoleFlow.t4);
        assertEquals("Quaternary Caregiver", asystoleFlow.r4);
    }
    
    @Test
    public void testInactiveRulesAreSkipped() throws Exception {
        File xmlFile = new File("src/test/resources/test-state-escalation.xml");
        
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
