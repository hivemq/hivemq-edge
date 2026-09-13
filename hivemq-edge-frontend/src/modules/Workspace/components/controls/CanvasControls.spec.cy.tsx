import type { ReactElement } from 'react'
import { ReactFlowProvider } from '@xyflow/react'

import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider'
import CanvasControls from './CanvasControls.tsx'

const wrapper = ({ children }: { children: ReactElement }) => (
  <EdgeFlowProvider>
    <ReactFlowProvider>{children}</ReactFlowProvider>
  </EdgeFlowProvider>
)

describe('CanvasControls', () => {
  beforeEach(() => {
    cy.viewport(400, 250)
    cy.intercept('/api/v1/frontend/capabilities', { statusCode: 200, body: { items: [] }, log: false })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { statusCode: 200, body: { items: [] }, log: false })
    cy.intercept('/api/v1/management/protocol-adapters/types', { statusCode: 200, body: { items: [] }, log: false })
    cy.intercept('/api/v1/management/bridges', { statusCode: 200, body: { items: [] }, log: false })
    cy.intercept('/api/v1/gateway/listeners', { statusCode: 200, body: { items: [] }, log: false })
    cy.intercept('/api/v1/management/combiners', { statusCode: 200, body: { items: [] }, log: false })
    cy.intercept('/api/v1/management/pulse/asset-mappers', { statusCode: 200, body: { items: [] }, log: false })
  })

  it('should carry the reload control alongside the other canvas controls', () => {
    cy.mountWithProviders(<CanvasControls />, { wrapper })

    // The six controls, in order: zoom in, zoom out, fit, lock, options, reload.
    cy.get('[role="group"]').last().find('button').should('have.length', 6)
    cy.get('[role="group"]').last().find('button').eq(5).should('have.attr', 'aria-label', 'Reload workspace')

    cy.getByTestId('reload-workspace-trigger').should('be.visible')
  })

  it('should open the reload dialog from the canvas controls', () => {
    cy.mountWithProviders(<CanvasControls />, { wrapper })

    cy.getByTestId('reload-workspace-dialog').should('not.exist')
    cy.getByTestId('reload-workspace-trigger').click()
    cy.getByTestId('reload-workspace-dialog').should('be.visible')
  })
})
