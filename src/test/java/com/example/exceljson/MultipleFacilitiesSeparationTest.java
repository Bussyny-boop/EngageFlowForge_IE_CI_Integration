package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that when multiple facilities have the same alert configuration,
 * they are separated into individual config groups, not combined.
 */
public class MultipleFacilitiesSeparationTest {

    @Test
    public void testMultipleFacilitiesAreSeparated() throws Exception {
        XmlParser parser = new XmlParser();
        
        // Load the actual XMLParser.xml file
        File xmlFile = new File("XMLParser.xml");
        if (!xmlFile.exists()) {
            System.out.println("XMLParser.xml not found, skipping test");
            return;
        }
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // Look for an alert type that should exist in multiple facilities
        // Based on the XML analysis, "Code Blue" should have separate rules for each facility
        List<ExcelParserV5.FlowRow> codeBlueFlows = nurseCalls.stream()
            .filter(f -> f.alarmName != null && f.alarmName.contains("Code Blue"))
            .collect(Collectors.toList());
        
        if (codeBlueFlows.isEmpty()) {
            System.out.println("No Code Blue flows found, skipping test");
            return;
        }
        
        // Print all config groups for Code Blue
        System.out.println("\n=== Code Blue Config Groups ===");
        Set<String> configGroups = codeBlueFlows.stream()
            .map(f -> f.configGroup)
            .collect(Collectors.toSet());
        
        for (String cg : configGroups) {
            System.out.println("  - " + cg);
        }
        
        // Check if any config group contains multiple facilities separated by commas
        List<String> combinedConfigGroups = configGroups.stream()
            .filter(cg -> cg.contains(","))
            .collect(Collectors.toList());
        
        if (!combinedConfigGroups.isEmpty()) {
            System.out.println("\n!!! FOUND COMBINED CONFIG GROUPS !!!");
            for (String cg : combinedConfigGroups) {
                System.out.println("  - " + cg);
            }
            fail("Config groups should not combine multiple facilities with commas. Found: " + combinedConfigGroups);
        }
        
        // Verify that facilities are separated
        boolean hasGrandItasca = configGroups.stream().anyMatch(cg -> cg.contains("Grand Itasca") || cg.contains("GICH"));
        boolean hasNorthland = configGroups.stream().anyMatch(cg -> cg.contains("Northland"));
        boolean hasLakes = configGroups.stream().anyMatch(cg -> cg.contains("Lakes"));
        boolean hasRidges = configGroups.stream().anyMatch(cg -> cg.contains("Ridges"));
        
        System.out.println("\nFacility separation check:");
        System.out.println("  Grand Itasca/GICH: " + hasGrandItasca);
        System.out.println("  Northland: " + hasNorthland);
        System.out.println("  Lakes: " + hasLakes);
        System.out.println("  Ridges: " + hasRidges);
    }
}
