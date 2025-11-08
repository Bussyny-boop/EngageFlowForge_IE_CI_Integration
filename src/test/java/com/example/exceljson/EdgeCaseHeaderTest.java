package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EdgeCaseHeaderTest {
    
    @Test
    void testSheetWithOnlyTwoRows() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Test");
            
            // Only create row 0 and row 1
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("Title");
            
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Col1");
            row1.createCell(1).setCellValue("Col2");
            row1.createCell(2).setCellValue("Col3");
            
            System.out.println("\n=== Sheet with only 2 rows (0, 1) ===");
            System.out.println("getLastRowNum(): " + sheet.getLastRowNum());
            
            int limit = Math.min(sheet.getLastRowNum(), 2);
            System.out.println("Primary search: r <= Math.min(" + sheet.getLastRowNum() + ", 2) = " + limit);
            System.out.println("Will check rows 0 through " + limit);
            
            // The limit should be 1, meaning we check rows 0 and 1
            assertEquals(1, limit, "Should check up to row 1");
        }
    }
    
    @Test
    void testFindingHeaderInRow3WhenSheetHasFewRows() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Test");
            
            // Create only rows 0, 1, NO row 2!
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("Something");
            
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Another");
            
            System.out.println("\n=== Edge case: Sheet stops at row 1, but we need row 2 ===");
            System.out.println("getLastRowNum(): " + sheet.getLastRowNum());
            
            int limit = Math.min(sheet.getLastRowNum(), 2);
            System.out.println("Primary search limit: " + limit);
            System.out.println("This means we WON'T check row 2 because getLastRowNum()=" + sheet.getLastRowNum());
            
            assertEquals(1, sheet.getLastRowNum());
            assertEquals(1, limit);
            
            // So if header is actually in row 2 (index 2), we won't find it in primary search!
        }
    }
}
