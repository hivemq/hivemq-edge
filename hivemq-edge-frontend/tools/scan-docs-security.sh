#!/usr/bin/env bash
set -eo pipefail

# Docs Security Scanner
# Scans documentation for accidentally committed secrets and credentials

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
CRITICAL_COUNT=0
HIGH_COUNT=0
FILES_SCANNED=0

# Flags
VERBOSE=0
STAGED_ONLY=0

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --verbose|-v)
      VERBOSE=1
      shift
      ;;
    --staged)
      STAGED_ONLY=1
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [--verbose] [--staged]"
      exit 1
      ;;
  esac
done

echo -e "${BLUE}🔒 Docs Security Scan${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Determine which files to scan
if [ "$STAGED_ONLY" -eq 1 ]; then
  echo "Scanning staged docs files..."
  FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E '^docs/.*\.md$|^README\.md$|^CLAUDE\.md$' || true)
else
  echo "Scanning all docs files..."
  FILES=$(find "$PROJECT_ROOT/docs" -type f -name "*.md" 2>/dev/null || true)
  if [ -f "$PROJECT_ROOT/README.md" ]; then
    FILES="$FILES $PROJECT_ROOT/README.md"
  fi
  if [ -f "$PROJECT_ROOT/CLAUDE.md" ]; then
    FILES="$FILES $PROJECT_ROOT/CLAUDE.md"
  fi
fi

if [ -z "$FILES" ]; then
  echo -e "${GREEN}✓ No documentation files to scan${NC}"
  exit 0
fi

# Function to check if line contains safe placeholder
is_safe_placeholder() {
  local line="$1"

  # List of safe placeholder patterns
  local safe_patterns=(
    "YOUR_API_KEY"
    "<your-password>"
    "<password>"
    "<token>"
    "Bearer <token>"
    "process\.env\."
    "\*\*\*"
    "example-"
    "test-"
    "demo-"
    "sample-"
  )

  for pattern in "${safe_patterns[@]}"; do
    if echo "$line" | grep -qE "$pattern"; then
      return 0
    fi
  done

  return 1
}

# Function to scan for a pattern
scan_pattern() {
  local file="$1"
  local pattern_name="$2"
  local pattern="$3"
  local severity="$4"

  local matches
  matches=$(grep -nE "$pattern" "$file" 2>/dev/null || true)

  if [ -n "$matches" ]; then
    while IFS= read -r match; do
      local line_num line_content
      line_num=$(echo "$match" | cut -d: -f1)
      line_content=$(echo "$match" | cut -d: -f2-)

      # Skip if safe placeholder
      if is_safe_placeholder "$line_content"; then
        continue
      fi

      # Report finding
      if [ "$severity" = "critical" ]; then
        echo -e "${RED}❌ CRITICAL: $pattern_name${NC}"
        CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
      else
        echo -e "${YELLOW}⚠️  WARNING: $pattern_name${NC}"
        HIGH_COUNT=$((HIGH_COUNT + 1))
      fi

      echo "   File: $file"
      echo "   Line: $line_num"
      # Truncate line content to 80 chars
      local content_preview
      content_preview=$(echo "$line_content" | head -c 80)
      echo "   Content: $content_preview..."
      echo ""
    done <<< "$matches"
  fi
}

# Scan each file
for file in $FILES; do
  [ ! -f "$file" ] && continue

  FILES_SCANNED=$((FILES_SCANNED + 1))

  if [ "$VERBOSE" -eq 1 ]; then
    echo -e "${BLUE}Scanning: $file${NC}"
  fi

  # Critical patterns
  scan_pattern "$file" "AWS Access Key" "AKIA[0-9A-Z]{16}" "critical"
  scan_pattern "$file" "GitHub Personal Token" "ghp_[a-zA-Z0-9]{36}" "critical"
  scan_pattern "$file" "GitHub OAuth Token" "gho_[a-zA-Z0-9]{36}" "critical"
  scan_pattern "$file" "Private Key" "-----BEGIN.*PRIVATE KEY-----" "critical"
  scan_pattern "$file" "SSH Private Key" "ssh-rsa AAAA[a-zA-Z0-9+/]{100,}" "critical"
  scan_pattern "$file" "Slack Token" "xox[baprs]-[0-9]+-[0-9]+-[a-zA-Z0-9]+" "critical"

  # High-risk patterns
  scan_pattern "$file" "Generic API Key" "api[_-]?key[_-]?[=:][[:space:]]*['\"]?[a-zA-Z0-9]{32,}" "high"
  scan_pattern "$file" "Password" "password[_-]?[=:][[:space:]]*['\"]?[^[:space:]'\"]{8,}" "high"
  scan_pattern "$file" "Bearer Token" "Bearer [a-zA-Z0-9\-_.]{20,}" "high"
  scan_pattern "$file" "JWT Token" "eyJ[a-zA-Z0-9_-]+\.eyJ[a-zA-Z0-9_-]+" "high"
  scan_pattern "$file" "Database URL with Password" "://[^:]+:[^@]+@" "high"
  scan_pattern "$file" "Client Secret" "client[_-]?secret[_-]?[=:][[:space:]]*['\"]?[a-zA-Z0-9]{20,}" "high"
done

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Results:"
echo "  Files scanned: $FILES_SCANNED"
echo "  Critical issues: $CRITICAL_COUNT"
echo "  Warnings: $HIGH_COUNT"
echo ""

# Exit based on findings
if [ "$CRITICAL_COUNT" -gt 0 ]; then
  echo -e "${RED}🚫 COMMIT BLOCKED: Critical secrets detected${NC}"
  echo "Remove all secrets before committing."
  exit 1
elif [ "$HIGH_COUNT" -gt 0 ]; then
  echo -e "${YELLOW}⚠️  WARNINGS: Review flagged content${NC}"
  echo "Verify all flagged items are safe before committing."
  exit 2
else
  echo -e "${GREEN}✓ No secrets detected${NC}"
  exit 0
fi
