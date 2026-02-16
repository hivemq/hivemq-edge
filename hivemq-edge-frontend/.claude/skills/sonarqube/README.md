# SonarQube Analysis Skill

Automatically fetch and analyze SonarQube quality metrics for pull requests.

## Quick Start

```bash
# Analyze current PR (auto-detect)
/sonarqube

# Analyze specific PR
/sonarqube 1386

# Detailed report
/sonarqube --detailed

# Quality gate only
/sonarqube --quality-gate-only
```

## What It Does

1. ✅ Verifies PR exists (Hook 1)
2. ✅ Checks GitHub Actions completed (Hook 2)
3. ✅ Fetches SonarQube metrics (Hook 3)
4. 📊 Generates comprehensive quality report

## Sample Output

```markdown
# SonarQube Analysis: PR #1386

**Quality Gate:** ✅ PASSED

## New Code Metrics

| Metric             | Value | Status |
| ------------------ | ----- | ------ |
| 🐛 Bugs            | 0     | ✅     |
| 🔒 Vulnerabilities | 0     | ✅     |
| 💡 Code Smells     | 2     | ✅     |
| 📊 Coverage        | 85.3% | ✅     |
| 📋 Duplications    | 1.2%  | ✅     |

## Ratings

- **Maintainability:** 🟢 A
- **Reliability:** 🟢 A
- **Security:** 🟢 A

✅ All quality checks passed!
```

## Configuration

Edit `.claude/skills/sonarqube/config.yaml` to customize:

- Thresholds
- Emoji
- Report sections
- GitHub workflow names

## Troubleshooting

### No PR Found

```bash
# Create PR first
gh pr create

# Or specify manually
/sonarqube 1386
```

### GH Action Still Running

```bash
# Wait for completion, or force
/sonarqube --force
```

### SonarQube Data Not Available

```bash
# Re-run GitHub workflow
gh pr checks <pr_number> --rerun

# Wait a few minutes
sleep 120

# Retry
/sonarqube
```

## Integration

Works great with `/pre-review` skill:

```bash
# Local checks first
/pre-review

# Then SonarQube analysis
/sonarqube
```

## Links

- **Skill Documentation:** [SKILL.md](./SKILL.md)
- **Configuration:** [config.yaml](./config.yaml)
- **SonarCloud:** https://sonarcloud.io/project/overview?id=hivemq_hivemq-edge
