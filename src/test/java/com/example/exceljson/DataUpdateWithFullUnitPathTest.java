package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that DataUpdate escalation works with full unit path "bed.room.unit.name"
 * and handles multi-word unit names like "3 Peds ICU".
 */
public class DataUpdateWithFullUnitPathTest {

    @Test
    public void testDataUpdateWithFullUnitPath() throws Exception {
        // Create test XML file with full unit path "bed.room.unit.name"
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<package version-major=\"1\" version-minor=\"0\">\n" +
            "  <meta-data><name>Test</name></meta-data>\n" +
            "  <contents>\n" +
            "    <datasets>\n" +
            "      <dataset active=\"true\">\n" +
            "        <name>Clinicals</name>\n" +
            "        <view><name>Alarm_types</name>\n" +
            "          <filter relation=\"in\"><path>alert_type</path><value>LHR,HHR</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Unit_is_PICU</name>\n" +
            "          <description>Unit is PICU</description>\n" +
            "          <filter relation=\"in\">\n" +
            "            <path>bed.room.unit.name</path>\n" +
            "            <value>3 Peds ICU</value>\n" +
            "          </filter>\n" +
            "        </view>\n" +
            "        <view><name>State_Primary</name>\n" +
            "          <filter relation=\"equal\"><path>state</path><value>Primary</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>State_Secondary</name>\n" +
            "          <filter relation=\"equal\"><path>state</path><value>Secondary</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Role_NURSE</name>\n" +
            "          <filter relation=\"equal\"><path>bed.locs.assignments.role.name</path><value>NURSE</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Role_NURSE_BUDDY</name>\n" +
            "          <filter relation=\"equal\"><path>bed.locs.assignments.role.name</path><value>NURSE BUDDY</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Not_responded</name>\n" +
            "          <filter relation=\"equal\"><path>responded</path><value>false</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Active</name>\n" +
            "          <filter relation=\"equal\"><path>active</path><value>true</value></filter>\n" +
            "        </view>\n" +
            "      </dataset>\n" +
            "    </datasets>\n" +
            "    <interfaces>\n" +
            "      <interface component=\"VMP\">\n" +
            "        <name>VMP</name>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>SEND PRIMARY PICU</purpose>\n" +
            "          <trigger-on create=\"true\"/>\n" +
            "          <condition>\n" +
            "            <view>Alarm_types</view>\n" +
            "            <view>State_Primary</view>\n" +
            "            <view>Role_NURSE</view>\n" +
            "            <view>Unit_is_PICU</view>\n" +
            "          </condition>\n" +
            "          <settings>{\"destination\":\"test\",\"priority\":\"2\"}</settings>\n" +
            "        </rule>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>SEND SECONDARY PICU</purpose>\n" +
            "          <trigger-on update=\"true\"/>\n" +
            "          <condition>\n" +
            "            <view>Alarm_types</view>\n" +
            "            <view>State_Secondary</view>\n" +
            "            <view>Role_NURSE_BUDDY</view>\n" +
            "            <view>Unit_is_PICU</view>\n" +
            "          </condition>\n" +
            "          <settings>{\"destination\":\"test\",\"priority\":\"2\"}</settings>\n" +
            "        </rule>\n" +
            "      </interface>\n" +
            "      <interface component=\"DataUpdate\">\n" +
            "        <name>DataUpdate for Alerts</name>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>ESCALATE TO SECONDARY | 60 SEC DELAY | ALL ALARMS | NO PRIMARY RESPONSE  | ALL UNITS | ALL FACILITIES</purpose>\n" +
            "          <trigger-on update=\"true\"/>\n" +
            "          <defer-delivery-by>60</defer-delivery-by>\n" +
            "          <condition>\n" +
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
        
        File xmlFile = new File("/tmp/test-dataupdate-full-path.xml");
        java.nio.file.Files.write(xmlFile.toPath(), xmlContent.getBytes());
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Should create flows for the alert types
        assertFalse(clinicals.isEmpty(), "Should create clinical flows");
        
        // Verify timing from DataUpdate is applied
        boolean foundTimingApplied = false;
        boolean foundPICUUnit = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("60".equals(flow.t2)) {
                foundTimingApplied = true;
                System.out.println("✓ Found flow with T2=60: " + flow.alarmName + 
                    ", configGroup=" + flow.configGroup + 
                    ", r1=" + flow.r1 + ", r2=" + flow.r2);
            }
            if (flow.configGroup != null && flow.configGroup.contains("3 Peds ICU")) {
                foundPICUUnit = true;
                System.out.println("✓ Found flow with PICU unit: " + flow.configGroup);
            }
        }
        
        assertTrue(foundTimingApplied, 
            "DataUpdate timing (60 sec) should be applied to flows with full unit path 'bed.room.unit.name'");
        
        assertTrue(foundPICUUnit,
            "Should find flows with '3 Peds ICU' unit from bed.room.unit.name path");
        
        // Cleanup
        xmlFile.delete();
    }
}
