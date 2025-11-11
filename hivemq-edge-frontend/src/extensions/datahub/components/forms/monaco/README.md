# Monaco Type Definitions & Templates - Developer Guide

## 📁 File Structure

```
src/extensions/datahub/components/forms/monaco/
├── types/
│   └── datahub-transforms.d.ts          ✅ SINGLE SOURCE OF TRUTH for type definitions
├── templates/
│   └── transform-template.js            ✅ SINGLE SOURCE OF TRUTH for template
└── languages/
    ├── javascript.config.ts             Uses ?raw import from datahub-transforms.d.ts
    └── datahub-commands.ts              Uses ?raw import from transform-template.js
```

## ✅ Maintainable Architecture

### Before (Bad - Duplicated Strings)

- ❌ Type definitions duplicated as strings in `datahub-types.ts`
- ❌ Template hardcoded as string in `datahub-commands.ts`
- ❌ Hard to maintain, easy to get out of sync

### After (Good - Single Source of Truth)

- ✅ **One `.d.ts` file** - Edit `datahub-transforms.d.ts` and changes apply everywhere
- ✅ **One template file** - Edit `transform-template.js` and changes apply everywhere
- ✅ **Vite ?raw imports** - Automatically load file contents as strings
- ✅ **No duplication** - DRY principle

---

## 🎯 How to Modify Type Definitions

### Editing Type Definitions

**File**: `src/extensions/datahub/components/forms/monaco/types/datahub-transforms.d.ts`

This is a **standard TypeScript declaration file**. Edit it with full TypeScript support!

#### Adding a New Interface

```typescript
/**
 * New type for your feature
 */
interface MyNewType {
  /** Property description */
  propertyName: string

  /** Method description */
  methodName(param: string): void
}
```

#### Adding a Property to Existing Interface

```typescript
interface Publish {
  // ...existing properties...

  /** New property description */
  newProperty: string
}
```

#### Adding a Method

```typescript
interface InitContext {
  // ...existing methods...

  /**
   * New method description
   * @param param - Parameter description
   */
  newMethod(param: string): ReturnType
}
```

### Changes Apply Automatically

1. Edit `datahub-transforms.d.ts`
2. Save the file
3. Vite hot-reloads (dev mode) or rebuilds (production)
4. Monaco automatically gets the updated types!

**No string manipulation needed!** 🎉

---

## 🎨 How to Modify the Template

### Editing the Template

**File**: `src/extensions/datahub/components/forms/monaco/templates/transform-template.js`

This is a **standard JavaScript file**. Edit it with full IDE support!

#### Changing the Template

```javascript
/**
 * @param {InitContext} initContext - Context providing setup methods
 */
function init(initContext) {
  // YOUR new template code here
}

/**
 * @param {Publish} publish - The MQTT PUBLISH packet to transform
 * @param {TransformContext} context - Transformation context
 * @returns {Publish|null} The transformed message, or null to drop it
 */
function transform(publish, context) {
  // YOUR new template code here
  return publish
}
```

#### Adding Examples

Just add commented code:

```javascript
function transform(publish, context) {
  // Example: Filter by topic
  // if (publish.topic.startsWith('sensor/')) {
  //   // Process sensor data
  // }

  // Example: Enrich with metadata
  // publish.payload.enrichedAt = Date.now();

  return publish
}
```

### Changes Apply Automatically

1. Edit `transform-template.js`
2. Save the file
3. Vite hot-reloads
4. Next template insertion uses updated code!

---

## 🔧 How It Works (Technical Details)

### Vite's ?raw Import

Vite provides a special import suffix `?raw` that loads file contents as a string:

```typescript
import myContent from './file.txt?raw'
// myContent is a string containing the file contents
```

### Type Definitions Loading

**File**: `javascript.config.ts`

```typescript
import datahubTransformsTypes from '../types/datahub-transforms.d.ts?raw'

// datahubTransformsTypes is now a string containing the .d.ts file
monaco.languages.typescript.javascriptDefaults.addExtraLib(
  datahubTransformsTypes,
  'ts:filename/datahub-transforms.d.ts'
)
```

### Template Loading

**File**: `datahub-commands.ts`

```typescript
import transformTemplate from '../templates/transform-template.js?raw'

// transformTemplate is now a string containing the template
const DATAHUB_TRANSFORM_TEMPLATE = transformTemplate
```

---

## 📝 Best Practices

### For Type Definitions (datahub-transforms.d.ts)

1. **Use JSDoc comments** - They appear in Monaco hover tooltips
2. **Add @param and @returns** - Shows in parameter hints
3. **Add @example** - Helps users understand usage
4. **Keep types specific** - Use `0 | 1 | 2` not `number`
5. **Document constraints** - Mention valid values, defaults, etc.

**Good Example**:

```typescript
interface Publish {
  /**
   * Quality of Service level.
   *
   * Valid values:
   * - 0: At most once delivery
   * - 1: At least once delivery
   * - 2: Exactly once delivery
   *
   * @example
   * publish.qos = 1; // At least once
   */
  qos: 0 | 1 | 2
}
```

### For Templates (transform-template.js)

1. **Use JSDoc type hints** - Required for IntelliSense
2. **Add helpful examples** - Show common patterns
3. **Comment extensively** - Users will read it
4. **Keep it simple** - Don't overcomplicate the starter code
5. **Show the return** - Always include `return publish;`

**Good Example**:

```javascript
/**
 * @param {Publish} publish - The MQTT PUBLISH packet to transform
 * @param {TransformContext} context - Transformation context
 * @returns {Publish|null} The transformed message, or null to drop it
 */
function transform(publish, context) {
  // Example: Add timestamp
  // publish.payload.timestamp = Date.now();

  // Example: Drop invalid messages
  // if (!publish.payload.valid) {
  //   return null;
  // }

  return publish
}
```

---

## 🧪 Testing Your Changes

### After Editing Type Definitions

1. **Refresh browser** (Cmd+Shift+R / Ctrl+Shift+F5)
2. **Open JavaScript editor** in DataHub
3. **Type test code**:
   ```javascript
   /**
    * @param {Publish} publish
    */
   function test(publish) {
     publish. // ← Your changes should appear here
   }
   ```
4. **Verify autocomplete** shows your new properties/methods

### After Editing Template

1. **Refresh browser**
2. **Open JavaScript editor**
3. **Run command**: F1 → "Insert DataHub Transform Template"
4. **Verify** your template changes appear

---

## 🚀 Adding New Features

### Example: Adding a New Method to Publish

**Step 1**: Edit `datahub-transforms.d.ts`

```typescript
interface Publish {
  // ...existing properties...

  /**
   * Converts payload to JSON string
   * @returns JSON string representation
   */
  toJSON(): string
}
```

**Step 2**: Update template if needed

```javascript
function transform(publish, context) {
  // Example: Use new method
  // const jsonString = publish.toJSON();

  return publish
}
```

**Step 3**: Test

```javascript
/**
 * @param {Publish} publish
 */
function test(publish) {
  publish.toJSON // ← Should show autocomplete with documentation
}
```

Done! ✅

---

## 🎓 Understanding the Flow

```
Developer edits .d.ts or template file
                ↓
         Vite watches for changes
                ↓
         Hot reload triggers
                ↓
    ?raw import re-reads file content
                ↓
    New string passed to Monaco API
                ↓
   Monaco IntelliSense updated
                ↓
    User sees changes immediately!
```

---

## 🐛 Troubleshooting

### Changes Not Appearing?

1. **Clear Vite cache**: `rm -rf node_modules/.vite && pnpm dev`
2. **Hard refresh browser**: Cmd+Shift+R (macOS) / Ctrl+Shift+F5 (Windows)
3. **Check console**: Look for "[DataHub JavaScript Config] Loading type definitions..."
4. **Verify file saved**: Make sure your changes are saved to disk

### TypeScript Errors in .d.ts?

- Use standard TypeScript syntax
- Don't include actual code, only type declarations
- Use `declare` for global functions/variables

### Template Not Updating?

- Make sure you're editing `transform-template.js`, not the old string
- Check browser console for errors
- Verify Vite dev server is running

---

## 📚 Related Documentation

- Monaco Editor API: https://microsoft.github.io/monaco-editor/
- TypeScript Declaration Files: https://www.typescriptlang.org/docs/handbook/declaration-files/introduction.html
- Vite Assets: https://vitejs.dev/guide/assets.html#importing-asset-as-string

---

## ✅ Benefits of This Architecture

| Aspect              | Before                        | After                                      |
| ------------------- | ----------------------------- | ------------------------------------------ |
| **Maintainability** | Edit strings, prone to errors | Edit proper files with IDE support         |
| **Discoverability** | Strings hidden in TS files    | Clear file structure                       |
| **Type Safety**     | Strings have no validation    | .d.ts has full TypeScript checking         |
| **DevX**            | Manual string escaping        | Full IDE features (autocomplete, refactor) |
| **Duplication**     | Multiple sources of truth     | Single source of truth                     |
| **Testing**         | Hard to test strings          | Can test files directly                    |

---

**Bottom line**: Edit the files, not the strings! 🎉
