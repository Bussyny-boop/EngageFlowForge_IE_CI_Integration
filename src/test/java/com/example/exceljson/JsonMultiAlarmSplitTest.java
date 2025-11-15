package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JsonMultiAlarmSplitTest {

    @Test
    void splitsMultipleAlarmsIntoSeparateRows(@TempDir Path tempDir) throws Exception {
        // Minimal JSON with a single delivery flow that has two alarms
        String json = """
        {
          "version": "1.1.0",
          "alarmAlertDefinitions": [
            {"name": "Aux 1 Disconnect Call", "type": "NurseCalls", "values": []},
            {"name": "Bed Disconnect Call", "type": "NurseCalls", "values": []}
          ],
          "deliveryFlows": [
            {
              "alarmsAlerts": ["Aux 1 Disconnect Call", "Bed Disconnect Call"],
              "conditions": [],
              "destinations": [],
              "interfaces": [],
              "name": "SEND NURSECALL | NORMAL | Aux 1 Disconnect Call / Bed Disconnect Call | Acute Care NC | MedSurg",
              "parameterAttributes": [],
              "priority": "normal",
              "status": "Active",
              "units": [{"facilityName":"BCH","name":"MedSurg"}]
            }
          ]
        }
        """;

        File f = tempDir.resolve("multi_alarms.json").toFile();
        try (FileWriter w = new FileWriter(f)) { w.write(json); }

        ExcelParserV5 p = new ExcelParserV5();
        p.loadJson(f);

        assertEquals(2, p.nurseCalls.size(), "Expected one GUI row per alarm");
        Set<String> names = p.nurseCalls.stream().map(r -> r.alarmName).collect(Collectors.toSet());
        assertTrue(names.contains("Aux 1 Disconnect Call"));
        assertTrue(names.contains("Bed Disconnect Call"));
    }
}
