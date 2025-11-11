# Monaco Configuration Architecture - After DevX Improvement

## 📊 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     SINGLE SOURCE OF TRUTH                       │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────────────────┐  ┌──────────────────────────────┐
│  datahub-transforms.d.ts     │  │  transform-template.js       │
│  (TypeScript Declaration)    │  │  (JavaScript Template)       │
│                              │  │                              │
│  • Full IDE support          │  │  • Full IDE support          │
│  • Type checking             │  │  • Syntax highlighting       │
│  • Refactoring tools         │  │  • No string escaping        │
│  • Git-friendly diffs        │  │  • Git-friendly diffs        │
└──────────────┬───────────────┘  └──────────────┬───────────────┘
               │                                  │
               │ ?raw import                      │ ?raw import
               │ (Vite loads as string)           │ (Vite loads as string)
               ↓                                  ↓
┌──────────────────────────────┐  ┌──────────────────────────────┐
│  javascript.config.ts        │  │  datahub-commands.ts         │
│                              │  │                              │
│  import types from           │  │  import template from        │
│  'datahub-transforms.d.ts    │  │  'transform-template.js      │
│  ?raw'                       │  │  ?raw'                       │
│                              │  │                              │
│  monaco.addExtraLib(types)   │  │  const TEMPLATE = template   │
└──────────────┬───────────────┘  └──────────────┬───────────────┘
               │                                  │
               └─────────────┬────────────────────┘
                             ↓
               ┌─────────────────────────────┐
               │      Monaco Editor          │
               │                             │
               │  • IntelliSense             │
               │  • Type checking            │
               │  • Template insertion       │
               └─────────────────────────────┘
                             ↓
               ┌─────────────────────────────┐
               │     User Experience         │
               │                             │
               │  • Autocomplete             │
               │  • Hover documentation      │
               │  • Parameter hints          │
               │  • Clean template           │
               └─────────────────────────────┘
```

## 🔄 Data Flow

```
Developer Action → Vite Hot Reload → Monaco Update → User Sees Changes

Example: Adding a new property to Publish

1. Developer edits datahub-transforms.d.ts
   ↓
2. Add: newProperty: string
   ↓
3. Save file
   ↓
4. Vite detects change
   ↓
5. ?raw import re-reads file
   ↓
6. Monaco receives updated types
   ↓
7. User types "publish." in editor
   ↓
8. Autocomplete shows newProperty! ✨
```

## 📁 File Organization

```
src/extensions/datahub/components/forms/monaco/
│
├── 📁 types/
│   └── 📄 datahub-transforms.d.ts        ← Edit types here
│       • TypeScript declaration file
│       • Full IDE support
│       • Type definitions for Monaco
│
├── 📁 templates/
│   └── 📄 transform-template.js          ← Edit template here
│       • JavaScript boilerplate
│       • Inserted via F1 command
│       • Full syntax highlighting
│
├── 📁 languages/
│   ├── 📄 javascript.config.ts
│   │   • Imports datahub-transforms.d.ts?raw
│   │   • Loads types into Monaco
│   │   • Configures IntelliSense
│   │
│   └── 📄 datahub-commands.ts
│       • Imports transform-template.js?raw
│       • Defines template insertion command
│       • Registers editor actions
│
├── 📁 themes/
│   └── 📄 themes.ts
│       • Monaco theme configuration
│
└── 📄 monacoConfig.ts
    • Main Monaco configuration
    • Editor options
```

## 🎯 Developer Workflow

### Adding a New Type

```typescript
// 1. Open: datahub-transforms.d.ts
// 2. Add interface:

interface MyNewType {
  /** Property description */
  myProperty: string
}

// 3. Use in existing interface:

interface Publish {
  // ...existing...

  /** New feature */
  myFeature: MyNewType
}

// 4. Save → Vite reloads → Done! ✅
```

### Updating Template

```javascript
// 1. Open: transform-template.js
// 2. Add example:

function transform(publish, context) {
  // Example: Use new feature
  // const feature = publish.myFeature;

  return publish
}

// 3. Save → Vite reloads → Done! ✅
```

## 🔍 How Vite ?raw Works

```typescript
// Without ?raw (regular import)
import module from './file.ts'
// module = { exports, default, ... }

// With ?raw (string import)
import content from './file.ts?raw'
// content = "/* file contents as string */"
```

### Example

```typescript
// File: my-types.d.ts
interface MyInterface {
  prop: string
}

// Import:
import types from './my-types.d.ts?raw'

// types equals:
;('interface MyInterface {\n  prop: string;\n}')

// Perfect for Monaco!
monaco.languages.typescript.javascriptDefaults.addExtraLib(types)
```

## ✅ Benefits Summary

### Before (String-based)

```typescript
// ❌ Hard to read
const TYPES = `
  interface Publish {
    topic: string;
    qos: 0 | 1 | 2;
    // ... escaped strings, no IDE help
  }
`
```

### After (File-based)

```typescript
// ✅ Easy to maintain
import types from '../types/datahub-transforms.d.ts?raw'
// Full IDE support in the .d.ts file!
```

## 🎉 Result

- **Maintainable**: Edit real files, not strings
- **Type-safe**: Full TypeScript checking
- **DRY**: Single source of truth
- **DevX**: Full IDE features (autocomplete, refactor, etc.)
- **Reliable**: Vite handles the string conversion

---

**Architecture Status**: ✅ Optimized for Developer Experience
