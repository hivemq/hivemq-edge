import { formatTopicString } from '@/components/MQTT/topic-utils.ts'
import { drop } from '@mswjs/data'

import { loginPage, homePage, assetsPage, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import {
  cy_interceptPulseWithMockDB,
  getPulseFactory,
  MOCK_MAIN_ASSET_MAPPER_ID,
} from 'cypress/utils/intercept-pulse.utils.ts'
import { assetMappingWizard } from 'cypress/pages/Pulse/AssetMappingWizardForm.ts'
import { ONBOARDING } from 'cypress/utils/constants.utils.ts'
import { assetMapperForm } from '../../pages/Workspace/AssetMapperFormPage.ts'

describe('Pulse Assets', () => {
  const mswDB = getPulseFactory()

  beforeEach(() => {
    drop(mswDB)

    cy_interceptCoreE2E()
    cy_interceptPulseWithMockDB(mswDB, true, true)

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    homePage.navLink.click()
  })

  describe('Assets Management', () => {
    it('should render assets', () => {
      homePage.taskSectionTitle(ONBOARDING.TASK_PULSE, 0).should('contain.text', 'Pulse is currently active.')

      homePage.pulseOnboarding.title.should('contain.text', 'Stay up-to-date with your asset mappings')

      const expectedTodoSummary = [2, 1, 0]
      homePage.pulseOnboarding.todos.should('have.length', expectedTodoSummary.length)
      homePage.pulseOnboarding.todosSummary.each(($element, idx) => {
        cy.wrap($element).should('contain.text', expectedTodoSummary[idx])
      })
      homePage.pulseOnboarding.todos.eq(0).find('a').click()

      assetsPage.location.should('equal', '/app/pulse-assets')
      assetsPage.table.rows.should('have.length', 2)

      assetsPage.search.input.type('1234')
      assetsPage.search.clear.click()

      assetsPage.search.filter(0).should('not.exist')
      assetsPage.search.filter('topic').type('topic{enter}')
      assetsPage.search.clearFilter('status').click()
      assetsPage.search.clearAll.click()

      assetsPage.table.actions(0).should('have.attr', 'aria-haspopup', 'menu')
      assetsPage.table.action(0, 'map').click()
    })
    it.skip('should relate assets to asset mappers', () => {})

    it('should be accessible', { tags: ['@percy'] }, () => {
      cy.injectAxe()
      homePage.taskSectionTitle(ONBOARDING.TASK_PULSE, 0).should('contain.text', 'Pulse is currently active.')
      homePage.pulseOnboarding.todos.eq(0).find('a').click()

      assetsPage.location.should('equal', '/app/pulse-assets')
      assetsPage.table.rows.should('have.length', 2)

      cy.checkAccessibility(undefined, {
        rules: {
          region: { enabled: false },
          'color-contrast': { enabled: false },
        },
      })
      cy.percySnapshot('Pulse - Assets Table')
    })
  })

  describe('Asset Mapping', () => {
    it('should create a new asset mapper', () => {
      homePage.taskSectionTitle(ONBOARDING.TASK_PULSE, 0).should('contain.text', 'Pulse is currently active.')
      homePage.pulseOnboarding.todos.eq(0).find('a').click()
      assetsPage.location.should('equal', '/app/pulse-assets')
      assetsPage.table.action(0, 'map').click()

      assetMappingWizard.form.should('be.visible').and('have.css', 'opacity', '1')
      assetMappingWizard.selectMapper.root.should('be.visible')
      assetMappingWizard.selectSources.root.should('not.exist')

      assetMappingWizard.selectMapper.label.should('have.text', 'Asset mapper')
      assetMappingWizard.selectMapper.moreInfo.should('have.attr', 'aria-label', 'More information')
      assetMappingWizard.selectMapper.select.should('be.visible')
      assetMappingWizard.selectMapper.value.should('not.exist')
      assetMappingWizard.selectMapper.placeholder.should('have.text', 'Type or select ...')
      assetMappingWizard.selectMapper.helperText.should('have.text', 'The asset mapper to use for the new mapping')

      assetMappingWizard.selectMapper.select.type('Non-existing mapper{enter}')
      assetMappingWizard.selectMapper.helperText.should(
        'have.text',
        'A new asset mapper will be created in the Workspace, with a predefined mapping for this asset'
      )

      assetMappingWizard.selectSources.root.should('be.visible')
      assetMappingWizard.selectSources.label.should('have.text', 'Data Sources')
      assetMappingWizard.selectSources.moreInfo.should('have.attr', 'aria-label', 'More information')
      assetMappingWizard.selectSources.select.should('be.visible')
      assetMappingWizard.selectSources.values.should('have.length', 2)
      assetMappingWizard.selectSources.helperText.should(
        'have.text',
        'The data sources this new mapper will be initially connected to'
      )
      assetMappingWizard.selectSources.select.type('my-adapter{enter}')
      assetMappingWizard.submit.click()
      assetsPage.toast.success.should('be.visible')
    })

    it('should add an asset to an existing mapper', () => {
      assetsPage.navLink.click()
      assetsPage.location.should('equal', '/app/pulse-assets')

      // create the first default mapper
      assetsPage.table.action(0, 'map').click()
      assetMappingWizard.selectMapper.select.type('Non-existing mapper{enter}')
      assetMappingWizard.selectSources.select.type('my-adapter{enter}')
      assetMappingWizard.submit.click()

      assetsPage.toast.success.should('be.visible')
      assetsPage.toast.close()

      // TODO Check that the mapper has been created in the workspace
      workspacePage.location.should('equal', '/app/workspace')
      workspacePage.canvas.should('be.visible')
      workspacePage.toolbox.fit.click()

      workspacePage
        .combinerNodeContent(MOCK_MAIN_ASSET_MAPPER_ID)
        .title.should('have.text', 'Non-existing mapper (new)')
      workspacePage
        .combinerNodeContent(MOCK_MAIN_ASSET_MAPPER_ID)
        .topic.should('have.text', formatTopicString('test/topic'))
      workspacePage.combinerNode(MOCK_MAIN_ASSET_MAPPER_ID).click()
      workspacePage.combinerNode(MOCK_MAIN_ASSET_MAPPER_ID).dblclick()
      assetMapperForm.formTab(2).click()

      // delete the mapping, to allow editing of the name
      assetMapperForm.assetMappings.action(0, 'delete').click()
      assetMapperForm.field('mappings').table.noDataMessage.should('have.text', 'No data received yet.')

      assetMapperForm.formTab(0).click()
      assetMapperForm.field('name').input.clear().type('my mapper')
      assetMapperForm.submit.click()

      workspacePage.combinerNodeContent(MOCK_MAIN_ASSET_MAPPER_ID).title.should('have.text', 'my mapper')
      workspacePage.combinerNodeContent(MOCK_MAIN_ASSET_MAPPER_ID).topic.should('not.exist')
    })

    it.skip('should remove an existing mapping', () => {})
    it.skip('should change an existing mapping', () => {})
  })
})
