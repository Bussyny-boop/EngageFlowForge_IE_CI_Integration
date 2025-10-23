package com.example.exceljson;


import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;


import java.util.*;


public class HeaderFinder {
/** Find the header row by scanning first N rows for the one that contains the most expected headers. */
public static int findHeaderRow(Sheet sheet, Set<String> expectedTokens, int scanRows) {
int bestRow = -1;
int bestScore = 0;
for (int r = 0; r < Math.min(scanRows, sheet.getPhysicalNumberOfRows()); r++) {
Row row = sheet.getRow(r);
if (row == null) continue;
int nonEmpty = 0; int hits = 0;
for (int c = 0; c < row.getLastCellNum(); c++) {
String val = getString(row, c);
if (!val.isBlank()) {
nonEmpty++;
String norm = normalize(val);
if (expectedTokens.contains(norm)) hits++;
}
}
int score = hits * 10 + nonEmpty; // weigh matches
if (score > bestScore) { bestScore = score; bestRow = r; }
}
return bestRow;
}


public static String getString(Row row, int col) {
if (row == null) return "";
try {
var cell = row.getCell(col);
if (cell == null) return "";
return switch (cell.getCellType()) {
case STRING -> cell.getStringCellValue();
case NUMERIC -> String.valueOf(cell.getNumericCellValue());
case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
default -> "";
};
} catch (Exception e) { return ""; }
}


public static String normalize(String s) {
return s.trim().toLowerCase().replaceAll("[^a-z0-9]+", " ").replaceAll(" +", " ").trim();
}
}
