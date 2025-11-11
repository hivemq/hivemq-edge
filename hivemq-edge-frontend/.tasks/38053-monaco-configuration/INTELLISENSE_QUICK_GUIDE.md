# DataHub JavaScript IntelliSense - Quick Guide

## How to Get Code Completion Working

For IntelliSense (autocomplete, hover documentation) to work in the JavaScript editor, you **MUST include JSDoc type hints** on your function parameters.

### ✅ Correct - With JSDoc (IntelliSense WORKS)

```javascript
/**
 * @param {Publish} publish
 * @param {TransformContext} context
 */
function transform(publish, context) {
  publish.   // ← Autocomplete shows: topic, qos, retain, payload, userProperties
  context.   // ← Autocomplete shows: policyId, clientId, arguments, branches
  return publish;
}
```

### ❌ Wrong - Without JSDoc (NO IntelliSense)

```javascript
function transform(publish, context) {
  publish.   // ← No autocomplete!
  context.   // ← No autocomplete!
  return publish;
}
```

## Quick Start: Use the Template

The easiest way is to use the built-in template:

**Method 1: Context Menu**

1. Right-click in the JavaScript editor
2. Select "Insert DataHub Transform Template"

**Method 2: Command Palette**

1. Press **F1** (or right-click → "Command Palette...")
2. Type "Insert DataHub"
3. Select "Insert DataHub Transform Template"

**Method 3: Keyboard Shortcut**

- **macOS**: Cmd+Shift+I
- **Windows/Linux**: Ctrl+Shift+I

The template includes all JSDoc hints for full IntelliSense!

## Available Types

- **`Publish`** - MQTT message with topic, qos, retain, payload, userProperties
- **`TransformContext`** - Context with policyId, clientId, arguments, branches
- **`InitContext`** - Init context with addBranch(), addClientConnectionState()
- **`Branch`** - Branch for routing messages
- **`ClientConnectionState`** - Stateful storage per client
- **`UserProperty`** - MQTT 5 user property (name/value pair)

## Properties You Can Use

### On `publish`:

- `publish.topic` (string)
- `publish.qos` (0 | 1 | 2)
- `publish.retain` (boolean)
- `publish.payload` (any JSON value)
- `publish.userProperties` (UserProperty[])

### On `context`:

- `context.policyId` (string)
- `context.clientId` (string)
- `context.arguments` (Record<string, string>[])
- `context.branches` (Record<string, Branch>)

### On `initContext`:

- `initContext.addBranch(branchId)` → Branch
- `initContext.addClientConnectionState(stateId, defaultValue)` → ClientConnectionState

## Troubleshooting

**Q: Autocomplete not showing?**  
A: Make sure you have the `@param {TypeName}` JSDoc comment above your function

**Q: Can I use TypeScript?**  
A: No, but JSDoc type hints give you similar benefits in JavaScript

**Q: Hover shows nothing?**  
A: Ensure the JSDoc is directly above the function, with no blank lines between

---

**TIP**: Hover over any property or function to see its documentation!
