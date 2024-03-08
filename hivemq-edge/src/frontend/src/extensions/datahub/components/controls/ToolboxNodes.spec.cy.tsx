/// <reference types="cypress" />

import { ToolboxNodes } from './ToolboxNodes.tsx'

describe('Toolbox', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render the toolbox', () => {
    cy.mountWithProviders(<ToolboxNodes />)

    cy.getByAriaLabel('Policy controls').find('[role="group"]').as('policyControlsGroups')

    // Behavior policy disabled, see VITE_FLAG_DATAHUB_BEHAVIOR_ENABLED
    cy.get('@policyControlsGroups').should('have.length', 3)
    cy.get('@policyControlsGroups').eq(0).should('contain.text', 'Pipeline')
    cy.get('@policyControlsGroups').eq(1).should('contain.text', 'Data Policy')
    // cy.get('@policyControlsGroups').eq(2).should('contain.text', 'Behavior Policy')
    cy.get('@policyControlsGroups').eq(2).should('contain.text', 'Operation')
  })
})
