/// <reference types="cypress" />

import { ReactFlowProvider } from 'reactflow'
import CanvasControls from './CanvasControls.tsx'

describe('CanvasControls', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(
      <ReactFlowProvider>
        <CanvasControls />
      </ReactFlowProvider>
    )
    cy.get("[role='group']").find('button').should('have.length', 4)
    cy.get("[role='group']").find('button').eq(0).should('have.attr', 'aria-label', 'Zoom in')
    cy.get("[role='group']").find('button').eq(1).should('have.attr', 'aria-label', 'Zoom out')
    cy.get("[role='group']").find('button').eq(2).should('have.attr', 'aria-label', 'Fit to the canvas')
    cy.get("[role='group']").find('button').eq(3).should('have.attr', 'aria-label', 'Lock the canvas')
  })
})
