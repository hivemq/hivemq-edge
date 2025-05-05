/// <reference types="cypress" />

import { ReactFlowProvider } from '@xyflow/react'
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
    cy.get("[role='group']").find('button').as('toolbox')
    cy.get('@toolbox').should('have.length', 5)
    cy.get('@toolbox').eq(0).should('have.attr', 'aria-label', 'Zoom in')
    cy.get('@toolbox').eq(1).should('have.attr', 'aria-label', 'Zoom out')
    cy.get('@toolbox').eq(2).should('have.attr', 'aria-label', 'Fit to the canvas')
    cy.get('@toolbox').eq(3).should('have.attr', 'aria-label', 'Lock the canvas')
    cy.get('@toolbox').eq(4).should('have.attr', 'aria-label', 'Help using the Designer')
  })
})
