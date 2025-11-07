# Monaco IntelliSense Configuration Guide

**Date:** November 6, 2025  
**Task:** 38053 - Monaco Configuration Enhancement  
**Subtask:** 4 - Custom IntelliSense Support

---

## Overview

This guide explains how to configure and extend Monaco Editor's IntelliSense for DataHub JavaScript transforms. The IntelliSense system provides autocomplete, hover documentation, parameter hints, and type checking for user scripts.

---

## Architecture

### Components

1. **Type Definitions** (`datahub-transforms.d.ts`)

   - TypeScript declaration file with complete API definitions
   - Loaded into Monaco's TypeScript language service
   - Provides the foundation for all IntelliSense features

2. **Monaco Configuration** (`javascript.config.ts`)

   - Configures TypeScript compiler options
   - Sets up diagnostics (error/warning behavior)
   - Loads type definitions into Monaco

3. **Custom Commands** (`datahub-commands.ts`)

   - Registers custom Monaco commands
   - Provides code actions (quick fixes)
   - Template insertion functionality

4. **Editor Options** (`monacoConfig.ts`)
   - Configures IntelliSense behavior
   - Controls suggestion widgets
   - Enables parameter hints and hover tooltips

---

## Type Definition Format

### Why TypeScript Declaration Files (`.d.ts`)?

- **Native Support**: Monaco uses the TypeScript language service, making `.d.ts` files the natural choice
- **Rich Documentation**: Supports JSDoc comments with `@param`, `@returns`, `@example`, etc.
- **Type Inference**: Automatic type inference for variables and expressions
- **Standard Format**: Well-documented and widely used

### Structure

```typescript
/**
 * Interface with JSDoc documentation
 *
 * @example
 * // Usage example
 * const value = object.property;
 */
interface MyInterface {
  /**
   * Property description
   */
  property: string

  /**
   * Method description
   * @param param - Parameter description
   * @returns Return value description
   */
  method(param: string): ReturnType
}

/**
 * Global function declaration
 * @param param - Parameter description
 * @returns Return value description
 */
declare function myFunction(param: MyInterface): ReturnType
```

---

## How to Modify Type Definitions

### Location

```
src/extensions/datahub/components/forms/monaco/types/datahub-transforms.d.ts
```

### Adding a New Interface

1. **Define the interface with JSDoc**:

```typescript
/**
 * Description of what this interface represents.
 * Include usage context and important notes.
 *
 * @example
 * // Show how to use this interface
 * const example: NewInterface = {
 *   property: 'value'
 * };
 */
interface NewInterface {
  /**
   * Detailed property description
   * Include type information and constraints
   */
  property: string
}
```

2. **Use it in existing interfaces**:

```typescript
interface Publish {
  // ...existing properties...

  /**
   * New property using the new interface
   */
  newProperty: NewInterface
}
```

3. **The changes are automatically loaded** - Monaco will pick up the new definitions on next load

### Modifying Existing Interfaces

To add a property to an existing interface:

```typescript
interface Publish {
  // ...existing properties...

  /**
   * New property description with examples
   *
   * @example
   * publish.newProperty = 'value';
   */
  newProperty: string
}
```

To change a property type:

```typescript
interface Publish {
  /**
   * Updated description
   * Now accepts multiple types
   */
  payload: Record<string, unknown> | string | number
}
```

### Adding Function Signatures

For functions available in the global scope:

```typescript
/**
 * Function description
 *
 * @param param1 - First parameter
 * @param param2 - Second parameter
 * @returns What the function returns
 *
 * @example
 * const result = myFunction('value', 123);
 */
declare function myFunction(param1: string, param2: number): ResultType
```

---

## IntelliSense Features

### 1. Hover Documentation

**What it does**: Shows function/property documentation when hovering

**Configuration**: Enabled in `monacoConfig.ts`:

```typescript
hover: {
  enabled: true,
  delay: 300,
}
```

**Documentation source**: JSDoc comments in type definitions

**Example**:

```typescript
/**
 * The MQTT topic for this PUBLISH packet.
 * Can be modified to route the message to a different topic.
 *
 * @example
 * publish.topic = 'sensors/' + context.clientId + '/temperature';
 */
topic: string
```

### 2. Code Completion (Autocomplete)

**What it does**: Suggests properties/methods as you type

**Trigger**: Type `.` after an object name, or press Ctrl+Space

**Configuration** in `monacoConfig.ts`:

```typescript
quickSuggestions: {
  other: true,      // Enable for identifiers
  comments: false,  // Disable in comments
  strings: true,    // Enable in strings
},
suggestOnTriggerCharacters: true,  // Show on typing '.'
wordBasedSuggestions: 'off',       // Only show TypeScript suggestions
suggest: {
  showFunctions: true,
  showFields: true,
  showVariables: true,
  showProperties: true,
  showMethods: true,
  showWords: false,  // Don't suggest random words
}
```

**What users see**:

- Type `publish.` → see: topic, qos, retain, payload, userProperties
- Type `initContext.` → see: addBranch, addClientConnectionState
- Type `context.` → see: arguments, policyId, clientId, branches

### 3. Parameter Hints

**What it does**: Shows function signature while typing parameters

**Trigger**: Automatically when typing `(` after function name, or press Ctrl+Shift+Space

**Configuration**:

```typescript
parameterHints: {
  enabled: true,
  cycle: true,  // Allow cycling through overloads
}
```

**Example**:

```
initContext.addBranch(█)
                     ↓
           branchId: string
```

### 4. Type Inference

**What it does**: Infers types of variables automatically

**How it works**: TypeScript analyzes assignment statements

**Example**:

```javascript
const topic = publish.topic // Inferred as: string
const qos = publish.qos // Inferred as: 0 | 1 | 2
```

---

## Compiler Options

Located in `javascript.config.ts`:

```typescript
monaco.languages.typescript.javascriptDefaults.setCompilerOptions({
  target: monaco.languages.typescript.ScriptTarget.ES2020,
  allowNonTsExtensions: true,
  checkJs: true, // Enable type checking
  strict: false, // Not too strict for user scripts
  noLib: false, // Include standard library
})
```

### Key Settings:

- **checkJs: true** - Enables type checking in JavaScript files
- **strict: false** - Allows flexible JavaScript patterns
- **noLib: false** - Includes built-in JavaScript types (Array, Object, etc.)

---

## Diagnostic Options

Controls error/warning behavior:

```typescript
monaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions({
  noSemanticValidation: false, // Enable semantic checks
  noSyntaxValidation: false, // Enable syntax checks
  noSuggestionDiagnostics: false, // Enable suggestions
  diagnosticCodesToIgnore: [
    1108, // Return outside function (common in snippets)
    1375, // Await outside async (if needed)
    2304, // Cannot find name (for user variables)
    7006, // Implicit 'any' type
  ],
})
```

### Adding Ignored Diagnostics:

If users are seeing unhelpful errors, add the error code:

1. See the error in Monaco
2. Note the error code (e.g., `TS2304`)
3. Add to `diagnosticCodesToIgnore` array (use just the number: `2304`)

---

## Custom Commands

### Template Insertion Command

**Command ID**: `datahub.insertTransformTemplate`

**How to trigger**:

- Command Palette: Ctrl+Shift+P (Cmd+Shift+P on Mac) → "Insert DataHub Transform Template"
- Code Action: Shows as quick fix when editor is empty

**Implementation** (`datahub-commands.ts`):

```typescript
monaco.editor.addCommand({
  id: 'datahub.insertTransformTemplate',
  run: (accessor) => {
    const editor = accessor as unknown as editor.IStandaloneCodeEditor
    // Insert template logic...
  },
})
```

### Modifying the Template

Edit the `DATAHUB_TRANSFORM_TEMPLATE` constant in `datahub-commands.ts`:

```typescript
const DATAHUB_TRANSFORM_TEMPLATE = `/**
 * Your custom template
 */
function init(initContext) {
  // Custom initialization
}

function transform(publish, context) {
  // Custom transformation
  return publish;
}
`
```

### Adding New Commands

1. **Define the command**:

```typescript
monaco.editor.addCommand({
  id: 'datahub.myCustomCommand',
  run: (accessor) => {
    const editor = accessor as unknown as editor.IStandaloneCodeEditor
    // Command logic
  },
})
```

2. **Register it**:

Add to `configureDataHubFeatures()` in `datahub-commands.ts`

3. **Trigger via**:
   - Command Palette
   - Keyboard shortcut (requires additional keybinding registration)
   - Code Action

---

## Code Actions (Quick Fixes)

**What they are**: Contextual suggestions shown as lightbulb icons

**Implementation**:

```typescript
monaco.languages.registerCodeActionProvider('javascript', {
  provideCodeActions: (model) => {
    // Analyze current code
    const isEmpty = model.getValue().trim() === ''

    if (isEmpty) {
      return {
        actions: [
          {
            title: 'Insert DataHub Transform Template',
            kind: 'quickfix',
            isPreferred: true,
            edit: {
              edits: [
                {
                  resource: model.uri,
                  textEdit: {
                    range: model.getFullModelRange(),
                    text: TEMPLATE,
                  },
                },
              ],
            },
          },
        ],
        dispose: () => {},
      }
    }

    return { actions: [], dispose: () => {} }
  },
})
```

### Adding Custom Code Actions:

1. **Determine when to show**:

   - Analyze model content
   - Check cursor position
   - Look for specific patterns

2. **Create the action**:

   - Provide clear title
   - Set appropriate kind (`quickfix`, `refactor`, etc.)
   - Define the edit to apply

3. **Return action array**:
   - Multiple actions can be returned
   - Set `isPreferred: true` for default action

---

## Testing

### Component Tests

Location: `src/extensions/datahub/components/forms/monaco/CodeEditor.IntelliSense.spec.cy.tsx`

**Running tests**:

```bash
pnpm cypress:run:component --spec "src/extensions/datahub/components/forms/monaco/CodeEditor.IntelliSense.spec.cy.tsx"
```

### Manual Testing

1. **Open the application** with Monaco editor
2. **Type `publish.`** → verify autocomplete appears
3. **Hover over `publish`** → verify tooltip shows documentation
4. **Type `initContext.addBranch(`** → verify parameter hint appears
5. **Press Ctrl+Shift+P** → verify custom commands are listed

---

## Troubleshooting

### IntelliSense not working

**Check**:

1. Are type definitions loaded? (Check browser console for errors)
2. Is `checkJs: true` in compiler options?
3. Are diagnostics enabled?

**Solution**:

- Clear browser cache
- Check Monaco configuration in `javascript.config.ts`
- Verify type definitions have no syntax errors

### Autocomplete shows wrong suggestions

**Cause**: Word-based suggestions enabled

**Solution**:

```typescript
wordBasedSuggestions: 'off' // In monacoConfig.ts
```

### Errors on valid code

**Cause**: Over-strict diagnostic settings

**Solution**:
Add error code to `diagnosticCodesToIgnore` array

### Type definitions not updating

**Cause**: Monaco caches type definitions

**Solution**:

- Restart dev server
- Clear browser cache
- Check that `addExtraLib()` is called with new content

---

## Best Practices

### Writing Type Definitions

1. **Always include JSDoc**:

   - Brief description on first line
   - Detailed explanation if needed
   - `@param` for parameters
   - `@returns` for return values
   - `@example` for common usage

2. **Use specific types**:

   ```typescript
   // Good
   qos: 0 | 1 | 2

   // Avoid
   qos: number
   ```

3. **Provide examples**:

   - Show real-world usage
   - Include edge cases
   - Demonstrate best practices

4. **Keep it updated**:
   - Match actual API behavior
   - Update when API changes
   - Remove deprecated items

### Configuration

1. **Balance strictness**:

   - Enable helpful errors
   - Ignore noisy warnings
   - Don't frustrate users

2. **Performance**:

   - Don't enable too many diagnostics
   - Keep type definitions concise
   - Use lazy loading if needed

3. **User experience**:
   - Fast autocomplete (< 300ms)
   - Relevant suggestions
   - Clear error messages

---

## Examples

### Adding a New API Method

**Scenario**: Add `addMetric` method to `InitContext`

1. **Update type definition**:

```typescript
interface InitContext {
  // ...existing methods...

  /**
   * Registers a custom metric for tracking.
   *
   * @param metricId - Unique identifier for the metric
   * @param metricType - Type of metric (counter, gauge, histogram)
   * @returns Metric object for recording values
   *
   * @example
   * const messageCounter = initContext.addMetric('messages', 'counter');
   * // Later in transform:
   * messageCounter.increment();
   */
  addMetric(metricId: string, metricType: 'counter' | 'gauge' | 'histogram'): Metric
}

/**
 * Metric interface for recording values
 */
interface Metric {
  /**
   * Increments a counter metric by 1
   */
  increment(): void

  /**
   * Records a value for gauge or histogram metrics
   * @param value - The value to record
   */
  record(value: number): void
}
```

2. **Test it**:

   - Type `initContext.` → see `addMetric` in list
   - Hover over `addMetric` → see documentation
   - Type `initContext.addMetric(` → see parameter hints

3. **Done!** Users now have IntelliSense for the new method

---

## Summary

- **Type definitions** (`.d.ts` files) provide the foundation
- **Monaco configuration** enables and tunes IntelliSense features
- **Custom commands** add convenience features
- **Keep documentation clear** and examples practical
- **Test thoroughly** after making changes

For questions or issues, refer to:

- Monaco Editor documentation: https://microsoft.github.io/monaco-editor/
- TypeScript Handbook: https://www.typescriptlang.org/docs/handbook/
- Task documentation: `.tasks/38053-monaco-configuration/`

---

**Last Updated:** November 6, 2025
