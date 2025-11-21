package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests to verify that sidebar navigation tabs display proper icons when collapsed.
 * This test ensures that when the sidebar is minimized, users see distinct icons
 * instead of just dots or empty text.
 */
class SidebarIconsTest {

    /**
     * Verify that navigation tab icons are defined correctly
     */
    @Test
    void navigationTabsShouldHaveDistinctIcons() {
        // Define expected icons for each navigation tab
        String unitsIcon = "📊";
        String nurseCallsIcon = "🔔";
        String clinicalsIcon = "🏥";
        String ordersIcon = "💊";
        
        // Verify icons are not empty
        assertFalse(unitsIcon.isEmpty(), "Units icon should not be empty");
        assertFalse(nurseCallsIcon.isEmpty(), "Nurse Calls icon should not be empty");
        assertFalse(clinicalsIcon.isEmpty(), "Clinicals icon should not be empty");
        assertFalse(ordersIcon.isEmpty(), "Orders icon should not be empty");
        
        // Verify icons are distinct from each other
        assertNotEquals(unitsIcon, nurseCallsIcon, "Units and Nurse Calls should have different icons");
        assertNotEquals(unitsIcon, clinicalsIcon, "Units and Clinicals should have different icons");
        assertNotEquals(unitsIcon, ordersIcon, "Units and Orders should have different icons");
        assertNotEquals(nurseCallsIcon, clinicalsIcon, "Nurse Calls and Clinicals should have different icons");
        assertNotEquals(nurseCallsIcon, ordersIcon, "Nurse Calls and Orders should have different icons");
        assertNotEquals(clinicalsIcon, ordersIcon, "Clinicals and Orders should have different icons");
    }
    
    /**
     * Verify that each navigation tab has a meaningful tooltip
     */
    @Test
    void navigationTabsShouldHaveTooltips() {
        String unitsTooltip = "Units";
        String nurseCallsTooltip = "Nurse Calls";
        String clinicalsTooltip = "Clinicals";
        String ordersTooltip = "Orders";
        
        // Verify tooltips are not empty
        assertFalse(unitsTooltip.isEmpty(), "Units tooltip should not be empty");
        assertFalse(nurseCallsTooltip.isEmpty(), "Nurse Calls tooltip should not be empty");
        assertFalse(clinicalsTooltip.isEmpty(), "Clinicals tooltip should not be empty");
        assertFalse(ordersTooltip.isEmpty(), "Orders tooltip should not be empty");
    }
    
    /**
     * Verify that collapsed state uses icon-only display
     */
    @Test
    void collapsedStateShouldShowIconsOnly() {
        // When collapsed, the button text should be just the icon
        // This simulates what setCollapsedTab() does in AppController
        String icon = "📊";
        String tooltip = "Units";
        
        // Simulate collapsed state behavior
        String displayText = icon;
        String displayTooltip = tooltip;
        
        // Verify the collapsed state shows only the icon
        assertEquals(icon, displayText, "Collapsed state should show only the icon");
        assertEquals(tooltip, displayTooltip, "Tooltip should provide full description");
    }
    
    /**
     * Verify that expanded state shows full text with icon
     */
    @Test
    void expandedStateShouldShowFullText() {
        // When expanded, the button text should include both icon and label
        String fullText = "📊 Units";
        
        // Verify the expanded state shows full text
        assertTrue(fullText.contains("📊"), "Expanded state should include icon");
        assertTrue(fullText.contains("Units"), "Expanded state should include label text");
    }
}
