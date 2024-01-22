/// <reference types="cypress" />

import PolicyEditor from './PolicyEditor.tsx'
import { ReactFlowProvider } from 'reactflow'
import { Route, Routes } from 'react-router-dom'
import { Box } from '@chakra-ui/react'

describe('PolicyEditor', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/frontend/capabilities', [])
    // cy.intercept('/api/v1/management/events?*', []).as('getEvents')
  })

  it('should render an error with the wrong route', () => {
    cy.mountWithProviders(<PolicyEditor />)
    cy.get('[role="alert"] div[data-status="error"]').eq(0).should('contain.text', 'Not identified')
    cy.get('[role="alert"] div[data-status="error"]').eq(1).should('contain.text', 'The policy is not a valid document')
  })

  it('should render properly', () => {
    cy.mountWithProviders(
      <ReactFlowProvider>
        <Routes>
          <Route
            path="/:policyType/:policyId"
            element={
              <Box w={'100%'} h={'95vh'}>
                <PolicyEditor />
              </Box>
            }
          ></Route>
        </Routes>
      </ReactFlowProvider>,
      { routerProps: { initialEntries: ['/BEHAVIOR/1'] } }
    )

    cy.get('[role="toolbar"]').should('have.attr', 'aria-label', 'Workspace toolbar')
    cy.getByAriaLabel('Open the toolbox').should('be.visible')
    cy.getByTestId('rf__minimap').should('be.visible')
    cy.getByAriaLabel('Canvas controls').find('button').should('have.length', 4)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <ReactFlowProvider>
        <Routes>
          <Route
            path="/:policyType/:policyId"
            element={
              <Box w={'100%'} h={'95vh'}>
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
