package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify VAssign:[Room] formatting and newline separators for XML parsing
 */
class XmlParserVAssignTest {

    @Test
    void testVAssignRoomPrefixForRoles(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create XML with role-based recipient
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <engage-configuration>
              <datasets>
                <dataset active="true">
                  <name>NurseCalls</name>
                  <views>
                    <view>
                      <name>ICU_Alarms</name>
                      <filters>
                        <filter relation="equal">
                          <path>alert_type</path>
                          <value>Code Blue</value>
                        </filter>
                        <filter relation="equal">
                          <path>bed.unit.name</path>
                          <value>ICU</value>
                        </filter>
                        <filter relation="equal">
                          <path>bed.assignments.role.name</path>
                          <value>Nurse</value>
                        </filter>
                      </filters>
                    </view>
                  </views>
                </dataset>
              </datasets>
              <interfaces>
                <interface component="VMP">
                  <rules>
                    <rule active="true" dataset="NurseCalls">
                      <purpose>Send to Nurse</purpose>
                      <trigger-on create="true" update="false"/>
                      <condition>
                        <view>ICU_Alarms</view>
                      </condition>
                      <settings>{"priority":"0","ttl":"300"}</settings>
                    </rule>
                  </rules>
                </interface>
              </interfaces>
            </engage-configuration>
            """;
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xml);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        assertEquals(1, parser.getNurseCalls().size(), "Should have 1 nurse call");
        ExcelParserV5.FlowRow flow = parser.getNurseCalls().get(0);
        
        // Verify VAssign:[Room] prefix is applied
        assertEquals("VAssign:[Room] Nurse", flow.r1, 
            "Recipient should have VAssign:[Room] prefix for role-based recipient");
        
        System.out.println("✅ VAssign:[Room] prefix test PASSED");
        System.out.println("   Recipient: " + flow.r1);
    }

    @Test
    void testNewlineSeparatorForMultipleRecipients(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create XML with comma-separated roles
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <engage-configuration>
              <datasets>
                <dataset active="true">
                  <name>NurseCalls</name>
                  <views>
                    <view>
                      <name>Multi_Roles</name>
                      <filters>
                        <filter relation="equal">
                          <path>alert_type</path>
                          <value>Emergency</value>
                        </filter>
                        <filter relation="equal">
                          <path>bed.assignments.role.name</path>
                          <value>Nurse,Doctor,RT</value>
                        </filter>
                      </filters>
                    </view>
                  </views>
                </dataset>
              </datasets>
              <interfaces>
                <interface component="VMP">
                  <rules>
                    <rule active="true" dataset="NurseCalls">
                      <purpose>Send to multiple roles</purpose>
                      <trigger-on create="true" update="false"/>
                      <condition>
                        <view>Multi_Roles</view>
                      </condition>
                      <settings>{"priority":"0"}</settings>
                    </rule>
                  </rules>
                </interface>
              </interfaces>
            </engage-configuration>
            """;
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xml);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        assertEquals(1, parser.getNurseCalls().size(), "Should have 1 nurse call");
        ExcelParserV5.FlowRow flow = parser.getNurseCalls().get(0);
        
        // Verify newline separator (not comma)
        String expected = "VAssign:[Room] Nurse\nVAssign:[Room] Doctor\nVAssign:[Room] RT";
        assertEquals(expected, flow.r1, 
            "Recipients should be separated by newline (\\n), not comma");
        
        // Verify it contains newlines
        assertTrue(flow.r1.contains("\n"), "Recipient string should contain newlines");
        assertFalse(flow.r1.contains(", "), "Recipient string should NOT contain comma-space separator");
        
        // Count newlines - should have 2 (for 3 recipients)
        long newlineCount = flow.r1.chars().filter(ch -> ch == '\n').count();
        assertEquals(2, newlineCount, "Should have 2 newlines for 3 recipients");
        
        System.out.println("✅ Newline separator test PASSED");
        System.out.println("   Recipients:");
        for (String line : flow.r1.split("\n")) {
            System.out.println("     - " + line);
        }
    }

    @Test
    void testConfigGroupWithFacilityAndUnit(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create XML with facility and unit
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <engage-configuration>
              <datasets>
                <dataset active="true">
                  <name>NurseCalls</name>
                  <views>
                    <view>
                      <name>BCH_ICU_Alarms</name>
                      <filters>
                        <filter relation="equal">
                          <path>alert_type</path>
                          <value>High Priority</value>
                        </filter>
                        <filter relation="equal">
                          <path>bed.unit.facility.name</path>
                          <value>BCH</value>
                        </filter>
                        <filter relation="equal">
                          <path>bed.unit.name</path>
                          <value>ICU</value>
                        </filter>
                        <filter relation="equal">
                          <path>bed.assignments.role.name</path>
                          <value>Charge Nurse</value>
                        </filter>
                      </filters>
                    </view>
                  </views>
                </dataset>
              </datasets>
              <interfaces>
                <interface component="VMP">
                  <rules>
                    <rule active="true" dataset="NurseCalls">
                      <purpose>Send to Charge Nurse</purpose>
                      <trigger-on create="true" update="false"/>
                      <condition>
                        <view>BCH_ICU_Alarms</view>
                      </condition>
                      <settings>{"priority":"0"}</settings>
                    </rule>
                  </rules>
                </interface>
              </interfaces>
            </engage-configuration>
            """;
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xml);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        assertEquals(1, parser.getNurseCalls().size(), "Should have 1 nurse call");
        ExcelParserV5.FlowRow flow = parser.getNurseCalls().get(0);
        
        // Verify config group includes facility, unit, and dataset (Facility_Unit_Dataset format)
        assertEquals("BCH_ICU_NurseCalls", flow.configGroup, 
            "Config group should be 'BCH_ICU_NurseCalls' (Facility_Unit_Dataset format)");
        
        // Verify unit row was created with facility
        assertEquals(1, parser.getUnits().size(), "Should have 1 unit row");
        ExcelParserV5.UnitRow unit = parser.getUnits().get(0);
        assertEquals("BCH", unit.facility, "Unit should have facility 'BCH'");
        assertEquals("ICU", unit.unitNames, "Unit should have name 'ICU'");
        
        System.out.println("✅ Config group (facility + unit) test PASSED");
        System.out.println("   Config Group: " + flow.configGroup);
        System.out.println("   Facility: " + unit.facility);
        System.out.println("   Unit: " + unit.unitNames);
    }

    @Test
    void testAllThreeFeaturesTogether(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <engage-configuration>
              <datasets>
                <dataset active="true">
                  <name>Clinicals</name>
                  <views>
                    <view>
                      <name>Hospital_ER_Monitors</name>
                      <filters>
                        <filter relation="equal">
                          <path>alert_type</path>
                          <value>SpO2 Low,Heart Rate High</value>
                        </filter>
                        <filter relation="equal">
                          <path>bed.unit.facility.name</path>
                          <value>MainHospital</value>
                        </filter>
                        <filter relation="equal">
                          <path>bed.unit.name</path>
                          <value>ER</value>
                        </filter>
                        <filter relation="equal">
                          <path>bed.assignments.role.name</path>
                          <value>Nurse,Physician</value>
                        </filter>
                      </filters>
                    </view>
                  </views>
                </dataset>
              </datasets>
              <interfaces>
                <interface component="VMP">
                  <rules>
                    <rule active="true" dataset="Clinicals">
                      <purpose>Clinical Monitoring</purpose>
                      <trigger-on create="true" update="false"/>
                      <condition>
                        <view>Hospital_ER_Monitors</view>
                      </condition>
                      <settings>{"priority":"1","ttl":"600"}</settings>
                    </rule>
                  </rules>
                </interface>
              </interfaces>
            </engage-configuration>
            """;
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xml);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        // Should create 2 flows (one per alert type)
        assertEquals(2, parser.getClinicals().size(), "Should have 2 clinical flows");
        
        for (ExcelParserV5.FlowRow flow : parser.getClinicals()) {
            // Feature 1: VAssign:[Room] prefix
            assertTrue(flow.r1.startsWith("VAssign:[Room] "), 
                "Recipient should start with VAssign:[Room]");
            
            // Feature 2: Newline separator (not comma)
            assertTrue(flow.r1.contains("\n"), 
                "Should use newline separator for multiple recipients");
            assertEquals("VAssign:[Room] Nurse\nVAssign:[Room] Physician", flow.r1,
                "Should have both recipients with newline separator");
            
            // Feature 3: Config group with facility, unit, and dataset (Facility_Unit_Dataset format)
            assertEquals("MainHospital_ER_Clinicals", flow.configGroup,
                "Config group should be 'MainHospital_ER_Clinicals' (Facility_Unit_Dataset format)");
        }
        
        // Verify unit row
        ExcelParserV5.UnitRow unit = parser.getUnits().get(0);
        assertEquals("MainHospital", unit.facility);
        assertEquals("ER", unit.unitNames);
        
        System.out.println("✅ ALL THREE FEATURES test PASSED");
        System.out.println("\n📋 Summary of Features:");
        System.out.println("   1. VAssign:[Room] prefix: ✓");
        System.out.println("   2. Newline separator: ✓");
        System.out.println("   3. Facility + Unit config: ✓");
        System.out.println("\n   Example Recipient Field:");
        System.out.println("   " + parser.getClinicals().get(0).r1.replace("\n", "\n   "));
        System.out.println("\n   Config Group: " + parser.getClinicals().get(0).configGroup);
        System.out.println("   Facility: " + unit.facility);
        System.out.println("   Unit: " + unit.unitNames);
    }
}
