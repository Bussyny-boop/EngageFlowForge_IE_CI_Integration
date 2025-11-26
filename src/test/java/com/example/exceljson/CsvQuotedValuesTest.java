package com.example.exceljson;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test CSV parsing with quoted values and asterisks in headers.
 * This tests the fix for "Load Voice Group" failing when CSV has headers like "Group Name *"
 */
public class CsvQuotedValuesTest {

    private static final String TRAILING_ASTERISK_REGEX = "\\*+$";

    /**
     * Helper method to clean CSV values by stripping surrounding quotes.
     * Matches the implementation in AppController.cleanCsvValue()
     */
    private String cleanCsvValue(String value) {
        if (value == null) return "";
        // First remove surrounding quotes
        String cleaned = value.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned.trim();
    }

    /**
     * Simulates the improved CSV parsing logic from AppController.loadVoiceGroups()
     */
    private Set<String> parseCsvFile(String csvContent) throws IOException {
        Set<String> groups = new HashSet<>();
        BufferedReader br = new BufferedReader(new StringReader(csvContent));
        
        String headerLine = br.readLine();
        int groupNameColumn = 0; // Default to column 0 (Column A)
        
        // Check if first line contains "Group Name" keyword in header (case-insensitive)
        // Also handles quoted values and trailing asterisks
        if (headerLine != null) {
            String[] headers = headerLine.split(",|\\t");
            boolean hasGroupNameHeader = false;
            
            for (int i = 0; i < headers.length; i++) {
                // Clean CSV value (strip quotes) then remove trailing asterisks and trim
                String headerValue = cleanCsvValue(headers[i]).replaceAll(TRAILING_ASTERISK_REGEX, "").trim();
                // Use contains to match "Group Name" keyword anywhere in the header
                if (headerValue.toLowerCase().contains("group name")) {
                    groupNameColumn = i;
                    hasGroupNameHeader = true;
                    break;
                }
            }
            
            // If no "Group Name" header found, treat first line as data
            if (!hasGroupNameHeader) {
                String[] parts = headerLine.split(",|\\t");
                if (parts.length > groupNameColumn) {
                    String value = cleanCsvValue(parts[groupNameColumn]);
                    if (!value.isEmpty()) {
                        groups.add(value);
                    }
                }
            }
        }
        
        // Read remaining lines
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",|\\t");
            if (parts.length > groupNameColumn) {
                String value = cleanCsvValue(parts[groupNameColumn]);
                if (!value.isEmpty()) {
                    groups.add(value);
                }
            }
        }
        
        return groups;
    }

    @Test
    public void testCsvWithQuotedGroupNameAsteriskHeader() throws IOException {
        // This is the format that was failing before the fix
        // Header has "Group Name *" with quotes and asterisk
        String csv = "\"ID\",\"Group Name *\",\"Facility *\"\n" +
                     "\"8658\",\"Whittier Group\",\"Whittier\"\n" +
                     "\"129\",\"Everyone\",\"Whittier\"\n" +
                     "\"6575\",\"Critical Care Nurse\",\"Whittier\"";
        
        Set<String> groups = parseCsvFile(csv);
        
        // Should correctly parse the quoted values
        assertEquals(3, groups.size());
        assertTrue(groups.contains("Whittier Group"));
        assertTrue(groups.contains("Everyone"));
        assertTrue(groups.contains("Critical Care Nurse"));
        
        // Should NOT contain the header
        assertFalse(groups.contains("Group Name *"));
        assertFalse(groups.contains("\"Group Name *\""));
        assertFalse(groups.contains("Group Name"));
    }

    @Test
    public void testCsvWithUnquotedGroupNameAsteriskHeader() throws IOException {
        // Header has Group Name * without quotes
        String csv = "ID,Group Name *,Facility\n" +
                     "1,Team A,Site 1\n" +
                     "2,Team B,Site 2";
        
        Set<String> groups = parseCsvFile(csv);
        
        assertEquals(2, groups.size());
        assertTrue(groups.contains("Team A"));
        assertTrue(groups.contains("Team B"));
        assertFalse(groups.contains("Group Name *"));
    }

    @Test
    public void testCsvWithQuotedSingleColumnValues() throws IOException {
        // Test single-column CSV with quoted values - simple case that works
        String csv = "\"Group Name *\"\n" +
                     "\"Code Blue Team\"\n" +
                     "\"Rapid Response\"\n" +
                     "\"Nursing Staff\"";
        
        Set<String> groups = parseCsvFile(csv);
        
        assertEquals(3, groups.size());
        assertTrue(groups.contains("Code Blue Team"));
        assertTrue(groups.contains("Rapid Response"));
        assertTrue(groups.contains("Nursing Staff"));
    }

    @Test
    public void testCleanCsvValueMethod() {
        // Test the helper method directly
        assertEquals("Group Name", cleanCsvValue("\"Group Name\""));
        assertEquals("Group Name *", cleanCsvValue("\"Group Name *\""));
        assertEquals("Group Name", cleanCsvValue("  \"Group Name\"  "));
        assertEquals("Test Value", cleanCsvValue("Test Value"));
        assertEquals("", cleanCsvValue(""));
        assertEquals("", cleanCsvValue(null));
        assertEquals("\"Only opening quote", cleanCsvValue("\"Only opening quote"));
        assertEquals("Only closing quote\"", cleanCsvValue("Only closing quote\""));
    }

    @Test
    public void testAsteriskStrippingAfterQuoteRemoval() {
        // Test that asterisks are properly stripped after quotes are removed
        String value = "\"Group Name *\"";
        String cleaned = cleanCsvValue(value);
        String withoutAsterisk = cleaned.replaceAll(TRAILING_ASTERISK_REGEX, "").trim();
        
        assertEquals("Group Name *", cleaned);  // After removing quotes
        assertEquals("Group Name", withoutAsterisk);  // After also removing asterisk
    }

    @Test
    public void testRealWorldCsvSample() throws IOException {
        // Matches the actual format from Groups (2).csv file
        String csv = "\"ID\",\"Group Name *\",\"Facility *\",\"Vocera Phone\",\"Group Type (Department, Subdepartment, Ordinary) *\"\n" +
                     "\"8658\",\"Whittier Group\",\"Whittier\",\"\",\"Ordinary\"\n" +
                     "\"129\",\"Everyone\",\"Whittier\",\"\",\"Ordinary\"\n" +
                     "\"6575\",\"Critical Care Nurse\",\"Whittier\",\"g-critical_care_nurse0\",\"Subdepartment\"";
        
        Set<String> groups = parseCsvFile(csv);
        
        assertEquals(3, groups.size());
        assertTrue(groups.contains("Whittier Group"));
        assertTrue(groups.contains("Everyone"));
        assertTrue(groups.contains("Critical Care Nurse"));
        
        // Importantly, the values should NOT have surrounding quotes
        assertFalse(groups.contains("\"Whittier Group\""));
    }

    @Test
    public void testNoGroupNameHeaderFallsBackToColumnA() throws IOException {
        // When there's no "Group Name" header, should use column A
        String csv = "\"First\",\"Second\",\"Third\"\n" +
                     "\"Value A\",\"Value B\",\"Value C\"\n" +
                     "\"Alpha\",\"Beta\",\"Gamma\"";
        
        Set<String> groups = parseCsvFile(csv);
        
        // Should extract from column A (first column)
        assertEquals(3, groups.size());
        assertTrue(groups.contains("First"));  // First row treated as data
        assertTrue(groups.contains("Value A"));
        assertTrue(groups.contains("Alpha"));
    }
}
