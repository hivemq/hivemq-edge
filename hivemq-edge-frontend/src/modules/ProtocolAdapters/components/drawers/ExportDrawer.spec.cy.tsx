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

  it('should render the form', () => {
    cy.mountWithProviders(<ExportDrawer />, {
      wrapper: Wrapper,
      routerProps: { initialEntries: [`/protocol-adapters/${MOCK_ADAPTER_ID}/export`] },
    })

    cy.getByTestId('adapter-export-title').should('contain.text', 'Adapter Export')
    cy.getByTestId('adapter-export-type').should('contain.text', 'Simulated Edge Device')

    cy.getByTestId('field-content-options').find('label').as('options')
    cy.get('@options').should('have.length', 2)
    cy.get('@options').eq(0).should('contain.text', 'Full configuration')
    cy.get('@options').eq(1).should('contain.text', 'Mappings')

    cy.getByTestId('field-format-label').should('contain.text', 'Select the file format of the output')
    cy.get('#field-format').should('contain.text', '.json')

    cy.get('@options').eq(1).click()
    cy.get('#field-format').should('contain.text', '.xlsx')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ExportDrawer />, {
      wrapper: Wrapper,
      routerProps: { initialEntries: [`/protocol-adapters/${MOCK_ADAPTER_ID}/export`] },
    })

    cy.getByTestId('field-content-options').find('label').eq(1).click()
    cy.get('#field-format').click()
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
      },
    })
    cy.percySnapshot('Component: ExportDrawer')
  })
})
