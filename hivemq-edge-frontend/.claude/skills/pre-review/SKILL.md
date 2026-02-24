---
name: pre-review
description: Automated code review against established patterns before PR creation. Analyzes git diff for critical issues, pattern violations, test coverage, and generates a comprehensive report.
argument-hint: [--base branch] [--focus areas] [--skip sections]
disable-model-invocation: false
user-invocable: true
allowed-tools: Bash, Grep, Read
---

# Pre-Review Skill

Analyzes code changes in the current branch against established patterns, catching common issues early before PR submission.

Generates a comprehensive report covering:

- Critical issues (must fix before PR)
- Pattern violations (should fix)
- Missing test coverage & quality gates
- Suggestions (nice to have)

## Usage

Arguments are parsed as follows:

- `--base <branch>`: Base branch to compare against (default: master)
- `--focus <areas>`: Comma-separated focus areas (e.g., "state-management,testing")
- `--skip <sections>`: Skip report sections (e.g., "suggestions")

Examples:

```bash
# Basic usage (analyzes current branch vs master)
/pre-review

# With custom base branch
/pre-review --base develop

# With focus areas
/pre-review --focus "state-management,testing"

# Skip sections
/pre-review --skip "suggestions"
```

---

## Skill Workflow

### Phase 1: Change Analysis (Gather)

1. **Identify changed files**

   ```bash
   git diff master...HEAD --name-status
   ```

2. **Categorize changes**

   - Source files (`.ts`, `.tsx`) - non-test
   - Test files (`.spec.ts`, `.cy.tsx`)
   - Generated files (`__generated__/`)
   - Config files

3. **Extract metrics**
   - Line count changes
   - Number of files modified
   - Test coverage ratio

### Phase 2: Critical Issues Check (Must Fix)

Run these checks and flag any violations:

#### 1. Type Safety Issues

```bash
git diff master...HEAD | grep -E "@ts-ignore|@ts-expect-error"
```

**Pattern:** All type suppressions must have:

- Justification comment
- TODO with ticket reference
- Expected fix timeline

**Example:**

```typescript
// ✅ Good
// TODO[TICKET-123]: Fix validator types to accept EntityQuery[]
// Temporary: validator expects old structure
// @ts-ignore - validator type needs update
const validator = useValidateCombiner(sources, entities)

// ❌ Bad
// @ts-ignore wrong type; need a fix
const validator = useValidateCombiner(sources, entities)
```

#### 2. Linter Suppressions

```bash
git diff master...HEAD | grep -E "eslint-disable"
```

**Pattern:** All `eslint-disable` must have:

- Inline comment explaining why
- Specific rule name (not `eslint-disable-line` without rule)
- Considered alternative solutions

**Example:**

```typescript
// ✅ Good
// Only re-run when mapping ID changes, not when formContext updates
// to avoid cascade reconstruction
// eslint-disable-next-line react-hooks/exhaustive-deps
}, [formData?.id])

// ❌ Bad
// eslint-disable-next-line react-hooks/exhaustive-deps
}, [formData?.id])
```

#### 3. Console Statements in Production

```bash
git diff master...HEAD | grep -E "^\+.*console\.(log|warn|error|debug)" | grep -v "\.spec\." | grep -v "\.cy\."
```

**Pattern:** No `console.*` in production code

- Use proper logging library
- Or remove debug statements

#### 4. TODO/FIXME Without Owner

```bash
git diff master...HEAD | grep -E "TODO|FIXME|XXX|HACK"
```

**Pattern:** All TODO comments must include:

- Ticket reference: `TODO[TICKET-123]`
- Or owner: `TODO[YourName]`
- Brief explanation

**Example:**

```typescript
// ✅ Good
// TODO[38943/NVL]: Investigate reusing RJSF templates
// Current: manually extracting options from uiSchema
// Ideal: reuse built-in template system

// ❌ Bad
// TODO: Would prefer to reuse the templates; need investigation
```

#### 5. Missing Test Coverage for New Public Functions

**Check:** For each new exported function, ensure test exists

**Pattern:**

```typescript
// New function in src/utils/foo.ts
export const newFunction = () => {}

// Must have test in src/utils/foo.spec.ts
describe('newFunction', () => {
  it('should handle basic case', () => {})
  it('should handle edge case', () => {})
})
```

#### 6. Hardcoded Strings (i18n)

```bash
git diff master...HEAD | grep -E "^\+.*<(Heading|Text|Button|Label).*>.*[A-Z]" | grep -v "t\("
```

**Pattern:** All user-facing text must use `t()` translation

### Phase 3: Pattern Violations (Should Fix)

#### 1. Inline Types vs Interfaces

**Pattern:** Use explicit interfaces, not inline object types

```typescript
// ❌ Bad
const [state, setState] = useState<
  | {
      tags: DataIdentifierReference[]
      topicFilters: DataIdentifierReference[]
    }
  | undefined
>()

// ✅ Good
import type { SelectedSources } from '@/modules/Mappings/types'
const [state, setState] = useState<SelectedSources | undefined>()
```

#### 2. Emoji in Production Code

**Pattern:** No emojis in code comments (markdown docs are fine)

```typescript
// ❌ Bad
adapterId: entity.id, // ✅ Direct access to entity

// ✅ Good
adapterId: entity.id, // Direct access to entity (no index needed)
```

#### 3. Magic Strings/Numbers

**Pattern:** Extract constants

```typescript
// ❌ Bad
if ((item as DomainTag).name) {
  // Check by property
}

// ✅ Good
const isDomainTag = (item: unknown): item is DomainTag => {
  return (item as DomainTag).name !== undefined
}
if (isDomainTag(item)) {
  // Type-safe check
}
```

#### 4. Large File Size

**Pattern:** Files > 300 lines should be considered for splitting

```bash
git diff master...HEAD --stat | awk '{if ($3 > 300) print $1, $3}'
```

#### 5. Inconsistent Naming

**Pattern:** Follow project conventions

- Components: PascalCase (`UserProfile.tsx`)
- Hooks: camelCase starting with `use` (`useUserData.ts`)
- Utils: camelCase (`formatDate.ts`)
- Constants: UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)

### Phase 4: Test Coverage & Quality Gates

#### 1. Test Files for Changed Components

**Rule:** Every new/changed component needs test file

```typescript
// src/components/NewComponent.tsx (new file)
// Must have: src/components/NewComponent.spec.cy.tsx
```

#### 2. Accessibility Tests

**Pattern:** Every component test must include:

```typescript
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<Component />)
  cy.checkAccessibility()  // Last test in describe block
})
```

#### 3. Test Selectors

**Pattern:** Must use in priority order:

1. `data-testid` (best)
2. ARIA roles/labels
3. Text content
4. Never CSS classes

```typescript
// ✅ Good
cy.getByTestId('user-profile-name')
cy.findByRole('button', { name: 'Submit' })

// ❌ Bad
cy.get('.chakra-button')
cy.get('.css-abc123')
```

#### 4. Backward Compatibility Tests

**Rule:** When deprecating or migrating, add compat tests

```typescript
// Example: DataCombiningEditorDrawer.backward-compat.spec.cy.tsx
describe('backward compatibility', () => {
  it('should handle old data format', () => {})
  it('should handle new data format', () => {})
})
```

#### 5. Edge Case Coverage

**Check:** For each new function, verify edge cases tested:

- Null/undefined inputs
- Empty arrays
- Missing optional fields
- Error conditions

### Phase 5: Suggestions (Nice to Have)

#### 1. JSDoc for Public APIs

```typescript
/**
 * Extracts the adapterId (scope) for a given tag from context.
 *
 * @param tagId - The tag identifier to look up
 * @param formContext - The combiner context
 * @returns The adapterId if found, undefined otherwise
 *
 * @example
 * const adapterId = getAdapterIdForTag('temperature', formContext)
 */
export const getAdapterIdForTag = (tagId: string, formContext?: CombinerContext) => {}
```

#### 2. Performance Considerations

**Check:** For loops/iterations over large arrays:

- Use `.reduce()` efficiently
- Avoid nested loops
- Consider memoization for expensive calculations

#### 3. Code Organization

**Suggest:**

- Extract complex logic to separate functions
- Group related functions
- Consider custom hooks for stateful logic

#### 4. Storybook Stories

**Suggest:** For new visual components, add Storybook story

---

## Output Format

Generate a markdown report with structure:

```markdown
# Pre-Review Report: [Branch Name]

**Branch:** {branch-name}
**Date:** {date}
**Scope:** {files changed}, {lines added/removed}

---

## Executive Summary

**Overall Assessment:** [Ready/Needs Work/Major Issues]

**Key Strengths:**

- ✅ ...

**Must Fix Before PR:**

- 🔴 ...

---

## Critical Issues (Must Fix Before PR)

### 🔴 1. [Issue Title]

**File:** {file}:{line}
**Issue:** {description}
**Why Critical:** {reason}
**Fix Required:**
{code example}

---

## Pattern Violations (Should Fix)

### ⚠️ 1. [Violation Title]

**File:** {file}:{line}
**Pattern:** {expected pattern}
**Current:**
{code}
**Fix:**
{code}

---

## Missing Test Coverage & Quality Gates

### ⚠️ 1. [Missing Coverage]

**Missing:** {what's missing}
**Recommended:**
{code example}

---

## Suggestions (Nice to Have)

### 💡 1. [Suggestion Title]

**File:** {file}
**Suggested:** {improvement}
**Benefit:** {why}

---

## Code Quality Metrics

| Metric        | Score    | Status   |
| ------------- | -------- | -------- |
| Test Coverage | X files  | ✅/⚠️/❌ |
| Type Safety   | X issues | ✅/⚠️/❌ |

| ...

---

## Checklist Before PR Submission

### Critical (Must Do)

- [ ] Fix item 1
- [ ] Fix item 2

### Recommended (Should Do)

- [ ] Fix item 3

### Nice to Have (Could Do)

- [ ] Suggestion 1

---

## PR Description Template

{generated template based on changes}
```

---

## Expandable Checklist

The checklist is stored in `.claude/skills/pre-review/checklist.yaml` and can be extended:

```yaml
critical_checks:
  - id: type-safety
    name: Type Safety Issues
    pattern: '@ts-ignore|@ts-expect-error'
    severity: critical
    rule: 'Must have justification + TODO'

pattern_violations:
  - id: inline-types
    name: Inline Types vs Interfaces
    severity: should-fix
    rule: 'Use explicit interfaces'

test_coverage:
  - id: accessibility
    name: Accessibility Tests
    severity: must-have
    rule: 'Every component needs a11y test'
```

Add new checks by editing the YAML file.

---

## Integration with Tasks

The skill automatically:

1. Checks for related task documentation in `.tasks/{task-id}/`
2. References analysis documents if found
3. Links to relevant pattern guides
4. Generates PR template with task context

---

## Example Invocation

```typescript
// In conversation:
User: "Check if my code is ready for PR"
Assistant: [Invokes /pre-review skill]

// Or explicit:
User: "/pre-review"
Assistant: [Analyzes current branch and generates report]
```
