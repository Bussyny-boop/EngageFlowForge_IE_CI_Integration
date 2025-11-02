package com.example.exceljson;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** FlowRow – editable row for NurseCalls / Clinicals tables. */
public class FlowRow {

    private final StringProperty type = new SimpleStringProperty(""); // NurseCalls or Clinicals
    private final StringProperty configGroup = new SimpleStringProperty("");
    private final StringProperty alarmName = new SimpleStringProperty("");
    private final StringProperty sendingName = new SimpleStringProperty("");
    private final StringProperty priority = new SimpleStringProperty(""); // normal/high/urgent
    private final StringProperty deviceA = new SimpleStringProperty("");
    private final StringProperty ringtone = new SimpleStringProperty("");
    private final StringProperty responseOptions = new SimpleStringProperty("");
    private final StringProperty breakThroughDND = new SimpleStringProperty("");
    private final StringProperty escalateAfter = new SimpleStringProperty("");
    private final StringProperty ttlValue = new SimpleStringProperty("");
    private final StringProperty enunciate = new SimpleStringProperty("");
    private final StringProperty emdan = new SimpleStringProperty("");
    private final StringProperty comments = new SimpleStringProperty("");

    // times + recipients (1 required, 2-4 optional)
    private final StringProperty t1 = new SimpleStringProperty("");
    private final StringProperty r1 = new SimpleStringProperty("");
    private final StringProperty t2 = new SimpleStringProperty("");
    private final StringProperty r2 = new SimpleStringProperty("");
    private final StringProperty t3 = new SimpleStringProperty("");
    private final StringProperty r3 = new SimpleStringProperty("");
    private final StringProperty t4 = new SimpleStringProperty("");
    private final StringProperty r4 = new SimpleStringProperty("");

    // getters/setters/properties
    public String getType() { return type.get(); }
    public void setType(String v) { type.set(v); }
    public StringProperty typeProperty() { return type; }

    public String getConfigGroup() { return configGroup.get(); }
    public void setConfigGroup(String v) { configGroup.set(v); }
    public StringProperty configGroupProperty() { return configGroup; }

    public String getAlarmName() { return alarmName.get(); }
    public void setAlarmName(String v) { alarmName.set(v); }
    public StringProperty alarmNameProperty() { return alarmName; }

    public String getSendingName() { return sendingName.get(); }
    public void setSendingName(String v) { sendingName.set(v); }
    public StringProperty sendingNameProperty() { return sendingName; }

    public String getPriority() { return priority.get(); }
    public void setPriority(String v) { priority.set(v); }
    public StringProperty priorityProperty() { return priority; }

    public String getDeviceA() { return deviceA.get(); }
    public void setDeviceA(String v) { deviceA.set(v); }
    public StringProperty deviceAProperty() { return deviceA; }

    public String getRingtone() { return ringtone.get(); }
    public void setRingtone(String v) { ringtone.set(v); }
    public StringProperty ringtoneProperty() { return ringtone; }

    public String getResponseOptions() { return responseOptions.get(); }
    public void setResponseOptions(String v) { responseOptions.set(v); }
    public StringProperty responseOptionsProperty() { return responseOptions; }

    public String getBreakThroughDND() { return breakThroughDND.get(); }
    public void setBreakThroughDND(String v) { breakThroughDND.set(v); }
    public StringProperty breakThroughDNDProperty() { return breakThroughDND; }

    public String getEscalateAfter() { return escalateAfter.get(); }
    public void setEscalateAfter(String v) { escalateAfter.set(v); }
    public StringProperty escalateAfterProperty() { return escalateAfter; }

    public String getTtlValue() { return ttlValue.get(); }
    public void setTtlValue(String v) { ttlValue.set(v); }
    public StringProperty ttlValueProperty() { return ttlValue; }

    public String getEnunciate() { return enunciate.get(); }
    public void setEnunciate(String v) { enunciate.set(v); }
    public StringProperty enunciateProperty() { return enunciate; }

    public String getEmdan() { return emdan.get(); }
    public void setEmdan(String v) { emdan.set(v); }
    public StringProperty emdanProperty() { return emdan; }

    public String getComments() { return comments.get(); }
    public void setComments(String v) { comments.set(v); }
    public StringProperty commentsProperty() { return comments; }

    public String getT1() { return t1.get(); }
    public void setT1(String v) { t1.set(v); }
    public StringProperty t1Property() { return t1; }

    public String getR1() { return r1.get(); }
    public void setR1(String v) { r1.set(v); }
    public StringProperty r1Property() { return r1; }

    public String getT2() { return t2.get(); }
    public void setT2(String v) { t2.set(v); }
    public StringProperty t2Property() { return t2; }

    public String getR2() { return r2.get(); }
    public void setR2(String v) { r2.set(v); }
    public StringProperty r2Property() { return r2; }

    public String getT3() { return t3.get(); }
    public void setT3(String v) { t3.set(v); }
    public StringProperty t3Property() { return t3; }

    public String getR3() { return r3.get(); }
    public void setR3(String v) { r3.set(v); }
    public StringProperty r3Property() { return r3; }

    public String getT4() { return t4.get(); }
    public void setT4(String v) { t4.set(v); }
    public StringProperty t4Property() { return t4; }

    public String getR4() { return r4.get(); }
    public void setR4(String v) { r4.set(v); }
    public StringProperty r4Property() { return r4; }
}
