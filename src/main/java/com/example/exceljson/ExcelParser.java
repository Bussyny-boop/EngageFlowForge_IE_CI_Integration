package com.example.exceljson;
}
return "";
}

private void parseUnitBreakdown() {
Sheet sheet = wb.getSheet("Unit Breakdown");
if (sheet == null) return;
Set<String> expected = new HashSet<>();
expected.addAll(MappingAliases.FACILITY);
expected.addAll(MappingAliases.UNIT_NAME);


int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 20);
if (headerRowIdx < 0) return;
Row header = sheet.getRow(headerRowIdx);
Map<String,Integer> hmap = headerIndex(header);


for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
Row row = sheet.getRow(r);
if (row == null) continue;
String facility = get(row, hmap, MappingAliases.FACILITY);
String unit = get(row, hmap, MappingAliases.UNIT_NAME);
if (facility.isBlank() && unit.isBlank()) continue;


Map<String,String> map = new LinkedHashMap<>();
map.put("Facility", facility);
map.put("Common Unit Name", unit);
unitRows.add(map);
}
}


private void parseNurseCall() {
Sheet sheet = wb.getSheet("Nurse Call");
if (sheet == null) return;
Set<String> expected = new HashSet<>() {{
addAll(MappingAliases.CFG_GROUP);
addAll(MappingAliases.ALERT_NAME_COMMON);
addAll(MappingAliases.SENDING_NAME);
addAll(MappingAliases.PRIORITY);
}};
int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 30);
if (headerRowIdx < 0) return;
Row header = sheet.getRow(headerRowIdx);
Map<String,Integer> hmap = headerIndex(header);


for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
Row row = sheet.getRow(r);
if (row == null) continue;
String cfg = get(row, hmap, MappingAliases.CFG_GROUP);
String alert = get(row, hmap, MappingAliases.ALERT_NAME_COMMON);
String send = get(row, hmap, MappingAliases.SENDING_NAME);
if (cfg.isBlank() && alert.isBlank() && send.isBlank()) continue;


Map<String,String> m = new LinkedHashMap<>();
m.put("Configuration Group", cfg);
m.put("Common Alert or Alarm Name", alert);
m.put("Sending System Alert Name",
