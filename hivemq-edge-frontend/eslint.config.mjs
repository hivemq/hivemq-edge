import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tsEslint from 'typescript-eslint'
import eslintConfigPrettier from 'eslint-config-prettier'
import pluginCypress from 'eslint-plugin-cypress/flat'
import pluginQuery from '@tanstack/eslint-plugin-query'

export default tsEslint.config(
  { ignores: ['dist', '**/__generated__/*'] },
  eslintConfigPrettier,
  pluginCypress.configs.recommended,
  ...pluginQuery.configs['flat/recommended'],
  {
    extends: [js.configs.recommended, ...tsEslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
      cypress: pluginCypress,
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

      'unused-expressions': 'off',
      '@typescript-eslint/no-unused-expressions': 'off',
      'cypress/no-unnecessary-waiting': 'error',
      '@typescript-eslint/consistent-type-imports': 'error',
    },
  }
)
