module.exports = {
  env: {
    browser: true,
    commonjs: true,
    es6: true,
    node: true,
    'jest/globals': true,
  },
  extends: ['standard', 'plugin:prettier/recommended'],
  plugins: ['jest'],
  globals: {
    Atomics: 'readonly',
    SharedArrayBuffer: 'readonly',
  },
  parserOptions: {
    ecmaVersion: 2015,
  },
  rules: {},
};
