package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for XML Parser settings extraction including:
 * - Response Options (displayValues)
 * - Genie Enunciation
 * - EMDAN Compliant field
 * - Escalate After field
 */
class XmlParserSettingsTest {

    @Test
    void testResponseOptionsExtraction(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create test XML with displayValues
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="1" version-minor="0">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Clinicals</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="in">
                        <path>alert_type</path>
                        <value>APNEA</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="Clinicals">
                      <purpose>CREATE TRIGGER | APNEA</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                    </rule>
                  </interface>
                  <interface component="VMP">
                    <rule active="true" dataset="Clinicals">
                      <purpose>SEND PRIMARY | APNEA | VMP</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                      <settings>{"destination":"test","priority":"2","ttl":10,\
            "enunciate":"SYSTEM_DEFAULT","overrideDND":false,\
            "displayValues":["Acknowledge","Escalate"]}</settings>
                    </rule>
                  </interface>
                </interfaces>
              </contents>
            </package>""";
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xmlContent);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertEquals(1, clinicals.size(), "Should have 1 clinical row");
        
        ExcelParserV5.FlowRow flow = clinicals.get(0);
        
        // Verify response options were extracted
        assertEquals("Acknowledge,Escalate", flow.responseOptions,
            "Response options should be extracted from displayValues");
    }

    @Test
    void testEnunciationExtraction(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create test XML with enunciate field
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="1" version-minor="0">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>NurseCalls</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="in">
                        <path>alert_type</path>
                        <value>Call</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>CREATE TRIGGER | Call</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                    </rule>
                  </interface>
                  <interface component="VMP">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>SEND PRIMARY | Call | VMP</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                      <settings>{"destination":"test","priority":"1",\
            "enunciate":"ENUNCIATE_ALWAYS","displayValues":["Accept"]}</settings>
                    </rule>
                  </interface>
                </interfaces>
              </contents>
            </package>""";
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xmlContent);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        assertEquals(1, nurseCalls.size(), "Should have 1 nurse call row");
        
        ExcelParserV5.FlowRow flow = nurseCalls.get(0);
        
        // Verify enunciation was extracted and normalized
        assertEquals("ENUNCIATE", flow.enunciate,
            "Enunciation should be extracted and ENUNCIATE_ALWAYS normalized to ENUNCIATE");
    }

    @Test
    void testEmdanFieldForClinicals(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create test XML for Clinicals dataset
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="1" version-minor="0">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Clinicals</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="in">
                        <path>alert_type</path>
                        <value>APNEA</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="Clinicals">
                      <purpose>CREATE TRIGGER | APNEA</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                    </rule>
                  </interface>
                  <interface component="VMP">
                    <rule active="true" dataset="Clinicals">
                      <purpose>SEND PRIMARY | APNEA | VMP</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                      <settings>{"destination":"test","priority":"2"}</settings>
                    </rule>
                  </interface>
                </interfaces>
              </contents>
            </package>""";
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xmlContent);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertEquals(1, clinicals.size(), "Should have 1 clinical row");
        
        ExcelParserV5.FlowRow flow = clinicals.get(0);
        
        // Verify EMDAN field is set to "Yes" for Clinicals dataset
        assertEquals("Yes", flow.emdan,
            "EMDAN Compliant should be 'Yes' for Clinicals dataset");
    }

    @Test
    void testEmdanFieldForNurseCalls(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create test XML for NurseCalls dataset
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="1" version-minor="0">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>NurseCalls</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="in">
                        <path>alert_type</path>
                        <value>Call</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>CREATE TRIGGER | Call</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                    </rule>
                  </interface>
                  <interface component="VMP">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>SEND PRIMARY | Call | VMP</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                      <settings>{"destination":"test","priority":"1"}</settings>
                    </rule>
                  </interface>
                </interfaces>
              </contents>
            </package>""";
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xmlContent);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        assertEquals(1, nurseCalls.size(), "Should have 1 nurse call row");
        
        ExcelParserV5.FlowRow flow = nurseCalls.get(0);
        
        // Verify EMDAN field is set to "No" for NurseCalls dataset
        assertEquals("No", flow.emdan,
            "EMDAN Compliant should be 'No' for NurseCalls dataset");
    }

    @Test
    void testEscalateAfterField(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create test XML
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="1" version-minor="0">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Clinicals</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="in">
                        <path>alert_type</path>
                        <value>APNEA</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="Clinicals">
                      <purpose>CREATE TRIGGER | APNEA</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                    </rule>
                  </interface>
                  <interface component="VMP">
                    <rule active="true" dataset="Clinicals">
                      <purpose>SEND PRIMARY | APNEA | VMP</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                      <settings>{"destination":"test","priority":"2"}</settings>
                    </rule>
                  </interface>
                </interfaces>
              </contents>
            </package>""";
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xmlContent);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertEquals(1, clinicals.size(), "Should have 1 clinical row");
        
        ExcelParserV5.FlowRow flow = clinicals.get(0);
        
        // Verify escalateAfter field is set to "1 decline"
        assertEquals("1 decline", flow.escalateAfter,
            "Escalate After should be set to '1 decline' when reading XML data");
    }

    @Test
    void testResponsesArrayStructure(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create test XML with responses array structure (from north_western_cdh_test.xml)
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="1" version-minor="0">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>NurseCalls</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="in">
                        <path>alert_type</path>
                        <value>Nurse</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>CREATE TRIGGER | Nurse</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                    </rule>
                  </interface>
                  <interface component="VMP">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>SEND PRIMARY | Nurse | VMP</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                      <settings>{"destination":"test","priority":"2","ttl":10,\
            "responseAction":"responses.action","responses":[\
            {"$$hashKey":"00I","displayValue":"Acknowledge","storedValue":"Accepted"},\
            {"displayValue":"Escalate","storedValue":"Decline Primary","$$hashKey":"00S"}\
            ]}</settings>
                    </rule>
                  </interface>
                </interfaces>
              </contents>
            </package>""";
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xmlContent);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        assertEquals(1, nurseCalls.size(), "Should have 1 nurse call row");
        
        ExcelParserV5.FlowRow flow = nurseCalls.get(0);
        
        // Verify response options were extracted from responses array
        assertEquals("Acknowledge,Escalate", flow.responseOptions,
            "Response options should be extracted from responses array displayValue fields");
    }

    @Test
    void testAllFieldsTogether(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        // Create comprehensive test XML with all fields
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="1" version-minor="0">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Clinicals</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="in">
                        <path>alert_type</path>
                        <value>APNEA</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="Clinicals">
                      <purpose>CREATE TRIGGER | APNEA</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                    </rule>
                  </interface>
                  <interface component="VMP">
                    <rule active="true" dataset="Clinicals">
                      <purpose>SEND PRIMARY | APNEA | VMP</purpose>
                      <trigger-on create="true"/>
                      <condition><view>Test_View</view></condition>
                      <settings>{"destination":"test","priority":"0","ttl":15,\
            "enunciate":"SYSTEM_DEFAULT","overrideDND":true,\
            "displayValues":["Acknowledge","Escalate"]}</settings>
                    </rule>
                  </interface>
                </interfaces>
              </contents>
            </package>""";
        
        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xmlContent);
        }
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertEquals(1, clinicals.size(), "Should have 1 clinical row");
        
        ExcelParserV5.FlowRow flow = clinicals.get(0);
        
        // Verify all fields
        assertEquals("Acknowledge,Escalate", flow.responseOptions,
            "Response options should be extracted from displayValues");
        assertEquals("SYSTEM_DEFAULT", flow.enunciate,
            "Enunciation should be extracted");
        assertEquals("Yes", flow.emdan,
            "EMDAN Compliant should be 'Yes' for Clinicals dataset");
        assertEquals("1 decline", flow.escalateAfter,
            "Escalate After should be set to '1 decline'");
        assertEquals("Urgent", flow.priorityRaw,
            "Priority should be mapped correctly");
        assertEquals("15", flow.ttlValue,
            "TTL should be extracted");
        assertEquals("TRUE", flow.breakThroughDND,
            "Break Through DND should be extracted");
    }
}
