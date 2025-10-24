# PR Screenshots Generation Guide

This document explains how to generate screenshots for the Pull Request documentation.

---

## Overview

The PR template requires BEFORE and AFTER screenshots to demonstrate the UX improvements. Since Cypress only captures screenshots automatically on test failures, we need to use alternative methods.

---

## Method 1: Extract from Cypress Video (Recommended)

The E2E test generates a video file that captures all test interactions.

**Video Location:**
```
cypress/videos/duplicate-combiner.spec.cy.ts.mp4
```

**How to Extract Screenshots:**

1. **Open the video** in any video player or editing tool
2. **Navigate to these timestamps** (approximate times from test execution):
   - **Empty Modal State**: ~2:30 - Shows modal with no mappings
   - **Modal with Mappings**: ~25:00 - Shows modal with populated mappings
   - **Canvas Animation**: ~15:00 - Shows fitView animation to existing combiner
3. **Take snapshots** at these points and save as PNG files
4. **Recommended filenames:**
   - `after-modal-empty-state.png`
   - `after-modal-with-mappings.png`
   - `after-canvas-animation.png`

---

## Method 2: Run Tests with Cypress GUI and Screenshot Manually

**Steps:**

1. **Start Cypress in headed mode:**
   ```bash
   npx cypress open
   ```

2. **Select E2E Testing** → **Choose Chrome browser**

3. **Run the test:**
   ```
   cypress/e2e/workspace/duplicate-combiner.spec.cy.ts
   ```

4. **Tests with visual snapshots:**
   - "Accessibility → should be accessible" - Captures empty modal state
   - "Accessibility → should be accessible with mappings" - Captures modal with mappings

5. **Use your OS screenshot tool** when the modal appears:
   - macOS: `Cmd + Shift + 4` (area selection) or `Cmd + Shift + 3` (full screen)
   - Windows: `Win + Shift + S`
   - Linux: Use your distribution's screenshot tool

6. **Save screenshots** with descriptive names in the task directory

---

## Method 3: Modify Test to Force Screenshot Capture

Add `cy.screenshot()` commands to the E2E test temporarily:

**Edit:** `cypress/e2e/workspace/duplicate-combiner.spec.cy.ts`

```typescript
// In "should be accessible" test, after modal appears:
workspacePage.duplicateCombinerModal.modal.should('be.visible')
cy.screenshot('duplicate-modal-empty-state', { 
  capture: 'viewport',
  overwrite: true 
})

// In "should be accessible with mappings" test:
workspacePage.duplicateCombinerModal.modal.should('be.visible')
cy.screenshot('duplicate-modal-with-mappings', { 
  capture: 'viewport',
  overwrite: true 
})
```

**Run the test:**
```bash
npx cypress run --spec "cypress/e2e/workspace/duplicate-combiner.spec.cy.ts"
```

**Screenshots will be saved to:**
```
cypress/screenshots/workspace/duplicate-combiner.spec.cy.ts/
```

**Remember to remove the screenshot commands** after capturing images.

---

## Method 4: Use Percy Visual Regression Snapshots

If Percy is already integrated and running:

1. **Percy snapshots are taken** during the accessibility tests
2. **View snapshots** at Percy dashboard: https://percy.io
3. **Download snapshots** from Percy's web interface
4. **Note:** This requires Percy to be configured and running in CI/CD

---

## BEFORE Screenshots

For the "BEFORE" section, you need screenshots of the **old toast notification** behavior:

### Option A: From Git History

If the old code is still in git history:

1. **Checkout the commit before this PR:**
   ```bash
   git stash  # Save current changes
   git checkout <commit-before-changes>
   ```

2. **Run the application:**
   ```bash
   pnpm dev
   ```

3. **Manually trigger duplicate combiner detection:**
   - Create a combiner with specific sources
   - Select the same sources again
   - Click "Combine" button
   - Screenshot the toast notification

4. **Return to current branch:**
   ```bash
   git checkout <your-branch>
   git stash pop  # Restore changes
   ```

### Option B: Mock the Old Behavior

Create a temporary test that simulates the old toast behavior:

```typescript
it('old behavior - toast notification', () => {
  // Setup
  workspacePage.canvas.should('be.visible')
  
  // Show a mock toast (similar to old behavior)
  cy.window().then((win) => {
    win.eval(`
      // Simulate old toast
      const toast = document.createElement('div')
      toast.innerText = 'A combiner with the same sources already exists'
      toast.style.cssText = 'position:fixed; top:20px; right:20px; background:#4299e1; color:white; padding:16px; border-radius:8px;'
      document.body.appendChild(toast)
    `)
  })
  
  cy.wait(1000)
  cy.screenshot('before-toast-notification')
})
```

---

## Recommended Screenshot Sizes

- **Full viewport**: 1280x800 or 1920x1080
- **Focused on modal**: Crop to show modal + surrounding canvas context
- **Format**: PNG with transparency if possible
- **Compression**: Optimize for web (50-200 KB per image)

---

## Final Checklist

Before updating the PR template:

- [ ] Captured modal empty state screenshot
- [ ] Captured modal with mappings screenshot  
- [ ] Captured canvas animation frame (optional)
- [ ] Captured "before" toast notification screenshot
- [ ] Optimized file sizes (<200 KB each)
- [ ] Saved screenshots to task directory: `.tasks/33168-duplicate-combiner/screenshots/`
- [ ] Updated PULL_REQUEST_TEMPLATE.md with actual filenames
- [ ] Verified screenshot image links work in markdown preview

---

## Screenshot Storage

**Recommended structure:**
```
.tasks/33168-duplicate-combiner/
├── PULL_REQUEST_TEMPLATE.md
├── PR_SCREENSHOTS_GUIDE.md (this file)
└── screenshots/
    ├── before-toast-notification.png
    ├── after-modal-empty-state.png
    ├── after-modal-with-mappings.png
    └── after-canvas-animation.png (optional)
```

**Update PR template image paths:**
```markdown
![Before - Toast Notification](./screenshots/before-toast-notification.png)
![After - Modal Empty State](./screenshots/after-modal-empty-state.png)
![After - Modal with Mappings](./screenshots/after-modal-with-mappings.png)
```

---

## Notes

- Screenshots are not automatically committed to git
- You may want to add them to `.gitignore` or commit them based on team policy
- Percy snapshots provide permanent visual regression history
- Video files are typically too large for git repositories
- Consider using GitHub's drag-and-drop image upload when creating the actual PR

