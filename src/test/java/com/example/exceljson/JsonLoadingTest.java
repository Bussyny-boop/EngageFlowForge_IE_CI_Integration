package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that JSON files can be loaded back into the application.
 */
class JsonLoadingTest {

    @Test
    void testLoadSimpleNurseCallJson(@TempDir Path tempDir) throws Exception {
        // Create a simple NurseCall JSON file
        File jsonFile = tempDir.resolve("test_nursecalls.json").toFile();
        
        String jsonContent = """
            {
              "version": "1.1.0",
              "alarmAlertDefinitions": [{
                "name": "Test Alarm",
                "type": "NurseCalls",
                "values": []
              }],
              "deliveryFlows": [{
                "name": "SEND NURSECALL | URGENT | Test Alarm | Group 1 | ",
                "priority": "urgent",
                "status": "Active",
                "alarmsAlerts": ["Test Alarm"],
                "interfaces": [{
                  "referenceName": "OutgoingWCTP",
                  "componentName": "OutgoingWCTP"
                }],
                "parameterAttributes": [{
                  "name": "breakThrough",
                  "value": "voceraAndDevice"
                }],
                "destinations": [],
                "units": []
              }]
            }
            """;
        
        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(jsonContent);
        }
        
        // Load the JSON file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.loadJson(jsonFile);
        
        // Verify that the flow was loaded
        assertEquals(1, parser.nurseCalls.size(), "Should load 1 nurse call flow");
        
        ExcelParserV5.FlowRow flow = parser.nurseCalls.get(0);
        assertEquals("Test Alarm", flow.alarmName);
        assertEquals("urgent", flow.priorityRaw.toLowerCase());
        assertEquals("Group 1", flow.configGroup);
    }

    @Test
    void testLoadCombinedJson(@TempDir Path tempDir) throws Exception {
        // Create a combined JSON file with both nurseCalls and clinicals
        File jsonFile = tempDir.resolve("test_combined.json").toFile();
        
        String jsonContent = """
            {
              "nurseCalls": {
                "version": "1.1.0",
                "alarmAlertDefinitions": [{
                  "name": "Nurse Alarm",
                  "type": "NurseCalls",
                  "values": []
                }],
                "deliveryFlows": [{
                  "name": "SEND NURSECALL | HIGH | Nurse Alarm | Group A | ",
                  "priority": "high",
                  "status": "Active",
                  "alarmsAlerts": ["Nurse Alarm"],
                  "interfaces": [],
                  "parameterAttributes": [],
                  "destinations": [],
                  "units": []
                }]
              },
              "clinicals": {
                "version": "1.1.0",
                "alarmAlertDefinitions": [{
                  "name": "Clinical Alarm",
                  "type": "Clinicals",
                  "values": []
                }],
                "deliveryFlows": [{
                  "name": "SEND CLINICAL | NORMAL | Clinical Alarm | Group B | ",
                  "priority": "normal",
                  "status": "Active",
                  "alarmsAlerts": ["Clinical Alarm"],
                  "interfaces": [],
                  "parameterAttributes": [],
                  "destinations": [],
                  "units": []
                }]
              }
            }
            """;
        
        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(jsonContent);
        }
        
        // Load the JSON file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.loadJson(jsonFile);
        
        // Verify both flows were loaded
        assertEquals(1, parser.nurseCalls.size(), "Should load 1 nurse call flow");
        assertEquals(1, parser.clinicals.size(), "Should load 1 clinical flow");
        
        assertEquals("Nurse Alarm", parser.nurseCalls.get(0).alarmName);
        assertEquals("Clinical Alarm", parser.clinicals.get(0).alarmName);
    }

    @Test
    void testLoadJsonWithParameterAttributes(@TempDir Path tempDir) throws Exception {
        // Create a JSON with parameter attributes
        File jsonFile = tempDir.resolve("test_params.json").toFile();
        
        String jsonContent = """
            {
              "version": "1.1.0",
              "alarmAlertDefinitions": [{
                "name": "Param Test",
                "type": "NurseCalls",
                "values": []
              }],
              "deliveryFlows": [{
                "name": "Test Flow",
                "priority": "urgent",
                "status": "Active",
                "alarmsAlerts": ["Param Test"],
                "interfaces": [],
                "parameterAttributes": [
                  {"name": "breakThrough", "value": "voceraOnly"},
                  {"name": "enunciate", "value": "true"},
                  {"name": "alertSound", "value": "Chime"},
                  {"name": "responseType", "value": "Accept/Decline"},
                  {"name": "ttl", "value": "300"}
                ],
                "destinations": [],
                "units": []
              }]
            }
            """;
        
        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(jsonContent);
        }
        
        // Load the JSON file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.loadJson(jsonFile);
        
        // Verify parameter attributes were parsed
        assertEquals(1, parser.nurseCalls.size());
        ExcelParserV5.FlowRow flow = parser.nurseCalls.get(0);
        
        // Reverse-mapped values from JSON to GUI-friendly forms
        assertEquals("Yes", flow.breakThroughDND);
        assertEquals("Yes", flow.enunciate);
        assertEquals("Chime", flow.ringtone);
        assertEquals("Accept", flow.responseOptions);
        assertEquals("300", flow.ttlValue);
    }
    
    @Test
    void testRoundTrip(@TempDir Path tempDir) throws Exception {
        // Create a parser with some data
        ExcelParserV5 parser1 = new ExcelParserV5();
        
        ExcelParserV5.FlowRow flow = new ExcelParserV5.FlowRow();
        flow.type = "NurseCalls";
        flow.alarmName = "Round Trip Test";
        flow.configGroup = "Test Group";
        flow.priorityRaw = "urgent";
        flow.deviceA = "OutgoingWCTP";
        flow.breakThroughDND = "voceraAndDevice";
        flow.enunciate = "true";
        flow.ringtone = "Alert";
        
        parser1.nurseCalls.add(flow);
        
        // Export to JSON
        File jsonFile = tempDir.resolve("roundtrip.json").toFile();
        parser1.writeNurseCallsJson(jsonFile);
        
        // Load it back
        ExcelParserV5 parser2 = new ExcelParserV5();
        parser2.loadJson(jsonFile);
        
        // Verify the data survived the round trip
        assertEquals(1, parser2.nurseCalls.size());
        ExcelParserV5.FlowRow loadedFlow = parser2.nurseCalls.get(0);
        
        assertEquals("Round Trip Test", loadedFlow.alarmName);
        assertEquals("urgent", loadedFlow.priorityRaw.toLowerCase());
        assertNotNull(loadedFlow.breakThroughDND);
    }
}
