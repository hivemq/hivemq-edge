# Visual Testing Suite for HiveMQ Edge Protocol Adapters

**Status:** Planned
**Created:** 2026-01-21

## Overview

A self-contained testing tool that allows Java adapter developers to visually test their configuration forms without running the full HiveMQ Edge application.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Developer's Machine                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐       ┌──────────────────────────┐   │
│  │  Adapter JAR     │       │   Testing Suite          │   │
│  │  (on classpath)  │       │                          │   │
│  │                  │       │  ┌──────────────────┐    │   │
│  │  - Config class  │◄──────┤  │  Java Server     │    │   │
│  │  - UI Schema     │       │  │  (JDK HttpServer)│    │   │
│  │  - Factory       │       │  │  Port 8080       │    │   │
│  └──────────────────┘       │  └────────┬─────────┘    │   │
│                             │           │              │   │
│                             │  ┌────────▼─────────┐    │   │
│                             │  │  React App       │    │   │
│                             │  │  (pre-built)     │    │   │
│                             │  │  RJSF + Chakra   │    │   │
│                             │  └──────────────────┘    │   │
│                             └──────────────────────────┘   │
│                                                              │
│  Browser: http://localhost:8080                             │
└─────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
hivemq-edge-adapter-sdk/
└── testing/
    └── ui/
        ├── build.gradle.kts
        ├── settings.gradle.kts
        ├── README.md
        │
        ├── server/
        │   └── src/main/java/com/hivemq/edge/adapters/testing/
        │       ├── AdapterTestServer.java       # Main entry, JDK HttpServer
        │       ├── SchemaGenerator.java         # JSON Schema from annotations
        │       ├── AdapterSchemaHandler.java    # GET /api/v1/.../types
        │       ├── StaticFileHandler.java       # Serves React bundle
        │       └── model/
        │           └── ProtocolAdapterType.java # API response model
        │
        ├── frontend/
        │   ├── package.json
        │   ├── vite.config.ts
        │   ├── dist/                    # Pre-built bundle (committed)
        │   ├── src/
        │   │   ├── App.tsx              # Adapter selector + form
        │   │   ├── AdapterForm.tsx      # RJSF wrapper
        │   │   ├── components/rjsf/     # Copied from Edge frontend
        │   │   └── validation/          # Custom format validators
        │   └── cypress/
        │       └── e2e/                 # Automated tests
        │
        └── scripts/
            └── start.sh                 # Convenience launcher
```

## Implementation Steps

### Phase 1: Java Server (4 files)

1. **AdapterTestServer.java** - Main entry point

   - Create JDK HttpServer on port 8080
   - Register `/api/v1/management/protocol-adapters/types` handler
   - Register `/` handler for static files
   - Load adapters via ServiceLoader

2. **SchemaGenerator.java** - Schema generation

   - Copy `CustomConfigSchemaGenerator` from hivemq-edge core
   - Dependencies: victools jsonschema-generator 4.38.0
   - Processes `@ModuleConfigField` annotations

3. **AdapterSchemaHandler.java** - API endpoint

   - Returns JSON matching Edge API contract:
     ```json
     {
       "items": [{
         "id": "Hello_World_Protocol",
         "configSchema": { ... },
         "uiSchema": { ... }
       }]
     }
     ```

4. **StaticFileHandler.java** - Serves React bundle
   - Serves files from `frontend/dist/`
   - SPA fallback to index.html

### Phase 2: React App (minimal RJSF)

**Copy from Edge frontend:**

- `ObjectFieldTemplate.tsx` - Tab support (`ui:tabs`)
- `FieldTemplate.tsx` - Field wrapper
- `BaseInputTemplate.tsx` - Input styling
- `ArrayFieldTemplate.tsx` - Array fields
- `UpDownWidget.tsx` - Number spinner
- `ToggleWidget.tsx` - Boolean switch
- `validation.utils.ts` - Custom format validators

**Simplifications (not needed):**

- No AdapterTagSelect (discovery feature)
- No EntitySelectWidget (MQTT entity browser)
- No MqttTransformationField
- No Zustand (use local React state)
- No React Query (simple fetch)

**Pre-build strategy:**

- Build with Vite
- Commit `dist/` folder to repo
- Java devs don't need Node.js

### Phase 3: Gradle Build

```kotlin
// testing/ui/build.gradle.kts
plugins {
    `java-library`
    application
}

dependencies {
    implementation("com.hivemq:hivemq-edge-adapter-sdk:${version}")
    implementation("com.github.victools:jsonschema-generator:4.38.0")
    implementation("com.github.victools:jsonschema-module-jackson:4.38.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
}

application {
    mainClass.set("com.hivemq.edge.adapters.testing.AdapterTestServer")
}
```

### Phase 4: Cypress Tests

**Automated checks from QA checklist:**

- Form renders without JS errors
- Required fields show validation errors
- Number constraints validated (min/max)
- Pattern validation works
- Tabs render correctly
- Field order matches ui:order

### Phase 5: Developer Workflow

```bash
# In adapter project
./gradlew build

# Run test server (adapter JAR on classpath)
java -cp "build/libs/my-adapter.jar:testing-ui.jar" \
    com.hivemq.edge.adapters.testing.AdapterTestServer

# Or via Gradle task
./gradlew testUI
```

## Critical Files to Reference

| Purpose           | File Path                                                                              |
| ----------------- | -------------------------------------------------------------------------------------- |
| Schema Generator  | `/edge/hivemq-edge/src/main/java/com/hivemq/api/json/CustomConfigSchemaGenerator.java` |
| RJSF Setup        | `/edge/hivemq-edge-frontend/src/components/rjsf/Form/ChakraRJSForm.tsx`                |
| Tab Template      | `/edge/hivemq-edge-frontend/src/components/rjsf/ObjectFieldTemplate.tsx`               |
| Format Validators | `/edge/hivemq-edge-frontend/src/components/rjsf/Form/validation.utils.ts`              |
| Test Target       | `/hivemq-hello-world-protocol-adapter/`                                                |

## Dependencies

**Java Server:**

- JDK 21 (HttpServer built-in)
- victools jsonschema-generator 4.38.0
- Jackson 2.14.2
- SLF4J + Logback

**React App:**

- @rjsf/chakra-ui 5.24.13
- @chakra-ui/react 2.8.2
- Vite (build only)
- Cypress (testing only)

## Decisions

| Decision          | Choice                           | Rationale                                                              |
| ----------------- | -------------------------------- | ---------------------------------------------------------------------- |
| Schema Generation | Copy CustomConfigSchemaGenerator | Self-contained, no hivemq-edge dependency. Document for future review. |
| React Sync        | Manual copy                      | Simple, update when needed. May drift but acceptable for testing tool. |
| Distribution      | Maven Central                    | Same publishing as SDK. One-line dependency for adapter devs.          |

## Hello-World Integration Example

```kotlin
// hello-world-adapter/build.gradle.kts
dependencies {
    testImplementation("com.hivemq:hivemq-edge-adapter-sdk-testing-ui:1.0")
}

tasks.register<JavaExec>("testUI") {
    group = "verification"
    description = "Launch visual UI test server"
    mainClass.set("com.hivemq.edge.adapters.testing.AdapterTestServer")
    classpath = sourceSets.main.get().runtimeClasspath +
                configurations.testRuntimeClasspath.get()
}
```

Developer runs: `./gradlew testUI` → Browser opens http://localhost:8080

## Estimated Work Breakdown

| Phase            | Tasks                                          | Files          |
| ---------------- | ---------------------------------------------- | -------------- |
| 1. Java Server   | AdapterTestServer, SchemaGenerator, Handlers   | 4-5 Java files |
| 2. React App     | Copy RJSF components, App.tsx, AdapterForm.tsx | ~15 files      |
| 3. Gradle Build  | build.gradle.kts, settings, publishing         | 3 files        |
| 4. Cypress Tests | Basic form rendering, validation tests         | 2-3 test files |
| 5. Documentation | README.md, integration guide                   | 2 files        |
| 6. Hello-World   | Add testUI task, verify integration            | 1 file         |
