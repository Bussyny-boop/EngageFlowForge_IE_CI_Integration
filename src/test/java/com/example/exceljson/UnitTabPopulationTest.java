package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for unit tab population from JSON and XML files.
 * Verifies that config groups, facility names, and unit names are properly populated.
 */
public class UnitTabPopulationTest {

    @Test
    public void testJsonLoaderPopulatesUnitTab(@TempDir Path tempDir) throws Exception {
        // Create JSON with units data
        String json = """
            {
              "nurseCalls": {
                "version": "1.1.0",
                "deliveryFlows": [{
                  "name": "SEND NURSECALL | URGENT | Code Blue | Config1 | ",
                  "priority": "urgent",
                  "alarmsAlerts": ["Code Blue"],
                  "interfaces": [{"referenceName": "OutgoingWCTP", "componentName": "OutgoingWCTP"}],
                  "destinations": [{
                    "order": 0,
                    "delayTime": 0,
                    "functionalRoles": [{"facilityName": "Main Hospital", "name": "Charge Nurse"}],
                    "groups": []
                  }],
                  "units": [
                    {"facilityName": "Main Hospital", "name": "ICU"},
                    {"facilityName": "Main Hospital", "name": "ER"}
                  ]
                }]
              },
              "clinicals": {
                "version": "1.1.0",
                "deliveryFlows": [{
                  "name": "SEND CLINICAL | HIGH | Patient Alert | Config2 | ",
                  "priority": "high",
                  "alarmsAlerts": ["Patient Alert"],
                  "interfaces": [{"referenceName": "Vocera", "componentName": "Vocera"}],
                  "destinations": [{
                    "order": 0,
                    "delayTime": 0,
                    "functionalRoles": [],
                    "groups": [{"facilityName": "Main Hospital", "name": "RN Group"}]
                  }],
                  "units": [
                    {"facilityName": "Main Hospital", "name": "ICU"}
                  ]
                }]
              }
            }
            """;

        File jsonFile = tempDir.resolve("test.json").toFile();
        Files.writeString(jsonFile.toPath(), json);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.loadJson(jsonFile);

        // Verify units were created
        assertFalse(parser.units.isEmpty(), "Units list should not be empty");
        
        // Should have 2 unique units: ICU and ER
        assertTrue(parser.units.size() >= 2, "Should have at least 2 unit entries");
        
        // Find ICU unit
        ExcelParserV5.UnitRow icuUnit = parser.units.stream()
            .filter(u -> "ICU".equals(u.unitNames))
            .findFirst()
            .orElse(null);
        
        assertNotNull(icuUnit, "ICU unit should exist");
        assertEquals("Main Hospital", icuUnit.facility, "ICU should have facility 'Main Hospital'");
        
        // The config group should be "Main Hospital_ICU_Config1" (from flow name parts[3])
        assertTrue(icuUnit.nurseGroup.contains("Main Hospital_ICU_Config1"), 
            "ICU should have nurse config group 'Main Hospital_ICU_Config1', but got: " + icuUnit.nurseGroup);
        assertTrue(icuUnit.clinGroup.contains("Main Hospital_ICU_Config2"), 
            "ICU should have clinical config group 'Main Hospital_ICU_Config2', but got: " + icuUnit.clinGroup);
        
        // Find ER unit
        ExcelParserV5.UnitRow erUnit = parser.units.stream()
            .filter(u -> "ER".equals(u.unitNames))
            .findFirst()
            .orElse(null);
        
        assertNotNull(erUnit, "ER unit should exist");
        assertEquals("Main Hospital", erUnit.facility, "ER should have facility 'Main Hospital'");
        assertTrue(erUnit.nurseGroup.contains("Main Hospital_ER_Config1"), 
            "ER should have nurse config group 'Main Hospital_ER_Config1'");
    }

    @Test
    public void testXmlLoaderPopulatesUnitTab(@TempDir Path tempDir) throws Exception {
        // Create XML with facility and unit data
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
                          <path>bed.facility.name</path>
                          <value>BCH</value>
                        </filter>
                        <filter relation="equal">
                          <path>bed.unit.name</path>
                          <value>ICU</value>
                        </filter>
                      </filters>
                    </view>
                  </views>
                </dataset>
              </datasets>
              <interfaces>
                <interface component="DataUpdate">
                  <rules>
                    <rule active="true" dataset="NurseCalls">
                      <purpose>CREATE TRIGGER | Code Blue</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>ICU_Alarms</view>
                      </condition>
                    </rule>
                  </rules>
                </interface>
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

        File xmlFile = tempDir.resolve("test.xml").toFile();
        Files.writeString(xmlFile.toPath(), xml);

        XmlParser parser = new XmlParser();
        parser.load(xmlFile);

        // Verify units were created
        assertFalse(parser.getUnits().isEmpty(), "Units list should not be empty");
        
        // Should have ICU unit
        ExcelParserV5.UnitRow unit = parser.getUnits().get(0);
        
        assertEquals("BCH", unit.facility, "Unit should have facility 'BCH'");
        assertEquals("ICU", unit.unitNames, "Unit should have name 'ICU'");
        assertFalse(unit.nurseGroup.isEmpty(), "Unit should have nurse config group");
        assertTrue(unit.nurseGroup.contains("BCH_ICU_NurseCalls"), 
            "Nurse config group should be 'BCH_ICU_NurseCalls'");
    }

    @Test
    public void testMultipleConfigGroupsForSameUnit(@TempDir Path tempDir) throws Exception {
        // Test that a single unit can have multiple config groups
        String json = """
            {
              "nurseCalls": {
                "version": "1.1.0",
                "deliveryFlows": [
                  {
                    "name": "SEND NURSECALL | URGENT | Alarm1 | Config1 | ",
                    "priority": "urgent",
                    "alarmsAlerts": ["Alarm1"],
                    "interfaces": [{"referenceName": "Edge", "componentName": "Edge"}],
                    "destinations": [],
                    "units": [{"facilityName": "Hospital", "name": "4North"}]
                  },
                  {
                    "name": "SEND NURSECALL | HIGH | Alarm2 | Config2 | ",
                    "priority": "high",
                    "alarmsAlerts": ["Alarm2"],
                    "interfaces": [{"referenceName": "Edge", "componentName": "Edge"}],
                    "destinations": [],
                    "units": [{"facilityName": "Hospital", "name": "4North"}]
                  }
                ]
              }
            }
            """;

        File jsonFile = tempDir.resolve("test.json").toFile();
        Files.writeString(jsonFile.toPath(), json);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.loadJson(jsonFile);

        // Should have only 1 unit (4North)
        assertEquals(1, parser.units.size(), "Should have exactly 1 unit");
        
        ExcelParserV5.UnitRow unit = parser.units.get(0);
        assertEquals("Hospital", unit.facility);
        assertEquals("4North", unit.unitNames);
        
        // Should have both config groups
        assertTrue(unit.nurseGroup.contains("Hospital_4North_Config1"), 
            "Nurse group should contain first config group 'Hospital_4North_Config1'");
        assertTrue(unit.nurseGroup.contains("Hospital_4North_Config2"), 
            "Nurse group should contain second config group 'Hospital_4North_Config2'");
    }

    @Test
    public void testUnitTabWithMixedFlowTypes(@TempDir Path tempDir) throws Exception {
        // Test unit with nurse, clinical, and orders config groups
        String json = """
            {
              "nurseCalls": {
                "deliveryFlows": [{
                  "alarmsAlerts": ["NC Alert"],
                  "interfaces": [{"referenceName": "Edge", "componentName": "Edge"}],
                  "units": [{"facilityName": "Fac1", "name": "Unit1"}]
                }]
              },
              "clinicals": {
                "deliveryFlows": [{
                  "alarmsAlerts": ["Clinical Alert"],
                  "interfaces": [{"referenceName": "Vocera", "componentName": "Vocera"}],
                  "units": [{"facilityName": "Fac1", "name": "Unit1"}]
                }]
              },
              "orders": {
                "deliveryFlows": [{
                  "alarmsAlerts": ["Order Alert"],
                  "interfaces": [{"referenceName": "XMPP", "componentName": "XMPP"}],
                  "units": [{"facilityName": "Fac1", "name": "Unit1"}]
                }]
              }
            }
            """;

        File jsonFile = tempDir.resolve("test.json").toFile();
        Files.writeString(jsonFile.toPath(), json);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.loadJson(jsonFile);

        // Should have 1 unit with all three config group types
        assertEquals(1, parser.units.size(), "Should have exactly 1 unit");
        
        ExcelParserV5.UnitRow unit = parser.units.get(0);
        assertEquals("Fac1", unit.facility);
        assertEquals("Unit1", unit.unitNames);
        
        assertFalse(unit.nurseGroup.isEmpty(), "Should have nurse config group");
        assertFalse(unit.clinGroup.isEmpty(), "Should have clinical config group");
        assertFalse(unit.ordersGroup.isEmpty(), "Should have orders config group");
        
        // Config groups will be like "Fac1_Unit1_<AlarmName>" 
        assertTrue(unit.nurseGroup.contains("Fac1_Unit1"), "Nurse group should contain facility and unit");
        assertTrue(unit.clinGroup.contains("Fac1_Unit1"), "Clinical group should contain facility and unit");
        assertTrue(unit.ordersGroup.contains("Fac1_Unit1"), "Orders group should contain facility and unit");
    }
}
