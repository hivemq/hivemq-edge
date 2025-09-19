import { MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'
import { MOCK_PULSE_ASSET_LIST, MOCK_PULSE_ASSET_MAPPED_UNIQUE } from '@/api/hooks/usePulse/__handlers__'
import AssetNameCell from '@/modules/Pulse/components/assets/AssetNameCell.tsx'

describe('AssetNameCell', () => {
  beforeEach(() => {
    cy.viewport(1000, 600)
    cy.intercept('/api/v1/frontend/capabilities', { items: [MOCK_CAPABILITY_PULSE_ASSETS] })
  })

  it('should render errors', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', { statusCode: 404 }).as('getStatus')

    cy.mountWithProviders(<AssetNameCell assetId={MOCK_PULSE_ASSET_MAPPED_UNIQUE.id} />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getStatus')

    cy.getByTestId('asset-error').should('have.text', '< not found >')
  })

  it('should render errors', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getStatus')

    cy.mountWithProviders(<AssetNameCell />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getStatus')

    cy.getByTestId('asset-error').should('have.text', '< unset >')
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getStatus')

    cy.mountWithProviders(<AssetNameCell assetId={MOCK_PULSE_ASSET_MAPPED_UNIQUE.id} />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getStatus')

    cy.getByTestId('asset-name').should('have.text', 'Almost the same asset')
  })

  it('should render description properly', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getStatus')

    cy.mountWithProviders(<AssetNameCell assetId={MOCK_PULSE_ASSET_MAPPED_UNIQUE.id} showDescription />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getStatus')

    cy.getByTestId('asset-name').should('have.text', 'Almost the same asset')
    cy.getByTestId('asset-description').should('have.text', 'Not sure how to describe that re-mapped asset')
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getStatus')
    cy.injectAxe()

    cy.mountWithProviders(<AssetNameCell assetId={MOCK_PULSE_ASSET_MAPPED_UNIQUE.id} showDescription />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getStatus')
    cy.checkAccessibility()
  })
})
