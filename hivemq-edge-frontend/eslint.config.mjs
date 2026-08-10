import js from '@eslint/js'
import globals from 'globals'
import reactPlugin from 'eslint-plugin-react'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tsEslint from 'typescript-eslint'
import eslintConfigPrettier from 'eslint-config-prettier'
import pluginCypress from 'eslint-plugin-cypress'
import pluginQuery from '@tanstack/eslint-plugin-query'
import sonarjs from 'eslint-plugin-sonarjs'
import { noBareIntercept } from './eslint-rules/no-bare-cy-intercept.mjs'

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
      react: { version: '19.2' },
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
      cypress: pluginCypress,
      sonarjs,
      local: { rules: { 'no-bare-cy-intercept': noBareIntercept } },
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      // v7 of the plugin folded the React Compiler diagnostics into `recommended`, taking it from 2
      // rules to 16. We don't run the compiler, and the new rules flag ~50 long-standing sites (rjsf
      // templates, the workspace canvas, the DataHub designer). Keep them reported, but don't gate CI
      // on a cross-cutting refactor that belongs in its own change. `rules-of-hooks` and
      // `exhaustive-deps` — the two we have always enforced — keep their recommended severity.
      ...Object.fromEntries(
        Object.keys(reactHooks.configs.recommended.rules)
          .filter((rule) => !['react-hooks/rules-of-hooks', 'react-hooks/exhaustive-deps'].includes(rule))
          .map((rule) => [rule, 'warn'])
      ),

      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      'local/no-bare-cy-intercept': 'off',
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

      // The HTTP client from useHttpClient() is a stable, lazily-created singleton (useState
      // initializer), so its sub-clients (appClient.*) never change between renders. The bumped
      // @tanstack/eslint-plugin-query flags them as missing queryKey dependencies, which are false
      // positives against our stable-client + QUERY_KEYS convention. Disable the rule.
      '@tanstack/query/exhaustive-deps': 'off',

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
    files: ['**/*.spec.cy.tsx', '**/*.spec.cy.ts', 'cypress/e2e/**/*.{ts,tsx}', 'cypress/utils/**/*.{ts,tsx}'],
    rules: {
      'sonarjs/no-duplicate-string': 'off',
      'local/no-bare-cy-intercept': 'off',
    },
  }
)
