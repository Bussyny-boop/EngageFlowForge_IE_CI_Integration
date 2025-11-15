package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DataUpdate interface component behavior.
 * 
 * According to requirements:
 * 1. Device-A is determined by the interface component name
 * 2. VMP interface should map to Device-A = "VMP"
 * 3. DataUpdate interface should map to Device-A = "Edge" (or the component name)
 * 4. Recipients and alert types are extracted from views
 * 5. When no unit.name in views, flows apply to ALL units
 */
public class DataUpdateInterfaceTest {

    @Test
    public void testVMPInterfaceDeviceMapping() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-interface.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        // Verify clinicals were created
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // All flows from VMP interface should have Device-A = "VMP"
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.r1 != null && !flow.r1.isEmpty()) {
                // Flows with recipients come from VMP interface (SEND rules)
                assertEquals("VMP", flow.deviceA, 
                    "Flows from VMP interface should have Device-A = VMP");
            }
        }
        
        // Verify we have flows with NURSE recipient (with VAssign prefix)
        boolean foundNurseRecipient = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("VAssign:NURSE".equals(flow.r1)) {
                foundNurseRecipient = true;
                assertEquals("VMP", flow.deviceA, "NURSE recipient flow should have Device-A = VMP");
            }
        }
        assertTrue(foundNurseRecipient, "Should find flows with VAssign:NURSE recipient");
        
        // Verify we have flows with NURSE BUDDY recipient (with VAssign prefix)
        boolean foundNurseBuddyRecipient = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("VAssign:NURSE BUDDY".equals(flow.r2)) {
                foundNurseBuddyRecipient = true;
                assertEquals("VMP", flow.deviceA, "NURSE BUDDY recipient flow should have Device-A = VMP");
            }
        }
        assertTrue(foundNurseBuddyRecipient, "Should find flows with VAssign:NURSE BUDDY as second recipient");
    }
    
    @Test
    public void testAlertTypesExtractedFromViews() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-interface.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Verify specific alert types were extracted
        boolean foundLHR = false;
        boolean foundHHR = false;
        boolean foundAPNEA = false;
        boolean foundAFIB = false;
        boolean foundETCO2HI = false;
        
        for (ExcelParserV5.FlowRow flow : clinicals) {
            String alarmName = flow.alarmName;
            if ("LHR".equals(alarmName)) foundLHR = true;
            if ("HHR".equals(alarmName)) foundHHR = true;
            if ("APNEA".equals(alarmName)) foundAPNEA = true;
            if ("AFIB".equals(alarmName)) foundAFIB = true;
            if ("ETCO2 HI".equals(alarmName)) foundETCO2HI = true;
        }
        
        assertTrue(foundLHR, "Should find LHR alert type");
        assertTrue(foundHHR, "Should find HHR alert type");
        assertTrue(foundAPNEA, "Should find APNEA alert type");
        assertTrue(foundAFIB, "Should find AFIB alert type");
        assertTrue(foundETCO2HI, "Should find ETCO2 HI alert type");
    }
    
    @Test
    public void testEscalationTimingFromDataUpdate() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-interface.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Verify escalation timing is set correctly
        // The DataUpdate interface defines escalation from Primary to Secondary at 60 seconds
        // This should set T2 (time to second recipient) to "60"
        boolean foundEscalationTiming = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.t2 != null && !flow.t2.isEmpty()) {
                foundEscalationTiming = true;
                assertEquals("60", flow.t2, "Time to second recipient should be 60 seconds (from DataUpdate escalation rule)");
                break;
            }
        }
        assertTrue(foundEscalationTiming, "Should find flows with escalation timing set");
    }
    
    @Test
    public void testNoUnitNameAppliesToAllUnits() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-interface.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Since there's no unit.name filter in the views, all flows should have empty config group
        // or config group that doesn't include a specific unit
        for (ExcelParserV5.FlowRow flow : clinicals) {
            // Config group should be just the dataset name when no unit is specified
            assertTrue(flow.configGroup.equals("Clinicals") || flow.configGroup.isEmpty(),
                "Config group should be just dataset name when no unit specified, got: " + flow.configGroup);
        }
    }
    
    @Test
    public void testVMPDeferDeliveryByIsIgnored() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-interface.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // VMP rules have defer-delivery-by="1" but this should be ignored for timing
        // Only DataUpdate interface defer-delivery-by="60" should be used
        // Verify that NO flow has T2="1" (which would indicate VMP timing was used)
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.t2 != null && !flow.t2.isEmpty()) {
                assertNotEquals("1", flow.t2, 
                    "T2 should not be 1 (VMP defer-delivery-by should be ignored). " +
                    "Only DataUpdate defer-delivery-by should set timing. Got T2=" + flow.t2);
                assertEquals("60", flow.t2, 
                    "T2 should be 60 from DataUpdate interface, not from VMP interface");
            }
        }
    }
    
    @Test
    public void testDeviceAComesFromSendRulesNotEscalationRules() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-dataupdate-interface.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // All flows should have Device-A = "VMP" (from VMP SEND rules)
        // Device-A should NEVER be "Edge" (which is what DataUpdate escalation rule would map to)
        for (ExcelParserV5.FlowRow flow : clinicals) {
            assertNotNull(flow.deviceA, "Device-A should be set");
            assertEquals("VMP", flow.deviceA, 
                "Device-A should be VMP from SEND rules, not Edge from DataUpdate escalation rules");
            assertNotEquals("Edge", flow.deviceA, 
                "Device-A should not be Edge (from DataUpdate). It should come from SEND rules (VMP).");
        }
    }
}
