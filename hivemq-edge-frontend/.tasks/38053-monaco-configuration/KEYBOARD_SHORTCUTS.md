# Monaco Editor - Keyboard Shortcuts Reference

**For DataHub Designer Code Editors (JavaScript, JSON Schema, Protobuf)**

---

## Code Completion

### Trigger Suggestions:

- **Ctrl+Space** (Windows/Linux) - Manually trigger code completion
- **Cmd+Space** (Mac) - Manually trigger code completion  
  ⚠️ _Note: On Mac, this may conflict with Spotlight. If it doesn't work, use Ctrl+I instead_
- **Ctrl+I** (All platforms) - Alternative trigger for suggestions

### Accept Suggestions:

- **Tab** - Accept the selected suggestion
- **Enter** - Accept the selected suggestion
- **Esc** - Dismiss the suggestion widget without accepting

### Navigation in Suggestions:

- **Up/Down arrows** - Navigate through suggestion list
- **Page Up/Page Down** - Jump through suggestions faster

---

## Auto-Completion Behavior

### Automatic Triggers:

Suggestions appear automatically when:

- Typing property names in JSON (e.g., `"typ` shows `type`)
- Typing object methods in JavaScript (e.g., `console.` shows methods)
- After trigger characters like `.`, `"`, `{`

### Manual Trigger When Needed:

Use **Ctrl+Space** when:

- Suggestions don't appear automatically
- You want to see all available options
- You're not sure what properties/methods are available

---

## Other Useful Shortcuts

### Editing:

- **Ctrl+/** (or **Cmd+/**) - Toggle line comment
- **Alt+Up/Down** - Move current line up or down
- **Ctrl+D** - Select next occurrence of current word
- **Ctrl+Shift+K** - Delete current line

### Navigation:

- **Ctrl+F** - Find
- **Ctrl+H** - Find and replace
- **Ctrl+G** - Go to line
- **Ctrl+P** - Quick file navigation (if available)

### Formatting:

- **Shift+Alt+F** - Format document
- **Ctrl+K Ctrl+F** - Format selection

---

## JSON Schema Editor Specific

When editing JSON Schema documents:

### Property Suggestions:

```json
{
  "typ"  ← Press Ctrl+Space to see: type, title, description, etc.
}
```

### Inside Properties:

```json
{
  "properties": {
    "myField": {
      "typ"  ← Press Ctrl+Space to see schema keywords
    }
  }
}
```

### Type Values:

```json
{
  "type": ""  ← Press Ctrl+Space inside quotes to see: string, number, object, array, etc.
}
```

---

## JavaScript Editor Specific

### Object Methods:

```javascript
console.  ← Suggestions appear automatically for: log, error, warn, etc.
JSON.     ← Suggestions appear automatically for: parse, stringify
Math.     ← Suggestions appear automatically for: floor, ceil, random, etc.
```

### Force Suggestions:

```javascript
const x =  ← Press Ctrl+Space to see available variables/functions
```

---

## Protobuf Editor Specific

### Word-Based Suggestions:

```protobuf
message Test {
  str  ← Press Ctrl+Space to see: string (word-based from document)
}
```

---

## Troubleshooting

### Suggestions Don't Appear:

1. **Try manual trigger** - Press **Ctrl+Space**
2. **Check context** - Are you in the right place (not in a comment/string)?
3. **Wait a moment** - Suggestions may have a small delay
4. **Type more characters** - More context = better suggestions

### Ctrl+Space Doesn't Work:

- **On Mac**: Try **Ctrl+I** instead (Cmd+Space conflicts with Spotlight)
- **Check focus**: Click in the editor first to ensure it has focus
- **Reload page**: Sometimes Monaco needs to reinitialize

### Wrong Suggestions:

- **Keep typing** - Suggestions filter as you type
- **Use arrows** - Navigate to the right suggestion
- **Use Esc** - Dismiss and type manually

---

## Special Notes

### SPACE Key Behavior:

- **SPACE now works correctly** thanks to our custom handler
- **SPACE does NOT accept suggestions** - only Tab/Enter do
- This prevents accidental suggestion acceptance

### Suggestion Acceptance:

- **Tab** and **Enter** are the only ways to accept suggestions
- This gives you full control over when to accept vs. keep typing

---

## Quick Reference Card

| Action                   | Windows/Linux | Mac                 |
| ------------------------ | ------------- | ------------------- |
| **Trigger suggestions**  | Ctrl+Space    | Cmd+Space or Ctrl+I |
| **Accept suggestion**    | Tab or Enter  | Tab or Enter        |
| **Dismiss suggestions**  | Esc           | Esc                 |
| **Navigate suggestions** | Up/Down       | Up/Down             |
| **Toggle comment**       | Ctrl+/        | Cmd+/               |
| **Find**                 | Ctrl+F        | Cmd+F               |
| **Format**               | Shift+Alt+F   | Shift+Option+F      |
| **Move line**            | Alt+Up/Down   | Option+Up/Down      |

---

**Remember: When in doubt, press Ctrl+Space to see what's available!** 🎯
