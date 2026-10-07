// @ts-check
const eslint = require('@eslint/js');
const { defineConfig } = require('eslint/config');
const eslintConfigPrettier = require('eslint-config-prettier/flat');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');

module.exports = defineConfig([
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      // Existing components still use valid constructor injection.
      '@angular-eslint/prefer-inject': 'off',
      // API payloads will be replaced with generated OpenAPI types in task 7.6.
      '@typescript-eslint/no-explicit-any': 'warn',
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'app',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'app',
          style: 'kebab-case',
        },
      ],
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended],
    rules: {
      // Existing templates can keep the supported structural-directive syntax.
      '@angular-eslint/template/prefer-control-flow': 'off',
    },
  },
  eslintConfigPrettier,
]);
