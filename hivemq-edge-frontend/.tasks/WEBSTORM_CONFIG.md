# WebStorm Configuration for .tasks/ Directory

## Disable Code Analysis for Documentation Code Blocks

To prevent WebStorm from analyzing TypeScript/JavaScript/CSS code inside markdown code blocks in this directory:

### Option 1: EditorConfig (Already Applied)

The `.editorconfig` file at the root has been configured to disable language injection for markdown files in `.tasks/`:

```
[.tasks/**/*.md]
ij_markdown_disable_injections = true
```

### Option 2: Manual WebStorm Settings (Recommended)

1. **Right-click on the `.tasks/` directory** in the Project view
2. Select **"Mark Directory as"** → **"Excluded"** (or "Resource Root" to keep it visible but reduce inspections)

**OR**

1. Open **Settings/Preferences** (Cmd+, on Mac, Ctrl+Alt+S on Windows/Linux)
2. Go to **Editor** → **Language Injections**
3. Find **"Markdown code fence"** injection
4. Click **Edit**
5. Add to **Places Patterns**:
   ```
   - file:.tasks//*.md
   ```
   (with a minus sign to exclude)

**OR**

1. Open **Settings/Preferences**
2. Go to **Editor** → **Inspections**
3. Search for **"Markdown"**
4. For each markdown-related inspection:
   - Click the **"Scope"** button
   - Add a custom scope excluding `.tasks/**/*.md`

### Option 3: Suppress Inspections in Files

Add this comment at the top of markdown files that have many false positives:

```markdown
<!-- @formatter:off -->
```

At the end:

```markdown
<!-- @formatter:on -->
```

### Verify

After applying settings:

- Restart WebStorm (if needed)
- Open a markdown file in `.tasks/`
- Code blocks should no longer show TypeScript errors
