#!/usr/bin/env bash

# Screenshot Cleanup Script
# Removes CI failure artifacts from cypress/screenshots/
#
# Usage:
#   ./tools/cleanup-screenshot-artifacts.sh           # Preview files to delete
#   ./tools/cleanup-screenshot-artifacts.sh --delete  # Actually delete files

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SCREENSHOTS_DIR="$PROJECT_ROOT/cypress/screenshots"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "Screenshot Cleanup Tool"
echo "======================="
echo ""

# Check if screenshots directory exists
if [ ! -d "$SCREENSHOTS_DIR" ]; then
  echo -e "${YELLOW}No screenshots directory found at: $SCREENSHOTS_DIR${NC}"
  exit 0
fi

# Find failed test screenshots
FAILED_SCREENSHOTS=$(find "$SCREENSHOTS_DIR" -type f -name "*failed*.png" 2>/dev/null || true)
ATTEMPT_SCREENSHOTS=$(find "$SCREENSHOTS_DIR" -type f -name "*attempt*.png" 2>/dev/null || true)

# Combine and count
TOTAL_ARTIFACTS=$(echo "$FAILED_SCREENSHOTS $ATTEMPT_SCREENSHOTS" | tr ' ' '\n' | grep -v '^$' | sort -u)
COUNT=$(echo "$TOTAL_ARTIFACTS" | grep -v '^$' | wc -l | tr -d ' ')

if [ "$COUNT" -eq 0 ]; then
  echo -e "${GREEN}✓ No CI failure artifacts found${NC}"
  echo ""
  echo "Screenshot directory is clean!"
  exit 0
fi

echo -e "${YELLOW}Found $COUNT CI failure artifacts:${NC}"
echo ""
echo "$TOTAL_ARTIFACTS" | while IFS= read -r file; do
  [ -z "$file" ] && continue
  rel_path="${file#$PROJECT_ROOT/}"
  echo "  - $rel_path"
done
echo ""

# Check for --delete flag
if [ "$1" = "--delete" ]; then
  echo -e "${RED}Deleting $COUNT files...${NC}"
  echo ""

  deleted=0
  echo "$TOTAL_ARTIFACTS" | while IFS= read -r file; do
    [ -z "$file" ] && continue
    if [ -f "$file" ]; then
      rm "$file"
      rel_path="${file#$PROJECT_ROOT/}"
      echo "  ✓ Deleted: $rel_path"
      deleted=$((deleted + 1))
    fi
  done

  echo ""
  echo -e "${GREEN}✓ Cleanup complete!${NC}"
  echo "Deleted $COUNT CI failure artifacts"

  # Clean up empty directories
  find "$SCREENSHOTS_DIR" -type d -empty -delete 2>/dev/null || true

else
  echo -e "${YELLOW}Preview mode - no files deleted${NC}"
  echo ""
  echo "To delete these files, run:"
  echo "  ./tools/cleanup-screenshot-artifacts.sh --delete"
fi

echo ""
