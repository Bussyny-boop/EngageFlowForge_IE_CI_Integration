package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BeyondLastRowTest {
    
    @Test
    void testAccessingRowBeyondLastRowNum() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Test");
            
            // Only create row 0
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("OnlyRow");
            
            System.out.println("\n=== Sheet with only row 0 ===");
            System.out.println("getLastRowNum(): " + sheet.getLastRowNum());
            
            // Try to access rows beyond lastRowNum
            System.out.println("\nAccessing row 0 (exists): " + (sheet.getRow(0) != null));
            System.out.println("Accessing row 1 (beyond lastRowNum): " + (sheet.getRow(1) != null));
            System.out.println("Accessing row 2 (beyond lastRowNum): " + (sheet.getRow(2) != null));
            System.out.println("Accessing row 100 (way beyond): " + (sheet.getRow(100) != null));
            
            // This should all return null, not throw exceptions
            assertNull(sheet.getRow(1));
            assertNull(sheet.getRow(2));
            assertNull(sheet.getRow(100));
        }
    }
}
