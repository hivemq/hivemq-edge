/// <reference types="cypress" />

import { ReactFlowProvider } from 'reactflow'
import { Route, Routes } from 'react-router-dom'
import { ToolboxNodes } from './ToolboxNodes.tsx'

describe('Toolbox', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render the toolbox', () => {
    cy.mountWithProviders(
      <ReactFlowProvider>
        <Routes>
          <Route path="/node/:type/:nodeId" element={<ToolboxNodes />}></Route>
        </Routes>
      </ReactFlowProvider>,
      { routerProps: { initialEntries: ['/node/Unknown/1'] } }
    )
    cy.getByTestId('toolbox-trigger').should('have.attr', 'aria-expanded', 'false')
    cy.getByTestId('toolbox-container').should('not.be.visible')

    cy.getByTestId('toolbox-trigger').click()
    cy.getByTestId('toolbox-trigger').should('have.attr', 'aria-expanded', 'true')
    cy.getByTestId('toolbox-container').should('be.visible')
    cy.getByTestId('toolbox-container').find('button').should('have.length', 9)

    cy.getByAriaLabel('Policy controls').find('[role="group"]').as('policyControlsGroups')

    cy.get('@policyControlsGroups').should('have.length', 4)
    cy.get('@policyControlsGroups').eq(0).should('contain.text', 'Pipeline')
    cy.get('@policyControlsGroups').eq(1).should('contain.text', 'Data Policy')
    cy.get('@policyControlsGroups').eq(2).should('contain.text', 'Behavior Policy')
    cy.get('@policyControlsGroups').eq(3).should('contain.text', 'Operation')
  })
})
