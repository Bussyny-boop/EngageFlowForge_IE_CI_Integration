# XML Parser - Device-A and Recipient Determination

## Overview

The XML parser correctly implements the following logic for determining Device-A, recipients, alert types, and timing from Vocera Engage XML configuration files.

## Device-A Determination

**Rule**: Device-A is determined by the `component` attribute of the `<interface>` element containing the rule.

### Interface Component Mapping

| Interface Component | Device-A Value |
|---------------------|----------------|
| VMP                 | VMP            |
| DataUpdate          | Edge           |
| Vocera              | Vocera         |
| XMPP                | XMPP           |
| *Other*             | *Same as component* |

### Example

```xml
<interface component="VMP">
  <name>VMP</name>
  <rule active="true" dataset="Clinicals">
    <!-- This rule will create flows with Device-A = "VMP" -->
  </rule>
</interface>
```

## Recipient Determination

**Rule**: Recipients are extracted from views that contain `role.name` path filters.

### State-Based Recipients

The state value in a rule's condition determines which recipient slot (R1-R5) the role is assigned to:

- **Primary state** → R1 (1st recipient)
- **Secondary state** → R2 (2nd recipient)
- **Tertiary state** → R3 (3rd recipient)
- **Quaternary state** → R4 (4th recipient)

### Example

```xml
<!-- In datasets section -->
<view>
  <name>Role_caregiver_NURSE_is_online_with_a_VMP_phone</name>
  <filter relation="equal">
    <path>bed.locs.assignments.role.name</path>
    <value>NURSE</value>  <!-- This becomes the recipient -->
  </filter>
</view>

<!-- In interface section -->
<rule active="true" dataset="Clinicals">
  <condition>
    <view>Alarm_is_at_primary_state</view>
    <view>Role_caregiver_NURSE_is_online_with_a_VMP_phone</view>
  </condition>
  <!-- Result: R1 = "NURSE" (Primary state determines R1) -->
</rule>
```

## Alert Type Determination

**Rule**: Alert types are extracted from views that contain `alert_type` path filters.

### Multiple Alert Types

When a view filter contains multiple alert types (comma-separated), the parser creates **separate flow rows** for each alert type.

### Example

```xml
<view>
  <name>Alarm_included_in_LowHR_HighHR_APNEA</name>
  <filter relation="in">
    <path>alert_type</path>
    <value>LHR,HHR,APNEA,AFIB,ST CHANGES</value>
  </filter>
</view>
```

This creates 5 separate flow rows:
- LHR
- HHR  
- APNEA
- AFIB
- ST CHANGES

## DataUpdate Interface - Escalation Timing

**Rule**: Rules in a `DataUpdate` interface component are used for determining **escalation timing**, not for creating separate message delivery flows.

### How It Works

DataUpdate rules:
1. Do NOT have a `destination` or `role` in their settings
2. DO have `parameters` that update the alarm state
3. Use `defer-delivery-by` to specify timing

These rules are merged with the corresponding message delivery rules (from VMP/Vocera/etc. interfaces) to set the **time to next recipient**.

### Example

```xml
<interface component="DataUpdate">
  <name>DataUpdate for Alerts</name>
  
  <!-- Escalation rule: transitions from Primary to Secondary after 60 seconds -->
  <rule active="true" dataset="Clinicals">
    <purpose>ESCALATE TO SECONDARY | 30-60 | ALL ALARMS | NO PRIMARY RESPONSE | ALL UNITS</purpose>
    <trigger-on update="true"/>
    <defer-delivery-by>60</defer-delivery-by>
    <condition>
      <view>Alarm_is_at_primary_state</view>  <!-- Checking PRIMARY state -->
    </condition>
    <settings>{"parameters":[{"path":"state","value":"Secondary"}]}</settings>
  </rule>
</interface>
```

**Result**: Sets T2 (time to 2nd recipient) = "60" for all flows with both Primary and Secondary recipients.

## Unit Scope Handling

**Rule**: When views do not contain a `unit.name` path filter, the flows apply to **ALL UNITS**.

### All Units vs. Specific Units

- **No unit.name filter** → Config Group = dataset name only (e.g., "Clinicals")
- **Has unit.name filter** → Config Group = dataset_unitname (e.g., "Clinicals_CVICU")

### Example - All Units

```xml
<!-- No unit filter in views -->
<rule active="true" dataset="Clinicals">
  <condition>
    <view>Alarm_is_active</view>
    <!-- No unit-specific view -->
  </condition>
</rule>
```

Result: Applies to all units in the facility.

### Example - Specific Unit

```xml
<!-- View with unit filter -->
<view>
  <name>Unit_CVICU</name>
  <filter relation="equal">
    <path>bed.room.unit.name</path>
    <value>CVICU</value>
  </filter>
</view>

<rule active="true" dataset="Clinicals">
  <condition>
    <view>Unit_CVICU</view>
  </condition>
</rule>
```

Result: Applies only to CVICU unit.

## Complete Example

This example demonstrates all concepts together:

```xml
<interface component="VMP">
  <name>VMP</name>
  
  <!-- PRIMARY: Send to NURSE immediately -->
  <rule active="true" dataset="Clinicals">
    <trigger-on update="true"/>
    <defer-delivery-by>1</defer-delivery-by>
    <condition>
      <view>Alarm_included_in_LowHR_HighHR_APNEA</view>  <!-- 3 alert types -->
      <view>Alarm_is_at_primary_state</view>               <!-- → R1 -->
      <view>Role_caregiver_NURSE</view>                    <!-- → R1 = "NURSE" -->
    </condition>
    <settings>{"destination":"...","priority":"2"}</settings>
  </rule>
  
  <!-- SECONDARY: Send to NURSE BUDDY -->
  <rule active="true" dataset="Clinicals">
    <trigger-on update="true"/>
    <defer-delivery-by>1</defer-delivery-by>
    <condition>
      <view>Alarm_included_in_LowHR_HighHR_APNEA</view>  <!-- Same 3 alert types -->
      <view>Alarm_is_at_secondary_state</view>             <!-- → R2 -->
      <view>Role_caregiver_NURSE_BUDDY</view>              <!-- → R2 = "NURSE BUDDY" -->
    </condition>
    <settings>{"destination":"...","priority":"2"}</settings>
  </rule>
</interface>

<interface component="DataUpdate">
  <name>DataUpdate for Alerts</name>
  
  <!-- ESCALATE: After 60 seconds, escalate from Primary to Secondary -->
  <rule active="true" dataset="Clinicals">
    <trigger-on update="true"/>
    <defer-delivery-by>60</defer-delivery-by>        <!-- → T2 = "60" -->
    <condition>
      <view>Alarm_is_at_primary_state</view>         <!-- Source state -->
    </condition>
    <settings>{"parameters":[{"path":"state","value":"Secondary"}]}</settings>
  </rule>
</interface>
```

**Result**: Creates 3 flow rows (one per alert type):

| Alarm Name | Device-A | R1 | R2 | T1 | T2 |
|------------|----------|----|----|----|----|
| LHR | VMP | NURSE | NURSE BUDDY | (empty) | 60 |
| HHR | VMP | NURSE | NURSE BUDDY | (empty) | 60 |
| APNEA | VMP | NURSE | NURSE BUDDY | (empty) | 60 |

## Testing

See `DataUpdateInterfaceTest.java` for comprehensive unit tests covering all these scenarios.
