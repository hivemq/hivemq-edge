# JavaScript Transform Editor - User Guide

**Audience**: End users (computer literate but not necessarily developers)  
**Purpose**: Comprehensive guide to using the JavaScript editor  
**Date**: November 6, 2025

---

## Table of Contents

1. [What is the JavaScript Transform Editor?](#what-is-it)
2. [Getting Started](#getting-started)
3. [Understanding the Template](#understanding-the-template)
4. [Editor Features](#editor-features)
5. [Common Tasks](#common-tasks)
6. [Tips and Tricks](#tips-and-tricks)
7. [Troubleshooting](#troubleshooting)

---

## What is the JavaScript Transform Editor? {#what-is-it}

### Overview

The JavaScript Transform Editor is a specialized code editor that helps you write scripts to modify MQTT messages. Think of it as a powerful text editor with built-in assistance - it suggests code as you type and explains what everything does.

### What You Can Do

With this editor, you can:

- **Route messages** - Change where messages go by modifying their topic
- **Transform data** - Add, modify, or remove information in message payloads
- **Filter messages** - Decide which messages to keep and which to drop
- **Enrich messages** - Add metadata like timestamps, identifiers, or processing flags
- **Conditional logic** - Apply different transformations based on message content

### Do I Need to Be a Programmer?

**No!** While the editor uses JavaScript, you don't need programming experience to get started:

- The editor provides a **template with working examples**
- **Auto-complete** suggests what to type next
- **Hover help** explains what each piece of code does
- **Examples** in the template show common tasks

You can start by modifying the examples to match your needs.

---

## Getting Started {#getting-started}

### Step 1: Open the Editor

When you create or edit a data policy, you'll see the JavaScript Transform Editor as one of the configuration fields.

### Step 2: Insert the Template

The easiest way to start is with the built-in template:

**Method 1: Right-Click Menu**

1. Right-click anywhere in the editor
2. Select **"Insert DataHub Transform Template"**
3. The template code appears with examples

**Method 2: Command Palette**

1. Press **F1** (or right-click and choose "Command Palette")
2. Type **"Insert"**
3. Select **"Insert DataHub Transform Template"**

**Method 3: Keyboard Shortcut** (if supported)

- **Mac**: Cmd+Shift+I
- **Windows/Linux**: Ctrl+Shift+I

**Note**: Inserting the template will replace any existing code in the editor.

### Step 3: Understand the Structure

The template provides two functions:

```javascript
function init(initContext) {
  // Setup code (optional)
}

function transform(publish, context) {
  // Your transformation code goes here
  return publish
}
```

**Most of the time, you'll only need to edit the `transform` function.**

---

## Understanding the Template {#understanding-the-template}

### The Two Functions

#### init Function (Optional - Advanced)

```javascript
function init(initContext) {
  // Create branches for message routing (optional)
  // const errorBranch = initContext.addBranch('error-handling');
  // Create client connection state (optional)
  // const messageCount = initContext.addClientConnectionState('count', 0);
}
```

**What it does**: Runs once when your transform script starts. Use it for setup tasks.

**When to use it**:

- Creating message branches for routing
- Setting up counters or state that persists across messages
- Initialization that only needs to happen once

**For beginners**: You can usually ignore this function and focus on `transform`.

#### transform Function (Main Code)

```javascript
function transform(publish, context) {
  // Your code here
  return publish
}
```

**What it does**: Runs for every MQTT message. This is where you make changes.

**Important**: Always `return publish;` at the end, or `return null;` to drop the message.

### What are "publish" and "context"?

Think of them as containers with information about the message:

**publish** - The MQTT message itself

- `publish.topic` - Where the message is going (e.g., "sensors/temperature")
- `publish.payload` - The message data (e.g., `{temperature: 25, humidity: 60}`)
- `publish.qos` - Quality of service level (0, 1, or 2)
- `publish.retain` - Whether to save this message (true/false)
- `publish.userProperties` - Additional metadata (MQTT 5 only)

**context** - Information about where the message came from

- `context.clientId` - Which client sent this message
- `context.policyId` - Which policy is processing this message
- `context.arguments` - Configuration arguments (advanced)
- `context.branches` - Access to message branches (advanced)

---

## Editor Features {#editor-features}

### Auto-Complete (Code Suggestions)

As you type, the editor shows suggestions for what you can write next.

**How to use it:**

1. Type the name of something (like `publish`)
2. Type a dot: `.`
3. A list appears showing what you can access
4. Use arrow keys to select an option
5. Press **Enter** or **Tab** to accept

**Example:**

```
Type: publish.
      ↓
See: topic, payload, qos, retain, userProperties
      ↓
Select: topic
      ↓
Result: publish.topic
```

**Manual trigger**: Press **Cmd+Space** (Mac) or **Ctrl+Space** (Windows) to show suggestions anytime.

### Hover Help (Documentation)

Hover your mouse over any code to see what it does.

**Try it:**

- Hover over `publish` - See: "MQTT PUBLISH packet object"
- Hover over `publish.topic` - See: "The MQTT topic for this PUBLISH packet"
- Hover over `context.clientId` - See: "The client ID of the MQTT client"

### Syntax Highlighting

The editor uses colors to help you read code:

- **Blue**: Keywords (like `function`, `return`, `if`)
- **Green**: Text strings (like `"sensors/temp"`)
- **Orange**: Numbers (like `25`, `1`)
- **Gray**: Comments (lines starting with `//`)

### Error Detection

Red squiggly lines under code mean there's a problem. Hover over them to see what's wrong.

**Common errors:**

- Missing semicolon `;` at end of line
- Misspelled variable name
- Missing closing bracket `}` or parenthesis `)`

### Code Formatting

The editor can automatically format your code to make it readable.

**How**: Usually happens automatically when you paste code or insert the template.

---

## Common Tasks {#common-tasks}

### Task 1: Add a Timestamp to Every Message

**What you want**: Add the current date/time to message data.

**Code:**

```javascript
function transform(publish, context) {
  // Add timestamp to payload
  publish.payload.timestamp = Date.now()

  return publish
}
```

**What it does**:

- `Date.now()` gets the current time as a number
- `publish.payload.timestamp` creates a new field called "timestamp"
- The message now includes when it was processed

**Result**:

- Before: `{temperature: 25}`
- After: `{temperature: 25, timestamp: 1699286400000}`

### Task 2: Change the Message Topic

**What you want**: Route messages to a different topic.

**Code:**

```javascript
function transform(publish, context) {
  // Prefix topic with 'processed/'
  publish.topic = 'processed/' + publish.topic

  return publish
}
```

**What it does**:

- Takes the original topic
- Adds "processed/" to the beginning
- Sets it as the new topic

**Result**:

- Before: Topic is `sensors/temperature`
- After: Topic is `processed/sensors/temperature`

### Task 3: Filter Messages Based on Content

**What you want**: Only process messages that meet certain conditions.

**Code:**

```javascript
function transform(publish, context) {
  // Only keep messages with temperature above 30
  if (publish.payload.temperature > 30) {
    return publish // Keep this message
  }

  return null // Drop all other messages
}
```

**What it does**:

- Checks if temperature is greater than 30
- If yes, returns the message (it continues)
- If no, returns `null` (message is dropped)

**Important**: `return null;` drops the message. It won't be processed further.

### Task 4: Modify Message Content

**What you want**: Change or add data in the message.

**Code:**

```javascript
function transform(publish, context) {
  // Convert temperature from Celsius to Fahrenheit
  const celsius = publish.payload.temperature
  const fahrenheit = (celsius * 9) / 5 + 32

  publish.payload.temperature = fahrenheit
  publish.payload.unit = 'F'

  return publish
}
```

**What it does**:

- Reads the temperature in Celsius
- Calculates Fahrenheit
- Updates the temperature value
- Adds a unit field

**Result**:

- Before: `{temperature: 25}`
- After: `{temperature: 77, unit: 'F'}`

### Task 5: Add Client Information

**What you want**: Include information about where the message came from.

**Code:**

```javascript
function transform(publish, context) {
  // Add client ID to payload
  publish.payload.sourceClient = context.clientId
  publish.payload.processedBy = context.policyId

  return publish
}
```

**What it does**:

- Adds client ID to the message data
- Adds policy ID for tracking
- Useful for debugging or auditing

### Task 6: Conditional Routing

**What you want**: Send messages to different topics based on their content.

**Code:**

```javascript
function transform(publish, context) {
  // Route high-priority messages differently
  if (publish.payload.priority === 'high') {
    publish.topic = 'alerts/' + publish.topic
  } else {
    publish.topic = 'normal/' + publish.topic
  }

  return publish
}
```

**What it does**:

- Checks the priority field
- High priority messages go to `alerts/...`
- Normal messages go to `normal/...`

---

## Tips and Tricks {#tips-and-tricks}

### Tip 1: Start Simple

Begin with one small change, test it, then add more. Don't try to do everything at once.

**Good approach:**

1. Insert template
2. Add one line of code (e.g., add timestamp)
3. Save and test
4. Add another change
5. Repeat

### Tip 2: Use Comments

Add notes to yourself using `//` at the start of a line:

```javascript
// This adds a timestamp
publish.payload.timestamp = Date.now()

// This changes the topic
publish.topic = 'processed/' + publish.topic
```

Comments are ignored by the system - they're just for you.

### Tip 3: Check Your Payload Structure

Before modifying `publish.payload`, make sure you know what's in it.

**Example**: If your messages look like this:

```json
{
  "sensor": "temp-01",
  "reading": 25.5
}
```

Access fields like:

```javascript
publish.payload.sensor // "temp-01"
publish.payload.reading // 25.5
```

### Tip 4: Use Console Output (For Testing)

You can add `console.log()` to see values:

```javascript
console.log('Topic:', publish.topic)
console.log('Payload:', publish.payload)
```

Check your browser's console (F12 → Console tab) to see the output.

**Remember**: Remove console.log statements before going to production!

### Tip 5: Test Incrementally

After making changes:

1. Save your policy
2. Send a test message
3. Check the results
4. If something's wrong, use browser console to debug

### Tip 6: Keep a Backup

Before making major changes, copy your code somewhere safe (like a text file).

**How**:

1. Select all code (Cmd+A / Ctrl+A)
2. Copy (Cmd+C / Ctrl+C)
3. Paste into a text file
4. Make your changes
5. If needed, paste the backup back

---

## Troubleshooting {#troubleshooting}

### Problem: Auto-Complete Isn't Working

**Possible causes:**

1. You haven't added the type hints at the top of the function
2. You typed something wrong before the dot

**Solution:**
Make sure your function has this format:

```javascript
/**
 * @param {Publish} publish
 * @param {TransformContext} context
 */
function transform(publish, context) {
  publish.   // ← Auto-complete should work here
}
```

The lines starting with `*` and `@param` tell the editor what `publish` and `context` are.

### Problem: Red Squiggly Lines Under My Code

**What it means**: The editor detected a syntax error.

**How to fix**:

1. Hover over the red line to see the error message
2. Common fixes:
   - Add missing `;` at end of line
   - Add missing `}` to close a block
   - Fix spelling (e.g., `publsh` → `publish`)

### Problem: Template Won't Insert

**Possible causes:**

1. Editor is locked/read-only
2. Browser cache issue

**Solutions:**

1. Check if you have permission to edit
2. Refresh the page (Cmd+R / Ctrl+R)
3. Try using F1 → "Insert DataHub Transform Template" instead of right-click

### Problem: My Code Doesn't Do Anything

**Check these:**

1. Did you save the policy after editing?
2. Is the policy enabled/active?
3. Did you return the publish object? (`return publish;`)
4. Are messages actually going through this policy?

**Debugging**:
Add console.log to see if the code runs:

```javascript
console.log('Transform running!')
```

Check browser console (F12) to see if the message appears.

### Problem: Messages Are Being Dropped

**Likely cause**: Your code returns `null` somewhere.

**Check**:

- Do you have an `if` statement that returns null?
- Do you always have `return publish;` at the end?

**Example of accidental dropping:**

```javascript
// BAD - drops messages when temperature <= 30
if (publish.payload.temperature > 30) {
  return publish
}
// Missing return here - messages get dropped!

// GOOD - handles both cases
if (publish.payload.temperature > 30) {
  // Do something special
  publish.payload.alert = true
}
return publish // Always return
```

### Problem: Error About "Cannot Read Property"

**Error message example**: `Cannot read property 'temperature' of undefined`

**What it means**: You're trying to access something that doesn't exist.

**Common causes:**

1. `publish.payload.temperature` but there's no `temperature` field
2. `publish.payload` is not an object

**Solution**: Check if it exists first:

```javascript
// Safe way
if (publish.payload && publish.payload.temperature) {
  // Now we know temperature exists
  const temp = publish.payload.temperature
}
```

### Problem: I Made a Mistake and Want to Start Over

**Easy fix:**

1. Right-click in the editor
2. Select "Insert DataHub Transform Template"
3. Confirm that you want to replace the existing code
4. Fresh template appears

**Note**: This replaces ALL your code, so make a backup first if needed!

---

## Quick Reference

### Accessing Message Parts

```javascript
publish.topic // "sensors/temperature"
publish.payload // {temperature: 25}
publish.payload.temp // 25
publish.qos // 0, 1, or 2
publish.retain // true or false
context.clientId // "sensor-01"
context.policyId // "my-policy"
```

### Common Modifications

```javascript
// Change topic
publish.topic = 'new/topic'

// Add field to payload
publish.payload.newField = 'value'

// Modify existing field
publish.payload.temperature = publish.payload.temperature + 10

// Remove field
delete publish.payload.unwantedField
```

### Control Flow

```javascript
// Keep or drop message
if (condition) {
  return publish // Keep
} else {
  return null // Drop
}

// Conditional modification
if (publish.payload.temperature > 30) {
  publish.payload.alert = true
}
```

### Useful JavaScript

```javascript
// Current timestamp
Date.now()

// Current date as string
new Date().toISOString()

// Combine strings
'hello' + ' ' + 'world'  // "hello world"

// Convert to uppercase
publish.topic.toUpperCase()

// Convert to lowercase
publish.topic.toLowerCase()

// Check if something exists
if (publish.payload.field) { ... }
```

---

## Getting Help

### In the Editor

- **Right-click** → See available commands
- **F1** → Open command palette
- **Hover** → See documentation for any code element
- **Cmd+Space / Ctrl+Space** → Show suggestions

### Documentation

- Use the template examples as a starting point
- Hover over code to see what it does
- Check error messages (they usually explain the problem)

### Support

If you're stuck:

1. Check this guide's troubleshooting section
2. Try inserting a fresh template and starting over
3. Contact your system administrator
4. Provide error messages and screenshots for faster help

---

## Summary

**Key Points to Remember:**

1. **Use the template** - It provides working examples
2. **Auto-complete is your friend** - Type dot (`.`) to see options
3. **Hover for help** - Mouse over anything to see what it does
4. **Start simple** - Make one change at a time
5. **Always return publish** - Unless you want to drop the message
6. **Test incrementally** - Save and test after each change
7. **Comments help** - Use `//` to add notes

**Most Important:**

You don't need to memorize JavaScript. The editor helps you with:

- Suggestions as you type
- Documentation on hover
- Working examples in the template

**Focus on what you want to accomplish, and let the editor guide you on how to write it.**

---

## Appendix: Example Scenarios

### Scenario 1: Temperature Monitoring

**Goal**: Add alerts for high temperatures

```javascript
function transform(publish, context) {
  // Check temperature
  if (publish.payload.temperature > 35) {
    publish.payload.alert = 'HIGH'
    publish.topic = 'alerts/' + publish.topic
  } else {
    publish.payload.alert = 'NORMAL'
  }

  // Add timestamp
  publish.payload.checkedAt = new Date().toISOString()

  return publish
}
```

### Scenario 2: Data Enrichment

**Goal**: Add metadata to all messages

```javascript
function transform(publish, context) {
  // Add processing information
  publish.payload.processedAt = Date.now()
  publish.payload.processedBy = context.policyId
  publish.payload.sourceClient = context.clientId

  // Add version for tracking
  publish.payload.dataVersion = '1.0'

  return publish
}
```

### Scenario 3: Message Filtering

**Goal**: Only process messages from specific sensors

```javascript
function transform(publish, context) {
  // List of allowed sensors
  const allowedSensors = ['sensor-01', 'sensor-02', 'sensor-03']

  // Check if client is in the list
  if (allowedSensors.includes(context.clientId)) {
    return publish // Keep message
  }

  return null // Drop message from other sensors
}
```

### Scenario 4: Data Transformation

**Goal**: Standardize units and format

```javascript
function transform(publish, context) {
  // Convert temperature to Fahrenheit if needed
  if (publish.payload.unit === 'C') {
    publish.payload.value = (publish.payload.value * 9) / 5 + 32
    publish.payload.unit = 'F'
  }

  // Round to 2 decimal places
  publish.payload.value = Math.round(publish.payload.value * 100) / 100

  // Add standard timestamp
  publish.payload.timestamp = new Date().toISOString()

  return publish
}
```

---

**Document Version**: 1.0  
**Last Updated**: November 6, 2025  
**Feedback**: If you have suggestions to improve this guide, please contact your administrator.
