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
 * This is a simplified version for proto3 syntax
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
}
