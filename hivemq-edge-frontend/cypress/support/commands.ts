/// <reference types="cypress" />
import type { ContextObject } from 'axe-core'
import type { Options } from 'cypress-axe'
import { getByTestId } from './commands/getByTestId'
import { getByAriaLabel } from './commands/getByAriaLabel'
import { checkAccessibility } from './commands/checkAccessibility'
import { clearInterceptList } from './commands/clearInterceptList'

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
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
    }
  }
}

Cypress.Commands.add('getByTestId', getByTestId)
Cypress.Commands.add('getByAriaLabel', getByAriaLabel)
Cypress.Commands.add('checkAccessibility', checkAccessibility)
Cypress.Commands.add('clearInterceptList', clearInterceptList)
