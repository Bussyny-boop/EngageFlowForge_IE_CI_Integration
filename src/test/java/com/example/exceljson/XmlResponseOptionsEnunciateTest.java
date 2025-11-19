package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for extracting response options and Genie enunciation from XML parser.
 * Verifies that displayValues and enunciate are correctly parsed from adapter send rule settings.
 */
class XmlResponseOptionsEnunciateTest {

    @TempDir
    Path tempDir;

    /**
     * Test that response options (displayValues) are extracted from adapter send rules
     */
    @Test
    void testDisplayValuesExtractedFromXml() throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="6" version-minor="6">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>NurseCalls</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>Bed Exit</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="VMP">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>TEST SEND RULE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
                      </condition>
                      <settings>{"destination":"test-group","displayValues":["Acknowledge","Escalate"],"enunciate":"SYSTEM_DEFAULT","priority":"1","ttl":10,"overrideDND":false}</settings>
                    </rule>
                  </interface>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>CREATE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
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
        
        // Verify nurse call flows were created
        assertFalse(parser.getNurseCalls().isEmpty(), "Should have parsed nurse call flows");
        
        // Find the flow with response options
        ExcelParserV5.FlowRow flow = parser.getNurseCalls().stream()
            .filter(f -> "Bed Exit".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(flow, "Should have found the Bed Exit flow");
        
        // Verify displayValues were extracted
        assertNotNull(flow.responseOptions, "Response options should be extracted");
        assertEquals("Acknowledge,Escalate", flow.responseOptions, 
            "Response options should be 'Acknowledge,Escalate'");
    }

    /**
     * Test that enunciate value is extracted from adapter send rules
     */
    @Test
    void testEnunciateExtractedFromXml() throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="6" version-minor="6">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>NurseCalls</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>Call Request</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="VMP">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>TEST SEND RULE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
                      </condition>
                      <settings>{"destination":"test-group","displayValues":["Accept"],"enunciate":"ENUNCIATE_ALWAYS","priority":"1","ttl":10,"overrideDND":false}</settings>
                    </rule>
                  </interface>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>CREATE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
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
        
        // Find the flow
        ExcelParserV5.FlowRow flow = parser.getNurseCalls().stream()
            .filter(f -> "Call Request".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(flow, "Should have found the Call Request flow");
        
        // Verify enunciate was extracted and normalized
        assertNotNull(flow.enunciate, "Enunciate should be extracted");
        assertEquals("ENUNCIATE", flow.enunciate, 
            "Enunciate should be normalized from ENUNCIATE_ALWAYS to ENUNCIATE");
    }

    /**
     * Test that SYSTEM_DEFAULT enunciate value is preserved
     */
    @Test
    void testSystemDefaultEnunciatePreserved() throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="6" version-minor="6">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Clinicals</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>High Heart Rate</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="VMP">
                    <rule active="true" dataset="Clinicals">
                      <purpose>TEST SEND RULE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
                      </condition>
                      <settings>{"destination":"test-group","displayValues":["Acknowledge","Escalate"],"enunciate":"SYSTEM_DEFAULT","priority":"0","ttl":10,"overrideDND":true}</settings>
                    </rule>
                  </interface>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="Clinicals">
                      <purpose>CREATE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
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
        
        // Find the flow in clinicals
        ExcelParserV5.FlowRow flow = parser.getClinicals().stream()
            .filter(f -> "High Heart Rate".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(flow, "Should have found the High Heart Rate flow");
        
        // Verify enunciate preserved as SYSTEM_DEFAULT
        assertEquals("SYSTEM_DEFAULT", flow.enunciate, 
            "SYSTEM_DEFAULT enunciate should be preserved");
    }

    /**
     * Test that EMDAN field is set to "Yes" for Clinicals dataset
     */
    @Test
    void testEmdanYesForClinicals() throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="6" version-minor="6">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Clinicals</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>Arrhythmia</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="VMP">
                    <rule active="true" dataset="Clinicals">
                      <purpose>TEST SEND RULE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
                      </condition>
                      <settings>{"destination":"test-group","displayValues":["Acknowledge"],"enunciate":"ENUNCIATE_ALWAYS","priority":"0","ttl":15,"overrideDND":false}</settings>
                    </rule>
                  </interface>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="Clinicals">
                      <purpose>CREATE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
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
        
        // Find the flow in clinicals
        ExcelParserV5.FlowRow flow = parser.getClinicals().stream()
            .filter(f -> "Arrhythmia".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(flow, "Should have found the Arrhythmia flow");
        
        // Verify EMDAN is set to "Yes" for Clinicals
        assertEquals("Yes", flow.emdan, 
            "EMDAN should be 'Yes' for Clinicals dataset");
    }

    /**
     * Test that EMDAN field is set to "No" for NurseCalls dataset
     */
    @Test
    void testEmdanNoForNurseCalls() throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="6" version-minor="6">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>NurseCalls</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>Call Request</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="VMP">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>TEST SEND RULE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
                      </condition>
                      <settings>{"destination":"test-group","displayValues":["Accept","Decline"],"enunciate":"SYSTEM_DEFAULT","priority":"1","ttl":10,"overrideDND":false}</settings>
                    </rule>
                  </interface>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>CREATE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
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
        
        // Find the flow in nurse calls
        ExcelParserV5.FlowRow flow = parser.getNurseCalls().stream()
            .filter(f -> "Call Request".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(flow, "Should have found the Call Request flow");
        
        // Verify EMDAN is set to "No" for NurseCalls
        assertEquals("No", flow.emdan, 
            "EMDAN should be 'No' for NurseCalls dataset");
    }

    /**
     * Test that EMDAN field is set to "No" for Orders dataset
     */
    @Test
    void testEmdanNoForOrders() throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="6" version-minor="6">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Orders</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>Lab Order</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="VMP">
                    <rule active="true" dataset="Orders">
                      <purpose>TEST SEND RULE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
                      </condition>
                      <settings>{"destination":"test-group","displayValues":["Accept"],"enunciate":"SYSTEM_DEFAULT","priority":"2","ttl":20,"overrideDND":false}</settings>
                    </rule>
                  </interface>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="Orders">
                      <purpose>CREATE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
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
        
        // Find the flow in orders
        ExcelParserV5.FlowRow flow = parser.getOrders().stream()
            .filter(f -> "Lab Order".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(flow, "Should have found the Lab Order flow");
        
        // Verify EMDAN is set to "No" for Orders
        assertEquals("No", flow.emdan, 
            "EMDAN should be 'No' for Orders dataset");
    }

    /**
     * Test that multiple response options are correctly joined
     */
    @Test
    void testMultipleResponseOptions() throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="6" version-minor="6">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>NurseCalls</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>Emergency</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="VMP">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>TEST SEND RULE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
                      </condition>
                      <settings>{"destination":"test-group","displayValues":["Accept","Escalate","Call Back"],"enunciate":"SYSTEM_DEFAULT","priority":"0","ttl":10,"overrideDND":true}</settings>
                    </rule>
                  </interface>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="NurseCalls">
                      <purpose>CREATE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
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
        
        // Find the flow
        ExcelParserV5.FlowRow flow = parser.getNurseCalls().stream()
            .filter(f -> "Emergency".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(flow, "Should have found the Emergency flow");
        
        // Verify all three response options are present
        assertEquals("Accept,Escalate,Call Back", flow.responseOptions, 
            "All three response options should be present and comma-separated");
    }

    /**
     * Test that empty displayValues array results in empty response options
     */
    @Test
    void testEmptyDisplayValues() throws Exception {
        File xmlFile = tempDir.resolve("test.xml").toFile();
        
        String xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package version-major="6" version-minor="6">
              <contents>
                <datasets>
                  <dataset active="true">
                    <name>Orders</name>
                    <view>
                      <name>Test_View</name>
                      <filter relation="equal">
                        <path>alert_type</path>
                        <value>Lab Order</value>
                      </filter>
                    </view>
                  </dataset>
                </datasets>
                <interfaces>
                  <interface component="VMP">
                    <rule active="true" dataset="Orders">
                      <purpose>TEST SEND RULE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
                      </condition>
                      <settings>{"destination":"test-group","displayValues":[],"enunciate":"SYSTEM_DEFAULT","priority":"2","ttl":20,"overrideDND":false}</settings>
                    </rule>
                  </interface>
                  <interface component="DataUpdate">
                    <rule active="true" dataset="Orders">
                      <purpose>CREATE</purpose>
                      <trigger-on create="true"/>
                      <condition>
                        <view>Test_View</view>
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
        
        // Find the flow
        ExcelParserV5.FlowRow flow = parser.getOrders().stream()
            .filter(f -> "Lab Order".equals(f.alarmName))
            .findFirst()
            .orElse(null);
        
        assertNotNull(flow, "Should have found the Lab Order flow");
        
        // Verify empty displayValues results in empty string
        assertEquals("", flow.responseOptions, 
            "Empty displayValues array should result in empty response options");
    }
}
