#!/bin/bash

# Copy Combiner Screenshots to Documentation Assets
# This script copies all combiner-*.png screenshots from cypress/screenshots
# to docs/assets/screenshots/combiner/ for use in documentation

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "📸 Copying Combiner screenshots to documentation assets..."
echo ""

# Create destination directory
mkdir -p docs/assets/screenshots/combiner

# Count screenshots before copy
SCREENSHOT_COUNT=$(find cypress/screenshots -name "combiner-*.png" 2>/dev/null | wc -l | tr -d ' ')

if [ "$SCREENSHOT_COUNT" -eq 0 ]; then
  echo "${YELLOW}⚠️  No combiner screenshots found in cypress/screenshots/${NC}"
  echo ""
  echo "Run tests to generate screenshots:"
  echo "  pnpm cypress:run:component --spec \"src/modules/Mappings/combiner/*.spec.cy.tsx\""
  echo "  pnpm cypress:run:e2e --spec \"cypress/e2e/mappings/combiner-documentation-screenshots.spec.cy.ts\""
  exit 1
fi

echo "Found ${SCREENSHOT_COUNT} combiner screenshots"
echo ""

# Copy all combiner screenshots
find cypress/screenshots -name "combiner-*.png" -exec cp -v {} docs/assets/screenshots/combiner/ \;

echo ""
echo "${GREEN}✅ Copied ${SCREENSHOT_COUNT} screenshots to docs/assets/screenshots/combiner/${NC}"
echo ""
echo "📁 Files in destination:"
ls -1 docs/assets/screenshots/combiner/ | grep "combiner-" || echo "  (none)"
echo ""
echo "Next steps:"
echo "  1. Verify screenshots in docs/assets/screenshots/combiner/"
echo "  2. Check image paths in docs/walkthroughs/RJSF_COMBINER.md"
echo "  3. Update screenshot placeholders if needed"
