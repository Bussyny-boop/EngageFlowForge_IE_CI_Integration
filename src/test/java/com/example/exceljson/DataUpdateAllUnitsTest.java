package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that DataUpdate escalation rules without unit.name filters
 * apply to ALL units (not just empty unit).
 * 
 * Requirement: "Any where the Dataupdate interface view when cross reference 
 * with the Dataset view does not return and alert type value or unit.name value 
 * then apply the time to recipients to all alerts and all units."
 */
public class DataUpdateAllUnitsTest {

    @Test
    public void testDataUpdateWithoutUnitFiltersAppliesToAllUnits() throws Exception {
        // Create test XML file
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
            "        <view><name>Unit_ICU</name>\n" +
            "          <filter relation=\"equal\"><path>unit.name</path><value>ICU</value></filter>\n" +
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
            "          <purpose>SEND PRIMARY ICU</purpose>\n" +
            "          <trigger-on create=\"true\"/>\n" +
            "          <condition>\n" +
            "            <view>Alarm_types</view>\n" +
            "            <view>State_Primary</view>\n" +
            "            <view>Role_NURSE</view>\n" +
            "            <view>Unit_ICU</view>\n" +
            "          </condition>\n" +
            "          <settings>{\"destination\":\"test\",\"priority\":\"2\"}</settings>\n" +
            "        </rule>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>SEND SECONDARY ICU</purpose>\n" +
            "          <trigger-on update=\"true\"/>\n" +
            "          <condition>\n" +
            "            <view>Alarm_types</view>\n" +
            "            <view>State_Secondary</view>\n" +
            "            <view>Role_NURSE_BUDDY</view>\n" +
            "            <view>Unit_ICU</view>\n" +
            "          </condition>\n" +
            "          <settings>{\"destination\":\"test\",\"priority\":\"2\"}</settings>\n" +
            "        </rule>\n" +
            "      </interface>\n" +
            "      <interface component=\"DataUpdate\">\n" +
            "        <name>DataUpdate</name>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>ESCALATE ALL UNITS</purpose>\n" +
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
        
        File xmlFile = new File("/tmp/test-dataupdate-all-units.xml");
        java.nio.file.Files.write(xmlFile.toPath(), xmlContent.getBytes());
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // Should create flows for the alert types
        assertFalse(clinicals.isEmpty(), "Should create clinical flows");
        
        // Verify timing from DataUpdate is applied
        boolean foundTimingApplied = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("60".equals(flow.t2)) {
                foundTimingApplied = true;
                System.out.println("Found flow with T2=60: " + flow.alarmName + 
                    ", configGroup=" + flow.configGroup + 
                    ", r1=" + flow.r1 + ", r2=" + flow.r2);
            }
        }
        
        assertTrue(foundTimingApplied, 
            "DataUpdate timing (60 sec) should be applied to flows even though " +
            "DataUpdate has no unit.name filter and VMP rules have Unit_ICU filter");
        
        // Cleanup
        xmlFile.delete();
    }
}
