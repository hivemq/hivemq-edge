/// <reference types="cypress" />

import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { MOCK_CAPABILITY_DATAHUB } from '@/api/hooks/useFrontendServices/__handlers__'
import { MOCK_DATAHUB_FUNCTIONS } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'

/**
 * This test suite captures screenshots specifically for documentation.
 * Screenshots are used in docs/architecture/DATAHUB_ARCHITECTURE.md and other DataHub documentation.
 *
 * IMPORTANT: All E2E screenshots MUST use HD viewport (1280x720)
 *
 * After running tests:
 * 1. Find screenshots in cypress/screenshots/datahub/datahub-documentation-screenshots.spec.cy.ts/
 * 2. Copy to docs/assets/screenshots/datahub/
 * 3. Update documentation with proper captions and alt text
 * 4. Update docs/assets/screenshots/INDEX.md
 *
 * To run this test:
 * pnpm cypress:run:e2e --spec "cypress/e2e/datahub/datahub-documentation-screenshots.spec.cy.ts"
 */
describe('DataHub - Documentation Screenshots', () => {
  beforeEach(() => {
    // REQUIRED: HD viewport (1280x720) for all E2E screenshots
    cy.viewport(1280, 720)
    cy_interceptCoreE2E()

    // Enable DataHub capability
    cy.intercept('/api/v1/frontend/capabilities', { items: [MOCK_CAPABILITY_DATAHUB] })
    cy.intercept('/api/v1/data-hub/function-specs', MOCK_DATAHUB_FUNCTIONS)
  })

  describe('Policy Designer Canvas', () => {
    it('should capture empty policy designer canvas', () => {
      cy.intercept('/api/v1/data-hub/data-validation/policies', { items: [] })
      cy.intercept('/api/v1/data-hub/behavior-validation/policies', { items: [] })

      cy.visit('/datahub/data-policies/new')
      cy.wait(800) // Allow canvas to stabilize

      // Capture clean canvas with toolbox and toolbar visible
      cy.screenshot('datahub-designer-canvas-empty', {
        overwrite: true,
        capture: 'viewport',
      })
    })
  })

  describe('Schema Table States', () => {
    it('should capture schema table empty state', () => {
      cy.intercept('/api/v1/data-hub/schemas', { items: [] })

      cy.visit('/datahub/schemas')
      cy.wait(800) // Allow render to stabilize

      // Capture empty state with "No schemas found" message
      cy.screenshot('datahub-schema-table-empty-state', {
        overwrite: true,
        capture: 'viewport',
      })
    })

    it('should capture schema table with data', () => {
      cy.intercept('/api/v1/data-hub/schemas', {
        items: [mockSchemaTempHumidity],
      })

      cy.visit('/datahub/schemas')
      cy.wait(800) // Allow render to stabilize

      // Capture table with single schema entry
      cy.screenshot('datahub-schema-table-with-data', {
        overwrite: true,
        capture: 'viewport',
      })
    })
  })

  describe('Policy Table States', () => {
    it('should capture policy table empty state', () => {
      cy.intercept('/api/v1/data-hub/data-validation/policies', { items: [] })

      cy.visit('/datahub/data-policies')
      cy.wait(800)

      cy.screenshot('datahub-policy-table-empty-state', {
        overwrite: true,
        capture: 'viewport',
      })
    })
  })

  describe('Script Table States', () => {
    it('should capture script table empty state', () => {
      cy.intercept('/api/v1/data-hub/scripts', { items: [] })

      cy.visit('/datahub/scripts')
      cy.wait(800)

      cy.screenshot('datahub-script-table-empty-state', {
        overwrite: true,
        capture: 'viewport',
      })
    })
  })
})
