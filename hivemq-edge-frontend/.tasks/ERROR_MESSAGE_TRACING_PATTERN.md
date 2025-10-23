# Error Message Tracing Pattern

**Created:** October 22, 2025  
**Purpose:** Document the systematic approach to tracing error messages through the entire technology stack from API to UI

---

## Overview

This document describes a methodology for understanding how error messages flow through the HiveMQ Edge Frontend architecture, enabling AI agents and developers to write more accurate, resilient tests and better understand the system's error handling.

## The Pattern: Full-Stack Error Tracing

When you need to test or understand error messages in the application, follow this systematic approach to trace the error from its origin through each layer of the stack.

---

## Technology Stack Layers

The HiveMQ Edge Frontend uses multiple technologies that work together to handle errors:

```
API Definition (OpenAPI)
    ↓
Generated Service (openapi-typescript-codegen)
    ↓
React Query Hook (TanStack Query)
    ↓
MSW Mock Handlers (for testing)
    ↓
React Component (UI Display)
    ↓
Cypress E2E Test (Verification)
```

Each layer adds or transforms error information. Understanding this flow is critical for accurate testing.

---

## Step-by-Step Tracing Guide

### Step 1: Identify the OpenAPI Service Definition

**Location:** `src/api/__generated__/services/[ServiceName].ts`

**What to find:**

- The HTTP method and endpoint
- The `errors` object which defines status codes and messages
- These are generated from OpenAPI specs and define the contract

**Example: Bridge Creation**

```typescript
// src/api/__generated__/services/BridgesService.ts
public addBridge(requestBody: Bridge): CancelablePromise<any> {
    return this.httpRequest.request({
        method: 'POST',
        url: '/api/v1/management/bridges',
        body: requestBody,
        mediaType: 'application/json',
        errors: {
            400: `Bridge is invalid`,  // ← API error message
        },
    });
}
```

**Key Insight:** The error messages here are the **API contract** - what the backend returns.

---

### Step 2: Find the React Query Hook

**Location:** `src/api/hooks/[useHookName]/[useHookName].ts`

**What to find:**

- The mutation or query function
- The return type: `useMutation<SuccessType, ApiError, InputType>`
- How errors are typed (`ApiError`)

**Example: Bridge Creation Hook**

```typescript
// src/api/hooks/useGetBridges/useCreateBridge.ts
export const useCreateBridge = () => {
  const appClient = useHttpClient()
  const queryClient = useQueryClient()

  const createBridge = (requestBody: Bridge) => {
    return appClient.bridges.addBridge(requestBody)
  }

  return useMutation<unknown, ApiError, Bridge>({
    // ← ApiError type
    mutationFn: createBridge,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.BRIDGES] })
    },
  })
}
```

**Key Insight:** React Query wraps the API error in an `ApiError` type which includes:

- `message`: Error status text
- `body`: Response body (often contains `title` with human-readable error)
- `status`: HTTP status code

---

### Step 3: Check the MSW Mock Handler

**Location:** `src/api/hooks/[useHookName]/__handlers__/index.ts`

**What to find:**

- The MSW `http` handler for the endpoint
- What the mock returns on success
- What error responses are mocked (if any)

**Example: Bridge Handlers**

```typescript
// src/api/hooks/useGetBridges/__handlers__/index.ts
export const handlers = [
  http.post('*/bridges', () => {
    return HttpResponse.json({}, { status: 200 }) // ← Success only
  }),

  // No error handler defined yet - could add:
  // http.post('*/bridges', () => {
  //   return HttpResponse.json(
  //     { title: 'Bridge is invalid' },
  //     { status: 400 }
  //   )
  // })
]
```

**Key Insight:** MSW handlers control what errors appear in tests. You can:

- Add error scenarios by creating additional handlers
- Return specific error messages in the response body
- Use conditional logic to trigger errors based on request data

---

### Step 4: Examine the React Component

**Location:** `src/modules/[ModuleName]/components/[ComponentName].tsx`

**What to find:**

- How the hook is called
- Error handling in `.catch()` or error state
- How errors are displayed (Toast, Alert, inline messages)
- Translation keys used (if i18n is involved)

**Example: Bridge Editor**

```typescript
// src/modules/Bridges/components/panels/BridgeEditor.tsx
const createBridge = useCreateBridge()

// Inside submit handler:
createBridge
  .mutateAsync(data)
  .then(() => {
    successToast({
      title: t('bridge.toast.create.title'),
      description: t('bridge.toast.create.description'),
    })
  })
  .catch((err: ApiError) =>
    errorToast(
      {
        title: t('bridge.toast.create.title'),
        description: t('bridge.toast.create.error'), // ← Generic message
      },
      err // ← Actual ApiError passed to toast
    )
  )
```

**Key Insight:** Components often show:

- **Generic user-facing messages** via i18n keys
- **Detailed error info** from the `ApiError` object
- Different UI patterns: toasts (transient), alerts (persistent), inline errors

---

### Step 5: Trace Client-Side Validation (RJSF Forms)

**Important:** Many errors in this application come from **client-side validation** before API calls!

**Location:** Form components using RJSF (React JSON Schema Form)

**What to find:**

- JSON Schema definitions (often from OpenAPI models)
- Required fields in the schema
- Custom validation rules
- Error summary panels

**Example: Bridge Form Validation**

```typescript
// Bridge schema (from OpenAPI)
export type Bridge = {
  id: string // Required
  host: string // Required
  clientId?: string // Optional in type, but form requires it
  // ... other fields
}
```

**RJSF Error Messages:**

- Format: `"[fieldName] is a required property"`
- Generated automatically from JSON Schema
- Displayed in error summary panel before API submission

**Key Insight:** Client-side validation errors appear **before** any API call, so:

- They don't appear in MSW handlers
- They come from JSON Schema validation
- Test these by asserting on the error summary panel content

---

### Step 6: Create Cypress Page Object Getters

**Location:** `cypress/pages/[PageName]/[PageName].ts`

**What to create:**

- Getters for error containers (alerts, panels, toasts)
- Getters for specific error messages
- Methods to access error text at different indices

**Example: Bridge Page Object**

```typescript
// cypress/pages/Bridges/BridgePage.ts
config = {
  get errorSummaryPanel() {
    return cy.get('[role="dialog"] form div[role="alert"]')
  },

  get errorSummary() {
    return cy.get('[role="dialog"] form div[role="alert"] ul li')
  },

  errorSummaryItem(index: number) {
    return cy.get('[role="dialog"] form div[role="alert"] ul li').eq(index)
  },
}
```

**Key Insight:** Page objects provide:

- Reusable selectors for error UI
- Type-safe access to error elements
- Abstraction over DOM structure changes

---

### Step 7: Write the Cypress Test with Assertions

**Location:** `cypress/e2e/[module]/[module].spec.cy.ts`

**What to test:**

1. **Trigger the error** (submit invalid data, use wrong credentials, etc.)
2. **Verify error UI appears** (panel visible, correct number of errors)
3. **Assert on specific error messages** (use exact or partial text matching)
4. **Take Percy snapshot** (for visual regression)

**Example: Bridge Validation Test**

```typescript
it('should capture bridge validation errors', { tags: ['@percy'] }, () => {
  cy.injectAxe()
  bridgePage.addNewBridge.click()

  // Trigger validation by clicking submit without required fields
  bridgePage.config.submitButton.click()

  // Verify error panel structure
  bridgePage.config.errorSummaryPanel.should('be.visible')
  bridgePage.config.errorSummary.should('have.length', 3)

  // Assert on specific error messages (from RJSF validation)
  bridgePage.config.errorSummaryItem(0).should('contain.text', 'id')
  bridgePage.config.errorSummaryItem(0).should('contain.text', 'required property')

  bridgePage.config.errorSummaryItem(1).should('contain.text', 'host')
  bridgePage.config.errorSummaryItem(1).should('contain.text', 'required property')

  bridgePage.config.errorSummaryItem(2).should('contain.text', 'clientId')
  bridgePage.config.errorSummaryItem(2).should('contain.text', 'required property')

  // Visual regression snapshot
  cy.checkAccessibility()
  cy.percySnapshot('Bridges - Validation Errors')
})
```

---

## Complete Example: Authentication Error Flow

Let's trace a complete example from the authentication system.

### 1. OpenAPI Service Definition

```typescript
// src/api/__generated__/services/AuthenticationService.ts
public authenticate(requestBody?: UsernamePasswordCredentials): CancelablePromise<ApiBearerToken> {
    return this.httpRequest.request({
        method: 'POST',
        url: '/api/v1/auth/authenticate',
        body: requestBody,
        mediaType: 'application/json',
        errors: {
            400: `Error in request.`,
            401: `The requested credentials could not be authenticated.`,  // ← API message
        },
    });
}
```

### 2. React Query Hook

```typescript
// src/api/hooks/usePostAuthentication/index.ts
export const usePostAuthentication = () => {
  const postCredential = (credentials: UsernamePasswordCredentials) => {
    const appClient = new HiveMqClient({ BASE: config.apiBaseUrl })
    return appClient.authentication.authenticate(credentials)
  }

  return useMutation<ApiBearerToken, ApiError, UsernamePasswordCredentials>({
    mutationFn: postCredential,
  })
}
```

### 3. MSW Mock Handler

```typescript
// src/api/hooks/usePostAuthentication/__handlers__/index.ts
export const mockAuthApi = (credentials: UsernamePasswordCredentials) => {
  if (credentials.userName === 'admin' && credentials.password === 'password')
    return (req) => req.reply({ token: TOKEN })

  return (req) => {
    req.reply({
      statusCode: 401,
      status: 401,
      body: {
        title: 'Invalid username and/or password', // ← Mock error message
        code: 401,
      },
    })
  }
}
```

### 4. React Component

```typescript
// src/modules/Login/components/Login.tsx
const { isPending, isError, error, mutateAsync: submitCredentials } = usePostAuthentication()

const onSubmit: SubmitHandler<UsernamePasswordCredentials> = (data) => {
  submitCredentials({ password: data.password, userName: data.userName })
    .then(verifyCredential)
    .catch((e: ApiError) => {
      setError('root.ApiError', {
        type: e.message,
        message: e.body.title  // ← Error displayed: "Invalid username and/or password"
      })
    })
}

// In JSX:
{errors.root?.ApiError && (
  <ErrorMessage
    type={errors.root?.ApiError.type}
    message={errors.root?.ApiError.message}  // ← Shows in Alert component
  />
)}
```

### 5. Page Object

```typescript
// cypress/pages/Login/LoginPage.ts
get errorAlert() {
  return cy.get('[role="alert"][data-status="error"]')
}

get errorAlertTitle() {
  return this.errorAlert.find('[class*="alert__title"]')
}

get errorAlertMessage() {
  return this.errorAlert.find('[class*="alert__description"]')
}
```

### 6. Cypress Test

```typescript
// cypress/e2e/Login/login.spec.cy.ts
it('should capture login error state', { tags: ['@percy'] }, () => {
  cy.injectAxe()

  // Trigger error with invalid credentials
  loginPage.usernameInput.type('admin')
  loginPage.passwordInput.type('wrong-password')
  loginPage.loginButton.click()

  // Assert on error message from MSW handler
  loginPage.errorAlert.should('be.visible')
  loginPage.errorAlertMessage.should('have.text', 'Invalid username and/or password')

  // Percy snapshot
  cy.checkAccessibility()
  cy.percySnapshot('Login - Error State')
})
```

---

## Error Message Source Matrix

Use this table to identify where different types of error messages come from:

| Error Type                       | Source                | Example Message                      | When It Appears                  |
| -------------------------------- | --------------------- | ------------------------------------ | -------------------------------- |
| **Client-Side Validation**       | RJSF/JSON Schema      | `"id is a required property"`        | Before API call, form validation |
| **API Error (Success Response)** | OpenAPI spec          | `"Bridge is invalid"`                | API returns error status code    |
| **API Error (Body Title)**       | MSW mock handler      | `"Invalid username and/or password"` | Test environment API response    |
| **User-Facing Message**          | i18n translation keys | `"Unable to create bridge"`          | Generic UI message for users     |
| **Network Error**                | Axios/Fetch           | `"Network Error"`                    | Connection failure               |
| **Timeout Error**                | React Query           | `"Request timed out"`                | Long-running request             |

---

## AI Agent Decision Tree

When an AI agent needs to test error messages, follow this decision tree:

```
1. Is this a form validation error?
   ├─ YES → Check JSON Schema for required fields
   │         Assert on RJSF error summary panel
   │         Example: "field is a required property"
   │
   └─ NO → Continue to step 2

2. Is this an API error?
   ├─ YES → Check OpenAPI service for error codes
   │         Check MSW handlers for mock response
   │         Assert on error display component (Toast/Alert)
   │
   └─ NO → Continue to step 3

3. Is this a network/timeout error?
   └─ YES → Check React Query configuration
             Assert on generic error handling
```

---

## Best Practices

### ✅ DO

1. **Trace the full stack** before writing assertions
2. **Use exact error messages** when they're critical to UX
3. **Use partial matches** when error format may change
4. **Create page object getters** for reusable error access
5. **Document the error source** in test comments
6. **Test both validation and API errors** separately

### ❌ DON'T

1. **Assume error messages** without checking sources
2. **Hard-code selectors** in tests (use page objects)
3. **Mix up client vs server errors** in assertions
4. **Skip MSW handlers** for API error scenarios
5. **Test transient errors** (toasts) with Percy (timing issues)

---

## Pattern Benefits

This systematic approach provides:

1. **Accuracy**: Tests assert on actual error messages from the system
2. **Maintainability**: Changes to error handling are easy to track
3. **Documentation**: The tracing process itself documents the system
4. **Confidence**: Tests verify the entire error flow, not just UI presence
5. **Reusability**: Page object getters work across multiple tests

---

## Related Documentation

- **AUTONOMY_TEMPLATE.md** - General AI work patterns
- **COVERAGE_MATRIX.md** - Visual regression coverage details
- **Percy Phase 2 Implementation** - Error state snapshot strategy

---

## Changelog

- **2025-10-22**: Initial documentation created
- Pattern demonstrated with Bridge validation errors
- Complete authentication error example added
- Decision tree and best practices documented

---

**This pattern was developed during task 37074-percy-optimisation and represents a key insight into multi-technology stack integration testing.**
