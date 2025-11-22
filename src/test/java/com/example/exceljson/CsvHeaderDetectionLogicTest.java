package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit test to verify CSV header detection logic without full AppController
 */
public class CsvHeaderDetectionLogicTest {

    /**
     * Simulates the CSV parsing logic from AppController.loadVoiceGroups()
     */
    private Set<String> parseCsvFile(String csvContent) throws IOException {
        Set<String> groups = new HashSet<>();
        BufferedReader br = new BufferedReader(new StringReader(csvContent));
        
        String headerLine = br.readLine();
        int groupNameColumn = 0; // Default to column 0 (Column A)
        
        // Check if first line contains "Group Name" header (case-insensitive)
        if (headerLine != null) {
            String[] headers = headerLine.split(",");
            boolean hasGroupNameHeader = false;
            
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase("Group Name")) {
                    groupNameColumn = i;
                    hasGroupNameHeader = true;
                    break;
                }
            }
            
            // If no "Group Name" header found, treat first line as data
            if (!hasGroupNameHeader) {
                String[] parts = headerLine.split(",");
                if (parts.length > groupNameColumn && !parts[groupNameColumn].trim().isEmpty()) {
                    groups.add(parts[groupNameColumn].trim());
                }
            }
        }
        
        // Read remaining lines
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length > groupNameColumn && !parts[groupNameColumn].trim().isEmpty()) {
                groups.add(parts[groupNameColumn].trim());
            }
        }
        
        return groups;
    }

    @Test
    public void testCsvWithGroupNameHeader() throws IOException {
        String csv = "Group Name\n" +
                     "Nursing Team\n" +
                     "Code Blue\n" +
                     "Rapid Response";
        
        Set<String> groups = parseCsvFile(csv);
        
        // Should skip header and extract only data rows
        assertEquals(3, groups.size());
        assertTrue(groups.contains("Nursing Team"));
        assertTrue(groups.contains("Code Blue"));
        assertTrue(groups.contains("Rapid Response"));
        assertFalse(groups.contains("Group Name")); // Header should NOT be included
    }

    @Test
    public void testCsvWithoutGroupNameHeader() throws IOException {
        String csv = "OB Nurses\n" +
                     "ICU Staff\n" +
                     "Surgery Team";
        
        Set<String> groups = parseCsvFile(csv);
        
        // Should treat all rows as data (including first row)
        assertEquals(3, groups.size());
        assertTrue(groups.contains("OB Nurses"));
        assertTrue(groups.contains("ICU Staff"));
        assertTrue(groups.contains("Surgery Team"));
    }

    @Test
    public void testCsvWithGroupNameHeaderInSecondColumn() throws IOException {
        String csv = "ID,Group Name,Description\n" +
                     "1,Cardiology,Heart specialists\n" +
                     "2,Neurology,Brain specialists";
        
        Set<String> groups = parseCsvFile(csv);
        
        // Should extract from second column (index 1)
        assertEquals(2, groups.size());
        assertTrue(groups.contains("Cardiology"));
        assertTrue(groups.contains("Neurology"));
        assertFalse(groups.contains("1")); // IDs should NOT be included
        assertFalse(groups.contains("2"));
        assertFalse(groups.contains("Group Name")); // Header should NOT be included
    }

    @Test
    public void testCsvWithLowercaseGroupNameHeader() throws IOException {
        String csv = "group name\n" + // lowercase
                     "Lab Team\n" +
                     "Radiology";
        
        Set<String> groups = parseCsvFile(csv);
        
        // Should recognize lowercase "group name" as header (case-insensitive)
        assertEquals(2, groups.size());
        assertTrue(groups.contains("Lab Team"));
        assertTrue(groups.contains("Radiology"));
        assertFalse(groups.contains("group name")); // Header should NOT be included
    }

    @Test
    public void testCsvWithUppercaseGroupNameHeader() throws IOException {
        String csv = "GROUP NAME\n" + // uppercase
                     "Pharmacy\n" +
                     "Social Work";
        
        Set<String> groups = parseCsvFile(csv);
        
        // Should recognize uppercase "GROUP NAME" as header (case-insensitive)
        assertEquals(2, groups.size());
        assertTrue(groups.contains("Pharmacy"));
        assertTrue(groups.contains("Social Work"));
        assertFalse(groups.contains("GROUP NAME")); // Header should NOT be included
    }

    @Test
    public void testCsvWithEmptyLines() throws IOException {
        String csv = "Group Name\n" +
                     "Team A\n" +
                     "\n" + // empty line
                     "Team B\n" +
                     ",\n" + // line with just commas
                     "Team C";
        
        Set<String> groups = parseCsvFile(csv);
        
        // Should skip empty lines
        assertEquals(3, groups.size());
        assertTrue(groups.contains("Team A"));
        assertTrue(groups.contains("Team B"));
        assertTrue(groups.contains("Team C"));
    }

    @Test
    public void testEmptyCsv() throws IOException {
        String csv = "";
        
        Set<String> groups = parseCsvFile(csv);
        
        // Should handle empty file gracefully
        assertEquals(0, groups.size());
    }

    @Test
    public void testCsvWithOnlyHeader() throws IOException {
        String csv = "Group Name\n";
        
        Set<String> groups = parseCsvFile(csv);
        
        // Should recognize header and have no data
        assertEquals(0, groups.size());
    }
}
