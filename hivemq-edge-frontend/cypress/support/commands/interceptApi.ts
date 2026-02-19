import type { Route } from '../__generated__/apiRoutes'

/**
 * Cypress static response envelope with a typed body.
 *
 * Mirrors Cypress's own StaticResponse but narrows `body` to the OpenAPI model
 * type `T`, so the payload is validated at compile time alongside the route.
 *
 * Use this form when you need Cypress routing options (delay, throttle, …)
 * alongside a typed response body:
 *
 * @example
 * cy.interceptApi(API_ROUTES.bridges.getBridges, { body: { items: [] }, delay: 500 })
 */
export type StaticResponse<T> = {
  body?: T
  statusCode?: number
  headers?: Record<string, string | string[]>
  delay?: number
  throttleKbps?: number
  forceNetworkError?: boolean
  log?: boolean
}

/**
 * Type-safe wrapper around cy.intercept for static API responses.
 *
 * TypeScript infers the response type `T` from the route object, so the response
 * body is validated against the OpenAPI model at compile time.
 *
 * @example
 * // ✅ Direct body — TypeScript validates { token } against ApiBearerToken
 * cy.interceptApi(API_ROUTES.authentication.authenticate, { token: 'fake_token' }).as('auth')
 *
 * // ✅ Status-only shorthand
 * cy.interceptApi(API_ROUTES.bridges.getBridgeById, { statusCode: 404 })
 *
 * // ✅ Cypress routing options with typed body
 * cy.interceptApi(API_ROUTES.bridges.getBridges, { body: { items: [] }, delay: 500 })
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
export const interceptApi = <T>(route: Route<T>, response: T | StaticResponse<T>): Cypress.Chainable<null> => {
  // The cast to `any` is intentional: type safety is provided by the function
  // signature above. Internally we delegate to cy.intercept which accepts the
  // same plain objects — the TypeScript boundary is the function signature.
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return cy.intercept(route.method, route.url, response as any)
}
