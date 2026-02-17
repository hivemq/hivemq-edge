---
title: "Claude Code — Tool Approval Behaviour"
date: "2026-02-17"
purpose: "Reference for understanding which tools require user approval when running skills and autonomous tasks"
---

# Claude Code — Tool Approval Behaviour

Understanding which tool calls pause for user approval is critical for designing skills and background tasks that can run autonomously without requiring constant supervision.

---

## Summary: What Requires Approval

| Tool | Requires approval? | Notes |
|------|--------------------|-------|
| `Read`, `Glob`, `Grep` | Never | Read-only, always safe |
| `Edit`, `Write` | No (in interactive mode) | File modifications are allowed without prompt |
| `Bash` — safe commands | No | `pnpm prettier`, `pnpm lint`, `git status` etc. |
| `Bash` — destructive patterns | Yes / blocked | `rm -rf`, `git reset --hard`, force push to main |
| `Task` (subagents) | No | Subagents inherit parent permission context |
| `AskUserQuestion` | Always | Intentional — designed to pause for human decision |
| MCP tools | Depends | Third-party MCP servers declare their own permission requirements |

**Observed in practice:** A full review-and-fix run across 39 documentation files — using Read, Glob, Grep, Edit (12 file modifications), and Bash (`pnpm prettier`) — completed with zero approval prompts.

---

## What Actually Pauses Execution

The interruptions you experience are not tool-level approval gates. They are:

1. **`AskUserQuestion` calls** — Intentional pauses when the agent needs a design decision (for example, "standalone doc or inline section?"). These are by design.
2. **Blocked Bash commands** — If a command matches a destructive heuristic (`rm -rf`, `git push --force` to main, etc.), Claude Code refuses or prompts. These are rare in documentation workflows.
3. **Hook-triggered blocks** — If you have Claude Code hooks configured (`.claude/settings.json`), hooks can intercept tool calls and block them with a message.

---

## Skill Design Implications

### Read-only skills — zero approval required

Skills with `allowed-tools: Glob, Grep, Read` are fully autonomous. The `technical-doc-writer` skill is designed this way:

```yaml
allowed-tools: Glob, Grep, Read
```

These skills can run as background tasks (`run_in_background: true`) with no supervision needed.

### Fix-applying skills — also zero approval required

Adding `Edit` to `allowed-tools` does not trigger approval prompts in interactive mode. A future `--fix` variant of `technical-doc-writer` could apply changes directly:

```yaml
allowed-tools: Glob, Grep, Read, Edit
```

The Edit calls themselves do not pause. Only a `Bash` command with a destructive pattern would.

### Bash-using skills — selective approval risk

Skills that use `Bash` are safe for most operations (`pnpm`, `git status`, `git log`). To be explicit about what is safe, use the `allowedPrompts` mechanism or configure `settings.json` allowlists.

---

## Pre-Approval Mechanisms

### 1. Plan mode `allowedPrompts`

When approving a plan via `ExitPlanMode`, you can declare semantic categories of actions that are pre-approved for the session:

```
allowedPrompts:
  - tool: Bash
    prompt: "run prettier on docs"
  - tool: Bash
    prompt: "run lint checks"
```

This covers an entire session without per-call interruption.

### 2. `settings.json` permanent allowlist

For operations that should always be permitted without prompting, add them to `.claude/settings.json`:

```json
{
  "permissions": {
    "allow": [
      "Bash(pnpm prettier*)",
      "Bash(pnpm lint*)",
      "Bash(pnpm test*)"
    ]
  }
}
```

These patterns apply to every task, every session, permanently.

### 3. Implicit approval by instruction

In interactive sessions, telling the agent "run it" or "apply the fixes" is an implicit approval for all Edit calls that follow. Claude Code does not re-prompt for each individual file edit within a task the user has explicitly initiated.

---

## Recommended Pattern for Autonomous Doc Skills

The cleanest pattern for documentation review and fix skills:

```
Phase 1 (review):   allowed-tools: Glob, Grep, Read     → always autonomous
Phase 2 (report):   output to conversation / write file → always autonomous
Phase 3 (fix):      allowed-tools: Glob, Grep, Read, Edit → autonomous in interactive session
Phase 4 (format):   Bash(pnpm prettier --write "docs/**") → autonomous (safe command)
```

No step in this pipeline requires user approval. The only decision point is the human reviewing the Phase 2 report and choosing to proceed with Phase 3.

---

## See Also

- [Claude Code settings documentation](https://docs.anthropic.com/en/docs/claude-code/settings)
- `.claude/skills/technical-doc-writer/SKILL.md` — the skill this analysis informed
- `.tasks/EDG-40-technical-documentation/DOC_QUALITY_REVIEW.md` — first review run results
