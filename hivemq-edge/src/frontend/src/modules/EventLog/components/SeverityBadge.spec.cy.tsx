/// <reference types="cypress" />

import { mockEdgeEvent } from '@/api/hooks/useEvents/__handlers__'
import SeverityBadge from './SeverityBadge.tsx'

describe('SeverityBadge', () => {
  beforeEach(() => {
    cy.viewport(1200, 800)
  })

  it('should render the adapter component', () => {
    cy.mountWithProviders(<SeverityBadge event={mockEdgeEvent(5)[0]} />)
    cy.get('[data-status]').should('contain.text', 'INFO')
  })
})
