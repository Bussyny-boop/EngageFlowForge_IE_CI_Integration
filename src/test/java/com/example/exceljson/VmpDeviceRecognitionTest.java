package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify VMP is recognized as a VCS/VMP device for badgeAlertSound handling
 */
class VmpDeviceRecognitionTest {

    @Test
    void testVmpDeviceUsesBadgeAlertSound(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("test.xlsx").toFile();
        
        // Create a minimal Excel file using the ExcelParserV5 writeExcel method
        ExcelParserV5 parser = new ExcelParserV5();
        
        // Create a flow with VMP device and ringtone
        ExcelParserV5.FlowRow flow = new ExcelParserV5.FlowRow();
        flow.type = "NurseCalls";
        flow.inScope = true;
        flow.configGroup = "TestGroup";
        flow.alarmName = "Test Alarm";
        flow.priorityRaw = "Urgent";
        flow.deviceA = "VMP";  // VMP device
        flow.ringtone = "Chime";
        flow.r1 = "Test Recipient";
        
        parser.nurseCalls.add(flow);
        
        // Add a unit
        ExcelParserV5.UnitRow unit = new ExcelParserV5.UnitRow();
        unit.facility = "TestFacility";
        unit.unitNames = "TestUnit";
        unit.nurseGroup = "TestGroup";
        parser.units.add(unit);
        
        // Generate JSON
        var json = parser.buildNurseCallsJson();
        
        // Verify the JSON structure
        assertNotNull(json);
        assertTrue(json.containsKey("deliveryFlows"));
        
        @SuppressWarnings("unchecked")
        var flows = (java.util.List<java.util.Map<String, Object>>) json.get("deliveryFlows");
        assertFalse(flows.isEmpty(), "Should have at least one delivery flow");
        
        var firstFlow = flows.get(0);
        
        // Check parameter attributes for badgeAlertSound
        assertTrue(firstFlow.containsKey("parameterAttributes"));
        
        @SuppressWarnings("unchecked")
        var params = (java.util.List<java.util.Map<String, Object>>) firstFlow.get("parameterAttributes");
        
        // Find badgeAlertSound parameter
        boolean hasBadgeAlertSound = params.stream()
            .anyMatch(p -> "badgeAlertSound".equals(p.get("name")));
        
        assertTrue(hasBadgeAlertSound, 
            "VMP device should use badgeAlertSound parameter");
        
        // Find the badgeAlertSound value
        var badgeAlert = params.stream()
            .filter(p -> "badgeAlertSound".equals(p.get("name")))
            .findFirst()
            .orElseThrow();
        
        String value = (String) badgeAlert.get("value");
        assertTrue(value.contains("Chime.wav"), 
            "badgeAlertSound should have .wav extension: " + value);
        
        System.out.println("✅ VMP device recognition test PASSED");
        System.out.println("   Device: VMP");
        System.out.println("   Uses: badgeAlertSound");
        System.out.println("   Value: " + value);
    }

    @Test
    void testVcsDeviceUsesBadgeAlertSound(@TempDir Path tempDir) throws Exception {
        ExcelParserV5 parser = new ExcelParserV5();
        
        // Test with "VCS" device
        ExcelParserV5.FlowRow flow = new ExcelParserV5.FlowRow();
        flow.type = "NurseCalls";
        flow.inScope = true;
        flow.configGroup = "TestGroup";
        flow.alarmName = "Test Alarm";
        flow.priorityRaw = "Urgent";
        flow.deviceA = "VCS";  // VCS device
        flow.ringtone = "Alert";
        flow.r1 = "Test Recipient";
        
        parser.nurseCalls.add(flow);
        
        ExcelParserV5.UnitRow unit = new ExcelParserV5.UnitRow();
        unit.facility = "TestFacility";
        unit.unitNames = "TestUnit";
        unit.nurseGroup = "TestGroup";
        parser.units.add(unit);
        
        var json = parser.buildNurseCallsJson();
        
        @SuppressWarnings("unchecked")
        var flows = (java.util.List<java.util.Map<String, Object>>) json.get("deliveryFlows");
        var firstFlow = flows.get(0);
        
        @SuppressWarnings("unchecked")
        var params = (java.util.List<java.util.Map<String, Object>>) firstFlow.get("parameterAttributes");
        
        boolean hasBadgeAlertSound = params.stream()
            .anyMatch(p -> "badgeAlertSound".equals(p.get("name")));
        
        assertTrue(hasBadgeAlertSound, 
            "VCS device should use badgeAlertSound parameter");
        
        System.out.println("✅ VCS device recognition test PASSED");
        System.out.println("   Device: VCS");
        System.out.println("   Uses: badgeAlertSound");
    }

    @Test
    void testVoceraVcsDeviceUsesBadgeAlertSound(@TempDir Path tempDir) throws Exception {
        ExcelParserV5 parser = new ExcelParserV5();
        
        // Test with "Vocera VCS" device
        ExcelParserV5.FlowRow flow = new ExcelParserV5.FlowRow();
        flow.type = "NurseCalls";
        flow.inScope = true;
        flow.configGroup = "TestGroup";
        flow.alarmName = "Test Alarm";
        flow.priorityRaw = "Urgent";
        flow.deviceA = "Vocera VCS";  // Vocera VCS device
        flow.ringtone = "Emergency";
        flow.r1 = "Test Recipient";
        
        parser.nurseCalls.add(flow);
        
        ExcelParserV5.UnitRow unit = new ExcelParserV5.UnitRow();
        unit.facility = "TestFacility";
        unit.unitNames = "TestUnit";
        unit.nurseGroup = "TestGroup";
        parser.units.add(unit);
        
        var json = parser.buildNurseCallsJson();
        
        @SuppressWarnings("unchecked")
        var flows = (java.util.List<java.util.Map<String, Object>>) json.get("deliveryFlows");
        var firstFlow = flows.get(0);
        
        @SuppressWarnings("unchecked")
        var params = (java.util.List<java.util.Map<String, Object>>) firstFlow.get("parameterAttributes");
        
        boolean hasBadgeAlertSound = params.stream()
            .anyMatch(p -> "badgeAlertSound".equals(p.get("name")));
        
        assertTrue(hasBadgeAlertSound, 
            "Vocera VCS device should use badgeAlertSound parameter");
        
        System.out.println("✅ Vocera VCS device recognition test PASSED");
        System.out.println("   Device: Vocera VCS");
        System.out.println("   Uses: badgeAlertSound");
    }

    @Test
    void testVoceraDeviceDoesNotUseBadgeAlertSoundAlone(@TempDir Path tempDir) throws Exception {
        ExcelParserV5 parser = new ExcelParserV5();
        
        // Test with plain "Vocera" device (not VCS, not VMP)
        ExcelParserV5.FlowRow flow = new ExcelParserV5.FlowRow();
        flow.type = "NurseCalls";
        flow.inScope = true;
        flow.configGroup = "TestGroup";
        flow.alarmName = "Test Alarm";
        flow.priorityRaw = "Urgent";
        flow.deviceA = "Vocera";  // Plain Vocera (should use alertSound + badgeAlertSound)
        flow.ringtone = "Beep";
        flow.r1 = "Test Recipient";
        
        parser.nurseCalls.add(flow);
        
        ExcelParserV5.UnitRow unit = new ExcelParserV5.UnitRow();
        unit.facility = "TestFacility";
        unit.unitNames = "TestUnit";
        unit.nurseGroup = "TestGroup";
        parser.units.add(unit);
        
        var json = parser.buildNurseCallsJson();
        
        @SuppressWarnings("unchecked")
        var flows = (java.util.List<java.util.Map<String, Object>>) json.get("deliveryFlows");
        var firstFlow = flows.get(0);
        
        @SuppressWarnings("unchecked")
        var params = (java.util.List<java.util.Map<String, Object>>) firstFlow.get("parameterAttributes");
        
        // Plain Vocera should have BOTH alertSound and badgeAlertSound
        boolean hasAlertSound = params.stream()
            .anyMatch(p -> "alertSound".equals(p.get("name")));
        boolean hasBadgeAlertSound = params.stream()
            .anyMatch(p -> "badgeAlertSound".equals(p.get("name")));
        
        assertTrue(hasAlertSound, 
            "Plain Vocera device should have alertSound parameter");
        assertTrue(hasBadgeAlertSound, 
            "Plain Vocera device should also have badgeAlertSound parameter");
        
        System.out.println("✅ Plain Vocera device test PASSED");
        System.out.println("   Device: Vocera");
        System.out.println("   Uses: alertSound + badgeAlertSound (both)");
    }

    @Test
    void testAllVmpVariantsRecognized() throws Exception {
        ExcelParserV5 parser = new ExcelParserV5();
        
        String[] vmpVariants = {"VMP", "vmp", "Vmp", "VMP Interface", "My VMP Device"};
        
        for (String deviceName : vmpVariants) {
            ExcelParserV5.FlowRow flow = new ExcelParserV5.FlowRow();
            flow.type = "NurseCalls";
            flow.inScope = true;
            flow.configGroup = "TestGroup";
            flow.alarmName = "Test " + deviceName;
            flow.priorityRaw = "Urgent";
            flow.deviceA = deviceName;
            flow.ringtone = "Test";
            flow.r1 = "Recipient";
            
            parser.nurseCalls.clear();
            parser.nurseCalls.add(flow);
            
            ExcelParserV5.UnitRow unit = new ExcelParserV5.UnitRow();
            unit.facility = "TestFacility";
            unit.unitNames = "TestUnit";
            unit.nurseGroup = "TestGroup";
            parser.units.clear();
            parser.units.add(unit);
            
            var json = parser.buildNurseCallsJson();
            
            @SuppressWarnings("unchecked")
            var flows = (java.util.List<java.util.Map<String, Object>>) json.get("deliveryFlows");
            var firstFlow = flows.get(0);
            
            @SuppressWarnings("unchecked")
            var params = (java.util.List<java.util.Map<String, Object>>) firstFlow.get("parameterAttributes");
            
            boolean hasBadgeAlertSound = params.stream()
                .anyMatch(p -> "badgeAlertSound".equals(p.get("name")));
            
            assertTrue(hasBadgeAlertSound, 
                "Device '" + deviceName + "' should be recognized as VMP and use badgeAlertSound");
        }
        
        System.out.println("✅ All VMP variants recognition test PASSED");
        System.out.println("   Tested variants: " + String.join(", ", vmpVariants));
        System.out.println("   All recognized correctly!");
    }
}
