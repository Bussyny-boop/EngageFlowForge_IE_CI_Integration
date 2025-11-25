package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for filtering out DataUpdate rules that reference views with
 * negative logic (not_in, not_equal) on role-related paths like
 * "bed.locs.assignments.role.name".
 * 
 * Requirements:
 * 1. When determining "Time to Recipients", skip DataUpdate rules whose
 *    condition views contain filters with negative relations (not_in, not_equal, not_like)
 *    on role.name paths.
 * 
 * 2. These rules represent "No role caregiver X is online" escalation scenarios
 *    that should not contribute to timing calculation.
 * 
 * 3. The logic should continue to use other valid DataUpdate rules
 *    (with positive role filters) for timing determination.
 * 
 * Example from fairview_eastbank_west_bank_prod.xml:
 * <rule active="true" dataset="Clinicals">
 *   <purpose>ESCALATE TO TERTIARY | ALL ALARMS | NO SECONDARY ASSIGNED | NURSE BUDDY | ALL UNITS</purpose>
 *   <trigger-on update="true"/>
 *   <defer-delivery-by>0</defer-delivery-by>
 *   <condition>
 *     <view>Alarm_is_active</view>
 *     <view>Alarm_is_at_secondary_state</view>
 *     <view>No_role_caregiver_NURSE_BUDDY_is_online</view>  <!-- Contains the filter to exclude -->
 *     <view>Room_is_blocked_for_construction</view>
 *   </condition>
 * </rule>
 * 
 * Where the view contains:
 * <filter relation="not_in">
 *   <path>bed.locs.assignments.role.name</path>
 *   <value>Buddy Nurse, Nurse Buddy</value>
 * </filter>
 */
public class NegativeRoleFilterExclusionTest {

    @Test
    public void testDataUpdateWithNegativeRoleFilterIsExcludedFromTiming() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-negative-role-filter-exclusion.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // Test scenario:
        // - CREATE rule sets state to Primary with 30 sec delay
        // - Normal escalation rule (Primary -> Secondary) has 60 sec delay with positive role filter
        // - "No Secondary Assigned" rule (Secondary -> Tertiary) has 0 sec delay with NEGATIVE role filter 
        //   (not_in on role.name) - should be EXCLUDED
        // - Normal tertiary escalation rule (Secondary -> Tertiary) has 90 sec delay with positive role filter
        // 
        // Expected timing:
        // - T1 = 30 (from CREATE rule)
        // - T2 = 60 (from normal Primary -> Secondary escalation)
        // - T3 = 90 (from normal Secondary -> Tertiary escalation, NOT 0 from the excluded rule)
        
        boolean foundTestFlow = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("APNEA".equals(flow.alarmName) || "BRADY".equals(flow.alarmName)) {
                foundTestFlow = true;
                
                System.out.println("Flow: " + flow.alarmName);
                System.out.println("  T1: " + flow.t1 + " (expected: 30 from CREATE rule)");
                System.out.println("  T2: " + flow.t2 + " (expected: 60 from normal escalation)");
                System.out.println("  T3: " + flow.t3 + " (expected: 90 from normal escalation, NOT 0)");
                System.out.println("  R1: " + flow.r1);
                System.out.println("  R2: " + flow.r2);
                System.out.println("  R3: " + flow.r3);
                
                // T3 should NOT be 0 from the "No Secondary Assigned" rule
                // That rule references No_role_caregiver_NURSE_BUDDY_is_online which has
                // not_in on bed.locs.assignments.role.name - it should be filtered out
                if (flow.t3 != null && !flow.t3.isEmpty()) {
                    assertNotEquals("0", flow.t3, 
                        "T3 should NOT be 0 - the 'No Secondary Assigned' DataUpdate rule (with not_in on role.name) should be filtered out from timing calculation");
                    assertEquals("90", flow.t3,
                        "T3 should be 90 from the normal tertiary escalation rule");
                }
            }
        }
        
        assertTrue(foundTestFlow, "Should find test flows (APNEA or BRADY)");
    }
    
    @Test
    public void testNormalEscalationRuleWithPositiveRoleFilterIsNotFiltered() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-negative-role-filter-exclusion.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // The normal DataUpdate rules (Role_caregiver_NURSE_BUDDY_is_online) should NOT be filtered
        // because they use positive relations (in, equal) on role.name
        
        boolean hasEscalationTiming = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            // Check for proper escalation timing (T2 = 60, T3 = 90)
            if (flow.t2 != null && !flow.t2.isEmpty() && 
                flow.t3 != null && !flow.t3.isEmpty()) {
                hasEscalationTiming = true;
                System.out.println("Flow with full escalation timing: " + flow.alarmName);
                System.out.println("  T2: " + flow.t2 + " (from normal Primary -> Secondary escalation)");
                System.out.println("  T3: " + flow.t3 + " (from normal Secondary -> Tertiary escalation)");
            }
        }
        
        assertTrue(hasEscalationTiming, 
            "Should have flows with escalation timing from normal DataUpdate rules (that use positive role filters)");
    }
    
    @Test
    public void testFairviewXmlParsingSucceeds() throws Exception {
        // Test with the actual fairview_eastbank_west_bank_prod.xml if available
        File xmlFile = new File("fairview_eastbank_west_bank_prod.xml");
        
        // Use Assumptions to properly skip the test if file doesn't exist
        Assumptions.assumeTrue(xmlFile.exists(), 
            "Skipping fairview test - fairview_eastbank_west_bank_prod.xml not found");
        
        XmlParser parser = new XmlParser();
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // The fairview XML is complex with many rules - we just verify it parses successfully
        // and produces some clinical flows
        assertFalse(clinicals.isEmpty(), "Should have clinical flows from fairview XML");
        
        System.out.println("Successfully parsed fairview XML");
        System.out.println("  Total clinical flows: " + clinicals.size());
        
        // Count flows with various timing values to verify the parser works
        long withT2 = clinicals.stream().filter(f -> f.t2 != null && !f.t2.isEmpty()).count();
        long withT3 = clinicals.stream().filter(f -> f.t3 != null && !f.t3.isEmpty()).count();
        
        System.out.println("  Flows with T2: " + withT2);
        System.out.println("  Flows with T3: " + withT3);
        
        // Print summary of the load
        System.out.println("\n" + parser.getLoadSummary());
    }
}
