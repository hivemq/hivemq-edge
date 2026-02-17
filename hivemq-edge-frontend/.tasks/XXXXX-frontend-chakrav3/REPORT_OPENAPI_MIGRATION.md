# OpenAPI Code Generator Migration Analysis

## Current State

### Package Details

- **Current Package**: `openapi-typescript-codegen` v0.25.0
- **Status**: Deprecated (archived on GitHub)
- **Last Release**: June 2023
- **License**: MIT

### Generation Configuration

```shell
# package.json script
"dev:openAPI": "openapi --input '../hivemq-edge-openapi/dist/bundle.yaml' -o ./src/api/__generated__ -c axios --name HiveMqClient --exportSchemas true"
```

### Generated Output Structure

```
src/api/__generated__/
├── HiveMqClient.ts              # Main client class (class-based API)
├── index.ts                      # Barrel exports
├── core/                         # HTTP infrastructure (8 files)
│   ├── ApiError.ts              # Error class
│   ├── ApiRequestOptions.ts     # Request options type
│   ├── ApiResult.ts             # Response wrapper
│   ├── AxiosHttpRequest.ts      # Axios implementation
│   ├── BaseHttpRequest.ts       # Base class
│   ├── CancelablePromise.ts     # Cancellation support
│   ├── OpenAPI.ts               # Config type
│   └── request.ts               # Core request logic
├── models/                       # TypeScript interfaces (~100 files)
├── schemas/                      # JSON Schema exports (~100 files)
└── services/                     # Service classes (~22 files)
```

### Service Classes (22 total)

| Service                        | Methods | Description           |
| :----------------------------- | :------ | :-------------------- |
| AuthenticationService          | 1       | Login                 |
| AuthenticationEndpointService  | 1       | Pre-login notice      |
| BridgesService                 | 6       | Bridge CRUD \+ status |
| CombinersService               | 7       | Combiner management   |
| DataHubBehaviorPoliciesService | 5       | Behavior policies     |
| DataHubDataPoliciesService     | 5       | Data policies         |
| DataHubFsmService              | 1       | FSM states            |
| DataHubFunctionsService        | 2       | Functions             |
| DataHubInterpolationService    | 1       | Interpolation         |
| DataHubSchemasService          | 4       | Schemas               |
| DataHubScriptsService          | 4       | Scripts               |
| DataHubStateService            | 1       | Client state          |
| DefaultService                 | 1       | Default endpoint      |
| EventsService                  | 1       | Event log             |
| FrontendService                | 3       | Frontend config       |
| GatewayEndpointService         | 1       | Gateway info          |
| HealthCheckEndpointService     | 1       | Health check          |
| MetricsService                 | 1       | Metrics               |
| MetricsEndpointService         | 1       | Metrics endpoint      |
| PayloadSamplingService         | 4       | Payload sampling      |
| ProtocolAdaptersService        | 15+     | Adapter management    |
| PulseService                   | 6       | Asset monitoring      |
| TopicFiltersService            | 5       | Topic filters         |
| UnsService                     | 2       | Unified namespace     |

---

## Current Usage Patterns

### Client Initialization

```ts
// src/api/hooks/useHttpClient/useHttpClient.ts
import { HiveMqClient } from '@/api/__generated__'

export const useHttpClient = () => {
  const { base } = useAuth()

  return useMemo(() => {
    return new HiveMqClient({
      BASE: base,
      // ... other config
    })
  }, [base])
}
```

### React Query Integration (Current)

```ts
// Typical pattern in custom hooks
import { useQuery } from '@tanstack/react-query'
import { useHttpClient } from '@/api/hooks/useHttpClient/useHttpClient.ts'
import type { ApiError, Bridge } from '@/api/__generated__'

export const useListBridges = () => {
  const appClient = useHttpClient()

  return useQuery<Bridge[] | undefined, ApiError>({
    queryKey: [QUERY_KEYS.BRIDGES],
    queryFn: async () => {
      const { items } = await appClient.bridges.getBridges()
      return items
    },
  })
}
```

### Type Imports

```ts
// Types are imported from generated code
import type { Bridge, Adapter, DataPolicy, ApiError, ProblemDetails } from '@/api/__generated__'
```

---

## Migration Candidates

### Option 1: @hey-api/openapi-ts (Recommended)

**Repository**: [https://github.com/hey-api/openapi-ts](https://github.com/hey-api/openapi-ts)  
**Stars**: 1.5k+  
**Last Update**: Active

**Output Structure**:

```ts
// Functional approach - no class instantiation
import { getBridges } from '@/api/services/BridgesService'
import type { Bridge } from '@/api/types'

// Direct function call
const bridges = await getBridges()

// Or with React Query plugin
import { useGetBridges } from '@/api/queries'
const { data } = useGetBridges()
```

**Pros**:

- ✅ Active development
- ✅ Tree-shakeable output
- ✅ Native React Query support (@hey-api/openapi-ts-tanstack-query)
- ✅ Supports Problem Detail (RFC 7807\)
- ✅ TypeScript-first
- ✅ Axios, Fetch, or native fetch support
- ✅ Better error typing

**Cons**:

- ⚠️ Different paradigm (functions vs class)
- ⚠️ Requires refactoring all \~100 custom hooks
- ⚠️ Learning curve for new patterns

**Configuration Example**:

```ts
// openapi-ts.config.ts
import { defineConfig } from '@hey-api/openapi-ts'

export default defineConfig({
  client: '@hey-api/client-axios',
  input: '../hivemq-edge-openapi/dist/bundle.yaml',
  output: 'src/api/generated',
  plugins: [
    '@hey-api/typescript',
    '@hey-api/schemas',
    {
      name: '@hey-api/tanstack-query',
      exportInfiniteQueries: true,
    },
  ],
})
```

### Option 2: @fabien0102/openapi-codegen

**Repository**: [https://github.com/fabien0102/openapi-codegen](https://github.com/fabien0102/openapi-codegen)  
**Stars**: 600+

**Output**: Generates React Query hooks directly

**Pros**:

- ✅ React Query native
- ✅ Less boilerplate
- ✅ Good for simple use cases

**Cons**:

- ⚠️ Less customization options
- ⚠️ Smaller community
- ⚠️ May not fit all patterns used

### Option 3: openapi-typescript \+ openapi-fetch

**Repository**: [https://github.com/drwpow/openapi-typescript](https://github.com/drwpow/openapi-typescript)  
**Stars**: 5k+

**Pros**:

- ✅ Very popular, well-maintained
- ✅ Type-safe fetch wrapper
- ✅ Minimal runtime overhead

**Cons**:

- ⚠️ Different approach (fetch-based)
- ⚠️ Requires switching from Axios

---

## Migration Impact Assessment

### Files Requiring Direct Changes

| Category          | Count  | Changes Needed          |
| :---------------- | :----- | :---------------------- |
| Custom API hooks  | \~100  | Rewrite query functions |
| DataHub API hooks | \~45   | Rewrite query functions |
| Type imports      | \~200+ | Update import paths     |
| Tests (unit)      | \~50+  | Update mocks            |
| Tests (component) | \~100+ | Update intercepts       |
| MSW handlers      | \~30+  | Update type references  |

### Import Statement Updates

Current pattern:

```ts
import { HiveMqClient } from '@/api/__generated__'
import type { Bridge, ApiError } from '@/api/__generated__'
```

New pattern (hey-api):

```ts
import { getBridges, type Bridge } from '@/api/generated'
// OR with React Query plugin
import { useGetBridges } from '@/api/generated/queries'
```

### Error Handling Changes

Current:

```ts
import { ApiError } from '@/api/__generated__'

try {
  await appClient.bridges.createBridge(data)
} catch (error) {
  if (error instanceof ApiError) {
    // Handle error
  }
}
```

New (hey-api with Problem Detail):

```ts
import type { ProblemDetails } from '@/api/generated/types'

const { error, data } = await createBridge({ body: data })
if (error) {
  const problemDetail = error as ProblemDetails
  // Better structured error handling
}
```

---

## Known Limitations to Address

### Current Issues

1. **Discriminated Unions** (from code comment):

```ts
// src/modules/TopicFilters/hooks/useTopicFilterManager.ts:31
// TODO[24980] This is due to limitation of the openapi-typescript-codegen library
```

2. **Error Response Types**: Current generator doesn't properly type error responses as Problem Detail

3. **Query Key Management**: Manual query key definitions needed

### Improvements with hey-api

1. **Better discriminated union support**
2. **Native Problem Detail (RFC 7807\) support**
3. **Automatic query key generation** (with React Query plugin)
4. **Tree-shaking** reduces bundle size

---

## Migration Strategy

### Phase 1: Infrastructure (1-2 weeks)

1. Install and configure @hey-api/openapi-ts
2. Generate new API code in parallel (`src/api/v2/`)
3. Create adapter layer for gradual migration
4. Update error types for Problem Detail

### Phase 2: Core Services (2-3 weeks)

1. Migrate Bridges hooks
2. Migrate Protocol Adapters hooks
3. Migrate Authentication hooks
4. Update related tests

### Phase 3: DataHub Services (2-3 weeks)

1. Migrate DataHub API hooks
2. Update DataHub components
3. Test policy editor flows

### Phase 4: Remaining Services (1-2 weeks)

1. Migrate all remaining services
2. Remove old generated code
3. Update documentation

### Phase 5: Cleanup (1 week)

1. Remove adapter layer
2. Final testing
3. Update CI/CD

---

## Recommendations

1. **Choose @hey-api/openapi-ts** as the replacement generator
2. **Use the React Query plugin** to reduce custom hook code
3. **Implement Problem Detail** error handling from the start
4. **Migrate incrementally** with parallel code during transition
5. **Maintain backwards compatibility** with adapter pattern initially

---

## References

- [@hey-api/openapi-ts Documentation](https://heyapi.vercel.app/)
- [RFC 7807 \- Problem Details for HTTP APIs](https://tools.ietf.org/html/rfc7807)
- [TanStack Query Integration](https://heyapi.vercel.app/openapi-ts/plugins/tanstack-query)
