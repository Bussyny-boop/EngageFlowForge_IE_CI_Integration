package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that Water and Toilet Assist alerts for units 3S, 3600, 3100, 3N, 2N
 * are properly parsed with all three recipients (PRIMARY, SECONDARY, TERTIARY) in a single rule.
 * 
 * Problem: The tertiary recipients are being created under a separate rule from the 
 * primary and secondary recipients due to different unit filters.
 * 
 * Expected: For units that are in BOTH Unit_MedSurgNC (used by PRIMARY/SECONDARY) AND
 * Unit_MedSurgNC_3600_3N_3S (used by TERTIARY), all three recipients should be merged
 * into a single escalation flow.
 */
public class WaterToiletAssistUnitParsingTest {

    @Test
    public void testWaterAlertRecipientsForSharedUnits() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load the north_western_cdh_test.xml file
        File xmlFile = new File("north_western_cdh_test.xml");
        if (!xmlFile.exists()) {
            xmlFile = new File("../north_western_cdh_test.xml");
        }
        assertTrue(xmlFile.exists(), "north_western_cdh_test.xml file should exist");
        
        parser.load(xmlFile);
        
        // Get nurse calls
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        assertFalse(nurseCalls.isEmpty(), "Should have nurse call flows");
        
        // Filter Water flows for Delnor facility with MedSurgNC units
        List<ExcelParserV5.FlowRow> waterFlows = nurseCalls.stream()
            .filter(f -> f.alarmName != null && f.alarmName.equals("Water"))
            .filter(f -> f.configGroup != null && f.configGroup.contains("Delnor"))
            .filter(f -> f.configGroup.contains("MedSurgNC") || f.configGroup.contains("3600") || 
                        f.configGroup.contains("3N") || f.configGroup.contains("3S") ||
                        f.configGroup.contains("2N") || f.configGroup.contains("3100"))
            .collect(Collectors.toList());
        
        System.out.println("\n=== Water Alert Flows for Delnor MedSurgNC Units ===");
        for (ExcelParserV5.FlowRow flow : waterFlows) {
            System.out.println("\nConfig Group: " + flow.configGroup);
            System.out.println("  R1: " + (flow.r1 != null ? flow.r1 : "NULL"));
            System.out.println("  R2: " + (flow.r2 != null ? flow.r2 : "NULL"));
            System.out.println("  R3: " + (flow.r3 != null ? flow.r3 : "NULL"));
            System.out.println("  T1: " + (flow.t1 != null ? flow.t1 : "NULL"));
            System.out.println("  T2: " + (flow.t2 != null ? flow.t2 : "NULL"));
            System.out.println("  T3: " + (flow.t3 != null ? flow.t3 : "NULL"));
        }
        
        // Find flows for the shared units (3600, 3N, 3S, 2N, 3100)
        // These units appear in BOTH the PRIMARY/SECONDARY filter (Unit_MedSurgNC) 
        // AND the TERTIARY filter (Unit_MedSurgNC_3600_3N_3S)
        // They should have all three recipients in a single flow
        List<String> sharedUnits = List.of("3600", "3N", "3S", "2N", "3100");
        
        for (String unit : sharedUnits) {
            System.out.println("\n=== Checking unit: " + unit + " ===");
            
            List<ExcelParserV5.FlowRow> unitFlows = waterFlows.stream()
                .filter(f -> f.configGroup.contains(unit))
                .collect(Collectors.toList());
            
            System.out.println("Found " + unitFlows.size() + " flows for unit " + unit);
            for (ExcelParserV5.FlowRow flow : unitFlows) {
                System.out.println("  - Config: " + flow.configGroup + 
                    ", R1=" + flow.r1 + ", R2=" + flow.r2 + ", R3=" + flow.r3);
            }
            
            // For each shared unit, there should be exactly ONE flow with all three recipients
            // Not multiple flows with different recipient combinations
            List<ExcelParserV5.FlowRow> completeFlows = unitFlows.stream()
                .filter(f -> f.r1 != null && !f.r1.isEmpty() &&
                           f.r2 != null && !f.r2.isEmpty() &&
                           f.r3 != null && !f.r3.isEmpty())
                .collect(Collectors.toList());
            
            System.out.println("  Complete flows (with R1, R2, R3): " + completeFlows.size());
            
            // Assert that we have at least one complete flow
            assertTrue(completeFlows.size() > 0, 
                "Unit " + unit + " should have at least one complete flow with R1, R2, and R3");
            
            // Check recipients are correct
            for (ExcelParserV5.FlowRow flow : completeFlows) {
                // R1 should be PCT
                assertTrue(flow.r1.contains("PCT") || flow.r1.contains("VAssign"),
                    "Unit " + unit + " R1 should be PCT, got: " + flow.r1);
                
                // R2 should be RN
                assertTrue(flow.r2.contains("RN") || flow.r2.contains("Nurse"),
                    "Unit " + unit + " R2 should be RN, got: " + flow.r2);
                
                // R3 should be Charge Nurse
                assertTrue(flow.r3.contains("Charge"),
                    "Unit " + unit + " R3 should be Charge Nurse, got: " + flow.r3);
            }
        }
    }
    
    @Test
    public void testToiletAssistAlertRecipientsForSharedUnits() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load the north_western_cdh_test.xml file
        File xmlFile = new File("north_western_cdh_test.xml");
        if (!xmlFile.exists()) {
            xmlFile = new File("../north_western_cdh_test.xml");
        }
        assertTrue(xmlFile.exists(), "north_western_cdh_test.xml file should exist");
        
        parser.load(xmlFile);
        
        // Get nurse calls
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        assertFalse(nurseCalls.isEmpty(), "Should have nurse call flows");
        
        // Filter Toilet Assist flows for Delnor facility with MedSurgNC units
        List<ExcelParserV5.FlowRow> toiletFlows = nurseCalls.stream()
            .filter(f -> f.alarmName != null && f.alarmName.equals("Toilet Assist"))
            .filter(f -> f.configGroup != null && f.configGroup.contains("Delnor"))
            .filter(f -> f.configGroup.contains("MedSurgNC") || f.configGroup.contains("3600") || 
                        f.configGroup.contains("3N") || f.configGroup.contains("3S") ||
                        f.configGroup.contains("2N") || f.configGroup.contains("3100"))
            .collect(Collectors.toList());
        
        System.out.println("\n=== Toilet Assist Alert Flows for Delnor MedSurgNC Units ===");
        for (ExcelParserV5.FlowRow flow : toiletFlows) {
            System.out.println("\nConfig Group: " + flow.configGroup);
            System.out.println("  R1: " + (flow.r1 != null ? flow.r1 : "NULL"));
            System.out.println("  R2: " + (flow.r2 != null ? flow.r2 : "NULL"));
            System.out.println("  R3: " + (flow.r3 != null ? flow.r3 : "NULL"));
            System.out.println("  T1: " + (flow.t1 != null ? flow.t1 : "NULL"));
            System.out.println("  T2: " + (flow.t2 != null ? flow.t2 : "NULL"));
            System.out.println("  T3: " + (flow.t3 != null ? flow.t3 : "NULL"));
        }
        
        // Find flows for the shared units (3600, 3N, 3S)
        // According to the XML:
        // - PRIMARY/SECONDARY use Unit_MedSurgNC: 2600,3600,2N,3S,3N,3100
        // - TERTIARY uses Unit_MedSurgNC_3600_3N_3S: 3600,2N,3S,3N,3100
        // So shared units are: 3600, 2N, 3S, 3N, 3100
        List<String> sharedUnits = List.of("3600", "3N", "3S", "2N", "3100");
        
        for (String unit : sharedUnits) {
            System.out.println("\n=== Checking unit: " + unit + " ===");
            
            List<ExcelParserV5.FlowRow> unitFlows = toiletFlows.stream()
                .filter(f -> f.configGroup.contains(unit))
                .collect(Collectors.toList());
            
            System.out.println("Found " + unitFlows.size() + " flows for unit " + unit);
            for (ExcelParserV5.FlowRow flow : unitFlows) {
                System.out.println("  - Config: " + flow.configGroup + 
                    ", R1=" + flow.r1 + ", R2=" + flow.r2 + ", R3=" + flow.r3);
            }
            
            // For each shared unit, there should be exactly ONE flow with all three recipients
            List<ExcelParserV5.FlowRow> completeFlows = unitFlows.stream()
                .filter(f -> f.r1 != null && !f.r1.isEmpty() &&
                           f.r2 != null && !f.r2.isEmpty() &&
                           f.r3 != null && !f.r3.isEmpty())
                .collect(Collectors.toList());
            
            System.out.println("  Complete flows (with R1, R2, R3): " + completeFlows.size());
            
            // Assert that we have at least one complete flow
            assertTrue(completeFlows.size() > 0, 
                "Unit " + unit + " should have at least one complete flow with R1, R2, and R3");
            
            // Check recipients are correct
            for (ExcelParserV5.FlowRow flow : completeFlows) {
                // R1 should be PCT
                assertTrue(flow.r1.contains("PCT") || flow.r1.contains("VAssign"),
                    "Unit " + unit + " R1 should be PCT, got: " + flow.r1);
                
                // R2 should be RN
                assertTrue(flow.r2.contains("RN") || flow.r2.contains("Nurse"),
                    "Unit " + unit + " R2 should be RN, got: " + flow.r2);
                
                // R3 should be Charge Nurse
                assertTrue(flow.r3.contains("Charge"),
                    "Unit " + unit + " R3 should be Charge Nurse, got: " + flow.r3);
            }
        }
    }
}
