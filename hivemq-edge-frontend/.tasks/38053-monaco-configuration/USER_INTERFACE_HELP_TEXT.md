# JavaScript Editor - User Interface Help Text

**Audience**: End users (displayed in UI)  
**Purpose**: Brief, clear explanation of editor features  
**Date**: November 6, 2025

---

## One-Line Helper Text (Below Field)

```
Write JavaScript code to transform MQTT messages. Use the template command for starter code with built-in help.
```

**Alternative versions** (choose based on space):

**Short**:

```
Transform MQTT messages with JavaScript. Click the editor menu for help and starter code.
```

**Medium**:

```
Write JavaScript to modify MQTT messages. The editor provides auto-complete suggestions as you type.
```

**Longer**:

```
Transform MQTT messages using JavaScript. Right-click in the editor for a code template with examples and auto-complete support.
```

---

## Extended Help Text (More Info Popup / Help Icon)

### Title: JavaScript Transform Editor

**Body Text**:

This editor helps you write JavaScript code to transform MQTT messages as they flow through the system.

**What You Can Do:**

- Modify message topics to route them to different destinations
- Add, change, or remove data from message payloads
- Filter messages based on content or metadata
- Add tracking information like timestamps

**Getting Started:**

1. Right-click in the editor and select **"Insert DataHub Transform Template"**
2. A starter code template will appear with helpful examples
3. As you type, suggestions will appear to help you complete your code
4. Hover over any code element to see what it does

**Code Assistance Features:**

- **Auto-complete**: Type a dot (.) after `publish` or `context` to see available options
- **Hover Help**: Hover your mouse over any code element to see its description
- **Template**: Get starter code by right-clicking and selecting "Insert DataHub Transform Template"

**Example:**
The template shows you how to:

- Access the message topic: `publish.topic`
- Modify the message content: `publish.payload`
- Use message metadata: `context.clientId`

You don't need to be a programmer - the editor guides you with suggestions and examples as you type.

---

## Tooltip Text (For UI Elements)

### "Insert DataHub Transform Template" Button/Menu Item

```
Insert starter code with examples and helpful comments
```

### Editor Area (When Empty)

```
Start typing JavaScript code or right-click to insert a template
```

### When Hovering Over Editor

```
JavaScript editor with auto-complete and inline help
```

---

## Error Messages (User-Friendly)

### When Code Has Syntax Errors

```
There's a problem with your code. Check the highlighted lines for errors.
Right-click and select "Insert DataHub Transform Template" to start with working code.
```

### When Template Insertion Fails

```
Unable to insert template. Try refreshing the page or contact support if the problem continues.
```

---

## Help Panel Content (Collapsible Panel in UI)

### JavaScript Transform Editor - Quick Help

**What is this?**  
This is a code editor where you write JavaScript to modify MQTT messages. Don't worry if you're not a programmer - the editor helps you with suggestions as you type.

**Quick Start:**

1. Right-click in the editor
2. Choose "Insert DataHub Transform Template"
3. Edit the example code for your needs

**Key Features:**

📝 **Template with Examples**  
Get starter code that shows common tasks like modifying message content or routing

💡 **Smart Suggestions**  
Type a dot (.) after `publish` or `context` to see what you can do

📖 **Inline Help**  
Hover over any code to see what it means

⌨️ **Keyboard Shortcuts:**

- Press **F1** to open the command menu
- Type `Cmd+Space` (Mac) or `Ctrl+Space` (Windows) to show suggestions
- Right-click for more options

**Common Tasks:**

**Add a timestamp to messages:**

```javascript
publish.payload.timestamp = Date.now()
```

**Change message topic:**

```javascript
publish.topic = 'sensors/' + publish.topic
```

**Filter messages:**

```javascript
if (!publish.payload.valid) {
  return null // Drop invalid messages
}
```

**Need Help?**  
Start with the template (right-click → Insert Template) and modify the examples to match your needs.

---

## Accessibility Text (Screen Readers)

### Editor Region

```
JavaScript code editor with auto-complete support. Press F1 for commands. Right-click or use the context menu to insert a code template with examples.
```

### Template Command

```
Insert DataHub Transform Template command. Inserts starter JavaScript code with helpful examples and comments.
```

---

## Mobile/Responsive Text (Shorter Versions)

### One-Line (Mobile)

```
JavaScript editor for MQTT message transforms
```

### Help Text (Mobile)

```
Write JavaScript to modify MQTT messages.
Tap the menu icon for starter code.
Auto-complete helps as you type.
```

---

## Onboarding/First-Time User Text

### Welcome Message (First Time Using Editor)

```
👋 Welcome to the JavaScript Editor!

This editor helps you transform MQTT messages with code.

New to JavaScript? No problem!
→ Tap the menu and select "Insert DataHub Transform Template" to see examples

The editor will suggest code as you type - just follow the suggestions!

[Get Started] [Learn More]
```

### Tooltip on First Load

```
💡 Tip: Right-click and choose "Insert DataHub Transform Template" for starter code with examples
```

---

## Contextual Help (Based on User Action)

### When User Types "publish."

```
💡 Suggestions available! These are the things you can access on the publish object.
```

### When Hover Shows Documentation

```
💡 Tip: Hover over any code to see what it does
```

### When User Right-Clicks

```
💡 Try "Insert DataHub Transform Template" for starter code
```

---

## Video Script / Tutorial Text

**Title**: Getting Started with the JavaScript Transform Editor

**Script**:

"The JavaScript Transform Editor lets you modify MQTT messages as they flow through your system.

Even if you're not a programmer, you can use this editor to make common changes.

Here's how to get started:

First, right-click anywhere in the editor. From the menu, select 'Insert DataHub Transform Template'. This gives you starter code with helpful examples.

The template shows you two functions: 'init' and 'transform'. The transform function is where you'll make changes to your messages.

As you type, the editor shows suggestions. For example, type 'publish' followed by a dot. See? It shows you all the things you can do with the message.

Want to change the message topic? Just type: publish.topic equals your new topic name.

Want to add data to the message? Type: publish.payload.yourFieldName equals your value.

Hover your mouse over any code to see what it does.

That's it! The editor guides you as you type, so you can focus on what you want to accomplish, not on memorizing code."

---

**Usage Notes for UI Designers:**

1. **Keep it Simple**: Users don't need to understand Monaco Editor, TypeScript, or JSDoc
2. **Focus on Benefits**: What can they accomplish, not how it works technically
3. **Use Examples**: Show, don't just tell
4. **Progressive Disclosure**: Start simple, reveal complexity as needed
5. **Assume Intelligence, Not Expertise**: Users are smart but may not be programmers

**Recommended Placement:**

- One-line helper: Below the editor field
- Extended help: Help icon (?) next to editor label
- Quick help panel: Collapsible panel to the right of editor
- Template button: Toolbar above editor or right-click menu
