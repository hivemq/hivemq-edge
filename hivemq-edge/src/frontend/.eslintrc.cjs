module.exports = {
  env: { browser: true, es2020: true, 'cypress/globals': true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react-hooks/recommended',
    'plugin:cypress/recommended',
  ],
  parser: '@typescript-eslint/parser',
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  plugins: ['react-refresh', 'cypress'],
  rules: {
    'react-refresh/only-export-components': 'warn',
    '@typescript-eslint/ban-ts-comment': 0,
  },
  ignorePatterns: ['**/__generated__/*'],
}
