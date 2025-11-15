package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that state="Group" maps to the 1st recipient (R1).
 */
public class GroupStateTest {

    @Test
    public void testGroupStateMapsToFirstRecipient() throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<package version-major=\"1\" version-minor=\"0\">\n" +
            "  <meta-data><name>Test Group State</name></meta-data>\n" +
            "  <contents>\n" +
            "    <datasets>\n" +
            "      <dataset active=\"true\">\n" +
            "        <name>Clinicals</name>\n" +
            "        <view><name>Alarm_types</name>\n" +
            "          <filter relation=\"in\"><path>alert_type</path><value>Code Blue</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>State_Group</name>\n" +
            "          <filter relation=\"equal\"><path>state</path><value>Group</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Role_PHYSICIAN</name>\n" +
            "          <filter relation=\"equal\"><path>bed.locs.assignments.role.name</path><value>Physician</value></filter>\n" +
            "        </view>\n" +
            "      </dataset>\n" +
            "    </datasets>\n" +
            "    <interfaces>\n" +
            "      <interface component=\"VMP\">\n" +
            "        <name>VMP</name>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>SEND TO GROUP</purpose>\n" +
            "          <trigger-on create=\"true\"/>\n" +
            "          <condition>\n" +
            "            <view>Alarm_types</view>\n" +
            "            <view>State_Group</view>\n" +
            "            <view>Role_PHYSICIAN</view>\n" +
            "          </condition>\n" +
            "          <settings>{\"destination\":\"g-CodeBlueTeam\",\"priority\":\"3\",\"ttl\":15}</settings>\n" +
            "        </rule>\n" +
            "      </interface>\n" +
            "    </interfaces>\n" +
            "  </contents>\n" +
            "</package>";
        
        File xmlFile = new File("/tmp/test-group-state.xml");
        java.nio.file.Files.write(xmlFile.toPath(), xmlContent.getBytes());
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should create flows");
        
        for (ExcelParserV5.FlowRow flow : clinicals) {
            System.out.println("Flow: " + flow.alarmName);
            System.out.println("  Device-A: " + flow.deviceA);
            System.out.println("  R1: " + flow.r1);
            System.out.println("  T1: " + flow.t1);
            System.out.println("  Priority: " + flow.priorityRaw);
            
            assertEquals("Code Blue", flow.alarmName, "Should have Code Blue alarm");
            assertEquals("g-CodeBlueTeam", flow.r1, 
                "Group state should set R1 to g-CodeBlueTeam");
            assertEquals("Immediate", flow.t1, "T1 should be Immediate for create rule");
            assertEquals("VMP", flow.deviceA, "Device-A should be VMP");
            assertEquals("3", flow.priorityRaw, "Priority should be 3");
        }
        
        xmlFile.delete();
    }
    
    @Test
    public void testGroupAndPrimaryStatesBothMapToR1() throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<package version-major=\"1\" version-minor=\"0\">\n" +
            "  <meta-data><name>Test</name></meta-data>\n" +
            "  <contents>\n" +
            "    <datasets>\n" +
            "      <dataset active=\"true\">\n" +
            "        <name>Clinicals</name>\n" +
            "        <view><name>Alarm_Code_Blue</name>\n" +
            "          <filter relation=\"equal\"><path>alert_type</path><value>Code Blue</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Alarm_Code_Red</name>\n" +
            "          <filter relation=\"equal\"><path>alert_type</path><value>Code Red</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>State_Group</name>\n" +
            "          <filter relation=\"equal\"><path>state</path><value>Group</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>State_Primary</name>\n" +
            "          <filter relation=\"equal\"><path>state</path><value>Primary</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Role_NURSE</name>\n" +
            "          <filter relation=\"equal\"><path>bed.locs.assignments.role.name</path><value>NURSE</value></filter>\n" +
            "        </view>\n" +
            "      </dataset>\n" +
            "    </datasets>\n" +
            "    <interfaces>\n" +
            "      <interface component=\"VMP\">\n" +
            "        <name>VMP</name>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>Code Blue - Group</purpose>\n" +
            "          <trigger-on create=\"true\"/>\n" +
            "          <condition>\n" +
            "            <view>Alarm_Code_Blue</view>\n" +
            "            <view>State_Group</view>\n" +
            "          </condition>\n" +
            "          <settings>{\"destination\":\"g-CodeBlue\",\"priority\":\"3\"}</settings>\n" +
            "        </rule>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>Code Red - Primary</purpose>\n" +
            "          <trigger-on create=\"true\"/>\n" +
            "          <condition>\n" +
            "            <view>Alarm_Code_Red</view>\n" +
            "            <view>State_Primary</view>\n" +
            "            <view>Role_NURSE</view>\n" +
            "          </condition>\n" +
            "          <settings>{\"destination\":\"test\",\"priority\":\"2\"}</settings>\n" +
            "        </rule>\n" +
            "      </interface>\n" +
            "    </interfaces>\n" +
            "  </contents>\n" +
            "</package>";
        
        File xmlFile = new File("/tmp/test-group-and-primary.xml");
        java.nio.file.Files.write(xmlFile.toPath(), xmlContent.getBytes());
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertEquals(2, clinicals.size(), "Should have 2 flows");
        
        for (ExcelParserV5.FlowRow flow : clinicals) {
            System.out.println("Flow: " + flow.alarmName + ", R1=" + flow.r1);
            
            // Both Group and Primary states should set R1
            assertNotNull(flow.r1, "R1 should be set for " + flow.alarmName);
            
            if ("Code Blue".equals(flow.alarmName)) {
                assertEquals("g-CodeBlue", flow.r1, "Code Blue (Group state) should have g-CodeBlue in R1");
            } else if ("Code Red".equals(flow.alarmName)) {
                assertEquals("NURSE", flow.r1, "Code Red (Primary state) should have NURSE in R1");
            }
        }
        
        xmlFile.delete();
    }
}
