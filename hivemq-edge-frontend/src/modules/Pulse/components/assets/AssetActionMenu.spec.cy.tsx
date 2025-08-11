import { MOCK_PULSE_ASSET } from '@/api/hooks/usePulse/__handlers__'
import { AssetActionMenu } from '@/modules/Pulse/components/assets/AssetActionMenu.tsx'

describe('AssetActionMenu', () => {
  beforeEach(() => {
    cy.viewport(350, 600)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<AssetActionMenu asset={MOCK_PULSE_ASSET} />)

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('assets-action-edit').should('be.visible')
    cy.getByTestId('assets-action-delete').should('be.visible')

    cy.get('body').click(0, 0)
    cy.getByTestId('device-action-start').should('not.exist')
  })

  it('should trigger edit action', () => {
    const onEdit = cy.stub().as('onEdit')

    cy.mountWithProviders(<AssetActionMenu asset={MOCK_PULSE_ASSET} onEdit={onEdit} />)

    cy.get('@onEdit').should('not.have.been.called')
    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('assets-action-edit').click()
    cy.get('@onEdit').should('have.been.calledWith', '3b028f58-f949-4de1-9b8b-c1a35b1643a4')
    cy.getByTestId('assets-action-start').should('not.exist')
  })

  it('should trigger delete action', () => {
    const onDelete = cy.stub().as('onDelete')

    cy.mountWithProviders(<AssetActionMenu asset={MOCK_PULSE_ASSET} onDelete={onDelete} />)

    cy.get('@onDelete').should('not.have.been.called')
    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('assets-action-delete').click()
    cy.get('@onDelete').should('have.been.calledWith', '3b028f58-f949-4de1-9b8b-c1a35b1643a4')
    cy.getByTestId('assets-action-start').should('not.exist')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<AssetActionMenu asset={MOCK_PULSE_ASSET} />)
    cy.getByAriaLabel('Actions').click()
    cy.checkAccessibility(undefined, {
      rules: {
        'color-contrast': { enabled: false },
      },
    })
  })
})
