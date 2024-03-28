/// <reference types="cypress" />

import { ReactFlowProvider } from 'reactflow'
import { Route, Routes } from 'react-router-dom'
import PropertyPanelController from './PropertyPanelController.tsx'

describe('PropertyPanelController', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { statusCode: 404 })
  })

  it('should display an error panel without an action', () => {
    cy.mountWithProviders(
      <ReactFlowProvider>
        <Routes>
          <Route path="/node/:type/:nodeId" element={<PropertyPanelController />}></Route>
        </Routes>
      </ReactFlowProvider>,
      { routerProps: { initialEntries: ['/node/Unknown/1'] } }
    )

    cy.getByTestId('node-editor-content').should('be.visible')
    cy.getByTestId('node-editor-name').should('contain.text', 'Unknown type')
    cy.getByTestId('node-editor-icon').find('svg').should('have.attr', 'aria-label', 'Unknown type')
    cy.getByTestId('node-editor-id').should('contain.text', '1')

    cy.getByTestId('node-editor-under-construction').should('be.visible')
    cy.get('button[type="submit"]').should('not.exist')
  })

  it('should render a panel with a submit button', () => {
    cy.mountWithProviders(
      <ReactFlowProvider>
        <Routes>
          <Route path="/node/:type/:nodeId" element={<PropertyPanelController />}></Route>
        </Routes>
      </ReactFlowProvider>,
      { routerProps: { initialEntries: ['/node/TOPIC_FILTER/1'] } }
    )

    cy.getByTestId('node-editor-under-construction').should('not.exist')
    cy.get('button[type="submit"]').should('be.visible')
  })
})
