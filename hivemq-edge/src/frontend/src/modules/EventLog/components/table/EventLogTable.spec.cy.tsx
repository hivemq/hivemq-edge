/// <reference types="cypress" />

import { mockEdgeEvent } from '@/api/hooks/useEvents/__handlers__'
import EventLogTable from '@/modules/EventLog/components/table/EventLogTable.tsx'
import { TypeIdentifier } from '@/api/__generated__'

describe('EventLogTable', () => {
  beforeEach(() => {
    cy.viewport(1200, 800)
    cy.intercept('/api/v1/management/events?*', { items: [...mockEdgeEvent(150)] })
  })

  it('should render the table', () => {
    cy.mountWithProviders(<EventLogTable onOpen={cy.stub()} />)

    cy.get('tbody').find('tr').eq(0).find('td').eq(0).find('button').should('be.visible')
    cy.get('tbody').find('tr').eq(0).find('td').eq(2).should('have.text', 'INFO')
    cy.get('tbody').find('tr').eq(0).find('td').eq(3).should('have.text', 'BRIDGE-0')
  })

  it('should open the summary when clicking on open', () => {
    const mockOnOpen = cy.stub().as('onOpen')

    cy.mountWithProviders(<EventLogTable onOpen={mockOnOpen} />)
    cy.getByAriaLabel('View details of event').eq(0).click()

    cy.get('@onOpen').should('have.been.calledWithMatch', {
      identifier: { identifier: 'EVENT-0', type: TypeIdentifier.type.EVENT },
    } as Partial<Event>)

    cy.getByAriaLabel('View details of event').eq(7).click()

    cy.get('@onOpen').should('have.been.calledWithMatch', {
      identifier: { identifier: 'EVENT-7', type: TypeIdentifier.type.EVENT },
    } as Partial<Event>)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<EventLogTable onOpen={cy.stub()} />)
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[#17144] Color-contrast fixed but still not passing. Flaky with expandable panel
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Component: EventLogTable')
  })
})
