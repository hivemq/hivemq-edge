/**
 * ESLint rule: no-bare-cy-intercept
 *
 * Warns when cy.intercept() is used with an /api/ URL and a static response body.
 * Static intercepts should use cy.interceptApi(API_ROUTES.x.y, response) instead,
 * which enforces the response shape against the OpenAPI model at compile time.
 *
 * Callback-based intercepts (req handlers) are excluded — they handle stateful
 * scenarios (CRUD, dynamic replies) where cy.interceptApi does not apply.
 *
 * @see {@link cypress/support/apiRoutes.ts} for the typed route registry
 * @see {@link https://linear.app/hivemq/issue/EDG-73}
 */

const HTTP_METHODS = new Set(['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'])

/** @type {import('eslint').Rule.RuleModule} */
export const noBareIntercept = {
  meta: {
    type: 'suggestion',
    docs: {
      description: 'Enforce cy.interceptApi() instead of cy.intercept() for static /api/ responses',
      recommended: false,
      url: 'https://linear.app/hivemq/issue/EDG-73',
    },
    messages: {
      useInterceptApi:
        'Use cy.interceptApi(API_ROUTES.namespace.route, response) instead of cy.intercept() for static API responses.' +
        ' See cypress/support/apiRoutes.ts for the typed route registry.',
    },
    schema: [],
  },

  create(context) {
    return {
      CallExpression(node) {
        // Match only cy.intercept(...)
        if (
          node.callee.type !== 'MemberExpression' ||
          node.callee.object.type !== 'Identifier' ||
          node.callee.object.name !== 'cy' ||
          node.callee.property.type !== 'Identifier' ||
          node.callee.property.name !== 'intercept'
        ) {
          return
        }

        const args = node.arguments
        if (args.length === 0) return

        // Exclude callback-based intercepts — any argument that is a function
        // expression signals a req handler (stateful/CRUD) which stays as cy.intercept
        const hasCallback = args.some(
          (arg) => arg.type === 'ArrowFunctionExpression' || arg.type === 'FunctionExpression'
        )
        if (hasCallback) return

        // cy.intercept('/api/...', ...) — URL-only or URL+response
        const firstArg = args[0]
        if (firstArg.type === 'Literal' && typeof firstArg.value === 'string' && firstArg.value.startsWith('/api/')) {
          context.report({ node, messageId: 'useInterceptApi' })
          return
        }

        // cy.intercept('GET', '/api/...', ...) — method + URL + optional response
        if (
          args.length >= 2 &&
          firstArg.type === 'Literal' &&
          typeof firstArg.value === 'string' &&
          HTTP_METHODS.has(firstArg.value.toUpperCase())
        ) {
          const secondArg = args[1]
          if (
            secondArg.type === 'Literal' &&
            typeof secondArg.value === 'string' &&
            secondArg.value.startsWith('/api/')
          ) {
            context.report({ node, messageId: 'useInterceptApi' })
          }
        }
      },
    }
  },
}
