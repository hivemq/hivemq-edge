/// <reference types="cypress" />

import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { BridgeActionMenu } from '@/modules/Bridges/components/BridgeActionMenu.tsx'

describe('BridgeActionMenu', () => {
  beforeEach(() => {
    cy.viewport(350, 600)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<BridgeActionMenu bridge={mockBridge} />)

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('device-action-start').should('be.visible')
    cy.getByTestId('bridge-action-edit').should('be.visible')
    cy.getByTestId('bridge-action-delete').should('be.visible')

    cy.get('body').click(0, 0)
    cy.getByTestId('device-action-start').should('not.exist')
  })

  it.only('should trigger actions', () => {
    const onEdit = cy.stub().as('onEdit')

    cy.mountWithProviders(<BridgeActionMenu bridge={mockBridge} onEdit={onEdit} />)

    cy.get('@onEdit').should('not.have.been.called')
    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('bridge-action-edit').click()
    cy.get('@onEdit').should('have.been.calledWith', 'bridge-id-01')
    cy.getByTestId('device-action-start').should('not.exist')
  })

  it.only('should trigger actions', () => {
    const onDelete = cy.stub().as('onDelete')

    cy.mountWithProviders(<BridgeActionMenu bridge={mockBridge} onDelete={onDelete} />)

    cy.get('@onDelete').should('not.have.been.called')
    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('bridge-action-delete').click()
    cy.get('@onDelete').should('have.been.calledWith', 'bridge-id-01')
    cy.getByTestId('device-action-start').should('not.exist')
  })

  it.only('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<BridgeActionMenu bridge={mockBridge} />)
    cy.getByAriaLabel('Actions').click()
    cy.checkAccessibility(undefined, {
      rules: {
        'color-contrast': { enabled: false },
      },
    })
  })
})
