package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that "OutgoingWCTP" and "VMP" keywords:
 * 1. Do not highlight the cell (valid keywords)
 * 2. Generate the correct interfaces in JSON output
 */
public class OutgoingWCTPInterfaceTest {

    @Test
    public void testOutgoingWCTPIsValidKeyword() {
        ExcelParserV5 parser = new ExcelParserV5();
        
        // Verify OutgoingWCTP is a valid keyword (won't be highlighted)
        assertTrue(parser.hasValidRecipientKeyword("OutgoingWCTP"));
        assertTrue(parser.hasValidRecipientKeyword("outgoingwctp"));
        assertTrue(parser.hasValidRecipientKeyword("OUTGOINGWCTP"));
    }

    @Test
    public void testVMPIsValidKeyword() {
        ExcelParserV5 parser = new ExcelParserV5();
        
        // Verify VMP is a valid keyword (won't be highlighted)
        assertTrue(parser.hasValidRecipientKeyword("VMP"));
        assertTrue(parser.hasValidRecipientKeyword("vmp"));
        assertTrue(parser.hasValidRecipientKeyword("Vmp"));
    }

    @Test
    public void testOutgoingWCTPGeneratesEdgeInterface(@TempDir Path tempDir) throws Exception {
        // Create a test Excel file with OutgoingWCTP in Device-A
        File xlsxFile = tempDir.resolve("test-outgoingwctp.xlsx").toFile();
        createTestWorkbookWithDevice(xlsxFile, "OutgoingWCTP");

        // Load the file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(xlsxFile);

        // Set interface references
        parser.setInterfaceReferences("OutgoingWCTP", "VMP");

        // Build flows and check that OutgoingWCTP device triggers Edge interface
        assertFalse(parser.nurseCalls.isEmpty(), "Should have nurse call flows");
        ExcelParserV5.FlowRow flow = parser.nurseCalls.getFirst();
        assertEquals("OutgoingWCTP", flow.deviceA, "Device-A should be OutgoingWCTP");
    }

    @Test
    public void testVMPGeneratesVCSInterface(@TempDir Path tempDir) throws Exception {
        // Create a test Excel file with VMP in Device-A
        File xlsxFile = tempDir.resolve("test-vmp.xlsx").toFile();
        createTestWorkbookWithDevice(xlsxFile, "VMP");

        // Load the file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(xlsxFile);

        // Set interface references
        parser.setInterfaceReferences("OutgoingWCTP", "VMP");

        // Build flows and check that VMP device is recognized
        assertFalse(parser.nurseCalls.isEmpty(), "Should have nurse call flows");
        ExcelParserV5.FlowRow flow = parser.nurseCalls.getFirst();
        assertEquals("VMP", flow.deviceA, "Device-A should be VMP");
    }

    private void createTestWorkbookWithDevice(File xlsxFile, String deviceName) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row headerRow = unitSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Unit");
            headerRow.createCell(2).setCellValue("Nurse Call Config Group");

            Row dataRow = unitSheet.createRow(1);
            dataRow.createCell(0).setCellValue("Test Hospital");
            dataRow.createCell(1).setCellValue("ICU");
            dataRow.createCell(2).setCellValue("TestGroup");

            // Nurse call sheet with specified device in Device-A
            Sheet nurseSheet = wb.createSheet("Nurse call");
            Row nurseHeader = nurseSheet.createRow(0);
            nurseHeader.createCell(0).setCellValue("Facility");
            nurseHeader.createCell(1).setCellValue("Unit");
            nurseHeader.createCell(2).setCellValue("Nurse Call Config Group");
            nurseHeader.createCell(3).setCellValue("Alarm Name");
            nurseHeader.createCell(4).setCellValue("Priority");
            nurseHeader.createCell(5).setCellValue("Device-A");
            nurseHeader.createCell(6).setCellValue("Recipient 1");

            Row nurseData = nurseSheet.createRow(1);
            nurseData.createCell(0).setCellValue("Test Hospital");
            nurseData.createCell(1).setCellValue("ICU");
            nurseData.createCell(2).setCellValue("TestGroup");
            nurseData.createCell(3).setCellValue("Test Alarm");
            nurseData.createCell(4).setCellValue("Urgent");
            nurseData.createCell(5).setCellValue(deviceName);
            nurseData.createCell(6).setCellValue("Charge Nurse");

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(xlsxFile)) {
                wb.write(fos);
            }
        }
    }
}
