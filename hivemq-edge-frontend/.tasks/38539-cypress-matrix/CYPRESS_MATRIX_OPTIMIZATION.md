# Cypress Matrix Load Balancing Optimization

## Date

December 4, 2025

## Current Problem

The Cypress matrix in `.github/workflows/check-frontend.yml` has imbalanced load distribution:

| Job        | Duration   | Test Files | Notes                                 |
| ---------- | ---------- | ---------- | ------------------------------------- |
| E2E        | **25 min** | 35 files   | Slowest - integration tests           |
| Components | ~? min     | 60 files   | Component tests                       |
| Extensions | **21 min** | 75 files   | Component tests                       |
| Modules    | **21 min** | 103 files  | Component tests (excluding Workspace) |
| Workspace  | ~? min     | 53 files   | Component tests                       |

### E2E Test Distribution by Directory:

```
Login/       :  2 tests
adapters/    : 12 tests  ← Large
bridges/     :  1 test
datahub/     :  2 tests
demo/        :  0 tests
eventLog/    :  1 test
mappings/    :  1 test
pulse/       :  2 tests
workspace/   : 14 tests  ← Large
```

### Module Test Distribution by Directory:

```
Workspace/:           53 tests  ← Largest (already separated)
Mappings/:            17 tests  ← Large
Pulse/:               16 tests  ← Large
Bridges/:             11 tests
ProtocolAdapters/:    10 tests
DomainOntology/:       9 tests
Metrics/:              8 tests
Device/:               7 tests
UnifiedNamespace/:     5 tests
TopicFilters/:         5 tests
EventLog/:             5 tests
Login/:                3 tests
Welcome/:              2 tests
Notifications/:        2 tests
Dashboard/:            2 tests
Trackers/:             1 test
Theme/:                0 tests
Auth/:                 0 tests
App/:                  0 tests
---
Total (excluding Workspace): 103 tests
```

### Key Insight

E2E tests take 25 minutes for only 35 files because they're integration tests (slower per file). Component tests in Modules run faster despite having 103 files.

**Modules Split Insight:** The top 3 directories (Mappings, Pulse, Bridges) account for 44 tests (~43% of non-Workspace tests). Splitting these out could balance the load.

---

## Recommended Solutions

### Option 1: Split E2E Tests (RECOMMENDED - Immediate Balance)

**Strategy:** Split the longest-running job (E2E) into 3 smaller jobs based on subdirectories.

**Benefits:**

- ✅ Addresses the main bottleneck (25 min → ~8-10 min per job)
- ✅ Minimal change (5 jobs → 7 jobs)
- ✅ Clear logical split by feature area
- ✅ Easy to understand and maintain

**Implementation:**

```yaml
matrix:
  cypress: [
      # E2E split into 3 jobs
      { component: false, spec: './cypress/e2e/adapters/**/*', target: 'E2E-Adapters' },
      { component: false, spec: './cypress/e2e/workspace/**/*', target: 'E2E-Workspace' },
      { component: false, spec: './cypress/e2e/!(adapters|workspace)/**/*', target: 'E2E-Core' },

      # Component tests remain the same
      { component: true, spec: './src/components/**/*', target: 'Components' },
      { component: true, spec: './src/extensions/**/*', target: 'Extensions' },
      { component: true, spec: './src/modules/!(Workspace)**/*', target: 'Modules' },
      { component: true, spec: './src/modules/Workspace/**/*', target: 'Workspace' },
    ]
```

**Note:** The `!(adapters|workspace)` pattern uses extglob syntax to match any directory that is NOT adapters or workspace. This automatically includes any new test directories added in the future.

**Glob Pattern Syntax:**

- `!(pattern1|pattern2)` - Matches anything EXCEPT pattern1 or pattern2 (extglob)
- `**/*` - Matches all files recursively
- `{a,b,c}` - Matches exactly a, b, or c (brace expansion)

**Why negation is better:**

- ✅ Automatically includes new test directories (Login, bridges, datahub, etc.)
- ✅ No need to update workflow when adding new test folders
- ✅ More maintainable and future-proof

**Expected Result:**

- E2E-Adapters: ~10 min (12 tests)
- E2E-Workspace: ~12 min (14 tests)
- E2E-Core: ~8 min (9 tests)
- Total E2E time reduced from 25 min to ~12 min (parallel)

---

### Option 2: Split Modules (Largest File Count)

**Strategy:** Split the Modules job which has the most test files (103).

**Benefits:**

- ✅ Distributes the largest test count
- ✅ Could help if Modules is close to timeout
- ✅ Keeps E2E together (simpler to debug)

**Implementation:**

```yaml
matrix:
  cypress: [
      { component: false, spec: './cypress/e2e/**/*', target: 'E2E' },
      { component: true, spec: './src/components/**/*', target: 'Components' },
      { component: true, spec: './src/extensions/**/*', target: 'Extensions' },

      # Modules split into 2 jobs
      {
        component: true,
        spec: './src/modules/{Auth,Bridges,Device,DomainOntology,EventLog}/**/*',
        target: 'Modules-A',
      },
      {
        component: true,
        spec: './src/modules/{Mappings,Metrics,ProtocolAdapters,Pulse,TopicFilters,UnifiedNamespace}/**/*',
        target: 'Modules-B',
      },

      { component: true, spec: './src/modules/Workspace/**/*', target: 'Workspace' },
    ]
```

**Trade-off:** E2E still runs as one 25-minute job.

---

### Option 3: Wider Split (8-10 Jobs - Maximum Parallelization)

**Strategy:** More granular split across both E2E and component tests.

**Benefits:**

- ✅ Maximum parallelization
- ✅ Addresses both E2E and large component test groups
- ✅ Best overall balance

**Drawbacks:**

- ⚠️ More jobs = more complexity
- ⚠️ Higher CI minute usage (if concurrent)
- ⚠️ More artifacts to manage

**Implementation:**

```yaml
matrix:
  cypress: [
      # E2E split by major areas (3 jobs)
      { component: false, spec: './cypress/e2e/adapters/**/*', target: 'E2E-Adapters' },
      { component: false, spec: './cypress/e2e/workspace/**/*', target: 'E2E-Workspace' },
      {
        component: false,
        spec: './cypress/e2e/{Login,bridges,datahub,eventLog,mappings,pulse}/**/*',
        target: 'E2E-Core',
      },

      # Components unchanged
      { component: true, spec: './src/components/**/*', target: 'Components' },

      # Extensions - could split DataHub separately if needed
      { component: true, spec: './src/extensions/datahub/**/*', target: 'DataHub-Extensions' },

      # Modules split by functional area (3-4 jobs)
      { component: true, spec: './src/modules/ProtocolAdapters/**/*', target: 'ProtocolAdapters' },
      {
        component: true,
        spec: './src/modules/{Bridges,Mappings,UnifiedNamespace}/**/*',
        target: 'Modules-Connectivity',
      },
      {
        component: true,
        spec: './src/modules/{Auth,Device,EventLog,Metrics,Pulse,TopicFilters,DomainOntology}/**/*',
        target: 'Modules-Core',
      },
      { component: true, spec: './src/modules/Workspace/**/*', target: 'Workspace' },
    ]
```

**Total Jobs:** 9 jobs (vs. current 5)

---

### Option 4: Balanced Split Based on Test Counts (DATA-DRIVEN)

**Strategy:** Split based on actual test file counts to achieve optimal balance.

**Benefits:**

- ✅ Data-driven approach using actual test counts
- ✅ Balances both E2E and Modules workload
- ✅ Groups related functionality together
- ✅ Moderate complexity (7 jobs vs. current 5)

**Analysis:**
Based on test counts:

- E2E split: 12 + 14 + 9 = ~12 min each
- Modules-Heavy: 17 + 16 + 11 = 44 tests (~10 min)
- Modules-Medium: 10 + 9 + 8 + 7 = 34 tests (~8 min)
- Modules-Light: 5 + 5 + 5 + 3 + rest = 25 tests (~6 min)
- Workspace: 53 tests (already separate)

**Implementation:**

```yaml
matrix:
  cypress: [
      # E2E split into 3 jobs (same as Option 1)
      { component: false, spec: './cypress/e2e/adapters/**/*', target: 'E2E-Adapters' },
      { component: false, spec: './cypress/e2e/workspace/**/*', target: 'E2E-Workspace' },
      { component: false, spec: './cypress/e2e/!(adapters|workspace)/**/*', target: 'E2E-Core' },

      # Components unchanged
      { component: true, spec: './src/components/**/*', target: 'Components' },
      { component: true, spec: './src/extensions/**/*', target: 'Extensions' },

      # Modules split by size (3 groups + Workspace)
      { component: true, spec: './src/modules/{Mappings,Pulse,Bridges}/**/*', target: 'Modules-Heavy' },
      {
        component: true,
        spec: './src/modules/{ProtocolAdapters,DomainOntology,Metrics,Device}/**/*',
        target: 'Modules-Medium',
      },
      {
        component: true,
        spec: './src/modules/!(Workspace|Mappings|Pulse|Bridges|ProtocolAdapters|DomainOntology|Metrics|Device)**/*',
        target: 'Modules-Light',
      },
      { component: true, spec: './src/modules/Workspace/**/*', target: 'Workspace' },
    ]
```

**Expected Balance:**

- E2E-Adapters: ~10 min (12 tests)
- E2E-Workspace: ~12 min (14 tests)
- E2E-Core: ~8 min (9 tests)
- Components: ~? min (60 tests)
- Extensions: ~21 min (75 tests) ← Still the longest
- Modules-Heavy: ~10 min (44 tests)
- Modules-Medium: ~8 min (34 tests)
- Modules-Light: ~6 min (25 tests)
- Workspace: ~12 min (53 tests)

**Total Jobs:** 9 jobs

**Note:** Extensions (21 min) remains the bottleneck. Consider splitting it if needed.

---

### Option 5: Split Everything for Maximum Balance

**Strategy:** Split E2E, Modules, AND Extensions for optimal balance.

**Implementation:**

```yaml
matrix:
  cypress: [
      # E2E split
      { component: false, spec: './cypress/e2e/adapters/**/*', target: 'E2E-Adapters' },
      { component: false, spec: './cypress/e2e/workspace/**/*', target: 'E2E-Workspace' },
      { component: false, spec: './cypress/e2e/!(adapters|workspace)/**/*', target: 'E2E-Core' },

      # Components unchanged
      { component: true, spec: './src/components/**/*', target: 'Components' },

      # Extensions split (75 tests total - could split DataHub)
      { component: true, spec: './src/extensions/datahub/**/*', target: 'Extensions-DataHub' },

      # Modules split by size
      { component: true, spec: './src/modules/{Mappings,Pulse,Bridges}/**/*', target: 'Modules-Heavy' },
      {
        component: true,
        spec: './src/modules/{ProtocolAdapters,DomainOntology,Metrics,Device}/**/*',
        target: 'Modules-Medium',
      },
      {
        component: true,
        spec: './src/modules/!(Workspace|Mappings|Pulse|Bridges|ProtocolAdapters|DomainOntology|Metrics|Device)**/*',
        target: 'Modules-Light',
      },
      { component: true, spec: './src/modules/Workspace/**/*', target: 'Workspace' },
    ]
```

**Total Jobs:** 9 jobs with best overall balance

---

## Alternative: Dynamic Matrix with Spec Sharding

If you want automatic load balancing without manual splits:

```yaml
matrix:
  cypress: [
      { component: false, spec: './cypress/e2e/**/*', target: 'E2E', shard: '1/3' },
      { component: false, spec: './cypress/e2e/**/*', target: 'E2E', shard: '2/3' },
      { component: false, spec: './cypress/e2e/**/*', target: 'E2E', shard: '3/3' },
      # ... etc
    ]
```

Then pass `--shard=${{ matrix.cypress.shard }}` to Cypress.

**Note:** This requires Cypress Cloud or custom sharding logic.

---

## Implementation Recommendation

### Phase 1: Quick Win (Option 1)

1. Implement Option 1 (split E2E into 3 jobs)
2. Monitor actual run times
3. Target: All jobs complete in ~10-15 minutes

### Phase 2: Fine-Tuning (If Needed)

If other jobs approach 20+ minutes:

- Split Modules into 2 jobs (Option 2)
- Or implement full Option 3 for maximum parallelization

### Phase 3: Consider Cypress Cloud (Long-term)

For automatic load balancing and test parallelization:

- Cypress Cloud handles sharding automatically
- No manual matrix management needed
- Smart test distribution based on historical run times

---

## Expected CI Time Improvement

### Current (Worst Case)

```
Longest job: 25 minutes (E2E)
Total pipeline time: ~25 minutes
```

### After Option 1

```
Longest jobs: ~12 minutes (E2E-Workspace or Extensions)
Total pipeline time: ~12-15 minutes
Improvement: ~40-50% faster
```

### After Option 3

```
Longest jobs: ~8-10 minutes (more balanced)
Total pipeline time: ~10 minutes
Improvement: ~60% faster
```

---

## Implementation Steps

1. **Backup current workflow:**

   ```bash
   cp .github/workflows/check-frontend.yml .github/workflows/check-frontend.yml.backup
   ```

2. **Update the matrix section** (lines ~147-155 in check-frontend.yml)

3. **Test on a branch:**

   - Push changes to a test branch
   - Monitor actual run times
   - Verify all tests still execute correctly

4. **Adjust as needed:**

   - If one job is still too long, split it further
   - If jobs are too short, combine some

5. **Document the decision:**
   - Add comments in the workflow file explaining the split logic
   - Update team documentation

---

## File to Edit

**Location:** `.github/workflows/check-frontend.yml`

**Section:** Lines ~147-155 (cypress_matrix strategy)

**Current:**

```yaml
strategy:
  matrix:
    cypress:
      [
        { component: false, spec: './cypress/e2e/**/*', target: 'E2E' },
        { component: true, spec: './src/components/**/*', target: 'Components' },
        { component: true, spec: './src/extensions/**/*', target: 'Extensions' },
        { component: true, spec: './src/modules/!(Workspace)**/*', target: 'Modules' },
        { component: true, spec: './src/modules/Workspace/**/*', target: 'Workspace' },
      ]
```

**Replace with Option 1 (Recommended):**

```yaml
strategy:
  matrix:
    cypress: [
        # E2E tests split into 3 jobs for better load balancing
        { component: false, spec: './cypress/e2e/adapters/**/*', target: 'E2E-Adapters' },
        { component: false, spec: './cypress/e2e/workspace/**/*', target: 'E2E-Workspace' },
        { component: false, spec: './cypress/e2e/!(adapters|workspace)/**/*', target: 'E2E-Core' },
        # Component tests
        { component: true, spec: './src/components/**/*', target: 'Components' },
        { component: true, spec: './src/extensions/**/*', target: 'Extensions' },
        { component: true, spec: './src/modules/!(Workspace)**/*', target: 'Modules' },
        { component: true, spec: './src/modules/Workspace/**/*', target: 'Workspace' },
      ]
```

**Note:** Using `!(adapters|workspace)` for E2E-Core automatically includes any new test directories without workflow updates.

---

## Monitoring and Validation

After implementation, check:

- ✅ All tests still run (no tests skipped)
- ✅ Coverage reports still merge correctly
- ✅ Percy parallel execution still works
- ✅ Artifact uploads successful for all jobs
- ✅ No job exceeds 15 minutes
- ✅ Total pipeline time reduced

---

## Additional Considerations

### Percy Token Usage

More parallel jobs = more Percy snapshots potentially running simultaneously. Monitor Percy usage limits.

### GitHub Actions Minutes

7 jobs instead of 5 = 40% more job overhead (setup time), but faster overall completion. Net positive for developer experience.

### Artifact Storage

More jobs = more coverage artifacts. Current retention is 1 day, which is fine.

---

## Summary

### Quick Recommendations by Priority:

1. **Quick Win - Option 1:** Split E2E only (5 → 7 jobs)

   - **Improvement:** ~40-50% faster (25 min → ~12-15 min)
   - **Risk:** Low - straightforward change
   - **Effort:** 5 minutes

2. **Balanced Approach - Option 4:** Split E2E + intelligent Modules split (5 → 9 jobs)

   - **Improvement:** ~50-60% faster (25 min → ~10-12 min)
   - **Risk:** Low - data-driven split
   - **Effort:** 10 minutes
   - **Best overall balance** based on test counts

3. **Maximum Optimization - Option 5:** Split everything including Extensions
   - **Improvement:** ~60%+ faster (25 min → ~10 min)
   - **Risk:** Low-medium - more moving parts
   - **Effort:** 15 minutes

### Decision Guide:

| If your goal is...           | Use Option      |
| ---------------------------- | --------------- |
| Quick fix for E2E bottleneck | **Option 1**    |
| Best overall balance         | **Option 4** ✅ |
| Maximum parallelization      | Option 3 or 5   |
| Keep it simple               | Option 1 or 2   |

**Recommended:** Start with **Option 1**, monitor results, then upgrade to **Option 4** if needed.

---

## Appendix A: Test Count Commands

### Count Tests by Directory

**E2E tests:**

```bash
cd cypress/e2e && for dir in */; do echo "$dir: $(find "$dir" -name "*.cy.ts" | wc -l | xargs)"; done | sort -t: -k2 -rn
```

**Component tests in src/modules:**

```bash
cd src/modules && for dir in */; do echo "$dir: $(find "$dir" -name "*.cy.tsx" | wc -l | xargs)"; done | sort -t: -k2 -rn
```

**Component tests in src/extensions:**

```bash
cd src/extensions && for dir in */; do echo "$dir: $(find "$dir" -name "*.cy.tsx" | wc -l | xargs)"; done | sort -t: -k2 -rn
```

**All component tests (overview):**

```bash
echo "Components: $(find src/components -name "*.cy.tsx" | wc -l | xargs)"
echo "Extensions: $(find src/extensions -name "*.cy.tsx" | wc -l | xargs)"
echo "Modules (excl Workspace): $(find src/modules -name "*.cy.tsx" ! -path "*/Workspace/*" | wc -l | xargs)"
echo "Workspace: $(find src/modules/Workspace -name "*.cy.tsx" | wc -l | xargs)"
```

**Command breakdown:**

- `for dir in */` - Loop through subdirectories
- `find "$dir" -name "*.cy.tsx"` - Find test files
- `wc -l` - Count lines (number of files)
- `xargs` - Trim whitespace
- `sort -t: -k2 -rn` - Sort by count (descending)

---

## Appendix B: Glob Pattern Reference

### Negation Patterns (Extglob)

| Pattern        | Description                   | Example                                   |
| -------------- | ----------------------------- | ----------------------------------------- |
| `!(pattern)`   | Match anything except pattern | `!(adapters)` matches all except adapters |
| `!(a\|b)`      | Match anything except a or b  | `!(adapters\|workspace)`                  |
| `!(*.spec.ts)` | Match files except .spec.ts   | Useful for excluding test files           |

### Standard Glob Patterns

| Pattern | Description                         | Example                               |
| ------- | ----------------------------------- | ------------------------------------- |
| `*`     | Match any characters (single level) | `src/*.ts` matches files in src/      |
| `**`    | Match any characters (recursive)    | `src/**/*.ts` matches all .ts files   |
| `?`     | Match single character              | `test?.ts` matches test1.ts, testA.ts |
| `{a,b}` | Match exactly a or b                | `{Login,logout}`                      |
| `[abc]` | Match any character in set          | `test[123].ts`                        |

### Cypress-Specific Usage

```yaml
# Match all except specific folders
spec: './cypress/e2e/!(adapters|workspace)/**/*'

# Match specific file patterns
spec: './src/**/*.cy.{ts,tsx}'

# Match specific folders only
spec: './src/{components,hooks}/**/*'

# Exclude certain file types
spec: './src/**/!(*.spec).ts'
```

### Best Practices

✅ **Use negation for "catch-all" groups** - Automatically includes new content
✅ **Use explicit patterns for known large groups** - Better visibility of what's included
✅ **Combine both approaches** - Split known heavy folders, catch rest with negation

**Example (recommended):**

```yaml
# Large known folders get their own jobs
{ spec: './cypress/e2e/adapters/**/*', target: 'Adapters' }
{ spec: './cypress/e2e/workspace/**/*', target: 'Workspace' }

# Everything else caught by negation (future-proof)
{ spec: './cypress/e2e/!(adapters|workspace)/**/*', target: 'Other' }
```
