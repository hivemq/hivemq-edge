/// <reference types="cypress" />

import SourceLink from './SourceLink.tsx'
import { mockEdgeEvent } from '@/api/hooks/useEvents/__handlers__'

describe('SourceLink', () => {
  beforeEach(() => {
    cy.viewport(1200, 800)
  })

  it('should render the bridge component', () => {
    cy.mountWithProviders(<SourceLink source={mockEdgeEvent(5)[0].source} />)

    cy.get('a').should('contain.text', 'BRIDGE-0')
    cy.get('a').should('have.attr', 'href', '/mqtt-bridges/BRIDGE-0')
  })

  it('should render the adapter component', () => {
    cy.mountWithProviders(<SourceLink source={mockEdgeEvent(5)[1].source} />)

    cy.get('a').should('contain.text', 'ADAPTER-1')
    cy.get('a').should('have.attr', 'href', '/protocol-adapters/ADAPTER-1')
  })

  it('should render the adapter type component', () => {
    cy.mountWithProviders(<SourceLink source={mockEdgeEvent(5)[2].source} />)

    cy.get('div').should('contain.text', 'ADAPTER_TYPE-2')
    cy.get('a').should('not.exist')
  })

  it('should render the event component', () => {
    cy.mountWithProviders(<SourceLink source={mockEdgeEvent(5)[3].source} />)

    cy.get('div').should('contain.text', 'EVENT-3')
    cy.get('a').should('not.exist')
  })

  it('should render the user component', () => {
    cy.mountWithProviders(<SourceLink source={mockEdgeEvent(5)[4].source} />)

    cy.get('div').should('contain.text', 'USER-4')
    cy.get('a').should('not.exist')
  })
})
