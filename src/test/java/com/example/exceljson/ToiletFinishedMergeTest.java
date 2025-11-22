package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that Toilet Finished flows for CDH_3S_NurseCalls are properly merged
 * into a single row instead of being scattered across multiple rows.
 */
public class ToiletFinishedMergeTest {

    @Test
    public void testToiletFinishedMerge_CDH_3S() throws Exception {
        // Load the XML file
        File xmlFile = new File("north_western_cdh_test.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");

        XmlParser parser = new XmlParser();
        parser.load(xmlFile);

        List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();

        // Find all Toilet Finished flows for CDH_3S_NurseCalls
        List<ExcelParserV5.FlowRow> toiletFinishedFlows = nurseCalls.stream()
                .filter(flow -> flow.configGroup != null && flow.configGroup.contains("CDH_3S"))
                .filter(flow -> flow.alarmName != null && flow.alarmName.equals("Toilet Finished"))
                .toList();

        System.out.println("\n=== Toilet Finished flows for CDH_3S ===");
        System.out.println("Found " + toiletFinishedFlows.size() + " flows");

        for (int i = 0; i < toiletFinishedFlows.size(); i++) {
            ExcelParserV5.FlowRow flow = toiletFinishedFlows.get(i);
            System.out.println("\nFlow " + (i + 1) + ":");
            System.out.println("  Config Group: " + flow.configGroup);
            System.out.println("  Alarm Name: " + flow.alarmName);
            System.out.println("  Priority: " + flow.priorityRaw);
            System.out.println("  Response Options: " + flow.responseOptions);
            System.out.println("  R1: " + flow.r1);
            System.out.println("  T1: " + flow.t1);
            System.out.println("  R2: " + flow.r2);
            System.out.println("  T2: " + flow.t2);
            System.out.println("  R3: " + flow.r3);
            
            // Check if flows are identical
            if (i > 0) {
                ExcelParserV5.FlowRow prev = toiletFinishedFlows.get(i - 1);
                boolean identical = true;
                if (!equals(flow.r1, prev.r1)) {
                    System.out.println("  -> R1 differs from previous");
                    identical = false;
                }
                if (!equals(flow.r2, prev.r2)) {
                    System.out.println("  -> R2 differs from previous");
                    identical = false;
                }
                if (!equals(flow.t1, prev.t1)) {
                    System.out.println("  -> T1 differs from previous");
                    identical = false;
                }
                if (!equals(flow.t2, prev.t2)) {
                    System.out.println("  -> T2 differs from previous");
                    identical = false;
                }
                if (identical) {
                    System.out.println("  -> IDENTICAL to previous flow - should have been merged!");
                }
            }
        }

        // The test expectation: There should be ONLY ONE flow for Toilet Finished in CDH_3S_NurseCalls
        // with all recipients and timing consolidated
        assertEquals(1, toiletFinishedFlows.size(),
                "There should be exactly 1 consolidated flow for Toilet Finished in CDH_3S_NurseCalls, " +
                        "but found " + toiletFinishedFlows.size() + " flows. " +
                        "The data should be merged into a single row, not scattered across multiple rows.");

        // Verify the merged flow has all expected data
        ExcelParserV5.FlowRow flow = toiletFinishedFlows.getFirst();
        assertNotNull(flow.priorityRaw, "Priority should be set");
        assertNotNull(flow.responseOptions, "Response Options should be set");
        assertNotNull(flow.r1, "R1 (1st Recipient) should be set");
        assertNotNull(flow.t1, "T1 (Time to 1st Recipient) should be set");
        assertNotNull(flow.r2, "R2 (2nd Recipient) should be set");
        assertNotNull(flow.t2, "T2 (Time to 2nd Recipient) should be set");
    }
    
    private boolean equals(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        return s1.equals(s2);
    }
}
