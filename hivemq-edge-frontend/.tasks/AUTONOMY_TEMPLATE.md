# Autonomy Template

## Document Purpose
This document captures key learnings, patterns, and guidelines discovered while working on autonomous tasks for the HiveMQ Edge frontend. These findings should inform future autonomous work on similar tasks.

---

## 🎯 CRITICAL: Everything Task-Related is in `.tasks/` Directory

**RULE #1: ALL task documentation, history, and context files are located in the `.tasks/` directory.**

When a user mentions working on a task:
1. **ALWAYS** start by reading `.tasks/ACTIVE_TASKS.md`
2. **NEVER** look for task files at the project root
3. **ALL** task files follow the pattern: `.tasks/{task-id}-{task-name}/`

### File Locations

```
.tasks/                           ← ALL task files are here
├── ACTIVE_TASKS.md              ← START HERE: Task registry
├── AUTONOMY_TEMPLATE.md         ← This file: AI guidelines
├── QUICK_START.md               ← User instructions
├── FOR_CONSIDERATION.md         ← Future improvements
└── {task-id}-{task-name}/       ← Specific task directory
    ├── TASK_BRIEF.md
    ├── TASK_SUMMARY.md
    ├── CONVERSATION_SUBTASK_N.md
    └── SESSION_FEEDBACK.md
```

**Why this matters:** Task files NEVER live at the project root. Always navigate to `.tasks/` first.

---

## Starting a New Conversation Thread ✅

**Problem:** When starting a new conversation, the AI needs to know which task to work on and where to find its history.

**Solution:** The user will mention a task ID, and you look in `.tasks/` directory

### Quick Resume Instructions

**For the User:**

In a new conversation, simply state:
```
We're working on task 37542-code-coverage
```
or
```
Continue work on task 37542
```

**For the AI Agent:**

When the user mentions a task ID, immediately:

1. **Read the task index** (it's ALWAYS in `.tasks/`):
   ```
   read_file(".tasks/ACTIVE_TASKS.md")
   ```

2. **Navigate to the task directory** (under `.tasks/`) based on the index entry

3. **Load context in this order** (all files under `.tasks/{task-id}/`):
   - `TASK_BRIEF.md` - Understand the task objective and approach
   - `TASK_SUMMARY.md` - Review all completed subtasks
   - Latest `CONVERSATION_SUBTASK_N.md` - Check most recent work
   - `SESSION_FEEDBACK.md` - Review any important learnings

4. **Confirm understanding** with the user:
   - Task objective
   - Number of completed subtasks
   - Current status
   - Ready to continue

### Task Index Location

**File:** `.tasks/ACTIVE_TASKS.md` ← ALWAYS HERE

This file maintains a registry of all active tasks with:
- Task ID and name
- Directory location (always under `.tasks/`)
- Quick summary
- Links to key documents
- Status and last update date

### Example Resume Flow

```
User: "We're working on task 37542-code-coverage"

Agent Actions:
1. read_file(".tasks/ACTIVE_TASKS.md")                          ← In .tasks/
2. read_file(".tasks/37542-code-coverage/TASK_BRIEF.md")       ← In .tasks/
3. read_file(".tasks/37542-code-coverage/TASK_SUMMARY.md")     ← In .tasks/
4. Summarize: "I can see we're working on code coverage improvements. 
   You've completed 3 subtasks covering 11 files with 42 new tests. 
   Ready to continue with the next subtask."
```

---

## Task Documentation Structure ✅

**Overview:** All task-related documents MUST reside in `.tasks/{task-id}/` subdirectory structure.

### Required Documents

1. **TASK_BRIEF.md** - Initial task description and objectives
   - Contains the task's primary goal and context
   - Lists all subtasks with their completion status
   - References related documentation files

2. **TASK_SUMMARY.md** - Comprehensive tracking of ALL subtasks across different discussion threads
   - Each subtask has its own section with detailed outcomes
   - Maintains summary statistics across all subtasks
   - Updated after each subtask completion
   - Provides a complete historical record of task progress

3. **CONVERSATION_SUBTASK_N.md** - Full conversation history for subtask N
   - One file per subtask (where N is the subtask number from TASK_SUMMARY)
   - Contains the complete dialogue and decision-making process
   - Records all feedback, iterations, and problem-solving steps
   - Preserves context for understanding how subtask was completed

4. **SESSION_FEEDBACK.md** - Lessons learned and retrospective notes
   - High-level observations and improvements identified
   - Process improvements for future tasks
   - Team collaboration insights

5. **AUTONOMY_TEMPLATE.md** - This document - patterns for autonomous work
   - Best practices and common patterns
   - Technical learnings and solutions
   - Guidelines for maintaining quality and consistency

### Directory Structure Pattern

```
.tasks/
  └── {task-id}-{task-name}/
      ├── TASK_BRIEF.md              # Task overview and subtask list
      ├── TASK_SUMMARY.md            # Comprehensive subtask tracking
      ├── CONVERSATION_SUBTASK_1.md  # Full conversation for subtask 1
      ├── CONVERSATION_SUBTASK_2.md  # Full conversation for subtask 2
      ├── CONVERSATION_SUBTASK_N.md  # Full conversation for subtask N
      └── SESSION_FEEDBACK.md        # Retrospective and learnings
```

### Key Principles

- **Cross-Thread Continuity:** Subtasks may be created across different discussion threads
- **Always Check History:** Review TASK_SUMMARY.md before starting new work to understand completed subtasks
- **Document Everything:** Record both outcomes (TASK_SUMMARY) and process (CONVERSATION_SUBTASK_N)
- **Maintain Consistency:** Use standardized formats for all documentation
- **Never at Project Root:** All task documents must be in `.tasks/` subdirectories

---

## Subtask Management Across Discussion Threads ✅

**Finding:** Subtasks may be created by the user in different discussion threads, requiring careful tracking

**Best Practice:**
1. **Always check** existing TASK_SUMMARY.md for prior subtasks before starting new work
2. **Maintain consistency** in subtask format across all entries in TASK_SUMMARY.md
3. **Record conversations** in dedicated CONVERSATION_SUBTASK_N.md files
4. **Use standardized format** for each subtask in TASK_SUMMARY.md:
   ```markdown
   ## Subtask N: {Descriptive Name} ✅ COMPLETED
   
   **Completion Date:** October XX, 2025
   
   **Files Modified:**
   1. File path 1
   2. File path 2
   
   **Uncovered Lines by File:**
   1. **File1** - Lines X-Y, Z
   2. **File2** - Lines A-B
   
   **Test Metrics:**
   - **Total Tests Added:** N new test cases
   - **Test Files Modified:** N
   - **Final Test Results:** X passed, Y skipped
   - **Success Rate:** 100% passing
   - **TypeScript Errors:** 0
   
   **Coverage Improvements by File:**
   
   | File | Lines Covered | Tests Added | Status |
   |------|--------------|-------------|---------|
   | file1.spec.ts | X-Y | N | ✅ |
   
   **Key Test Scenarios Covered:**
   - Scenario descriptions
   
   **Technical Achievements:**
   - ✅ Achievement list
   
   **Issues Resolved:**
   - Issue descriptions
   ```

5. **Update summary statistics** at the end of TASK_SUMMARY.md after each subtask
6. **Create conversation file** named CONVERSATION_SUBTASK_N.md containing the full dialogue

---

### 3. TypeScript Validation Before Completion ✅ CRITICAL

**Finding:** Must check TypeScript validity before declaring a subtask complete

**Mandatory Step:**
```typescript
// Always run before marking subtask as complete:
get_errors(["/path/to/modified/file.ts"])
```

**Common TypeScript Issues Encountered:**

1. **Union Type Property Access:**
   ```typescript
   // ❌ WRONG - Property may not exist on all union members
   result.current.treeData.children
   
   // ✅ CORRECT - Use type guard first
   expect(result.current.treeData.type).toBe('node')
   const treeData = result.current.treeData as TreeNode
   treeData.children // Now safe to access
   ```

2. **Implicit Any Types in Callbacks:**
   ```typescript
   // ❌ WRONG - Implicit any type
   .find((child) => child.name === 'Tags')
   
   // ✅ CORRECT - Explicit type annotation
   .find((child: Tree) => child.name === 'Tags')
   ```

3. **Type Imports:**
   ```typescript
   // ✅ Always import all relevant types when dealing with union types
   import type { Tree, TreeLeaf, TreeNode } from '@/types'
   ```

**Validation Checklist:**
- [ ] Run `get_errors()` on all modified test files
- [ ] Verify 0 TypeScript compile errors (severity ERROR)
- [ ] Warnings (severity WARNING) about test assertions are acceptable
- [ ] All tests passing
- [ ] No implicit `any` types

---

### 4. Test Coverage Patterns for React Hooks

**Pattern 1: MSW Mock Handler Override**
```typescript
it('should test specific scenario', async () => {
  // Override default handlers with specific test data
  server.use(
    http.get('*/api/endpoint', () => {
      return HttpResponse.json({ items: [...] }, { status: 200 })
    })
  )
  
  const { result } = renderHook(useCustomHook, { wrapper })
  
  await waitFor(() => {
    expect(result.current.isLoading).toBeFalsy()
  })
  
  // Assertions
})
```

**Pattern 2: Testing Error Paths**
```typescript
it('should handle error state', async () => {
  server.use(
    http.get('*/api/endpoint', () => {
      return HttpResponse.json({}, { status: 500 })
    })
  )
  
  const { result } = renderHook(useCustomHook, { wrapper })
  
  await waitFor(() => {
    expect(result.current.isError).toBeTruthy()
  })
})
```

**Pattern 3: Testing Conditional Logic**
```typescript
// When testing conditional checks (e.g., "if (x !== -1 && y !== -1 && value)")
// Create test cases for:
// 1. Both conditions true + value present
// 2. One condition false
// 3. Value missing (null/undefined)
```

**Pattern 4: Matching Mock Data Across Endpoints**
```typescript
// ✅ CRITICAL: Ensure related endpoints return matching data
server.use(
  http.get('*/northboundMappings', () => {
    return HttpResponse.json({
      items: [{ tagName: 'test/tag1', topic: 'my/topic' }]
    })
  }),
  http.get('*/domain-tags', () => {
    return HttpResponse.json({
      items: [{ name: 'test/tag1' }] // Must match tagName above!
    })
  })
)
```

---

### 5. Common Coverage Gaps and Solutions

**Gap Type 1: Early Return/Guard Clauses**
```typescript
// Source code:
if (isLoading) return emptyStateData

// Test needed:
it('should return empty state when loading', () => {
  // Test during loading phase before data arrives
})
```

**Gap Type 2: Error Handling in useEffect**
```typescript
// Source code:
useEffect(() => {
  if (isError) return
  // ... setup interval
}, [isError])

// Test needed:
it('should not set up interval when there are errors', () => {
  // Mock error state and verify cleanup
})
```

**Gap Type 3: Conditional Data Processing**
```typescript
// Source code:
if (x !== -1 && y !== -1 && value) {
  // process data
}

// Tests needed:
// 1. Valid indices with value
// 2. Invalid index (-1)
// 3. Valid indices but no value
```

**Gap Type 4: MQTT Topic Matching**
```typescript
// Source code uses mqtt-match library for wildcard matching
const matched = mqttTopicMatch(filter, topic)

// Test needed:
it('should match topics against wildcard filters', () => {
  // Test patterns like 'sensor/+/room1' and 'sensor/#'
})
```

---

### 6. Test File Organization

**Standard Structure:**
```typescript
describe('hookName or utilityName', () => {
  beforeEach(() => {
    // Setup: MSW handlers, mocks
    server.use(...handlers)
  })

  afterEach(() => {
    // Cleanup
    vi.restoreAllMocks()
    server.resetHandlers()
  })

  it('should return a valid payload', () => {
    // Basic happy path test
  })

  it('should handle [specific edge case]', () => {
    // Edge case test targeting specific uncovered lines
  })
})
```

---

### 7. Debug Strategy for Failing Tests

**When Test Assertion Fails:**

1. **Check Mock Data Completeness**
   - Are all required endpoints mocked?
   - Does mock data match what the code expects?
   - Are there relationships between endpoints that need matching data?

2. **Verify Type Safety**
   - Run `get_errors()` to catch TypeScript issues
   - Check for implicit `any` types
   - Verify union type handling

3. **Inspect Actual vs Expected**
   - Use `console.log()` or `waitFor()` to inspect actual data
   - Check if the code path is actually being executed
   - Verify mock handlers are being used (not default handlers)

4. **Common Pitfalls:**
   - Default handlers from `beforeEach` overriding test-specific `server.use()`
   - Mock data mismatch between related endpoints
   - Incorrect type assertions/casts
   - Missing `await waitFor()` for async operations

---

### 8. Documentation Requirements

**After Each Subtask:**
1. ✅ Update TASK_SUMMARY.md with complete subtask entry
2. ✅ Verify all tests passing with `npm test`
3. ✅ Check TypeScript validity with `get_errors()`
4. ✅ Update summary statistics (total tests, files improved)
5. ✅ Document any issues encountered and resolutions

**Session Summary Format Requirements:**
- Consistent table formatting for coverage improvements
- Clear metrics (tests added, lines covered, success rate)
- Technical achievements and issues resolved sections
- Overall summary statistics at the end

---

### 9. Tools and Commands

**Essential Tools:**
```bash
# Run specific test files
npm test -- path/to/test.spec.ts --run

# Run with coverage
npm test -- path/to/test.spec.ts --coverage

# Check TypeScript errors
get_errors(["/path/to/file.ts"])

# Clear test cache if needed
rm -rf node_modules/.vite
```

**File Operations:**
- Use `file_search` to locate files
- Use `read_file` to examine code
- Use `grep_search` to find patterns
- Use `insert_edit_into_file` for modifications (never full rewrites)

---

### 10. Quality Checklist Before Subtask Completion

**Pre-Completion Checklist:**
- [ ] All identified uncovered lines have corresponding tests
- [ ] All tests passing (100% success rate)
- [ ] TypeScript errors = 0 (excluding test assertion warnings)
- [ ] No implicit `any` types introduced
- [ ] Mock data properly configured and matching across endpoints
- [ ] Type guards used for union types before property access
- [ ] Tests follow existing patterns in codebase
- [ ] TASK_SUMMARY.md updated with complete subtask entry
- [ ] Summary statistics updated
- [ ] Committed changes (if applicable)

---

## Task-Specific Insights: Code Coverage (37542)

### DomainOntology Module Characteristics

**Data Flow Pattern:**
```
API Data → Hook Processing → Visualization Format
```

**Common Transformations:**
1. **Chord Matrix** - Adjacency matrix for relationship visualization
2. **Sankey Diagram** - Flow diagram with links between nodes
3. **Sunburst Chart** - Hierarchical data in circular layout
4. **Tree Structure** - Hierarchical node structure with links
5. **Cluster** - Grouped hierarchical data

**Key Functions:**
- `mqttTopicMatch` - Critical for filter matching
- Link building between tags, topics, and filters
- Empty state handling for visualization components

### Testing Strategy for Visualization Hooks

1. **Test data transformation logic** - Core functionality
2. **Test empty state handling** - Fallback when no data
3. **Test relationship building** - Links between entities
4. **Test MQTT matching** - Wildcard filter logic
5. **Type safety** - Union type handling

---

## Future Recommendations

1. **Coverage Monitoring:** Set up automated coverage reporting to catch gaps early
2. **Test Templates:** Create templates for common hook testing patterns
3. **Type Safety:** Consider stricter TypeScript settings to catch union type issues
4. **Documentation:** Maintain this autonomy template as new patterns emerge
5. **Integration Tests:** Consider E2E tests for visualization component rendering

---

**Last Updated:** October 17, 2025  
**Task:** #37542 - Code Coverage Improvements  
**Status:** Active Template - Update as new patterns discovered
