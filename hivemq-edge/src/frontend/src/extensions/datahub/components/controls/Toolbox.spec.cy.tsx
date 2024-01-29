/// <reference types="cypress" />

import { ReactFlowProvider } from 'reactflow'
import { Route, Routes } from 'react-router-dom'
import { Toolbox } from './Toolbox.tsx'

describe('Toolbox', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render the toolbox', () => {
    cy.mountWithProviders(
      <ReactFlowProvider>
        <Routes>
          <Route path="/node/:type/:nodeId" element={<Toolbox />}></Route>
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
    // cy.getByTestId('toolbox-container').find('[role="group"]').eq(0).should('have.', 'fddf')

    cy.getByAriaLabel('Policy controls').find('[role="group"]').should('have.length', 4)
    cy.getByAriaLabel('Policy controls').find('[role="group"]').eq(0).should('contain.text', 'Pipeline')
    cy.getByAriaLabel('Policy controls').find('[role="group"]').eq(1).should('contain.text', 'Data Policy')
    cy.getByAriaLabel('Policy controls').find('[role="group"]').eq(2).should('contain.text', 'Behavior Policy')
    cy.getByAriaLabel('Policy controls').find('[role="group"]').eq(3).should('contain.text', 'Operation')
  })
})
