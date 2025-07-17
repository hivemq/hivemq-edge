import { WrapperTestRoute } from '@/__test-utils__/hooks/WrapperTestRoute.tsx'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import BridgePage from '@/modules/Bridges/BridgePage.tsx'

describe('BridgePage', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getBridges')
  })

  it('should render properly', () => {
    cy.mountWithProviders(<BridgePage />, {
      wrapper: WrapperTestRoute,
      routerProps: { initialEntries: [`/mqtt-bridge`] },
    })
    cy.wait('@getBridges')

    cy.get('header h1').should('have.text', 'MQTT bridges')
    cy.get('header h1 + p').should(
      'have.text',
      'MQTT bridges let you connect multiple MQTT brokers to enable seamless data sharing between different networks or systems.'
    )

    cy.getByTestId('page-container-cta').within(() => {
      cy.get('button').should('have.text', 'Add bridge connection')
    })
    cy.get('table').should('have.attr', 'aria-label', 'List of bridges')
    cy.get('table tbody tr td').eq(0).should('have.text', 'bridge-id-01')
  })

  it('should create a new bridge by the main CTA', () => {
    cy.mountWithProviders(<BridgePage />, {
      wrapper: WrapperTestRoute,
      routerProps: { initialEntries: [`/mqtt-bridges`] },
    })
    cy.wait('@getBridges')

    cy.getByTestId('test-pathname').should('have.text', '/mqtt-bridges')
    cy.getByTestId('page-container-cta').within(() => {
      cy.get('button').should('have.text', 'Add bridge connection')
      cy.get('button').click()
    })
    cy.getByTestId('test-pathname').should('have.text', '/mqtt-bridges/new')
  })

  it('should be accessible', () => {
    cy.injectAxe()

    cy.mountWithProviders(<BridgePage />)
    cy.wait('@getBridges')

    cy.get('table tbody tr td').eq(0).should('have.text', 'bridge-id-01')

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[#111] Skeleton are not accessible
        'color-contrast': { enabled: false },
      },
    })
  })
})
