import type { ContextObject } from 'axe-core'
import type { Options } from 'cypress-axe'
import type { Route } from '@cypr/support/__generated__/apiRoutes'
import type { StaticResponse } from '@cypr/support/commands/interceptApi'

// TODO[NVL] Duplicate from Cypress. Still failing to get them recognised by the IDE in the src
declare global {
  namespace Cypress {
    interface Chainable {
      getByTestId(value: string): Chainable<JQuery<HTMLElement>>
      getByAriaLabel(value: string): Chainable<JQuery<HTMLElement>>
      checkAccessibility(
        context?: string | Node | ContextObject | undefined,
        options?: Options | undefined,
        skipFailures?: boolean
      ): Chainable<JQuery<HTMLElement>>
      clearInterceptList(interceptAlias: string): Chainable<JQuery<HTMLElement>>
      mount(jsx: React__default.ReactNode, options?: MountOptions, rerenderKey?: string): Cypress.Chainable<MountReturn>
      mountWithProviders(
        component: React.ReactNode,
        options?: MountOptions & { routerProps?: MemoryRouterProps } & {
          wrapper?: React.JSXElementConstructor<{ children: React.ReactNode }>
        }
      ): Cypress.Chainable<MountReturn>
      checkI18nKeys(): Chainable<void>
      /**
       * Type-safe cy.intercept wrapper for static API responses.
       * TypeScript infers the response type from the route object and validates
       * the response body against the OpenAPI model at compile time.
       *
       * @see {@link API_ROUTES} in `cypress/support/apiRoutes.ts`
       * @example
       * cy.interceptApi(API_ROUTES.authentication.authenticate, { token: 'fake' }).as('auth')
       * cy.interceptApi(API_ROUTES.bridges.getBridgeById.withParams({ bridgeId: 'x' }), bridgeMock)
       * cy.interceptApi(API_ROUTES.bridges.getBridges, { statusCode: 404 })
       */
      interceptApi<T>(route: Route<T>, response: T | StaticResponse<T>): Chainable<null>
      saveHTMLSnapshot(name: string): Chainable<void>
    }
  }
}

export {}
