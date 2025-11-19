package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for XML parser with real-world example from problem statement
 */
class XmlParserComprehensiveTest {

    @TempDir
    Path tempDir;

    @Test
    void testRealWorldExample() throws Exception {
        File xmlFile = tempDir.resolve("real_world.xml").toFile();
        
        // Example from problem statement
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="6" version-minor="6">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Clinicals</name>
                    <view>
                      <name>Critical_Alert_View</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>Critical Heart Rate</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="VMP">
                    <rule active="true" dataset="Clinicals">
                      <purpose>Send Critical Alert</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Critical_Alert_View</view>
                      </condition>
                      <settings>{"callbackNumber":"","callbackResponse":"","destination":"#{bed.locs.assignments.usr.devices.lines.number}","displayValues":["Acknowledge","Escalate"],"enunciate":"SYSTEM_DEFAULT","escalationLevel":"#{state}","eventID":"Clinicals:#{id}","message":"Room: #{bed.room.room_number} - #{bed.bed_number}\\nNote:#{bed.locs.notes}\\n\\n#{alert_summary}\\nAlert Type: #{alert_type}\\nAlarm Time: #{alarm_time.as_time}\\n\\nDetails:\\n#{clinical_details.as_list(, detail_type, value, uom)}","overrideDND":false,"parameters":[{"name":"declineCount","value":"\\"All Recipients\\""}],"participationStatus":true,"patientMRN":"#{clinical_patient.mrn}","priority":"0","responseAction":"responses.action","ruleAction":"SEND_MESSAGE","shortMessage":"#{bed.room.room_number} Bed #{bed.bed_number} #{alert_type}","storedValues":["Accepted","Decline Primary"],"subject":"#{alert_type} - Room #{bed.room.room_number}-#{bed.bed_number}","ttl":10,"username":"responses.usr.login"}</settings>
                    </rule>
                  </interface>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="Clinicals">
                      <purpose>CREATE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Critical_Alert_View</view>
                      </condition>
                      <settings>{}</settings>
                    </rule>
                  </interface>
                </interfaces>
              </contents>
            </package>
            """;
        
        Files.writeString(xmlFile.toPath(), xmlContent);
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        // Verify the flow was created
        assertFalse(parser.getClinicals().isEmpty(), "Should have parsed clinical flows");
        
        ExcelParserV5.FlowRow flow = parser.getClinicals().stream()
            .filter(f -> "Critical Heart Rate".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(flow, "Should have found Critical Heart Rate flow");
        
        // Verify all extracted fields
        System.out.println("Response Options: " + flow.responseOptions);
        System.out.println("Enunciate: " + flow.enunciate);
        System.out.println("Escalate After: " + flow.escalateAfter);
        System.out.println("TTL: " + flow.ttlValue);
        System.out.println("Priority: " + flow.priorityRaw);
        
        // Assert key fields
        assertEquals("Acknowledge,Escalate", flow.responseOptions, 
            "Response options should be extracted as comma-separated");
        assertEquals("SYSTEM_DEFAULT", flow.enunciate, 
            "Enunciate should be SYSTEM_DEFAULT");
        assertEquals("All declines", flow.escalateAfter, 
            "Escalate after should be 'All declines' when declineCount='All Recipients'");
        assertEquals("10", flow.ttlValue, "TTL should be 10");
        assertEquals("Urgent", flow.priorityRaw, "Priority 0 should map to Urgent");
        assertEquals("FALSE", flow.breakThroughDND, "overrideDND false should map to FALSE");
        assertEquals("Yes", flow.emdan, "EMDAN should be Yes for Clinicals");
    }
}
