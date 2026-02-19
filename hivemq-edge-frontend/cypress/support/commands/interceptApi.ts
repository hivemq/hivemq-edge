import type { Route } from '../apiRoutes'

/**
 * Type-safe wrapper around cy.intercept for static API responses.
 *
 * TypeScript infers the response type `T` from the route object, so the response
 * body is validated against the OpenAPI model at compile time.
 *
 * @example
 * // ✅ Typed response — TypeScript validates { token } against ApiBearerToken
 * cy.interceptApi(API_ROUTES.authentication.authenticate, { token: 'fake_token' }).as('auth')
 *
 * // ✅ Status-only — no body, just intercept with a status code
 * cy.interceptApi(API_ROUTES.bridges.getBridgeById, { statusCode: 404 })
 *
 * // ✅ Parametric route — resolved to exact URL via withParams()
 * cy.interceptApi(
 *   API_ROUTES.bridges.getBridgeById.withParams({ bridgeId: 'my-bridge-123' }),
 *   bridgeMock
 * )
 *
 * @see {@link API_ROUTES} for the full route registry
 * @see {@link https://linear.app/hivemq/issue/EDG-73} for implementation context
 */
export const interceptApi = <T>(
  route: Route<T>,
  response: T | { statusCode: number; log?: boolean }
): Cypress.Chainable<null> => {
  // The cast to `any` is intentional: type safety is provided by the function
  // signature above. Internally we delegate to cy.intercept which accepts the
  // same plain objects — the TypeScript boundary is the function signature.
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return cy.intercept(route.method, route.url, response as any)
}
