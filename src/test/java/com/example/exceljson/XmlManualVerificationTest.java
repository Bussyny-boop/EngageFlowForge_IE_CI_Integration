package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Manual verification test to check XML parsing with the actual sample file.
 * This test is disabled by default as it requires the sample XML file to exist.
 */
class XmlManualVerificationTest {

    @Test
    @Disabled("Enable manually to test with actual XML file")
    void testParseActualXmlFile() throws Exception {
        File xmlFile = new File("/home/runner/work/FlowForge-with-Image-generator/FlowForge-with-Image-generator/XMLParser.xml");
        
        if (!xmlFile.exists()) {
            System.out.println("Sample XML file not found at: " + xmlFile.getAbsolutePath());
            return;
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        System.out.println("\n=== XML Parser Results ===");
        System.out.println(parser.getLoadSummary());
        
        // Check nurse calls for response options and enunciate
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        System.out.println("\n=== Sample Nurse Call Flows ===");
        int count = 0;
        for (ExcelParserV5.FlowRow flow : nurseCalls) {
            if (count++ >= 5) break; // Show first 5
            System.out.println("Alarm: " + flow.alarmName);
            System.out.println("  Response Options: " + flow.responseOptions);
            System.out.println("  Enunciate: " + flow.enunciate);
            System.out.println("  EMDAN: " + flow.emdan);
            System.out.println();
        }
        
        // Check clinicals for response options and enunciate
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        System.out.println("=== Sample Clinical Flows ===");
        count = 0;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (count++ >= 5) break; // Show first 5
            System.out.println("Alarm: " + flow.alarmName);
            System.out.println("  Response Options: " + flow.responseOptions);
            System.out.println("  Enunciate: " + flow.enunciate);
            System.out.println("  EMDAN: " + flow.emdan);
            System.out.println();
        }
        
        // Check orders for response options and enunciate
        List<ExcelParserV5.FlowRow> orders = parser.getOrders();
        System.out.println("=== Sample Order Flows ===");
        count = 0;
        for (ExcelParserV5.FlowRow flow : orders) {
            if (count++ >= 5) break; // Show first 5
            System.out.println("Alarm: " + flow.alarmName);
            System.out.println("  Response Options: " + flow.responseOptions);
            System.out.println("  Enunciate: " + flow.enunciate);
            System.out.println("  EMDAN: " + flow.emdan);
            System.out.println();
        }
        
        // Verify EMDAN fields are set correctly
        boolean allNurseCallsHaveEmdanNo = nurseCalls.stream()
            .allMatch(f -> "No".equals(f.emdan));
        assertTrue(allNurseCallsHaveEmdanNo, "All NurseCalls should have EMDAN='No'");
        
        boolean allClinicalsHaveEmdanYes = clinicals.stream()
            .allMatch(f -> "Yes".equals(f.emdan));
        assertTrue(allClinicalsHaveEmdanYes, "All Clinicals should have EMDAN='Yes'");
        
        boolean allOrdersHaveEmdanNo = orders.stream()
            .allMatch(f -> "No".equals(f.emdan));
        assertTrue(allOrdersHaveEmdanNo, "All Orders should have EMDAN='No'");
        
        System.out.println("=== Verification Complete ===");
        System.out.println("✓ All NurseCalls have EMDAN='No'");
        System.out.println("✓ All Clinicals have EMDAN='Yes'");
        System.out.println("✓ All Orders have EMDAN='No'");
    }
}
