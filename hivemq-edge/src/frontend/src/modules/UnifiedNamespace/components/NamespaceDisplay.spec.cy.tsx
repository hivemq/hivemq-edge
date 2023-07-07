/// <reference types="cypress" />

import NamespaceDisplay from './NamespaceDisplay.tsx'
import { MOCK_BREADCRUMB, MOCK_NAMESPACE } from '@/__test-utils__/mocks.ts'

describe('NamespaceDisplay', () => {
  beforeEach(() => {
    cy.viewport(800, 150)
  })

  const selectors = [
    { test: 'with a full namespace', namespace: MOCK_NAMESPACE, breadcrumb: MOCK_BREADCRUMB },
    {
      test: 'with site omitted',
      namespace: { ...MOCK_NAMESPACE, site: undefined },
      breadcrumb: MOCK_BREADCRUMB.filter((e) => e !== 'Site'),
    },
  ]
  it.each(selectors)(
    (selector) => `should render properly ${selector.test}`,
    (selector) => {
      cy.mountWithProviders(<NamespaceDisplay namespace={selector.namespace} />)

      cy.get('nav').get('ol').children().should('have.length', selector.breadcrumb.length)
      cy.get('nav')
        .get('ol')
        .children()
        .each((e, index) => {
          cy.wrap(e).should('contain.text', selector.breadcrumb[index])
        })
    }
  )

  it('should render properly an empty breadcrumb with an empty namespace', () => {
    cy.mountWithProviders(<NamespaceDisplay namespace={{}} />)
    cy.get('nav').get('ol').children().should('have.length', 0)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<NamespaceDisplay namespace={MOCK_NAMESPACE} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: NamespaceDisplay')
  })

  it('should be accessible for small size', () => {
    cy.injectAxe()
    cy.mountWithProviders(<NamespaceDisplay namespace={MOCK_NAMESPACE} fontSize={'sm'} />)
    cy.checkAccessibility()
  })
})
