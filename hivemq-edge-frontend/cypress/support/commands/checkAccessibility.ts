/// <reference types="cypress" />
import type { ContextObject, Result } from 'axe-core'
import type { Options } from 'cypress-axe'
import { ignoreGlobalRules } from '../a11y.ts'

const violationCallback = (violations: Result[]) => {
  for (const violation of violations) {
    for (const node of violation.nodes) {
      cy.log(violation.id, node.target, node.html)
    }
  }
}

export const checkAccessibility = (
  context?: string | Node | ContextObject | undefined,
  options?: Options | undefined,
  skipFailures?: boolean
): void => {
  const { rules, ...rest } = options || {}
  const allRules = { ...rules, ...ignoreGlobalRules.rules }
  Object.keys(allRules).length > 0
    ? cy.log(`A11y test will ignore the following rules: ${Object.keys(allRules)}`)
    : cy.log('No elements are being ignored')

  cy.checkA11y(context, { rules: { ...rules, ...ignoreGlobalRules.rules }, ...rest }, violationCallback, skipFailures)
}
