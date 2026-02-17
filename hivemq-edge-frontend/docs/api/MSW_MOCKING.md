---
title: "MSW API Mocking"
author: "Edge Frontend Team"
last_updated: "2026-02-16"
purpose: "Documents Mock Service Worker patterns for API mocking in tests"
audience: "Frontend Developers, AI Agents"
maintained_at: "docs/api/MSW_MOCKING.md"
---

# MSW API Mocking

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Setup](#setup)
- [Handler Organization](#handler-organization)
- [Mock Data Patterns](#mock-data-patterns)
- [Testing Patterns](#testing-patterns)
- [Cypress Integration](#cypress-integration)
- [Debugging](#debugging)
- [Advanced Patterns](#advanced-patterns)
- [Best Practices](#best-practices)
- [Common Pitfalls](#common-pitfalls)
- [Checklist: Adding New MSW Handlers](#checklist-adding-new-msw-handlers)
- [Glossary](#glossary)
- [Related Documentation](#related-documentation)

---

## Overview

HiveMQ Edge frontend uses **Mock Service Worker (MSW)** for API mocking in tests. MSW intercepts network requests at the network level, allowing tests to run without a real backend.

**Benefits:**

- **Realistic testing** - Intercepts actual `fetch`/`axios` calls (not mocked functions)
- **Reusable handlers** - Same mocks work in unit, component, and E2E tests
- **Fast execution** - No network latency, instant responses
- **Deterministic tests** - Consistent mock data prevents flaky tests
- **Error simulation** - Easy to test error states and edge cases

**Version:** msw ^2.x (latest)

---

## Architecture

### How MSW Works

```mermaid
%%{init: {'theme':'base', 'themeVariables': {
  'primaryColor':'#0066CC',
  'primaryTextColor':'#FFFFFF',
  'primaryBorderColor':'#003D7A',
  'secondaryColor':'#28A745',
  'secondaryTextColor':'#FFFFFF',
  'secondaryBorderColor':'#1E7E34',
  'tertiaryColor':'#6C757D',
  'tertiaryTextColor':'#FFFFFF',
  'tertiaryBorderColor':'#495057',
  'edgeLabelBackground':'transparent',
  'lineColor':'#555555'
}}}%%
graph LR
    A[Component] -->|HTTP Request| B[MSW Interceptor]
    B -->|Match Handler?| C{Handler Exists?}
    C -->|Yes| D[Mock Response]
    C -->|No| E[Passthrough to Real API]
    D --> A
    E --> F[Real Backend]
    F --> A

    style A fill:#0066CC,stroke:#003D7A,color:#FFFFFF
    style B fill:#28A745,stroke:#1E7E34,color:#FFFFFF
    style C fill:#6C757D,stroke:#495057,color:#FFFFFF
    style D fill:#28A745,stroke:#1E7E34,color:#FFFFFF
    style E fill:#DC3545,stroke:#B02A37,color:#FFFFFF
    style F fill:#6C757D,stroke:#495057,color:#FFFFFF
```

**Color Legend:**
- **Blue** - UI Component
- **Green** - MSW Interceptor and Mock Response
- **Gray** - Decision Point and Real Backend
- **Red** - Passthrough (no mock)

---

## Setup

### Installation

MSW is already installed in the project:

```bash
pnpm add -D msw
```

### Test Setup

**Vitest configuration:**

```typescript
// src/__test-utils__/setup.ts
import { beforeAll, afterEach, afterAll } from 'vitest'
import { server } from './msw/server'

// Start MSW server before all tests
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

// Reset handlers after each test
afterEach(() => server.resetHandlers())

// Clean up after all tests
afterAll(() => server.close())
```

**Cypress configuration:**

```typescript
// cypress/support/component.ts
import { server } from '@/__test-utils__/msw/server'

// Start MSW before component tests
before(() => {
  server.listen({ onUnhandledRequest: 'warn' })
})

afterEach(() => {
  server.resetHandlers()
})

after(() => {
  server.close()
})
```

---

## Handler Organization

### Colocated Handlers Pattern

**Convention:** MSW handlers live in `__handlers__/` subdirectory next to the hook they mock.

**Directory structure:**

```
src/api/hooks/
├── useProtocolAdapters/
│   ├── useGetAllProtocolAdapters.ts          # Hook
│   ├── useGetAllProtocolAdapters.spec.ts     # Test
│   ├── useCreateProtocolAdapter.ts           # Hook
│   ├── useCreateProtocolAdapter.spec.ts      # Test
│   └── __handlers__/
│       └── index.ts                          # MSW handlers
├── useCombiners/
│   ├── useListCombiners.ts
│   ├── useListCombiners.spec.ts
│   ├── useCreateCombiner.ts
│   ├── useCreateCombiner.spec.ts
│   └── __handlers__/
│       └── index.ts
└── ...
```

**Benefits:**

- **Easy to find** - Handlers live next to hooks they support
- **Scoped mocks** - Each feature has its own handlers
- **Maintainability** - Changes to API hooks suggest checking handlers

---

### Handler Structure

**Pattern:** Export array of HTTP handlers.

```typescript
// src/api/hooks/useProtocolAdapters/__handlers__/index.ts
import { http, HttpResponse } from 'msw'
import type { Adapter, AdaptersList, ProtocolAdaptersList } from '@/api/__generated__'
import { mockAdapter, mockProtocolAdapter } from '@/__test-utils__/adapters'

export const handlers = [
  // GET /api/v1/management/protocol-adapters/types
  http.get('*/protocol-adapters/types', () => {
    return HttpResponse.json<ProtocolAdaptersList>(
      { items: [mockProtocolAdapter] },
      { status: 200 }
    )
  }),

  // GET /api/v1/management/protocol-adapters/adapters
  http.get('*/protocol-adapters/adapters', () => {
    return HttpResponse.json<AdaptersList>(
      { items: [mockAdapter] },
      { status: 200 }
    )
  }),

  // GET /api/v1/management/protocol-adapters/adapters/:adapterId
  http.get('*/protocol-adapters/adapters/:adapterId', ({ params }) => {
    const { adapterId } = params
    return HttpResponse.json<Adapter>(
      { ...mockAdapter, id: adapterId as string },
      { status: 200 }
    )
  }),

  // POST /api/v1/management/protocol-adapters/adapters/:adapterType
  http.post('*/protocol-adapters/adapters/:adapterType', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  // PUT /api/v1/management/protocol-adapters/adapters/:adapterId
  http.put('*/protocol-adapters/adapters/:adapterId', () => {
    return HttpResponse.json({}, { status: 200 })
  }),

  // DELETE /api/v1/management/protocol-adapters/adapters/:adapterId
  http.delete('*/protocol-adapters/adapters/:adapterId', () => {
    return HttpResponse.json({}, { status: 200 })
  }),
]
```

**Key points:**

- **Type responses** - Use TypeScript generics for `HttpResponse.json<T>()`
- **Wildcard URLs** - `*/path` matches any base URL
- **Path parameters** - Extract from `params` object
- **Status codes** - Second argument to `HttpResponse.json()`

---

### Central Handler Registration

**Pattern:** Aggregate handlers from all modules.

```typescript
// src/__test-utils__/msw/handlers.ts
import { handlers as adapterHandlers } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { handlers as combinerHandlers } from '@/api/hooks/useCombiners/__handlers__'
import { handlers as pulseHandlers } from '@/api/hooks/usePulse/__handlers__'
import { handlerCapabilities } from '@/api/hooks/useFrontendServices/__handlers__'

export const createInterceptHandlers = () => {
  return [
    ...handlerCapabilities(),
    ...adapterHandlers,
    ...combinerHandlers,
    ...pulseHandlers,
  ]
}
```

```typescript
// src/__test-utils__/msw/server.ts
import { setupServer } from 'msw/node'
import { createInterceptHandlers } from './handlers'

export const server = setupServer(...createInterceptHandlers())
```

---

## Mock Data Patterns

### Centralized Mock Data

**Convention:** Shared mock data lives in `src/__test-utils__/`.

```typescript
// src/__test-utils__/adapters/simulation.ts
import type { Adapter, ProtocolAdapter } from '@/api/__generated__'
import { Status } from '@/api/__generated__'

export const MOCK_ADAPTER_ID = 'simulation-1'

export const mockProtocolAdapter: ProtocolAdapter = {
  id: 'simulation',
  protocol: 'Simulation',
  name: 'Simulated Edge Device',
  description: 'Simulate traffic from an edge device',
  version: 'Development Version',
  installed: true,
  capabilities: ['READ'],
  category: {
    name: 'SIMULATION',
    displayName: 'Simulation',
  },
  configSchema: { /* JSON Schema */ },
  uiSchema: { /* UI Schema */ },
}

export const mockAdapter: Adapter = {
  id: MOCK_ADAPTER_ID,
  type: 'simulation',
  config: {
    minValue: 0,
    maxValue: 1000,
    simulationToMqtt: {
      pollingIntervalMillis: 10000,
      simulationToMqttMappings: [
        { mqttTopic: 'sensor/temperature', qos: 0 },
      ],
    },
  },
  status: {
    startedAt: '2023-08-21T11:51:24.234+01',
    connection: Status.connection.CONNECTED,
    runtime: Status.runtime.STARTED,
  },
}
```

**Benefits:**

- **Reuse across tests** - One mock data object, many tests
- **Consistency** - Same data in unit, component, and E2E tests
- **Type safety** - Mock data typed against generated models

---

### Factory Functions

**Pattern:** Generate dynamic mock data.

```typescript
// src/__test-utils__/adapters/index.ts
import type { Adapter } from '@/api/__generated__'
import { Status } from '@/api/__generated__'

export const createMockAdapter = (overrides?: Partial<Adapter>): Adapter => ({
  id: 'test-adapter',
  type: 'simulation',
  config: {},
  status: {
    connection: Status.connection.CONNECTED,
    runtime: Status.runtime.STARTED,
  },
  ...overrides,
})

// Usage in tests
const connectedAdapter = createMockAdapter({ id: 'adapter-1' })
const errorAdapter = createMockAdapter({
  id: 'adapter-2',
  status: {
    connection: Status.connection.ERROR,
    message: 'Connection failed',
  },
})
```

---

## Testing Patterns

### Basic Handler Usage

**Test file structure:**

```typescript
// src/api/hooks/useProtocolAdapters/useGetAllProtocolAdapters.spec.ts
import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { useGetAllProtocolAdapters } from './useGetAllProtocolAdapters'
import { wrapper } from '@/__test-utils__/hooks/SimpleWrapper'

describe('useGetAllProtocolAdapters', () => {
  it('should fetch all adapters', async () => {
    const { result } = renderHook(() => useGetAllProtocolAdapters(), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(result.current.data?.items).toHaveLength(1)
    expect(result.current.data?.items[0].id).toBe('simulation-1')
  })
})
```

**Wrapper provides:**

- React Query `QueryClientProvider`
- MSW handlers automatically active (via `setup.ts`)

---

### Override Handler Per Test

**Pattern:** Replace default handler for specific test case.

```typescript
import { server } from '@/__test-utils__/msw/server'
import { http, HttpResponse } from 'msw'

describe('useGetAllProtocolAdapters', () => {
  it('should handle empty adapter list', async () => {
    // Override default handler
    server.use(
      http.get('*/protocol-adapters/adapters', () => {
        return HttpResponse.json<AdaptersList>({ items: [] }, { status: 200 })
      })
    )

    const { result } = renderHook(() => useGetAllProtocolAdapters(), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.items).toHaveLength(0)
  })
})
```

**Note:** `server.use()` handlers are reset after each test by `afterEach(() => server.resetHandlers())`.

---

### Testing Error States

**Pattern:** Return error responses to test error handling.

```typescript
describe('useGetAllProtocolAdapters - Error States', () => {
  it('should handle 500 server error', async () => {
    server.use(
      http.get('*/protocol-adapters/adapters', () => {
        return HttpResponse.json(
          { error: 'Internal Server Error' },
          { status: 500 }
        )
      })
    )

    const { result } = renderHook(() => useGetAllProtocolAdapters(), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(result.current.error?.status).toBe(500)
  })

  it('should handle 401 unauthorized', async () => {
    server.use(
      http.get('*/protocol-adapters/adapters', () => {
        return HttpResponse.json(
          { error: 'Unauthorized' },
          { status: 401 }
        )
      })
    )

    const { result } = renderHook(() => useGetAllProtocolAdapters(), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(result.current.error?.status).toBe(401)
  })

  it('should handle network error', async () => {
    server.use(
      http.get('*/protocol-adapters/adapters', () => {
        return HttpResponse.error()  // Network error
      })
    )

    const { result } = renderHook(() => useGetAllProtocolAdapters(), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
```

---

### Testing Mutations

**Pattern:** Verify mutation calls correct endpoint with correct data.

```typescript
import { renderHook, waitFor } from '@testing-library/react'
import { server } from '@/__test-utils__/msw/server'
import { http } from 'msw'

describe('useCreateProtocolAdapter', () => {
  it('should create adapter', async () => {
    let capturedRequest: Adapter | null = null

    // Capture request body
    server.use(
      http.post('*/protocol-adapters/adapters/:adapterType', async ({ request, params }) => {
        const { adapterType } = params
        capturedRequest = await request.json()
        return HttpResponse.json({}, { status: 200 })
      })
    )

    const { result } = renderHook(() => useCreateProtocolAdapter(), { wrapper })

    const newAdapter: Adapter = {
      id: 'new-adapter',
      type: 'simulation',
      config: {},
    }

    result.current.mutate({
      adapterType: 'simulation',
      requestBody: newAdapter,
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    // Verify request was sent with correct data
    expect(capturedRequest).toEqual(newAdapter)
  })
})
```

---

### Simulating Delays

**Pattern:** Add artificial delay to test loading states.

```typescript
import { delay, http, HttpResponse } from 'msw'

server.use(
  http.get('*/protocol-adapters/adapters', async () => {
    await delay(2000)  // 2 second delay
    return HttpResponse.json<AdaptersList>({ items: [mockAdapter] })
  })
)

it('should show loading state', async () => {
  const { result } = renderHook(() => useGetAllProtocolAdapters(), { wrapper })

  // Initially loading
  expect(result.current.isLoading).toBe(true)

  await waitFor(() => expect(result.current.isSuccess).toBe(true))

  // Eventually loaded
  expect(result.current.isLoading).toBe(false)
})
```

---

### Dynamic Responses

**Pattern:** Return different responses based on request parameters.

```typescript
const adapterStore = new Map<string, Adapter>([
  ['simulation-1', mockAdapter],
  ['opcua-1', mockAdapterOPCUA],
])

http.get('*/protocol-adapters/adapters/:adapterId', ({ params }) => {
  const { adapterId } = params
  const adapter = adapterStore.get(adapterId as string)

  if (!adapter) {
    return HttpResponse.json(
      { error: 'Adapter not found' },
      { status: 404 }
    )
  }

  return HttpResponse.json<Adapter>(adapter, { status: 200 })
})

http.delete('*/protocol-adapters/adapters/:adapterId', ({ params }) => {
  const { adapterId } = params
  adapterStore.delete(adapterId as string)
  return HttpResponse.json({}, { status: 200 })
})
```

---

## Cypress Integration

### Component Tests with MSW

**Pattern:** MSW handlers work automatically in Cypress component tests.

```typescript
// cypress/support/component.ts
import { server } from '@/__test-utils__/msw/server'

before(() => {
  server.listen({ onUnhandledRequest: 'warn' })
})

afterEach(() => {
  server.resetHandlers()
})

after(() => {
  server.close()
})
```

**Component test:**

```typescript
// src/modules/ProtocolAdapters/AdapterList.spec.cy.tsx
import { AdapterList } from './AdapterList'

describe('AdapterList', () => {
  it('should display adapters', () => {
    cy.mountWithProviders(<AdapterList />)

    // MSW handler returns mockAdapter
    cy.getByTestId('adapter-list').should('exist')
    cy.contains('simulation-1').should('be.visible')
  })
})
```

---

### E2E Tests (Separate MSW Instance)

**Pattern:** E2E tests use custom intercepts, not shared MSW handlers.

```typescript
// cypress/e2e/adapters/adapter-list.cy.ts
describe('Adapter List', () => {
  it('should display adapters', () => {
    // Custom intercept for E2E
    cy.intercept('GET', '/api/v1/management/protocol-adapters/adapters', {
      statusCode: 200,
      body: { items: [mockAdapter] },
    }).as('getAdapters')

    cy.visit('/adapters')

    cy.wait('@getAdapters')
    cy.contains('simulation-1').should('be.visible')
  })
})
```

**Why separate?**

- E2E tests verify full app behavior including routing
- `cy.intercept()` provides E2E-specific features (wait, alias)
- MSW handlers stay focused on unit/component tests

---

## Debugging

### Log Handler Calls

```typescript
// src/__test-utils__/msw/server.ts
import { setupServer } from 'msw/node'

export const server = setupServer(...createInterceptHandlers())

// Enable logging in development
if (import.meta.env.DEV) {
  server.events.on('request:start', ({ request }) => {
    console.log('MSW intercepted:', request.method, request.url)
  })
}
```

---

### Unhandled Requests

**Configuration:**

```typescript
// Strict mode (fail tests on unhandled requests)
server.listen({ onUnhandledRequest: 'error' })

// Warn mode (log but don't fail)
server.listen({ onUnhandledRequest: 'warn' })

// Bypass mode (let real requests through)
server.listen({ onUnhandledRequest: 'bypass' })
```

**When to use:**

- **`error`** - Unit/component tests (all requests should be mocked)
- **`warn`** - Debugging (identify missing handlers)
- **`bypass`** - Integration tests with real backend

---

### Inspect Request Body

```typescript
http.post('*/protocol-adapters/adapters/:adapterType', async ({ request }) => {
  const body = await request.json()
  console.log('Received request body:', body)
  return HttpResponse.json({}, { status: 200 })
})
```

---

## Advanced Patterns

### GraphQL Mocking

**Pattern:** MSW supports GraphQL queries and mutations.

```typescript
import { graphql, HttpResponse } from 'msw'

const graphqlHandlers = [
  graphql.query('GetUser', ({ variables }) => {
    return HttpResponse.json({
      data: {
        user: { id: variables.id, name: 'John Doe' }
      }
    })
  }),

  graphql.mutation('UpdateUser', ({ variables }) => {
    return HttpResponse.json({
      data: {
        updateUser: { success: true }
      }
    })
  }),
]
```

**Note:** HiveMQ Edge uses REST, not GraphQL. This is for reference only.

---

### Response Composition

**Pattern:** Build complex responses programmatically.

```typescript
import { http, HttpResponse } from 'msw'

http.get('*/protocol-adapters/adapters', () => {
  const adapters = Array.from({ length: 10 }, (_, i) => ({
    ...mockAdapter,
    id: `adapter-${i + 1}`,
  }))

  return HttpResponse.json<AdaptersList>(
    { items: adapters },
    {
      status: 200,
      headers: {
        'X-Total-Count': '10',
        'X-Page': '1',
      },
    }
  )
})
```

---

### Stateful Handlers

**Pattern:** Maintain state across requests within a test.

```typescript
describe('Adapter CRUD operations', () => {
  let adapterStore: Map<string, Adapter>

  beforeEach(() => {
    adapterStore = new Map([
      ['simulation-1', mockAdapter],
    ])

    server.use(
      http.get('*/protocol-adapters/adapters', () => {
        return HttpResponse.json<AdaptersList>({
          items: Array.from(adapterStore.values()),
        })
      }),

      http.post('*/protocol-adapters/adapters/:adapterType', async ({ request }) => {
        const newAdapter = await request.json()
        adapterStore.set(newAdapter.id, newAdapter)
        return HttpResponse.json({}, { status: 200 })
      }),

      http.delete('*/protocol-adapters/adapters/:adapterId', ({ params }) => {
        adapterStore.delete(params.adapterId as string)
        return HttpResponse.json({}, { status: 200 })
      }),
    )
  })

  it('should create and list adapter', async () => {
    // Create
    const createResult = renderHook(() => useCreateProtocolAdapter(), { wrapper })
    createResult.result.current.mutate({ adapterType: 'simulation', requestBody: newAdapter })
    await waitFor(() => expect(createResult.result.current.isSuccess).toBe(true))

    // List
    const listResult = renderHook(() => useGetAllProtocolAdapters(), { wrapper })
    await waitFor(() => expect(listResult.result.current.isSuccess).toBe(true))
    expect(listResult.result.current.data?.items).toHaveLength(2)
  })

  it('should delete adapter', async () => {
    // Delete
    const deleteResult = renderHook(() => useDeleteProtocolAdapter(), { wrapper })
    deleteResult.result.current.mutate({ adapterId: 'simulation-1' })
    await waitFor(() => expect(deleteResult.result.current.isSuccess).toBe(true))

    // List
    const listResult = renderHook(() => useGetAllProtocolAdapters(), { wrapper })
    await waitFor(() => expect(listResult.result.current.isSuccess).toBe(true))
    expect(listResult.result.current.data?.items).toHaveLength(0)
  })
})
```

---

## Best Practices

### ✅ Do

- **Colocate handlers** - Keep MSW handlers next to hooks in `__handlers__/`
- **Type responses** - Use `HttpResponse.json<T>()` with generated types
- **Use mock data utilities** - Import from `@/__test-utils__/adapters`
- **Reset handlers after each test** - `afterEach(() => server.resetHandlers())`
- **Test error states** - Override handlers to return errors
- **Extract to factories** - Use factory functions for dynamic mock data
- **Log unhandled requests** - Use `onUnhandledRequest: 'warn'` during development

### ❌ Don't

- **Don't mock in E2E tests** - Use `cy.intercept()` instead
- **Don't commit `.only()` tests** - Handlers are shared, isolation matters
- **Don't hardcode URLs** - Use wildcard `*/path` for flexibility
- **Don't ignore TypeScript errors** - Mock data must match generated types
- **Don't forget `await request.json()`** - Request body is async
- **Don't mix MSW and `cy.intercept()`** - Choose one per test type
- **Don't share state between tests** - Use `beforeEach()` to reset state

---

## Common Pitfalls

### Issue 1: Handler Not Matching

**Problem:** Request goes through to real API despite handler existing.

**Cause:** URL pattern doesn't match.

**Solution:**

```typescript
// ❌ Wrong: Too specific
http.get('http://localhost:8080/api/v1/management/protocol-adapters/adapters', ...)

// ✅ Correct: Wildcard
http.get('*/protocol-adapters/adapters', ...)
```

---

### Issue 2: Stale Handlers

**Problem:** Handler from previous test affects current test.

**Cause:** `server.resetHandlers()` not called.

**Solution:**

```typescript
// Add to test setup
afterEach(() => {
  server.resetHandlers()
})
```

---

### Issue 3: Async Request Body

**Problem:** `await request.json()` not used.

```typescript
// ❌ Wrong: Body is a promise
http.post('*/adapters', ({ request }) => {
  const body = request.json()  // Promise!
  console.log(body)  // [Object Promise]
})

// ✅ Correct: Await the promise
http.post('*/adapters', async ({ request }) => {
  const body = await request.json()
  console.log(body)  // { id: 'adapter-1', ... }
})
```

---

### Issue 4: Handler Order

**Problem:** More specific handler is never reached.

**Cause:** MSW matches handlers in order. First match wins.

```typescript
// ❌ Wrong order: Specific handler is never reached
http.get('*/adapters/:adapterId', ...),  // Matches all adapter IDs
http.get('*/adapters/special-id', ...),  // Never reached

// ✅ Correct order: Specific handler first
http.get('*/adapters/special-id', ...),  // Matches special-id
http.get('*/adapters/:adapterId', ...),  // Matches all others
```

---

## Checklist: Adding New MSW Handlers

- [ ] Create `__handlers__/index.ts` in hook directory
- [ ] Export array of MSW handlers
- [ ] Use `http.{method}()` from `msw` package
- [ ] Type responses with `HttpResponse.json<T>()`
- [ ] Use wildcard URLs (`*/path`)
- [ ] Import mock data from `@/__test-utils__/`
- [ ] Handle path parameters with `({ params }) =>`
- [ ] Register handlers in `src/__test-utils__/msw/handlers.ts`
- [ ] Test handlers in hook spec file
- [ ] Document complex handler logic if needed

---

## Glossary

| Term | Definition |
|------|------------|
| **MSW** | Mock Service Worker - library for intercepting network requests in tests |
| **Handler** | Function that defines how to respond to a specific HTTP request pattern |
| **Interceptor** | MSW component that captures network requests before they reach the network |
| **Service Worker** | Browser API used by MSW to intercept requests at the network level |
| **Request Handler** | MSW function that matches HTTP requests and returns mock responses |
| **HttpResponse** | MSW utility for creating mock HTTP responses with status codes and bodies |
| **Wildcard URL** | URL pattern using `*` to match any base URL (e.g., `*/api/users`) |
| **Path Parameter** | Dynamic segment in URL extracted from request (e.g., `:userId` in `/users/:userId`) |
| **Passthrough** | Allowing a request to bypass MSW and reach the real API |
| **Handler Override** | Temporarily replacing a handler in a specific test using `server.use()` |
| **Factory Function** | Function that creates mock data with customizable overrides |
| **Stateful Handler** | Handler that maintains state across multiple requests within a test |

---

## Related Documentation

**API:**
- [OpenAPI Integration](./OPENAPI_INTEGRATION.md)
- [React Query Patterns](./REACT_QUERY_PATTERNS.md)

**Guides:**
- [Testing Guide](../guides/TESTING_GUIDE.md)
- [Cypress Guide](../guides/CYPRESS_GUIDE.md)
