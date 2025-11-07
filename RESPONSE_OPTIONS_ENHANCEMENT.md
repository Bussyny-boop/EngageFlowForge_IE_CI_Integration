# Response Options Enhancement - Implementation Summary

## Overview

Enhanced the response options logic across all flow types (NurseCalls, Clinicals, and Orders) to support additional response variations including "Reject" and ensure consistent behavior.

## Changes Made

### 1. Added "Reject" Support

The system now recognizes "Reject" as a valid decline response option:

- **Decline Badge Phrase Priority**: `Decline > Reject > Escalate`
- When both "Decline" and "Reject" are present, "Decline" takes precedence
- When both "Reject" and "Escalate" are present, "Reject" takes precedence
- When only "Reject" is specified, it's used as the decline badge phrase

### 2. Extended Response Logic to Orders Flows

Previously, Orders flows had hardcoded parameters and did not support the response options logic. Now:

- Orders flows use the same response option parsing logic as NurseCalls and Clinicals
- Response options from Excel are properly parsed and converted to badge phrases
- All response combinations (Accept/Acknowledge + Decline/Reject/Escalate) work for Orders

### 3. Unified Response Option Handling

All three flow types (NurseCalls, Clinicals, Orders) now use the same unified logic for:
- Parsing response options from Excel
- Determining accept and decline badge phrases
- Building parameter attributes with proper response types

## Response Option Examples

### Example 1: Accept, Reject
```json
{
  "name": "acceptBadgePhrases",
  "value": "[\"Accept\"]"
},
{
  "name": "declineBadgePhrases",
  "value": "[\"Reject\"]"
}
```

### Example 2: Acknowledge, Escalate
```json
{
  "name": "acceptBadgePhrases",
  "value": "[\"Acknowledge\"]"
},
{
  "name": "declineBadgePhrases",
  "value": "[\"Escalate\"]"
}
```

### Example 3: Accept, Decline
```json
{
  "name": "acceptBadgePhrases",
  "value": "[\"Accept\"]"
},
{
  "name": "declineBadgePhrases",
  "value": "[\"Decline\"]"
}
```

### Example 4: Acknowledge, Reject
```json
{
  "name": "acceptBadgePhrases",
  "value": "[\"Acknowledge\"]"
},
{
  "name": "declineBadgePhrases",
  "value": "[\"Reject\"]"
}
```

## Technical Implementation

### Code Changes in `ExcelParserV5.java`

1. **Response Option Parsing** (lines 1135-1157):
   - Added `hasReject` flag to detect "Reject" in response options
   - Updated decline phrase selection logic: `Decline > Reject > Escalate`
   - Combined flags to treat Acknowledge as Accept and Decline/Reject as Escalate for flow logic

2. **Orders Flow Integration** (lines 1213-1270):
   - Removed the early return for Orders flows
   - Orders flows now go through the full response option logic
   - Added conditional message/parameter handling for Orders vs NurseCalls vs Clinicals
   - Preserved Orders-specific messages and field references

3. **Message Handling** (lines 1228-1267):
   - Added three-way conditional for message parameters: Orders, NurseCalls, Clinicals
   - Each flow type gets appropriate patient/clinical field references
   - Orders use `#{patient.*}` fields instead of `#{bed.patient.*}`
   - placeUid is excluded for Orders flows (not applicable)

## Test Coverage

Created comprehensive test suite in `RejectResponseTest.java`:

1. ✅ `rejectSetsDeclineBadgePhrasesToReject` - Verifies Reject is properly set as decline badge phrase
2. ✅ `acknowledgeRejectCombination` - Tests Acknowledge + Reject combination
3. ✅ `declineHasPriorityOverReject` - Confirms Decline takes precedence over Reject
4. ✅ `rejectHasPriorityOverEscalate` - Confirms Reject takes precedence over Escalate
5. ✅ `ordersFlowSupportsResponseOptions` - Verifies Orders flows handle response options
6. ✅ `ordersFlowWithReject` - Tests Reject option specifically for Orders
7. ✅ `clinicalsFlowWithReject` - Tests Reject option specifically for Clinicals

All existing tests continue to pass, ensuring backward compatibility.

## Supported Response Options

### Accept Phrases
- "Accept" → `acceptBadgePhrases: ["Accept"]`
- "Acknowledge" → `acceptBadgePhrases: ["Acknowledge"]`

### Decline Phrases (in priority order)
1. "Decline" → `declineBadgePhrases: ["Decline"]`
2. "Reject" → `declineBadgePhrases: ["Reject"]`
3. "Escalate" → `declineBadgePhrases: ["Escalate"]`

### Valid Combinations
All combinations work across all flow types:
- Accept, Escalate
- Accept, Decline
- Accept, Reject
- Acknowledge, Escalate
- Acknowledge, Decline
- Acknowledge, Reject
- Accept, Escalate, Call Back
- Acknowledge, Decline, Call Back
- And more...

## Flow-Specific Behavior

### NurseCalls
- Uses `#{bed.*}` field references
- Decline text: "Decline Primary" when hasEscalate is true
- Includes destination names for all 5 recipients

### Clinicals
- Uses `#{bed.*}` and `#{clinical_patient.*}` field references
- Decline text: "Decline" when hasEscalate is true
- Includes NoCaregiver destination parameters

### Orders
- Uses `#{patient.*}` field references
- Decline text: "Decline" when hasEscalate is true
- Includes destination names for all 5 recipients
- No placeUid parameter
- Different message format with procedure information

## Backward Compatibility

All existing functionality is preserved:
- Existing response options continue to work as before
- No changes to NurseCalls or Clinicals behavior for existing options
- Orders flows maintain their specific message formats and field references
- All existing tests pass without modification
