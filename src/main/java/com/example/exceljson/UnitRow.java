package com.example.exceljson;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class UnitRow {
    private final StringProperty facility = new SimpleStringProperty("");
    private final StringProperty unitName = new SimpleStringProperty("");
    private final StringProperty configGroup = new SimpleStringProperty("");

    public UnitRow(String facility, String unitName, String configGroup) {
        this.facility.set(facility);
        this.unitName.set(unitName);
        this.configGroup.set(configGroup);
    }

    public StringProperty facilityProperty() { return facility; }
    public StringProperty unitNameProperty() { return unitName; }
    public StringProperty configGroupProperty() { return configGroup; }

    public String getFacility() { return facility.get(); }
    public void setFacility(String value) { facility.set(value); }

    public String getUnitName() { return unitName.get(); }
    public void setUnitName(String value) { unitName.set(value); }

    public String getConfigGroup() { return configGroup.get(); }
    public void setConfigGroup(String value) { configGroup.set(value); }
}
