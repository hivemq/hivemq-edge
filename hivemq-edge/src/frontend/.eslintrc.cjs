module.exports = {
  env: { browser: true, es2020: true, 'cypress/globals': true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react/recommended',
    'plugin:react-hooks/recommended',
    'plugin:cypress/recommended',
  ],
  parser: '@typescript-eslint/parser',
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  plugins: ['react-refresh', 'cypress'],
  rules: {
    'react/react-in-jsx-scope': 0,
    'react/prop-types': 0,
    'react/jsx-curly-brace-presence': ['error', { props: 'never', children: 'never' }],
    'react-refresh/only-export-components': 'warn',
    '@typescript-eslint/ban-ts-comment': 0,
    '@typescript-eslint/no-unused-vars': ['error', { ignoreRestSiblings: true }],
  },
  ignorePatterns: ['**/__generated__/*'],
}
