# EDG-73: Codemod Handover

The codemod has been developed and tested but the migration was halted because epic branches are in flight that will introduce new `cy.intercept()` calls. Apply this once all open branches have been merged into master.

---

## How to run

```bash
# 1. Preview what would change (no writes)
node --experimental-strip-types tools/migrateCyIntercept.ts --dry-run

# 2. Apply
node --experimental-strip-types tools/migrateCyIntercept.ts

# 3. Type-check the result
pnpm build:tsc
```

The dry-run summary tells you:

- **Files to be modified** — auto-fixable
- **Disambiguated to GET** — URL-only intercepts that matched multiple methods; GET was preferred (correct for ~100% of load/render test scenarios)
- **Violations skipped** — spy-only intercepts (no response body) or wildcard URLs with no registry match; these stay as bare `cy.intercept()` and are fine

---

## TypeScript errors to expect after applying

Run `pnpm build:tsc` immediately after the codemod. Errors fall into a small number of repeating patterns:

### 1. `never[]` — empty array passed to a list route

```
Argument of type 'never[]' is not assignable to parameter of type 'MetricList | StaticResponse<MetricList>'
```

**Cause:** `cy.intercept('/api/...', [])` → `cy.interceptApi(API_ROUTES.x.getX, [])` — list types have an `items` wrapper.
**Fix:** `{ items: [] }`

### 2. `body` / `delay` not in type — `StaticResponse<T>` not recognised

```
Object literal may only specify known properties, and 'body' does not exist in type 'SomeType | { statusCode: number; log?: boolean }'
```

**Cause:** The global Cypress type declaration (`src/types/cypress.d.ts`) had a narrow type. This was already fixed in this branch — verify `interceptApi` is declared as `T | StaticResponse<T>`.

### 3. `title` not in response type — error body in a test

```
Object literal may only specify known properties, and 'title' does not exist in type 'SchemaList'
```

**Cause:** A test passes an HTTP Problem Details body `{ title: '...', status: 500 }` for an error scenario.
**Fix (preferred):** Drop the `body` entirely — keep only `{ statusCode: 500 }`. Tests that assert error UI behaviour don't need the response body.
**Fix (if the test asserts `title` text is visible):** Cast with `body: { title: '...' } as any`.

### 4. Wrong route chosen — `void` route receiving typed data

```
Argument of type 'Bridge' is not assignable to parameter of type 'void | StaticResponse<void>'
```

**Cause:** The codemod matched a URL to a DELETE/void route instead of GET (can happen when the original intercept used a specific ID rather than `**`, or when the only route registered for that path is DELETE).
**Fix:** Change to the correct GET route, e.g. `API_ROUTES.bridges.getBridgeByName`.

### 5. Missing required field in mock data

```
Property 'adapterId' is missing in type '{ name: string; definition: JsonNode; }' but required in type 'DomainTagOwner'
```

**Cause:** The type system now validates mock objects — a previously-loose `cy.intercept` body becomes strict.
**Fix:** Add the missing field, e.g. `adapterId: 'test-adapter'`.

### 6. Unused type imports

```
All imports in import declaration are unused  (TS6192 / TS6133)
```

**Cause:** Type annotations used as casts for the old `cy.intercept` are no longer needed.
**Fix:** Remove the import line.

---

## Genuinely un-migratable calls (35 at time of writing)

These stay as bare `cy.intercept()` permanently and are expected:

- **Spy-only intercepts** — `cy.intercept('/api/v1/...')` with no response body (used to wait/assert on a request, not mock a response). `cy.interceptApi` requires a response argument.
- **Wildcard URLs with no registry entry** — e.g. `/api/v1/management/protocol-adapters/adapters/my-adapter/tags` (adapter-specific tag URLs). These match parametric routes only via the `**` form; since the URL contains a real ID the Levenshtein distance exceeds the threshold.

The ESLint rule (`local/no-bare-cy-intercept`) will continue to warn on these. They can be suppressed with `// eslint-disable-next-line local/no-bare-cy-intercept` if the warning is unwanted.
