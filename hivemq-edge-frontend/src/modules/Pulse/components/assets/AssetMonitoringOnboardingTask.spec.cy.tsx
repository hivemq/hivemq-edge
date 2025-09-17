import { WrapperTestRoute } from '@/__test-utils__/hooks/WrapperTestRoute.tsx'
import { MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'
import { MOCK_PULSE_ASSET_LIST, MOCK_PULSE_ASSET_MAPPED } from '@/api/hooks/usePulse/__handlers__'
import AssetMonitoringOnboardingTask from '@/modules/Pulse/components/assets/AssetMonitoringOnboardingTask.tsx'

describe('AssetMonitoringOnboardingTask', () => {
  beforeEach(() => {
    cy.viewport(500, 600)
    cy.intercept('/api/v1/frontend/capabilities', { items: [MOCK_CAPABILITY_PULSE_ASSETS] })
  })

  it('should render properly', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getAssets')
    cy.mountWithProviders(<AssetMonitoringOnboardingTask />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getAssets')
    cy.getByTestId('asset-monitoring-onboarding-todo')
      .should('be.visible')
      .within(() => {
        cy.get('li').should('have.length', 3)
        cy.get('li')
          .eq(0)
          .within(() => {
            cy.getByTestId('asset-monitoring-todo-count').should('have.text', '1')
            cy.getByTestId('asset-monitoring-todo-link').should('have.text', 'One new asset requires mapping')
          })
        cy.get('li')
          .eq(1)
          .within(() => {
            cy.getByTestId('asset-monitoring-todo-count').should('have.text', '2')
            cy.getByTestId('asset-monitoring-todo-link').should('have.text', 'Several assets require remapping')
          })
        cy.get('li')
          .eq(2)
          .within(() => {
            cy.getByTestId('asset-monitoring-todo-count').should('have.text', '0')
            cy.getByTestId('asset-monitoring-todo-text').should('have.text', 'No asset mapping reports errors')
          })
      })
  })

  it('should render all streaming properly', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', {
      items: [MOCK_PULSE_ASSET_MAPPED],
    }).as('getAssets')
    cy.mountWithProviders(<AssetMonitoringOnboardingTask />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getAssets')
    cy.getByTestId('asset-monitoring-onboarding-todo')
      .should('be.visible')
      .within(() => {
        cy.get('li').should('have.length', 1)
        cy.get('li')
          .eq(0)
          .within(() => {
            cy.getByTestId('asset-monitoring-todo-text').should('have.text', 'All assets are streaming')
          })
      })
  })
  it('should render errors', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', { statusCode: 404 }).as('getAssets')
    cy.mountWithProviders(<AssetMonitoringOnboardingTask />)

    cy.getByTestId('loading-spinner').should('be.visible')
    cy.wait('@getAssets')
    cy.get('[role="alert"]')
      .should('be.visible')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'Not Found')
    cy.getByTestId('asset-monitoring-onboarding-todo').should('not.exist')
  })

  it('should navigate properly', () => {
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getAssets')
    cy.mountWithProviders(<AssetMonitoringOnboardingTask />, { wrapper: WrapperTestRoute })

    cy.wait('@getAssets')
    cy.getByTestId('asset-monitoring-onboarding-todo').should('be.visible')
    cy.getByTestId('test-pathname').should('have.text', '/')

    cy.getByTestId('asset-monitoring-onboarding-todo').find('li').eq(0).click()
    cy.getByTestId('test-pathname').should('have.text', '/pulse-assets')
    cy.getByTestId('test-search').should('have.text', '?mapping_status=UNMAPPED')

    cy.getByTestId('asset-monitoring-onboarding-todo').find('li').eq(1).click()
    cy.getByTestId('test-pathname').should('have.text', '/pulse-assets')
    cy.getByTestId('test-search').should('have.text', '?mapping_status=REQUIRES_REMAPPING')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.intercept('/api/v1/management/pulse/managed-assets', MOCK_PULSE_ASSET_LIST).as('getAssets')
    cy.mountWithProviders(<AssetMonitoringOnboardingTask />)
    cy.checkAccessibility()
  })
})
