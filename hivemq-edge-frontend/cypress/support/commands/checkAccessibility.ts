/// <reference types="cypress" />
import type { ContextObject, Result, RuleObject } from 'axe-core'
import type { Options } from 'cypress-axe'
import { ignoreGlobalRules } from '../a11y.ts'

const violationCallback = (violations: Result[]) => {
  for (const violation of violations) {
    for (const node of violation.nodes) {
      cy.log(violation.id, node.target, node.html)
    }
  }
}

// In component tests Chakra renders modals/popovers/menus into a portal attached to the document
// body. In the isolated mount that portal does not inherit the color-mode CSS variables that scope
// the themed text colors, so axe blends an incorrect background and reports false light-on-light
// `color-contrast` violations (see the chakra-ui 2.10 upgrade). The real application renders these
// colors correctly, and color contrast is still verified by the e2e (full-app) suite, so the rule is
// only skipped for component tests.
const componentOnlyIgnoredRules: RuleObject =
  Cypress.testingType === 'component' ? { 'color-contrast': { enabled: false } } : {}

export const checkAccessibility = (
  context?: string | Node | ContextObject | undefined,
  options?: Options | undefined,
  skipFailures?: boolean
): void => {
  const { rules, ...rest } = options || {}
  const allRules = { ...rules, ...ignoreGlobalRules.rules, ...componentOnlyIgnoredRules }
  Object.keys(allRules).length > 0
    ? cy.log(`A11y test will ignore the following rules: ${Object.keys(allRules)}`)
    : cy.log('No elements are being ignored')

  cy.checkA11y(context, { rules: allRules, ...rest }, violationCallback, skipFailures)
}
