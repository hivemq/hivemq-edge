# Docs Security Scan Skill

## Purpose

Scans `./docs/` directory for accidentally committed secrets, credentials, and confidential information before commits.

## When to Use

**Automatically:**

- Pre-commit hook (if configured)
- CI pipeline (recommended)

**Manually:**

- Before committing documentation changes
- After updating any document with URLs, tokens, or configuration
- During documentation review

## What It Scans

### High-Risk Patterns (Block Commit)

**API Keys & Tokens:**

- AWS keys: `AKIA[0-9A-Z]{16}`
- GitHub tokens: `ghp_[a-zA-Z0-9]{36}`
- Generic API keys: `api[_-]?key[_-]?[=:]\s*['\"]?[a-zA-Z0-9]{20,}`
- Bearer tokens: `Bearer\s+[a-zA-Z0-9\-_]+`
- JWT tokens: `eyJ[a-zA-Z0-9_-]+\.eyJ[a-zA-Z0-9_-]+`

**Passwords:**

- Password fields: `password[_-]?[=:]\s*['\"]?[^\s'\"]+`
- Database URLs with passwords: `://[^:]+:[^@]+@`

**Private Keys:**

- Private key headers: `-----BEGIN.*PRIVATE KEY-----`
- SSH keys: `ssh-rsa\s+AAAA[a-zA-Z0-9+/]+`

**Secrets & Credentials:**

- Secret values: `secret[_-]?[=:]\s*['\"]?[a-zA-Z0-9]{20,}`
- Client secrets: `client[_-]?secret`
- Auth tokens: `auth[_-]?token`

**Internal URLs:**

- Private IP addresses: `(10\.|172\.(1[6-9]|2[0-9]|3[01])\.|192\.168\.)`
- Internal hostnames: `\.internal\.|\.local\.|\.corp\.`

**Email Addresses (Organizational):**

- Company emails: `[a-zA-Z0-9._%+-]+@hivemq\.com`
- Specific patterns that might expose internal structure

### Medium-Risk Patterns (Warn)

**Configuration:**

- Hardcoded ports with credentials
- Database connection strings
- SMTP credentials
- S3 bucket names with sensitive context

**Personal Information:**

- Phone numbers
- Physical addresses (if not public office)

## Allowed Patterns

**Safe to commit:**

- Public documentation URLs
- GitHub repository URLs (public repos)
- Published blog posts
- Public email addresses (support@, info@, etc.)
- Example/placeholder values clearly marked as such
- Documentation about security (not actual secrets)

**Example Safe Patterns:**

```markdown
✅ `api_key: "YOUR_API_KEY_HERE"` # Placeholder
✅ `password: "<your-password>"` # Placeholder
✅ `https://github.com/hivemq/...` # Public repo
✅ `Bearer <token>` # Documentation template
✅ `secret: process.env.SECRET` # Code reference
```

## Usage

### Command Line

```bash
# Scan all docs
pnpm run docs:security-scan

# Scan specific file
pnpm run docs:security-scan docs/technical/TECHNICAL_STACK.md

# Scan with verbose output
pnpm run docs:security-scan --verbose
```

### Pre-Commit Hook

Add to `.husky/pre-commit`:

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

# Check if any files in docs/ are staged
if git diff --cached --name-only | grep -q "^docs/"; then
  echo "🔒 Scanning docs for secrets..."
  pnpm run docs:security-scan --staged || exit 1
fi
```

### CI Pipeline

Add to `.github/workflows/check-frontend.yml`:

```yaml
security_scan_docs:
  name: Docs Security Scan
  runs-on: ubuntu-latest
  steps:
    - name: 👓 Checkout repository
      uses: actions/checkout@v6

    - name: 🔒 Scan docs for secrets
      run: |
        # Run security scan on docs directory
        ./tools/scan-docs-security.sh
```

## Configuration

**File:** `.claude/skills/docs-security-scan/config.yaml`

```yaml
scan_paths:
  - ./docs/**/*.md
  - ./docs/**/*.mdx
  - ./*.md  # Root-level docs like README.md

exclude_patterns:
  - "**/node_modules/**"
  - "**/.git/**"
  - "**/dist/**"
  - "**/.tasks/**"  # Task docs may have temporary test data

high_risk_patterns:
  - name: "AWS Access Key"
    pattern: "AKIA[0-9A-Z]{16}"
    severity: "critical"

  - name: "GitHub Token"
    pattern: "ghp_[a-zA-Z0-9]{36}"
    severity: "critical"

  - name: "Private Key"
    pattern: "-----BEGIN.*PRIVATE KEY-----"
    severity: "critical"

  - name: "Generic API Key"
    pattern: "api[_-]?key[_-]?[=:]\s*['\"]?[a-zA-Z0-9]{20,}"
    severity: "high"

  - name: "Password"
    pattern: "password[_-]?[=:]\s*['\"]?[^\s'\"]{8,}"
    severity: "high"

  - name: "Bearer Token"
    pattern: "Bearer\s+[a-zA-Z0-9\\-_]{20,}"
    severity: "high"

  - name: "JWT Token"
    pattern: "eyJ[a-zA-Z0-9_-]+\\.eyJ[a-zA-Z0-9_-]+"
    severity: "high"

  - name: "Database URL with Password"
    pattern: "://[^:]+:[^@]+@"
    severity: "high"

  - name: "Private IP"
    pattern: "(10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.)"
    severity: "medium"

allowed_exceptions:
  - "YOUR_API_KEY_HERE"
  - "<your-password>"
  - "<token>"
  - "process.env."
  - "Bearer <token>"
  - "api_key: ***"
```

## Exit Codes

- `0` - No secrets found, safe to commit
- `1` - High-risk secrets found, commit blocked
- `2` - Medium-risk patterns found, review recommended
- `3` - Scan failed (file not found, permission denied, etc.)

## Output Format

```
🔒 Docs Security Scan
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Scanning: ./docs/**/*.md

❌ CRITICAL: AWS Access Key found
   File: docs/technical/CONFIGURATION.md
   Line: 42
   Pattern: AKIA1234567890ABCDEF

⚠️  WARNING: Private IP address found
   File: docs/architecture/DEPLOYMENT.md
   Line: 128
   Pattern: 192.168.1.100
   Suggestion: Use public IP or "internal-server.example.com"

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Results:
  Files scanned: 15
  Secrets found: 1 critical, 1 warning

🚫 COMMIT BLOCKED: Critical secrets detected
```

## False Positives

**If scan flags safe content:**

1. **Check if it's actually safe:**

   - Is it a placeholder? Mark clearly: `<YOUR_KEY>`
   - Is it example code? Add comment: `# Example only`
   - Is it documentation about secrets? Use code blocks

2. **Add to allowed exceptions:**

   - Edit `config.yaml`
   - Add exact string to `allowed_exceptions`
   - Document why it's safe

3. **Use generic patterns:**
   ```markdown
   ✅ Instead of: `token: abc123xyz`
   ✅ Use: `token: <your-token-here>`
   ```

## Maintenance

**Review quarterly:**

- Add new secret patterns as threats emerge
- Update allowed exceptions
- Check false positive rate
- Review scanning performance

**Add patterns when:**

- New service APIs are integrated
- New authentication methods used
- Security audit identifies gaps

## Related

- **RULE 9:** All diagrams must use Mermaid (includes exported images)
- **Pre-commit hooks:** `.husky/pre-commit`
- **CI Security:** `.github/workflows/check-frontend.yml`
