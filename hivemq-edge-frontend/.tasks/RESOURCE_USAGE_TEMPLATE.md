# Resource Usage Template

---

## Overview

This document describes how to track token usage, tool calls, and resource efficiency across all conversation sessions for a task.

---

### When to Save Conversation Summaries

| Token Usage | Action                   | Reason                            |
| ----------- | ------------------------ | --------------------------------- |
| 0-50%       | Optional                 | Plenty of budget remaining        |
| 50-70%      | Recommended              | Good checkpoint before major work |
| 70-85%      | **Strongly Recommended** | Approaching limits, save progress |
| 85-95%      | **Required**             | Critical - save immediately       |
| 95-100%     | **Emergency**            | Save and prepare to end session   |

### Optimization Tips Applied

1. **Batch file operations** - Group related changes together
2. **Use replace_string_in_file** - More efficient than insert_edit_into_file
3. **Read files strategically** - Only read necessary sections
4. **Minimize repeated operations** - Cache information when possible
5. **Clear communication** - Reduces back-and-forth token usage

---

## Tool Usage Best Practices

### Most Efficient Tools (This Session)

1. ✅ **`replace_string_in_file`** - 42 calls, very efficient for targeted changes
2. ✅ **`read_file`** - 35 calls, essential for context gathering
3. ✅ **`get_errors`** - 18 calls, crucial for validation

### Less Used But Important

- **`create_file`** - 3 calls, only when creating new files (test, docs)
- **`run_in_terminal`** - 11 calls, for compilation checks and verification
- **`grep_search`** - 8 calls, efficient for finding specific patterns

### Tools to Avoid (When Possible)

- **Multiple `read_file` calls for same file** - Cache information instead
- **Excessive `get_errors`** calls - Batch changes before checking
- **Unnecessary `list_dir`** calls - Know the structure in advance

---

## Recommendations for Future Sessions

### Continue Current Efficiency

✅ **Keep doing:**

- Batch related changes together
- Use efficient file operations
- Validate after completing groups of changes
- Document learnings immediately

### Potential Improvements

💡 **Consider:**

- Pre-read related files in parallel when starting complex tasks
- Group validation checks (fewer `get_errors` calls)
- Use more `grep_search` before `read_file` to find exact locations

### Budget Planning

For similar refactoring tasks (10+ files, comprehensive changes):

- **Estimated token usage:** 120,000-150,000 tokens
- **Recommended buffer:** 200,000 tokens total
- **Safe session length:** Can complete 5-6 similar tasks in one session

---

## Session Comparison Template

Use this template for future sessions:

```markdown
## Conversation Session N

**Date:** [Date]
**Duration:** [Description]
**Status:** [Completed/In Progress]

### Token Usage

- Total Tokens Used: [number]
- Tokens Remaining: [number]
- Percentage Used: [X.XX%]

### Tool Usage

- Total Tool Calls: [number]
- File Operations: [number]
- Terminal Operations: [number]
- Code Analysis: [number]

### Work Accomplished

- Files Modified: [number]
- Lines Changed: [estimate]
- Tests Created: [number]
- Documentation: [number] files

### Efficiency Metrics

- Tokens per file: [number]
- Tool calls per file: [number]
- Success rate: [percentage]

### Rating: ⭐⭐⭐⭐⭐ [1-5 stars]

[Brief analysis]
```

## Cumulative Statistics (All Sessions)

- add an updated cumulative statistics section at the end of the document, after the last session

### Total Resource Usage

| Metric                  | Session N | Total    |
| ----------------------- | --------- | -------- |
| **Tokens Used**         | [number]  | [number] |
| **Tool Calls**          | [number]  | [number] |
| **Files Modified**      | [number]  | [number] |
| **Tests Created**       | [number]  | [number] |
| **Documentation Pages** | [number]  | [number] |

### Efficiency Trends

**Token Efficiency Over Time:**

- Session 1: [number] of budget used ([efficiency rating])

**Average Metrics:**

- Tokens per file: [number]
- Tool calls per file: [number]
- Success rate: [number]

---

## Notes

### Why Track Resource Usage?

1. **Budget Management** - Know when to save progress
2. **Efficiency Analysis** - Identify optimization opportunities
3. **Session Planning** - Estimate resources for future tasks
4. **Quality Metrics** - Track success rates and effectiveness
5. **Learning** - Understand which approaches are most efficient

### Auto-Save Triggers

This document should be updated:

- ✅ When token usage crosses 20% thresholds (20%, 40%, 60%, 80%)
- ✅ When completing major milestones
- ✅ When user requests token usage information
- ✅ Before ending a session
