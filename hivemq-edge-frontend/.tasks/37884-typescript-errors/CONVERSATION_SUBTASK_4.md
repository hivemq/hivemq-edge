# Subtask 4: Runtime Type Safety - COMPLETE ✅

## Objective

Fix 1 error where `unknown` type was passed to function expecting `GenericObjectType`.

## Error to Fix

`src/modules/Workspace/hooks/useGetPoliciesMatching.ts` (line 40)

## Work Log

### Fix: useGetPoliciesMatching.ts

**Error:** Argument of type 'unknown' is not assignable to parameter of type 'GenericObjectType'

**Root cause:**

- `node.data.config` has type `JsonNode | undefined` (which TypeScript treats as `unknown`)
- Function `discoverAdapterTopics` expects `GenericObjectType` (from @rjsf/utils)

**Solution:**

1. Added import for `GenericObjectType` from `@rjsf/utils`
2. Added type assertion when calling the function

```typescript
import type { GenericObjectType } from '@rjsf/utils'

const getPoliciesForAdapter = (node: Node): DataPolicy[] | undefined => {
  const adapterProtocol = protocols?.items?.find((e) => e.id === node.data.type)
  if (!adapterProtocol) return undefined

  const allTopics = discoverAdapterTopics(adapterProtocol, node.data.config as GenericObjectType)

  return dataPolicies?.items?.filter((policy) =>
    allTopics.some((topic) => mqttTopicMatch(policy.matching.topicFilter, topic))
  )
}
```

## Files Modified

1. `src/modules/Workspace/hooks/useGetPoliciesMatching.ts` - Added import and type assertion

---

**Status:** COMPLETED ✅  
**Errors fixed:** 1  
**Next:** Check if all 24 original errors are resolved
