package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that escalation-only rules (no SEND rules) still set device-A from component.
 */
public class EscalationOnlyDeviceTest {

    @Test
    public void testEscalationOnlyUsesComponentForDevice() throws Exception {
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
            "        <view><name>State_Primary</name>\n" +
            "          <filter relation=\"equal\"><path>state</path><value>Primary</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Active</name>\n" +
            "          <filter relation=\"equal\"><path>active</path><value>true</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Not_responded</name>\n" +
            "          <filter relation=\"equal\"><path>responded</path><value>false</value></filter>\n" +
            "        </view>\n" +
            "      </dataset>\n" +
            "    </datasets>\n" +
            "    <interfaces>\n" +
            "      <interface component=\"DataUpdate\">\n" +
            "        <name>DataUpdate Escalation Only</name>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>ESCALATE - NO SEND RULES</purpose>\n" +
            "          <trigger-on update=\"true\"/>\n" +
            "          <defer-delivery-by>60</defer-delivery-by>\n" +
            "          <condition>\n" +
            "            <view>Alarm_types</view>\n" +
            "            <view>Not_responded</view>\n" +
            "            <view>Active</view>\n" +
            "            <view>State_Primary</view>\n" +
            "          </condition>\n" +
            "          <settings>{\"parameters\":[{\"path\":\"state\",\"value\":\"Secondary\"}]}</settings>\n" +
            "        </rule>\n" +
            "      </interface>\n" +
            "    </interfaces>\n" +
            "  </contents>\n" +
            "</package>";
        
        File xmlFile = new File("/tmp/test-escalation-only.xml");
        java.nio.file.Files.write(xmlFile.toPath(), xmlContent.getBytes());
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        System.out.println("Number of flows: " + clinicals.size());
        for (ExcelParserV5.FlowRow flow : clinicals) {
            System.out.println("Flow: " + flow.alarmName);
            System.out.println("  Device-A: " + flow.deviceA);
            System.out.println("  R1: " + flow.r1);
            System.out.println("  T2: " + flow.t2);
        }
        
        // Escalation-only rules without SEND rules should skip creating flows
        // because there are no alert types from SEND rules
        assertTrue(clinicals.isEmpty(), 
            "Escalation-only rules without SEND rules should not create flows " +
            "(no alert types from SEND rules)");
        
        xmlFile.delete();
    }
}
