/// <reference types="cypress" />

import SeverityBadge from './SeverityBadge.tsx'
import { mockEdgeEvent } from '@/api/hooks/useEvents/__handlers__'

describe('SeverityBadge', () => {
  beforeEach(() => {
    cy.viewport(1200, 800)
  })

  it('should render the adapter component', () => {
    cy.mountWithProviders(<SeverityBadge event={mockEdgeEvent(5)[0]} />)
    cy.get('[data-status]').should('contain.text', 'INFO')
  })
})
