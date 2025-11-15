package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that destination starting with "g-" is used as the recipient,
 * and Device-A is set from the interface component.
 */
public class GroupDestinationTest {

    @Test
    public void testGroupDestinationWithGroupState(@TempDir Path tempDir) throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<package version-major=\"1\" version-minor=\"0\">\n" +
            "  <meta-data><name>Test Group Destination</name></meta-data>\n" +
            "  <contents>\n" +
            "    <datasets>\n" +
            "      <dataset active=\"true\">\n" +
            "        <name>NurseCalls</name>\n" +
            "        <view><name>Alert_has_not_been_responded_to</name>\n" +
            "          <filter relation=\"equal\"><path>responded</path><value>false</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Alert_is_active</name>\n" +
            "          <filter relation=\"equal\"><path>active</path><value>true</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Alert_is_at_Group_state</name>\n" +
            "          <filter relation=\"equal\"><path>state</path><value>Group</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Alert_type_included_in_Code_Blue</name>\n" +
            "          <filter relation=\"in\"><path>alert_type</path><value>Code Blue</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Facility_name_GICH</name>\n" +
            "          <filter relation=\"equal\"><path>facility.name</path><value>GICH</value></filter>\n" +
            "        </view>\n" +
            "      </dataset>\n" +
            "    </datasets>\n" +
            "    <interfaces>\n" +
            "      <interface component=\"VMP\">\n" +
            "        <name>VMP</name>\n" +
            "        <rule active=\"true\" dataset=\"NurseCalls\" no_loopback=\"true\">\n" +
            "          <purpose>SEND GROUP | URGENT NO CALLBACK | ADULT CODE BLUE | GROUP | VMP | GICH</purpose>\n" +
            "          <trigger-on update=\"true\"/>\n" +
            "          <condition>\n" +
            "            <view>Alert_has_not_been_responded_to</view>\n" +
            "            <view>Alert_is_active</view>\n" +
            "            <view>Alert_is_at_Group_state</view>\n" +
            "            <view>Alert_type_included_in_Code_Blue</view>\n" +
            "            <view>Facility_name_GICH</view>\n" +
            "          </condition>\n" +
            "          <settings>{\"callbackNumber\":\"\",\"callbackResponse\":\"Called\",\"destination\":\"g-code_blue1\",\"displayValues\":[\"Acknowledge\"],\"enunciate\":\"ENUNCIATE_ALWAYS\",\"escalationLevel\":\"#{state}\",\"eventID\":\"NurseCalls:#{id}\",\"message\":\"Unit: #{bed.room.unit.name}\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number}\\n\\nPatient name: #{bed.patient.last_name}, #{bed.patient.first_name}\\nAdmitting Reason: #{bed.patient.reason}\\nReceived: #{created_at.as_date} #{created_at.as_time}\",\"overrideDND\":true,\"parameters\":[],\"participationStatus\":false,\"patientMRN\":\"#{bed.patient.mrn}\",\"priority\":\"0\",\"responseAction\":\"responses.action\",\"ruleAction\":\"SEND_DLS_MESSAGE\",\"shortMessage\":\"#{bed.room.name} Bed #{bed.bed_number} #{alert_type}\",\"storedValues\":[\"Accepted\"],\"subject\":\"#{alert_type} - #{bed.room.name}-#{bed.bed_number}\",\"ttl\":10,\"username\":\"responses.usr.login\"}</settings>\n" +
            "        </rule>\n" +
            "      </interface>\n" +
            "    </interfaces>\n" +
            "  </contents>\n" +
            "</package>";
        
        File xmlFile = tempDir.resolve("test-group-destination.xml").toFile();
        java.nio.file.Files.write(xmlFile.toPath(), xmlContent.getBytes());
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        assertFalse(nurseCalls.isEmpty(), "Should create nurse call flows");
        
        for (ExcelParserV5.FlowRow flow : nurseCalls) {
            System.out.println("Flow: " + flow.alarmName);
            System.out.println("  Device-A: " + flow.deviceA);
            System.out.println("  R1: " + flow.r1);
            System.out.println("  Priority: " + flow.priorityRaw);
            System.out.println("  TTL: " + flow.ttlValue);
            
            // Verify requirements:
            // 1. Destination "g-code_blue1" should be used as recipient R1
            assertEquals("g-code_blue1", flow.r1, 
                "Recipient should be the entire g- destination value");
            
            // 2. Device-A should be set from interface component (VMP)
            assertEquals("VMP", flow.deviceA, 
                "Device-A should be VMP (from interface component)");
            
            // 3. Verify other settings are applied
            assertEquals("0", flow.priorityRaw, "Priority should be 0");
            assertEquals("10", flow.ttlValue, "TTL should be 10");
            
            // 4. Verify alarm type
            assertEquals("Code Blue", flow.alarmName, "Alarm should be Code Blue");
        }
        
        xmlFile.delete();
    }
    
    @Test
    public void testVariousGroupDestinations(@TempDir Path tempDir) throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<package version-major=\"1\" version-minor=\"0\">\n" +
            "  <meta-data><name>Test</name></meta-data>\n" +
            "  <contents>\n" +
            "    <datasets>\n" +
            "      <dataset active=\"true\">\n" +
            "        <name>Clinicals</name>\n" +
            "        <view><name>Alert_CB</name>\n" +
            "          <filter relation=\"equal\"><path>alert_type</path><value>Code Blue</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Alert_CR</name>\n" +
            "          <filter relation=\"equal\"><path>alert_type</path><value>Code Red</value></filter>\n" +
            "        </view>\n" +
            "        <view><name>Alert_CS</name>\n" +
            "          <filter relation=\"equal\"><path>alert_type</path><value>Code Silver</value></filter>\n" +
            "        </view>\n" +
            "      </dataset>\n" +
            "    </datasets>\n" +
            "    <interfaces>\n" +
            "      <interface component=\"VMP\">\n" +
            "        <name>VMP</name>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>Code Blue Group</purpose>\n" +
            "          <trigger-on create=\"true\"/>\n" +
            "          <condition><view>Alert_CB</view></condition>\n" +
            "          <settings>{\"destination\":\"g-code_blue1\",\"priority\":\"1\"}</settings>\n" +
            "        </rule>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>Code Red Group</purpose>\n" +
            "          <trigger-on create=\"true\"/>\n" +
            "          <condition><view>Alert_CR</view></condition>\n" +
            "          <settings>{\"destination\":\"g-emergency_team\",\"priority\":\"2\"}</settings>\n" +
            "        </rule>\n" +
            "        <rule active=\"true\" dataset=\"Clinicals\">\n" +
            "          <purpose>Code Silver Group</purpose>\n" +
            "          <trigger-on create=\"true\"/>\n" +
            "          <condition><view>Alert_CS</view></condition>\n" +
            "          <settings>{\"destination\":\"g-security_response\",\"priority\":\"3\"}</settings>\n" +
            "        </rule>\n" +
            "      </interface>\n" +
            "    </interfaces>\n" +
            "  </contents>\n" +
            "</package>";
        
        File xmlFile = tempDir.resolve("test-various-groups.xml").toFile();
        java.nio.file.Files.write(xmlFile.toPath(), xmlContent.getBytes());
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertEquals(3, clinicals.size(), "Should have 3 flows");
        
        for (ExcelParserV5.FlowRow flow : clinicals) {
            System.out.println("Flow: " + flow.alarmName + ", R1=" + flow.r1 + ", Device-A=" + flow.deviceA);
            
            // All should have VMP as Device-A
            assertEquals("VMP", flow.deviceA, "Device-A should be VMP for " + flow.alarmName);
            
            // Verify recipients match g- destinations
            assertNotNull(flow.r1, "R1 should be set for " + flow.alarmName);
            assertTrue(flow.r1.startsWith("g-"), "R1 should start with g- for " + flow.alarmName);
            
            // Verify specific mappings
            if ("Code Blue".equals(flow.alarmName)) {
                assertEquals("g-code_blue1", flow.r1);
            } else if ("Code Red".equals(flow.alarmName)) {
                assertEquals("g-emergency_team", flow.r1);
            } else if ("Code Silver".equals(flow.alarmName)) {
                assertEquals("g-security_response", flow.r1);
            }
        }
        
        xmlFile.delete();
    }
}
