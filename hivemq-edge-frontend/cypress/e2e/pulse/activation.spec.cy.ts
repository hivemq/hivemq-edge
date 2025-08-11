import { MOCK_JWT } from '@/__test-utils__/mocks.ts'
import type { ProtocolAdapter } from '@/api/__generated__'
import { MOCK_CAPABILITY_PERSISTENCE, MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'
import { drop, factory, primaryKey } from '@mswjs/data'

import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
import { loginPage, homePage, pulseActivationPanel } from 'cypress/pages'
import { ONBOARDING } from '../../utils/constants.utils.ts'

describe('Pulse Client Activation', () => {
  const mswDB = factory({
    capabilities: {
      id: primaryKey(String),
      json: String,
    },
  })

  beforeEach(() => {
    drop(mswDB)

    cy_interceptCoreE2E()

    // Load the mock capabilities into the mock databases
    mswDB.capabilities.create({
      id: 'capabilities',
      json: JSON.stringify({
        items: [MOCK_CAPABILITY_PERSISTENCE],
      }),
    })

    cy.intercept<ProtocolAdapter>('GET', '/api/v1/frontend/capabilities', (req) => {
      const data = mswDB.capabilities.findFirst({
        where: {
          id: {
            equals: 'capabilities',
          },
        },
      })
      req.reply(200, JSON.parse(data.json))
    }).as('getCapabilities')

    cy.intercept<ProtocolAdapter>('POST', '/api/v1/management/pulse/activation-token', (req) => {
      mswDB.capabilities.update({
        where: {
          id: {
            equals: 'capabilities',
          },
        },

        data: {
          json: JSON.stringify({
            items: [MOCK_CAPABILITY_PERSISTENCE, MOCK_CAPABILITY_PULSE_ASSETS],
          }),
        },
      })
      req.reply(200)
    }).as('activatePulse')

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

      homePage.taskSections(ONBOARDING.TASK_PULSE).should('have.length', 2)
      homePage.taskSection(ONBOARDING.TASK_PULSE, 0).within(() => {
        pulseActivationPanel.trigger.should('have.text', 'Activate Pulse')
        pulseActivationPanel.trigger.click()
      })

      pulseActivationPanel.form.should('be.visible')
      pulseActivationPanel.status.should('contain.text', 'Pulse is not activated')
    })

    it('should activate the pulse client', () => {
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

      homePage.toast.success.should('contain.text', 'The Pulse Client has been successfully activated')
      homePage.toast.close()

      homePage.taskSection(ONBOARDING.TASK_PULSE, 0).within(() => {
        pulseActivationPanel.trigger.click()
      })
      pulseActivationPanel.form.should('be.visible')
      pulseActivationPanel.status.should('contain.text', 'Pulse is activated')
    })
  })
})
