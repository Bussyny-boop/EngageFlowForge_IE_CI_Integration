package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for state-based escalation parsing from XML.
 * According to the requirements:
 * - state=Primary determines time to 2nd recipient (T2)
 * - state=Secondary determines time to 3rd recipient (T3)
 * - state=Tertiary determines time to 4th recipient (T4)
 * - state=Quaternary determines time to 5th recipient (T5)
 * 
 * The parser cross-references views in rules with views in datasets:
 * - Looks for views with path="state" to determine escalation state
 * - Looks for views with path ending in "role.name" to determine recipients
 * - SEND rules (with role/destination) provide recipient information
 * - ESCALATE rules (with defer-delivery-by) provide timing information
 * - Rules are merged when they share the same dataset, units, and alert types
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
        // T1/R1: Primary state (immediate send on create)
        assertEquals("Immediate", apneaFlow.t1, "T1 should be Immediate for Primary state");
        assertEquals("VAssign:[Room] Primary Caregiver", apneaFlow.r1, "R1 should be VAssign:[Room] Primary Caregiver");
        
        // T2/R2: Determined by Primary state escalation rule (defer-delivery-by=30)
        assertEquals("30", apneaFlow.t2, "T2 should be 30 (time to 2nd recipient, from Primary state escalation)");
        assertEquals("VAssign:[Room] Secondary Caregiver", apneaFlow.r2, "R2 should be VAssign:[Room] Secondary Caregiver");
        
        // T3/R3: Determined by Secondary state escalation rule (defer-delivery-by=60)
        assertEquals("60", apneaFlow.t3, "T3 should be 60 (time to 3rd recipient, from Secondary state escalation)");
        assertEquals("VAssign:[Room] Tertiary Caregiver", apneaFlow.r3, "R3 should be VAssign:[Room] Tertiary Caregiver");
        
        // T4/R4: Determined by Tertiary state escalation rule (defer-delivery-by=90)
        assertEquals("90", apneaFlow.t4, "T4 should be 90 (time to 4th recipient, from Tertiary state escalation)");
        assertEquals("VAssign:[Room] Quaternary Caregiver", apneaFlow.r4, "R4 should be VAssign:[Room] Quaternary Caregiver");
        
        // Check second flow (ASYSTOLE) has same structure
        ExcelParserV5.FlowRow asystoleFlow = clinicals.stream()
            .filter(f -> "ASYSTOLE".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(asystoleFlow, "ASYSTOLE flow should exist");
        assertEquals("Immediate", asystoleFlow.t1);
        assertEquals("VAssign:[Room] Primary Caregiver", asystoleFlow.r1);
        assertEquals("30", asystoleFlow.t2);
        assertEquals("VAssign:[Room] Secondary Caregiver", asystoleFlow.r2);
        assertEquals("60", asystoleFlow.t3);
        assertEquals("VAssign:[Room] Tertiary Caregiver", asystoleFlow.r3);
        assertEquals("90", asystoleFlow.t4);
        assertEquals("VAssign:[Room] Quaternary Caregiver", asystoleFlow.r4);
    }
    
    @Test
    public void testInactiveRulesAreSkipped() throws Exception {
        File xmlFile = new File("src/test/resources/test-state-escalation.xml");
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Verify that no flow has "THIS SHOULD BE SKIPPED" in its name
        // The test XML has an inactive rule that should not be parsed
        for (ExcelParserV5.FlowRow flow : clinicals) {
            assertFalse(flow.alarmName.contains("THIS SHOULD BE SKIPPED"),
                "Inactive rules (active=\"false\") should not be parsed");
        }
    }
    
    @Test
    public void testViewCrossReferencing() throws Exception {
        // This test verifies that views are cross-referenced correctly
        File xmlFile = new File("src/test/resources/test-state-escalation.xml");
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Verify that all flows have the correct type
        for (ExcelParserV5.FlowRow flow : clinicals) {
            assertEquals("Clinicals", flow.type, "All flows should be Clinicals type");
        }
        
        // Verify that settings from SEND rules are applied
        ExcelParserV5.FlowRow flow = clinicals.get(0);
        assertEquals("VMP", flow.deviceA, "Device should be set from component attribute");
        assertNotNull(flow.ttlValue, "TTL should be set from settings");
        assertNotNull(flow.priorityRaw, "Priority should be set from settings");
    }
}
