package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test to verify that facility names are properly split
 * when they appear as comma-separated values in XML view filters.
 */
public class FacilitySplitVerificationTest {

    @Test
    public void testNoConfigGroupsContainCommas() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("XMLParser.xml");
        if (!xmlFile.exists()) {
            System.out.println("XMLParser.xml not found, skipping test");
            return;
        }
        
        parser.load(xmlFile);
        
        // Collect all config groups from all flow types
        Set<String> allConfigGroups = Set.of();
        List<ExcelParserV5.FlowRow> allFlows = List.of();
        
        // Collect from all flow types
        allFlows = new java.util.ArrayList<>();
        allFlows.addAll(parser.getNurseCalls());
        allFlows.addAll(parser.getClinicals());
        allFlows.addAll(parser.getOrders());
        
        allConfigGroups = allFlows.stream()
            .map(f -> f.configGroup)
            .filter(cg -> cg != null && !cg.isEmpty())
            .collect(Collectors.toSet());
        
        System.out.println("\n=== Config Group Analysis ===");
        System.out.println("Total flows: " + allFlows.size());
        System.out.println("Unique config groups: " + allConfigGroups.size());
        
        // Check for any config groups that contain commas (which would indicate combined facilities)
        List<String> groupsWithCommas = allConfigGroups.stream()
            .filter(cg -> cg.contains(","))
            .collect(Collectors.toList());
        
        if (!groupsWithCommas.isEmpty()) {
            System.out.println("\n❌ FOUND CONFIG GROUPS WITH COMMAS:");
            for (String cg : groupsWithCommas) {
                System.out.println("  - " + cg);
                // Also print which flows have this config group
                long count = allFlows.stream()
                    .filter(f -> cg.equals(f.configGroup))
                    .count();
                System.out.println("    (used by " + count + " flows)");
            }
            fail("Config groups should NOT contain commas. " +
                 "Each facility should have its own config group. " +
                 "Found " + groupsWithCommas.size() + " groups with commas.");
        }
        
        System.out.println("\n✅ All config groups are properly separated (no commas found)");
        
        // Additional check: Verify that known facilities from XMLParser.xml exist as separate groups
        boolean hasGrandItascaNurseCalls = allConfigGroups.stream()
            .anyMatch(cg -> cg.matches(".*Grand Itasca.*NurseCalls"));
        boolean hasNorthlandNurseCalls = allConfigGroups.stream()
            .anyMatch(cg -> cg.matches(".*Northland.*NurseCalls"));
        boolean hasLakesNurseCalls = allConfigGroups.stream()
            .anyMatch(cg -> cg.matches(".*Lakes.*NurseCalls"));
        boolean hasRidgesNurseCalls = allConfigGroups.stream()
            .anyMatch(cg -> cg.matches(".*Ridges.*NurseCalls"));
        
        System.out.println("\nFacility-specific NurseCalls config groups:");
        System.out.println("  Grand Itasca: " + (hasGrandItascaNurseCalls ? "✓" : "✗"));
        System.out.println("  Northland: " + (hasNorthlandNurseCalls ? "✓" : "✗"));
        System.out.println("  Lakes: " + (hasLakesNurseCalls ? "✓" : "✗"));
        System.out.println("  Ridges: " + (hasRidgesNurseCalls ? "✓" : "✗"));
        
        // At least some of these should exist
        assertTrue(hasGrandItascaNurseCalls || hasNorthlandNurseCalls || 
                  hasLakesNurseCalls || hasRidgesNurseCalls,
                  "Should have at least one facility-specific NurseCalls config group");
    }
    
    @Test
    public void testEachFacilityHasSeparateFlows() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("XMLParser.xml");
        if (!xmlFile.exists()) {
            System.out.println("XMLParser.xml not found, skipping test");
            return;
        }
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
        
        // Count flows per facility for a common alert type
        long grandItascaFlows = nurseCalls.stream()
            .filter(f -> f.configGroup != null && f.configGroup.contains("Grand Itasca"))
            .count();
        
        long northlandFlows = nurseCalls.stream()
            .filter(f -> f.configGroup != null && f.configGroup.contains("Northland"))
            .count();
        
        long lakesFlows = nurseCalls.stream()
            .filter(f -> f.configGroup != null && f.configGroup.contains("Lakes"))
            .count();
        
        long ridgesFlows = nurseCalls.stream()
            .filter(f -> f.configGroup != null && f.configGroup.contains("Ridges"))
            .count();
        
        System.out.println("\n=== Flow Count by Facility (NurseCalls) ===");
        System.out.println("  Grand Itasca: " + grandItascaFlows);
        System.out.println("  Northland: " + northlandFlows);
        System.out.println("  Lakes: " + lakesFlows);
        System.out.println("  Ridges: " + ridgesFlows);
        
        // Each facility should have at least some flows
        // (We know from the XML that they all have Code Blue rules)
        assertTrue(grandItascaFlows > 0, "Grand Itasca should have flows");
        assertTrue(northlandFlows > 0, "Northland should have flows");
        assertTrue(lakesFlows > 0, "Lakes should have flows");
        assertTrue(ridgesFlows > 0, "Ridges should have flows");
        
        System.out.println("\n✅ Each facility has separate flows");
    }
}
