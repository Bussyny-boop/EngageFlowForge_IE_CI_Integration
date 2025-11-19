package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that SEND rules are only processed when there's a CREATE DataUpdate rule
 * that includes the alert type (i.e., the alert is NOT excluded via NOT_IN).
 */
public class NotInExclusionSendRuleTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSendRuleNotProcessedWhenAlertExcludedByNotIn() throws Exception {
        // Create XML with:
        // 1. DataUpdate CREATE rule that excludes "Code Blue" via NOT_IN
        // 2. VMP SEND rule for "Code Blue" (should NOT be processed)
        // 3. VMP SEND rule for "Probe Disconnect" (should be processed)
        
        String xml = """
<?xml version="1.0" encoding="UTF-8"?>
<engage>
  <dataset active="true">
    <name>Clinicals</name>
    <view>
      <name>AllAlertsExceptCodeBlue</name>
      <filter relation="not_in">
        <path>alert_type</path>
        <value>Code Blue</value>
      </filter>
    </view>
    <view>
      <name>CodeBlueView</name>
      <filter relation="equal">
        <path>alert_type</path>
        <value>Code Blue</value>
      </filter>
    </view>
    <view>
      <name>ProbeDisconnectOnly</name>
      <filter relation="equal">
        <path>alert_type</path>
        <value>Probe Disconnect</value>
      </filter>
    </view>
  </dataset>
  
  <interface component="DataUpdate">
    <rule active="true" dataset="Clinicals">
      <purpose>Create alerts excluding Code Blue</purpose>
      <trigger-on create="true" update="false"/>
      <condition>
        <view>AllAlertsExceptCodeBlue</view>
      </condition>
      <settings>{"state": "Primary"}</settings>
    </rule>
  </interface>
  
  <interface component="VMP">
    <rule active="true" dataset="Clinicals">
      <purpose>Send Code Blue to Nurse (should NOT be processed)</purpose>
      <trigger-on create="false" update="true"/>
      <condition>
        <view>CodeBlueView</view>
      </condition>
      <settings>{"destination": "Nurse"}</settings>
    </rule>
  </interface>
  
  <interface component="VMP">
    <rule active="true" dataset="Clinicals">
      <purpose>Send Probe Disconnect to Nurse (should be processed)</purpose>
      <trigger-on create="false" update="true"/>
      <condition>
        <view>ProbeDisconnectOnly</view>
      </condition>
      <settings>{"destination": "Nurse"}</settings>
    </rule>
  </interface>
</engage>
""";

        File xmlFile = tempDir.resolve("test.xml").toFile();
        Files.writeString(xmlFile.toPath(), xml);

        XmlParser parser = new XmlParser();
        parser.load(xmlFile);

        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();

        System.out.println("=== NOT_IN Exclusion Test Debug ===");
        System.out.println("Total clinical flows: " + clinicals.size());
        for (ExcelParserV5.FlowRow flow : clinicals) {
            System.out.println("  Flow: " + flow.alarmName + " | Config: " + flow.configGroup);
        }

        // Verify: Should have NO flows for "Code Blue" (excluded by NOT_IN)
        long codeBlueCount = clinicals.stream()
            .filter(f -> "Code Blue".equalsIgnoreCase(f.alarmName))
            .count();
        
        assertEquals(0, codeBlueCount, 
            "Code Blue should NOT be processed because it's excluded by NOT_IN in DataUpdate CREATE rule");

        // Verify: Should have flows for "Probe Disconnect" (NOT excluded)
        long probeDisconnectCount = clinicals.stream()
            .filter(f -> "Probe Disconnect".equalsIgnoreCase(f.alarmName))
            .count();
        
        assertTrue(probeDisconnectCount > 0, 
            "Probe Disconnect should be processed because it's NOT in the exclusion list");

        System.out.println("✅ NOT_IN Exclusion Test PASSED");
        System.out.println("   Code Blue flows: " + codeBlueCount + " (expected: 0)");
        System.out.println("   Probe Disconnect flows: " + probeDisconnectCount + " (expected: >0)");
    }

    @Test
    public void testSendRuleProcessedWhenAlertIncludedByIn() throws Exception {
        // Create XML with:
        // 1. DataUpdate CREATE rule that includes ONLY "Probe Disconnect" via IN
        // 2. VMP SEND rule for "Probe Disconnect" (should be processed)
        // 3. VMP SEND rule for "Code Blue" (should NOT be processed - not in CREATE rule)
        
        String xml = """
<?xml version="1.0" encoding="UTF-8"?>
<engage>
  <dataset active="true">
    <name>Clinicals</name>
    <view>
      <name>ProbeDisconnectOnly</name>
      <filter relation="in">
        <path>alert_type</path>
        <value>Probe Disconnect</value>
      </filter>
    </view>
    <view>
      <name>CodeBlueView</name>
      <filter relation="equal">
        <path>alert_type</path>
        <value>Code Blue</value>
      </filter>
    </view>
  </dataset>
  
  <interface component="DataUpdate">
    <rule active="true" dataset="Clinicals">
      <purpose>Create only Probe Disconnect alerts</purpose>
      <trigger-on create="true" update="false"/>
      <condition>
        <view>ProbeDisconnectOnly</view>
      </condition>
      <settings>{"state": "Primary"}</settings>
    </rule>
  </interface>
  
  <interface component="VMP">
    <rule active="true" dataset="Clinicals">
      <purpose>Send Probe Disconnect to Nurse (should be processed)</purpose>
      <trigger-on create="false" update="true"/>
      <condition>
        <view>ProbeDisconnectOnly</view>
      </condition>
      <settings>{"destination": "Nurse"}</settings>
    </rule>
  </interface>
  
  <interface component="VMP">
    <rule active="true" dataset="Clinicals">
      <purpose>Send Code Blue to Nurse (should NOT be processed)</purpose>
      <trigger-on create="false" update="true"/>
      <condition>
        <view>CodeBlueView</view>
      </condition>
      <settings>{"destination": "Nurse"}</settings>
    </rule>
  </interface>
</engage>
""";

        File xmlFile = tempDir.resolve("test.xml").toFile();
        Files.writeString(xmlFile.toPath(), xml);

        XmlParser parser = new XmlParser();
        parser.load(xmlFile);

        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();

        // Verify: Should have flows for "Probe Disconnect" (included by IN)
        long probeDisconnectCount = clinicals.stream()
            .filter(f -> "Probe Disconnect".equalsIgnoreCase(f.alarmName))
            .count();
        
        assertTrue(probeDisconnectCount > 0, 
            "Probe Disconnect should be processed because it's included by IN in DataUpdate CREATE rule");

        // Verify: Should have NO flows for "Code Blue" (not in DataUpdate CREATE rule)
        long codeBlueCount = clinicals.stream()
            .filter(f -> "Code Blue".equalsIgnoreCase(f.alarmName))
            .count();
        
        assertEquals(0, codeBlueCount, 
            "Code Blue should NOT be processed because it's not in the DataUpdate CREATE rule");

        System.out.println("✅ IN Inclusion Test PASSED");
        System.out.println("   Probe Disconnect flows: " + probeDisconnectCount + " (expected: >0)");
        System.out.println("   Code Blue flows: " + codeBlueCount + " (expected: 0)");
    }
}
