import type { BehaviorPolicy } from '@/api/__generated__'
import { drop, factory, primaryKey } from '@mswjs/data'
import { DateTime } from 'luxon'

import mockDataPolicy from 'cypress/fixtures/test-2025-07-23.json'
import _mockBehaviorPolicy from 'cypress/fixtures/test-behavior-policy-2025-11-03.json'
import mockSchemaValidator from 'cypress/fixtures/test-schema-2025-07-23.json'
import mockSchemaDeserialise from 'cypress/fixtures/test-deserialise-2025-07-23.json'
import mockSchemaSerialise from 'cypress/fixtures/test-serialise-2025-07-23.json'
import mockFunction from 'cypress/fixtures/test-function-2025-07-23.json'

const mockBehaviorPolicy = _mockBehaviorPolicy as BehaviorPolicy

import type { DataHubFactory } from 'cypress/utils/intercept.utils.ts'
import { cy_interceptCoreE2E, cy_interceptDataHubWithMockDB } from 'cypress/utils/intercept.utils.ts'
import { datahubPage, loginPage, datahubDesignerPage } from 'cypress/pages'

import { MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'
import { MOCK_DATAHUB_FUNCTIONS } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'

/**
 * Helper to validate a policy and open the report drawer
 */
const validateAndOpenReport = (policyType: 'DATA_POLICY' | 'BEHAVIOR_POLICY') => {
  datahubDesignerPage.canvas.should('be.visible')
  datahubDesignerPage.statusBar.modify.click()
  datahubDesignerPage.designer.selectNode(policyType)
  datahubDesignerPage.toolbar.checkPolicy.should('not.be.disabled')
  datahubDesignerPage.toolbar.checkPolicy.click()
  datahubDesignerPage.toolbar.showReport.should('not.be.disabled')
  datahubDesignerPage.toolbar.showReport.click()
  datahubDesignerPage.dryRunPanel.drawer.should('be.visible')
}

describe('DataHub - Policy Report Content', () => {
  const mswDB: DataHubFactory = factory({
    dataPolicy: {
      id: primaryKey(String),
      json: String,
    },
    behaviourPolicy: {
      id: primaryKey(String),
      json: String,
    },
    schema: {
      id: primaryKey(String),
      json: String,
    },
    script: {
      id: primaryKey(String),
      json: String,
    },
  })

  beforeEach(() => {
    drop(mswDB)
    cy_interceptCoreE2E()
    cy.intercept('/api/v1/frontend/capabilities', MOCK_CAPABILITIES)
    cy.intercept('/api/v1/data-hub/function-specs', {
      items: MOCK_DATAHUB_FUNCTIONS.items.map((specs) => {
        specs.metadata.inLicenseAllowed = true
        return specs
      }),
    })
    cy_interceptDataHubWithMockDB(mswDB)
    loginPage.visit('/app/datahub')
    loginPage.loginButton.click()
    datahubPage.navLink.click()
  })

  describe('Data Policy Report', () => {
    beforeEach(() => {
      const dateNow = Date.now()
      const formattedDate = DateTime.fromMillis(dateNow).minus({ minutes: 30 }).toISO({ format: 'basic' }) as string

      mswDB.dataPolicy.create({
        id: mockDataPolicy.id,
        json: JSON.stringify({ ...mockDataPolicy, createdAt: formattedDate, lastUpdatedAt: formattedDate }),
      })
      mswDB.schema.create({
        id: mockSchemaValidator.id,
        json: JSON.stringify({ ...mockSchemaValidator, createdAt: formattedDate }),
      })
      mswDB.schema.create({
        id: mockSchemaDeserialise.id,
        json: JSON.stringify({ ...mockSchemaDeserialise, createdAt: formattedDate }),
      })
      mswDB.schema.create({
        id: mockSchemaSerialise.id,
        json: JSON.stringify({ ...mockSchemaSerialise, createdAt: formattedDate }),
      })
      mswDB.script.create({
        id: mockFunction.id,
        json: JSON.stringify({ ...mockFunction, createdAt: formattedDate }),
      })
    })

    it('should show data policy overview with correct type and ID', () => {
      datahubPage.policiesTable.action(0, 'edit').click()
      validateAndOpenReport('DATA_POLICY')

      // Verify policy overview shows correct information
      datahubDesignerPage.dryRunPanel.policyOverview.should('exist')
      datahubDesignerPage.dryRunPanel.policyType.should('contain.text', 'Data Policy')
      datahubDesignerPage.dryRunPanel.policyId.should('contain.text', 'test')
      datahubDesignerPage.dryRunPanel.policyStatusBadge.should('contain.text', 'Update')
    })

    it('should show topic filters in policy overview', () => {
      datahubPage.policiesTable.action(0, 'edit').click()
      validateAndOpenReport('DATA_POLICY')

      // Verify topic filters are shown
      datahubDesignerPage.dryRunPanel.topicFiltersList.should('exist')
      datahubDesignerPage.dryRunPanel.topicFiltersList.find('li').should('have.length.at.least', 1)
    })

    it('should show resources breakdown with schemas and scripts', () => {
      datahubPage.policiesTable.action(0, 'edit').click()
      validateAndOpenReport('DATA_POLICY')

      // Verify resources are listed
      datahubDesignerPage.dryRunPanel.resourcesBreakdown.should('exist')
      // Check that schemas and scripts sections exist (they contain the resource names)
      datahubDesignerPage.dryRunPanel.resourcesBreakdown.should('contain.text', 'test-schema')
      datahubDesignerPage.dryRunPanel.resourcesBreakdown.should('contain.text', 'test-function')
    })

    it('should show JSON view with policy data', () => {
      datahubPage.policiesTable.action(0, 'edit').click()
      validateAndOpenReport('DATA_POLICY')

      // Verify JSON view exists
      datahubDesignerPage.dryRunPanel.jsonView.should('exist')
      datahubDesignerPage.dryRunPanel.jsonToggleButton.should('exist')

      // Expand and verify tabs exist
      datahubDesignerPage.dryRunPanel.jsonToggleButton.click()
      datahubDesignerPage.dryRunPanel.jsonTab('Policy').should('exist')
      datahubDesignerPage.dryRunPanel.jsonTab('Schemas').should('exist')
      datahubDesignerPage.dryRunPanel.jsonTab('Scripts').should('exist')
    })
  })

  describe('Behavior Policy Report', () => {
    beforeEach(() => {
      const dateNow = Date.now()
      const formattedDate = DateTime.fromMillis(dateNow).minus({ minutes: 30 }).toISO({ format: 'basic' }) as string

      mswDB.behaviourPolicy.create({
        id: mockBehaviorPolicy.id,
        json: JSON.stringify({ ...mockBehaviorPolicy, createdAt: formattedDate, lastUpdatedAt: formattedDate }),
      })
    })

    it('should show behavior policy overview with correct type and ID', () => {
      datahubPage.policiesTable.action(0, 'edit').click()
      cy.url().should('contain', '/datahub/BEHAVIOR_POLICY/test-behavior-policy')

      validateAndOpenReport('BEHAVIOR_POLICY')

      // Verify policy overview shows correct information
      datahubDesignerPage.dryRunPanel.policyOverview.should('exist')
      datahubDesignerPage.dryRunPanel.policyType.should('contain.text', 'Behavior Policy')
      datahubDesignerPage.dryRunPanel.policyId.should('contain.text', 'test-behavior-policy')
    })

    it('should show transitions in policy overview', () => {
      datahubPage.policiesTable.action(0, 'edit').click()
      cy.url().should('contain', '/datahub/BEHAVIOR_POLICY/test-behavior-policy')

      validateAndOpenReport('BEHAVIOR_POLICY')

      // Verify transitions are shown (not topic filters)
      datahubDesignerPage.dryRunPanel.transitionsList.should('exist')
      datahubDesignerPage.dryRunPanel.transitionsList.find('li').should('have.length.at.least', 1)
    })
  })

  describe('Visual Regression - PR Screenshots', { tags: ['@percy'] }, () => {
    it('should capture data policy report', () => {
      const dateNow = Date.now()
      const formattedDate = DateTime.fromMillis(dateNow).minus({ minutes: 30 }).toISO({ format: 'basic' }) as string

      mswDB.dataPolicy.create({
        id: mockDataPolicy.id,
        json: JSON.stringify({ ...mockDataPolicy, createdAt: formattedDate, lastUpdatedAt: formattedDate }),
      })
      mswDB.schema.create({
        id: mockSchemaValidator.id,
        json: JSON.stringify({ ...mockSchemaValidator, createdAt: formattedDate }),
      })
      mswDB.schema.create({
        id: mockSchemaDeserialise.id,
        json: JSON.stringify({ ...mockSchemaDeserialise, createdAt: formattedDate }),
      })
      mswDB.schema.create({
        id: mockSchemaSerialise.id,
        json: JSON.stringify({ ...mockSchemaSerialise, createdAt: formattedDate }),
      })
      mswDB.script.create({
        id: mockFunction.id,
        json: JSON.stringify({ ...mockFunction, createdAt: formattedDate }),
      })

      datahubPage.policiesTable.action(0, 'edit').click()
      validateAndOpenReport('DATA_POLICY')

      // Wait for content to be present
      datahubDesignerPage.dryRunPanel.policyOverview.should('exist')
      datahubDesignerPage.dryRunPanel.policyId.should('contain.text', 'test')

      cy.percySnapshot('DataHub - Data Policy Report')
      cy.screenshot('workspace-data-policy-report', {
        capture: 'viewport',
        overwrite: true,
      })
    })

    it('should capture behavior policy report', () => {
      const dateNow = Date.now()
      const formattedDate = DateTime.fromMillis(dateNow).minus({ minutes: 30 }).toISO({ format: 'basic' }) as string

      mswDB.behaviourPolicy.create({
        id: mockBehaviorPolicy.id,
        json: JSON.stringify({ ...mockBehaviorPolicy, createdAt: formattedDate, lastUpdatedAt: formattedDate }),
      })

      datahubPage.policiesTable.action(0, 'edit').click()
      cy.url().should('contain', '/datahub/BEHAVIOR_POLICY/test-behavior-policy')

      validateAndOpenReport('BEHAVIOR_POLICY')

      // Wait for content to be present
      datahubDesignerPage.dryRunPanel.policyOverview.should('exist')
      datahubDesignerPage.dryRunPanel.policyId.should('contain.text', 'test-behavior-policy')

      cy.percySnapshot('DataHub - Behavior Policy Report')
      cy.screenshot('workspace-behaviour-policy-report', {
        capture: 'viewport',
        overwrite: true,
      })
    })
  })
})
