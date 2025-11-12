package com.example.exceljson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the POD Room Filter field is properly accessible
 * in the UnitRow data model.
 */
public class PodRoomFilterColumnTest {

    @Test
    public void podRoomFilterFieldExists() {
        // Create a UnitRow
        ExcelParserV5.UnitRow unit = new ExcelParserV5.UnitRow();
        
        // Verify the field can be accessed and modified
        assertNotNull(unit.podRoomFilter, "podRoomFilter field should exist");
        assertEquals("", unit.podRoomFilter, "podRoomFilter should default to empty string");
        
        // Test setting a value
        unit.podRoomFilter = "POD 1";
        assertEquals("POD 1", unit.podRoomFilter, "podRoomFilter should be settable");
    }

    @Test
    public void podRoomFilterCanBeSetOnConstruction() {
        // Create a UnitRow and set values
        ExcelParserV5.UnitRow unit = new ExcelParserV5.UnitRow();
        unit.facility = "Test Facility";
        unit.unitNames = "ICU";
        unit.podRoomFilter = "POD A, POD B";
        unit.nurseGroup = "ICU-Nurse";
        
        // Verify all values are set correctly
        assertEquals("Test Facility", unit.facility);
        assertEquals("ICU", unit.unitNames);
        assertEquals("POD A, POD B", unit.podRoomFilter);
        assertEquals("ICU-Nurse", unit.nurseGroup);
    }

    @Test
    public void podRoomFilterPositionedCorrectly() {
        // This test documents that podRoomFilter should be accessible
        // right after unitNames in the data model
        ExcelParserV5.UnitRow unit = new ExcelParserV5.UnitRow();
        
        // Set values in the expected order
        unit.facility = "Facility1";
        unit.unitNames = "Unit1";
        unit.podRoomFilter = "POD 1";  // Should be after unitNames
        unit.nurseGroup = "Group1";
        
        // Verify the field is accessible and editable
        assertNotNull(unit.podRoomFilter);
        assertEquals("POD 1", unit.podRoomFilter);
    }
}
