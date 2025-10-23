package com.example.exceljson;
}
}
});


btnExport.setOnAction(e -> {
if (parser == null) return;
try {
Map<String, Object> rootJson = parser.buildJson();
ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
String jsonText = ow.writeValueAsString(rootJson);


FileChooser fc = new FileChooser();
fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
fc.setInitialFileName("export.json");
File out = fc.showSaveDialog(null);
if (out != null) {
Files.writeString(out.toPath(), jsonText);
showInfo("Saved", "JSON exported to:\n" + out.getAbsolutePath());
}
} catch (Exception ex) {
showError("Export failed", ex);
}
});


return root;
}


private void populateTables() {
// Unit Breakdown
List<Map<String, String>> units = parser.getUnitBreakdownRows();
FXTableUtils.populate(unitTable, units);


// Nurse Call
List<Map<String, String>> nc = parser.getNurseCallRows();
FXTableUtils.populate(nurseCallTable, nc);


// Patient Monitoring
List<Map<String, String>> pm = parser.getPatientMonitoringRows();
FXTableUtils.populate(patientMonTable, pm);
}


private void buildAndPreviewJSON() throws Exception {
Map<String, Object> rootJson = parser.buildJson();
ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
jsonPreview.setText(ow.writeValueAsString(rootJson));
}


private void showError(String title, Exception ex) {
ex.printStackTrace();
Alert a = new Alert(Alert.AlertType.ERROR);
a.setTitle(title);
a.setHeaderText(title);
a.setContentText(ex.getMessage());
a.showAndWait();
}
private void showInfo(String title, String msg) {
Alert a = new Alert(Alert.AlertType.INFORMATION);
a.setTitle(title);
a.setHeaderText(title);
a.setContentText(msg);
a.showAndWait();
}
}
