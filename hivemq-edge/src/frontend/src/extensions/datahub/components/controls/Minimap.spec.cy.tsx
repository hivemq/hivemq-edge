/// <reference types="cypress" />

import { ReactFlowProvider } from 'reactflow'
import Minimap from './Minimap.tsx'

describe('Minimap', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(
      <ReactFlowProvider>
        <Minimap position="top-left" />
      </ReactFlowProvider>
    )
  })
})
