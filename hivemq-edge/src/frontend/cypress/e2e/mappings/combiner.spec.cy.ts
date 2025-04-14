import type { Combiner } from '@/api/__generated__'
import { factory, primaryKey, drop } from '@mswjs/data'

import { MOCK_ADAPTER_OPC_UA, MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters/opc-ua.ts'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { adapterPage, loginPage, rjsf, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { combinerForm } from '../../pages/Workspace/CombinerFormPage.ts'

const COMBINER_ID = '9e975b62-6f8d-410f-9007-3f83719aec6f'

describe('Combiner', () => {
  // Creating a mock storage for Combiner
  const mswDB = factory({
    combiner: {
      id: primaryKey(String),
      // TODO[E2E] Could we use the OpenAPI or the TS types?
      // Stringified version of the payload
      json: String,
    },
  })

  beforeEach(() => {
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

    cy.intercept<Combiner>('DELETE', '/api/v1/management/combiners/**', (req) => {
      if (req.url.endsWith(COMBINER_ID)) {
        mswDB.combiner.delete({
          where: {
            id: {
              equals: COMBINER_ID,
            },
          },
        })
        req.reply(200, '')
      } else req.reply(400)
    }).as('deleteCombiner')

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()
  })

  afterEach(() => {
    drop(mswDB)
  })

  it('should render the workspace', () => {
    // Check that the expected content is there

    workspacePage.combinerNode(COMBINER_ID).should('not.exist')
  })

  it('should create the first combiner', () => {
    workspacePage.canvas.should('be.visible')
    workspacePage.toolbox.fit.click()

    workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
    workspacePage.toolbar.combine.click()

    cy.wait('@postCombiner')
    workspacePage.toast.success
      .should('be.visible')
      .should('contain.text', "We've successfully created the combiner for you.")

    cy.wait('@getCombiners')

    workspacePage.closeToast.click()

    workspacePage.combinerNode(COMBINER_ID).should('be.visible').should('contain.text', 'unnamed combiner')

    // double tap is needed
    workspacePage.combinerNode(COMBINER_ID).click()
    workspacePage.combinerNode(COMBINER_ID).dblclick()

    combinerForm.form.should('be.visible')

    rjsf.field('name').input.should('have.value', '< unnamed combiner >')
    rjsf.field('name').input.clear()
    rjsf.field('name').input.type('my adapter')

    // technically should not be from adapterPage
    adapterPage.config.formTab(2).click()

    rjsf.field('mappings').table.noDataMessage.should('have.text', 'No data received yet.')
    rjsf.field('mappings').table.addItem.click()
    rjsf.field('mappings').table.rows.should('have.length', 1)
    rjsf.field('mappings').table.row(0).edit.click()

    combinerForm.mappingEditor.form.should('be.visible')
    combinerForm.mappingEditor.sources.selector.should('contain.text', 'Select tags and topic filters to combine ...')
    combinerForm.mappingEditor.sources.selector.click()
    combinerForm.mappingEditor.sources.options.should('have.length', 3)

    combinerForm.mappingEditor.sources.selector.type('a/topic/')
    combinerForm.mappingEditor.sources.options.click()
    combinerForm.mappingEditor.sources.selector.should('contain.text', 'a/topic/+/filter')
    combinerForm.mappingEditor.sources.schema.should('have.length', 2)
    combinerForm.mappingEditor.primary.selector.type('a/topic{enter}')

    combinerForm.mappingEditor.destination.selector.type('my/topic{enter}')
    combinerForm.mappingEditor.destination.inferSchema.click()

    combinerForm.inferSchema.modal.should('be.visible')
    combinerForm.inferSchema.submit.click()

    combinerForm.mappingEditor.destination.schema.should('have.length', 2)
    combinerForm.mappingEditor.instruction(0).mapping.should('have.text', 'description')
    combinerForm.mappingEditor.instruction(1).status.should('have.attr', 'data-status', 'success')
    combinerForm.mappingEditor.instruction(1).mapping.should('have.text', 'name')
    combinerForm.mappingEditor.instruction(1).status.should('have.attr', 'data-status', 'success')

    combinerForm.mappingEditor.submit.click()

    adapterPage.config.formTab(2).click()

    rjsf.field('mappings').table.rows.should('have.length', 1)
    combinerForm.table.destination.should('have.text', 'my / topic')
    combinerForm.table.sources.within(() => {
      cy.getByTestId('primary-wrapper').should('have.length', 1)
      cy.getByTestId('primary-wrapper').should('have.text', 'a / topic / + / filter')
    })

    // eslint-disable-next-line cypress/no-unnecessary-waiting
    cy.wait(500)

    combinerForm.submit.click()

    // workspacePage.toast.error
    //   .should('contain.text', 'There was a problem trying to update the combiner')
    workspacePage.toast.success.should('contain.text', "We've successfully updated the combiner for you")
    workspacePage.closeToast.click()

    workspacePage.combinerNode(COMBINER_ID).should('be.visible').should('contain.text', 'my adapter')
  })

  it.only('should delete the first combiner', () => {
    workspacePage.canvas.should('be.visible')
    workspacePage.toolbox.fit.click()

    workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
    workspacePage.toolbar.combine.click()

    cy.wait('@postCombiner')
    workspacePage.toast.success
      .should('be.visible')
      .should('contain.text', "We've successfully created the combiner for you.")

    cy.wait('@getCombiners')

    workspacePage.closeToast.click()

    workspacePage.combinerNode(COMBINER_ID).should('be.visible').should('contain.text', 'unnamed combiner')

    // double tap is needed
    workspacePage.combinerNode(COMBINER_ID).click()
    workspacePage.combinerNode(COMBINER_ID).dblclick()

    combinerForm.form.should('be.visible')
    combinerForm.delete.click()

    combinerForm.confirmDelete.modal.should('be.visible')
    combinerForm.confirmDelete.submit.click()
    combinerForm.confirmDelete.modal.should('not.exist')

    cy.wait('@deleteCombiner')
    workspacePage.toast.success
      .should('be.visible')
      .should('contain.text', "We've successfully deleted the combiner for you.")

    cy.wait('@getCombiners')

    workspacePage.closeToast.click()
  })
})
