import { ReactFlowProvider } from '@xyflow/react'
import CanvasToolbar from '@/modules/Workspace/components/controls/CanvasToolbar.tsx'

describe('CanvasToolbar', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<CanvasToolbar />, {
      wrapper: ({ children }: { children: JSX.Element }) => <ReactFlowProvider>{children}</ReactFlowProvider>,
    })

    cy.getByTestId('canvas-toolbar').should('be.visible')
    cy.getByTestId('workspace-search').should('be.visible')
  })
})
