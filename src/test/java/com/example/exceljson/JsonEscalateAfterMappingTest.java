package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonEscalateAfterMappingTest {

    @Test
    void defaultsToOneDeclineWhenResponseOptionsPresentButNoDeclineCount(@TempDir Path tempDir) throws Exception {
        // Build JSON programmatically to ensure strict compatibility with the simple parser
        java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("version", "1.1.0");
        java.util.List<java.util.Map<String,Object>> aad = new java.util.ArrayList<>();
        aad.add(new java.util.LinkedHashMap<>(java.util.Map.of("name","Normal Call","type","NurseCalls","values",java.util.List.of())));
        root.put("alarmAlertDefinitions", aad);
        java.util.List<java.util.Map<String,Object>> flows = new java.util.ArrayList<>();
        java.util.Map<String,Object> flow = new java.util.LinkedHashMap<>();
        flow.put("alarmsAlerts", java.util.List.of("Normal Call"));
        flow.put("conditions", java.util.List.of());
        flow.put("destinations", java.util.List.of());
        flow.put("interfaces", java.util.List.of());
        flow.put("name", "SEND NURSECALL | NORMAL | Normal Call | TestGroup | TestUnit");
        java.util.List<java.util.Map<String,Object>> params = new java.util.ArrayList<>();
        params.add(new java.util.LinkedHashMap<>(java.util.Map.of("name","responseType","value","\"Accept/Decline\"")));
        flow.put("parameterAttributes", params);
        flow.put("priority", "normal");
        flow.put("status", "Active");
        flow.put("units", java.util.List.of(new java.util.LinkedHashMap<>(java.util.Map.of("facilityName","BCH","name","MedSurg"))));
        flows.add(flow);
        root.put("deliveryFlows", flows);

        String json = ExcelParserV5.pretty(root);
        File f = tempDir.resolve("one_decline_default.json").toFile();
        try (FileWriter w = new FileWriter(f)) { w.write(json); }

        ExcelParserV5 p = new ExcelParserV5();
        p.loadJson(f);

        assertEquals(1, p.nurseCalls.size());
        assertEquals("1 decline", p.nurseCalls.getFirst().escalateAfter);
        assertEquals("Accept", p.nurseCalls.getFirst().responseOptions, "Response options should map from Accept/Decline");
    }

    @Test
    void setsAllDeclinesWhenDeclineCountAllRecipients(@TempDir Path tempDir) throws Exception {
        java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("version", "1.1.0");
        java.util.List<java.util.Map<String,Object>> aad = new java.util.ArrayList<>();
        aad.add(new java.util.LinkedHashMap<>(java.util.Map.of("name","Normal Call","type","NurseCalls","values",java.util.List.of())));
        root.put("alarmAlertDefinitions", aad);
        java.util.List<java.util.Map<String,Object>> flows = new java.util.ArrayList<>();
        java.util.Map<String,Object> flow = new java.util.LinkedHashMap<>();
        flow.put("alarmsAlerts", java.util.List.of("Normal Call"));
        flow.put("conditions", java.util.List.of());
        flow.put("destinations", java.util.List.of());
        flow.put("interfaces", java.util.List.of());
        flow.put("name", "SEND NURSECALL | NORMAL | Normal Call | TestGroup | TestUnit");
        java.util.List<java.util.Map<String,Object>> params = new java.util.ArrayList<>();
        params.add(new java.util.LinkedHashMap<>(java.util.Map.of("name","responseType","value","\"Accept/Decline\"")));
        params.add(new java.util.LinkedHashMap<>(java.util.Map.of("name","declineCount","value","\"All Recipients\"")));
        flow.put("parameterAttributes", params);
        flow.put("priority", "normal");
        flow.put("status", "Active");
        flow.put("units", java.util.List.of(new java.util.LinkedHashMap<>(java.util.Map.of("facilityName","BCH","name","MedSurg"))));
        flows.add(flow);
        root.put("deliveryFlows", flows);

        String json = ExcelParserV5.pretty(root);
        File f = tempDir.resolve("all_declines.json").toFile();
        try (FileWriter w = new FileWriter(f)) { w.write(json); }

        ExcelParserV5 p = new ExcelParserV5();
        p.loadJson(f);

        assertEquals(1, p.nurseCalls.size());
        assertEquals("All declines", p.nurseCalls.getFirst().escalateAfter);
    }
}
