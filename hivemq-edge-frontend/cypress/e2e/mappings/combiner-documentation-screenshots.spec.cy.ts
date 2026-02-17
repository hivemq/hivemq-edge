import type { Combiner } from '@/api/__generated__'
import { factory, primaryKey, drop } from '@mswjs/data'

import { MOCK_ADAPTER_OPC_UA, MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters/opc-ua.ts'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { adapterPage, loginPage, rjsf, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { workspaceCombinerPanel } from '../../pages/Workspace/CombinerFormPage.ts'

const COMBINER_ID = '9e975b62-6f8d-410f-9007-3f83719aec6f'

describe('Combiner Documentation Screenshots', () => {
  // Creating a mock storage for Combiner
  const mswDB = factory({
    combiner: {
      id: primaryKey(String),
      json: String,
    },
  })

  beforeEach(() => {
    cy.viewport(1280, 720) // HD viewport for E2E screenshots
    cy_interceptCoreE2E()

    cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 203, log: false })
    cy.intercept('/api/v1/gateway/listeners', { statusCode: 203, log: false })
    cy.intercept('/api/v1/management/bridges', { statusCode: 203, log: false })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/northboundMappings', { statusCode: 203, log: false })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/southboundMappings', { statusCode: 203, log: false })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/*/tags', { statusCode: 203, log: false })

    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_OPC_UA] }).as('getProtocols')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [MOCK_ADAPTER_OPC_UA, { ...MOCK_ADAPTER_OPC_UA, id: 'opcua-boiler' }],
    }).as('getAdapters')
    cy.intercept('/api/v1/management/topic-filters', {
      items: [
        MOCK_TOPIC_FILTER,
        {
          topicFilter: 'another/filter',
          description: 'This is a topic filter',
        },
        {
          topicFilter: 'another/filter/too',
          description: 'This is another topic filter',
        },
      ],
    }).as('getTopicFilters')

    cy.intercept<Combiner>('POST', '/api/v1/management/combiners', (req) => {
      const combiner = req.body
      const newCombinerData = mswDB.combiner.create({
        id: COMBINER_ID,
        json: JSON.stringify({ ...combiner, id: COMBINER_ID }),
      })
      req.reply(200, newCombinerData)
    }).as('postCombiner')

    cy.intercept('GET', '/api/v1/management/combiners', (req) => {
      const allCombinerData = mswDB.combiner.getAll()
      const allCombiners = allCombinerData.map<Combiner>((data) => ({ ...JSON.parse(data.json) }))
      req.reply(200, { items: allCombiners })
    }).as('getCombiners')

    cy.intercept<Combiner>('PUT', '/api/v1/management/combiners/**', (req) => {
      const combiner = req.body
      mswDB.combiner.update({
        where: {
          id: {
            equals: combiner.id,
          },
        },
        data: { json: JSON.stringify({ ...combiner, id: COMBINER_ID }) },
      })
      req.reply(200, '')
    }).as('putCombiner')

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()
  })

  afterEach(() => {
    drop(mswDB)
  })

  it('should capture tabs navigation screenshot', () => {
    workspacePage.canvas.should('be.visible')
    workspacePage.toolbox.fit.click()

    workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
    workspacePage.toolbar.combine.click()

    cy.wait('@postCombiner')
    cy.wait('@getCombiners')
    workspacePage.closeToast.click()

    workspacePage.combinerNode(COMBINER_ID).should('be.visible')
    workspacePage.combinerNode(COMBINER_ID).click()
    workspacePage.combinerNode(COMBINER_ID).dblclick()

    workspaceCombinerPanel.form.should('be.visible')
    cy.wait(300) // Stabilize render

    // Screenshot: Tabs navigation showing Configuration, Sources, Mappings tabs
    cy.screenshot('combiner-tabs-navigation', {
      overwrite: true,
      capture: 'viewport',
    })
  })

  it('should capture mapping drawer closed screenshot', () => {
    workspacePage.canvas.should('be.visible')
    workspacePage.toolbox.fit.click()

    workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
    workspacePage.toolbar.combine.click()

    cy.wait('@postCombiner')
    cy.wait('@getCombiners')
    workspacePage.closeToast.click()

    workspacePage.combinerNode(COMBINER_ID).should('be.visible')
    workspacePage.combinerNode(COMBINER_ID).click()
    workspacePage.combinerNode(COMBINER_ID).dblclick()

    workspaceCombinerPanel.form.should('be.visible')

    // Navigate to Mappings tab
    adapterPage.config.formTab(2).click()
    rjsf.field('mappings').table.noDataMessage.should('have.text', 'No data received yet.')
    cy.wait(300) // Stabilize render

    // Screenshot: Mappings tab showing empty table (drawer closed)
    cy.screenshot('combiner-mapping-drawer', {
      overwrite: true,
      capture: 'viewport',
    })
  })

  it('should capture mapping drawer open screenshot', () => {
    workspacePage.canvas.should('be.visible')
    workspacePage.toolbox.fit.click()

    workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
    workspacePage.toolbar.combine.click()

    cy.wait('@postCombiner')
    cy.wait('@getCombiners')
    workspacePage.closeToast.click()

    workspacePage.combinerNode(COMBINER_ID).should('be.visible')
    workspacePage.combinerNode(COMBINER_ID).click()
    workspacePage.combinerNode(COMBINER_ID).dblclick()

    workspaceCombinerPanel.form.should('be.visible')

    // Navigate to Mappings tab
    adapterPage.config.formTab(2).click()
    rjsf.field('mappings').table.addItem.click()
    rjsf.field('mappings').table.rows.should('have.length', 1)
    rjsf.field('mappings').table.row(0).edit.click()

    workspaceCombinerPanel.mappingEditor.form.should('be.visible')
    cy.wait(300) // Stabilize render

    // Screenshot: Mapping editor drawer open showing sources, destination, schemas
    cy.screenshot('combiner-mapping-drawer-open', {
      overwrite: true,
      capture: 'viewport',
    })
  })
})
