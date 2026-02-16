# Docs Security Scan Skill

**Purpose:** Pre-commit security scanning for documentation to prevent accidental secrets/credentials commits.

## Quick Start

```bash
# Scan all docs
./tools/scan-docs-security.sh

# Scan with verbose output
./tools/scan-docs-security.sh --verbose

# Scan only staged files (for pre-commit)
./tools/scan-docs-security.sh --staged
```

## What It Detects

### Critical (Blocks Commit)
- AWS access keys (AKIA...)
- GitHub personal access tokens (ghp_...)
- Private keys (-----BEGIN PRIVATE KEY-----)
- SSH keys
- Slack tokens

### High Risk (Warns)
- Generic API keys
- Passwords in config
- Bearer tokens
- JWT tokens
- Database connection strings with passwords
- Client secrets

### Medium Risk (Info)
- Private IP addresses
- Internal hostnames
- Company email addresses

## Setup

### Add to package.json

```json
{
  "scripts": {
    "docs:security-scan": "./tools/scan-docs-security.sh",
    "docs:security-scan:staged": "./tools/scan-docs-security.sh --staged"
  }
}
```

### Pre-Commit Hook (Optional)

Install husky:
```bash
pnpm add -D husky
npx husky install
```

Create `.husky/pre-commit`:
```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

# Check if any files in docs/ are staged
if git diff --cached --name-only | grep -q "^docs/"; then
  echo "🔒 Scanning docs for secrets..."
  ./tools/scan-docs-security.sh --staged || exit 1
fi
```

### CI Integration

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

## Configuration

Edit `.claude/skills/docs-security-scan/config.yaml` to:
- Add new patterns
- Adjust severity levels
- Add allowed exceptions
- Whitelist specific lines

## False Positives

If the scanner flags safe content:

1. **Use clear placeholders:**
   ```markdown
   ✅ api_key: YOUR_API_KEY_HERE
   ✅ password: <your-password>
   ✅ Bearer <token>
   ```

2. **Add to allowed exceptions** in `config.yaml`:
   ```yaml
   allowed_exceptions:
     - "your-safe-pattern"
   ```

3. **Use code blocks for examples:**
   ````markdown
   ```bash
   export API_KEY="example-key"  # This won't be scanned in code blocks
   ```
   ````

## Testing the Scanner

Create a test file:
```bash
echo "password: secret123" > test-secret.md
./tools/scan-docs-security.sh
# Should detect and block
rm test-secret.md
```

## Exit Codes

- `0` - No secrets found ✅
- `1` - Critical secrets found 🚫
- `2` - Warnings found ⚠️

## Maintenance

Review quarterly:
- Update patterns for new threats
- Check false positive rate
- Review allowed exceptions
- Update documentation

---

**See:** [SKILL.md](./SKILL.md) for comprehensive documentation
