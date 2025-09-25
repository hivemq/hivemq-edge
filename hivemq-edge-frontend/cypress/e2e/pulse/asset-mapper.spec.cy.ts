import { drop } from '@mswjs/data'

import { loginPage, homePage, assetsPage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { cy_interceptPulseWithMockDB, getPulseFactory } from 'cypress/utils/intercept-pulse.utils.ts'
import { ONBOARDING } from '../../utils/constants.utils.ts'

describe('Pulse Assets', () => {
  const mswDB = getPulseFactory()

  beforeEach(() => {
    drop(mswDB)

    cy_interceptCoreE2E()
    cy_interceptPulseWithMockDB(mswDB, true)

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    homePage.navLink.click()
  })

  describe('Assets Management', () => {
    it('should render assets', () => {
      homePage.taskSectionTitle(ONBOARDING.TASK_PULSE, 0).should('contain.text', 'Pulse is currently active.')

      homePage.pulseOnboarding.title.should('contain.text', 'Stay up-to-date with your asset mappings')

      const expectedTodoSummary = [2, 0, 0]
      homePage.pulseOnboarding.todos.should('have.length', expectedTodoSummary.length)
      homePage.pulseOnboarding.todosSummary.each(($element, idx) => {
        cy.wrap($element).should('contain.text', expectedTodoSummary[idx])
      })
      homePage.pulseOnboarding.todos.eq(0).find('a').click()

      assetsPage.location.should('equal', '/app/pulse-assets')
      assetsPage.table.rows.should('have.length', 2)

      assetsPage.search.input.type('1234')
      assetsPage.search.clear.click()

      assetsPage.search.filter(0).should('not.exist')
      assetsPage.search.filter('topic').type('topic{enter}')
      assetsPage.search.clearFilter('status').click()
      assetsPage.search.clearAll.click()

      assetsPage.table.actions(0).should('have.attr', 'aria-haspopup', 'menu')
      assetsPage.table.action(0, 'map').click()
    })
    it.skip('should relate assets to asset mappers', () => {})
  })

  describe('Asset Mapping', () => {
    it.skip('should create a new asset mapper', () => {})
    it.skip('should add an asset to an existing mapper', () => {})
    it.skip('should remove an existing mapping', () => {})
    it.skip('should change an existing mapping', () => {})
  })
})
