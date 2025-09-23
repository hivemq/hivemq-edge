import { MOCK_PULSE_ASSET_LIST } from '@/api/hooks/usePulse/__handlers__'
import AssetMonitoringBadge from '@/modules/Pulse/components/assets/AssetMonitoringBadge.tsx'

describe('AssetMonitoringBadge', () => {
  beforeEach(() => {
    cy.viewport(350, 600)
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getAssets')
    cy.mountWithProviders(<AssetMonitoringBadge />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.getByTestId('asset-monitoring-unattended').should('have.text', '3')
  })

  it('should handle errors', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', { statusCode: 404 }).as('getAssets')
    cy.mountWithProviders(<AssetMonitoringBadge />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.getByTestId('asset-monitoring-unattended').should('have.text', '?')
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST)
    cy.injectAxe()
    cy.mountWithProviders(<AssetMonitoringBadge />)
    cy.checkAccessibility()
  })
})
