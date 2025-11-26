# E2E vs Component Tests: Why Coverage Was Broken

## The Key Difference

### 🔴 E2E Tests (`component: false`)

**Uses production build served via `pnpm preview`**

```yaml
strategy:
  matrix:
    cypress: [
        { component: false, spec: './cypress/e2e/**/*', target: 'E2E' }, # ← E2E
        { component: true, spec: './src/components/**/*', target: 'Components' },
        # ... more component tests
      ]
steps:
  - name: Download artifact # ← Downloads pre-built production bundle
    uses: actions/download-artifact@...
    with:
      name: application
      path: ./hivemq-edge-frontend/dist

  - name: 🧪 Run Cypress suite
    uses: cypress-io/github-action@v6
    with:
      component: ${{ matrix.cypress.component }} # ← false for E2E
      spec: ${{ matrix.cypress.spec }}
      start: pnpm preview --port 3000 # ← Serves the ./dist folder (production build)
```

**What happens:**

1. ✅ `build_production` job creates production bundle → stored in `./dist`
2. ✅ `cypress_matrix` downloads the `./dist` artifact
3. ✅ `pnpm preview --port 3000` serves the **pre-built files from `./dist`**
4. ❌ **NO dev server, NO live compilation, NO Istanbul instrumentation**
5. ❌ Tests run against static production files that weren't instrumented

---

### 🟢 Component Tests (`component: true`)

**Uses Vite dev server with live compilation**

```yaml
strategy:
  matrix:
    cypress: [
        { component: false, spec: './cypress/e2e/**/*', target: 'E2E' },
        { component: true, spec: './src/components/**/*', target: 'Components' }, # ← Component
        { component: true, spec: './src/extensions/**/*', target: 'Extensions' }, # ← Component
        # ... more component tests
      ]
steps:
  - name: Download artifact # ← Still downloads but doesn't use it!
    uses: actions/download-artifact@...
    with:
      name: application
      path: ./hivemq-edge-frontend/dist

  - name: 🧪 Run Cypress suite
    uses: cypress-io/github-action@v6
    with:
      component: ${{ matrix.cypress.component }} # ← true for components
      spec: ${{ matrix.cypress.spec }}
      start: pnpm preview --port 3000 # ← Ignored by component tests!
```

**What happens:**

1. ✅ Cypress sees `component: true`
2. ✅ Reads `cypress.config.ts` → finds `devServer: { bundler: 'vite' }`
3. ✅ **Starts Vite dev server internally** (ignores `pnpm preview`)
4. ✅ Vite dev server compiles components on-the-fly
5. ✅ `vite-plugin-istanbul` instruments code during compilation
6. ✅ Coverage data is collected successfully

---

## Why Both Download the Artifact

Look at the workflow - **ALL matrix jobs download the artifact**, but:

- **E2E tests**: Actually USE it (serve via `pnpm preview`)
- **Component tests**: IGNORE it (use Vite dev server instead)

The `start: pnpm preview --port 3000` command is **only used by E2E tests**. Component tests ignore it because they have their own dev server configured in `cypress.config.ts`:

```typescript
component: {
  devServer: {
    framework: 'react',
    bundler: 'vite',  // ← This starts Vite's dev server
  },
}
```

---

## The Coverage Problem

### Before the Fix:

```
E2E Tests Flow:
1. Build production bundle (NO instrumentation)
2. Serve static files from ./dist
3. Run tests → NO coverage data collected
4. Error: "file out.json has no coverage information"

Component Tests Flow:
1. Start Vite dev server
2. Compile components with Istanbul plugin
3. Run tests → Coverage data collected ✅
```

### After the Fix:

```
E2E Tests Flow:
1. Build production bundle WITH instrumentation (VITE_COVERAGE=true)
2. Serve instrumented files from ./dist
3. Run tests → Coverage data collected ✅

Component Tests Flow:
(No change - still works as before)
```

---

## Visual Summary

```
┌─────────────────────────────────────────────────────────────┐
│                    cypress_matrix Job                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Matrix Strategy:                                            │
│  ┌────────────────┬────────────────────────────────────┐   │
│  │ component:     │ What Actually Runs                  │   │
│  ├────────────────┼────────────────────────────────────┤   │
│  │ false (E2E)    │ pnpm preview → serves ./dist        │   │
│  │                │ (production build)                  │   │
│  ├────────────────┼────────────────────────────────────┤   │
│  │ true (Comp)    │ Vite dev server → compiles on-fly   │   │
│  │                │ (ignores pnpm preview)              │   │
│  └────────────────┴────────────────────────────────────┘   │
│                                                              │
│  Both download ./dist artifact, but only E2E uses it!       │
└─────────────────────────────────────────────────────────────┘
```

---

## How to Verify

Run locally to see the difference:

```bash
# E2E tests (uses production build from ./dist)
pnpm build
pnpm preview --port 3000 &
npx cypress run --e2e

# Component tests (uses Vite dev server)
npx cypress run --component
# Note: No need to build or preview - Cypress starts Vite internally
```

In the component tests, even though the workflow says `start: pnpm preview`, Cypress **ignores that** because you have `devServer` configured in `cypress.config.ts`.
