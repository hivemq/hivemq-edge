import { MOCK_PROTOCOL_HTTP, MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_SIMULATION } from '@/__test-utils__/adapters'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import { mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'

import { loginPage, workspacePage, wizardPage } from '../../../pages'
import { cy_interceptCoreE2E } from '../../../utils/intercept.utils.ts'

const MOCK_HTTP_ADAPTER_ID = 'test-http-adapter'

describe('Wizard: Create Adapter', () => {
  beforeEach(() => {
    // Base interceptors for E2E
    cy_interceptCoreE2E()
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
      statusCode: 202,
      log: false,
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/southboundMappings', {
      statusCode: 202,
      log: false,
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/tags', {
      statusCode: 202,
      log: false,
    })

    // Protocol types (for selection)
    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [MOCK_PROTOCOL_HTTP, MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_SIMULATION],
    }).as('getProtocols')

    // Existing adapters (so we don't create duplicates)
    cy.intercept('GET', '/api/v1/management/protocol-adapters/adapters', {
      items: [mockAdapter_OPCUA],
    }).as('getExistingAdapters')

    // POST to create new adapter - success response
    cy.intercept('POST', '/api/v1/management/protocol-adapters/adapters/**', {
      statusCode: 201,
      body: {
        id: MOCK_HTTP_ADAPTER_ID,
        protocol_type: MockAdapterType.HTTP,
      },
    }).as('createAdapter')

    cy.intercept('GET', '/api/v1/management/bridges', { statusCode: 202, log: false })
    cy.intercept('GET', '/api/v1/management/topic-filters', { statusCode: 202, log: false })
    cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 202, log: false })
    cy.intercept('/api/v1/management/events?*', { statusCode: 202, log: false })

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()

    workspacePage.canvas.should('be.visible')
  })

  // ========== 1. ACCESSIBILITY TEST ==========
  /**
   * Verifies the adapter wizard is accessible (WCAG2AA)
   * Captures Percy snapshot for visual regression
   */
  it(
    'should be accessible and capture wizard screens for documentation and visual consistency',
    { tags: ['@percy'] },
    () => {
      cy.injectAxe()

      workspacePage.toolbox.fit.click()
      wizardPage.createEntityButton.should('be.visible')

      // DEBUG: Capture initial workspace state for AI debugging
      cy.saveHTMLSnapshot('wizard-adapter-initial-workspace')
      cy.logDOMState('Workspace with create button')

      wizardPage.createEntityButton.click()

      // DEBUG: Capture wizard menu state
      cy.saveHTMLSnapshot('wizard-adapter-menu-open')
      cy.logDOMState('Wizard menu with entity options')

      cy.screenshot('Workspace Wizard / Wizard menu', { overwrite: true })
      cy.checkAccessibility(undefined, {
        rules: {
          // Chakra UI Portal creates region elements without accessible names - known third-party issue
          region: { enabled: false },
        },
      })

      wizardPage.wizardMenu.selectOption('ADAPTER')

      // DEBUG: Capture ghost preview state
      cy.saveHTMLSnapshot('wizard-adapter-ghost-preview')
      cy.logDOMState('Adapter ghost preview')

      // Check progress bar exists and shows step 1
      wizardPage.progressBar.container.should('be.visible')
      wizardPage.progressBar.container.should('contain', 'Step 1')

      cy.screenshot('Workspace Wizard / Adapter wizard progress', { overwrite: true })

      // Check for ghost nodes on canvas
      wizardPage.canvas.ghostNode.should('be.visible')
      wizardPage.canvas.ghostEdges.should('exist')

      cy.percySnapshot('Wizard: Adapter Ghosts nodes')

      wizardPage.progressBar.nextButton.click()

      // // Now we should see the configuration form
      // cy.wait('@getProtocols')

      // DEBUG: Capture configuration form state
      cy.saveHTMLSnapshot('wizard-adapter-configuration-form')
      cy.logDOMState('Adapter configuration form')

      wizardPage.progressBar.container.should('contain', 'Step 2')

      // Form should be accessible
      cy.screenshot('Workspace Wizard / Adapter configuration', { overwrite: true })
      wizardPage.adapterConfig.protocolSelectors.then(($form) => {
        cy.checkAccessibility($form[0], {
          rules: {
            'color-contrast': { enabled: false },
          },
        })
      })

      cy.percySnapshot('Wizard: Adapter Configuration Form')
    }
  )

  it('should create an HTTP adapter successfully', () => {
    wizardPage.createEntityButton.click()
    wizardPage.wizardMenu.selectOption('ADAPTER')
    wizardPage.progressBar.nextButton.click()

    // DEBUG: Capture initial workspace state for AI debugging
    cy.saveHTMLSnapshot('wizard-adapter-creation-protocol-select')
    cy.logDOMState('Before HTTP protocol selection')

    wizardPage.adapterConfig.selectProtocol(MockAdapterType.HTTP).click()
    wizardPage.adapterConfig.setAdapterName(MOCK_HTTP_ADAPTER_ID)

    // For HTTP adapter, there should be configuration fields
    // (These are protocol-specific, so we only check some are present)
    wizardPage.adapterConfig.configForm.within(() => {
      cy.get('input, select, textarea').should('have.length.greaterThan', 1)
    })

    // Capture filled form before submission
    cy.saveHTMLSnapshot('wizard-adapter-creation-before-submit')
    cy.logDOMState('Before adapter creation submission')

    wizardPage.adapterConfig.submitButton.click()

    cy.wait('@createAdapter')
    wizardPage.toast.success.should('be.visible')

    // Capture success state
    cy.saveHTMLSnapshot('wizard-adapter-creation-success')
    cy.logDOMState('After successful adapter creation')
  })
})
