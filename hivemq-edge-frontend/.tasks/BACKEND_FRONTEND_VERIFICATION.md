# Backend/Frontend Cross-Functional Verification Guidelines

**Purpose:** This document describes the process for verifying frontend implementation assumptions against backend code in a monorepo structure, without switching branches or affecting working directory state.

**Last Updated:** February 5, 2026

---

## Overview

When implementing frontend features that depend on backend API contracts, it's critical to verify assumptions about data structures, validation rules, and serialization behavior. This is especially important for:

- Optional vs required fields
- `null` vs `undefined` handling
- Validation rules and constraints
- Enum values and their meanings
- Error handling expectations

## Repository Structure

```
/Users/nicolas/dev-projects/edge/hivemq-edge/
├── hivemq-edge/              # Backend (Java/Gradle)
│   ├── src/main/java/
│   ├── src/test/java/
│   └── build.gradle
└── hivemq-edge-frontend/     # Frontend (TypeScript/React)
    ├── src/
    ├── .tasks/               # Task documentation
    └── package.json
```

**Relative Path:** Backend is at `../hivemq-edge/` from frontend directory.

---

## Verification Process (Without Switching Branches)

### Step 1: Identify Backend Branch

Determine which backend branch contains the relevant work:

```bash
# List remote branches related to the feature
git branch -r | grep "feature/feature-name"

# Common patterns:
# - origin/epic/XXXXX-epic-name (epic branch with multiple features)
# - origin/feature/XXXXX-feature-name (specific feature branch)
```

### Step 2: Explore Changed Files

See what files were modified in the backend branch:

```bash
cd /Users/nicolas/dev-projects/edge/hivemq-edge

# List all changed files
git diff --name-only origin/master...origin/feature/XXXXX-feature-name

# Filter for relevant files
git diff --name-only origin/master...origin/feature/XXXXX-feature-name | grep -i "keyword"

# Example: Find DataIdentifierReference changes
git diff --name-only origin/master...origin/feature/38627-add-scope | grep -i "dataidentifier"
```

### Step 3: Read Specific Files from Branch

Use `git show` to read files without checking out the branch:

```bash
# Read a specific file from backend branch
git show origin/feature/XXXXX-feature-name:relative/path/to/File.java

# Example: Read model class
git show origin/feature/38627-add-scope:hivemq-edge/src/main/java/com/hivemq/combining/model/DataIdentifierReference.java

# Example: Read test file
git show origin/feature/38627-add-scope:hivemq-edge/src/test/java/com/hivemq/combining/model/DataIdentifierReferenceTest.java
```

### Step 4: Search Across Backend Branch

Search for patterns without checking out:

```bash
# Search for a pattern in Java files
git grep "pattern" origin/feature/XXXXX-feature-name -- '*.java'

# Example: Find scope validation logic
git grep "isScopeValid\|scope == null" origin/feature/38627-add-scope -- '*.java'

# Example: Find test assertions about scope
git grep "@Test.*scope" origin/feature/38627-add-scope -- '*Test.java'
```

### Step 5: Find Class Definitions

Locate where classes/records are defined:

```bash
# Find class or record definitions
git grep -l "class ClassName\|record ClassName" origin/feature/XXXXX-feature-name -- '*.java'

# Example: Find DataIdentifierReference definition
git grep -l "class DataIdentifierReference\|record DataIdentifierReference" origin/feature/38627-add-scope -- '*.java'
```

---

## What to Look For

### 1. Model Class/Record Definition

**Key Information:**
- Field types and nullability annotations (`@Nullable`, `@NotNull`)
- Default values or constructors
- Validation methods
- Conversion methods (to/from API models)

**Example:**
```java
public record DataIdentifierReference(
    String id,
    Type type,
    @Nullable String scope  // ← Nullable annotation!
) {
    // Convenience constructor
    public DataIdentifierReference(final String id, final Type type) {
        this(id, type, null);  // ← Default value is null
    }

    // Validation method
    public boolean isScopeValid() {
        return type == Type.TAG
            ? scope != null && !scope.isBlank()  // ← TAG requires non-null
            : scope == null;                      // ← Others require null
    }
}
```

**What this tells us:**
- `scope` can be `null` (not just missing)
- For TAG types, `scope` MUST be non-null and non-blank
- For other types, `scope` MUST be `null` (not omitted, explicit null)

### 2. Test Files

**Key Information:**
- Expected values in test data
- Validation test cases
- Round-trip serialization tests
- Edge cases and error conditions

**Example:**
```java
@Test
void isScopeValid_topicFilterWithNullScope_true() {
    final DataIdentifierReference ref =
        new DataIdentifierReference("filter/+", Type.TOPIC_FILTER, null);
    assertThat(ref.isScopeValid()).isTrue();  // ← null is valid
}

@Test
void isScopeValid_topicFilterWithScope_false() {
    final DataIdentifierReference ref =
        new DataIdentifierReference("filter/+", Type.TOPIC_FILTER, "adapter-1");
    assertThat(ref.isScopeValid()).isFalse();  // ← any scope is INVALID
}
```

**What this tells us:**
- TOPIC_FILTER with `scope: null` is valid
- TOPIC_FILTER with `scope: "adapter-1"` is invalid
- Frontend MUST send explicit `null`, not omit the property

### 3. OpenAPI Schema

**Key Information:**
- Required vs optional fields
- Nullable vs non-nullable
- Enum values
- Descriptions and constraints

**Example:**
```yaml
scope:
  type: string
  nullable: true  # ← Can be null, not just optional
  description: >
    Scoping identifier. For TAG type, this is the adapter ID that owns
    the tag. For other types, this is null.
```

**What this tells us:**
- Property is optional (can be omitted in request)
- Property is nullable (can explicitly be `null`)
- Backend expects `null` for non-TAG types

### 4. Serialization Annotations

**Key Information (Java/Jackson):**
- `@JsonInclude(JsonInclude.Include.NON_NULL)` - Omit null values
- `@JsonProperty(required = true)` - Field is required
- `@JsonSerialize` / `@JsonDeserialize` - Custom serialization

**Example:**
```java
@JsonInclude(JsonInclude.Include.ALWAYS)  // ← Always include, even if null
public String getScope() {
    return scope;
}
```

### 5. Validation Logic

**Key Information:**
- Custom validators
- JSR-303 annotations (`@NotNull`, `@NotBlank`, `@Size`)
- Conditional validation based on other fields

**Example:**
```java
public boolean isScopeValid() {
    // Conditional validation based on type
    return type == Type.TAG
        ? scope != null && !scope.isBlank()
        : scope == null;
}
```

---

## Real-World Example: Task 38936

### Context

Frontend implementing `scope` field for `DataIdentifierReference`. Initial assumption: use `undefined` (omit property) for non-TAG types.

### Verification Process

1. **Identified backend branch:** `origin/feature/38627-add-scope-to-data-identifier-reference`

2. **Found changed files:**
```bash
git diff --name-only origin/master...origin/feature/38627-add-scope | grep -i dataidentifier
```

3. **Read model class:**
```bash
git show origin/feature/38627-add-scope:hivemq-edge/src/main/java/com/hivemq/combining/model/DataIdentifierReference.java
```

4. **Read tests:**
```bash
git show origin/feature/38627-add-scope:hivemq-edge/src/test/java/com/hivemq/combining/model/DataIdentifierReferenceTest.java
```

### Key Finding

```java
public boolean isScopeValid() {
    return type == Type.TAG ? scope != null && !scope.isBlank() : scope == null;
    //                                                              ^^^^^^^^^^^^^^
    //                        Explicit check: scope MUST be null for non-TAG
}
```

**Tests confirmed:**
- `scope: null` for TOPIC_FILTER → VALID ✅
- `scope: "adapter-1"` for TOPIC_FILTER → INVALID ❌
- Omitted scope (undefined) → Would fail validation

### Impact on Frontend

**Changed from:**
```typescript
// WRONG ASSUMPTION
{ id: "topic", type: "TOPIC_FILTER" } // omit scope
```

**Changed to:**
```typescript
// CORRECT IMPLEMENTATION
{ id: "topic", type: "TOPIC_FILTER", scope: null } // explicit null
```

**Avoided Issues:**
- Backend validation would fail with omitted scope
- Round-trip save/load would lose data
- Integration tests would fail unexpectedly

---

## Checklist for Backend Verification

When implementing a frontend feature that depends on backend API:

- [ ] **Identify backend branch** containing the related work
- [ ] **Find model/DTO classes** that define the data structure
- [ ] **Read class definition** looking for:
  - [ ] Field types and nullability
  - [ ] Default values
  - [ ] Validation methods
- [ ] **Read test files** looking for:
  - [ ] Test data fixtures
  - [ ] Validation test cases
  - [ ] Edge cases
- [ ] **Check OpenAPI schema** for:
  - [ ] Required vs optional
  - [ ] Nullable vs non-nullable
  - [ ] Enum values
- [ ] **Search for validation logic** using patterns like:
  - [ ] `validate`, `isValid`, `check`
  - [ ] Field name patterns (`scope`, `type`, etc.)
- [ ] **Document findings** in task plan with:
  - [ ] Backend branch reference
  - [ ] Key code snippets
  - [ ] Test evidence
  - [ ] Impact on frontend implementation

---

## Common Pitfalls

### ❌ Don't Assume

**BAD:** "The field is optional, so I'll just omit it"
- Backend may require explicit `null` vs omitted property
- Validation rules may differ from schema

**BAD:** "undefined and null are the same in JSON"
- `undefined` omits the key entirely
- `null` includes `"field": null`
- Backend may explicitly check for `null`

**BAD:** "The OpenAPI schema is the source of truth"
- Schema shows structure, not validation rules
- Backend code may have additional constraints

### ✅ Do Verify

**GOOD:** Read the actual validation method in backend code

**GOOD:** Check test cases for expected values

**GOOD:** Search for how the field is used in backend logic

**GOOD:** Document your findings and reasoning

---

## Benefits of This Process

1. **Caught issues before implementation** - Avoided wasting time on wrong approach
2. **Prevented integration failures** - Implementation matches backend expectations
3. **Documented assumptions** - Clear evidence for decisions
4. **Improved collaboration** - Frontend and backend stay aligned
5. **Faster debugging** - When issues arise, clear reference to backend behavior

---

## Tools and Commands Reference

### Quick Command Patterns

```bash
# Change to backend directory (from frontend)
cd ../hivemq-edge

# List files changed in backend branch
git diff --name-only origin/master...origin/feature/XXXXX

# Read specific file from branch
git show origin/feature/XXXXX:path/to/File.java

# Search for pattern in branch
git grep "pattern" origin/feature/XXXXX -- '*.java'

# Find class definitions
git grep -l "class ClassName\|record ClassName" origin/feature/XXXXX -- '*.java'

# Return to frontend directory
cd ../hivemq-edge-frontend
```

### Common Search Patterns

```bash
# Find validation methods
git grep "boolean is.*Valid\|void validate" origin/feature/XXXXX -- '*.java'

# Find test assertions
git grep "assertThat.*scope\|assertEquals.*scope" origin/feature/XXXXX -- '*Test.java'

# Find nullable annotations
git grep "@Nullable String scope\|@NotNull String scope" origin/feature/XXXXX -- '*.java'

# Find enum definitions
git grep "enum.*Type\|public enum" origin/feature/XXXXX -- '*.java'
```

---

## When to Use This Process

**Always verify when:**
- Implementing new API integration
- Updating existing API contracts (schema changes)
- Dealing with optional/nullable fields
- Implementing validation that mirrors backend
- Using enums or constant values
- Handling error responses

**Especially important for:**
- `null` vs `undefined` decisions
- Required vs optional fields
- Validation rules and constraints
- Default values and edge cases
- Enum values and their semantics

---

## Integration with Task Planning

### In TASK_BRIEF.md

Add a "Backend Verification" section:

```markdown
## Backend Verification

**Backend Branch:** origin/feature/XXXXX-feature-name
**Key Files Verified:**
- Model: path/to/Model.java
- Tests: path/to/ModelTest.java
- OpenAPI: path/to/schema.yaml

**Key Findings:**
[Document critical findings here]
```

### In TASK_PLAN.md

Reference backend behavior in implementation notes:

```markdown
**Backend Requirement (verified):**
- TAG types: scope must be non-null, non-blank string
- TOPIC_FILTER: scope must be explicit null
- Evidence: DataIdentifierReference.isScopeValid() line 60
```

---

## Example Session

```bash
# Start in frontend directory
cd /Users/nicolas/dev-projects/edge/hivemq-edge/hivemq-edge-frontend

# Switch to backend (no branch checkout needed!)
cd ../hivemq-edge

# Find what changed
git diff --name-only origin/master...origin/feature/38627-add-scope | grep DataIdentifier
# Output:
# hivemq-edge/src/main/java/com/hivemq/combining/model/DataIdentifierReference.java
# hivemq-edge/src/test/java/com/hivemq/combining/model/DataIdentifierReferenceTest.java

# Read the model class
git show origin/feature/38627-add-scope:hivemq-edge/src/main/java/com/hivemq/combining/model/DataIdentifierReference.java | grep -A 5 "isScopeValid"
# Output shows validation logic

# Read the test file
git show origin/feature/38627-add-scope:hivemq-edge/src/test/java/com/hivemq/combining/model/DataIdentifierReferenceTest.java | grep -A 5 "topicFilterWithNullScope"
# Output shows test expectations

# Return to frontend
cd ../hivemq-edge-frontend

# Document findings in task plan
# Update implementation based on verified backend behavior
```

---

**Remember:** This process costs 5-10 minutes but can save hours of debugging integration issues. Always verify assumptions against actual backend code!

---

**See Also:**
- `.github/AI_MANDATORY_RULES.md` - General AI agent guidelines
- `.tasks/TESTING_GUIDELINES.md` - Testing patterns
- `.tasks/38936-tag-reference-scope/TASK_PLAN.md` - Real-world example of this process
