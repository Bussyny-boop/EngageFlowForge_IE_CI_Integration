package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for adapter rules (VMP, XMPP, CUCM, Vocera) that use create="true" trigger.
 * 
 * Requirements:
 * - Adapter rules with trigger-on create="true" should function WITHOUT requiring a DataUpdate CREATE rule
 * - This is because the adapter itself is performing the "Create" operation
 * - Example: SEND EPIC ORDER | ALL ORDERS CODES | VOCERA GROUP | VMP | GICH
 */
public class AdapterCreateRuleTest {
    
    @Test
    public void testAdapterRuleWithCreateTrigger_WithoutDataUpdateCreateRule() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load XML file with VMP adapter rule that has create="true" but NO DataUpdate CREATE rule
        File xmlFile = new File("src/test/resources/test-adapter-create-rule.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        // Verify orders were created
        List<ExcelParserV5.FlowRow> orders = parser.getOrders();
        assertFalse(orders.isEmpty(), "Should have order flows even without DataUpdate CREATE rule");
        
        // Verify the specific order flow was created
        boolean foundEpicOrder = false;
        for (ExcelParserV5.FlowRow flow : orders) {
            if (flow.alarmName.contains("Epic Order")) {
                foundEpicOrder = true;
                assertEquals("VMP", flow.deviceA, "Device should be VMP");
                assertEquals("Immediate", flow.t1, "T1 should be Immediate for create trigger");
                assertNotNull(flow.configGroup, "Config group should be set");
                assertTrue(flow.configGroup.contains("GICH") || flow.configGroup.contains("All_Facilities"), 
                    "Config group should contain facility");
            }
        }
        assertTrue(foundEpicOrder, "Should find Epic Order flow from VMP adapter with create trigger");
    }
    
    @Test
    public void testXmppAdapterRuleWithCreateTrigger() throws Exception {
        // Create an XML with XMPP adapter using create="true"
        File xmlFile = new File("src/test/resources/test-xmpp-adapter-create-rule.xml");
        
        // This test demonstrates that XMPP adapters with create="true" should also work
        // without DataUpdate CREATE rules
        if (!xmlFile.exists()) {
            // Test will be skipped if the file doesn't exist
            return;
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows from XMPP adapter with create trigger");
    }
    
    @Test
    public void testVoceraAdapterRuleWithCreateTrigger() throws Exception {
        // Create an XML with Vocera adapter using create="true"
        File xmlFile = new File("src/test/resources/test-vocera-adapter-create-rule.xml");
        
        // This test demonstrates that Vocera adapters with create="true" should also work
        // without DataUpdate CREATE rules
        if (!xmlFile.exists()) {
            // Test will be skipped if the file doesn't exist
            return;
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        assertFalse(nurseCalls.isEmpty(), "Should have nurse call flows from Vocera adapter with create trigger");
    }
    
    @Test
    public void testAdapterRuleWithUpdateTrigger_StillRequiresDataUpdateCreateRule() throws Exception {
        // This test verifies that adapter rules with update="true" (not create="true")
        // still require a DataUpdate CREATE rule
        File xmlFile = new File("src/test/resources/test-adapter-update-rule-only.xml");
        
        if (!xmlFile.exists()) {
            // Test will be skipped if the file doesn't exist
            return;
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        // Should have no flows because there's no DataUpdate CREATE rule
        List<ExcelParserV5.FlowRow> orders = parser.getOrders();
        assertTrue(orders.isEmpty(), "Should have no flows when adapter has update trigger without DataUpdate CREATE rule");
    }
}
