import js from '@eslint/js'
import globals from 'globals'
import reactPlugin from 'eslint-plugin-react'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tsEslint from 'typescript-eslint'
import eslintConfigPrettier from 'eslint-config-prettier'
import pluginCypress from 'eslint-plugin-cypress/flat'
import pluginQuery from '@tanstack/eslint-plugin-query'
import sonarjs from 'eslint-plugin-sonarjs'

export default tsEslint.config(
  { ignores: ['dist', '**/__generated__/*'] },
  reactPlugin.configs.flat.recommended,
  reactPlugin.configs.flat['jsx-runtime'],
  pluginCypress.configs.recommended,
  ...pluginQuery.configs['flat/recommended'],
  eslintConfigPrettier,
  {
    extends: [js.configs.recommended, ...tsEslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    settings: {
      react: { version: 'detect' },
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
      cypress: pluginCypress,
      sonarjs,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,

      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      '@typescript-eslint/ban-ts-comment': 0,
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          caughtErrors: 'none',
          ignoreRestSiblings: true,
        },
      ],

      'react/prop-types': 0,
      'react/display-name': 0,

      'unused-expressions': 'off',
      '@typescript-eslint/no-unused-expressions': 'off',
      'cypress/no-unnecessary-waiting': 'error',
      '@typescript-eslint/consistent-type-imports': 'error',
      'react/jsx-curly-brace-presence': ['error', { props: 'never', children: 'never' }],
      // temporary sonarQube rules
      'sonarjs/todo-tag': 'off',
      'sonarjs/no-all-duplicated-branches': 'off',
      'sonarjs/no-duplicate-in-composite': 'off',
      'sonarjs/no-duplicate-string': 'off',
      'sonarjs/no-duplicated-branches': 'off',
      // 'sonarjs/no-ignored-exceptions': 'off',
      // 'sonarjs/no-commented-code': 'off',
      // 'sonarjs/no-nested-functions': 'off',
      // 'sonarjs/cognitive-complexity': 'warn',
      // 'sonarjs/no-small-switch': 'warn',
      // 'sonarjs/no-nested-conditional': 'warn',
    },
  },
  {
    files: ['**/*.spec.cy.tsx'],
    rules: {
      'sonarjs/no-duplicate-string': 'off',
    },
  }
)
