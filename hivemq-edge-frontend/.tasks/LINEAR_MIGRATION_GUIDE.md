# Linear Migration Guide

**Migration Date:** February 16, 2026
**Previous System:** BusinessMap (Kanbanize)
**New System:** Linear

---

## What Changed

### Task Identifier Format

**Before (BusinessMap):**

- Card numbers: `38943`, `33168`, `37542`
- No consistent prefix
- URL: `https://businessmap.io/c/57/{card-id}`

**After (Linear):**

- Issue identifiers: `EDG-40`, `EDG-38`, `EDG-35`
- Team prefix + issue number
- URL: `https://linear.app/hivemq/issue/{issue-id}/...`

### Task Directory Naming

**Pattern remains the same:** `.tasks/{task-id}-{short-description}/`

**Examples:**

| System      | Issue/Card   | Directory Name                           |
| ----------- | ------------ | ---------------------------------------- |
| BusinessMap | Card 38943   | `.tasks/38943-mapping-ownership-review/` |
| Linear      | Issue EDG-40 | `.tasks/EDG-40-technical-documentation/` |

**Key Rules:**

- Use the Linear issue identifier (e.g., `EDG-40`)
- Include the team prefix (e.g., `EDG`, not just `40`)
- Use hyphens throughout (no slashes)
- Match branch name pattern without slashes

### Branch Naming

**Git branches may include slashes:**

- `feat/EDG-40/technical-documentation` ✅
- `fix/EDG-38/readonly-schemas` ✅

**Task directories use hyphens:**

- `.tasks/EDG-40-technical-documentation/` ✅
- `.tasks/EDG-38-readonly-schemas/` ✅

---

## Updated Documentation

The following files were updated to reflect the Linear migration:

1. **`.github/AI_MANDATORY_RULES.md`**

   - Rule 7: Task Documentation Structure & Naming
   - Examples updated to use Linear issue identifiers

2. **`CLAUDE.md`**

   - Task Documentation section updated
   - Added Linear pattern explanation

3. **`.tasks/README.md`**

   - Integration section updated
   - Examples updated to Linear patterns

4. **`.tasks/PULL_REQUEST_TEMPLATE.md`**

   - Updated URL patterns from BusinessMap to Linear
   - Updated instructions for getting issue URLs

5. **`.tasks/DEFAULT_BOARD.md`**
   - Completely rewritten for Linear teams
   - Now titled "Default Team Configuration"
   - Includes Edge team details and Linear patterns

---

## For AI Agents

### When Creating New Tasks

1. Get the Linear issue identifier (e.g., `EDG-40`)
2. Create directory: `.tasks/{issue-id}-{description}/`
3. Create standard files: `TASK_BRIEF.md`, `README.md`, etc.

### When Referencing Issues

Use Linear issue identifiers in all documentation:

- ✅ "Working on EDG-40"
- ✅ "See EDG-38 for context"
- ❌ "Working on card 40"
- ❌ "See issue 38"

### Getting Linear Issue URLs

Use the Linear MCP server:

```typescript
// Get issue details
mcp__linear-server__get_issue({ id: "EDG-40" })

// Returns:
{
  "url": "https://linear.app/hivemq/issue/EDG-40/...",
  "identifier": "EDG-40",
  "title": "Frontend handover documentation",
  ...
}
```

---

## Historical Tasks

**Do NOT update historical task directories that reference BusinessMap.**

These are historical records:

- `.tasks/38943-mapping-ownership-review/` (completed)
- `.tasks/33168-duplicate-combiner/` (completed)
- `.tasks/37542-code-coverage/` (completed)

Keep their references to BusinessMap unchanged for historical accuracy.

---

## Pull Request Updates

### New PR Template Format

**Old:**

```markdown
**Kanban Ticket:** https://businessmap.io/c/57/33168
```

**New:**

```markdown
**Linear Issue:** https://linear.app/hivemq/issue/EDG-33/duplicate-combiner-detection
```

### Getting Issue URLs for PRs

**With Linear MCP:**

1. Use `mcp__linear-server__get_issue(id: "EDG-40")`
2. Copy the `url` field from response

**Manually:**

1. Open issue in Linear
2. Copy URL from browser address bar
3. Format: `https://linear.app/hivemq/issue/{ISSUE-ID}/...`

---

## Common Patterns

### Creating a New Task

```bash
# 1. Get Linear issue identifier
# User assigns you EDG-40

# 2. Create task directory
mkdir -p .tasks/EDG-40-technical-documentation

# 3. Create standard files
cd .tasks/EDG-40-technical-documentation
touch TASK_BRIEF.md README.md

# 4. Create matching branch (can use slashes)
git checkout -b feat/EDG-40/technical-documentation
```

### Linking to Issues

**In Markdown:**

```markdown
This fixes the issue described in EDG-40.

See related work:

- EDG-38: Read-only schema handling
- EDG-35: Tag rendering improvements

**Linear Issue:** https://linear.app/hivemq/issue/EDG-40/...
```

**In Commit Messages:**

```bash
git commit -m "feat(docs): add technical documentation

Addresses EDG-40 requirements for frontend handover docs.

Linear: https://linear.app/hivemq/issue/EDG-40/..."
```

---

## FAQs

### Q: Do I need to rename old task directories?

**A:** No. Historical tasks keep their original names for continuity.

### Q: What if a branch name has slashes but task directory can't?

**A:** Use slashes in branch names, hyphens in directory names:

- Branch: `feat/EDG-40/technical-documentation`
- Directory: `.tasks/EDG-40-technical-documentation/`

### Q: How do I find the Linear issue ID?

**A:** Use Linear MCP or check the Linear UI. Format is always `{TEAM-KEY}-{NUMBER}` (e.g., `EDG-40`).

### Q: Should commit messages reference Linear?

**A:** Yes, include the Linear issue identifier and optionally the full URL:

```
feat: implement feature

Implements EDG-40 requirements.

Linear: https://linear.app/hivemq/issue/EDG-40/...
```

### Q: What about old PRs that reference BusinessMap?

**A:** Leave them as-is. They're historical records. Only update new PRs.

---

## Checklist: Creating a New Task

- [ ] Get Linear issue identifier (e.g., `EDG-40`)
- [ ] Create directory: `.tasks/{issue-id}-{description}/`
- [ ] Create branch: `feat/{issue-id}/{description}` (can use slashes)
- [ ] Create `TASK_BRIEF.md` with Linear issue details
- [ ] Reference Linear issue in all documentation
- [ ] Use Linear URL in PR template
- [ ] Commit with Linear issue reference

---

**Last Updated:** February 16, 2026
**Related Documents:**

- `.github/AI_MANDATORY_RULES.md` - Rule 7
- `CLAUDE.md` - Task Documentation section
- `.tasks/README.md` - Task system overview
- `.tasks/PULL_REQUEST_TEMPLATE.md` - PR template
