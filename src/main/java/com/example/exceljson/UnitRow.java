package com.example.exceljson;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * UnitRow
 * - Represents a row in Unit Breakdown tab
 * - Configuration groups are strings (not checkboxes)
 */
public class UnitRow {
    private final StringProperty facility = new SimpleStringProperty("");
    private final StringProperty unitName = new SimpleStringProperty("");
    private final StringProperty nurseCallGroup = new SimpleStringProperty("");
    private final StringProperty patientMonitoringGroup = new SimpleStringProperty("");
    private final StringProperty noCaregiverGroup = new SimpleStringProperty("");

    public UnitRow() { }

    public UnitRow(String facility, String unitName,
                   String nurseCallGroup, String patientMonitoringGroup,
                   String noCaregiverGroup) {
        setFacility(facility);
        setUnitName(unitName);
        setNurseCallGroup(nurseCallGroup);
        setPatientMonitoringGroup(patientMonitoringGroup);
        setNoCaregiverGroup(noCaregiverGroup);
    }

    public String getFacility() { return facility.get(); }
    public void setFacility(String v) { facility.set(v == null ? "" : v); }
    public StringProperty facilityProperty() { return facility; }

    public String getUnitName() { return unitName.get(); }
    public void setUnitName(String v) { unitName.set(v == null ? "" : v); }
    public StringProperty unitNameProperty() { return unitName; }

    public String getNurseCallGroup() { return nurseCallGroup.get(); }
    public void setNurseCallGroup(String v) { nurseCallGroup.set(v == null ? "" : v); }
    public StringProperty nurseCallGroupProperty() { return nurseCallGroup; }

    public String getPatientMonitoringGroup() { return patientMonitoringGroup.get(); }
    public void setPatientMonitoringGroup(String v) { patientMonitoringGroup.set(v == null ? "" : v); }
    public StringProperty patientMonitoringGroupProperty() { return patientMonitoringGroup; }

    public String getNoCaregiverGroup() { return noCaregiverGroup.get(); }
    public void setNoCaregiverGroup(String v) { noCaregiverGroup.set(v == null ? "" : v); }
    public StringProperty noCaregiverGroupProperty() { return noCaregiverGroup; }
}
