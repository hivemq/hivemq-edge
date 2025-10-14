import { MOCK_JWT } from '@/__test-utils__/mocks.ts'
import { drop } from '@mswjs/data'

import { loginPage, homePage, pulseActivationPanel } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { cy_interceptPulseWithMockDB, getPulseFactory } from 'cypress/utils/intercept-pulse.utils.ts'
import { ONBOARDING } from 'cypress/utils/constants.utils.ts'

describe('Pulse Agent Activation', () => {
  const mswDB = getPulseFactory()

  beforeEach(() => {
    drop(mswDB)

    cy_interceptCoreE2E()
    cy_interceptPulseWithMockDB(mswDB)

    // There seems to be a bug in the CI without something in the path
    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    homePage.navLink.click()
  })

  context('Pulse activation', () => {
    it('should render the landing page', () => {
      cy.location().should((loc) => {
        expect(loc.pathname).to.eq('/app')
      })

      cy.wait('@getCapabilities')

      homePage.pageHeader.should('have.text', 'Welcome to HiveMQ Edge')
      homePage.pageHeaderSubTitle.should(
        'have.text',
        'Connect to and from any device for seamless data streaming to your enterprise infrastructure.'
      )
      homePage.tasksHeader.should('have.text', 'Get data flowing')
      homePage.tasks.should('have.length', 4)
      homePage.task(ONBOARDING.TASK_ADAPTER).should('contain.text', 'From Devices To HiveMQ Edge')
      homePage.task(ONBOARDING.TASK_BRIDGE).should('contain.text', 'From HiveMQ Edge to the Enterprise')
      homePage.task(ONBOARDING.TASK_CLOUD).should('contain.text', 'Connect To HiveMQ Cloud')
      homePage.task(ONBOARDING.TASK_PULSE).should('contain.text', 'Connect to HiveMQ Pulse')

      homePage.taskSections(ONBOARDING.TASK_PULSE).should('have.length', 1)
      homePage.taskSection(ONBOARDING.TASK_PULSE, 0).within(() => {
        pulseActivationPanel.trigger.should('have.text', 'Activate Pulse')
        pulseActivationPanel.trigger.click()
      })

      pulseActivationPanel.form.should('be.visible')
      pulseActivationPanel.status.should('contain.text', 'Pulse is not activated')
    })

    it('should activate the Pulse Agent', () => {
      cy.location().should((loc) => {
        expect(loc.pathname).to.eq('/app')
      })

      cy.wait('@getCapabilities')

      homePage.taskSection(ONBOARDING.TASK_PULSE, 0).within(() => {
        pulseActivationPanel.trigger.click()
      })

      pulseActivationPanel.form.should('be.visible')
      pulseActivationPanel.status.should('contain.text', 'Pulse is not activated')
      pulseActivationPanel.submitButton.should('be.disabled')
      pulseActivationPanel
        .field(undefined)
        .textarea.should('have.attr', 'placeholder', 'Paste or drop the activation token here')
        .should('not.have.attr', 'aria-invalid')

      pulseActivationPanel.field(undefined).textarea.type('my-activation-token')
      pulseActivationPanel.field(undefined).textarea.should('have.attr', 'aria-invalid', 'true')
      pulseActivationPanel
        .field(undefined)
        .errors.should('contain.text', 'The token is not a completely valid JSON object conforming to JWT format')

      pulseActivationPanel.field(undefined).textarea.clear()
      pulseActivationPanel.field(undefined).textarea.type(MOCK_JWT)
      pulseActivationPanel.submitButton.should('not.be.disabled')
      pulseActivationPanel.submitButton.click()

      homePage.toast.success.should('contain.text', 'The Pulse Agent has been successfully activated')
      homePage.toast.close()

      homePage.taskSection(ONBOARDING.TASK_PULSE, 0).within(() => {
        pulseActivationPanel.trigger.click()
      })
      pulseActivationPanel.form.should('be.visible')
      pulseActivationPanel.status.should('contain.text', 'Pulse is activated')
    })
  })
})
