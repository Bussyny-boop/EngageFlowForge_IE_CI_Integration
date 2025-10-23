package com.example.exceljson;

import java.util.*;

public class MappingAliases {
    // Define normalized header aliases per logical field
    public Set<String> CFG_GROUP = set("configuration group", "config group");
    public Set<String> ALERT_NAME_COMMON = set("common alert or alarm name", "alarm name", "common alert name");
    public Set<String> SENDING_NAME = set("sending system alert name", "sending system alarm name");
    public Set<String> PRIORITY = set("priority");
    public Set<String> DEVICE_A = set("device a", "device", "device a ");
    public Set<String> RINGTONE_A = set("ringtone device a", "ringtone");
    public Set<String> T1 = set("time to 1st recipient", "time to first recipient", "delay to 1st");
    public Set<String> R1 = set("1st recipient", "first recipient");
    public Set<String> T2 = set("time to 2nd recipient", "time to second recipient", "delay to 2nd");
    public Set<String> R2 = set("2nd recipient", "second recipient");
    public Set<String> RESPONSE = set("response options", "response option");
    public Set<String> EMDAN = set("emdan compliant", "emdan compliant?");
    public Set<String> COMMENTS = set("comments", "comment");

    // Unit Breakdown
    public Set<String> FACILITY = set("facility");
    public Set<String> UNIT_NAME = set("common unit name", "unit", "unit name");
    public Set<String> UNIT_GROUPS = set("configuration group", "config group", "applies to", "workflow group", "delivery flow");

    public static Set<String> set(String... vals) {
        return new HashSet<>(Arrays.asList(vals));
    }
}
