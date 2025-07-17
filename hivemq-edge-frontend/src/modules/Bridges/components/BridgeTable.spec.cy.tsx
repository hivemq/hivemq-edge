import { WrapperTestRoute } from '@/__test-utils__/hooks/WrapperTestRoute.tsx'
import { MOCK_CREATED_AT } from '@/__test-utils__/mocks.ts'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { BridgeTable } from '@/modules/Bridges/components/BridgeTable.tsx'
import { DateTime } from 'luxon'

const expectedColumnHeaders = ['ID', 'Local Subscriptions', 'Remote Subscriptions', 'Status', 'Last started', 'Actions']

describe('BridgeTable', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getBridges')

    cy.stub(DateTime, 'now').returns(DateTime.fromISO(MOCK_CREATED_AT).plus({ day: 2 }))
  })

  it('should render properly', () => {
    cy.mountWithProviders(<BridgeTable />, {
      wrapper: WrapperTestRoute,
      routerProps: { initialEntries: [`/mqtt-bridges`] },
    })
    cy.wait('@getBridges')

    cy.get('table').should('have.attr', 'aria-label', 'List of bridges')
    cy.get('table thead th').should('have.length', 6)

    cy.get('table thead th').each((el, index) => {
      cy.wrap(el).should('have.text', expectedColumnHeaders[index])
    })
  })

  it('should render the data', () => {
    cy.mountWithProviders(<BridgeTable />, {
      wrapper: WrapperTestRoute,
      routerProps: { initialEntries: [`/mqtt-bridges`] },
    })
    cy.wait('@getBridges')

    cy.get('table').should('have.attr', 'aria-label', 'List of bridges')
    cy.get('table thead th').should('have.length', 6)

    cy.get('tbody tr').should('have.length', 1)
    cy.get('tbody tr')
      .first()
      .within(() => {
        cy.get('td').should('have.length', 6)
        cy.get('td').eq(0).should('have.text', 'bridge-id-01')
        cy.get('td').eq(1).should('have.text', '1')
        cy.get('td').eq(2).should('have.text', '1')
        cy.get('td').eq(3).should('have.text', 'Connected')
        // This is clearly a bug!
        cy.get('td').eq(4).should('have.text', '1 month ago')
        cy.get('td')
          .eq(5)
          .within(() => {
            cy.getByAriaLabel('Actions').should('have.attr', 'aria-haspopup', 'menu')
            cy.get('[role="menu"]').should('not.be.visible')
            cy.getByAriaLabel('Actions').click()
            cy.get('[role="menu"]').should('be.visible')
            cy.get('[role="menu"]').within(() => {
              cy.get('[role="menuitem"]').should('have.length', 4)
              cy.get('[role="menuitem"]').first().should('have.text', 'Start')
            })
          })
      })
  })

  it('should be accessible', () => {
    cy.injectAxe()

    cy.mountWithProviders(<BridgeTable />)
    cy.wait('@getBridges')

    cy.get('table tbody tr td').eq(0).should('have.text', 'bridge-id-01')

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[#111] Skeleton are not accessible
        'color-contrast': { enabled: false },
      },
    })
  })

  //   cy.get('header h1').should('have.text', 'MQTT bridges')
  //   cy.get('header h1 + p').should(
  //     'have.text',
  //     'MQTT bridges let you connect multiple MQTT brokers to enable seamless data sharing between different networks or systems.'
  //   )
  //
  //   cy.getByTestId('page-container-cta').within(() => {
  //     cy.get('button').should('have.text', 'Add bridge connection')
  //   })
  //   cy.get('table').should('have.attr', 'aria-label', 'List of bridges')
  //   cy.get('table tbody tr td').eq(0).should('have.text', 'bridge-id-01')
  // })
  //
  // it.only('should create a new bridge by the main CTA', () => {
  //   cy.mountWithProviders(<BridgePage />, {
  //     wrapper: WrapperTestRoute,
  //     routerProps: { initialEntries: [`/mqtt-bridge`] },
  //   })
  //   cy.wait('@getBridges')
  //
  //   cy.getByTestId('test-pathname').should('have.text', '/mqtt-bridge')
  //   cy.getByTestId('page-container-cta').within(() => {
  //     cy.get('button').should('have.text', 'Add bridge connection')
  //     cy.get('button').click()
  //   })
  //   cy.getByTestId('test-pathname').should('have.text', '/mqtt-bridge/new')
  // })
  //
  // it('should be accessible', () => {
  //   cy.injectAxe()
  //
  //   cy.mountWithProviders(<BridgeTable />)
  //   cy.wait('@getBridges')
  //
  //   cy.get('table tbody tr td').eq(0).should('have.text', 'bridge-id-01')
  //
  //   cy.checkAccessibility(undefined, {
  //     rules: {
  //       // TODO[#111] Skeleton are not accessible
  //       'color-contrast': { enabled: false },
  //     },
  //   })
})
