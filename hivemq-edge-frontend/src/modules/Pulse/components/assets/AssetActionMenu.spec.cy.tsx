import { MOCK_PULSE_ASSET, MOCK_PULSE_ASSET_MAPPED } from '@/api/hooks/usePulse/__handlers__'
import { AssetActionMenu } from '@/modules/Pulse/components/assets/AssetActionMenu.tsx'

describe('AssetActionMenu', () => {
  beforeEach(() => {
    cy.viewport(350, 600)
  })

  it('should render unmapped properly', () => {
    cy.mountWithProviders(<AssetActionMenu asset={MOCK_PULSE_ASSET} />)

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('assets-action-view').should('have.text', 'View details').should('not.be.disabled')
    cy.getByTestId('assets-action-map').should('have.text', 'Map the asset').should('not.be.disabled')
    cy.getByTestId('assets-action-delete').should('have.text', 'Delete mapping').should('be.disabled')
    cy.getByTestId('assets-action-mapper').should('have.text', 'Open asset mapper').should('be.disabled')

    cy.get('body').click(0, 0)
    cy.getByTestId('assets-action-view').should('not.exist')
  })

  it('should render mapped properly', () => {
    cy.mountWithProviders(<AssetActionMenu asset={MOCK_PULSE_ASSET_MAPPED} />)

    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('assets-action-view').should('not.be.disabled')
    cy.getByTestId('assets-action-map').should('be.disabled')
    cy.getByTestId('assets-action-delete').should('not.be.disabled')
    cy.getByTestId('assets-action-mapper').should('not.be.disabled')

    // cy.get('body').click(0, 0)
    // cy.getByTestId('assets-action-view').should('not.exist')
  })

  it('should trigger view action', () => {
    const onView = cy.stub().as('onView')

    cy.mountWithProviders(<AssetActionMenu asset={MOCK_PULSE_ASSET} onView={onView} />)

    cy.get('@onView').should('not.have.been.called')
    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('assets-action-view').click()
    cy.get('@onView').should('have.been.calledWith', '3b028f58-f949-4de1-9b8b-c1a35b1643a4')
    cy.getByTestId('assets-action-view').should('not.exist')
  })

  it('should trigger edit action', () => {
    const onEdit = cy.stub().as('onEdit')

    cy.mountWithProviders(<AssetActionMenu asset={MOCK_PULSE_ASSET} onEdit={onEdit} />)

    cy.get('@onEdit').should('not.have.been.called')
    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('assets-action-map').click()
    cy.get('@onEdit').should('have.been.calledWith', '3b028f58-f949-4de1-9b8b-c1a35b1643a4')
    cy.getByTestId('assets-action-map').should('not.exist')
  })

  it('should trigger delete action', () => {
    const onDelete = cy.stub().as('onDelete')

    cy.mountWithProviders(<AssetActionMenu asset={MOCK_PULSE_ASSET_MAPPED} onDelete={onDelete} />)

    cy.get('@onDelete').should('not.have.been.called')
    cy.getByAriaLabel('Actions').click()
    cy.getByTestId('assets-action-delete').click()
    cy.get('@onDelete').should('have.been.calledWith', '3b028f58-f949-4de1-9b8b-c1a35b1643a5')
    cy.getByTestId('assets-action-delete').should('not.exist')
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
