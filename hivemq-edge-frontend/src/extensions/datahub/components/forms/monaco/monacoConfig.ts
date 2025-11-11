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
    formatOnType: false,

    // Accessibility
    accessibilitySupport: 'auto',

    // Performance
    renderValidationDecorations: 'on',
  }

  // Language-specific options
  const languageOptions: Record<string, Partial<editor.IStandaloneEditorConstructionOptions>> = {
    javascript: {
      quickSuggestions: {
        other: true,
        comments: false,
        strings: true,
      },
      suggestOnTriggerCharacters: true,
      acceptSuggestionOnEnter: 'on',
      tabCompletion: 'on',
      wordBasedSuggestions: 'off', // Use only TypeScript-based suggestions
      parameterHints: {
        enabled: true,
        cycle: true, // Allow cycling through parameter hints
      },
      suggest: {
        showFunctions: true,
        showFields: true,
        showVariables: true,
        showConstants: true,
        showInterfaces: true,
        showProperties: true,
        showMethods: true,
        showKeywords: true,
        showWords: false, // Don't suggest random words from document
        showSnippets: true,
      },
      // Enable hover tooltips
      hover: {
        enabled: true,
        delay: 300,
      },
    },
    json: {
      quickSuggestions: {
        other: true,
        comments: false,
        strings: true,
      },
      suggestOnTriggerCharacters: true,
      acceptSuggestionOnEnter: 'on',
      tabCompletion: 'on',
      wordBasedSuggestions: 'off',
      parameterHints: {
        enabled: true,
      },
    },
    proto: {
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
