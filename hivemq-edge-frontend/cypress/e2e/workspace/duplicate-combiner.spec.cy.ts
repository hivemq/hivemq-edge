import type { Combiner } from '@/api/__generated__'
import { factory, primaryKey, drop } from '@mswjs/data'

import { MOCK_ADAPTER_OPC_UA, MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters/opc-ua.ts'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { adapterPage, loginPage, rjsf, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { workspaceCombinerPanel } from 'cypress/pages/Workspace/CombinerFormPage.ts'

const COMBINER_ID = '9e975b62-6f8d-410f-9007-3f83719aec6f'

describe('Duplicate Combiner Detection', () => {
  // Creating a mock storage for Combiner
  const mswDB = factory({
    combiner: {
      id: primaryKey(String),
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

    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_OPC_UA] })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [MOCK_ADAPTER_OPC_UA, { ...MOCK_ADAPTER_OPC_UA, id: 'opcua-boiler' }],
    })
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
    })

    cy.intercept<Combiner>('POST', '/api/v1/management/combiners', (req) => {
      const combiner = req.body
      const newCombinerData = mswDB.combiner.create({
        id: COMBINER_ID,
        json: JSON.stringify({ ...combiner, id: COMBINER_ID }),
      })
      req.reply(200, JSON.parse(newCombinerData.json))
    }).as('postCombiner')

    cy.intercept('GET', '/api/v1/management/combiners', (req) => {
      const allCombinerData = mswDB.combiner.getAll()
      const allCombiners = allCombinerData.map<Combiner>((data) => JSON.parse(data.json))
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
        data: { json: JSON.stringify(combiner) },
      })
      req.reply(200, combiner)
    }).as('putCombiner')

    cy.intercept<Combiner>('DELETE', '/api/v1/management/combiners/**', (req) => {
      const pathParts = req.url.split('/')
      const combinerId = pathParts[pathParts.length - 1]

      mswDB.combiner.delete({
        where: {
          id: {
            equals: combinerId,
          },
        },
      })
      req.reply(200, '')
    })

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()
  })

  afterEach(() => {
    drop(mswDB)
  })

  describe('Modal Interaction', () => {
    it('should show duplicate modal when creating combiner with same sources', { tags: ['@flaky'] }, () => {
      // Step 1: Create initial combiner
      workspacePage.canvas.should('be.visible')
      workspacePage.toolbox.fit.click()

      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()

      cy.wait('@postCombiner').then((interception) => {
        expect(interception.response?.statusCode).to.equal(200)
      })
      cy.wait('@getCombiners')

      workspacePage.toast.success
        .should('be.visible')
        .should('contain.text', "We've successfully created the combiner for you.")
      workspacePage.closeToast.click()

      // Verify first combiner exists
      workspacePage.combinerNode(COMBINER_ID).should('be.visible')

      // Step 2: Attempt to create duplicate combiner
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()

      // Step 3: Verify modal appears
      workspacePage.duplicateCombinerModal.modal.should('be.visible')
      workspacePage.duplicateCombinerModal.title.should('contain.text', 'Possible Duplicate Combiner')
      workspacePage.duplicateCombinerModal.combinerName.should('contain.text', 'unnamed combiner')
    })

    it('should display correct modal content for combiner', () => {
      // Create initial combiner
      workspacePage.canvas.should('be.visible')
      workspacePage.toolbox.fit.click()

      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()
      cy.wait('@postCombiner')
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      // Attempt duplicate
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()

      // Verify modal content
      workspacePage.duplicateCombinerModal.modal.should('be.visible')
      workspacePage.duplicateCombinerModal.description.should(
        'contain.text',
        'A combiner with the same source connections already exists'
      )
      workspacePage.duplicateCombinerModal.mappingsLabel.should('be.visible')
      workspacePage.duplicateCombinerModal.prompt.should('contain.text', 'What would you like to do')
    })

    it('should close modal when cancel button is clicked', () => {
      // Create initial combiner
      workspacePage.toolbox.fit.click()
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()
      cy.wait('@postCombiner')
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      // Attempt duplicate
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()

      // Close modal
      workspacePage.duplicateCombinerModal.modal.should('be.visible')
      workspacePage.duplicateCombinerModal.buttons.cancel.click()
      workspacePage.duplicateCombinerModal.modal.should('not.exist')
    })

    it('should close modal when X button is clicked', () => {
      // Create initial combiner
      workspacePage.toolbox.fit.click()
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()
      cy.wait('@postCombiner')
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      // Attempt duplicate
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()

      // Close modal with X button
      workspacePage.duplicateCombinerModal.modal.should('be.visible')
      workspacePage.duplicateCombinerModal.closeButton.click()
      workspacePage.duplicateCombinerModal.modal.should('not.exist')
    })

    it('should navigate to existing combiner when "Use Existing" is clicked', () => {
      // Create initial combiner
      workspacePage.toolbox.fit.click()
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()
      cy.wait('@postCombiner')
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      // Attempt duplicate
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()

      // Use existing
      workspacePage.duplicateCombinerModal.modal.should('be.visible')
      workspacePage.duplicateCombinerModal.buttons.useExisting.click()

      // Verify modal closes and combiner form opens
      workspacePage.duplicateCombinerModal.modal.should('not.exist')
      workspaceCombinerPanel.form.should('be.visible')

      // Verify we're editing the existing combiner
      cy.url().should('include', COMBINER_ID)
    })

    it('should create new combiner when "Create New Anyway" is clicked', () => {
      cy.intercept<Combiner>('POST', '/api/v1/management/combiners', (req) => {
        const combiner = req.body
        const newCombinerData = mswDB.combiner.create({
          id: combiner.id,
          json: JSON.stringify({ ...combiner, id: combiner.id }),
        })
        req.reply(200, JSON.parse(newCombinerData.json))
      }).as('postCombiner')

      // Create initial combiner
      workspacePage.toolbox.fit.click()
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()
      cy.wait('@postCombiner').then((interception) => {
        const firstID = interception.response?.body.id
        expect(firstID).not.to.be.undefined
        cy.wrap(firstID).as('firstID')
        console.log('First combiner created with ID:', firstID)
      })
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      cy.get('@firstID').then((id) => {
        workspacePage.combinerNode(id as unknown as string).should('be.visible')
      })

      // Attempt duplicate
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()

      // Create new anyway
      workspacePage.duplicateCombinerModal.modal.should('be.visible')
      workspacePage.duplicateCombinerModal.buttons.createNew.should('be.visible')
      workspacePage.duplicateCombinerModal.buttons.createNew.click()
      cy.wait('@postCombiner').then((interception) => {
        expect(interception.response?.statusCode).to.equal(200)
      })
    })
  })

  describe('Modal with Mappings', () => {
    it('should display existing mappings in modal', () => {
      // Create combiner with mappings
      workspacePage.toolbox.fit.click()
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()
      cy.wait('@postCombiner')
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      // Open combiner and add mapping
      workspacePage.combinerNode(COMBINER_ID).click()
      workspacePage.combinerNode(COMBINER_ID).dblclick()
      workspaceCombinerPanel.form.should('be.visible')

      adapterPage.config.formTab(2).click()
      cy.getByTestId('combiner-mapping-list-add').click()
      rjsf.field('mappings').table.row(0).edit.click()

      workspaceCombinerPanel.mappingEditor.sources.selector.click()
      workspaceCombinerPanel.mappingEditor.sources.selector.type('a/topic/')
      workspaceCombinerPanel.mappingEditor.sources.options.first().click()
      workspaceCombinerPanel.mappingEditor.destination.selector.type('my/destination{enter}')
      workspaceCombinerPanel.mappingEditor.primary.selector.type('a/topic/{enter}')

      workspaceCombinerPanel.mappingEditor.destination.inferSchema.click()

      workspaceCombinerPanel.inferSchema.modal.should('be.visible')
      workspaceCombinerPanel.inferSchema.submit.click()

      workspaceCombinerPanel.mappingEditor.submit.click()
      workspaceCombinerPanel.submit.click()
      cy.wait('@putCombiner')
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      // Attempt to create duplicate
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()

      // Verify mappings are shown in modal
      workspacePage.duplicateCombinerModal.modal.should('be.visible')
      workspacePage.duplicateCombinerModal.mappingsList.should('be.visible')

      // Verify mapping details are visible
      cy.get('[data-testid^="mapping-item-"]').first().should('be.visible')
      cy.get('[data-testid^="mapping-destination-"]').first().should('contain.text', 'my/destination')
    })

    it('should show empty state when combiner has no mappings', () => {
      // Create combiner without mappings
      workspacePage.toolbox.fit.click()
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()
      cy.wait('@postCombiner')
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      // Attempt duplicate
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()

      // Verify empty state
      workspacePage.duplicateCombinerModal.modal.should('be.visible')
      workspacePage.duplicateCombinerModal.mappingsListEmpty.should('contain.text', 'No mappings defined yet')
    })
  })

  describe('Keyboard Navigation', () => {
    it('should close modal when ESC is pressed', () => {
      // Create initial combiner
      workspacePage.toolbox.fit.click()
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()
      cy.wait('@postCombiner')
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      // Attempt duplicate
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()

      // Press ESC
      workspacePage.duplicateCombinerModal.modal.should('be.visible')
      cy.get('body').type('{esc}')
      workspacePage.duplicateCombinerModal.modal.should('not.exist')
    })

    it('should focus "Use Existing" button by default', () => {
      // Create initial combiner
      workspacePage.toolbox.fit.click()
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()
      cy.wait('@postCombiner')
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      // Attempt duplicate
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()

      // Verify initial focus
      workspacePage.duplicateCombinerModal.modal.should('be.visible')
      workspacePage.duplicateCombinerModal.buttons.useExisting.should('have.focus')
    })
  })

  describe('Accessibility', () => {
    it('should be accessible', { tags: ['@percy'] }, () => {
      cy.injectAxe()

      // Create initial combiner
      workspacePage.toolbox.fit.click()
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()
      cy.wait('@postCombiner')
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      // Attempt duplicate to show modal
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()

      // Verify modal is visible
      workspacePage.duplicateCombinerModal.modal.should('be.visible')

      // Check accessibility
      cy.checkAccessibility(undefined, {
        rules: {
          region: { enabled: false },
          'color-contrast': { enabled: false },
        },
      })

      // Percy snapshot
      cy.percySnapshot('Workspace - Duplicate Combiner Modal')

      // Wait for modal slide-in animation to complete by checking opacity
      workspacePage.duplicateCombinerModal.modal.should('have.css', 'opacity', '1')

      // Screenshot for PR template (last command)
      cy.screenshot('after/after-modal-empty-state', {
        capture: 'viewport',
        overwrite: true,
      })
    })

    it('should be accessible with mappings', { tags: ['@percy'] }, () => {
      cy.injectAxe()

      // Create combiner with mappings
      workspacePage.toolbox.fit.click()
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()
      cy.wait('@postCombiner')
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      // Add mapping
      workspacePage.combinerNode(COMBINER_ID).click()
      workspacePage.combinerNode(COMBINER_ID).dblclick()
      workspaceCombinerPanel.form.should('be.visible')

      adapterPage.config.formTab(2).click()
      cy.getByTestId('combiner-mapping-list-add').click()
      rjsf.field('mappings').table.row(0).edit.click()

      workspaceCombinerPanel.mappingEditor.sources.selector.click()
      workspaceCombinerPanel.mappingEditor.sources.selector.type('a/topic/')
      workspaceCombinerPanel.mappingEditor.sources.options.first().click()
      workspaceCombinerPanel.mappingEditor.destination.selector.type('my/destination{enter}')
      workspaceCombinerPanel.mappingEditor.primary.selector.type('a/topic/{enter}')

      workspaceCombinerPanel.mappingEditor.destination.inferSchema.click()

      workspaceCombinerPanel.inferSchema.modal.should('be.visible')
      workspaceCombinerPanel.inferSchema.submit.click()

      workspaceCombinerPanel.mappingEditor.submit.click()
      workspaceCombinerPanel.submit.click()
      cy.wait('@putCombiner')
      cy.wait('@getCombiners')
      workspacePage.closeToast.click()

      // Show modal
      workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
      workspacePage.toolbar.combine.click()
      workspacePage.duplicateCombinerModal.modal.should('be.visible')

      // Check accessibility
      cy.checkAccessibility(undefined, {
        rules: {
          region: { enabled: false },
          'color-contrast': { enabled: false },
        },
      })

      // Percy snapshot
      cy.percySnapshot('Workspace - Duplicate Combiner Modal with Mappings')

      // Wait for modal slide-in animation to complete by checking opacity
      workspacePage.duplicateCombinerModal.modal.should('have.css', 'opacity', '1')

      // Screenshot for PR template (last command)
      cy.screenshot('after/after-modal-with-mappings', {
        capture: 'viewport',
        overwrite: true,
      })
    })
  })
})
