import debug from 'debug'
import type { MonacoInstance } from '../types'
const debugLogger = debug('DataHub:monaco:protobuf')

/**
 * Check if Monaco has built-in protobuf support
 */
const hasProtobufSupport = (monaco: MonacoInstance): boolean => {
  const languages = monaco.languages.getLanguages()
  return languages.some((lang) => lang.id === 'proto' || lang.id === 'protobuf')
}

/**
 * Register completion provider for protobuf keywords and types
 */
const registerProtobufCompletionProvider = (monaco: MonacoInstance) => {
  const keywords = [
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
  ]

  const typeKeywords = [
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
  ]

  // Register completion item provider for IntelliSense
  monaco.languages.registerCompletionItemProvider('proto', {
    provideCompletionItems: () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const suggestions: any[] = []

      // Add keyword suggestions
      keywords.forEach((keyword) => {
        suggestions.push({
          label: keyword,
          kind: monaco.languages.CompletionItemKind.Keyword,
          insertText: keyword,
          documentation: `Protobuf keyword: ${keyword}`,
        })
      })

      // Add type suggestions
      typeKeywords.forEach((type) => {
        suggestions.push({
          label: type,
          kind: monaco.languages.CompletionItemKind.TypeParameter,
          insertText: type,
          documentation: `Protobuf type: ${type}`,
        })
      })

      return { suggestions }
    },
  })

  debugLogger('Completion provider registered with', keywords.length, 'keywords and', typeKeywords.length, 'types')
}

/**
 * Register minimal protobuf language definition
 * This is a simplified version for proto3 syntax
 */
const registerProtobufLanguage = (monaco: MonacoInstance) => {
  debugLogger('Registering protobuf language...')

  // Register the language
  monaco.languages.register({ id: 'proto' })

  // Define keywords and types for reuse
  const keywords = [
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
  ]

  const typeKeywords = [
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
  ]

  // Set tokenization rules (syntax highlighting)
  monaco.languages.setMonarchTokensProvider('proto', {
    keywords,
    typeKeywords,
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

  // Register completion provider
  registerProtobufCompletionProvider(monaco)
}

/**
 * Configure Protobuf language support
 */
export const configureProtobuf = (monaco: MonacoInstance) => {
  debugLogger('Checking for protobuf support...')

  if (!hasProtobufSupport(monaco)) {
    debugLogger('No built-in support found, registering custom protobuf language')
    // Monaco doesn't have built-in proto support, register it
    registerProtobufLanguage(monaco)
  } else {
    debugLogger('Built-in protobuf support detected, adding completion provider')
    // Monaco has built-in support, but still add our completion provider
    registerProtobufCompletionProvider(monaco)
  }
}
