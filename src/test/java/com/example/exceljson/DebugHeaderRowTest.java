package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DebugHeaderRowTest {
    
    @Test
    void debugEmptyRowsBehavior() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Test");
            
            // Don't create rows 0 and 1 at all - they will be null
            // Row 3 (index 2) - header with data
            Row header = sheet.createRow(2);
            header.createCell(0).setCellValue("Column1");
            header.createCell(1).setCellValue("Column2");
            header.createCell(2).setCellValue("Column3");
            
            System.out.println("\n=== Sheet with header ONLY at row 2 ===");
            System.out.println("getFirstRowNum(): " + sheet.getFirstRowNum());
            System.out.println("getLastRowNum(): " + sheet.getLastRowNum());
            System.out.println("getPhysicalNumberOfRows(): " + sheet.getPhysicalNumberOfRows());
            
            // Test the loop condition from findHeaderRow
            int limit = Math.min(sheet.getLastRowNum(), 2);
            System.out.println("\nPrimary search: r <= Math.min(" + sheet.getLastRowNum() + ", 2) = " + limit);
            System.out.println("Loop will check rows: 0, 1, 2");
            
            for (int r = 0; r <= limit; r++) {
                Row row = sheet.getRow(r);
                System.out.println("  Row " + r + ": " + (row == null ? "NULL (will be skipped)" : "EXISTS"));
            }
            
            // This should succeed
            assertEquals(2, sheet.getLastRowNum());
            assertEquals(1, sheet.getPhysicalNumberOfRows());
        }
    }
    
    @Test
    void debugWithActualEmptyRows() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Test");
            
            // Create actual empty rows (with createRow but no data)
            sheet.createRow(0); // Empty row 1
            sheet.createRow(1); // Empty row 2
            
            // Row 3 (index 2) - header with data
            Row header = sheet.createRow(2);
            header.createCell(0).setCellValue("Column1");
            header.createCell(1).setCellValue("Column2");
            header.createCell(2).setCellValue("Column3");
            
            System.out.println("\n=== Sheet with EMPTY rows 0-1, header at row 2 ===");
            System.out.println("getFirstRowNum(): " + sheet.getFirstRowNum());
            System.out.println("getLastRowNum(): " + sheet.getLastRowNum());
            System.out.println("getPhysicalNumberOfRows(): " + sheet.getPhysicalNumberOfRows());
            
            // Test the loop condition from findHeaderRow
            int limit = Math.min(sheet.getLastRowNum(), 2);
            System.out.println("\nPrimary search: r <= Math.min(" + sheet.getLastRowNum() + ", 2) = " + limit);
            
            for (int r = 0; r <= limit; r++) {
                Row row = sheet.getRow(r);
                if (row != null) {
                    int nonEmpty = 0;
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        if (cell != null && cell.getCellType() != CellType.BLANK) {
                            String val = cell.toString();
                            if (!val.isBlank()) nonEmpty++;
                        }
                    }
                    System.out.println("  Row " + r + ": EXISTS, nonEmpty=" + nonEmpty + (nonEmpty >= 3 ? " <- WOULD BE SELECTED" : ""));
                } else {
                    System.out.println("  Row " + r + ": NULL (will be skipped)");
                }
            }
        }
    }
}
