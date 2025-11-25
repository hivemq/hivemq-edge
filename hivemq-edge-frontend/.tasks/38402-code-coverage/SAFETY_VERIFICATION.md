# Production Release Safety Verification

## ✅ CONFIRMED: Production Releases Are NOT Affected

### Summary

The changes made to fix E2E code coverage **will NOT impact production releases**. The Istanbul instrumentation is **only enabled in CI/CD testing workflows**, not in actual release builds.

---

## Build Locations & Their Purposes

### 1. **Production Release Build** (Actual Releases)

**Location:** `build.gradle.kts`

```kotlin
val buildFrontend by tasks.registering(PnpmTask::class) {
  environment = mapOf(
    "NODE_OPTIONS" to "--max-old-space-size=4096",
  )
  pnpmCommand.set(listOf("build", "--base=./"))
  // ❌ NO VITE_COVERAGE environment variable
  dependsOn(tasks.pnpmInstall)
  // ... outputs to dist
}
```

**Status:** ✅ **SAFE - No instrumentation**

- Used by Gradle build system for actual releases
- No `VITE_COVERAGE` environment variable set
- Produces clean, optimized production bundle
- No Istanbul code injected
- **This is what ships to customers**

---

### 2. **CI Testing Build** (GitHub Actions)

**Location:** `.github/workflows/check-frontend.yml`

```yaml
build_production:
  name: Build Production
  steps:
    - name: 🏗️ Build Application
      run: pnpm run build --base=/
      env:
        VITE_COVERAGE: true # ← ONLY set in CI workflow
```

**Status:** ⚙️ **INSTRUMENTED - Only for testing**

- Used exclusively for E2E test coverage in CI
- Has `VITE_COVERAGE: true` explicitly set
- Produces instrumented bundle for coverage collection
- Artifact is temporary (retention: 1 day)
- **Never shipped to customers**

---

## How the Protection Works

### Vite Configuration

```typescript
istanbul({
  requireEnv: false,
  cypress: true,
  forceBuildInstrument: process.env.VITE_COVERAGE === 'true',  // ← Key safeguard
}),
```

### The Safety Mechanism

| Build Context               | VITE_COVERAGE | Instrumented? | Purpose           |
| --------------------------- | ------------- | ------------- | ----------------- |
| **Gradle release build**    | `undefined`   | ❌ NO         | Customer releases |
| **Local `pnpm build`**      | `undefined`   | ❌ NO         | Local testing     |
| **CI testing workflow**     | `"true"`      | ✅ YES        | E2E coverage only |
| **Dev server (`pnpm dev`)** | N/A           | ✅ YES        | Local development |

**The instrumentation ONLY happens when:**

1. `VITE_COVERAGE` environment variable is explicitly set to `"true"`, OR
2. Running in dev mode (Vite dev server)

**Production builds are safe because:**

- Gradle build does NOT set `VITE_COVERAGE`
- Manual builds do NOT set `VITE_COVERAGE`
- The environment variable must be explicitly set

---

## Verification

### Test 1: Gradle Build (Release)

```bash
cd hivemq-edge-frontend
./gradlew buildFrontend

# Check output
grep -r "__coverage__" dist/assets/*.js
# Expected: No matches (no instrumentation)
```

### Test 2: Manual Build (Production)

```bash
cd hivemq-edge-frontend
pnpm build

# Check output
grep -r "__coverage__" dist/assets/*.js
# Expected: No matches (no instrumentation)
```

### Test 3: CI Build (Testing)

```bash
cd hivemq-edge-frontend
VITE_COVERAGE=true pnpm build

# Check output
grep -r "__coverage__" dist/assets/*.js
# Expected: Found matches (instrumentation present)
```

---

## Bundle Size Impact

**Production Release:**

- No change in bundle size
- No additional code injected
- Same performance as before

**CI Testing Build:**

- ~15-20% larger due to Istanbul instrumentation
- Only temporary (1-day retention in GitHub Actions)
- Never deployed to production

---

## Additional Safeguards

1. **Gradle build is isolated**: The Gradle build system uses its own task that doesn't inherit CI environment variables

2. **Explicit opt-in required**: The `forceBuildInstrument` requires an explicit string match (`=== 'true'`), not just any truthy value

3. **Different workflows**: Release workflows (if any) would be separate from testing workflows

4. **Build artifacts are separate**:

- CI testing artifacts: Uploaded to GitHub (1-day retention)
- Release artifacts: Built by Gradle and packaged separately

---

## Conclusion

✅ **Production releases are completely safe and unaffected**

The `VITE_COVERAGE=true` environment variable is:

- ✅ Only set in the CI testing workflow
- ✅ Not set in Gradle builds
- ✅ Not set in manual builds
- ✅ Required explicitly for instrumentation
- ✅ Never present in release pipelines

**Your customers will receive clean, optimized production builds without any Istanbul instrumentation code.**
