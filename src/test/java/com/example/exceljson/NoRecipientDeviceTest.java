package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that when recipients are not found but we have rule settings,
 * device-A should be set from the interface component.
 */
public class NoRecipientDeviceTest {

    @Test
    public void testNoRecipientUsesComponentForDevice() throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<package version-major=\"1\" version-minor=\"0\">\n" +
            "  <meta-data><name>Test</name></meta-data>\n" +
            "  <contents>\n" +
            "    <datasets>\n" +
            "      <dataset active=\"true\">\n" +
            "        <name>Clinicals</name>\n" +
            "        <view><name>Alarm_types</name>\n" +
            "          <filter relation=\"in\"><path>alert_type</path><value>LHR</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Active</name>\n" +
            "          <filter relation=\"equal\"><path>active</path><value>true</value></filter>\n" +
            "        </view>\n" +
            "      </dataset>\n" +
            "    </datasets>\n" +
            "    <interfaces>\n" +
            "      <interface component=\"DataUpdate\">\n" +
            "        <name>DataUpdate Test</name>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>Test with settings but no recipient</purpose>\n" +
            "          <trigger-on create=\"true\"/>\n" +
            "          <condition>\n" +
            "            <view>Alarm_types</view>\n" +
            "            <view>Active</view>\n" +
            "          </condition>\n" +
            "          <settings>{\"priority\":\"2\",\"ttl\":10}</settings>\n" +
            "        </rule>\n" +
            "      </interface>\n" +
            "    </interfaces>\n" +
            "  </contents>\n" +
            "</package>";
        
        File xmlFile = new File("/tmp/test-no-recipient.xml");
        java.nio.file.Files.write(xmlFile.toPath(), xmlContent.getBytes());
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should create flows");
        
        for (ExcelParserV5.FlowRow flow : clinicals) {
            System.out.println("Flow: " + flow.alarmName);
            System.out.println("  Device-A: " + flow.deviceA);
            System.out.println("  R1: " + flow.r1);
            System.out.println("  Priority: " + flow.priorityRaw);
            
            // When no recipient is found but we have settings,
            // device-A should be set from the interface component
            assertEquals("Edge", flow.deviceA, 
                "Device-A should be 'Edge' (from DataUpdate component mapping)");
            
            // Verify settings are applied
            assertEquals("2", flow.priorityRaw, "Priority should be applied from settings");
        }
        
        xmlFile.delete();
    }
}
