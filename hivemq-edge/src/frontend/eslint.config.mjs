import { fixupConfigRules } from '@eslint/compat'
import reactRefresh from 'eslint-plugin-react-refresh'
import globals from 'globals'
import tsParser from '@typescript-eslint/parser'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import js from '@eslint/js'
import { FlatCompat } from '@eslint/eslintrc'
import pluginChaiFriendly from 'eslint-plugin-chai-friendly'
import pluginCypress from 'eslint-plugin-cypress/flat'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const compat = new FlatCompat({
  baseDirectory: __dirname,
  recommendedConfig: js.configs.recommended,
  allConfig: js.configs.all,
})

export default [
  {
    ignores: ['**/__generated__/*'],
  },
  ...fixupConfigRules(
    compat.extends(
      'eslint:recommended',
      'plugin:@typescript-eslint/recommended',
      'plugin:react/recommended',
      'plugin:react/jsx-runtime',
      'plugin:react-hooks/recommended',
      'plugin:@tanstack/eslint-plugin-query/recommended',
      'plugin:deprecation/recommended'
    )
  ),
  {
    plugins: {
      'react-refresh': reactRefresh,
      'chai-friendly': pluginChaiFriendly,
      cypress: pluginCypress,
    },

    languageOptions: {
      globals: {
        ...globals.browser,
      },

      parser: tsParser,
      ecmaVersion: 'latest',
      sourceType: 'module',
    },

    rules: {
      'react/prop-types': 0,

      'react/jsx-curly-brace-presence': [
        'error',
        {
          props: 'never',
          children: 'never',
        },
      ],

      'react-refresh/only-export-components': 'warn',
      '@typescript-eslint/ban-ts-comment': 0,

      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          caughtErrors: 'none',
          ignoreRestSiblings: true,
        },
      ],

      'unused-expressions': 'off',
      '@typescript-eslint/no-unused-expressions': 'off',
      'chai-friendly/no-unused-expressions': 'error',
      'cypress/no-unnecessary-waiting': 'off',

      'deprecation/deprecation': 0,
      '@typescript-eslint/consistent-type-imports': 'error',
    },
  },
]
