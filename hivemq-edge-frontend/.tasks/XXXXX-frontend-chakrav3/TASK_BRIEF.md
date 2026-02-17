# Task Brief: Chakra UI v2 to v3 Migration

**Task ID**: XXXXX-frontend-chakrav3  
**Created**: December 12, 2025  
**Status**: Investigation Phase

## Objective

Prepare and execute the migration of the HiveMQ Edge frontend from Chakra UI v2 (2.8.2) to Chakra UI v3.

## Scope of Investigation

This document contains structured reports on three key areas that will affect the migration complexity:

1. **Feature Decomposition**: Workspace vs DataHub vs Core App
2. **OpenAPI Code Generation**: Migration from deprecated `openapi-typescript-codegen`
3. **RJSF (React JSON Schema Form)**: Custom component migration

---

## Quick Summary

| Area                       | Estimated Impact                | Priority |
| :------------------------- | :------------------------------ | :------- |
| **Workspace Module**       | \~200 files, 11% of codebase    | High     |
| **DataHub Extension**      | \~321 files, 20% of codebase    | High     |
| **OpenAPI Generated Code** | \~210+ files, complete rewrite  | Critical |
| **RJSF Custom Components** | \~100+ files, major refactoring | Critical |
| **Core App (rest)**        | \~1,095 files, 69% of codebase  | Medium   |

---

## Report 1: Workspace vs DataHub vs Core App Distribution

### Overall Codebase Metrics

- **Total Source Files**: 1,616 TypeScript/TSX files in `src/`
- **Generated Code**: \~210 files in `src/api/__generated__/` (auto-generated, not counted in custom code)

### Workspace Module

**Location**: `src/modules/Workspace/`

| Metric           | Count                                            |
| :--------------- | :----------------------------------------------- |
| Total Files      | \~200                                            |
| React Components | \~60 TSX files                                   |
| Hooks            | \~21 files                                       |
| Utils            | \~34 files                                       |
| Types            | \~5 files                                        |
| Tests (Vitest)   | \~45 `.spec.ts` files                            |
| Tests (Cypress)  | \~50+ `.spec.cy.tsx` component tests             |
| E2E Tests        | 12 Cypress E2E specs in `cypress/e2e/workspace/` |

**Key Subfolders**:

- `components/controls/` \- Canvas toolbar, controls (6 files)
- `components/nodes/` \- Custom React Flow nodes (15+ files)
- `components/edges/` \- Custom React Flow edges (6 files)
- `components/drawers/` \- Property drawers (9 files)
- `components/wizard/` \- Entity creation wizard (20+ files)
- `components/filters/` \- Filter components (14 files)
- `components/layout/` \- Layout management (6 files)
- `hooks/` \- Zustand stores, React hooks (21 files)
- `utils/layout/` \- Layout algorithms (12 files)

**Chakra UI Dependencies**:

- Heavy use of `Drawer`, `Box`, `Flex`, `Button`, `IconButton`
- Uses Chakra's form components
- Custom theme colors for status indicators
- React Flow integration with Chakra styling

### DataHub Extension

**Location**: `src/extensions/datahub/`

| Metric           | Count                                         |
| :--------------- | :-------------------------------------------- |
| Total Files      | 321                                           |
| React Components | \~120 TSX files                               |
| Designer Nodes   | \~50 files                                    |
| API Hooks        | \~45 files                                    |
| Utils            | \~25 files                                    |
| Tests (Vitest)   | \~40 `.spec.ts` files                         |
| Tests (Cypress)  | \~80+ `.spec.cy.tsx` component tests          |
| E2E Tests        | 3 Cypress E2E specs in `cypress/e2e/datahub/` |

**Key Subfolders**:

- `designer/` \- Policy designer nodes (behavior_policy, client_filter, data_policy, operation, schema, script, topic_filter, transition, validator)
- `components/controls/` \- Designer toolbox, minimap (15+ files)
- `components/forms/` \- Custom form components, Monaco integration (20+ files)
- `components/pages/` \- Page components (10 files)
- `components/toolbar/` \- Policy toolbar (8 files)
- `components/helpers/` \- Helper components (18 files)
- `components/nodes/` \- Base node components (7 files)
- `api/hooks/` \- Service-specific hooks (45+ files)
- `hooks/` \- Draft store, policy checks (10 files)

**Chakra UI Dependencies**:

- Complex form layouts with RJSF integration
- Custom widgets using Chakra components
- Drawer-based property panels
- Monaco editor with Chakra theming
- Toast notifications for API errors

### Core Application (Rest of Codebase)

**Locations**: `src/modules/*` (excluding Workspace), `src/components/`, `src/hooks/`, `src/api/hooks/`

| Area                     | Files  | Description                     |
| :----------------------- | :----- | :------------------------------ |
| Protocol Adapters        | \~60   | Adapter management UI           |
| Bridges                  | \~50   | MQTT Bridge configuration       |
| Mappings                 | \~40   | Data mapping UI                 |
| Pulse (Asset Monitoring) | \~45   | Asset management                |
| DomainOntology           | \~35   | Domain visualization charts     |
| Auth                     | \~15   | Authentication flow             |
| Dashboard                | \~20   | Navigation, side panel          |
| Theme                    | \~15   | Chakra theme customization      |
| EventLog                 | \~15   | Event log display               |
| Metrics                  | \~25   | Metrics visualization           |
| Shared Components        | \~80   | Reusable UI components          |
| RJSF Components          | \~100+ | JSON Schema Form customizations |
| API Hooks                | \~100+ | React Query hooks               |

### Test Distribution by Feature

**Cypress E2E Tests** (77 files in `cypress/`):

| Feature    | E2E Specs                     |
| :--------- | :---------------------------- |
| Workspace  | 12                            |
| DataHub    | 3                             |
| Adapters   | 12                            |
| Bridges    | 1                             |
| Pulse      | 2                             |
| Login      | 2                             |
| Event Log  | 1                             |
| Mappings   | 1                             |
| Supporting | \~43 (pages, utils, commands) |

**Component Tests** (`.spec.cy.tsx` files):

- Workspace: \~50 files
- DataHub: \~80 files
- RJSF: \~30 files
- Shared components: \~60 files

---

## Report 2: OpenAPI Code Generation Migration

### Current State

**Package**: `openapi-typescript-codegen` v0.25.0 (deprecated)

**Command**:

```shell
pnpm dev:openAPI
# Runs: openapi --input '../hivemq-edge-openapi/dist/bundle.yaml' -o ./src/api/__generated__ -c axios --name HiveMqClient --exportSchemas true
```

**Generated Structure** (`src/api/__generated__/`):

```
__generated__/
├── HiveMqClient.ts         # Main client class
├── index.ts                # Barrel exports
├── core/                   # HTTP request infrastructure
│   ├── ApiError.ts
│   ├── ApiRequestOptions.ts
│   ├── ApiResult.ts
│   ├── AxiosHttpRequest.ts
│   ├── BaseHttpRequest.ts
│   ├── CancelablePromise.ts
│   ├── OpenAPI.ts
│   └── request.ts
├── models/                 # TypeScript interfaces (~100+ files)
│   ├── Adapter.ts
│   ├── Bridge.ts
│   ├── DataPolicy.ts
│   └── ... (100+ model files)
├── schemas/                # JSON schemas for models (~100+ files)
│   ├── $Adapter.ts
│   ├── $Bridge.ts
│   └── ... (100+ schema files)
└── services/               # API service classes (~20+ files)
    ├── AuthenticationService.ts
    ├── BridgesService.ts
    ├── DataHubDataPoliciesService.ts
    └── ... (20+ service files)
```

### Usage Pattern

The generated code follows this pattern:

```ts
// src/api/hooks/useHttpClient/useHttpClient.ts
import { HiveMqClient } from '@/api/__generated__'

// Custom hook wraps the generated client
export const useHttpClient = () => {
  // Returns configured HiveMqClient instance
}

// src/api/hooks/useGetBridges/useListBridges.ts
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

### Migration Options

#### Option A: `@hey-api/openapi-ts`

**Pros**:

- Active development, modern tooling
- Supports Axios, Fetch, and native fetch
- Tree-shakeable output
- Built-in React Query integration option
- Good Problem Detail (RFC 7807\) support

**Cons**:

- Different API structure (functional vs class-based)
- Requires significant hook refactoring

#### Option B: `fabien0102/openapi-codegen`

**Pros**:

- React Query first approach
- Generates hooks directly

**Cons**:

- Less control over customization
- Different paradigm

### Migration Impact

| Category              | Impact                                                |
| :-------------------- | :---------------------------------------------------- |
| **Generated Files**   | Complete replacement (\~210 files)                    |
| **Custom API Hooks**  | Refactor all \~100+ hooks in `src/api/hooks/`         |
| **Import Statements** | Update all files importing from `@/api/__generated__` |
| **Type Names**        | Potential type name changes (check case conventions)  |
| **Error Handling**    | Update `ApiError` handling for Problem Detail support |
| **Tests**             | Update all API-related tests                          |

### Files Requiring Updates

**Direct imports from generated code** (estimated):

- `src/api/hooks/**/*.ts` \- \~100+ files
- `src/extensions/datahub/api/**/*.ts` \- \~45 files
- `src/modules/**/*.ts` \- \~50+ files with type imports
- `cypress/**/*.ts` \- Test utilities using types

### Known Limitations of Current Generator

From codebase search:

```ts
// src/modules/TopicFilters/hooks/useTopicFilterManager.ts:31
// TODO[24980] This is due to limitation of the openapi-typescript-codegen library
```

This indicates existing pain points that a new generator might resolve.

---

## Report 3: RJSF Custom Components

### Current RJSF Setup

**Packages**:

```json
"@rjsf/chakra-ui": "5.24.13",
"@rjsf/core": "5.24.13",
"@rjsf/utils": "5.24.13",
"@rjsf/validator-ajv8": "5.24.13"
```

### RJSF Integration Points

**Main Form Component**: `src/components/rjsf/Form/ChakraRJSForm.tsx`

**Import locations** (from `@rjsf/chakra-ui`):

1. `src/components/rjsf/Form/ChakraRJSForm.tsx`
2. `src/extensions/datahub/components/forms/ReactFlowSchemaForm.tsx`
3. `src/extensions/datahub/components/forms/CodeEditor.tsx`
4. `src/modules/DomainOntology/components/cluster/ConfigurationPanel.tsx`
5. `src/__test-utils__/rjsf/rjsf.mocks.tsx`
6. `src/__test-utils__/rjsf/CustomFormTesting.tsx`

### Custom RJSF Components Inventory

**Location**: `src/components/rjsf/` (100+ files)

#### Templates (9 files)

| File                                          | Purpose                      |
| :-------------------------------------------- | :--------------------------- |
| `ArrayFieldTemplate.tsx`                      | Custom array field rendering |
| `ArrayFieldItemTemplate.tsx`                  | Array item with drag-drop    |
| `BaseInputTemplate.tsx`                       | Custom input wrapper         |
| `FieldTemplate.tsx`                           | Field wrapper with labels    |
| `ObjectFieldTemplate.tsx`                     | Object field layout          |
| `Templates/CompactArrayFieldItemTemplate.tsx` | Compact array items          |
| `Templates/CompactArrayFieldTemplate.tsx`     | Compact array layout         |
| `Templates/CompactBaseInputTemplate.tsx`      | Compact input                |
| `Templates/CompactFieldTemplate.tsx`          | Compact field wrapper        |
| `Templates/CompactObjectFieldTemplate.tsx`    | Compact object layout        |
| `Templates/DescriptionFieldTemplate.tsx`      | Description rendering        |
| `Templates/ErrorListTemplate.tsx`             | Error list display           |
| `Templates/TitleFieldTemplate.tsx`            | Title rendering              |

#### Custom Widgets (8+ files)

| File                             | Purpose            | Chakra Components Used |
| :------------------------------- | :----------------- | :--------------------- |
| `Widgets/AdapterTagSelect.tsx`   | Tag tree selection | Tree view, Select      |
| `Widgets/EntitySelectWidget.tsx` | Entity selection   | Select, Modal          |
| `Widgets/SchemaWidget.tsx`       | Schema selection   | Select, Button         |
| `Widgets/ToggleWidget.tsx`       | Toggle input       | Switch                 |
| `Widgets/UpDownWidget.tsx`       | Number input       | NumberInput            |

#### Custom Fields (5+ files)

| File                                 | Purpose                 |
| :----------------------------------- | :---------------------- |
| `Fields/CompactArrayField.tsx`       | Compact array handling  |
| `Fields/InternalNotice.tsx`          | Internal notice display |
| `Fields/MqttTransformationField.tsx` | MQTT transformation     |

#### Internal Components (6 files)

| File                                  | Purpose                 |
| :------------------------------------ | :---------------------- |
| `__internals/AddButton.tsx`           | Add array item button   |
| `__internals/ChakraIconButton.tsx`    | Icon button wrapper     |
| `__internals/IconButton.tsx`          | Generic icon button     |
| `__internals/RenderFieldTemplate.tsx` | Field template renderer |
| `__internals/TopicInputTemplate.tsx`  | Topic input template    |

#### Form Infrastructure (8 files)

| File                          | Purpose               |
| :---------------------------- | :-------------------- |
| `Form/ChakraRJSForm.tsx`      | Main form component   |
| `Form/error-focus.utils.ts`   | Error focus handling  |
| `Form/types.ts`               | Type definitions      |
| `Form/useFormControlStore.ts` | Form state management |
| `Form/validation.utils.ts`    | Validation utilities  |

#### Complex Features (40+ files)

**Batch Mode Mappings** (`BatchModeMappings/`):

- `BatchUploadButton.tsx`
- `components/ColumnMatcherStep.tsx`
- `components/ConfirmStep.tsx`
- `components/DataSourceStep.tsx`
- `components/MappingsValidationStep.tsx`
- `components/UploadStepper.tsx`
- `hooks/useBatchModeSteps.ts`
- `utils/` \- Various utilities

**MQTT Transformation** (`MqttTransformation/`):

- `JsonSchemaBrowser.tsx`
- `components/DataModelDestination.tsx`
- `components/DataModelSources.tsx`
- `components/EntitySelector.tsx`
- `components/ListMappings.tsx`
- `components/MappingContainer.tsx`
- `components/MappingDrawer.tsx`
- `components/MappingEditor.tsx`
- `components/MappingInstructionList.tsx`
- `components/mapping/MappingInstruction.tsx`
- `components/mapping/ValidationStatus.tsx`
- `components/schema/PropertyItem.tsx`

**Split Array Editor** (`SplitArrayEditor/`):

- `components/ArrayItemDrawer.tsx`

### DataHub-Specific RJSF Components

**Location**: `src/extensions/datahub/`

| File                                                | Purpose                         |
| :-------------------------------------------------- | :------------------------------ |
| `designer/datahubRJSFWidgets.tsx`                   | DataHub-specific widgets        |
| `components/forms/ReactFlowSchemaForm.tsx`          | React Flow integrated form      |
| `components/forms/CodeEditor.tsx`                   | Monaco-based code editor widget |
| `components/forms/FunctionCreatableSelect.tsx`      | Function selector               |
| `components/forms/MessageInterpolationTextArea.tsx` | Interpolation editor            |
| `components/forms/MessageTypeSelect.tsx`            | Message type selector           |
| `components/forms/MetricCounterInput.tsx`           | Metric counter                  |
| `components/forms/ResourceNameCreatableSelect.tsx`  | Resource name selector          |
| `components/forms/TransitionSelect.tsx`             | FSM transition selector         |
| `components/forms/VersionManagerSelect.tsx`         | Version manager                 |
| `components/forms/AdapterSelect.tsx`                | Adapter selector                |

### Test Coverage for RJSF

**Component Tests** (`.spec.cy.tsx`):

- `src/components/rjsf/` \- \~30 test files
- `src/extensions/datahub/components/forms/` \- \~15 test files

### Chakra UI v3 Impact on RJSF

The `@rjsf/chakra-ui` package will need to release a v3-compatible version. Key concerns:

1. **Breaking Changes in Chakra v3**:

- Component API changes (props, styling)
- Theme structure changes
- Color mode handling

2. **Custom Template Migration**:

- All 13+ template files need updating
- Theme token usage must be updated
- Styling approach changes

3. **Custom Widget Migration**:

- All 8+ widget files need updating
- Form control patterns may change
- Accessibility attributes may differ

4. **Internal Component Migration**:

- Button variants and styling
- Icon button patterns
- Form element styling

### Migration Strategy for RJSF

1. **Wait for Official Support**: Monitor `@rjsf/chakra-ui` for v3 release
2. **Parallel Development**: Fork and update if official support is delayed
3. **Staged Migration**:

- Phase 1: Core templates
- Phase 2: Standard widgets
- Phase 3: Complex features (BatchMode, MqttTransformation)
- Phase 4: DataHub-specific components

---

## Appendix: Key Files Index

### Theme Files (Chakra-specific)

```
src/modules/Theme/
├── themeHiveMQ.ts          # Main theme configuration
├── utils.ts                # Theme utilities
├── foundations/
│   └── colors.ts           # Color definitions
├── components/             # Component style overrides
│   ├── Alert.ts
│   ├── Button.ts
│   ├── Drawer.ts
│   ├── FormControl.ts
│   ├── FormErrorMessage.ts
│   ├── Spinner.ts
│   └── Stat.ts
└── globals/                # Global style overrides
    ├── react-flow.ts
    └── treeview.ts
```

### Shared Chakra Components

```
src/components/Chakra/
├── ButtonBadge.tsx
├── ClipboardCopyIconButton.tsx
├── ColorPicker.tsx
├── DrawerExpandButton.tsx
├── FormLabel.tsx
├── IconButton.tsx
├── LoaderSpinner.tsx
├── RadioButton.tsx
├── ShortcutRenderer.tsx
├── SwitchModeButton.tsx
├── TooltipBadge.tsx
└── TooltipIcon.tsx
```

---

## Next Steps

- [ ] Create detailed migration plan with phases
- [ ] Identify Chakra v2 → v3 breaking changes applicable to this codebase
- [ ] Evaluate OpenAPI generator options with POC
- [ ] Monitor `@rjsf/chakra-ui` for v3 support timeline
- [ ] Create test coverage requirements for each phase
- [ ] Estimate effort for each migration phase
