package com.example.exceljson;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FlowRow {
    private final StringProperty configGroup = new SimpleStringProperty("");
    private final StringProperty alarmName = new SimpleStringProperty("");
    private final StringProperty priority = new SimpleStringProperty("");
    private final StringProperty ringtone = new SimpleStringProperty("");
    private final StringProperty responseOptions = new SimpleStringProperty("");
    private final StringProperty r1 = new SimpleStringProperty("");
    private final StringProperty r2 = new SimpleStringProperty("");
    private final StringProperty r3 = new SimpleStringProperty("");
    private final StringProperty r4 = new SimpleStringProperty("");
    private final StringProperty r5 = new SimpleStringProperty("");
    private final StringProperty failSafe = new SimpleStringProperty("");

    public FlowRow() {}

    public FlowRow(String configGroup, String alarmName, String priority,
                   String ringtone, String responseOptions,
                   String r1, String r2, String r3, String r4, String failSafe) {
        this.configGroup.set(configGroup);
        this.alarmName.set(alarmName);
        this.priority.set(priority);
        this.ringtone.set(ringtone);
        this.responseOptions.set(responseOptions);
        this.r1.set(r1);
        this.r2.set(r2);
        this.r3.set(r3);
        this.r4.set(r4);
        setR5(failSafe);
    }

    public StringProperty configGroupProperty() { return configGroup; }
    public StringProperty alarmNameProperty() { return alarmName; }
    public StringProperty priorityProperty() { return priority; }
    public StringProperty ringtoneProperty() { return ringtone; }
    public StringProperty responseOptionsProperty() { return responseOptions; }
    public StringProperty r1Property() { return r1; }
    public StringProperty r2Property() { return r2; }
    public StringProperty r3Property() { return r3; }
    public StringProperty r4Property() { return r4; }
    public StringProperty r5Property() { return r5; }
    public StringProperty failSafeProperty() { return failSafe; }

    public String getConfigGroup() { return configGroup.get(); }
    public void setConfigGroup(String v) { configGroup.set(v); }

    public String getAlarmName() { return alarmName.get(); }
    public void setAlarmName(String v) { alarmName.set(v); }

    public String getPriority() { return priority.get(); }
    public void setPriority(String v) { priority.set(v); }

    public String getRingtone() { return ringtone.get(); }
    public void setRingtone(String v) { ringtone.set(v); }

    public String getResponseOptions() { return responseOptions.get(); }
    public void setResponseOptions(String v) { responseOptions.set(v); }

    public String getR1() { return r1.get(); }
    public void setR1(String v) { r1.set(v); }

    public String getR2() { return r2.get(); }
    public void setR2(String v) { r2.set(v); }

    public String getR3() { return r3.get(); }
    public void setR3(String v) { r3.set(v); }

    public String getR4() { return r4.get(); }
    public void setR4(String v) { r4.set(v); }

    public String getR5() { return r5.get(); }
    public void setR5(String v) {
        r5.set(v);
        failSafe.set(v);
    }

    public String getFailSafe() { return failSafe.get(); }
    public void setFailSafe(String v) {
        failSafe.set(v);
        r5.set(v);
    }
}
