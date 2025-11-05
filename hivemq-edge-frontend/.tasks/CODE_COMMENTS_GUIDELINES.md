# Code Comments and Documentation Guidelines

**Date Added:** November 3, 2025  
**Priority:** ✅ CRITICAL

---

## Fundamental Rule

**Code MUST be self-explanatory. Comments are only added when information is not immediately available or clear from the code itself.**

**Problem:** Over-commenting creates noise and reduces code readability. The goal is to maximize the signal-to-noise ratio in code files.

---

## ✅ ALLOWED Comments

### 1. JSDoc for Functions/Classes

Only when the function purpose is not immediately obvious from the name and signature.

```typescript
/**
 * Extracts policy summary from validation report's final item.
 */
export function extractPolicySummary(report: DryRunResults[]) {}
```

**Note:** Don't document every parameter if they're self-explanatory from their names and types.

### 2. JSDoc for TypeScript Types/Interfaces

Only when needed for clarity - most types are self-documenting.

```typescript
// ✅ Good - Types are self-explanatory, no JSDoc needed
export interface PolicyPayload {
  policy: object
  resources: {
    schemas: object[]
    scripts: object[]
  }
}
```

### 3. Complex Algorithm Explanations

When the **why** is not obvious from the code.

```typescript
// Use flatMap because each transition can have multiple event keys
summary.transitions = policy.onTransitions.flatMap((t) =>
  Object.keys(t).filter((k) => k !== 'fromState' && k !== 'toState')
)
```

---

## ❌ FORBIDDEN Comments (DO NOT ADD)

### 1. File-Level Comments

Documentation belongs in README files, not at the top of code files.

```typescript
// ❌ WRONG - Noisy and redundant
/**
 * This file contains utility functions for extracting policy summary
 * information from validation reports.
 */
```

### 2. Comments Within JSX

Creates visual noise and clutters the component structure.

```tsx
// ❌ WRONG - JSX comments are distracting
<Box>
  {/* Resource summary section */}
  <ResourcesBreakdown resources={resources} />
  {/* Policy overview section */}
  <PolicyOverview policy={policy} />
</Box>

// ✅ CORRECT - Self-explanatory JSX
<Box>
  <ResourcesBreakdown resources={resources} />
  <PolicyOverview policy={policy} />
</Box>
```

### 3. Redundant JSDoc Parameter Descriptions

Don't document parameters when their names and types are self-explanatory.

```typescript
// ❌ WRONG - Redundant parameter documentation
/**
 * Groups resources by type.
 * @param resources - Array of ResourceSummary objects  ← REDUNDANT
 * @param includeEmpty - Whether to include empty groups  ← REDUNDANT
 * @returns Object with schemas and scripts arrays      ← REDUNDANT
 */

// ✅ CORRECT - Function signature is self-explanatory
export function groupResourcesByType(
  resources: ResourceSummary[],
  includeEmpty = false
): { schemas: ResourceSummary[]; scripts: ResourceSummary[] }
```

### 4. Comments for Every Statement

Don't narrate the code - let it speak for itself.

```typescript
// ❌ WRONG - Over-commented
export function extractPolicySummary(report: DryRunResults[]) {
  // Check if report exists
  if (!report || report.length === 0) return undefined

  // Get the final summary item
  const finalSummary = [...report].pop()

  // Check if data exists
  if (!finalSummary?.data) return undefined

  // Determine if new policy
  const isNew = status === DesignerStatus.DRAFT

  // Return summary object
  return { id, type, isNew }
}

// ✅ CORRECT - Self-explanatory code
export function extractPolicySummary(report: DryRunResults[]) {
  if (!report || report.length === 0) return undefined

  const finalSummary = [...report].pop()
  if (!finalSummary?.data) return undefined

  const isNew = status === DesignerStatus.DRAFT
  return { id, type, isNew }
}
```

### 5. Obvious Variable/Constant Comments

```typescript
// ❌ WRONG - Stating the obvious
const isNew = true // Indicates if policy is new

// ✅ CORRECT - Name is self-explanatory
const isNew = designerStatus === DesignerStatus.DRAFT
```

---

## Decision Framework: "Should I Add a Comment?"

Ask yourself these questions in order:

1. **Is the code self-explanatory?**  
   → **No comment needed**

2. **Would renaming variables/functions make it clearer?**  
   → **Refactor instead of commenting**

3. **Is this a complex algorithm or non-obvious logic?**  
   → **Comment the 'why', not the 'what'**

4. **Is this a workaround or hack?**  
   → **Explain why it's necessary**

5. **Is this JSDoc for public API?**  
   → **Keep it minimal, only document non-obvious aspects**

---

## Examples from Real Code

### Example 1: Utility Function

**❌ Over-commented (TOO MUCH NOISE):**

```typescript
// Extract policy summary from validation report
export function extractPolicySummary(report: DryRunResults[]) {
  // Validate input
  if (!report || report.length === 0) return undefined

  // Get final summary
  const finalSummary = [...report].pop()

  // Validate summary has data
  if (!finalSummary?.data) return undefined

  // Get policy type from node
  const policyType = finalSummary.node.type

  // Check if valid policy type
  if (policyType !== DATA_POLICY && policyType !== BEHAVIOR_POLICY) {
    return undefined
  }

  // Extract policy data
  const policyData = finalSummary.data

  // Determine if new or update
  const isNew = status === DesignerStatus.DRAFT

  // Build summary object
  const summary = { id: policyData.id, type: policyType, isNew }

  // Return the summary
  return summary
}
```

**✅ Clean, self-explanatory (GOOD):**

```typescript
export function extractPolicySummary(report: DryRunResults[]) {
  if (!report || report.length === 0) return undefined

  const finalSummary = [...report].pop()
  if (!finalSummary?.data || !finalSummary.node) return undefined

  const policyType = finalSummary.node.type
  if (policyType !== DATA_POLICY && policyType !== BEHAVIOR_POLICY) {
    return undefined
  }

  return {
    id: finalSummary.data.id,
    type: policyType,
    isNew: status === DesignerStatus.DRAFT,
  }
}
```

### Example 2: React Component

**❌ Over-commented:**

```tsx
export const ResourcesBreakdown: FC<Props> = ({ resources }) => {
  // Group resources by type
  const { schemas, scripts } = groupResourcesByType(resources)

  return (
    <Box>
      {/* Schemas section */}
      <VStack>
        {/* Schema list */}
        {schemas.map(schema => (
          {/* Individual schema item */}
          <Badge key={schema.id}>{schema.id}</Badge>
        ))}
      </VStack>

      {/* Scripts section */}
      <VStack>
        {/* Script list */}
        {scripts.map(script => (
          {/* Individual script item */}
          <Badge key={script.id}>{script.id}</Badge>
        ))}
      </VStack>
    </Box>
  )
}
```

**✅ Clean:**

```tsx
export const ResourcesBreakdown: FC<Props> = ({ resources }) => {
  const { schemas, scripts } = groupResourcesByType(resources)

  return (
    <Box>
      <VStack>
        {schemas.map((schema) => (
          <Badge key={schema.id}>{schema.id}</Badge>
        ))}
      </VStack>

      <VStack>
        {scripts.map((script) => (
          <Badge key={script.id}>{script.id}</Badge>
        ))}
      </VStack>
    </Box>
  )
}
```

---

## Objective: Maximize Signal-to-Noise Ratio

**Goal:** Code files should be primarily code, not commentary.

- **Code = Signal** (what the program does)
- **Excessive comments = Noise** (clutter that obscures understanding)

**Analogy:** Good code is like good prose - clear, concise, and self-explanatory. Comments are footnotes - use sparingly and only when necessary.

---

## When Reviewing AI-Generated Code

Before accepting generated code, ask:

1. **Can I understand what this does without the comments?**
2. **If I remove all comments, is the code still clear?**
3. **Are there excessive comments explaining obvious operations?**
4. **Are variable/function names descriptive enough?**

**If YES to #1-2 and NO to #3:** The code is well-written. **Remove unnecessary comments.**

---

## Summary

**DO:**

- ✅ Write self-explanatory code with clear names
- ✅ Add JSDoc only for non-obvious function purposes
- ✅ Comment the 'why' for complex algorithms
- ✅ Explain workarounds and hacks

**DON'T:**

- ❌ Add file-level comment blocks
- ❌ Add comments within JSX structure
- ❌ Document every JSDoc parameter when self-explanatory
- ❌ Narrate code with inline comments
- ❌ State the obvious

**Remember:** If you need a comment to explain what the code does, consider refactoring the code to be clearer instead.
