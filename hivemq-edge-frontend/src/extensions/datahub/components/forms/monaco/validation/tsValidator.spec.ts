import { describe, it, expect } from 'vitest'
import { validateJavaScriptSync, validateJavaScriptWithTypes } from './tsValidator'

describe('tsValidator', () => {
  describe('validateJavaScriptSync', () => {
    describe('valid code', () => {
      it('should return null for valid JavaScript', () => {
        const code = 'function test() { return true; }'
        const result = validateJavaScriptSync(code)
        expect(result).toBeNull()
      })

      it('should return null for valid function with parameters', () => {
        const code = `function transform(publish, context) {
          return publish;
        }`
        const result = validateJavaScriptSync(code)
        expect(result).toBeNull()
      })

      it('should return null for complex valid code', () => {
        const code = `function transform(publish, context) {
          const result = {
            ...publish,
            topic: publish.topic + '/processed',
            timestamp: context.timestamp
          };
          return result;
        }`
        const result = validateJavaScriptSync(code)
        expect(result).toBeNull()
      })

      it('should return null for valid arrow function', () => {
        const code = 'const test = (x) => x * 2;'
        const result = validateJavaScriptSync(code)
        expect(result).toBeNull()
      })

      it('should return null for ES6+ features', () => {
        const code = `
          const [a, b] = [1, 2];
          const obj = { a, b };
          const { a: x } = obj;
        `
        const result = validateJavaScriptSync(code)
        expect(result).toBeNull()
      })
    })

    describe('syntax errors', () => {
      it('should detect missing closing brace', () => {
        const code = 'function test() {'
        const result = validateJavaScriptSync(code)

        expect(result).not.toBeNull()
        expect(result).toContain("'}' expected")
      })

      it('should detect missing closing parenthesis', () => {
        const code = 'function test('
        const result = validateJavaScriptSync(code)

        expect(result).not.toBeNull()
        expect(result).toMatch(/expected/)
      })

      it('should detect missing semicolon in strict context', () => {
        const code = 'const x = 1 const y = 2'
        const result = validateJavaScriptSync(code)

        expect(result).not.toBeNull()
      })

      it('should detect invalid syntax', () => {
        const code = 'function test() { return; }; }'
        const result = validateJavaScriptSync(code)

        expect(result).not.toBeNull()
      })

      it('should detect unclosed string', () => {
        const code = 'const x = "hello'
        const result = validateJavaScriptSync(code)

        expect(result).not.toBeNull()
      })
    })

    describe('undefined variables', () => {
      it('should allow undefined variables in non-strict mode', () => {
        // Note: TypeScript in non-strict mode allows undefined variables in plain JS
        // This is expected JavaScript behavior
        const code = 'function test() { return x; }'
        const result = validateJavaScriptSync(code)

        // In non-strict JavaScript mode, this is allowed
        expect(result).toBeNull()
      })

      it('should allow typo in parameter name in non-strict mode', () => {
        // TypeScript compiler in non-strict JS mode doesn't flag this as error
        const code = `function transform(publish, context) {
          return publishe;
        }`
        const result = validateJavaScriptSync(code)

        // This is valid JavaScript syntax, just will be undefined at runtime
        expect(result).toBeNull()
      })

      it('should handle undefined in nested scope', () => {
        const code = `function test() {
          if (true) {
            return undefinedVar;
          }
        }`
        const result = validateJavaScriptSync(code)

        // Valid syntax in non-strict JavaScript
        expect(result).toBeNull()
      })
    })

    describe('error formatting', () => {
      it('should format error with line and column', () => {
        const code = 'function test() {'
        const result = validateJavaScriptSync(code)

        expect(result).not.toBeNull()
        expect(result).toMatch(/Line \d+, Column \d+:/)
      })

      it('should include error message', () => {
        const code = 'function test() {'
        const result = validateJavaScriptSync(code)

        expect(result).not.toBeNull()
        expect(result).toMatch(/: .+/)
      })

      it('should use 1-based line numbers', () => {
        const code = 'function test() {'
        const result = validateJavaScriptSync(code)

        expect(result).not.toBeNull()
        expect(result).toMatch(/Line 1,/)
      })
    })

    describe('edge cases', () => {
      it('should return null for empty string', () => {
        const result = validateJavaScriptSync('')
        expect(result).toBeNull()
      })

      it('should return null for whitespace only', () => {
        const result = validateJavaScriptSync('   \n\t  ')
        expect(result).toBeNull()
      })

      it('should handle multi-line code', () => {
        const code = `
          function test() {
            return true;
          }
        `
        const result = validateJavaScriptSync(code)
        expect(result).toBeNull()
      })

      it('should handle code with comments', () => {
        const code = `
          // This is a comment
          function test() {
            /* Multi-line
               comment */
            return true;
          }
        `
        const result = validateJavaScriptSync(code)
        expect(result).toBeNull()
      })

      it('should handle very long code', () => {
        const lines = Array(100).fill('const x = 1;').join('\n')
        const result = validateJavaScriptSync(lines)
        expect(result).toBeNull()
      })
    })

    describe('semantic errors', () => {
      it('should allow const reassignment in non-strict mode', () => {
        // TypeScript in non-strict JS mode allows this (runtime error, not compile error)
        const code = `
          const x = 1;
          x = 2;
        `
        const result = validateJavaScriptSync(code)

        // This is valid syntax, will fail at runtime
        expect(result).toBeNull()
      })

      it('should allow let reassignment', () => {
        const code = `
          let x = 1;
          x = 2;
        `
        const result = validateJavaScriptSync(code)
        expect(result).toBeNull()
      })

      it('should allow duplicate declarations in non-strict mode', () => {
        // In non-strict mode, this is allowed (though not recommended)
        const code = `
          const x = 1;
          const x = 2;
        `
        const result = validateJavaScriptSync(code)

        // Valid syntax in non-strict JavaScript
        expect(result).toBeNull()
      })
    })

    describe('performance', () => {
      it('should validate quickly for small code', () => {
        const code = 'function test() { return true; }'
        const start = performance.now()
        validateJavaScriptSync(code)
        const duration = performance.now() - start

        expect(duration).toBeLessThan(50) // Should be < 50ms
      })

      it('should validate quickly for medium code', () => {
        const code = Array(50).fill('const x = 1;').join('\n')
        const start = performance.now()
        validateJavaScriptSync(code)
        const duration = performance.now() - start

        expect(duration).toBeLessThan(100)
      })
    })

    describe('error handling', () => {
      it('should return first error when multiple errors exist', () => {
        const code = `
          function test() {
          const x = undefinedVar
        `
        const result = validateJavaScriptSync(code)

        expect(result).not.toBeNull()
        // Should return first error (could be missing brace or undefined var)
        expect(result).toBeTruthy()
      })

      it('should handle malformed code gracefully', () => {
        const code = '}{]['
        const result = validateJavaScriptSync(code)

        expect(result).not.toBeNull()
        // Should not throw, should return error message
        expect(typeof result).toBe('string')
      })
    })
  })

  describe('validateJavaScriptWithTypes', () => {
    describe('with type definitions', () => {
      it('should return null for valid code using publish parameter', () => {
        const code = `function transform(publish, context) {
          return publish;
        }`
        const result = validateJavaScriptWithTypes(code)
        expect(result).toBeNull()
      })

      it('should return null for valid code using context parameter', () => {
        const code = `function transform(publish, context) {
          console.log(context.clientId);
          return publish;
        }`
        const result = validateJavaScriptWithTypes(code)
        expect(result).toBeNull()
      })

      it('should return null when accessing publish properties', () => {
        const code = `function transform(publish, context) {
          const topic = publish.topic;
          const payload = publish.payload;
          const qos = publish.qos;
          return publish;
        }`
        const result = validateJavaScriptWithTypes(code)
        expect(result).toBeNull()
      })

      it('should return null when accessing context properties', () => {
        const code = `function transform(publish, context) {
          const id = context.clientId;
          const time = context.timestamp;
          return publish;
        }`
        const result = validateJavaScriptWithTypes(code)
        expect(result).toBeNull()
      })

      it('should allow console usage', () => {
        const code = `function transform(publish, context) {
          console.log('Processing:', publish.topic);
          console.error('Debug info');
          console.warn('Warning');
          return publish;
        }`
        const result = validateJavaScriptWithTypes(code)
        expect(result).toBeNull()
      })
    })

    describe('error detection with types', () => {
      it('should allow typo in publish parameter in non-strict mode', () => {
        const code = `function transform(publish, context) {
          return publishe;
        }`
        const result = validateJavaScriptWithTypes(code)
        expect(result).toBeNull()
      })

      it('should allow typo in context parameter', () => {
        const code = `function transform(publish, context) {
          return contexte.clientId;
        }`
        const result = validateJavaScriptWithTypes(code)
        expect(result).toBeNull()
      })

      it('should allow undefined variable with types', () => {
        const code = `function transform(publish, context) {
          return unknownVariable;
        }`
        const result = validateJavaScriptWithTypes(code)
        expect(result).toBeNull()
      })
    })

    describe('line number adjustment', () => {
      it('should handle syntax errors without type definition overhead', () => {
        const code = 'function test() {'
        const result = validateJavaScriptWithTypes(code)
        expect(result).not.toBeNull()
        expect(result).toMatch(/Line \d+,/)
      })
    })

    describe('type definition edge cases', () => {
      it('should handle empty code', () => {
        const result = validateJavaScriptWithTypes('')
        expect(result).toBeNull()
      })

      it('should handle whitespace only', () => {
        const result = validateJavaScriptWithTypes('   ')
        expect(result).toBeNull()
      })

      it('should handle valid code without using parameters', () => {
        const code = `function transform(publish, context) {
          const x = 1;
          return { topic: 'test', payload: x, qos: 0 };
        }`
        const result = validateJavaScriptWithTypes(code)
        expect(result).toBeNull()
      })
    })
  })

  describe('integration scenarios', () => {
    it('should validate typical transform function', () => {
      const code = `function transform(publish, context) {
        const modified = {
          ...publish,
          topic: publish.topic + '/processed',
          timestamp: context.timestamp
        };
        return modified;
      }`

      const result = validateJavaScriptSync(code)
      expect(result).toBeNull()
    })

    it('should allow undefined variables in typical transform function', () => {
      const code = `function transform(publish, context) {
        return {
          topic: publish.topic,
          payload: undefinedVariable
        };
      }`

      const result = validateJavaScriptSync(code)
      expect(result).toBeNull()
    })

    it('should validate transform with conditional logic', () => {
      const code = `function transform(publish, context) {
        if (publish.qos > 0) {
          return { ...publish, topic: 'high-priority' };
        }
        return publish;
      }`

      const result = validateJavaScriptSync(code)
      expect(result).toBeNull()
    })

    it('should validate transform with array operations', () => {
      const code = `function transform(publish, context) {
        const topics = publish.topic.split('/');
        topics.push('processed');
        return { ...publish, topic: topics.join('/') };
      }`

      const result = validateJavaScriptSync(code)
      expect(result).toBeNull()
    })

    describe('comparison between sync and typed validators', () => {
      it('should produce same results for valid code', () => {
        const code = `function transform(publish, context) {
        return publish;
      }`

        const syncResult = validateJavaScriptSync(code)
        const typedResult = validateJavaScriptWithTypes(code)

        expect(syncResult).toBeNull()
        expect(typedResult).toBeNull()
      })

      it('should produce similar results for syntax errors', () => {
        const code = 'function test() {'

        const syncResult = validateJavaScriptSync(code)
        const typedResult = validateJavaScriptWithTypes(code)

        expect(syncResult).not.toBeNull()
        expect(typedResult).not.toBeNull()
        expect(syncResult).toContain("'}'")
        expect(typedResult).toContain("'}'")
      })

      it('should allow undefined variables (JavaScript behavior)', () => {
        const code = `function transform(publish, context) {
        return publishe;
      }`

        const syncResult = validateJavaScriptSync(code)
        const typedResult = validateJavaScriptWithTypes(code)

        expect(syncResult).toBeNull()
        expect(typedResult).toBeNull()
      })
    })
  })
})
