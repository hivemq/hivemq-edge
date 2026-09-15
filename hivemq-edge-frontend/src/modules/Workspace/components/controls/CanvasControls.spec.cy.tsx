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

  it('should carry the reload control beside, not inside, the attached group', () => {
    cy.mountWithProviders(<CanvasControls />, { wrapper })

    // The attached group keeps its five icon controls: zoom in, zoom out, fit, lock, options.
    cy.get('[role="group"]').last().find('button').should('have.length', 5)

    // The reload control sits outside that group and carries a visible label, so it can be found.
    cy.getByTestId('reload-workspace-trigger').should('be.visible').should('contain.text', 'Reload workspace')
    cy.get('[role="group"]').last().find('[data-testid="reload-workspace-trigger"]').should('not.exist')
  })

  it('should open the reload dialog from the canvas controls', () => {
    cy.mountWithProviders(<CanvasControls />, { wrapper })

    cy.getByTestId('reload-workspace-dialog').should('not.exist')
    cy.getByTestId('reload-workspace-trigger').click()
    cy.getByTestId('reload-workspace-dialog').should('be.visible')
  })
})
