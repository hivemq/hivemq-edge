/// <reference types="cypress" />

import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { ConnectionStatus } from '@/api/__generated__'
import AdapterActionMenu from '@/modules/ProtocolAdapters/components/adapters/AdapterActionMenu.tsx'

describe('AdapterActionMenu', () => {
  beforeEach(() => {
    cy.viewport(350, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<AdapterActionMenu adapter={mockAdapter} />)

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('adapter-action-connect').should('be.visible')
    cy.getByTestId('adapter-action-create').should('be.visible')
    cy.getByTestId('adapter-action-edit').should('be.visible')
    cy.getByTestId('adapter-action-delete').should('be.visible')

    cy.get('body').click(0, 0)
    cy.getByTestId('adapter-action-connect').should('not.be.visible')
  })

  it('should render connected status properly', () => {
    cy.mountWithProviders(<AdapterActionMenu adapter={mockAdapter} />)

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('adapter-action-connect').should('be.visible').should('have.text', 'Disconnect')
  })

  it('should render disconnected status properly', () => {
    const adapter = {
      ...mockAdapter,
      adapterRuntimeInformation: {
        ...mockAdapter.adapterRuntimeInformation,
        connectionStatus: { status: ConnectionStatus.status.DISCONNECTED },
      },
    }
    cy.mountWithProviders(<AdapterActionMenu adapter={adapter} />)

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('adapter-action-connect').should('be.visible').should('have.text', 'Connect')
  })

  it('should trigger actions', () => {
    const onEdit = cy.stub().as('onEdit')
    const onCreate = cy.stub().as('onCreate')
    const onDelete = cy.stub().as('onDelete')

    cy.mountWithProviders(
      <AdapterActionMenu adapter={mockAdapter} onEdit={onEdit} onCreate={onCreate} onDelete={onDelete} />
    )

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('adapter-action-create').click()
    cy.get('@onCreate').should('have.been.calledWith', 'simulation')
    cy.getByTestId('adapter-action-create').should('not.be.visible')

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('adapter-action-edit').click()
    cy.get('@onEdit').should('have.been.calledWith', 'my-adapter', 'simulation')
    cy.getByTestId('adapter-action-edit').should('not.be.visible')

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('adapter-action-delete').click()
    cy.get('@onDelete').should('have.been.calledWith', 'my-adapter')
    cy.getByTestId('adapter-action-delete').should('not.be.visible')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<AdapterActionMenu adapter={mockAdapter} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: AdapterTypeSummary')
  })
})
