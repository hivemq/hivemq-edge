/// <reference types="cypress" />

import { ReactFlowProvider } from 'reactflow'
import PropertyPanelController from '@/extensions/datahub/components/controls/PropertyPanelController.tsx'
import { Route, Routes } from 'react-router-dom'

describe('PropertyPanelController', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should renders properly the side panel', () => {
    cy.mountWithProviders(
      <ReactFlowProvider>
        <Routes>
          <Route path="/node/:type/:nodeId" element={<PropertyPanelController />}></Route>
        </Routes>
      </ReactFlowProvider>,
      { routerProps: { initialEntries: ['/node/TOPIC_FILTER/1'] } }
    )

    cy.getByTestId('node-editor-content').should('be.visible')
    cy.getByTestId('node-editor-name').should('contain.text', 'Topic Filter')
    cy.getByTestId('node-editor-icon').find('svg').should('have.attr', 'aria-label', 'Topic Filter')
    cy.getByTestId('node-editor-id').should('contain.text', '1')
    cy.getByTestId('node-editor-under-construction').should('be.visible')
  })
})
