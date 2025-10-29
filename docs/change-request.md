# Change Request Summary

This change request captures all Engage parameter updates that were implemented in
`ExcelParserV5` to resolve the quoting issues encountered during testing.

## Summary of Adjustments

1. **Alert sound handling** – Preserve Excel-provided ringtones on nurse flows so
   Engage receives the exact sound identifier supplied by clinicians.
2. **Response option badges** – Emit `acceptBadgePhrases` as a JSON array string
   (`["Accept"]`) so Engage correctly displays the Accept badge text.
3. **Boolean Engage flags** – Write `enunciate` and `popup` parameter attributes
   as plain boolean strings (`true`/`false`) to remove the double-quoted values
   that Engage rejects.
4. **Time-to-live and retract rules** – Store the `ttl` value as the literal
   number `10` and `retractRules` as the JSON array string
   `["ttlHasElapsed"]`, matching Engage’s expected schema.
5. **Tracking metadata** – Ensure Accept/Decline responses include the
   `respondingLine`, `respondingUser`, and `responsePath` attributes so
   Engage audits contain the responding caregiver information.
6. **Clinical flow defaults** – Align the clinical flow attributes with the same
   boolean, literal, and array handling used by the nurse flow to maintain
   consistent Engage serialization.

## Impact

These adjustments standardize how Engage parameter attributes are serialized,
removing previously escaped literals while keeping the original Excel-driven
content intact. The generated JSON now matches the syntax Engage expects for all
boolean, numeric, and array-based attributes, preventing runtime validation
errors.
