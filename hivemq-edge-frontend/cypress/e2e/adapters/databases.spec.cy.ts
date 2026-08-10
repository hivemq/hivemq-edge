import { MOCK_PROTOCOL_DATABASES } from '@/__test-utils__/adapters/databases.ts'
import { loginPage, adapterPage } from 'cypress/pages'

import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('Databases Protocol Adapter', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_DATABASES] }).as('getProtocols')

    loginPage.visit('/app/protocol-adapters/catalog/new/databases')
    loginPage.loginButton.click()
    adapterPage.navLink.click()
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.wait('@getAdapters')
    // Disable CSS transitions so axe does not capture mid-animation contrast values
    cy.document().then((doc) => {
      const style = doc.createElement('style')
      style.textContent =
        '*, *::before, *::after { transition-duration: 0ms !important; animation-duration: 0ms !important; }'
      doc.head.appendChild(style)
    })

    // While the adapter list is loading, the table is filled with four placeholder rows and every
    // cell is wrapped in a Chakra <Skeleton>. A loading skeleton paints its text at `opacity: 0.7`
    // over a grey fill, so axe blends the two and reports a `color-contrast` violation against
    // placeholder text that is not meant to be read. `cy.wait` above only proves the response
    // arrived, not that React has re-rendered from it, which is the window this raced in on CI.
    // Chakra keeps the `chakra-skeleton` class once loaded and only drops the opacity, so wait for
    // the placeholders to settle rather than for them to disappear.
    cy.get('.chakra-skeleton').should(($skeletons) => {
      const stillLoading = $skeletons
        .toArray()
        .filter((element) => element.ownerDocument.defaultView?.getComputedStyle(element).opacity !== '1')
      expect(stillLoading, 'skeleton placeholders still loading').to.have.length(0)
    })

    adapterPage.addNewAdapter.should('be.visible')
    cy.checkAccessibility()

    adapterPage.addNewAdapter.click()
    adapterPage.protocols.list.should('be.visible')
    cy.checkAccessibility()
  })
})
