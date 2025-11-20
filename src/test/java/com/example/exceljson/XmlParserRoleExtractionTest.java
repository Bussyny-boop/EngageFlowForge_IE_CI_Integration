package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify role extraction logic from XML views
 * Based on the requirement:
 * - Use ALL values when path ends in "role.name" with "in", "like", "equal", "contains" relations
 * - Use "AllRoles_except_X" notation for "not_in", "not_like", "not_equal" relations
 * - Extract role names ONLY from filter values, not from view names or descriptions
 */
class XmlParserRoleExtractionTest {

    @Test
    void testRoleExtractionWithInRelation(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create XML with role.name path and "in" relation with comma-separated values
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="5" version-minor="5">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Clinicals</name>
                    <view>
                      <name>Role_ALL_Unit_Caregivers_Online_VMP</name>
                      <description>aaa All Unit Caregivers (PCT, RN &amp; Charge Nurse) online with VMP phones</description>
                      <filter relation="in">
                        <path>bed.room.unit.rooms.beds.locations.assignments.role.name</path>
                        <value>PCT,TECH,RN,Registered Nurse,CHARGE,Charge Nurse</value>
                      </filter>
                      <filter relation="equal">
                        <path>bed.room.unit.rooms.beds.locations.assignments.state</path>
                        <value>Active</value>
                      </filter>
                    </view>
                    <view>
                      <name>Alert_is_Asystole</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>Asystole</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="DataUpdate">
                    <name>DataUpdate</name>
                    <rule active="true" dataset="Clinicals">
                      <purpose>CREATE | Alarm</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Alert_is_Asystole</view>
                      </condition>
                    </rule>
                  </interface>
                  <interface component="VMP">
                    <name>VMP</name>
                    <rule active="true" dataset="Clinicals">
                      <purpose>SEND | Asystole | All Unit Caregivers</purpose>
                      <trigger-on update="true"/>
                      <condition>
                        <view>Role_ALL_Unit_Caregivers_Online_VMP</view>
                        <view>Alert_is_Asystole</view>
                      </condition>
                      <settings>{"destination":"#{bed.room.unit.rooms.beds.locations.assignments.usr.devices.lines.number}","priority":"0","ttl":"300"}</settings>
                    </rule>
                  </interface>
                </interfaces>
              </contents>
            </package>
            """;
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xml);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        assertEquals(1, parser.getClinicals().size(), "Should have 1 clinical alert");
        ExcelParserV5.FlowRow flow = parser.getClinicals().get(0);
        
        // Verify ALL roles are captured, formatted with VAssign:[Room] prefix and separated by newlines
        String expected = "VAssign:[Room] PCT\n" +
                         "VAssign:[Room] TECH\n" +
                         "VAssign:[Room] RN\n" +
                         "VAssign:[Room] Registered Nurse\n" +
                         "VAssign:[Room] CHARGE\n" +
                         "VAssign:[Room] Charge Nurse";
        assertEquals(expected, flow.r1, 
            "All comma-separated role values should be captured when path ends with 'role.name' and relation is 'in'");
        
        System.out.println("✅ Role extraction with 'in' relation test PASSED");
        System.out.println("   Recipients:");
        for (String line : flow.r1.split("\n")) {
            System.out.println("     - " + line);
        }
    }

    @Test
    void testRoleExtractionWithNotInRelation(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create XML with role.name path and "not_in" relation
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="5" version-minor="5">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Clinicals</name>
                    <view>
                      <name>Role_Exclude_PCT</name>
                      <description>All roles except PCT</description>
                      <filter relation="not_in">
                        <path>bed.room.unit.rooms.beds.locations.assignments.role.name</path>
                        <value>PCT,TECH</value>
                      </filter>
                    </view>
                    <view>
                      <name>Alert_is_Asystole</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>Asystole</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="DataUpdate">
                    <name>DataUpdate</name>
                    <rule active="true" dataset="Clinicals">
                      <purpose>CREATE | Alarm</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Alert_is_Asystole</view>
                      </condition>
                    </rule>
                  </interface>
                  <interface component="VMP">
                    <name>VMP</name>
                    <rule active="true" dataset="Clinicals">
                      <purpose>SEND | Asystole | All except PCT</purpose>
                      <trigger-on update="true"/>
                      <condition>
                        <view>Role_Exclude_PCT</view>
                        <view>Alert_is_Asystole</view>
                      </condition>
                      <settings>{"destination":"#{bed.room.unit.rooms.beds.locations.assignments.usr.devices.lines.number}","priority":"0","ttl":"300"}</settings>
                    </rule>
                  </interface>
                </interfaces>
              </contents>
            </package>
            """;
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xml);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        assertEquals(1, parser.getClinicals().size(), "Should have 1 clinical alert");
        ExcelParserV5.FlowRow flow = parser.getClinicals().get(0);
        
        // Verify exclusion notation is used
        String expected = "AllRoles_except_PCT,TECH";
        assertEquals(expected, flow.r1, 
            "Should use 'AllRoles_except_X' notation when relation is 'not_in'");
        
        System.out.println("✅ Role extraction with 'not_in' relation test PASSED");
        System.out.println("   Recipient: " + flow.r1);
    }

    @Test
    void testRoleExtractionIgnoresViewNameAndDescription(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create XML where view name/description contains role names that should be ignored
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="5" version-minor="5">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Clinicals</name>
                    <view>
                      <name>Role_RN_and_PCT_Online</name>
                      <description>RN and PCT roles online</description>
                      <filter relation="in">
                        <path>bed.room.unit.rooms.beds.locations.assignments.role.name</path>
                        <value>Nurse</value>
                      </filter>
                    </view>
                    <view>
                      <name>Alert_is_Asystole</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>Asystole</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="DataUpdate">
                    <name>DataUpdate</name>
                    <rule active="true" dataset="Clinicals">
                      <purpose>CREATE | Alarm</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Alert_is_Asystole</view>
                      </condition>
                    </rule>
                  </interface>
                  <interface component="VMP">
                    <name>VMP</name>
                    <rule active="true" dataset="Clinicals">
                      <purpose>SEND | Asystole | Nurse</purpose>
                      <trigger-on update="true"/>
                      <condition>
                        <view>Role_RN_and_PCT_Online</view>
                        <view>Alert_is_Asystole</view>
                      </condition>
                      <settings>{"destination":"#{bed.room.unit.rooms.beds.locations.assignments.usr.devices.lines.number}","priority":"0","ttl":"300"}</settings>
                    </rule>
                  </interface>
                </interfaces>
              </contents>
            </package>
            """;
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xml);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        assertEquals(1, parser.getClinicals().size(), "Should have 1 clinical alert");
        ExcelParserV5.FlowRow flow = parser.getClinicals().get(0);
        
        // Should only use "Nurse" from the filter value, not "RN and PCT" from view name/description
        String expected = "VAssign:[Room] Nurse";
        assertEquals(expected, flow.r1, 
            "Should only extract role from filter path value, ignoring view name and description");
        
        System.out.println("✅ Role extraction ignores view name/description test PASSED");
        System.out.println("   Recipient: " + flow.r1);
        System.out.println("   (Correctly ignored 'RN' and 'PCT' from view name/description)");
    }

    @Test
    void testRoleExtractionWithEqualRelation(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create XML with role.name path and "equal" relation with comma-separated values
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="5" version-minor="5">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Clinicals</name>
                    <view>
                      <name>Role_Multiple</name>
                      <description>Multiple roles</description>
                      <filter relation="equal">
                        <path>bed.room.unit.rooms.beds.locations.assignments.role.name</path>
                        <value>RN,Charge Nurse</value>
                      </filter>
                    </view>
                    <view>
                      <name>Alert_is_Asystole</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>Asystole</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="DataUpdate">
                    <name>DataUpdate</name>
                    <rule active="true" dataset="Clinicals">
                      <purpose>CREATE | Alarm</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Alert_is_Asystole</view>
                      </condition>
                    </rule>
                  </interface>
                  <interface component="VMP">
                    <name>VMP</name>
                    <rule active="true" dataset="Clinicals">
                      <purpose>SEND | Asystole | RN and Charge</purpose>
                      <trigger-on update="true"/>
                      <condition>
                        <view>Role_Multiple</view>
                        <view>Alert_is_Asystole</view>
                      </condition>
                      <settings>{"destination":"#{bed.room.unit.rooms.beds.locations.assignments.usr.devices.lines.number}","priority":"0","ttl":"300"}</settings>
                    </rule>
                  </interface>
                </interfaces>
              </contents>
            </package>
            """;
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xml);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        assertEquals(1, parser.getClinicals().size(), "Should have 1 clinical alert");
        ExcelParserV5.FlowRow flow = parser.getClinicals().get(0);
        
        // Verify ALL roles are captured with "equal" relation
        String expected = "VAssign:[Room] RN\nVAssign:[Room] Charge Nurse";
        assertEquals(expected, flow.r1, 
            "All comma-separated role values should be captured when relation is 'equal'");
        
        System.out.println("✅ Role extraction with 'equal' relation test PASSED");
        System.out.println("   Recipients:");
        for (String line : flow.r1.split("\n")) {
            System.out.println("     - " + line);
        }
    }
}
