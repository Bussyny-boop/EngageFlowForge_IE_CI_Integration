package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for filtering out DataUpdate rules that have views with 
 * "not_equal" filter on "assignments.state" with value "Active".
 * 
 * Requirements:
 * 1. When determining "Time to Recipients", skip DataUpdate rules whose 
 *    condition views contain a filter with:
 *    - relation="not_equal"
 *    - path ending with "assignments.state"
 *    - value="Active"
 * 
 * 2. These rules represent "No Primary Assigned" escalation scenarios
 *    that should not contribute to timing calculation.
 * 
 * 3. The logic should continue to use other valid DataUpdate rules 
 *    for timing determination.
 * 
 * Example of a rule that should be filtered:
 * <rule active="true" dataset="Clinicals">
 *   <purpose>ESCALATE TO SECONDARY | ALL ALARMS | NO PRIMARY ASSIGNED | NURSE | ALL UNITS</purpose>
 *   <trigger-on update="true"/>
 *   <defer-delivery-by>0</defer-delivery-by>
 *   <condition>
 *     <view>Alarm_is_active</view>
 *     <view>Alarm_is_at_primary_state</view>
 *     <view>No_role_caregiver_NURSE_is_online</view>  <!-- Contains the filter to exclude -->
 *   </condition>
 * </rule>
 * 
 * Where the view contains:
 * <filter relation="not_equal">
 *   <path>bed.locs.assignments.state</path>
 *   <value>Active</value>
 * </filter>
 */
public class InactiveAssignmentFilterTest {

    @Test
    public void testDataUpdateWithInactiveAssignmentFilterIsExcludedFromTiming() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-inactive-assignment-filter.xml");
        assertTrue(xmlFile.exists(), "Test XML file should exist");
        
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        assertFalse(clinicals.isEmpty(), "Should have clinical flows");
        
        // Test scenario:
        // - There's a DataUpdate rule with 60 sec delay that references Role_caregiver_NURSE_is_online (normal)
        // - There's a DataUpdate rule with 0 sec delay that references No_role_caregiver_NURSE_is_online 
        //   (contains the not_equal filter on assignments.state = Active) - should be EXCLUDED
        // 
        // The T2 timing should be 60 seconds (from the normal escalation rule)
        // NOT 0 seconds (from the "No Primary Assigned" rule which should be filtered out)
        
        boolean foundTestFlow = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if ("APNEA".equals(flow.alarmName) || "BRADY".equals(flow.alarmName)) {
                foundTestFlow = true;
                
                System.out.println("Flow: " + flow.alarmName);
                System.out.println("  T1: " + flow.t1 + " (should be 30 or Immediate from CREATE rule)");
                System.out.println("  T2: " + flow.t2 + " (should be 60 from normal escalation, NOT 0)");
                System.out.println("  R1: " + flow.r1);
                System.out.println("  R2: " + flow.r2);
                
                // T1 should be from CREATE rule (30 seconds)
                // T2 should be 60 seconds from the NORMAL escalation rule
                // T2 should NOT be 0 from the "No Primary Assigned" rule (that rule should be filtered out)
                
                // The "No Primary Assigned" rule with 0 delay should be filtered out
                // So T2 should be 60, not 0
                if (flow.t2 != null && !flow.t2.isEmpty()) {
                    assertNotEquals("0", flow.t2, 
                        "T2 should NOT be 0 - the 'No Primary Assigned' DataUpdate rule (with not_equal on assignments.state = Active) should be filtered out from timing calculation");
                    assertEquals("60", flow.t2,
                        "T2 should be 60 from the normal escalation rule");
                }
            }
        }
        
        assertTrue(foundTestFlow, "Should find test flows (APNEA or BRADY)");
    }
    
    @Test
    public void testNormalEscalationRuleIsNotFiltered() throws Exception {
        XmlParser parser = new XmlParser();
        
        File xmlFile = new File("src/test/resources/test-inactive-assignment-filter.xml");
        parser.load(xmlFile);
        
        List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
        
        // The normal DataUpdate rule (Role_caregiver_NURSE_is_online) should NOT be filtered
        // because it uses "equal" relation on assignments.state = Active, not "not_equal"
        
        boolean hasEscalationTiming = false;
        for (ExcelParserV5.FlowRow flow : clinicals) {
            if (flow.t2 != null && !flow.t2.isEmpty() && !"0".equals(flow.t2)) {
                hasEscalationTiming = true;
                System.out.println("Flow with escalation timing: " + flow.alarmName);
                System.out.println("  T2: " + flow.t2);
            }
        }
        
        assertTrue(hasEscalationTiming, 
            "Should have flows with escalation timing from normal DataUpdate rule (that uses equal on assignments.state = Active)");
    }
}
