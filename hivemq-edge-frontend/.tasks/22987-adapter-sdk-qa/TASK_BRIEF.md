# Task 22987: Adapter SDK QA

**Status:** Planning
**Created:** January 21, 2026
**Type:** Cross-Repository (TypeScript + Java)
**Master Repository:** hivemq-edge-frontend

---

## Overview

The context of the task is the configuration of protocol adapters in the Edge Frontend. Each adapter has its own specific
configuration schema, given to the frontend through API and rendered using the RJSF library.
The configuration comes under two documents:

- the `JSON-Schema` representing the logic of the configuration and the payload of any creation/modification request sent to the backend
- the `UI-schema` representing the visual customization of the form in the frontend

Recently, we performed a major quality analysis of every adapter configuration, looking at errors and potential mis-specification of the schemas.
You can find the methodology and the reports on task 38658.

Our task goal is to improve the `Developer Experience` of developing adapters, either new or existing ones.

### Objectives

- improve the documentation of the Protocol Adapter SDK with a better description of the JSON-Schema and the UI-Schema, in particular the use of custom format and validation
- Summarize the methodology of the quality analysis into an actionable checklist for developers
- Create a simple testing suite for the UI of an adapter, as part of the SDK

### Visual Testing of Adapters while in `dev`

This part of the proposal needs planning and discussion. A rough idea could be as follow:

- In the dev environment, create a simple local dev server that will expose the API of the adapter config (see the OpenAPI specs)
- Create a generic Cypress test suite that will server a simple React app containing the RJSF necessary to render the form, with all the customisation used in the frontend
- Create a suite of tests that will validate the rendering of the form in the frontend
  - check that the form is rendered correctly
  - check that the form has all properties defined in the JAVA code and in the JSON-Schema
  - check that the form has all customisation defined in the UI-Schema
  - check that the QA checklist is followed, as long as they can be operationalised

### For context:

- the SDK for creating adapters can be found here: https://github.com/hivemq/hivemq-edge-adapter-sdk
- An example of a `Hello World` adapter can be found here: https://github.com/hivemq/hivemq-hello-world-protocol-adapter
- The OpenAPI specs for Edge can be found here: https://github.com/hivemq/hivemq-edge/tree/master/hivemq-edge-openapi
- The frontend API to retrieve the specs of adapters can be found here: `src/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts`
- The main UI component for rendering the config form of an adapter can be found here: `src/modules/ProtocolAdapters/components/drawers/AdapterInstanceDrawer.tsx`

---

## Repositories Involved

| Repository                          | Language   | Role                                       |
| ----------------------------------- | ---------- | ------------------------------------------ |
| hivemq-edge-frontend                | TypeScript | Master/Coordination, Testing Suite         |
| hivemq-edge-adapter-sdk             | Java       | SDK Documentation                          |
| hivemq-hello-world-protocol-adapter | Java       | Example Adapter, Testing Target            |
| hivemq-edge                         | Java       | OpenAPI Specs, Core Adapter Infrastructure |

---

## Requirements

### 1. SDK Documentation Improvements

Update the Protocol Adapter SDK documentation with:

- **JSON Schema patterns** - How `@ModuleConfigField` annotations map to JSON Schema properties
- **UI Schema patterns** - How to structure UI schemas for proper rendering in RJSF
- **Custom formats** - Available format types (IDENTIFIER, HOSTNAME, URI, PASSWORD)
- **Validation constraints** - Using `numberMin/Max`, `stringMinLength/MaxLength`, `stringPattern`
- **Conditional visibility** - Using JSON Schema `dependencies` for field relationships
- **Enum display names** - Using `enumDisplayValues` for user-friendly labels

### 2. Developer QA Checklist

Create an actionable checklist derived from task 38658 methodology:

**JSON Schema Checklist:**

- [ ] All `@ModuleConfigField` annotations have `title` and `description`
- [ ] Titles are properly capitalized (not camelCase)
- [ ] Descriptions are grammatically correct US-EN
- [ ] Numeric fields use `numberMin`/`numberMax` (NOT string constraints)
- [ ] String fields with patterns use `stringPattern`
- [ ] Required fields have BOTH `@JsonProperty(required = true)` AND `@ModuleConfigField(required = true)`
- [ ] Enum fields have matching `enum` and `enumNames` counts
- [ ] Boolean toggle fields have `dependencies` for conditional sub-fields
- [ ] Cross-field validations are documented
- [ ] Getter methods return correct field values

**UI Schema Checklist:**

- [ ] UI schema has appropriate tabs for field grouping
- [ ] `ui:order` array defines logical field sequence
- [ ] `updown` widget for bounded numeric fields (ports, intervals)
- [ ] `password` widget for sensitive fields
- [ ] `textarea` for multi-line content
- [ ] `ui:disabled` handled dynamically (not hardcoded)
- [ ] `ui:batchMode` for array mappings
- [ ] `ui:collapsable` with `titleKey` for expandable items

### 3. Visual Testing Suite

Create a **self-contained testing framework** that lives in the SDK repo:

**Key Constraint:** Java developers won't have access to the Edge frontend during adapter development. The test suite must be:

- Located entirely within the SDK repository
- Familiar to Java developers (npm/node as only external dependency)
- Simple to run: `npm install` → `npm test` (or equivalent)
- No knowledge of the frontend codebase required

**Components:**

- Pre-built React app bundle with RJSF and all frontend customizations (widgets, validators, templates)
- Simple HTTP server to serve the test app and receive adapter schemas
- Cypress test suite with generic adapter form tests
- CLI wrapper for easy execution

**Developer Workflow:**

```bash
# From adapter project root
cd testing/ui
npm install        # One-time setup
npm test           # Run visual tests against local adapter
```

**Test Coverage:**

- Form renders without errors
- All properties from JSON Schema are present in the form
- UI Schema customizations are applied (tabs, widgets, ordering)
- QA checklist items that can be automated (e.g., title casing, required fields)
- Accessibility testing (axe-core)

---

## Scope

### In Scope

- Protocol Adapter SDK documentation updates
- Developer QA checklist document
- Visual testing infrastructure design
- Cypress test suite for adapter form validation
- Example implementation using hello-world adapter

### Out of Scope

- Fixing existing adapter issues (covered by task 38658 remediation)
- Backend code changes to adapter modules
- New adapter development
- i18n/multi-language support

---

## Technical Context

### How Adapter Configuration Works

1. **Backend** defines config in Java using `@JsonProperty` and `@ModuleConfigField` annotations
2. Backend generates **JSON Schema** from annotations at runtime
3. Backend provides **UI Schema** from JSON resource files
4. **Frontend** receives schemas via `/api/v1/protocol-adapters/types` endpoint
5. Frontend renders form using **RJSF** (@rjsf/chakra-ui) with custom widgets/validators

### Key Frontend Components

```
src/components/rjsf/           # RJSF customizations
├── Form/ChakraRJSForm.tsx     # Main form component
├── Form/validation.utils.ts   # Custom format validators
├── Widgets/                   # Custom widgets (updown, password, etc.)
├── Fields/                    # Custom fields
└── Templates/                 # Custom templates (tabs, arrays)

src/modules/ProtocolAdapters/
├── components/drawers/AdapterInstanceDrawer.tsx  # Main adapter config drawer
└── utils/uiSchema.utils.ts    # UI schema processing
```

### SDK Annotation: @ModuleConfigField

Located in `com.hivemq.adapter.sdk.api.annotations.ModuleConfigField`:

```java
@ModuleConfigField(
    title = "Field Title",              // User-facing label
    description = "Help text",          // Field description
    format = FieldType.HOSTNAME,        // Format validation
    required = true,                    // Required field
    defaultValue = "default",           // Default value

    // Numeric validation
    numberMin = 1,
    numberMax = 65535,

    // String validation
    stringMinLength = 1,
    stringMaxLength = 1024,
    stringPattern = "^[a-zA-Z0-9]*$",

    // Array validation
    arrayMinItems = 1,
    arrayMaxItems = 100,
    arrayUniqueItems = true,

    // Enum display
    enumDisplayValues = {"Display 1", "Display 2"},

    // Access control
    readOnly = false,
    writeOnly = false
)
```

**FieldType enum values:**
`DATE`, `TIME`, `DATE_TIME`, `REGEX`, `EMAIL`, `HOSTNAME`, `IPV4`, `IPV6`,
`JSON_POINTER`, `URI`, `IDENTIFIER`, `BOOLEAN`, `MQTT_TOPIC`, `MQTT_TOPIC_FILTER`, `MQTT_TAG`

### Reference Documentation

- Task 38658 analysis: `.tasks/38658-adapter-jsonschema-review/`
- RJSF Guidelines: `.tasks/RJSF_GUIDELINES.md`
- Testing Guidelines: `.tasks/TESTING_GUIDELINES.md`

---

## Success Criteria

1. **Documentation** - SDK docs include comprehensive JSON Schema and UI Schema guide
2. **Checklist** - Developers have a single-page checklist for adapter QA
3. **Testing Suite** - Running `pnpm test:adapter-forms` validates form rendering
4. **Example** - hello-world adapter passes all automated tests
5. **Reproducible** - Any developer can run tests locally during adapter development

---

## Dependencies

- Access to hivemq-edge-adapter-sdk repository
- Access to hivemq-hello-world-protocol-adapter repository
- Task 38658 remediation (for understanding common issues)

---

## Notes

### Related Task 38658 Findings

The QA analysis identified 28 issues across adapters:

- 2 Critical (File tag schema wrong, Databases getter bug)
- 7 High (Invalid constraints, missing dependencies)
- 13 Medium (Title casing, grammar, missing tabs)
- 6 Low (Grammar, orphaned components)

Key patterns to document:

- Type constraint mismatches (string constraints on Integer fields)
- Missing conditional field dependencies
- Inconsistent ui:disabled usage
- Missing enumNames for user-friendly display

### Testing Suite Architecture

**Location:** `hivemq-edge-adapter-sdk/testing/ui/`

```
testing/ui/
├── package.json               # npm install entry point
├── cypress.config.js          # Cypress configuration
├── dist/                      # Pre-built React app (committed)
│   ├── index.html
│   ├── assets/
│   └── ...
├── server/                    # Simple Node.js server
│   └── index.js              # Serves app + receives schemas
├── cypress/
│   ├── e2e/
│   │   ├── form-rendering.cy.js
│   │   ├── schema-validation.cy.js
│   │   └── accessibility.cy.js
│   └── support/
│       └── commands.js
├── checklist/
│   └── validate.js           # Programmatic QA checklist
└── README.md                  # Setup and usage guide
```

**Key Design Decisions:**

1. **Pre-built app bundle** - The React app is built in the frontend repo and committed as dist/ to the SDK. Java devs don't need to build it.
2. **Minimal dependencies** - Only Node.js required (npm comes with it)
3. **Schema injection** - Lightweight Java server exposes adapter schemas via API
4. **Generic tests** - Tests work with any adapter, not adapter-specific

**Two Servers Running:**

1. **Node.js (frontend)** - Serves the pre-built React app on port 3000
2. **Java (backend)** - Exposes adapter schema on port 8080

**API Contract:**

The Java server implements a minimal version of `/api/v1/management/protocol-adapters/types`:

```typescript
// Response shape (ProtocolAdaptersList)
{
  items: [{
    id: "my-adapter",
    name: "My Protocol Adapter",
    description: "...",
    configSchema: { /* JSON Schema from @ModuleConfigField */ },
    uiSchema: { /* UI Schema from resources */ },
    capabilities: ["READ"],
    category: { ... }
  }]
}
```

**Lightweight Java Server Requirements:**

- Loads adapter class at runtime (from compiled JAR/classes)
- Extracts `configSchema` using existing SDK schema generation
- Loads `uiSchema` from classpath resources
- Single endpoint: `GET /api/v1/management/protocol-adapters/types`
- No auth, no persistence - dev-only

**Existing Stack (from hello-world adapter):**

- **Build:** Gradle with Kotlin DSL (`build.gradle.kts`)
- **Testing:** JUnit 5 + Mockito (already configured)
- **Serialization:** Jackson-databind
- **Logging:** SLF4J + Logback

**Java HTTP Server Options:**

1. **Javalin** - Lightweight, popular, good DX, uses Jetty
2. **Spark Java** - Similar to Javalin, minimal footprint
3. **JDK HttpServer** - Built-in, zero additional dependencies

**Recommendation:** Use **Javalin** or JDK HttpServer to minimize dependency footprint while maintaining good developer experience.

**Build Pipeline (Frontend Side):**

```bash
# In hivemq-edge-frontend
pnpm build:adapter-test-app   # Builds standalone RJSF app
# Output copied to SDK repo testing/ui/dist/
```
