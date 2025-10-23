import { drop, factory, primaryKey } from '@mswjs/data'
import { DateTime } from 'luxon'

import mockDataPolicy from 'cypress/fixtures/test-2025-07-23.json'
import mockSchemaValidator from 'cypress/fixtures/test-schema-2025-07-23.json'
import mockSchemaDeserialise from 'cypress/fixtures/test-deserialise-2025-07-23.json'
import mockSchemaSerialise from 'cypress/fixtures/test-serialise-2025-07-23.json'
import mockFunction from 'cypress/fixtures/test-function-2025-07-23.json'

import type { DataHubFactory } from 'cypress/utils/intercept.utils.ts'
import { cy_interceptCoreE2E, cy_interceptDataHubWithMockDB } from 'cypress/utils/intercept.utils.ts'
import { datahubPage, loginPage, datahubDesignerPage } from 'cypress/pages'
import { cy_checkDataPolicyGraph } from 'cypress/utils/datahub.utils.ts'

import { MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'
import { MOCK_DATAHUB_FUNCTIONS } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'

describe('Data Hub', () => {
  // Creating a mock storage for the Data Hub
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

  it('should render properly', () => {
    datahubPage.pageHeader.should('have.text', 'Data Hub on Edge')
    datahubPage.policiesTable.status.should('have.text', 'No data received yet.')

    datahubPage.addNewPolicy.click()

    datahubDesignerPage.toolbox.trigger.click()

    datahubDesignerPage.toolbox.dataPolicy.drag('[role="application"][data-testid="rf__wrapper"]')
    datahubDesignerPage.controls.fit.click()

    datahubDesignerPage.toolbox.trigger.click()
    datahubDesignerPage.toolbox.topicFilter.drag('[role="application"][data-testid="rf__wrapper"]')
    datahubDesignerPage.controls.fit.click()

    datahubDesignerPage.designer.connectNodes('TOPIC_FILTER', 'topic-0', 'DATA_POLICY', 'topicFilter')
    datahubDesignerPage.controls.fit.click()

    datahubDesignerPage.designer.createOnDrop('DATA_POLICY', 'onSuccess')
    datahubDesignerPage.controls.fit.click()

    datahubDesignerPage.designer.createOnDrop('DATA_POLICY', 'onError')
    datahubDesignerPage.controls.fit.click()

    // TODO[NVL] Cannot make the .move works here. Need to investigate
    datahubDesignerPage.designer.mode('TOPIC_FILTER').type(datahubDesignerPage.leftArrows)
  })

  describe('Data Policy', () => {
    beforeEach(() => {
      const dateNow = Date.now()
      const formattedDate = DateTime.fromMillis(dateNow).minus({ minutes: 20 }).toISO({ format: 'basic' }) as string

      // Load the mock data policy into the mock databases
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

    it('should capture complex policy design', { tags: ['@percy'] }, () => {
      cy.injectAxe()
      datahubPage.pageHeader.should('have.text', 'Data Hub on Edge')
      datahubPage.policiesTable.rows.should('have.length', 1)

      datahubPage.policiesTable.cell(0, 'id').should('have.text', 'test')
      datahubPage.policiesTable.cell(0, 'type').should('have.text', 'Data Policy')
      datahubPage.policiesTable.cell(0, 'matching').should('have.text', 'topic/example/1')
      datahubPage.policiesTable.cell(0, 'created').should('have.text', '20 minutes ago')
      datahubPage.policiesTable.action(0, 'edit').click()

      cy.url().should('contain', '/datahub/DATA_POLICY/test')

      datahubDesignerPage.designer.modes().then(($elements) => {
        const dataIds = $elements.toArray().map((el) => el.getAttribute('data-id'))

        expect(dataIds.length).to.equal(8)
        // IDs must be unique
        expect(new Set(dataIds).size).to.equal(dataIds.length)
        cy.wrap(dataIds).as('nodeIds')
      })

      datahubDesignerPage.designer.edges().then(($elements) => {
        const dataIds = $elements.toArray().map((el) => el.getAttribute('data-id'))

        expect(dataIds.length).to.equal(7)

        cy.wrap(dataIds).as('edgeIds')
      })

      cy_checkDataPolicyGraph()

      cy.checkAccessibility(undefined, {
        rules: {
          region: { enabled: false },
          'color-contrast': { enabled: false },
        },
      })
      // Snapshot 3: Complex loaded policy with multiple nodes
      cy.percySnapshot('DataHub - Designer Complex')
    })
  })

  describe.skip('Behaviour Policy', () => {
    // Need to be done
  })

  it('should be accessible', { tags: ['@percy'] }, () => {
    cy.injectAxe()

    // Snapshot 1: Empty state landing page
    datahubPage.pageHeader.should('have.text', 'Data Hub on Edge')
    datahubPage.policiesTable.status.should('have.text', 'No data received yet.')
    cy.checkAccessibility(undefined, {
      rules: {
        region: { enabled: false },
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('DataHub - Empty State')

    // Snapshot 2: Designer with basic policy structure
    datahubPage.addNewPolicy.click()
    datahubDesignerPage.toolbox.trigger.click()
    datahubDesignerPage.toolbox.dataPolicy.drag('[role="application"][data-testid="rf__wrapper"]')
    datahubDesignerPage.controls.fit.click()

    datahubDesignerPage.toolbox.trigger.click()
    datahubDesignerPage.toolbox.topicFilter.drag('[role="application"][data-testid="rf__wrapper"]')
    datahubDesignerPage.controls.fit.click()

    datahubDesignerPage.designer.connectNodes('TOPIC_FILTER', 'topic-0', 'DATA_POLICY', 'topicFilter')
    datahubDesignerPage.controls.fit.click()

    cy.checkAccessibility(undefined, {
      rules: {
        region: { enabled: false },
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('DataHub - Designer Basic')
  })
})
