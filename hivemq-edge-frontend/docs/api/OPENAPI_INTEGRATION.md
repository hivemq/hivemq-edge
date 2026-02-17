---
title: "OpenAPI Integration"
author: "Edge Frontend Team"
last_updated: "2026-02-16"
purpose: "Documents the OpenAPI code generation workflow and generated client usage patterns"
audience: "Frontend Developers, AI Agents"
maintained_at: "docs/api/OPENAPI_INTEGRATION.md"
---

# OpenAPI Integration

---

## Table of Contents

- [Overview](#overview)
- [Code Generation Tool](#code-generation-tool)
- [Generated Client Structure](#generated-client-structure)
- [Using the Generated Client](#using-the-generated-client)
- [Model Types](#model-types)
- [JSON Schemas for Forms](#json-schemas-for-forms)
- [Wrapper: useHttpClient Hook](#wrapper-usehttpclient-hook)
- [Development Workflow](#development-workflow)
- [Customization](#customization)
- [Troubleshooting](#troubleshooting)
- [Generated vs Custom Code](#generated-vs-custom-code)
- [Migration Notes](#migration-notes)
- [Best Practices](#best-practices)
- [Checklist: Adding a New Endpoint](#checklist-adding-a-new-endpoint)
- [Glossary](#glossary)
- [Related Documentation](#related-documentation)

---

## Overview

HiveMQ Edge frontend uses **OpenAPI code generation** to automatically create a type-safe TypeScript client from the backend OpenAPI specification. This approach ensures:

- **Type safety:** TypeScript interfaces match backend models
- **API contract enforcement:** Breaking changes fail at compile time
- **Developer productivity:** No manual API client maintenance
- **Single source of truth:** Backend OpenAPI spec drives frontend types

---

## Code Generation Tool

### Tool: openapi-typescript-codegen

**Package:** `openapi-typescript-codegen`
**Version:** Check `package.json` for current version
**Documentation:** https://github.com/ferdikoomen/openapi-typescript-codegen

**Command:**

```bash
pnpm dev:openAPI
```

**Full script:**

```json
{
  "dev:openAPI": "openapi --input '../hivemq-edge-openapi/dist/bundle.yaml' -o ./src/api/__generated__ -c axios --name HiveMqClient --exportSchemas true"
}
```

### Configuration Breakdown

| Parameter | Value | Purpose |
|-----------|-------|---------|
| `--input` | `../hivemq-edge-openapi/dist/bundle.yaml` | Source OpenAPI specification (monorepo sibling) |
| `-o` | `./src/api/__generated__` | Output directory for generated client |
| `-c` | `axios` | HTTP client library to use |
| `--name` | `HiveMqClient` | Name of generated client class |
| `--exportSchemas` | `true` | Export JSON Schema models for RJSF forms |

---

## Generated Client Structure

### Directory Layout

```
src/api/__generated__/
├── index.ts                   # Main exports
├── HiveMqClient.ts            # Client class
├── core/
│   ├── OpenAPI.ts             # Configuration interface
│   ├── BaseHttpRequest.ts     # Abstract HTTP layer
│   ├── AxiosHttpRequest.ts    # Axios implementation
│   ├── ApiError.ts            # Error types
│   ├── ApiRequestOptions.ts   # Request configuration
│   └── ApiResult.ts           # Response types
├── models/
│   ├── Adapter.ts             # Generated TypeScript interfaces
│   ├── Bridge.ts
│   ├── ProtocolAdapter.ts
│   ├── Status.ts
│   └── ... (100+ model files)
├── schemas/
│   ├── $Adapter.ts            # JSON Schema for forms
│   ├── $Bridge.ts
│   └── ... (corresponding schemas)
└── services/
    ├── ProtocolAdaptersService.ts   # API service methods
    ├── BridgesService.ts
    ├── DataHubDataPoliciesService.ts
    └── ... (20+ service files)
```

---

## Using the Generated Client

### 1. Client Instance

The generated `HiveMqClient` class provides access to all API services:

```typescript
import { HiveMqClient } from '@/api/__generated__'

const client = new HiveMqClient({
  BASE: 'http://localhost:8080',
  WITH_CREDENTIALS: false,
})

// Access services via client properties
client.protocolAdapters.getAdapters()
client.bridges.getBridges()
client.dataHubDataPolicies.getAllDataPolicies()
```

### 2. Service Structure

Each service corresponds to an OpenAPI tag and contains all related endpoints:

**Example: ProtocolAdaptersService**

```typescript
class ProtocolAdaptersService {
  // GET /api/v1/management/protocol-adapters/types
  getAdapterTypes(): Promise<ProtocolAdaptersList>

  // GET /api/v1/management/protocol-adapters/adapters
  getAdapters(): Promise<AdaptersList>

  // POST /api/v1/management/protocol-adapters/adapters/{adapterType}
  addAdapter(adapterType: string, requestBody: Adapter): Promise<void>

  // PUT /api/v1/management/protocol-adapters/adapters/{adapterId}
  updateAdapter(adapterId: string, requestBody: Adapter): Promise<void>

  // DELETE /api/v1/management/protocol-adapters/adapters/{adapterId}
  deleteAdapter(adapterId: string): Promise<void>

  // PUT /api/v1/management/protocol-adapters/adapters/{adapterId}/status
  setConnectionStatus(adapterId: string, requestBody: StatusTransitionCommand): Promise<StatusTransitionResult>

  // GET /api/v1/management/protocol-adapters/adapters/{adapterId}/discover
  discover(adapterId: string, options: DiscoverOptions): Promise<ValuesTree>

  // ... (device tag endpoints, mapping endpoints)
}
```

---

## Model Types

### Generated TypeScript Interfaces

Every OpenAPI model becomes a TypeScript interface:

```typescript
// Generated from OpenAPI spec
export interface Adapter {
  id: string
  type: string
  config: GenericObjectType
  status?: Status
}

export interface Status {
  connection?: Status.connection
  runtime?: Status.runtime
  id?: string
  type?: string
  startedAt?: string
  message?: string
}

export namespace Status {
  export enum connection {
    CONNECTED = 'CONNECTED',
    DISCONNECTED = 'DISCONNECTED',
    ERROR = 'ERROR',
    STATELESS = 'STATELESS',
    UNKNOWN = 'UNKNOWN',
  }

  export enum runtime {
    STARTED = 'STARTED',
    STOPPED = 'STOPPED',
  }
}
```

### Enums

OpenAPI enums become TypeScript const enums for type safety:

```typescript
// Usage in code
const adapter: Adapter = {
  id: 'my-adapter',
  type: 'simulation',
  config: {},
  status: {
    connection: Status.connection.CONNECTED,
    runtime: Status.runtime.STARTED,
  }
}
```

---

## JSON Schemas for Forms

**Why export schemas?**

The `--exportSchemas true` flag generates JSON Schema objects used by **RJSF** (React JSON Schema Form) to dynamically render configuration forms for protocol adapters.

### Schema Generation

For each model, a corresponding `$Model.ts` file is generated:

```typescript
// src/api/__generated__/schemas/$Adapter.ts
export const $Adapter = {
  type: 'object',
  properties: {
    id: {
      type: 'string',
      minLength: 1,
      maxLength: 1024,
    },
    type: {
      type: 'string',
    },
    config: {
      type: 'object',
    },
    status: {
      $ref: '#/components/schemas/Status',
    },
  },
  required: ['id', 'type', 'config'],
} as const
```

### Usage in RJSF Forms

```typescript
import { $Adapter } from '@/api/__generated__/schemas/$Adapter'

// Use in RJSF Form component
<Form
  schema={$Adapter}
  uiSchema={uiSchema}
  formData={formData}
  onSubmit={handleSubmit}
/>
```

**See:** RJSF Guide for complete form integration patterns.

---

## Wrapper: useHttpClient Hook

**Why wrap the generated client?**

The generated `HiveMqClient` is wrapped in a custom hook to:

1. **Inject runtime configuration** (BASE URL, auth headers)
2. **Handle authentication** (JWT tokens, credentials)
3. **Provide consistent error handling**
4. **Enable testing** (mock client injection)

### Implementation

```typescript
// src/api/hooks/useHttpClient/useHttpClient.ts
import { useMemo } from 'react'
import { HiveMqClient } from '@/api/__generated__'
import { useAuth } from '@/modules/Auth/hooks/useAuth'

export const useHttpClient = () => {
  const { token } = useAuth()

  return useMemo(() => {
    return new HiveMqClient({
      BASE: import.meta.env.VITE_API_BASE_URL || '/api',
      WITH_CREDENTIALS: true,
      TOKEN: token,
      HEADERS: {
        'Content-Type': 'application/json',
      },
    })
  }, [token])
}
```

### Usage in Hooks

All React Query hooks use `useHttpClient()` to get a configured client instance:

```typescript
// src/api/hooks/useProtocolAdapters/useGetAllProtocolAdapters.ts
import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient'
import { QUERY_KEYS } from '@/api/utils'

export const useGetAllProtocolAdapters = () => {
  const appClient = useHttpClient()

  return useQuery({
    queryKey: [QUERY_KEYS.ADAPTERS],
    queryFn: () => appClient.protocolAdapters.getAdapters(),
  })
}
```

---

## Development Workflow

### Step 1: Backend OpenAPI Spec Update

When the backend team updates the OpenAPI spec:

1. Backend changes are committed to `hivemq-edge-openapi` repository
2. OpenAPI bundle is regenerated: `hivemq-edge-openapi/dist/bundle.yaml`
3. Frontend monorepo reference is updated (if needed)

### Step 2: Regenerate Frontend Client

```bash
# From frontend directory
cd hivemq-edge-frontend

# Regenerate client
pnpm dev:openAPI
```

**What happens:**

- Old generated files in `src/api/__generated__/` are replaced
- New models, services, schemas are created
- TypeScript compilation will fail if breaking changes exist

### Step 3: Fix Breaking Changes

**Example: Field renamed**

```typescript
// Before (old OpenAPI spec)
interface Adapter {
  adapterId: string  // ❌ Renamed
}

// After (new OpenAPI spec)
interface Adapter {
  id: string  // ✅ New field name
}

// Frontend code must update
const adapter: Adapter = {
  id: 'my-adapter',  // Fix: use new field name
  type: 'simulation',
  config: {},
}
```

TypeScript will show errors at compile time, ensuring all usages are updated.

### Step 4: Update Tests

Regenerated types may affect test mocks:

```typescript
// src/__test-utils__/adapters/simulation.ts
export const MOCK_ADAPTER: Adapter = {
  id: 'simulation-1',  // Update to match new interface
  type: 'simulation',
  config: { /* ... */ },
}
```

### Step 5: Verify Build

```bash
pnpm build:tsc     # Type check only
pnpm build         # Full build
```

---

## Customization

### Custom HTTP Client

If you need to replace axios with a different HTTP client:

1. **Implement `BaseHttpRequest` interface**

```typescript
// src/api/custom/FetchHttpRequest.ts
import { BaseHttpRequest } from '../__generated__/core/BaseHttpRequest'
import type { ApiRequestOptions } from '../__generated__/core/ApiRequestOptions'

export class FetchHttpRequest extends BaseHttpRequest {
  public override request<T>(options: ApiRequestOptions): Promise<T> {
    return fetch(/* ... */)
      .then(response => response.json())
  }
}
```

2. **Pass custom client to HiveMqClient**

```typescript
import { HiveMqClient } from '@/api/__generated__'
import { FetchHttpRequest } from '@/api/custom/FetchHttpRequest'

const client = new HiveMqClient(
  { BASE: '/api' },
  FetchHttpRequest  // Custom HTTP client
)
```

### Interceptors

Add request/response interceptors via axios config:

```typescript
import axios from 'axios'

// Configure axios instance before HiveMqClient instantiation
axios.interceptors.request.use((config) => {
  console.log('Request:', config)
  return config
})

axios.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)
```

---

## Troubleshooting

### Issue 1: Generated Client Not Found

**Error:**

```
Cannot find module '@/api/__generated__/HiveMqClient'
```

**Solution:**

```bash
# Regenerate client
pnpm dev:openAPI

# If error persists, check OpenAPI spec path
ls -la ../hivemq-edge-openapi/dist/bundle.yaml
```

---

### Issue 2: TypeScript Errors After Regeneration

**Error:**

```
Property 'adapterId' does not exist on type 'Adapter'
```

**Solution:**

This is expected when the backend OpenAPI spec has breaking changes. Update all references:

```bash
# Find all usages
grep -r "adapterId" src/

# Update to new field name
sed -i 's/adapterId/id/g' src/**/*.ts
```

---

### Issue 3: Schema Not Exported

**Error:**

```
Cannot find module '@/api/__generated__/schemas/$MyModel'
```

**Solution:**

Ensure `--exportSchemas true` is in the generation script:

```bash
# Verify script in package.json
grep "dev:openAPI" package.json

# Should include: --exportSchemas true
```

If missing, add it and regenerate:

```bash
pnpm dev:openAPI
```

---

### Issue 4: Service Method Missing

**Error:**

```
Property 'myNewEndpoint' does not exist on type 'ProtocolAdaptersService'
```

**Solution:**

The backend OpenAPI spec may not include the endpoint. Verify:

1. Check if endpoint exists in backend OpenAPI spec:

```bash
grep "myNewEndpoint" ../hivemq-edge-openapi/dist/bundle.yaml
```

2. If missing, coordinate with backend team to add endpoint to spec

3. Once added, regenerate client:

```bash
pnpm dev:openAPI
```

---

## Generated vs Custom Code

### ✅ Generated (Do Not Edit)

**Everything in `src/api/__generated__/`:**

- **Never manually edit** generated files
- Changes will be overwritten on next `pnpm dev:openAPI`
- Add `/* generated using openapi-typescript-codegen -- do no edit */` header to all generated files

### ✅ Custom (Edit Freely)

**Everything in `src/api/hooks/`:**

- Custom React Query hooks wrapping generated client
- Business logic and transformations
- Error handling and caching strategies
- Safe to edit and customize

**Example structure:**

```
src/api/
├── __generated__/        ❌ DO NOT EDIT (auto-generated)
│   ├── models/
│   ├── services/
│   └── HiveMqClient.ts
└── hooks/                ✅ EDIT FREELY (custom code)
    ├── useProtocolAdapters/
    │   ├── useGetAllProtocolAdapters.ts
    │   ├── useCreateProtocolAdapter.ts
    │   └── __handlers__/  (MSW mocks)
    └── useHttpClient/
        └── useHttpClient.ts
```

---

## Migration Notes

### From openapi-typescript-codegen to @hey-api/openapi-ts

**Potential future migration:** The `openapi-typescript-codegen` package is in maintenance mode. Consider migrating to `@hey-api/openapi-ts` for future projects.

**Differences:**

| Feature | openapi-typescript-codegen | @hey-api/openapi-ts |
|---------|---------------------------|----------------------|
| Maintenance | Low (maintenance mode) | Active development |
| Syntax | Class-based services | Functional API |
| Tree shaking | Limited | Better |
| Bundle size | Larger | Smaller |

**Migration blockers:**

- Current integration with RJSF requires exported schemas
- React Query hooks are tightly coupled to class-based services
- Significant refactor required (~50+ hooks)

**Recommendation:** Evaluate migration during major version upgrade.

---

## Best Practices

### ✅ Do

- **Regenerate after every OpenAPI spec update**
- **Commit generated code** to version control (ensures consistency across team)
- **Use `useHttpClient()` hook** instead of instantiating `HiveMqClient` directly
- **Wrap generated services** in custom React Query hooks
- **Export and reuse** TypeScript interfaces from `@/api/__generated__/models`

### ❌ Don't

- **Never edit files in `src/api/__generated__/`** (changes will be lost)
- **Don't bypass TypeScript errors** after regeneration (fix breaking changes)
- **Don't commit OpenAPI spec changes** without regenerating client
- **Don't instantiate client without configuration** (use `useHttpClient` hook)

---

## Checklist: Adding a New Endpoint

- [ ] Backend adds endpoint to OpenAPI spec
- [ ] OpenAPI spec updated in `hivemq-edge-openapi/dist/bundle.yaml`
- [ ] Run `pnpm dev:openAPI` to regenerate client
- [ ] Create custom React Query hook in `src/api/hooks/use{Feature}/use{Action}.ts`
- [ ] Add MSW handler in `src/api/hooks/use{Feature}/__handlers__/index.ts`
- [ ] Write tests for custom hook
- [ ] Update `QUERY_KEYS` in `src/api/utils.ts` if needed
- [ ] Document new endpoint usage if complex

---

## Glossary

| Term | Definition |
|------|------------|
| **OpenAPI** | A specification format for describing RESTful APIs in a machine-readable way (formerly Swagger) |
| **Code Generation** | Automated process of creating TypeScript client code from OpenAPI specification |
| **HiveMqClient** | Generated TypeScript class that provides access to all backend API services |
| **Service** | A class in the generated client corresponding to an OpenAPI tag, containing related endpoint methods |
| **Model** | TypeScript interface generated from OpenAPI schema definitions |
| **JSON Schema** | A vocabulary for validating the structure of JSON data, exported for RJSF forms |
| **RJSF** | React JSON Schema Form - library for generating forms from JSON schemas |
| **openapi-typescript-codegen** | NPM package that generates TypeScript client code from OpenAPI specifications |
| **BaseHttpRequest** | Abstract class in generated client that defines HTTP request interface |
| **useHttpClient** | Custom React hook that provides configured HiveMqClient instance with auth and base URL |

---

## Related Documentation

**API:**
- [React Query Patterns](./REACT_QUERY_PATTERNS.md)
- [MSW API Mocking](./MSW_MOCKING.md)

**Architecture:**
- [Protocol Adapter Architecture](../architecture/PROTOCOL_ADAPTER_ARCHITECTURE.md)

**Technical:**
- [Technical Stack](../technical/TECHNICAL_STACK.md)
