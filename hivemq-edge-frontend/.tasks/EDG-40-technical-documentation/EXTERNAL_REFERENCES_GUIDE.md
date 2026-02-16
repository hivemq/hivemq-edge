# External References Guide

**Task:** EDG-40-technical-documentation
**Date:** 2026-02-13

This document addresses how to handle external references in documentation.

---

## 1. MIRO Boards Integration

### Current Capability

**I cannot read MIRO boards directly.** MIRO boards require authentication and no native integration exists.

### Recommended Solutions

#### Option A: Recreate Key Diagrams as Mermaid (RECOMMENDED)

**Benefits:**

- Version-controlled
- WCAG AA accessible
- Renders in GitHub/GitLab
- No authentication needed

**Process:**

1. Extract key diagrams from MIRO
2. Recreate as Mermaid diagrams following RULE 9
3. Link to MIRO as "source of truth"

**Example:**

````markdown
## Architecture Overview

```mermaid
%%{init: {'theme':'base', 'themeVariables': {'primaryColor':'#0066CC'}}}%%
flowchart TD
    A[Client] --> B[API Gateway]
    B --> C[Backend]
```
````

**Source:** [MIRO Board - Full Architecture](https://miro.com/app/board/xxx)

````

#### Option B: Export and Store

**Process:**
1. Export MIRO boards as PDF/PNG
2. Store in `docs/assets/miro/`
3. Link to both MIRO URL and exported file

**Example:**
```markdown
**Architecture Diagram:**
- [Live MIRO Board](https://miro.com/...) (requires access)
- [PDF Export](./assets/miro/architecture-overview.pdf) (static snapshot)
````

**Directory Structure:**

```
docs/
└── assets/
    └── miro/
        ├── architecture-overview.pdf
        ├── data-flow-diagram.png
        └── README.md  # Export date, source URLs
```

#### Option C: MIRO MCP Server (IF AVAILABLE)

Check if MIRO MCP server exists:

- Search: https://github.com/modelcontextprotocol/servers
- If available, add to `.claude/settings.local.json`

**Currently:** No MIRO MCP server found.

### Recommendation

**Use Option A** (Mermaid) for critical diagrams + **Option B** (exports) for complex boards.

**Benefits:**

- Mermaid diagrams: Accessible, version-controlled, always up-to-date
- PDF/PNG exports: Preserve full complexity when Mermaid insufficient
- MIRO links: Source of truth for those with access

---

## 2. Pull Request References

### Current Capability

**Yes, I can access historical PRs via:**

### A. GitHub CLI (`gh`)

**List PRs:**

```bash
# All PRs (paginated)
gh pr list --state all --limit 100

# By label
gh pr list --label "architecture" --state all

# By date range
gh pr list --search "created:2023-01-01..2024-12-31" --state all

# By search term
gh pr list --search "Chakra" --state closed
```

**View PR Details:**

```bash
# Full PR information
gh pr view 1234 --json title,body,labels,files

# Files changed
gh pr view 1234 --json files | jq '.files[].path'

# Comments
gh api repos/hivemq/hivemq-edge/pulls/1234/comments
```

**Find PRs by File:**

```bash
# PRs that touched specific file
gh pr list --search "path:src/modules/Workspace" --state all
```

### B. Git Commands (Enhanced with View Access)

**File History:**

```bash
# See all changes to a file
git log --follow -- src/modules/Workspace/WorkspaceCanvas.tsx

# With diffs
git log -p --follow -- src/modules/Workspace/WorkspaceCanvas.tsx
```

**Feature Introduction:**

```bash
# Find when feature was introduced
git log --all --grep="DataHub"

# Find PR merge commits
git log --merges --grep="Merge pull request"

# By author
git log --author="contributor" --since="2023-01-01"
```

**PR from Commit:**

```bash
# Get PR number from merge commit
git log --merges --oneline | grep "Merge pull request #"

# Then get PR details
gh pr view <PR_NUMBER>
```

### C. Example: Documenting Architecture Decision

**Goal:** Document when and why Chakra UI was chosen

```bash
# 1. Find relevant PRs
gh pr list --search "Chakra" --state all --limit 20

# 2. Get PR details
gh pr view 123 --json title,body,createdAt,author

# 3. See what changed
gh pr view 123 --json files | jq '.files[] | select(.path | contains("package.json"))'

# 4. Get discussion
gh api repos/hivemq/hivemq-edge/pulls/123/comments

# 5. Link in documentation
echo "**Reference:** PR #123 - Migrate to Chakra UI" >> docs/architecture/OVERVIEW.md
```

### Recommendation

**git access + `gh` CLI is sufficient** for historical PR analysis and documentation.

**For ADR retrospective documentation:**

1. Use `gh pr list --search` to find relevant PRs
2. Use `git log --grep` to find feature introduction
3. Use `gh pr view` to get decision context
4. Link PRs in ADR documents for traceability

---

## 3. Security Scanning for Docs

### Solution: Docs Security Scan Skill

**Created:** `.claude/skills/docs-security-scan/`

**Purpose:** Pre-commit security scanning to prevent accidental secrets in documentation.

### What It Detects

#### Critical (Blocks Commit) ❌

- AWS access keys (AKIA...)
- GitHub personal access tokens (ghp\_...)
- Private keys (-----BEGIN PRIVATE KEY-----)
- SSH keys
- Slack tokens
- OAuth tokens

#### High Risk (Warns) ⚠️

- Generic API keys
- Passwords in configuration
- Bearer tokens (actual, not placeholders)
- JWT tokens (actual)
- Database connection strings with passwords
- Client secrets

#### Safe Patterns (Allowed) ✅

- Placeholders: `YOUR_API_KEY_HERE`, `<your-password>`, `<token>`
- Environment references: `process.env.SECRET`
- Masked values: `password: ***`
- Documentation examples clearly marked

### Usage

**Command Line:**

```bash
# Scan all docs
pnpm docs:security-scan

# Scan only staged files
pnpm docs:security-scan:staged

# Verbose output
pnpm docs:security-scan:verbose
```

**Pre-Commit Hook (Optional):**

```bash
# .husky/pre-commit
#!/bin/sh
if git diff --cached --name-only | grep -q "^docs/"; then
  echo "🔒 Scanning docs for secrets..."
  pnpm docs:security-scan:staged || exit 1
fi
```

**CI Integration (Recommended):**

Add to `.github/workflows/check-frontend.yml`:

```yaml
security_scan_docs:
  name: Docs Security Scan
  runs-on: ubuntu-latest
  steps:
    - name: 👓 Checkout repository
      uses: actions/checkout@v6

    - name: 🔒 Scan docs for secrets
      run: ./tools/scan-docs-security.sh
```

### Configuration

**Location:** `.claude/skills/docs-security-scan/config.yaml`

**Customize:**

- Add new patterns
- Adjust severity levels
- Add allowed exceptions
- Whitelist specific content

### Exit Codes

- `0` - No secrets found, safe to commit ✅
- `1` - Critical secrets found, commit blocked 🚫
- `2` - Warnings found, review recommended ⚠️

### Example Output

```
🔒 Docs Security Scan
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

❌ CRITICAL: AWS Access Key found
   File: docs/technical/CONFIGURATION.md
   Line: 42
   Content: aws_access_key: AKIAIOSFODNN7EXAMPLE...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Results:
  Files scanned: 9
  Critical issues: 1
  Warnings: 0

🚫 COMMIT BLOCKED: Critical secrets detected
```

### Testing

**Verify scanner works:**

```bash
# Create test file with fake secret
echo "password: secret123" > /tmp/test.md

# Scanner should detect it
# (won't detect in /tmp, only scans docs/)

# Test with placeholder (should NOT detect)
echo "password: YOUR_PASSWORD_HERE" > docs/test.md
./tools/scan-docs-security.sh
rm docs/test.md
```

### Files Created

```
.claude/skills/docs-security-scan/
├── SKILL.md           # Comprehensive documentation
├── README.md          # Quick start guide
└── config.yaml        # Pattern configuration

tools/
└── scan-docs-security.sh  # Executable scanner script

package.json           # Added scripts:
                      # - docs:security-scan
                      # - docs:security-scan:staged
                      # - docs:security-scan:verbose
```

---

## Summary

### ✅ MIRO Boards

- **Capability:** Cannot read directly
- **Solution:** Recreate as Mermaid (RULE 9 compliant) + link to MIRO
- **Alternative:** Export PDFs to `docs/assets/miro/`

### ✅ Pull Requests

- **Capability:** Full access via `gh` CLI + git
- **Sufficient for:** Historical analysis, ADR documentation, traceability
- **Commands:** `gh pr list`, `gh pr view`, `git log --grep`

### ✅ Security Scanning

- **Status:** Complete and tested
- **Detects:** AWS keys, tokens, passwords, private keys
- **Usage:** `pnpm docs:security-scan`
- **Integration:** Ready for pre-commit hooks and CI

---

**All three concerns addressed with working solutions!**
