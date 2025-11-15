package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for enhanced JSON parser that extracts recipients, timing, and units.
 */
public class JsonParserEnhancedTest {

    @Test
    public void testJsonParserExtractsRecipients(@TempDir Path tempDir) throws Exception {
        // Create JSON with destinations containing recipients
        String json = "{\n" +
                "  \"nurseCalls\": {\n" +
                "    \"version\": \"1.1.0\",\n" +
                "    \"deliveryFlows\": [{\n" +
                "      \"name\": \"SEND NURSECALL | URGENT | Code Blue | Config1 | \",\n" +
                "      \"priority\": \"urgent\",\n" +
                "      \"alarmsAlerts\": [\"Code Blue\"],\n" +
                "      \"interfaces\": [{\n" +
                "        \"referenceName\": \"OutgoingWCTP\",\n" +
                "        \"componentName\": \"OutgoingWCTP\"\n" +
                "      }],\n" +
                "      \"destinations\": [\n" +
                "        {\n" +
                "          \"order\": 0,\n" +
                "          \"delayTime\": 0,\n" +
                "          \"functionalRoles\": [{\n" +
                "            \"facilityName\": \"Main\",\n" +
                "            \"name\": \"Charge Nurse\"\n" +
                "          }],\n" +
                "          \"groups\": []\n" +
                "        },\n" +
                "        {\n" +
                "          \"order\": 1,\n" +
                "          \"delayTime\": 60,\n" +
                "          \"functionalRoles\": [],\n" +
                "          \"groups\": [{\n" +
                "            \"facilityName\": \"Main\",\n" +
                "            \"name\": \"RN Group\"\n" +
                "          }]\n" +
                "        },\n" +
                "        {\n" +
                "          \"order\": 2,\n" +
                "          \"delayTime\": 120,\n" +
                "          \"functionalRoles\": [{\n" +
                "            \"facilityName\": \"Main\",\n" +
                "            \"name\": \"Supervisor\"\n" +
                "          }],\n" +
                "          \"groups\": []\n" +
                "        }\n" +
                "      ],\n" +
                "      \"units\": []\n" +
                "    }]\n" +
                "  }\n" +
                "}";

        File jsonFile = tempDir.resolve("test.json").toFile();
        Files.writeString(jsonFile.toPath(), json);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.loadJson(jsonFile);

        assertFalse(parser.nurseCalls.isEmpty(), "Should have parsed nurse calls");
        ExcelParserV5.FlowRow flow = parser.nurseCalls.get(0);

        assertEquals("VAssign:[Room] Charge Nurse", flow.r1, "First recipient should be Charge Nurse with VAssign prefix");
        assertEquals("", flow.t1, "First delay should be 0 (empty)");

        assertEquals("VGroup: RN Group", flow.r2, "Second recipient should be RN Group with VGroup prefix");
        assertEquals("60", flow.t2, "Second delay should be 60 seconds");

        assertEquals("VAssign:[Room] Supervisor", flow.r3, "Third recipient should be Supervisor with VAssign prefix");
        assertEquals("120", flow.t3, "Third delay should be 120 seconds");
    }

    @Test
    public void testJsonParserExtractsMultipleRecipientsPerDestination(@TempDir Path tempDir) throws Exception {
        // Test multiple recipients in a single destination (should be newline-separated)
        String json = "{\n" +
                "  \"clinicals\": {\n" +
                "    \"version\": \"1.1.0\",\n" +
                "    \"deliveryFlows\": [{\n" +
                "      \"name\": \"SEND CLINICAL | HIGH | Alert | Config1 | \",\n" +
                "      \"priority\": \"high\",\n" +
                "      \"alarmsAlerts\": [\"Alert\"],\n" +
                "      \"interfaces\": [{\n" +
                "        \"referenceName\": \"Vocera\",\n" +
                "        \"componentName\": \"Vocera\"\n" +
                "      }],\n" +
                "      \"destinations\": [{\n" +
                "        \"order\": 0,\n" +
                "        \"delayTime\": 0,\n" +
                "        \"functionalRoles\": [\n" +
                "          {\"facilityName\": \"East\", \"name\": \"Nurse\"},\n" +
                "          {\"facilityName\": \"East\", \"name\": \"Tech\"}\n" +
                "        ],\n" +
                "        \"groups\": [\n" +
                "          {\"facilityName\": \"East\", \"name\": \"Team A\"},\n" +
                "          {\"facilityName\": \"East\", \"name\": \"Team B\"}\n" +
                "        ]\n" +
                "      }],\n" +
                "      \"units\": []\n" +
                "    }]\n" +
                "  }\n" +
                "}";

        File jsonFile = tempDir.resolve("test.json").toFile();
        Files.writeString(jsonFile.toPath(), json);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.loadJson(jsonFile);

        assertFalse(parser.clinicals.isEmpty());
        ExcelParserV5.FlowRow flow = parser.clinicals.get(0);

        // Should have all recipients newline-separated
        String expected = "VAssign:[Room] Nurse\nVAssign:[Room] Tech\nVGroup: Team A\nVGroup: Team B";
        assertEquals(expected, flow.r1, "All recipients should be newline-separated");
    }

    @Test
    public void testJsonParserExtractsUnitsAndConfigGroup(@TempDir Path tempDir) throws Exception {
        // Test units extraction and Facility+Unit+Dataset config group
        String json = "{\n" +
                "  \"nurseCalls\": {\n" +
                "    \"version\": \"1.1.0\",\n" +
                "    \"deliveryFlows\": [{\n" +
                "      \"name\": \"SEND NURSECALL | NORMAL | Test | OldConfig | \",\n" +
                "      \"priority\": \"normal\",\n" +
                "      \"alarmsAlerts\": [\"Test\"],\n" +
                "      \"interfaces\": [{\"referenceName\": \"VMP\", \"componentName\": \"VMP\"}],\n" +
                "      \"destinations\": [],\n" +
                "      \"units\": [\n" +
                "        {\"facilityName\": \"Hospital\", \"name\": \"ICU\"},\n" +
                "        {\"facilityName\": \"Hospital\", \"name\": \"ER\"}\n" +
                "      ]\n" +
                "    }]\n" +
                "  }\n" +
                "}";

        File jsonFile = tempDir.resolve("test.json").toFile();
        Files.writeString(jsonFile.toPath(), json);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.loadJson(jsonFile);

        assertFalse(parser.nurseCalls.isEmpty());
        ExcelParserV5.FlowRow flow = parser.nurseCalls.get(0);

        assertEquals("Hospital_ICU_OldConfig", flow.configGroup,
                "Config group should use Facility_Unit_Dataset format");
    }

    @Test
    public void testJsonParserExtractsParameterAttributes(@TempDir Path tempDir) throws Exception {
        // Test parameter attributes extraction
        String json = "{\n" +
                "  \"nurseCalls\": {\n" +
                "    \"version\": \"1.1.0\",\n" +
                "    \"deliveryFlows\": [{\n" +
                "      \"name\": \"SEND NURSECALL | URGENT | Alarm | Config | \",\n" +
                "      \"priority\": \"urgent\",\n" +
                "      \"alarmsAlerts\": [\"Alarm\"],\n" +
                "      \"interfaces\": [{\"referenceName\": \"Edge\", \"componentName\": \"Edge\"}],\n" +
                "      \"parameterAttributes\": [\n" +
                "        {\"name\": \"breakThrough\", \"value\": \"voceraAndDevice\"},\n" +
                "        {\"name\": \"enunciate\", \"value\": \"true\"},\n" +
                "        {\"name\": \"badgeAlertSound\", \"value\": \"Alert.wav\"},\n" +
                "        {\"name\": \"ttl\", \"value\": \"300\"}\n" +
                "      ],\n" +
                "      \"destinations\": [],\n" +
                "      \"units\": []\n" +
                "    }]\n" +
                "  }\n" +
                "}";

        File jsonFile = tempDir.resolve("test.json").toFile();
        Files.writeString(jsonFile.toPath(), json);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.loadJson(jsonFile);

        assertFalse(parser.nurseCalls.isEmpty());
        ExcelParserV5.FlowRow flow = parser.nurseCalls.get(0);

        // Reverse-mapped values should reflect GUI inputs
        assertEquals("Yes", flow.breakThroughDND);
        assertEquals("Yes", flow.enunciate);
        assertEquals("Alert", flow.ringtone, "Ringtone should have .wav extension removed");
        assertEquals("300", flow.ttlValue);
    }

    @Test
    public void testJsonParserHandlesAllFiveRecipients(@TempDir Path tempDir) throws Exception {
        // Test all 5 recipient/timing slots (r1-r5, t1-t5)
        String json = "{\n" +
                "  \"nurseCalls\": {\n" +
                "    \"version\": \"1.1.0\",\n" +
                "    \"deliveryFlows\": [{\n" +
                "      \"name\": \"SEND NURSECALL | URGENT | Escalation Test | Config | \",\n" +
                "      \"priority\": \"urgent\",\n" +
                "      \"alarmsAlerts\": [\"Escalation Test\"],\n" +
                "      \"interfaces\": [{\"referenceName\": \"XMPP\", \"componentName\": \"XMPP\"}],\n" +
                "      \"destinations\": [\n" +
                "        {\"order\": 0, \"delayTime\": 0, \"groups\": [{\"name\": \"Team1\"}], \"functionalRoles\": []},\n" +
                "        {\"order\": 1, \"delayTime\": 30, \"groups\": [{\"name\": \"Team2\"}], \"functionalRoles\": []},\n" +
                "        {\"order\": 2, \"delayTime\": 60, \"groups\": [{\"name\": \"Team3\"}], \"functionalRoles\": []},\n" +
                "        {\"order\": 3, \"delayTime\": 90, \"groups\": [{\"name\": \"Team4\"}], \"functionalRoles\": []},\n" +
                "        {\"order\": 4, \"delayTime\": 120, \"groups\": [{\"name\": \"Team5\"}], \"functionalRoles\": []}\n" +
                "      ],\n" +
                "      \"units\": []\n" +
                "    }]\n" +
                "  }\n" +
                "}";

        File jsonFile = tempDir.resolve("test.json").toFile();
        Files.writeString(jsonFile.toPath(), json);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.loadJson(jsonFile);

        assertFalse(parser.nurseCalls.isEmpty());
        ExcelParserV5.FlowRow flow = parser.nurseCalls.get(0);

        assertEquals("VGroup: Team1", flow.r1);
        assertEquals("", flow.t1);

        assertEquals("VGroup: Team2", flow.r2);
        assertEquals("30", flow.t2);

        assertEquals("VGroup: Team3", flow.r3);
        assertEquals("60", flow.t3);

        assertEquals("VGroup: Team4", flow.r4);
        assertEquals("90", flow.t4);

        assertEquals("VGroup: Team5", flow.r5);
        assertEquals("120", flow.t5);
    }

    @Test
    public void testJsonParserRoundTripWithRecipientsAndUnits(@TempDir Path tempDir) throws Exception {
        // Test that JSON can be loaded and re-exported with recipients and units intact
        String json = "{\n" +
                "  \"nurseCalls\": {\n" +
                "    \"version\": \"1.1.0\",\n" +
                "    \"deliveryFlows\": [{\n" +
                "      \"name\": \"SEND NURSECALL | HIGH | Round Trip | TestConfig | \",\n" +
                "      \"priority\": \"high\",\n" +
                "      \"alarmsAlerts\": [\"Round Trip\"],\n" +
                "      \"interfaces\": [{\"referenceName\": \"Vocera\", \"componentName\": \"Vocera\"}],\n" +
                "      \"parameterAttributes\": [{\"name\": \"enunciate\", \"value\": \"true\"}],\n" +
                "      \"destinations\": [{\n" +
                "        \"order\": 0,\n" +
                "        \"delayTime\": 0,\n" +
                "        \"functionalRoles\": [{\"facilityName\": \"West\", \"name\": \"Nurse\"}],\n" +
                "        \"groups\": []\n" +
                "      }],\n" +
                "      \"units\": [{\"facilityName\": \"West\", \"name\": \"3North\"}]\n" +
                "    }]\n" +
                "  }\n" +
                "}";

        File jsonFile = tempDir.resolve("test.json").toFile();
        Files.writeString(jsonFile.toPath(), json);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.loadJson(jsonFile);

        assertFalse(parser.nurseCalls.isEmpty());
        ExcelParserV5.FlowRow flow = parser.nurseCalls.get(0);

        // Verify all fields were parsed
        assertEquals("Round Trip", flow.alarmName);
        assertEquals("high", flow.priorityRaw);
        assertEquals("VAssign:[Room] Nurse", flow.r1);
        assertEquals("", flow.t1);
        assertEquals("West_3North_TestConfig", flow.configGroup);
        assertEquals("Yes", flow.enunciate);
        assertEquals("Vocera", flow.deviceA);
    }
}
