/// <reference types="cypress" />

import { ReactFlowProvider } from 'reactflow'
import { Route, Routes } from 'react-router-dom'
import { Box } from '@chakra-ui/react'

import PolicyEditor from './PolicyEditor.tsx'

describe('PolicyEditor', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/frontend/capabilities', [])
  })

  it('should render properly', () => {
    cy.mountWithProviders(
      <ReactFlowProvider>
        <Routes>
          <Route
            path="/:policyType/:policyId"
            element={
              <Box w="100%" h="95vh">
                <PolicyEditor />
              </Box>
            }
          ></Route>
        </Routes>
      </ReactFlowProvider>,
      { routerProps: { initialEntries: ['/BEHAVIOR/1'] } }
    )

    cy.get('[role="toolbar"]').should('have.attr', 'aria-label', 'Policy Designer toolbars')
    cy.getByAriaLabel('Open the toolbox').should('be.visible')
    cy.getByTestId('rf__minimap').should('be.visible')
    cy.getByAriaLabel('Canvas controls').find('button').should('have.length', 5)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <ReactFlowProvider>
        <Routes>
          <Route
            path="/:policyType/:policyId"
            element={
              <Box w="100%" h="95vh">
                <PolicyEditor />
              </Box>
            }
          ></Route>
        </Routes>
      </ReactFlowProvider>,
      { routerProps: { initialEntries: ['/BEHAVIOR/1'] } }
    )

    cy.checkAccessibility()
    cy.percySnapshot('Component: PolicyEditor')
  })
})
