package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DataUpdate interface with global escalation (no alert_type or unit.name).
 * 
 * Requirements:
 * 1. When DataUpdate interface views don't have alert_type or unit.name, 
 *    apply timing to ALL alerts and ALL units with multi-state escalation
 * 2. Treat state value of "Group" like "Primary" for determining recipients
 * 3. When interface rule cannot determine recipient, use destination in settings
 */
public class DataUpdateGlobalEscalationTest {

    @Test
    public void testDataUpdateAppliesToAllAlertsWhenNoAlertTypeSpecified() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-global-escalation.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // The DataUpdate rule has NO alert_type filter but the VMP rules have alert_type=LHR,HHR
        // The DataUpdate timing should apply to ALL alerts (LHR and HHR)
        boolean foundLHR = false;
        boolean foundHHR = false;
        
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("LHR".equals(flow.alarmName)) {
                foundLHR = true;
                // Should have escalation timing from DataUpdate
                assertEquals("60", flow.t2, 
                    "LHR should have T2=60 from DataUpdate global escalation");
            }
            if ("HHR".equals(flow.alarmName)) {
                foundHHR = true;
                // Should have escalation timing from DataUpdate
                assertEquals("60", flow.t2, 
                    "HHR should have T2=60 from DataUpdate global escalation");
            }
        }
        
        assertTrue(foundLHR, "Should find LHR alert");
        assertTrue(foundHHR, "Should find HHR alert");
    }
    
    @Test
    public void testGroupStateTreatedAsPrimary() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-global-escalation.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // The SEND GROUP rule has state="Group" which should be normalized to "Primary"
        // This means CHARGE NURSE should be R1 (primary recipient)
        boolean foundChargeNurse = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("VAssign:CHARGE NURSE".equals(flow.r1)) {
                foundChargeNurse = true;
                assertEquals("VMP", flow.deviceA, "Flow with CHARGE NURSE should use VMP");
            }
        }
        
        assertTrue(foundChargeNurse, "Should find flow with VAssign:CHARGE NURSE as R1 (Group state treated as Primary)");
    }
    
    @Test
    public void testDataUpdateOnlyAppliesIfMultipleStates() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-global-escalation.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // All flows should have multi-state escalation (Primary -> Secondary)
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.t2 != null && !flow.t2.isEmpty()) {
                assertEquals("60", flow.t2, 
                    "Flows with multi-state escalation should have T2=60 from DataUpdate");
            }
        }
        
        // Verify we have at least one flow with both R1 and R2 (multi-state)
        boolean foundMultiState = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.r1 != null && !flow.r1.isEmpty() && 
                flow.r2 != null && !flow.r2.isEmpty()) {
                foundMultiState = true;
                break;
            }
        }
        assertTrue(foundMultiState, "Should have at least one flow with multi-state escalation");
    }
    
    @Test
    public void testRecipientFallbackToDestination() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-global-escalation.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // All flows should have recipients extracted from role names or destinations
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.r1 != null && !flow.r1.isEmpty()) {
                // Recipients should be one of: VAssign:NURSE, VAssign:NURSE BUDDY, VAssign:CHARGE NURSE, or a destination value
                assertTrue(
                    flow.r1.equals("VAssign:NURSE") || 
                    flow.r1.equals("VAssign:NURSE BUDDY") || 
                    flow.r1.equals("VAssign:CHARGE NURSE") ||
                    flow.r1.startsWith("#{"),
                    "R1 should be a valid recipient: " + flow.r1
                );
            }
        }
    }
}
