# Conversation: Subtask 3 - Exclude Test Utilities from Code Duplication Detection

**Date:** October 25, 2025  
**Status:** ✅ Complete

## Objective

Configure SonarQube Cloud to exclude test utility directories from code duplication analysis, as duplication in test helpers is an acceptable trade-off for maintainability.

## Context

SonarQube Cloud was flagging code duplication in test utility directories:
- `**/__test-utils__/**` - directories containing test helpers and utilities
- `**/__handlers__/**` - directories containing mock service handlers

These directories contain intentionally similar code patterns for testing purposes, and should not be counted in code quality metrics.

## Problem

While these directories were already excluded from **coverage reporting** via `sonar.coverage.exclusions`, they were still being analyzed for **code duplication detection**, leading to inflated duplication metrics.

## Solution

Added `sonar.cpd.exclusions` property to `sonar-project.properties` to exclude these patterns from Copy/Paste Detection (CPD).

### Files Modified

**File:** `sonar-project.properties`

**Change:**
```properties
# Exclude following files from code duplication detection (CPD)
sonar.cpd.exclusions=\
    **/__test-utils__/**, \
    **/__handlers__/**
```

### Key Configuration Properties

1. **`sonar.coverage.exclusions`** - Excludes files from coverage analysis (already configured)
2. **`sonar.cpd.exclusions`** - Excludes files from code duplication detection (newly added)

These are separate properties because you might want different exclusion rules for each metric.

## Implementation Details

The glob patterns used:
- `**/__test-utils__/**` - Matches any directory named `__test-utils__` at any depth
- `**/__handlers__/**` - Matches any directory named `__handlers__` at any depth

The double asterisk (`**`) ensures the pattern matches these directories regardless of where they appear in the project structure.

## Testing

Changes will take effect on the next SonarQube scan, which happens automatically via the GitHub Actions workflow when code is pushed.

## Expected Outcome

After the next scan:
1. ✅ Test utility directories will not contribute to code duplication metrics
2. ✅ Code duplication percentage should decrease
3. ✅ Quality gate focused on production code quality
4. ✅ Test files remain excluded from coverage analysis

## Next Steps

1. Push the configuration change to the repository
2. Monitor the next SonarQube scan results
3. Verify duplication metrics reflect only production code
4. Consider if any other test-related patterns should be excluded

## References

- SonarQube CPD Documentation: https://docs.sonarsource.com/sonarqube/latest/project-administration/analysis-scope/
- Project: `hivemq_hivemq-edge` on SonarQube Cloud

