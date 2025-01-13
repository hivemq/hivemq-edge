/// <reference types="cypress" />

import { ToolboxNodes } from './ToolboxNodes.tsx'

describe('ToolboxNodes', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render the toolbox', () => {
    cy.mountWithProviders(<ToolboxNodes />)

    cy.getByAriaLabel('Policy controls').find('[role="group"]').as('policyControlsGroups')

    cy.get('@policyControlsGroups').should('have.length', 4)
    cy.get('@policyControlsGroups').eq(0).should('contain.text', 'Edge Integration')
    cy.get('@policyControlsGroups').eq(1).should('contain.text', 'Data Policy')
    cy.get('@policyControlsGroups').eq(2).should('contain.text', 'Behavior Policy')
    cy.get('@policyControlsGroups').eq(3).should('contain.text', 'Operation')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ToolboxNodes />)

    cy.checkAccessibility()
  })
})
