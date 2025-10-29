# Combined Code Coverage

This directory contains merged coverage reports from multiple test sources:

- Vitest unit tests (`coverage-vitest/`)
- Cypress E2E tests
- Cypress Component tests (matrix runs)

## Directory Structure

```
coverage-combined/
├── README.md           # This file
├── e2e/               # Individual E2E test coverage
├── components/        # Individual component test coverage
├── extensions/        # Individual extension test coverage
├── modules/           # Individual module test coverage
├── index.html         # Final merged HTML report
├── lcov.info          # Final merged LCOV report
└── coverage-final.json # Final merged JSON report
```

## Usage

### Run Cypress Matrix Tests and Merge Coverage

```bash
# Run all Cypress matrix tests and merge their coverage
pnpm run cypress:coverage:report
```

This will:

1. Run E2E tests → `coverage-combined/e2e/`
2. Run component tests → `coverage-combined/components/`
3. Run extension tests → `coverage-combined/extensions/`
4. Run module tests → `coverage-combined/modules/`
5. Merge all reports → `coverage-combined/` (root)

### Merge Existing Cypress Coverage Only

```bash
# If you already ran the tests, just merge the coverage
pnpm run coverage:merge:cypress
```

### Merge ALL Coverage (Vitest + Cypress)

```bash
# Run all tests and merge everything
pnpm run coverage:report:all
```

Or manually:

```bash
# 1. Run Vitest tests with coverage
pnpm run test:coverage

# 2. Run Cypress matrix tests
pnpm run cypress:run:matrix

# 3. Merge all coverage (including Vitest)
pnpm run coverage:merge:all
```

### Merge Only Cypress Coverage (Already Run)

```bash
# Merge just Cypress reports
node tools/merge-cypress-coverage.cjs
```

### View Reports

After merging, open the HTML report:

```bash
# macOS
open coverage-combined/index.html

# Linux
xdg-open coverage-combined/index.html

# Windows
start coverage-combined/index.html
```

## How It Works

1. **Individual Test Runs**: Each Cypress matrix script runs tests on a subset of files and copies the coverage to a subdirectory (e2e/, components/, etc.)

2. **NYC Merge**: The merge scripts use NYC to combine all `coverage-final.json` files into a single merged report

3. **Report Generation**: NYC generates HTML, LCOV, and JSON reports from the merged coverage data

## Troubleshooting

### No coverage files found

Make sure you've run the tests first:

```bash
pnpm run cypress:run:matrix
```

### Coverage seems incomplete

Check that all matrix scripts completed successfully. You can run them individually:

```bash
pnpm run cypress:run:matrix:e2e
pnpm run cypress:run:matrix:components
pnpm run cypress:run:matrix:extensions
pnpm run cypress:run:matrix:modules
```

### Vitest coverage not included

Use `--include-vitest` flag or the dedicated script:

```bash
node tools/merge-all-coverage.cjs --include-vitest
# or
pnpm run coverage:merge:all
```
