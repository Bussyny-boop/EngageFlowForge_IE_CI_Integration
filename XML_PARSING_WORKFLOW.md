# XML Parsing Workflow Diagram

## Complete Data Flow

```
┌────────────────────────────────────────────────────────────────────┐
│                      EngagexmLV1 XML File                          │
│                                                                    │
│  <datasets>                           <interfaces>                │
│    <dataset name="Clinicals">           <interface component="VMP">│
│      <view>                               <rule dataset="Clinicals">│
│        <filter path="alert_type">           <purpose>SEND APNEA</purpose>│
│          <value>APNEA</value>                <settings>...</settings>│
│        </filter>                            </rule>                │
│      </view>                              </interface>             │
│    </dataset>                           </interfaces>              │
│  </datasets>                                                       │
└────────────┬───────────────────────────────────────────────────────┘
             │
             │ XmlParser.load(file)
             ▼
┌────────────────────────────────────────────────────────────────────┐
│                         Step 1: Parse Datasets                     │
│                      parseDatasets(Document)                       │
│                                                                    │
│  Input: <datasets> section                                        │
│  Output: Map<String, Map<String, ViewDefinition>>                 │
│                                                                    │
│  Example:                                                          │
│    "Clinicals" → {                                                │
│      "Alarm_included_in_APNEA" → ViewDefinition {                 │
│        filters: [alert_type = "APNEA"]                            │
│      }                                                             │
│    }                                                               │
└────────────┬───────────────────────────────────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────────────────────────────┐
│                      Step 2: Parse Interfaces                      │
│                     parseInterfaces(Document)                      │
│                                                                    │
│  Input: <interfaces> section                                      │
│  Process:                                                          │
│    For each <interface component="...">                           │
│      For each <rule dataset="..." active="true">                  │
│        → parseRule()                                               │
│        → Create RuleData object                                    │
│        → Add to collectedRules list                                │
│                                                                    │
│  Output: List<RuleData> collectedRules                            │
└────────────┬───────────────────────────────────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────────────────────────────┐
│                    Step 3: Extract Rule Data                       │
│                     parseRule(Element, String)                     │
│                                                                    │
│  For each rule:                                                    │
│    ✓ Extract dataset ("Clinicals", "NurseCalls", "Orders")       │
│    ✓ Extract component ("VMP", "DataUpdate", etc.)               │
│    ✓ Extract purpose (description)                                │
│    ✓ Extract defer-delivery-by (timing)                           │
│    ✓ Extract trigger-on (create/update)                           │
│    ✓ Parse condition views                                         │
│    ✓ Parse settings JSON                                           │
│    ✓ Lookup view definitions                                       │
│    ✓ Extract alert types from filters                             │
│    ✓ Extract facilities from filters                              │
│    ✓ Extract units from filters                                   │
│    ✓ Extract state from filters                                   │
│    ✓ Extract role from filters                                    │
│                                                                    │
│  RuleData {                                                        │
│    dataset: "Clinicals"                                           │
│    component: "VMP"                                                │
│    alertTypes: ["APNEA"]                                          │
│    facilities: ["Northland"]                                      │
│    units: ["NICU"]                                                │
│    state: "Primary"                                               │
│    role: null                                                      │
│    settings: {priority: "0", ttl: "5", ...}                       │
│    deferDeliveryBy: null                                           │
│  }                                                                 │
└────────────┬───────────────────────────────────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────────────────────────────┐
│                 Step 4: Merge State-Based Rules                    │
│                    mergeStateBasedRules()                          │
│                                                                    │
│  Group Rules By: dataset + alert types                            │
│                                                                    │
│  Example Group:                                                    │
│    Clinicals + APNEA                                              │
│      ├─ Rule 1: VMP, Primary state → R1 = "RN Bedside"          │
│      ├─ Rule 2: DataUpdate, defer=30 → T2 = "30SEC"             │
│      ├─ Rule 3: VMP, Secondary state → R2 = "Charge Nurse"      │
│      ├─ Rule 4: DataUpdate, defer=60 → T3 = "60SEC"             │
│      └─ Rule 5: VMP, Tertiary state → R3 = "Manager"            │
│                                                                    │
│  Merge Logic:                                                      │
│    1. Separate SEND rules (have destination/role)                 │
│    2. Separate ESCALATE rules (DataUpdate with defer)             │
│    3. Map states:                                                  │
│       - Primary state → R1 column                                  │
│       - Secondary state → R2 column                                │
│       - Tertiary state → R3 column                                 │
│    4. Map escalation timing:                                       │
│       - defer from Primary check → T2 column                       │
│       - defer from Secondary check → T3 column                     │
│    5. Create single FlowRow with all data                         │
│                                                                    │
│  Output: Merged FlowRow                                            │
│    type: "Clinicals"                                              │
│    configGroup: "Clinicals_NICU"                                  │
│    alarmName: "APNEA"                                             │
│    deviceA: "VMP"                                                 │
│    t1: "Immediate"                                                │
│    r1: "RN Bedside"                                               │
│    t2: "30SEC"                                                    │
│    r2: "Charge Nurse"                                             │
│    t3: "60SEC"                                                    │
│    r3: "Manager"                                                  │
│    priority: "Urgent"                                             │
│    ttl: "5"                                                        │
└────────────┬───────────────────────────────────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────────────────────────────┐
│                    Step 5: Create Unit Rows                        │
│                       createUnitRows()                             │
│                                                                    │
│  Input: facilityToUnits Map                                       │
│    "Northland" → ["NICU", "ICU", "4E"]                           │
│    "GICH" → ["Emergency", "Surgery"]                              │
│                                                                    │
│  For each facility/unit combination:                              │
│    Create UnitRow {                                               │
│      facility: "Northland"                                        │
│      unitNames: "NICU"                                            │
│      nurseGroup: "NurseCalls_NICU"   (if has nurse rules)        │
│      clinGroup: "Clinicals_NICU"     (if has clinical rules)     │
│      ordersGroup: "Orders_NICU"      (if has order rules)        │
│    }                                                               │
│                                                                    │
│  Output: List<UnitRow> units                                      │
└────────────┬───────────────────────────────────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────────────────────────────┐
│                     Parser Output Collections                      │
│                                                                    │
│  ✓ units: List<UnitRow>                                          │
│  ✓ nurseCalls: List<FlowRow> (type="NurseCalls")                │
│  ✓ clinicals: List<FlowRow> (type="Clinicals")                  │
│  ✓ orders: List<FlowRow> (type="Orders")                        │
└────────────┬───────────────────────────────────────────────────────┘
             │
             │ xmlParser.getUnits()
             │ xmlParser.getNurseCalls()
             │ xmlParser.getClinicals()
             │ xmlParser.getOrders()
             ▼
┌────────────────────────────────────────────────────────────────────┐
│                   AppController Integration                        │
│                    (Line 825+ in AppController.java)              │
│                                                                    │
│  parser.units.addAll(xmlParser.getUnits())                        │
│  parser.nurseCalls.addAll(xmlParser.getNurseCalls())              │
│  parser.clinicals.addAll(xmlParser.getClinicals())                │
│  parser.orders.addAll(xmlParser.getOrders())                      │
│                                                                    │
│  ↓ refreshTables()                                                 │
│                                                                    │
│  unitsFullList → tableUnits                                       │
│  nurseCallsFullList → tableNurseCalls                             │
│  clinicalsFullList → tableClinicals                               │
│  ordersFullList → tableOrders                                     │
└────────────┬───────────────────────────────────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────────────────────────────┐
│                         JavaFX GUI Display                         │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │ Tab: Units                                               │    │
│  ├──────────┬──────────┬─────────────┬─────────────────────┤    │
│  │ Facility │ Unit     │ Nurse Group │ Clinical Group      │    │
│  ├──────────┼──────────┼─────────────┼─────────────────────┤    │
│  │Northland │ NICU     │NurseCalls_..│Clinicals_NICU       │    │
│  │Northland │ ICU      │NurseCalls_..│Clinicals_ICU        │    │
│  └──────────┴──────────┴─────────────┴─────────────────────┘    │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │ Tab: Nurse Calls                                         │    │
│  ├──┬─────────┬────────┬─────┬────┬────┬────┬────┬────┬────┤    │
│  │✓│Config   │Alarm   │Pri  │Dev │T1  │R1  │T2  │R2  │... │    │
│  ├──┼─────────┼────────┼─────┼────┼────┼────┼────┼────┼────┤    │
│  │✓│NurseCa..│Patient │Norm │VMP │Imm │Prim│2MIN│Chge│... │    │
│  └──┴─────────┴────────┴─────┴────┴────┴────┴────┴────┴────┘    │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │ Tab: Clinicals                                           │    │
│  ├──┬─────────┬────────┬─────┬────┬────┬────┬────┬────┬────┤    │
│  │✓│Config   │Alarm   │Pri  │Dev │T1  │R1  │T2  │R2  │T3  │    │
│  ├──┼─────────┼────────┼─────┼────┼────┼────┼────┼────┼────┤    │
│  │✓│Clinical.│APNEA   │Urg  │VMP │Imm │RN B│30S │Chrg│60S │    │
│  └──┴─────────┴────────┴─────┴────┴────┴────┴────┴────┴────┘    │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │ Tab: Orders                                              │    │
│  ├──┬─────────┬────────────┬─────┬────┬────┬──────┬────────┤    │
│  │✓│Config   │Alarm       │Pri  │Dev │T1  │R1    │TTL     │    │
│  ├──┼─────────┼────────────┼─────┼────┼────┼──────┼────────┤    │
│  │✓│Orders_..│Epic Order  │Norm │VMP │Imm │g-123 │20160   │    │
│  └──┴─────────┴────────────┴─────┴────┴────┴──────┴────────┘    │
└────────────────────────────────────────────────────────────────────┘
```

## Key Decision Points

### Decision 1: Active Rule Filter
```
Parse Rule?
  ├─ YES → active="true"  ✅ Process rule
  └─ NO  → active="false" ❌ Skip rule
```

### Decision 2: Rule Type Detection
```
Has state in condition?
  ├─ YES → State-based rule
  │         ├─ Has destination/role? 
  │         │   ├─ YES → SEND rule (creates recipient)
  │         │   └─ NO  → ESCALATE rule (provides timing)
  │         └─ Group by alert type → Merge
  └─ NO  → Simple rule (no escalation)
            └─ Create single FlowRow
```

### Decision 3: Component to Device Mapping
```
component attribute
  ├─ "VMP" → deviceA = "VMP"
  ├─ "DataUpdate" → deviceA = "Edge"
  ├─ "Vocera" → deviceA = "Vocera"
  ├─ "XMPP" → deviceA = "XMPP"
  └─ other → deviceA = [component value]
```

### Decision 4: Dataset to Flow Type
```
dataset attribute
  ├─ "NurseCalls" → type = "NurseCalls" → Add to nurseCalls list
  ├─ "Clinicals" → type = "Clinicals" → Add to clinicals list
  ├─ "Orders" → type = "Orders" → Add to orders list
  └─ other → Skip
```

## Performance Notes

### Parsing EngagexmLV1 File
- **File Size**: 44,132 lines
- **Total Datasets**: ~100+
- **Relevant Datasets**: 3 (Clinicals, NurseCalls, Orders)
- **Total Rules**: ~200-300
- **Active Rules**: ~100-150
- **Parse Time**: < 5 seconds
- **Memory**: Minimal (parsed objects only)

### Output Statistics
- **Units**: 10-15 rows
- **Nurse Calls**: 40-50 rows
- **Clinicals**: 70-80 rows
- **Orders**: 20-30 rows
- **Total FlowRows**: 130-160 rows

## Error Handling

```
Try to parse XML
  ├─ XML malformed → throw Exception → Show error dialog
  ├─ No active rules → Empty lists → Show warning
  ├─ View not found → Skip filter → Log warning
  └─ JSON parse fails → Use empty settings → Continue
```

## Summary

This workflow transforms **complex, multi-file XML configurations** into **simple, editable GUI tables**:

1. **XML Complexity**: 44,000 lines, nested structures, multiple interfaces
2. **Parser Intelligence**: Merges escalations, extracts types, maps facilities
3. **GUI Simplicity**: Clean tables with checkboxes and text fields
4. **User Benefit**: Edit healthcare alert routing visually, no XML knowledge needed

**Parser Status**: ✅ Fully Functional  
**Integration Status**: ✅ Complete  
**Documentation Status**: ✅ Comprehensive
