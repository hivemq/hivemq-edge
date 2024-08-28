/// <reference types="cypress" />

import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { Adapter, Status } from '@/api/__generated__'
import AdapterActionMenu from '@/modules/ProtocolAdapters/components/adapters/AdapterActionMenu.tsx'

describe('AdapterActionMenu', () => {
  beforeEach(() => {
    cy.viewport(350, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<AdapterActionMenu adapter={mockAdapter} />)

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('device-action-start').should('be.visible')
    cy.getByTestId('adapter-action-create').should('be.visible')
    cy.getByTestId('adapter-action-edit').should('be.visible')
    cy.getByTestId('adapter-action-delete').should('be.visible')
    cy.getByTestId('adapter-action-export').should('be.visible')

    cy.get('body').click(0, 0)
    cy.getByTestId('device-action-start').should('not.be.visible')
  })

  it('should render connected status properly', () => {
    cy.mountWithProviders(<AdapterActionMenu adapter={mockAdapter} />)

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('device-action-start').should('be.visible').should('have.text', 'Start')
  })

  it('should render start status properly', () => {
    const adapter: Adapter = {
      ...mockAdapter,
      status: {
        ...mockAdapter.status,
        connection: Status.connection.DISCONNECTED,
      },
    }
    cy.mountWithProviders(<AdapterActionMenu adapter={adapter} />)

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('device-action-start').should('be.visible').should('have.text', 'Start')
  })

  it('should trigger actions', () => {
    const onEdit = cy.stub().as('onEdit')
    const onCreate = cy.stub().as('onCreate')
    const onDelete = cy.stub().as('onDelete')
    const onViewWorkspace = cy.stub().as('onViewWorkspace')
    const onExport = cy.stub().as('onExport')

    cy.mountWithProviders(
      <AdapterActionMenu
        adapter={mockAdapter}
        onEdit={onEdit}
        onCreate={onCreate}
        onDelete={onDelete}
        onViewWorkspace={onViewWorkspace}
        onExport={onExport}
      />
    )

    cy.get('@onCreate').should('not.have.been.called')
    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('adapter-action-create').click()
    cy.get('@onCreate').should('have.been.calledWith', 'simulation')
    cy.getByTestId('adapter-action-create').should('not.be.visible')

    cy.get('@onEdit').should('not.have.been.called')
    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('adapter-action-edit').click()
    cy.get('@onEdit').should('have.been.calledWith', 'my-adapter', 'simulation')
    cy.getByTestId('adapter-action-edit').should('not.be.visible')

    cy.get('@onDelete').should('not.have.been.called')
    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('adapter-action-delete').click()
    cy.get('@onDelete').should('have.been.calledWith', 'my-adapter')
    cy.getByTestId('adapter-action-delete').should('not.be.visible')

    cy.get('@onViewWorkspace').should('not.have.been.called')
    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('adapter-action-workspace').click()
    cy.get('@onViewWorkspace').should('have.been.calledWith', 'my-adapter', 'simulation')
    cy.getByTestId('adapter-action-workspace').should('not.be.visible')

    cy.get('@onExport').should('not.have.been.called')
    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('adapter-action-export').click()
    cy.get('@onExport').should('have.been.calledWith', 'my-adapter', 'simulation')
    cy.getByTestId('adapter-action-export').should('not.be.visible')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<AdapterActionMenu adapter={mockAdapter} />)
    cy.getByAriaLabel('Actions').click()
    cy.checkAccessibility(undefined, {
      rules: {
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Component: AdapterActionMenu')
  })
})
