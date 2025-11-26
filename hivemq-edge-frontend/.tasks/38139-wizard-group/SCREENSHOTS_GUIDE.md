# PR Screenshots Guide - Group Wizard

This document explains how to generate the screenshots referenced in PULL_REQUEST.md and BLOG_POST.md.

---

## Screenshot Requirements

The PR and blog post reference 5 key screenshots:

1. **PR-wizard-menu-dropdown.png** - Wizard menu with GROUP option visible
2. **PR-wizard-progress-bar.png** - Progress bar showing Step 1 of 2
3. **PR-ghost-nodes-single.png** - Ghost group with single node selected
4. **PR-ghost-nodes-multiple.png** - Ghost group with 2+ nodes selected (main PR image)
5. **PR-configuration-panel.png** - Configuration drawer with form
6. **PR-wizard-completion.png** - Success state with toast notification

---

## Method 1: Extract from Cypress Video (Recommended)

The E2E tests generate a video that captures all interactions. Extract screenshots at specific timestamps.

### Steps

1. **Run the PR screenshot tests:**

   ```bash
   cd /Users/nicolas/IdeaProjects/hivemq-edge/hivemq-edge-frontend
   pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts"
   ```

2. **Locate the video file:**

   ```
   cypress/videos/wizard-create-group.spec.cy.ts.mp4
   ```

3. **Open in video player** (QuickTime, VLC, or any video editor)

4. **Extract frames at these approximate timestamps:**

   | Screenshot                  | Test Name                                     | Approximate Time | What to Capture                                 |
   | --------------------------- | --------------------------------------------- | ---------------- | ----------------------------------------------- |
   | PR-wizard-menu-dropdown.png | should capture wizard menu and progress bar   | ~0:15            | Dropdown menu open, GROUP option visible        |
   | PR-wizard-progress-bar.png  | should capture wizard menu and progress bar   | ~0:20            | Progress bar at bottom, "Step 1 of 2"           |
   | PR-ghost-nodes-single.png   | should capture ghost nodes and selection mode | ~0:35            | One adapter selected, ghost group visible       |
   | PR-ghost-nodes-multiple.png | should capture ghost nodes and selection mode | ~0:45            | Two adapters selected, ghost group expanded     |
   | PR-configuration-panel.png  | should capture configuration panel            | ~0:55            | Drawer open with form, workspace visible behind |
   | PR-wizard-completion.png    | should capture complete workflow              | ~1:10            | Success toast, real group node visible          |

5. **Save screenshots** to `cypress/screenshots/wizard-create-group.spec.cy.ts/` directory

6. **Verify image quality:**
   - Resolution: 1400x1016 (default Cypress viewport)
   - Format: PNG
   - File size: 50-200KB per screenshot

---

## Method 2: Run Tests with Custom Screenshot Commands

If Cypress screenshots are not being saved by default, add explicit commands to the tests.

### Option A: Modify Existing Tests Temporarily

Edit: `cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts`

Already includes explicit `cy.screenshot()` commands:

```typescript
cy.screenshot('PR-wizard-menu-dropdown', {
  overwrite: true,
  capture: 'viewport',
})
```

The tests should automatically save screenshots to:

```
cypress/screenshots/wizard-create-group.spec.cy.ts/PR-wizard-menu-dropdown.png
```

### Option B: Verify Cypress Configuration

Check `cypress.config.ts`:

```typescript
e2e: {
  video: true,
  screenshotOnRunFailure: false, // Explicit cy.screenshot() commands will still work
  // ...
}
```

Explicit `cy.screenshot()` calls should work regardless of `screenshotOnRunFailure` setting.

---

## Method 3: Manual Screenshots with Cypress GUI

For highest quality control:

1. **Open Cypress in headed mode:**

   ```bash
   npx cypress open
   ```

2. **Select E2E Testing → Chrome browser**

3. **Run the visual regression tests:**

   - Navigate to `wizard-create-group.spec.cy.ts`
   - Expand "Visual Regression & PR Screenshots" suite
   - Run each test individually

4. **Pause at key moments:**

   - Use Chrome DevTools to pause execution at breakpoints
   - Or add `cy.pause()` commands temporarily in the test

5. **Take screenshots using OS tools:**

   - **macOS**: `Cmd + Shift + 4` (select area) or `Cmd + Shift + 3` (full screen)
   - **Windows**: `Win + Shift + S`
   - **Linux**: Use your distribution's screenshot tool

6. **Save with correct filenames** in `screenshots/` directory

---

## Method 4: Percy Visual Testing (If Available)

If Percy integration is set up:

```bash
PERCY_TOKEN=your_token pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts"
```

Percy captures snapshots at these points:

- `cy.percySnapshot('PR: Group Wizard Menu Dropdown')`
- `cy.percySnapshot('PR: Group Wizard Progress Bar')`
- `cy.percySnapshot('PR: Ghost Group with Multiple Nodes')`
- `cy.percySnapshot('PR: Group Configuration Panel')`
- `cy.percySnapshot('PR: Group Wizard Success')`

Download snapshots from Percy dashboard and rename to match PR requirements.

---

## Screenshot Naming Convention

All PR screenshots follow this pattern:

```
PR-{feature-area}-{specific-element}.png
```

Examples:

- `PR-wizard-menu-dropdown.png` - Wizard menu dropdown
- `PR-ghost-nodes-multiple.png` - Ghost group with multiple nodes
- `PR-configuration-panel.png` - Configuration drawer

This distinguishes them from:

- Functional test screenshots (no "PR-" prefix)
- Percy snapshots (different naming)
- Development screenshots (no naming convention)

---

## Troubleshooting

### Screenshots Not Saved

**Problem**: Tests pass but no screenshots in `cypress/screenshots/` directory

**Solutions**:

1. Check Cypress config - ensure screenshots not explicitly disabled
2. Verify write permissions on `cypress/screenshots/` directory
3. Check console output for screenshot save errors
4. Try running with `--headed` mode to see visual confirmation
5. Use Method 1 (video extraction) as fallback

### Video Not Generated

**Problem**: No video file in `cypress/videos/` directory

**Solutions**:

1. Check `cypress.config.ts` - verify `video: true` in e2e section
2. Ensure tests run in `runMode` (not `openMode`)
3. Check disk space - videos can be 10-50MB
4. Verify write permissions on `cypress/videos/` directory

### Poor Image Quality

**Problem**: Screenshots are blurry or low resolution

**Solutions**:

1. Extract from video using high-quality export settings (PNG, not JPEG)
2. Use native Cypress viewport size (1400x1016) - don't resize
3. Capture at 100% browser zoom level
4. Use lossless PNG compression
5. Avoid scaling screenshots after extraction

### Wrong Timing in Video

**Problem**: Can't find the exact moment to capture in video

**Solutions**:

1. Use video player with frame-by-frame controls (VLC, QuickTime)
2. Watch for test titles in video - they appear as console logs
3. Look for distinctive UI changes:
   - Dropdown opening (sudden menu appearance)
   - Progress bar appearing (bottom-center panel)
   - Ghost node appearing (dashed border)
   - Drawer sliding in (right side animation)
   - Success toast (green notification top-right)

---

## Verification Checklist

Before using screenshots in PR:

- [ ] All 6 screenshots captured
- [ ] Resolution is 1400x1016 (Cypress default viewport)
- [ ] File format is PNG (not JPEG)
- [ ] File sizes reasonable (50-200KB each)
- [ ] Screenshots show complete UI (no truncation)
- [ ] Text is readable (not blurry)
- [ ] Filenames match references in PULL_REQUEST.md
- [ ] Screenshots saved to correct directory
- [ ] Image captions updated if viewport size differs

---

## Quick Commands Reference

```bash
# Run all tests including PR screenshot tests
pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts"

# Run only PR screenshot tests
pnpm cypress:run:e2e --spec "cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts" --grep "Visual Regression"

# Open Cypress GUI for manual screenshot capture
npx cypress open

# Check if screenshots were generated
ls -la cypress/screenshots/wizard-create-group.spec.cy.ts/

# Check if video was generated
ls -la cypress/videos/
```

---

**Last Updated:** November 24, 2025  
**Cypress Version:** 13.x  
**Test File:** `cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts`
