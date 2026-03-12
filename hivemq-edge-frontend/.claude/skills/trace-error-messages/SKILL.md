---
name: trace-error-messages
description: >
  Use this skill when writing tests that assert on error messages, or when an error
  assertion is failing and the source of the message is unclear. Invoke automatically
  when: a Cypress assertion on error text is failing, you are writing a new test for
  an error state, the user asks "why is this error message showing?", or you need to
  mock an API error in a test. Also invoke when adding error state coverage to a
  form or page.
argument-hint: '[component-or-test-file]'
disable-model-invocation: false
user-invocable: true
allowed-tools: Bash, Read, Grep, Glob
---

# Trace Error Messages

Systematic approach to locating error message sources before writing test assertions.

**Rule: Never assume an error message. Trace it to its source first.**

---

## When to invoke

Invoke this skill when:

- A Cypress assertion on error text is failing
- You are writing a new test for an error state
- The user asks "why is this error message showing?"
- You need to mock an API error in a test
- You are adding error state coverage to a form or page

---

## Stack layers

Errors flow through these layers — identify which layer owns the message before writing assertions:

```
API Definition (OpenAPI)
    ↓
Generated Service (src/api/__generated__/services/)
    ↓
React Query Hook (src/api/hooks/)
    ↓
MSW Mock Handler (src/api/hooks/[hook]/__handlers__/)
    ↓
React Component (error display: Toast / Alert / inline)
    ↓
RJSF Form (client-side validation only)
    ↓
Page Object (cypress/pages/)
    ↓
Cypress Test (cypress/e2e/)
```

---

## Decision tree — start here

```
Is this a form validation error (before any API call)?
├─ YES → Step 5 (RJSF). Check JSON Schema required fields.
│         Assert on the RJSF error summary panel.
│
└─ NO → Is this an API error?
         ├─ YES → Steps 1–4. Trace: OpenAPI → hook → MSW handler → component.
         │
         └─ NO → Network/timeout error → Step 4. React Query + generic error UI.
```

---

## 7-step tracing guide

### Step 1 — OpenAPI service definition

**Where:** `src/api/__generated__/services/[ServiceName].ts`

Find the `errors` object — these are the API contract messages:

```typescript
errors: {
  400: `Bridge is invalid`,  // ← this becomes ApiError.message
  401: `The requested credentials could not be authenticated.`,
}
```

The `errors` field values propagate as `ApiError.message`. They are **not** what the UI displays — components usually show a different i18n key or `ApiError.body.title`.

### Step 2 — React Query hook

**Where:** `src/api/hooks/[hookName]/[hookName].ts`

Verify the error type: `useMutation<Success, ApiError, Input>`.

`ApiError` contains:

- `message` — status text from the OpenAPI `errors` object
- `body.title` — human-readable message from the response body
- `status` — HTTP status code

The component typically displays `err.body.title`, not `err.message`.

### Step 3 — MSW mock handler

**Where:** `src/api/hooks/[hookName]/__handlers__/index.ts`

This controls what error the test actually sees. If an error handler does not exist yet, add one:

```typescript
http.post('*/endpoint', () => {
  return HttpResponse.json({ title: 'Your error text here' }, { status: 400 })
})
```

The `title` in the response body is what `ApiError.body.title` contains — and what the component displays.

### Step 4 — React component

**Where:** `src/modules/[Module]/components/[Component].tsx`

Find where the hook's error is consumed:

- `.catch((err: ApiError) => ...)` on mutations
- `error` property from `useQuery`

Identify what the component actually renders:

- `err.body.title` → the server-provided message (from the MSW handler)
- `t('some.key')` → a generic i18n message
- Both mixed (generic title + specific detail from `err.body.title`)

### Step 5 — RJSF client-side validation

**Where:** JSON Schema for the form (in `src/api/schemas/` or inline in the component)

RJSF generates error messages automatically from the schema:

- Required field: `"[fieldName] is a required property"`
- Format error: `"[fieldName] must match format 'email'"`

These appear **before** any API call. Assert on the RJSF error summary panel:

```typescript
cy.get('[role="alert"] ul li').eq(0).should('contain.text', 'id is a required property')
```

### Step 6 — Page Object getter

**Where:** `cypress/pages/[Module]/[ModulePage].ts`

Check whether an error getter already exists:

```bash
grep -r "error\|alert\|toast" cypress/pages/[Module]/ --include="*.ts"
```

If not, add one. Never use raw selectors in test bodies.

### Step 7 — Cypress test

Write assertions only after completing steps 1–6. Use the exact message from whichever layer owns it:

```typescript
// RJSF validation (step 5 message)
bridgePage.config.errorSummaryItem(0).should('contain.text', 'id is a required property')

// API error (step 3 MSW body.title)
loginPage.errorAlertMessage.should('have.text', 'Invalid username and/or password')
```

Use `contain.text` for partial matches when message format may evolve. Use `have.text` for exact matches on critical UX strings.

---

## Error Message Source Matrix

| Error type             | Source                   | Example                              | When it appears                           |
| ---------------------- | ------------------------ | ------------------------------------ | ----------------------------------------- |
| Client-side validation | RJSF / JSON Schema       | `"id is a required property"`        | Before API call                           |
| API contract message   | OpenAPI `errors` object  | `"Bridge is invalid"`                | `ApiError.message`                        |
| API response body      | MSW handler `body.title` | `"Invalid username and/or password"` | `ApiError.body.title` — what the UI shows |
| Generic user-facing    | i18n translation key     | `"Unable to create bridge"`          | Generic label wrapping the detail         |
| Network error          | Axios / Fetch            | `"Network Error"`                    | Connection failure                        |

---

## Do and don't

**Do:**

- Trace the full stack before writing any assertion
- Use partial matches (`contain.text`) when message format may change
- Create page object getters for error containers before writing assertions
- Comment the error source in the test: `// MSW handler: body.title`
- Test RJSF validation and API errors as separate test cases

**Don't:**

- Assume the error text — always verify in source
- Hard-code DOM selectors in test bodies (use page objects)
- Mix up client validation errors and API errors in the same assertion
- Test toast errors with Percy — timing makes snapshots unreliable
