# Coverage Testing - Simple Guide

## Overview

The HiveMQ Edge Frontend project uses two test frameworks:

- **Vitest** for unit tests
- **Cypress** for E2E and component tests

Coverage reports are generated separately and need to be merged for a complete picture.

## Quick Commands

### Cypress Only

```bash
# Run Cypress tests and generate coverage (sequential)
pnpm run cypress:coverage

# Run Cypress tests in parallel (faster)
pnpm run cypress:coverage:parallel

# Run with verbose output (shows test failure details)
pnpm run cypress:coverage:verbose
pnpm run cypress:coverage:parallel:verbose
```

### All Tests (Vitest + Cypress) 🚀

```bash
# Run ALL tests (Vitest + Cypress) in parallel - FASTEST!
pnpm run coverage:all:parallel

# Run ALL tests sequentially
pnpm run coverage:all

# Run with verbose output (shows test failure details)
pnpm run coverage:all:verbose
pnpm run coverage:all:parallel:verbose
```

### Merge Only

```bash
# Merge Cypress coverage only
pnpm run coverage:merge:cypress

# Merge Vitest + Cypress coverage
pnpm run coverage:merge:all
```

## How It Works

1. **Tests run** in groups:
   - **Vitest** (when using `--all` flag)
   - **Cypress E2E** tests
   - **Cypress Component** tests (components, extensions, modules)
2. **Coverage is collected** from each group
3. **Coverage is merged** automatically (uses appropriate merge script)
4. **Report generated** at `coverage-combined/index.html`

## Example Output (All Tests)

```
🚀 Running all tests (Vitest + Cypress) in parallel...

▶ Starting: vitest
▶ Starting: e2e
▶ Starting: components
▶ Starting: extensions
▶ Starting: modules
✓ Completed: vitest (12.3s)
✓ Completed: extensions (17.8s)
✓ Completed: components (18.2s)
Still running: e2e, modules
✗ Failed: e2e (exit code: 2, 19.4s)
Still running: modules
✓ Completed: modules (45.3s)

══════════════════════════════════════════════════
📊 Test Results Summary
══════════════════════════════════════════════════

✓ Passed: 4/5
  • vitest
  • components
  • extensions
  • modules

✗ Failed: 1/5
  • e2e

══════════════════════════════════════════════════
🔄 Merging Coverage Reports
══════════════════════════════════════════════════

Merging 5 coverage file(s) (Vitest + Cypress)...
✓ Coverage merged successfully
📁 View: coverage-combined/index.html
```

Each test group has its own color for easy tracking during parallel execution!

## Features

- ✅ Sequential or parallel execution
- ✅ Failing tests don't stop the script
- ✅ Coverage always collected and merged
- ✅ Progress feedback during execution
- ✅ Verbose mode available
- ✅ Coverage thresholds disabled during merge (no errors on low coverage)
- ✅ Exit code 0 when coverage merges successfully (even if tests fail)
- ✅ Clickable file:// link to HTML report
- ✅ Videos: disabled by default, enabled in verbose mode
- ✅ Clean output - test failure details only shown in verbose mode

## Clean Output Mode (Default)

By default, test output is kept minimal for clean coverage runs:

**What you see:**

```
✗ Failed: e2e (exit code: 2, 42.66s)
```

**What's hidden:**

- Stack traces
- Cypress runner internal errors
- Verbose test output
- Video recordings

**Why?** During coverage runs, you typically just need to know:

- Which tests passed/failed
- How long they took
- The final coverage report

**Need details?** Use verbose mode:

```bash
pnpm run cypress:coverage:parallel:verbose
```

This shows:

- ✅ All test output including stack traces
- ✅ Error details
- ✅ Video recordings for failed tests

## Exit Code Behavior

The script now exits with code 0 when coverage is successfully merged, **even if some tests failed**. This prevents the "Command failed with exit code 1" error at the end.

- **Exit 0**: Coverage merged successfully (tests may have failed, but coverage is available)
- **Exit 1**: Coverage merge failed (something went wrong with the merge process)

If tests failed but coverage was collected, you'll see:

```
⚠️  Note: Some tests failed, but coverage was collected and merged
```

## Video Recording

Video recording behavior depends on the mode:

| Mode        | Videos      | Use Case                |
| ----------- | ----------- | ----------------------- |
| **Default** | ❌ Disabled | Fast coverage runs      |
| **Verbose** | ✅ Enabled  | Debugging test failures |

- **Default mode**: `CYPRESS_video=false` - Fast execution, no videos
- **Verbose mode**: `CYPRESS_video=true` - Full debugging with videos + error output

**Why?**

- Normal runs: Speed and disk space matter
- Verbose runs: You're debugging, so you want all the information including videos

Examples:

```bash
# Fast, no videos
pnpm run cypress:coverage:parallel

# Debugging mode with videos + full output
pnpm run cypress:coverage:parallel:verbose
```

```bash
pnpm run cypress:open        # Interactive mode
pnpm run cypress:run:e2e     # Headless with videos
```

## Headless vs Headed Mode

Coverage runs use **headless mode** (`cypress run`) which is the **recommended mode for performance**:

| Mode            | Command        | Speed  | Use Case                        |
| --------------- | -------------- | ------ | ------------------------------- |
| **Headless** ✅ | `cypress run`  | Fast   | Coverage, CI/CD, automated runs |
| **Headed**      | `cypress open` | Slower | Debugging, development          |

**Current coverage commands use headless mode** - no changes needed for optimal performance!

### Performance Optimizations Already Enabled

✅ **Headless mode** - Runs without browser UI  
✅ **Video disabled** - Saves time and disk space  
✅ **Parallel execution** - Multiple test groups run simultaneously  
✅ **Quiet mode** (`-q`) - Minimal console output  
✅ **Retries** - 2 automatic retries for flaky tests (from `cypress.config.ts`)

## Coverage Thresholds

The merge scripts disable NYC coverage threshold checking (`--check-coverage=false`) to ensure the merge completes successfully even when coverage is below your configured thresholds in `.nycrc.json`.

This means:

- Merging always succeeds regardless of coverage percentage
- You can still view the actual coverage in the HTML report
- CI/CD can enforce thresholds separately if needed

## Test Groups

Edit `tools/run-tests.cjs` to modify test groups:

```javascript
const ALL_TESTS = [
  { name: 'vitest', type: 'vitest', spec: null, color: colors.blue },
  { name: 'e2e', type: 'e2e', spec: './cypress/e2e/Login/**/*', color: colors.cyan },
  { name: 'components', type: 'component', spec: './src/components/**/*', color: colors.green },
  // ... add more groups
]
```

**Note**: Vitest is only included when using the `--all` flag. Without it, only Cypress tests run.

## Excluding Files/Folders from Coverage

To exclude specific files or folders from coverage, edit `coverage.config.cjs`:

```javascript
module.exports = {
  exclude: [
    // Test files
    'cypress/**/*.*',
    '**/*.cy.tsx',
    '**/*.spec.ts',

    // Add your custom exclusions:
    'src/legacy/**/*', // Exclude legacy code
    'src/experimental/**/*', // Exclude experimental features
    'src/**/deprecated/**/*', // Exclude deprecated modules
    '**/generated/**/*', // Exclude generated code
  ],
}
```

### Where Exclusions Apply

1. **NYC (Cypress coverage)** - Uses patterns from `.nycrc.json` (keep in sync with `coverage.config.cjs`)
2. **Vitest** - Edit `vitest.config.ts` > `test.coverage.exclude`
3. **Cypress plugin** - Edit `cypress.config.ts` > `env.codeCoverage.exclude`

**Tip**: Keep all three configs in sync for consistent coverage across all test types. The `coverage.config.cjs` serves as the reference.

## Scripts

- `tools/run-tests.cjs` - Main test runner
- `tools/merge-cypress-coverage.cjs` - Merge Cypress coverage
- `tools/merge-all-coverage.cjs` - Merge Vitest + Cypress

## Technical Details

### NYC Merge Process

1. **Collect**: Find all `coverage-final.json` files from various directories
2. **Copy**: Copy them to a temporary directory with unique names
3. **Merge**: Use `nyc merge` to combine into a single coverage object
4. **Report**: Use `nyc report` to generate HTML, LCOV, and JSON outputs

### NYC Configuration

The project uses multiple NYC configurations:

#### .nycrc.json (Main)

Default configuration for standard Cypress runs.

- Output: `coverage-cypress/`

#### .nycrc.matrix.json

Configuration for matrix Cypress runs (currently not actively used, but available for custom configurations).

### File Format

NYC uses Istanbul's JSON coverage format. Each `coverage-final.json` contains:

```json
{
  "path/to/file.ts": {
    "path": "path/to/file.ts",
    "statementMap": { ... },
    "fnMap": { ... },
    "branchMap": { ... },
    "s": { ... },
    "f": { ... },
    "b": { ... }
  }
}
```

When merging, NYC combines coverage data by file path and merges the execution counts.
