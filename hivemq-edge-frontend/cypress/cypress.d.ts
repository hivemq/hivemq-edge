import type { ContextObject } from 'axe-core'
import type { Options } from 'cypress-axe'

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
    }
  }
}

export {}
