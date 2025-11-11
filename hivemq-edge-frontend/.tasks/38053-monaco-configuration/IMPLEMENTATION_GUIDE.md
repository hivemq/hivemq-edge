# Monaco Configuration Implementation Guide

**Task:** 38053 - Monaco Configuration  
**Created:** November 6, 2025

---

## Implementation Roadmap

This document provides step-by-step implementation instructions with code examples.

---

## Phase 1: Setup & Structure

### Step 1.1: Create Directory Structure

```bash
mkdir -p src/extensions/datahub/components/forms/monaco/languages
mkdir -p src/extensions/datahub/components/forms/monaco/themes
```

**Files to create:**

```
src/extensions/datahub/components/forms/monaco/
├── monacoConfig.ts              # Main configuration orchestrator
├── types.ts                     # TypeScript type definitions
├── languages/
│   ├── javascript.config.ts     # JavaScript/TypeScript config
│   ├── json.config.ts           # JSON Schema config
│   └── protobuf.config.ts       # Protobuf config
└── themes/
    └── themes.ts                # Theme definitions
```

---

## Phase 2: Type Definitions

### File: `monaco/types.ts`

```typescript
import type { Monaco } from '@monaco-editor/react'
import type { editor } from 'monaco-editor'

export type MonacoInstance = Monaco
export type EditorInstance = editor.IStandaloneCodeEditor

export interface MonacoConfig {
  configureLanguages: (monaco: MonacoInstance) => void
  configureThemes: (monaco: MonacoInstance, options?: ThemeOptions) => void
  getEditorOptions: (language: string, isReadOnly: boolean) => editor.IStandaloneEditorConstructionOptions
}

export interface ThemeOptions {
  backgroundColor?: string
  isReadOnly?: boolean
}

export interface LanguageConfig {
  configure: (monaco: MonacoInstance) => void
}
```

---

## Phase 3: Theme Extraction

### File: `monaco/themes/themes.ts`

```typescript
import type { MonacoInstance, ThemeOptions } from '../types'

/**
 * Define custom themes for Monaco Editor
 */
export const configureThemes = (monaco: MonacoInstance, options?: ThemeOptions) => {
  const { backgroundColor = '#ffffff', isReadOnly = false } = options || {}

  // Light theme for normal editing
  monaco.editor.defineTheme('lightTheme', {
    base: 'vs',
    inherit: true,
    rules: [],
    colors: {
      'editor.background': backgroundColor,
    },
  })

  // Read-only theme with muted colors
  if (isReadOnly) {
    monaco.editor.defineTheme('readOnlyTheme', {
      base: 'vs',
      inherit: false,
      rules: [
        { token: '', foreground: '#808080' }, // Gray for all tokens
      ],
      colors: {
        'editor.foreground': '#808080',
        'editor.background': backgroundColor,
      },
    })
  }
}

/**
 * Get theme name based on editor state
 */
export const getThemeName = (isReadOnly: boolean): string => {
  return isReadOnly ? 'readOnlyTheme' : 'lightTheme'
}
```

---

## Phase 4: JavaScript Configuration

### File: `monaco/languages/javascript.config.ts`

```typescript
import type { MonacoInstance } from '../types'

/**
 * Configure JavaScript/TypeScript language features
 */
export const configureJavaScript = (monaco: MonacoInstance) => {
  // Set compiler options for better IntelliSense
  monaco.languages.typescript.javascriptDefaults.setCompilerOptions({
    target: monaco.languages.typescript.ScriptTarget.ES2020,
    allowNonTsExtensions: true,
    moduleResolution: monaco.languages.typescript.ModuleResolutionKind.NodeJs,
    module: monaco.languages.typescript.ModuleKind.CommonJS,
    noEmit: true,
    esModuleInterop: true,
    allowJs: true,
    checkJs: false, // Don't type-check, just provide IntelliSense
  })

  // Enable diagnostics for better error detection
  monaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions({
    noSemanticValidation: false, // Enable semantic validation
    noSyntaxValidation: false, // Enable syntax validation
    diagnosticCodesToIgnore: [
      1108, // Return statement outside function (for snippets)
      1375, // await outside async function (for snippets)
    ],
  })

  // Add common browser/Node globals for better IntelliSense
  monaco.languages.typescript.javascriptDefaults.addExtraLib(
    `
      // Console API
      declare var console: {
        log(...data: any[]): void;
        error(...data: any[]): void;
        warn(...data: any[]): void;
        info(...data: any[]): void;
        debug(...data: any[]): void;
        trace(...data: any[]): void;
      };

      // Common browser globals
      declare var window: any;
      declare var document: any;
      
      // setTimeout/setInterval
      declare function setTimeout(handler: Function, timeout?: number, ...args: any[]): number;
      declare function setInterval(handler: Function, timeout?: number, ...args: any[]): number;
      declare function clearTimeout(handle: number): void;
      declare function clearInterval(handle: number): void;
      
      // JSON
      declare var JSON: {
        parse(text: string): any;
        stringify(value: any, replacer?: any, space?: string | number): string;
      };
      
      // Common utilities
      declare var Math: any;
      declare var Date: any;
      declare var RegExp: any;
      declare var Array: any;
      declare var Object: any;
      declare var String: any;
      declare var Number: any;
      declare var Boolean: any;
    `,
    'global.d.ts'
  )

  // TODO: Add DataHub-specific type definitions in future PR
  // This would include types for: publish, context, etc.
}
```

---

## Phase 5: JSON Schema Configuration

### File: `monaco/languages/json.config.ts`

```typescript
import type { MonacoInstance } from '../types'

/**
 * JSON Schema meta-schemas for validation
 * These are minimal schemas - full schemas are large
 */
const JSON_SCHEMA_DRAFT_07 = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  $id: 'http://json-schema.org/draft-07/schema#',
  title: 'Core schema meta-schema',
  definitions: {
    schemaArray: {
      type: 'array',
      minItems: 1,
      items: { $ref: '#' },
    },
  },
  type: ['object', 'boolean'],
  properties: {
    type: {
      anyOf: [
        { $ref: '#/definitions/simpleTypes' },
        {
          type: 'array',
          items: { $ref: '#/definitions/simpleTypes' },
          minItems: 1,
          uniqueItems: true,
        },
      ],
    },
    properties: {
      type: 'object',
      additionalProperties: { $ref: '#' },
    },
    required: {
      type: 'array',
      items: { type: 'string' },
    },
    // ... more properties would be here
  },
}

/**
 * Configure JSON language with schema validation
 */
export const configureJSON = (monaco: MonacoInstance) => {
  // Set JSON validation options
  monaco.languages.json.jsonDefaults.setDiagnosticsOptions({
    validate: true,
    allowComments: false, // Strict JSON
    schemas: [
      {
        uri: 'http://json-schema.org/draft-07/schema#',
        fileMatch: ['*'], // Match all JSON files
        schema: JSON_SCHEMA_DRAFT_07,
      },
    ],
    enableSchemaRequest: true, // Allow fetching external schemas
    schemaValidation: 'error', // Show validation errors
    schemaRequest: 'warning', // Warn if schema can't be fetched
  })

  // Configure IntelliSense behavior
  monaco.languages.json.jsonDefaults.setModeConfiguration({
    documentFormattingEdits: true,
    documentRangeFormattingEdits: true,
    completionItems: true,
    hovers: true,
    documentSymbols: true,
    tokens: true,
    colors: true,
    foldingRanges: true,
    diagnostics: true,
    selectionRanges: true,
  })
}
```

---

## Phase 6: Protobuf Configuration

### File: `monaco/languages/protobuf.config.ts`

```typescript
import type { MonacoInstance } from '../types'

/**
 * Check if Monaco has built-in protobuf support
 */
const hasProtobufSupport = (monaco: MonacoInstance): boolean => {
  const languages = monaco.languages.getLanguages()
  return languages.some((lang) => lang.id === 'proto' || lang.id === 'protobuf')
}

/**
 * Register minimal protobuf language definition
 * This is a simplified version - full proto3 grammar would be larger
 */
const registerProtobufLanguage = (monaco: MonacoInstance) => {
  // Register the language
  monaco.languages.register({ id: 'proto' })

  // Set tokenization rules (syntax highlighting)
  monaco.languages.setMonarchTokensProvider('proto', {
    keywords: [
      'syntax',
      'import',
      'package',
      'option',
      'message',
      'enum',
      'service',
      'rpc',
      'returns',
      'repeated',
      'optional',
      'required',
      'oneof',
      'map',
      'reserved',
      'extend',
      'extensions',
    ],
    typeKeywords: [
      'double',
      'float',
      'int32',
      'int64',
      'uint32',
      'uint64',
      'sint32',
      'sint64',
      'fixed32',
      'fixed64',
      'sfixed32',
      'sfixed64',
      'bool',
      'string',
      'bytes',
    ],
    operators: ['=', '<', '>', '{', '}', '[', ']', '(', ')', ';', ',', '.'],
    tokenizer: {
      root: [
        // Keywords
        [
          /[a-z_$][\w$]*/,
          {
            cases: {
              '@keywords': 'keyword',
              '@typeKeywords': 'type',
              '@default': 'identifier',
            },
          },
        ],
        // Strings
        [/"([^"\\]|\\.)*$/, 'string.invalid'], // non-terminated string
        [/"/, 'string', '@string'],
        // Numbers
        [/\d+/, 'number'],
        // Comments
        [/\/\/.*$/, 'comment'],
        [/\/\*/, 'comment', '@comment'],
      ],
      string: [
        [/[^\\"]+/, 'string'],
        [/"/, 'string', '@pop'],
      ],
      comment: [
        [/[^/*]+/, 'comment'],
        [/\*\//, 'comment', '@pop'],
        [/[/*]/, 'comment'],
      ],
    },
  })

  // Set language configuration (auto-closing, comments, etc.)
  monaco.languages.setLanguageConfiguration('proto', {
    comments: {
      lineComment: '//',
      blockComment: ['/*', '*/'],
    },
    brackets: [
      ['{', '}'],
      ['[', ']'],
      ['(', ')'],
      ['<', '>'],
    ],
    autoClosingPairs: [
      { open: '{', close: '}' },
      { open: '[', close: ']' },
      { open: '(', close: ')' },
      { open: '"', close: '"' },
      { open: '<', close: '>' },
    ],
    surroundingPairs: [
      { open: '{', close: '}' },
      { open: '[', close: ']' },
      { open: '(', close: ')' },
      { open: '"', close: '"' },
      { open: '<', close: '>' },
    ],
  })
}

/**
 * Configure Protobuf language support
 */
export const configureProtobuf = (monaco: MonacoInstance) => {
  if (!hasProtobufSupport(monaco)) {
    // Monaco doesn't have built-in proto support, register it
    registerProtobufLanguage(monaco)
  }

  // Future: Could add validation here if needed
  // For now, basic syntax highlighting is sufficient
}
```

---

## Phase 7: Editor Options

### File: `monaco/monacoConfig.ts`

```typescript
import type { editor } from 'monaco-editor'
import type { MonacoInstance, MonacoConfig } from './types'
import { configureJavaScript } from './languages/javascript.config'
import { configureJSON } from './languages/json.config'
import { configureProtobuf } from './languages/protobuf.config'
import { configureThemes } from './themes/themes'

/**
 * Get editor options based on language and state
 */
export const getEditorOptions = (
  language: string,
  isReadOnly: boolean
): editor.IStandaloneEditorConstructionOptions => {
  const baseOptions: editor.IStandaloneEditorConstructionOptions = {
    readOnly: isReadOnly,

    // Layout
    automaticLayout: true,
    lineNumbers: 'on',
    glyphMargin: true,
    folding: true,
    foldingStrategy: 'indentation',
    renderWhitespace: 'selection',
    scrollBeyondLastLine: false,

    // Minimap (disabled by default to save space)
    minimap: {
      enabled: false,
    },

    // Scrollbar
    scrollbar: {
      vertical: 'visible',
      horizontal: 'visible',
      useShadows: false,
      verticalScrollbarSize: 10,
      horizontalScrollbarSize: 10,
    },

    // Formatting
    formatOnPaste: true,
    formatOnType: false, // Can be annoying while typing

    // Accessibility
    accessibilitySupport: 'auto',

    // Performance
    renderValidationDecorations: 'on',
  }

  // Language-specific options
  const languageOptions: Record<string, Partial<editor.IStandaloneEditorConstructionOptions>> = {
    javascript: {
      // IntelliSense options for JavaScript
      quickSuggestions: {
        other: true,
        comments: false,
        strings: true,
      },
      suggestOnTriggerCharacters: true,
      acceptSuggestionOnCommitCharacter: true,
      acceptSuggestionOnEnter: 'on',
      tabCompletion: 'on',
      wordBasedSuggestions: 'matchingDocuments',
      parameterHints: {
        enabled: true,
      },
      snippetSuggestions: 'inline',
    },
    json: {
      // IntelliSense for JSON
      quickSuggestions: {
        other: true,
        comments: false,
        strings: true,
      },
      suggestOnTriggerCharacters: true,
      tabCompletion: 'on',
      wordBasedSuggestions: 'off', // Schema-based only
    },
    proto: {
      // Basic options for Protobuf
      quickSuggestions: {
        other: true,
        comments: false,
        strings: false,
      },
      wordBasedSuggestions: 'currentDocument',
    },
  }

  return {
    ...baseOptions,
    ...(languageOptions[language] || {}),
  }
}

/**
 * Configure all Monaco language features
 */
export const configureLanguages = (monaco: MonacoInstance) => {
  configureJavaScript(monaco)
  configureJSON(monaco)
  configureProtobuf(monaco)
}

/**
 * Main Monaco configuration
 */
export const monacoConfig: MonacoConfig = {
  configureLanguages,
  configureThemes,
  getEditorOptions,
}

export default monacoConfig
```

---

## Phase 8: Refactor CodeEditor Component

### File: `CodeEditor.tsx` (Updated)

```typescript
import { useEffect, useMemo, useRef, useState } from 'react'
import { Editor, useMonaco } from '@monaco-editor/react'
import type { editor } from 'monaco-editor'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { useTranslation } from 'react-i18next'
import { generateWidgets } from '@rjsf/chakra-ui'
import { FormControl, FormLabel, Text, useColorModeValue, useToken, VStack } from '@chakra-ui/react'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { getChakra } from '@/components/rjsf/utils/getChakra'
import monacoConfig from './monaco/monacoConfig'

const CodeEditor = (lng: string, props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })
  const monaco = useMonaco()
  const [isLoaded, setIsLoaded] = useState(false)
  const [isConfigured, setIsConfigured] = useState(false)
  const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null)
  const isUserEditingRef = useRef(false)

  const [editorBgLight, editorBgDark] = useToken('colors', ['white', 'gray.200'])
  const editorBackgroundColor = useColorModeValue(editorBgLight, editorBgDark)

  const { TextareaWidget } = generateWidgets()

  const isReadOnly = useMemo(() => {
    return props.readonly || props.options.readonly || props.disabled || props.options.disabled
  }, [props.readonly, props.options.readonly, props.options.disabled, props.disabled])

  // Configure Monaco once when it loads
  useEffect(() => {
    if (monaco && !isConfigured) {
      try {
        // Configure all languages
        monacoConfig.configureLanguages(monaco)

        // Configure themes
        monacoConfig.configureThemes(monaco, {
          backgroundColor: editorBackgroundColor,
          isReadOnly,
        })

        setIsConfigured(true)
        setIsLoaded(true)
      } catch (error) {
        console.error('Failed to configure Monaco:', error)
        // Fall back to textarea
      }
    }
  }, [monaco, isConfigured, editorBackgroundColor, isReadOnly])

  // Update theme when background color or readonly state changes
  useEffect(() => {
    if (monaco && isConfigured) {
      monacoConfig.configureThemes(monaco, {
        backgroundColor: editorBackgroundColor,
        isReadOnly,
      })
    }
  }, [monaco, isConfigured, editorBackgroundColor, isReadOnly])

  const handleEditorMount = (editor: editor.IStandaloneCodeEditor) => {
    editorRef.current = editor
  }

  const handleEditorChange = (value: string | undefined) => {
    // Mark that user is editing to prevent programmatic updates from interfering
    isUserEditingRef.current = true
    props.onChange(value)

    // Reset the flag after a short delay to allow programmatic updates again
    setTimeout(() => {
      isUserEditingRef.current = false
    }, 100)
  }

  useEffect(() => {
    if (editorRef.current && !isUserEditingRef.current && props.value !== editorRef.current.getValue()) {
      // Preserve cursor position during programmatic updates
      const position = editorRef.current.getPosition()
      editorRef.current.setValue(props.value || '')

      // Restore cursor position if it was valid
      if (position) {
        editorRef.current.setPosition(position)
      }
    }
  }, [props.value])

  // Get enhanced editor options
  const editorOptions = useMemo(() => {
    if (!isConfigured) return { readOnly: isReadOnly }
    return monacoConfig.getEditorOptions(lng, isReadOnly)
  }, [lng, isReadOnly, isConfigured])

  if (!isLoaded) {
    const { options, ...rest } = props

    return (
      <>
        <TextareaWidget {...rest} options={{ ...options, rows: 6 }} />
        <Text fontSize="sm">{t('workspace.codeEditor.loadingError')}</Text>
      </>
    )
  }

  return (
    <FormControl
      {...chakraProps}
      isDisabled={props.disabled || props.readonly}
      isRequired={props.required}
      isReadOnly={isReadOnly}
      isInvalid={props.rawErrors && props.rawErrors.length > 0}
    >
      {labelValue(
        <FormLabel htmlFor={props.id} id={`${props.id}-label`}>
          {props.label}
        </FormLabel>,
        props.hideLabel || !props.label
      )}

      <VStack gap={3} alignItems="flex-start" id={props.id}>
        <Editor
          loading={<LoaderSpinner />}
          height="40vh"
          defaultLanguage={lng}
          defaultValue={props.value}
          theme={isReadOnly ? 'readOnlyTheme' : 'lightTheme'}
          onChange={handleEditorChange}
          onMount={handleEditorMount}
          options={editorOptions}
        />
      </VStack>
    </FormControl>
  )
}

export const JavascriptEditor = (props: WidgetProps) => CodeEditor('javascript', props)
export const JSONSchemaEditor = (props: WidgetProps) => CodeEditor('json', props)
export const ProtoSchemaEditor = (props: WidgetProps) => CodeEditor('proto', props)
```

---

## Phase 9: Testing

### Enhanced Test File: `CodeEditor.spec.cy.tsx`

```typescript
import type { WidgetProps } from '@rjsf/utils'
import {
  MOCK_JAVASCRIPT_SCHEMA,
  MOCK_JSONSCHEMA_SCHEMA,
  MOCK_PROTOBUF_SCHEMA,
} from '@/extensions/datahub/__test-utils__/schema.mocks.ts'
import { JavascriptEditor, JSONSchemaEditor, ProtoSchemaEditor } from '@datahub/components/forms/CodeEditor.tsx'

// @ts-ignore
const MOCK_WIDGET_PROPS: WidgetProps = {
  id: 'code-widget',
  label: 'Source Code',
  name: 'code',
  onBlur: () => undefined,
  onChange: () => undefined,
  onFocus: () => undefined,
  schema: {},
  options: {},
}

describe('CodeEditor', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  describe('Basic Rendering', () => {
    it('should render the Javascript Editor', () => {
      cy.mountWithProviders(<JavascriptEditor {...MOCK_WIDGET_PROPS} value={MOCK_JAVASCRIPT_SCHEMA} />)
      cy.get('.monaco-editor').should('be.visible')
    })

    it('should render the Protobuf Editor', () => {
      cy.mountWithProviders(<ProtoSchemaEditor {...MOCK_WIDGET_PROPS} value={MOCK_PROTOBUF_SCHEMA} />)
      cy.get('.monaco-editor').should('be.visible')
    })

    it('should render the JSONSchema Editor', () => {
      cy.mountWithProviders(<JSONSchemaEditor {...MOCK_WIDGET_PROPS} value={MOCK_JSONSCHEMA_SCHEMA} />)
      cy.get('.monaco-editor').should('be.visible')
    })
  })

  describe('IntelliSense Features', () => {
    it('should show suggestions in JavaScript editor', () => {
      cy.mountWithProviders(<JavascriptEditor {...MOCK_WIDGET_PROPS} value="console." />)

      cy.get('.monaco-editor').should('be.visible')

      // Wait for Monaco to load
      cy.wait(1000)

      // Type to trigger IntelliSense
      cy.get('.monaco-editor').click()
      cy.get('.monaco-editor textarea').type('log', { force: true })

      // Suggestion widget should appear
      cy.get('.monaco-editor .suggest-widget').should('be.visible')
    })

    it('should validate JSON Schema', () => {
      const invalidSchema = '{"type": "invalid"}'

      cy.mountWithProviders(<JSONSchemaEditor {...MOCK_WIDGET_PROPS} value={invalidSchema} />)

      cy.get('.monaco-editor').should('be.visible')

      // Wait for validation
      cy.wait(1000)

      // Should show error decoration
      cy.get('.monaco-editor .squiggly-error').should('exist')
    })
  })

  describe('Editor Behavior', () => {
    it('should auto-close brackets', () => {
      cy.mountWithProviders(<JavascriptEditor {...MOCK_WIDGET_PROPS} value="" />)

      cy.get('.monaco-editor').should('be.visible')
      cy.get('.monaco-editor textarea').type('{', { force: true })

      // Should automatically insert closing bracket
      cy.window().then((win) => {
        // @ts-ignore
        const editors = win.monaco.editor.getEditors()
        const value = editors[0].getValue()
        expect(value).to.include('{}')
      })
    })

    it('should support code folding', () => {
      const jsCode = `function test() {\n  console.log('hello');\n}`

      cy.mountWithProviders(<JavascriptEditor {...MOCK_WIDGET_PROPS} value={jsCode} />)

      cy.get('.monaco-editor').should('be.visible')

      // Folding icon should be visible
      cy.get('.monaco-editor .cldr').should('exist')
    })
  })

  describe('Read-Only Mode', () => {
    it('should apply read-only theme', () => {
      cy.mountWithProviders(<JavascriptEditor {...MOCK_WIDGET_PROPS} value="test" readonly />)

      cy.get('.monaco-editor').should('be.visible')

      // Verify editor is read-only
      cy.window().then((win) => {
        // @ts-ignore
        const editors = win.monaco.editor.getEditors()
        const options = editors[0].getOptions()
        expect(options.get(monaco.editor.EditorOption.readOnly)).to.be.true
      })
    })
  })

  describe('Fallback Behavior', () => {
    it('should render the fallback editor when Monaco fails to load', () => {
      Cypress.on('uncaught:exception', () => {
        return false
      })

      cy.intercept('https://cdn.jsdelivr.net/**', { statusCode: 404 }).as('getMonaco')
      cy.mountWithProviders(<JSONSchemaEditor {...MOCK_WIDGET_PROPS} value={MOCK_JSONSCHEMA_SCHEMA} />)

      cy.get('textarea').should('be.visible')
      cy.get("[role='group'] + p").should(
        'contain.text',
        'The advanced editor cannot be loaded. Syntax highlighting is not supported'
      )
    })
  })
})
```

---

## Phase 10: Bundle Size Measurement

### Before Implementation

```bash
# Build and check bundle size
pnpm run build

# Look for Monaco chunks
ls -lh dist/assets/ | grep -i monaco
```

### After Implementation

```bash
# Build again
pnpm run build

# Compare Monaco chunk sizes
# Expected increase: < 10KB gzipped
```

### Monitoring Script

```typescript
// tools/check-bundle-size.ts
import fs from 'fs'
import path from 'path'
import { gzipSync } from 'zlib'

const distPath = path.join(__dirname, '../dist/assets')
const files = fs.readdirSync(distPath)

const monacoFiles = files.filter((f) => f.includes('monaco') || f.includes('editor'))

let totalSize = 0
let totalGzipped = 0

monacoFiles.forEach((file) => {
  const filePath = path.join(distPath, file)
  const content = fs.readFileSync(filePath)
  const size = content.length
  const gzipped = gzipSync(content).length

  totalSize += size
  totalGzipped += gzipped

  console.log(`${file}: ${(size / 1024).toFixed(2)} KB (${(gzipped / 1024).toFixed(2)} KB gzipped)`)
})

console.log(`\nTotal Monaco: ${(totalSize / 1024).toFixed(2)} KB (${(totalGzipped / 1024).toFixed(2)} KB gzipped)`)
```

---

## Phase 11: Documentation

### Update README or Component Docs

````markdown
## Monaco Editor Configuration

The DataHub Designer uses Monaco Editor with enhanced language support:

### Supported Languages

- **JavaScript/TypeScript**: Full IntelliSense, syntax checking, auto-completion
- **JSON Schema**: Schema validation, keyword completion, error diagnostics
- **Protobuf**: Syntax highlighting, basic validation

### Features

- Auto-completion on trigger characters (`.`, `"`, etc.)
- Auto-closing brackets and quotes
- Code folding for nested structures
- Format on paste
- Syntax error detection
- Read-only mode support

### Configuration

Editor behavior can be customized via the Monaco configuration module:

```typescript
import monacoConfig from './monaco/monacoConfig'

// Configure languages
monacoConfig.configureLanguages(monaco)

// Configure themes
monacoConfig.configureThemes(monaco, options)

// Get editor options
const options = monacoConfig.getEditorOptions('javascript', false)
```
````

---

## Rollback Plan

If issues arise:

1. **Revert CodeEditor.tsx** to previous version
2. **Remove monaco/ directory**
3. **Clear browser cache** (Monaco config is cached)
4. **Rebuild** application

Previous version preserved in git history.

---

## Success Metrics

After implementation, verify:

- ✅ JavaScript IntelliSense works (type `console.` and see suggestions)
- ✅ JSON Schema validation works (invalid schemas show errors)
- ✅ Protobuf syntax highlighting works
- ✅ Bundle size increase < 20KB gzipped
- ✅ All existing tests pass
- ✅ No performance regression
- ✅ RJSF integration still works

---

## Future Enhancements (Phase 3)

Ideas for later:

1. **Custom Type Definitions**

   - Add DataHub API types (publish, context, etc.)
   - Better IntelliSense for function definitions

2. **ESLint Integration**

   - Real-time linting in JavaScript editor
   - Configurable rules

3. **Prettier Integration**

   - Format on save
   - Configurable formatting rules

4. **Advanced JSON Schema**

   - External schema resolution
   - Schema libraries
   - Custom validation

5. **Protobuf Validation**
   - Full proto3 grammar
   - Import resolution
   - Type checking

---

## Contact & Support

Questions about implementation? See:

- TASK_BRIEF.md - Overview
- ENHANCEMENT_EXAMPLES.md - Visual examples
- This file - Implementation details
