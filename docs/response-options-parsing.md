# Dynamic Response Options Parsing

## Overview

The `buildParamAttributesQuoted` method in `ExcelParserV5.java` has been enhanced to dynamically read and interpret the `responseOptions` field from Excel sheets, replacing the previous hardcoded response logic.

## Changes Made

### 1. Response Options Parser

A new `ResponseOptions` class and `parseResponseOptions()` method were added to dynamically parse the `responseOptions` field from Excel:

```java
private static final class ResponseOptions {
    final boolean noResponse;
    final boolean hasAccept;
    final boolean hasEscalate;
    final boolean hasCallBack;
    final String responseType;
}

private ResponseOptions parseResponseOptions(String responseOptionsText)
```

### 2. Supported Response Option Combinations

The parser supports the following combinations (case-insensitive, whitespace around commas ignored):

- **"No Response"**: Sets `responseType` to `"None"`, no response parameters added
- **"Accept"**: Sets `responseType` to `"Accept"`, adds accept parameters
- **"Accept,Escalate"**: Sets `responseType` to `"Accept/Decline"`, adds both accept and escalate parameters
- **"Accept,Escalate,Call Back"**: Sets `responseType` to `"Accept/Decline"`, adds accept, escalate, and callback parameters

### 3. Parameter Ordering

The ringtone parameter (`alertSound`) is now placed AFTER response parameters and BEFORE standard NurseCall attributes like `breakThrough`. The order is now:

1. Response parameters (if applicable):
   - `accept` and `acceptBadgePhrases` (if Accept is present)
   - `acceptAndCall` (if Call Back is present)
   - `decline` and `declineBadgePhrases` (if Escalate is present)
2. **`alertSound`** (ringtone) - MOVED HERE
3. Standard NurseCall attributes:
   - `breakThrough`
   - `enunciate`
   - `message`
   - etc.

### 4. Response Type Mapping

| Response Options | responseType Value | Parameters Added |
|-----------------|-------------------|------------------|
| "No Response" or empty | "None" | None |
| "Accept" only | "Accept" | accept, acceptBadgePhrases, respondingLine, respondingUser, responsePath |
| "Accept,Escalate" | "Accept/Decline" | accept, acceptBadgePhrases, decline, declineBadgePhrases, respondingLine, respondingUser, responsePath |
| "Accept,Escalate,Call Back" | "Accept/Decline" | accept, acceptBadgePhrases, acceptAndCall, decline, declineBadgePhrases, respondingLine, respondingUser, responsePath |

## Testing

Seven new tests were added in `ResponseOptionsTest.java` to verify:

1. ✅ "No Response" sets `responseType` to "None"
2. ✅ "Accept" only sets `responseType` to "Accept"
3. ✅ "Accept,Escalate" sets `responseType` to "Accept/Decline"
4. ✅ "Accept,Escalate,Call Back" includes all parameters
5. ✅ Case and whitespace are ignored
6. ✅ Ringtone appears after response parameters, before breakThrough
7. ✅ Empty response options default to "None"

All 34 tests pass (27 original + 7 new).

## Manual Verification

Manual testing with a sample Excel file confirmed:

- ✅ Correct responseType for each combination
- ✅ Correct parameters added based on response options
- ✅ Correct parameter ordering (response params → alertSound → standard attributes)
- ✅ Case-insensitive parsing
- ✅ Whitespace tolerance around commas

## Example JSON Output

### "Accept,Escalate,Call Back" Example

```json
"parameterAttributes": [
  {
    "name": "accept",
    "value": "\"Accepted\""
  },
  {
    "name": "acceptBadgePhrases",
    "value": "[\"Accept\"]"
  },
  {
    "name": "acceptAndCall",
    "value": "\"Call Back\""
  },
  {
    "name": "decline",
    "value": "\"Decline Primary\""
  },
  {
    "name": "declineBadgePhrases",
    "value": "[\"Escalate\"]"
  },
  {
    "name": "alertSound",
    "value": "\"Tone 4\""
  },
  {
    "name": "breakThrough",
    "value": "\"voceraAndDevice\""
  },
  ...
  {
    "name": "responseType",
    "value": "\"Accept/Decline\""
  },
  {
    "name": "respondingLine",
    "value": "\"responses.line.number\""
  },
  ...
]
```

## Backward Compatibility

The changes maintain backward compatibility with existing Excel files. If the `responseOptions` field is empty or not present, the system defaults to "No Response" behavior.
