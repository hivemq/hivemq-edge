# EDG-173 — Task Brief

## Linear Issue
https://linear.app/hivemq/issue/EDG-173/ajv-schema-with-key-or-id-already-exists-error-when-uploading

## Summary
Fix intermittent `AJV: schema with key or id "..." already exists` error thrown when
uploading a destination schema in the mapping editor.

## Root Cause (one sentence)
`decodeDataUriJsonSchema()` calls `validator.ajv.compile(json)` on a **global singleton**
AJV instance; when the uploaded schema has a `$id`, AJV caches it under that key, and any
subsequent decode of the same schema (re-render, re-open of drawer) throws a collision error.

## Chosen Fix — Solution A
Strip both `$schema` and `$id` from the schema before passing to `validator.ajv.compile()`,
using a shared `cleanSchemaForValidation()` helper applied consistently in all compile call
sites within the validator layer.

See `ANALYSIS.md` for full investigation and option comparison.

## Files Changed
| File | Change |
|---|---|
| `src/modules/TopicFilters/utils/topic-filter.schema.ts` | Use `cleanSchemaForValidation()` before compile |
| `src/components/rjsf/Form/ChakraRJSForm.tsx` | Use `cleanSchemaForValidation()` before compile (was missing `$id` strip) |
| `src/components/rjsf/Form/validation.utils.ts` | Add exported `cleanSchemaForValidation()` helper |
| `src/modules/TopicFilters/utils/topic-filter.schema.spec.ts` | Add regression test: same `$id` twice must not throw |

## Verification
```bash
pnpm test --reporter=verbose src/modules/TopicFilters/utils/topic-filter.schema.spec.ts
pnpm build:tsc
```
