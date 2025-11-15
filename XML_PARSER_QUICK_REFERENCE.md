# XML Parser Quick Reference

## 📚 Documentation Files (Read in This Order)

1. **XML_PARSING_IMPLEMENTATION_SUMMARY.md** ⭐ START HERE
   - Quick start guide
   - 5-minute overview
   - Testing instructions

2. **XML_PARSING_GUIDE.md**
   - Complete technical details
   - Field mappings
   - Integration code

3. **XML_TO_GUI_VISUAL_GUIDE.md**
   - Visual examples
   - Table layouts
   - Special cases

4. **XML_REAL_EXAMPLES.md**
   - Actual data from your EngagexmLV1 file
   - Line number references
   - Complete rule examples

5. **XML_PARSING_WORKFLOW.md**
   - Architecture diagram
   - Data flow visualization
   - Decision trees

---

## ⚡ Quick Start (30 Seconds)

### Load XML File
```java
// In GUI: Click "Load" → Select EngagexmLV1 → Done!

// Programmatically:
XmlParser parser = new XmlParser();
parser.load(new File("EngagexmLV1"));
List<FlowRow> clinicals = parser.getClinicals();
```

### Verify Success
Look for status message:
```
✅ XML Load Complete

Loaded:
  • 12 Unit rows
  • 45 Nurse Call rows
  • 78 Clinical rows
  • 23 Orders rows
```

---

## 🔑 Key Concepts

### Alert Types
- Extracted from view filters
- Examples: APNEA, Code Blue, Epic Order
- Becomes "Alarm Name" in GUI

### States
- Primary, Secondary, Tertiary, Quaternary, Quinary
- Maps to R1, R2, R3, R4, R5 columns
- Controls escalation flow

### Components
- VMP → Mobile phones → Device A = "VMP"
- DataUpdate → State control → Device A = "Edge"
- Vocera → Badges → Device A = "Vocera"
- XMPP → XMPP devices → Device A = "XMPP"

### Escalation
Multiple rules with different states → Single GUI row
- Primary rule → R1 column
- Secondary rule → R2 column
- Escalation delay → T2 column

---

## 📊 XML → GUI Mapping

| XML Location | XML Element | GUI Column | Example |
|--------------|-------------|------------|---------|
| View filter | `alert_type` value | Alarm Name | APNEA |
| Rule attribute | `dataset` | Type | Clinicals |
| Interface attribute | `component` | Device A | VMP |
| Settings JSON | `priority` | Priority | 0→Urgent |
| Settings JSON | `ttl` | TTL | 10 |
| Settings JSON | `destination` | R1/R2/R3... | Primary Nurse |
| Rule element | `defer-delivery-by` | T2/T3/T4... | 30SEC |
| Settings JSON | `displayValues` | Response Options | Accept,Decline |
| Settings JSON | `overrideDND` | Break Through DND | TRUE |
| Settings JSON | `enunciate` | Enunciate | SYSTEM_DEFAULT |

---

## 🎯 Common Patterns

### Pattern 1: Simple Immediate Alert
```xml
<rule active="true" dataset="NurseCalls">
  <trigger-on create="true"/>
  <settings>{"destination":"RN","priority":"2"}</settings>
</rule>
```
**Result:** R1=RN, T1=Immediate, Priority=Normal

### Pattern 2: Delayed Alert
```xml
<rule active="true" dataset="Clinicals">
  <trigger-on create="true"/>
  <defer-delivery-by>30</defer-delivery-by>
  <settings>{"destination":"RN"}</settings>
</rule>
```
**Result:** R1=RN, T1=30SEC

### Pattern 3: Two-Level Escalation
```xml
<!-- Send Primary -->
<rule dataset="NurseCalls">
  <condition><view>Alert_is_at_primary_state</view></condition>
  <settings>{"destination":"Staff RN"}</settings>
</rule>

<!-- Escalate to Secondary -->
<rule dataset="NurseCalls">
  <trigger-on update="true"/>
  <defer-delivery-by>120</defer-delivery-by>
  <condition><view>Alert_is_at_primary_state</view></condition>
</rule>

<!-- Send Secondary -->
<rule dataset="NurseCalls">
  <condition><view>Alert_is_at_secondary_state</view></condition>
  <settings>{"destination":"Charge RN"}</settings>
</rule>
```
**Result:** R1=Staff RN, T2=2MIN, R2=Charge RN

---

## 🛠️ Troubleshooting

| Issue | Check | Solution |
|-------|-------|----------|
| No data appears | Console errors? | Verify XML is valid |
| Missing alert types | Rule active? | Set active="true" |
| Escalation not merging | Alert types match? | Use same alert type in all state rules |
| No recipients | Component type? | DataUpdate rules don't have recipients |
| Wrong device | Interface component? | Check which interface contains the rule |

---

## 📋 Testing Checklist

- [ ] Load EngagexmLV1 file successfully
- [ ] Units table shows facilities (Northland, GICH, etc.)
- [ ] Nurse Calls shows Code Blue, Patient, etc.
- [ ] Clinicals shows APNEA, Asystole, etc.
- [ ] Orders shows Epic Order entries
- [ ] Escalation rules merge (check APNEA has R1, R2, R3)
- [ ] Config groups populated
- [ ] Status shows correct row counts

---

## 🔍 Where to Look

### In Your Code
- **Parser**: `src/main/java/com/example/exceljson/XmlParser.java`
- **Integration**: `src/main/java/com/example/exceljson/AppController.java` line 825
- **Data Model**: `src/main/java/com/example/exceljson/FlowRow.java`

### In XML File
- **Datasets**: Lines 1-15000 (view definitions)
- **Clinicals**: Line 3378
- **NurseCalls**: Line 7548  
- **Orders**: Line 10566
- **Interfaces**: Lines 37000-44132 (rules)
- **VMP Interface**: Line 40409

---

## 💡 Pro Tips

1. **Alert Types**: Look in view filters, not in rule purpose text
2. **Escalation**: DataUpdate provides timing, VMP/Vocera provide recipients
3. **States**: "Group" state ≈ "Primary" for immediate group sends
4. **Config Groups**: Auto-generated from dataset + unit name
5. **Multiple Alerts**: One view with comma-separated values = multiple rows

---

## 🚀 Advanced Features

### Merging Algorithm
```
For each alert type:
  1. Find all SEND rules (have destination/role)
  2. Find all ESCALATE rules (have defer-delivery-by)
  3. Group by state
  4. Map Primary→R1, Secondary→R2, etc.
  5. Map escalation delays→T2, T3, etc.
  6. Create single FlowRow
```

### View Filter Extraction
```
<filter relation="in">
  <path>alert_type</path>
  <value>A, B, C</value>
</filter>

→ Creates 3 separate alert types: A, B, C
→ Each gets its own FlowRow
```

### Facility/Unit Detection
```
<filter relation="equal">
  <path>bed.room.unit.facility.name</path>
  <value>Northland</value>
</filter>

→ Adds to facilityToUnits map
→ Creates UnitRow during createUnitRows()
```

---

## 📞 Support Resources

1. **Error Messages**: Check console for stack traces
2. **XML Validation**: Use XML editor to verify structure
3. **Sample Data**: Use provided EngagexmLV1 file
4. **Documentation**: See 5 guide files in project root
5. **Code Comments**: XmlParser.java has extensive inline docs

---

## ✅ Success Criteria

Your parser is working correctly if:

✅ All 3 datasets load (Clinicals, NurseCalls, Orders)  
✅ Escalation rules merge into single rows  
✅ Alert types appear in Alarm Name column  
✅ Timing values appear in T1, T2, T3 columns  
✅ Recipients appear in R1, R2, R3 columns  
✅ Facilities and units populate Units table  
✅ Config groups appear in filter dropdowns  

---

## 📖 Example Flow

```
XML: <filter><value>APNEA, Asystole</value></filter>
  ↓
Parser: Splits into ["APNEA", "Asystole"]
  ↓
GUI: Two rows in Clinicals table
  Row 1: Alarm Name = APNEA
  Row 2: Alarm Name = Asystole
```

---

## 🎓 Learning Path

1. **Beginner**: Read IMPLEMENTATION_SUMMARY.md (this is you!)
2. **Intermediate**: Review VISUAL_GUIDE.md for examples
3. **Advanced**: Study REAL_EXAMPLES.md with line numbers
4. **Expert**: Analyze WORKFLOW.md and PARSING_GUIDE.md

---

**Current Status**: ✅ Parser fully functional and integrated  
**Your Next Step**: Click "Load" and select EngagexmLV1! 🚀
