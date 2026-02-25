# EDG-73: Type-Safe Cypress `cy.intercept` Infrastructure

## Problem Statement

The `cy.intercept` calls in Cypress tests are untyped, creating false positives in testing.
Specifically in `src/modules/Login/components/Login.spec.cy.tsx`:

```typescript
cy.intercept('/api/v1/auth/authenticate', {
  token: 'fake_token',
}).as('getConfig')
```

This intercept should return an `ApiBearerToken` object, but TypeScript does not enforce this.

## Problems Identified

### Problem 1: Response body is not type-checked

The response body `{ token: 'fake_token' }` is passed as a plain object with no TypeScript
validation that it conforms to `ApiBearerToken`. Any arbitrary object can be passed without
compile-time errors.

### Problem 2: URL strings are disconnected from service definitions

The URL `/api/v1/auth/authenticate` is a magic string with no link to the `AuthenticationService`
where `authenticate()` defines both the URL and the return type `ApiBearerToken`. URL changes
or typos in the service are not caught in tests.

### Problem 3: `cy.intercept<T>` generic is misunderstood

Existing uses like `cy.intercept<NorthboundMappingOwnerList>('/api/v1/...', { items: [...] })`
give a **false** sense of type safety. The generic `<T>` in Cypress types the **request body**
(`req.body`), not the response body. The second argument (static response object) is not
validated at all.

## Scale

- **556+** occurrences in `src/` (component tests)
- **286+** occurrences in `cypress/` (e2e tests and utils)

## Current Infrastructure (What Works)

The Vitest/MSW layer (`src/api/hooks/*/__handlers__/`) IS fully typed:

```typescript
// src/api/hooks/usePostAuthentication/__handlers__/index.ts
http.post('*/auth/authenticate', () => {
  return HttpResponse.json<ApiBearerToken>({ token: TOKEN }, { status: 200 })
})
```

MSW uses proper generics: `http.get<PathParams, RequestBody>()` and
`HttpResponse.json<ResponseType>()`. The problem is exclusively in Cypress tests.

## Goals

1. **Type-safe response body**: TypeScript should enforce that the response matches the OpenAPI
   model for a given route
2. **URL-to-service linkage**: URLs should be traceable back to the generated service definitions
3. **Migration path**: A tool or pattern to migrate all existing bare `cy.intercept` calls
4. **Developer ergonomics**: The new API should be easy to use and not require manual lookup of
   types per URL
5. **Maintenance**: When the OpenAPI spec changes and services regenerate, the test intercepts
   should fail at compile time if types change

## Related Files

- `src/api/__generated__/services/AuthenticationService.ts` — defines the URL and return type
- `src/api/__generated__/models/ApiBearerToken.ts` — the expected response model
- `src/modules/Login/components/Login.spec.cy.tsx` — the example of the problem
- `cypress/support/commands.ts` — where custom Cypress commands are registered
- `cypress/support/component.ts` — component test support file
- `cypress/utils/intercept.utils.ts` — shared E2E intercept utilities
- `src/api/hooks/usePostAuthentication/__handlers__/index.ts` — the correct MSW pattern
