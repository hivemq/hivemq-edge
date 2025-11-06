# Task 38053 - Monaco Editor Configuration Enhancement

**Created:** November 6, 2025  
**Status:** 🔄 IN PROGRESS  
**Priority:** Medium

---

## Objective

Improve Monaco Editor configuration for enhanced code completion, validation, and core functionalities while maintaining lightweight bundle size.

---

## Context

- **Current Implementation:** Basic Monaco Editor setup via `@monaco-editor/react` wrapper
- **File:** `src/extensions/datahub/components/forms/CodeEditor.tsx`
- **Languages Supported:**
  - JavaScript/TypeScript (`text/javascript`)
  - JSON Schema (`application/schema+json`)
  - Protocol Buffers (`application/octet-stream`)
- **Usage:** DataHub Designer for editing schemas, scripts, and protobuf definitions

---

## Current State Analysis

### Existing Configuration

```typescript
// Basic theme definition for light/dark/readonly modes
monaco.editor.defineTheme('lightTheme', { ... })
monaco.editor.defineTheme('readOnlyTheme', { ... })

// Three editor variants
- JavascriptEditor (language: 'javascript')
- JSONSchemaEditor (language: 'json')
- ProtoSchemaEditor (language: 'proto')
```

### Current Features

✅ Custom themes (light/readonly)
✅ Cursor position preservation
✅ User editing detection
✅ Read-only mode support
✅ Integration with RJSF (React JSON Schema Form)

### Missing Features

❌ Code completion for JS/TS
❌ JSON Schema validation and IntelliSense
❌ Protobuf syntax validation
❌ Type definitions
❌ Linting/diagnostics
❌ Advanced editor options

---

## Requirements

### Functional Requirements

1. **JavaScript/TypeScript Support**

   - Basic code completion (built-in JS/TS features)
   - Syntax highlighting (already present)
   - Basic type inference

2. **JSON Schema Support**

   - Schema version detection (draft-04, draft-07, 2019-09, 2020-12)
   - Schema validation as you type
   - IntelliSense for JSON Schema keywords
   - Error diagnostics

3. **Protobuf Support**
   - Syntax validation
   - Basic completion for protobuf keywords
   - Error highlighting

### Non-Functional Requirements

- ⚠️ **Bundle Size:** Minimal impact - must consult before adding heavy features
- ✅ **Performance:** Fast editor initialization
- ✅ **UX:** Non-intrusive IntelliSense
- ✅ **Accessibility:** Maintain keyboard navigation

---

## Proposed Enhancements

### Phase 1: Core Language Configuration (Lightweight)

**Impact:** Low bundle size increase (~5-10KB)

1. **JavaScript Configuration**

   - Enable built-in TypeScript/JavaScript language features
   - Configure compiler options for better IntelliSense
   - Add common browser globals

2. **JSON Configuration**

   - Enable JSON schema validation
   - Configure schema resolution
   - Add JSON Schema meta-schemas for different versions

3. **Protobuf Configuration**
   - Register proto3 language definition (if not present)
   - Basic syntax validation

### Phase 2: Editor Options Enhancement (No bundle impact)

**Impact:** 0KB - just configuration

1. **Editor Behavior**

   - Quick suggestions on trigger characters
   - Auto-closing brackets/quotes
   - Code folding
   - Minimap (optional - can be toggled)
   - Line numbers configuration

2. **IntelliSense Settings**
   - Suggestion filtering
   - Snippet suggestions
   - Word-based suggestions

### Phase 3: Advanced Features (Deferred - requires approval)

**Impact:** Medium to High bundle size increase

- Custom JSON Schema libraries
- ESLint integration
- Prettier integration
- Custom type definitions
- Advanced protobuf tooling

---

## Implementation Strategy

### Step 1: Analyze Bundle Impact

- Check current Monaco bundle size
- Test incremental feature additions
- Document size impact of each enhancement

### Step 2: Create Monaco Configuration Module

```
src/extensions/datahub/components/forms/
├── CodeEditor.tsx (existing)
├── CodeEditor.spec.cy.tsx (existing)
└── monaco/
    ├── monacoConfig.ts (new - base configuration)
    ├── languages/
    │   ├── javascript.ts (JS/TS config)
    │   ├── json.ts (JSON Schema config)
    │   └── protobuf.ts (Proto config)
    └── themes/
        └── themes.ts (extracted theme definitions)
```

### Step 3: Implement Language-Specific Configs

- Extract language setup from CodeEditor component
- Create modular configuration functions
- Add editor options per language

### Step 4: Testing

- Test code completion in each language
- Verify validation works correctly
- Ensure no performance regression
- Check bundle size impact

---

## Success Criteria

✅ JavaScript editor shows basic IntelliSense  
✅ JSON editor validates against JSON Schema specs  
✅ Protobuf editor highlights syntax errors  
✅ Bundle size increase < 50KB (gzipped)  
✅ Editor initialization time unchanged  
✅ All existing tests pass  
✅ New tests for enhanced features

---

## Constraints

- ⚠️ Must maintain lightweight bundle
- ⚠️ Must not break existing RJSF integration
- ⚠️ Must support read-only mode for all features
- ⚠️ Must work in both light and dark themes

---

## References

- Monaco Editor API: https://microsoft.github.io/monaco-editor/api/index.html
- Monaco React: https://github.com/suren-atoyan/monaco-react
- JSON Schema Specification: https://json-schema.org/specification.html
- Protocol Buffers v3: https://protobuf.dev/programming-guides/proto3/

---

## Next Steps

1. Get approval for Phase 1 enhancements
2. Measure current bundle size
3. Implement monaco configuration module
4. Test and validate improvements
5. Document configuration options
