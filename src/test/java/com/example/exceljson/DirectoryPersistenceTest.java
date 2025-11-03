package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that directory persistence using Java Preferences API works correctly.
 * This tests the underlying mechanism used by AppController without requiring JavaFX initialization.
 */
public class DirectoryPersistenceTest {

    private static final String PREF_KEY_LAST_EXCEL_DIR = "lastExcelDir";
    private static final String PREF_KEY_LAST_JSON_DIR = "lastJsonDir";
    private Preferences prefs;

    @BeforeEach
    public void setUp() {
        // Use a test-specific preferences node
        prefs = Preferences.userNodeForPackage(DirectoryPersistenceTest.class);
        // Clear any existing test preferences
        prefs.remove(PREF_KEY_LAST_EXCEL_DIR);
        prefs.remove(PREF_KEY_LAST_JSON_DIR);
    }

    @AfterEach
    public void tearDown() {
        // Clean up test preferences
        prefs.remove(PREF_KEY_LAST_EXCEL_DIR);
        prefs.remove(PREF_KEY_LAST_JSON_DIR);
    }

    @Test
    public void testCanSaveAndRetrieveExcelDirectory() {
        String testPath = "/test/excel/directory";
        
        // Save the preference
        prefs.put(PREF_KEY_LAST_EXCEL_DIR, testPath);
        
        // Retrieve the preference
        String retrieved = prefs.get(PREF_KEY_LAST_EXCEL_DIR, null);
        
        assertNotNull(retrieved, "Retrieved Excel directory should not be null");
        assertEquals(testPath, retrieved, "Retrieved path should match saved path");
    }

    @Test
    public void testCanSaveAndRetrieveJsonDirectory() {
        String testPath = "/test/json/directory";
        
        // Save the preference
        prefs.put(PREF_KEY_LAST_JSON_DIR, testPath);
        
        // Retrieve the preference
        String retrieved = prefs.get(PREF_KEY_LAST_JSON_DIR, null);
        
        assertNotNull(retrieved, "Retrieved JSON directory should not be null");
        assertEquals(testPath, retrieved, "Retrieved path should match saved path");
    }

    @Test
    public void testExcelAndJsonDirectoriesAreIndependent() {
        String excelPath = "/test/excel/directory";
        String jsonPath = "/test/json/directory";
        
        // Save both preferences
        prefs.put(PREF_KEY_LAST_EXCEL_DIR, excelPath);
        prefs.put(PREF_KEY_LAST_JSON_DIR, jsonPath);
        
        // Retrieve both preferences
        String retrievedExcel = prefs.get(PREF_KEY_LAST_EXCEL_DIR, null);
        String retrievedJson = prefs.get(PREF_KEY_LAST_JSON_DIR, null);
        
        assertNotNull(retrievedExcel, "Retrieved Excel directory should not be null");
        assertNotNull(retrievedJson, "Retrieved JSON directory should not be null");
        assertEquals(excelPath, retrievedExcel, "Excel path should match");
        assertEquals(jsonPath, retrievedJson, "JSON path should match");
        assertNotEquals(retrievedExcel, retrievedJson, "Excel and JSON paths should be different");
    }

    @Test
    public void testCanRemovePreferences() {
        String testPath = "/test/directory";
        
        // Save preference
        prefs.put(PREF_KEY_LAST_EXCEL_DIR, testPath);
        assertNotNull(prefs.get(PREF_KEY_LAST_EXCEL_DIR, null), "Preference should exist after saving");
        
        // Remove preference
        prefs.remove(PREF_KEY_LAST_EXCEL_DIR);
        
        // Verify it's removed (should return default value)
        String retrieved = prefs.get(PREF_KEY_LAST_EXCEL_DIR, null);
        assertNull(retrieved, "Preference should be null after removal");
    }

    @Test
    public void testDefaultValueWhenPreferenceNotSet() {
        // Don't set any preference
        String retrieved = prefs.get(PREF_KEY_LAST_EXCEL_DIR, null);
        
        assertNull(retrieved, "Should return null when preference is not set");
    }

    @Test
    public void testCanOverwriteExistingPreference() {
        String initialPath = "/initial/path";
        String newPath = "/new/path";
        
        // Save initial preference
        prefs.put(PREF_KEY_LAST_EXCEL_DIR, initialPath);
        assertEquals(initialPath, prefs.get(PREF_KEY_LAST_EXCEL_DIR, null));
        
        // Overwrite with new value
        prefs.put(PREF_KEY_LAST_EXCEL_DIR, newPath);
        
        // Verify new value is stored
        String retrieved = prefs.get(PREF_KEY_LAST_EXCEL_DIR, null);
        assertEquals(newPath, retrieved, "Preference should be updated to new value");
        assertNotEquals(initialPath, retrieved, "Old value should be replaced");
    }
}
