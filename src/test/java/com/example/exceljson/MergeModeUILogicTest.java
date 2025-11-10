package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for the UI merge mode helper logic.
 * Simulates checkbox states and validates that getCurrentMergeMode() returns the correct mode.
 */
class MergeModeUILogicTest {

    /**
     * Simulates the getCurrentMergeMode() logic from AppController
     */
    private ExcelParserV5.MergeMode getCurrentMergeMode(
            boolean noMergeSelected, 
            boolean mergeFlowsSelected, 
            boolean mergeByConfigGroupSelected) {
        
        if (mergeFlowsSelected) {
            return ExcelParserV5.MergeMode.MERGE_ALL;
        } else if (mergeByConfigGroupSelected) {
            return ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP;
        } else {
            // Default to NONE if noMergeSelected is true or nothing is selected
            return ExcelParserV5.MergeMode.NONE;
        }
    }

    @Test
    void noMergeSelected_ReturnsNone() {
        ExcelParserV5.MergeMode mode = getCurrentMergeMode(true, false, false);
        assertEquals(ExcelParserV5.MergeMode.NONE, mode);
    }

    @Test
    void mergeAllSelected_ReturnsMergeAll() {
        ExcelParserV5.MergeMode mode = getCurrentMergeMode(false, true, false);
        assertEquals(ExcelParserV5.MergeMode.MERGE_ALL, mode);
    }

    @Test
    void mergeByConfigGroupSelected_ReturnsMergeByConfigGroup() {
        ExcelParserV5.MergeMode mode = getCurrentMergeMode(false, false, true);
        assertEquals(ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP, mode);
    }

    @Test
    void nothingSelected_DefaultsToNone() {
        // This shouldn't happen in the UI due to mutual exclusion,
        // but we should handle it gracefully
        ExcelParserV5.MergeMode mode = getCurrentMergeMode(false, false, false);
        assertEquals(ExcelParserV5.MergeMode.NONE, mode);
    }

    @Test
    void mergeAllTakesPrecedence_WhenMultipleSelected() {
        // This shouldn't happen in the UI, but mergeAll should take precedence
        ExcelParserV5.MergeMode mode = getCurrentMergeMode(true, true, true);
        assertEquals(ExcelParserV5.MergeMode.MERGE_ALL, mode);
    }

    @Test
    void mergeByConfigGroupSecondPrecedence() {
        // If mergeAll is not selected but mergeByConfigGroup is
        ExcelParserV5.MergeMode mode = getCurrentMergeMode(true, false, true);
        assertEquals(ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP, mode);
    }

    @Test
    void enumValuesAreCorrect() {
        // Verify the enum has exactly the expected values
        ExcelParserV5.MergeMode[] modes = ExcelParserV5.MergeMode.values();
        assertEquals(3, modes.length);
        assertEquals(ExcelParserV5.MergeMode.NONE, modes[0]);
        assertEquals(ExcelParserV5.MergeMode.MERGE_ALL, modes[1]);
        assertEquals(ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP, modes[2]);
    }

    @Test
    void enumNamingConventions() {
        // Verify enum names are correctly formatted
        assertEquals("NONE", ExcelParserV5.MergeMode.NONE.name());
        assertEquals("MERGE_ALL", ExcelParserV5.MergeMode.MERGE_ALL.name());
        assertEquals("MERGE_BY_CONFIG_GROUP", ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP.name());
    }
}
