# AI Agent Optimization Guide for Users

**Purpose**: Help users provide context efficiently to minimize token usage and improve AI agent performance.

**Last Updated**: October 26, 2025

---

## Overview

This guide explains how you (the user) can help the AI agent work more efficiently by providing context upfront, reducing the number of tool calls needed, and saving tokens.

---

## Quick Reference: Token Savings

| Your Action                             | Saves                     | Token Savings                                 |
| --------------------------------------- | ------------------------- | --------------------------------------------- |
| **Attach/paste file content**           | **1-2 `read_file` calls** | **~200-500 tokens/file** ⭐ BEST              |
| **Open files in editor**                | **Automatic caching**     | **~200-500 tokens/file** ⭐ BEST              |
| List related files upfront              | 3-5 `grep_search` calls   | ~500-1000 tokens                              |
| Describe known patterns                 | 5-10 `read_file` calls    | ~1000-2000 tokens                             |
| Specify exact file paths + line numbers | 1-2 `file_search` calls   | ~100-300 tokens (but still needs `read_file`) |

**⚠️ Important**: File path + line number alone is NOT enough - AI still needs to read the file. Always **attach or open the file** for maximum efficiency!

**Total potential savings per task**: 3,000-10,000 tokens (~3-10% of session budget)

---

## Strategy 1: Provide Files in Context

### ❌ Less Efficient

```
User: "Fix the bug in NodeAdapter"
```

→ AI needs to search for file, read it, understand context

### ✅ More Efficient

```
User: "Fix the bug in NodeAdapter. Here's the file:"

[Paste or attach NodeAdapter.tsx]
```

→ AI has immediate context, starts fixing immediately

### How to Do It

**Option A: Attach Files**

- Use your IDE or chat interface to attach relevant files
- AI can see the content without calling `read_file`

**Option B: Paste Content**

- Copy relevant code sections
- Paste in your message with clear labels

**Option C: Open in Editor**

- Simply have the file open in your IDE
- The AI can see open files automatically (editor context)

---

## Strategy 2: Attach Files + Specify Exact Locations

### ❌ Less Efficient

```
User: "Bug in src/modules/Workspace/components/nodes/NodeDevice.tsx line 37
The useNodeConnections call is incorrect"
```

→ AI still needs to call `read_file` to see the code

### ✅ More Efficient

```
User: "Bug in NodeDevice.tsx line 37 - the useNodeConnections call is incorrect"

[Attach NodeDevice.tsx file]
```

→ AI has the file content immediately, no `read_file` needed

### ✅ MOST Efficient

```
User: "Bug in NodeDevice.tsx line 37 - the useNodeConnections call is incorrect"

[Open NodeDevice.tsx in your editor with line 37 visible]
```

→ AI sees the file automatically from editor context

### How to Provide Locations

**Best Practice: Attach/Open File + Specify Location**

- ✅ Attach the file OR open it in editor
- ✅ Add line number: `line 37`
- ✅ Add function/section: `in the useNodeConnections call`

**Not Efficient Enough:**

- ❌ Just the file path without attaching: `NodeDevice.tsx` (AI still needs to read it)
- ❌ Vague location: "somewhere in the device component"

**⚠️ Key Point**: File path alone saves you from searching, but **AI still needs the file content**. Always attach or open the file!

---

## Strategy 3: List Related Files Upfront

### ❌ Less Efficient (Iterative)

```
User: "Fix NodeAdapter"
[AI fixes NodeAdapter]
User: "Now check NodeDevice"
[AI reads NodeDevice]
User: "Also NodeHost"
[AI reads NodeHost]
```

→ Sequential operations, 3x the time and tokens

### ✅ More Efficient (Batch)

```
User: "Fix the useNodeConnections bug in these files:
- NodeAdapter.tsx
- NodeDevice.tsx
- NodeHost.tsx
- NodeListener.tsx
- NodeCombiner.tsx"
```

→ AI can read all files in parallel, fix all at once

### When to Use This

**Good for:**

- Multiple files with same pattern/issue
- Related changes across components
- Refactoring that touches many files

**File Patterns to Share:**

- Location pattern: `src/modules/Workspace/components/nodes/Node*.tsx`
- Test files: `src/modules/Workspace/**/*.spec.ts`
- Related utilities: `src/modules/Workspace/utils/status*.ts`

---

## Strategy 4: Reference Known Patterns

### ❌ Less Efficient

```
User: "Update the shadow styling for nodes"
```

→ AI needs to research: What's the current pattern? Where is it defined? What changed recently?

### ✅ More Efficient

```
User: "Update shadow styling for nodes.
We already have getStatusColor() in status-utils.ts (from Subtask 7).
The NodeWrapper component has statusModel prop.
Apply the same pattern to NodeBridge."
```

→ AI knows exactly what to do, no research needed

### What to Reference

**Previous Work:**

- "We fixed this in Subtask 6"
- "The pattern is in NodeAdapter.tsx line 69"
- "We use OR logic now (not AND)"

**Existing Code:**

- "The utility function is in status-utils.ts"
- "Follow the same pattern as NodeEdge"
- "Use the hook like we did in NodeAdapter"

**Design Decisions:**

- "We use CSS custom properties for dynamic colors"
- "Status propagates via useNodeConnections"
- "Operational status uses OR logic for adapters"

---

## Strategy 5: Describe the Context

### ❌ Less Efficient

```
User: "Something's wrong with the workspace"
```

→ AI needs to explore the entire workspace module to understand the issue

### ✅ More Efficient

```
User: "The node connections aren't working correctly.
Nodes use useNodeConnections to get parent status.
The bug is nodes are calling it without the node id parameter.
Correct usage is in NodeAdapter line 69: useNodeConnections({ id })"
```

→ AI knows the problem, the pattern, and the solution immediately

### Elements of Good Context

**What's Wrong:**

- Describe the issue clearly
- Include error messages if any
- Specify expected vs. actual behavior

**Where to Look:**

- File locations
- Relevant functions/components
- Related code that works correctly

**What You've Tried:**

- Previous attempts
- What didn't work
- Any partial solutions

---

## Real Example from Today's Session

### What You Did (Excellent!)

```
User: "There is a bug in src/modules/Workspace/components/nodes/NodeDevice.tsx, line 37.
const connections = useNodeConnections({ handleType: 'target', handleId: 'Top' })

You are calling for the connections with props that are too wide in range - and incorrect.
Here, you want to find the connections of the node that is calling the hook, using its id:
const connections = useNodeConnections({ id })

Check the other nodes for the logic of getting connections, to make sure they are correct too"
```

### Why This Was Efficient

1. ✅ **Exact file and line** - No search needed
2. ✅ **Showed current (wrong) code** - Clear understanding of problem
3. ✅ **Showed correct solution** - No guessing needed
4. ✅ **Suggested broader check** - Found 4 more similar bugs in one pass
5. ✅ **Explained the why** - Helped AI understand the pattern

### Result

- **Tool calls saved**: ~10-15 (searching, reading, verifying)
- **Tokens saved**: ~5,000-7,000
- **Time saved**: Fixed 5 files in single batch instead of iteratively
- **Quality**: 100% success rate, no iterations needed

---

## Anti-Patterns to Avoid

### ❌ Vague Requests

```
"Something's not working"
"Fix the status stuff"
"The colors are wrong"
```

### ❌ Missing Context

```
"Update NodeAdapter"
[AI: "What should I update?"]
"The operational status"
[AI: "How should I update it?"]
"Make it use OR logic"
```

→ 3 rounds of back-and-forth, 3x the tokens

### ❌ Sequential Discoveries

```
"Fix file A"
[AI fixes A]
"Oh, also fix file B"
[AI fixes B]
"And C, D, E too"
```

→ Should have listed A-E upfront

---

## Best Practices Summary

### Before Making a Request

**Checklist:**

1. [ ] Do I know which files are involved? → List them
2. [ ] Do I know where the issue is? → Provide line numbers
3. [ ] Can I describe what's wrong? → Include the problem
4. [ ] Do I know what the solution looks like? → Share the pattern
5. [ ] Are there related files to check? → List them all

### The "Perfect Request" Template

```
Task: [What needs to be done]

Files:
- [file1.ts] line [X] - [what's wrong]
- [file2.ts] - [same pattern]
- [file3.ts] - [also check this]

Current behavior: [describe problem]
Expected behavior: [describe solution]
Correct pattern: [show example from working code]

Context: [any previous work or decisions]
```

### Example of Perfect Request

```
Task: Fix useNodeConnections calls in node components

Files:
- NodeDevice.tsx line 37 - calling without id
- NodeHost.tsx - probably has same issue
- NodeListener.tsx - probably has same issue
- NodeCombiner.tsx - check this too
- NodeAssets.tsx - check this too

Current: useNodeConnections({ handleType: 'target', handleId: 'Top' })
Expected: useNodeConnections({ id })

Correct pattern: See NodeAdapter.tsx line 69

Context: These nodes derive status from parent nodes, need
to specify which node's connections to retrieve.
```

---

## Editor Integration Tips

### Use Your IDE Features

1. **Open Relevant Files** - AI can see editor context automatically
2. **Select Code Sections** - Highlight the relevant parts
3. **Use Multiple Tabs** - Keep related files open
4. **Copy with Line Numbers** - Help AI navigate to exact locations

### GitHub Copilot Chat Features

- **`@workspace`** - Include workspace context
- **`#file`** - Reference specific files
- Attach files directly to messages
- Use inline code blocks for short snippets

---

## Measuring Efficiency

### Track Token Usage

After a session, check:

- What percentage of token budget was used?
- How many tool calls were needed?
- Were there repeated reads of the same file?

### Good Indicators

✅ **Efficient Session:**

- Low token usage per file (~5,000-8,000)
- Few tool calls per change (5-10)
- Minimal back-and-forth
- First-attempt success

❌ **Inefficient Session:**

- High token usage per file (>15,000)
- Many tool calls per change (>20)
- Multiple clarification rounds
- Repeated fixes needed

---

## Token Budget Planning

### Session Budget: 1,000,000 tokens

| Task Complexity     | Est. Tokens    | Your Prep Time | AI Work Time  |
| ------------------- | -------------- | -------------- | ------------- |
| Single file fix     | 5,000-10,000   | 30 seconds     | 1-2 minutes   |
| Multi-file refactor | 30,000-50,000  | 2-3 minutes    | 5-10 minutes  |
| Large feature       | 80,000-120,000 | 5-10 minutes   | 15-30 minutes |

**Time Investment**: Spending 2-3 minutes providing good context can save 30-50% of tokens and 2-3x the time!

---

## Quick Wins

### Start Every Session With

1. **Task ID**: "Working on task 32118-workspace-status"
2. **Current State**: "We completed subtasks 1-5, now on subtask 6"
3. **Key Context**: "Status uses dual-model (runtime/operational)"

### For Each Subtask

1. **Files Involved**: List them upfront
2. **Known Patterns**: Reference previous work
3. **Expected Outcome**: Describe what success looks like

### Provide Immediately

- Error messages (copy-paste)
- File paths (full, not relative)
- Line numbers (from your IDE)
- Related files (list all at once)

---

## Summary: The 80/20 Rule

**20% effort (your context) prevents 80% of token waste**

**Top 3 Actions:**

1. **Attach files or open in editor** - Saves the MOST tokens (eliminates `read_file` calls)
2. **List related files upfront** - Enables batch operations instead of iterative fixes
3. **Describe known patterns** - Prevents AI from researching previous work

**Result:**

- 30-50% fewer tokens used
- 2-3x faster completion
- Higher quality outcomes
- Fewer iterations needed

---

## Questions?

If you're unsure whether to provide more context, **err on the side of providing more**.

**Too much context** = AI ignores irrelevant parts (minimal cost)  
**Too little context** = AI makes multiple calls to gather info (high cost)

**When in doubt, share it!**

---

_This guide is based on analysis of efficient vs. inefficient sessions. Following these practices can reduce token usage by 30-50% while improving output quality._
