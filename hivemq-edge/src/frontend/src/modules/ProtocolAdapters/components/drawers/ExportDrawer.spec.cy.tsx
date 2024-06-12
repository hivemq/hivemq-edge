import { Route, Routes } from 'react-router-dom'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

import ExportDrawer from '@/modules/ProtocolAdapters/components/drawers/ExportDrawer.tsx'

const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <Routes>
      <Route path="/protocol-adapters/:adapterId/export" element={children}></Route>
    </Routes>
  )
}

describe('ExportDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] })
    cy.intercept('api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ExportDrawer />, {
      wrapper: Wrapper,
      routerProps: { initialEntries: [`/protocol-adapters/${MOCK_ADAPTER_ID}/export`] },
    })
    cy.checkAccessibility()
    cy.percySnapshot('Component: ExportDrawer')
  })
})
