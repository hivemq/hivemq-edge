import { WrapperTestRoute } from '@/__test-utils__/hooks/WrapperTestRoute.tsx'
import ProtocolAdapterPage from '@/modules/ProtocolAdapters/ProtocolAdapterPage.tsx'

describe('ProtocolAdapterPage', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render the page header', () => {
    cy.mountWithProviders(<ProtocolAdapterPage />, {
      wrapper: WrapperTestRoute,
      routerProps: { initialEntries: [`/protocol-adapters`] },
    })

    cy.getByTestId('page-container-header').find('h1').should('have.text', 'Protocol Adapters')

    cy.getByTestId('page-container-header')
      .find('h1 + p')
      .should(
        'contain.text',
        'Protocol adapters allow you to connect your HiveMQ Edge installation to local and remote devices that use communication protocols other than MQTT. Once a connection is established, you can seamlessly consolidate all device data into your HiveMQ Edge MQTT broker.'
      )

    cy.getByTestId('test-pathname').should('have.text', '/protocol-adapters')

    cy.getByTestId('page-container-cta').find('button').should('have.text', 'Add a new adapter')
    cy.getByTestId('page-container-cta').find('button').click()

    cy.getByTestId('page-container-cta').find('button').should('have.text', 'Back to active adapters')
    cy.getByTestId('test-pathname').should('have.text', '/protocol-adapters/catalog')

    cy.getByTestId('page-container-cta').find('button').click()
    cy.getByTestId('test-pathname').should('have.text', '/protocol-adapters')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ProtocolAdapterPage />, {
      wrapper: WrapperTestRoute,
      routerProps: { initialEntries: [`/protocol-adapters`] },
    })
    cy.checkAccessibility()
  })
})
