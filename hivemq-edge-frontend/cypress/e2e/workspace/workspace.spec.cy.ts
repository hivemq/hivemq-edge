import { MOCK_PROTOCOL_HTTP, MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_SIMULATION } from '@/__test-utils__/adapters'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_METRICS } from '@/api/hooks/useGetMetrics/__handlers__'
import { MOCK_DEVICE_TAGS, mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

import { loginPage, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('Workspace', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [MOCK_PROTOCOL_HTTP, MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_SIMULATION],
    }).as('getProtocols')

    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getConfig3')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [mockAdapter_OPCUA, { ...mockAdapter_OPCUA, id: 'opcua-2' }],
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
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
      statusCode: 202,
      log: false,
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/**/southboundMappings', {
      statusCode: 202,
      log: false,
    })

    cy.intercept('GET', '/api/v1/management/protocol-adapters/adapters/**/tags', (req) => {
      const pathname = new URL(req.url).pathname
      const id = pathname.split('/')[6]

      req.reply(200, { items: MOCK_DEVICE_TAGS(id, MockAdapterType.OPC_UA) })
    })
    cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 202, log: false })
    cy.intercept('/api/v1/metrics', { items: MOCK_METRICS })
    cy.intercept('/api/v1/metrics/**/*', { statusCode: 202, log: false })
    cy.intercept('/api/v1/management/events?*', { statusCode: 202, log: false })

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()
  })

  it('should be accessible', { tags: ['@percy'] }, () => {
    cy.injectAxe()
    workspacePage.toolbox.fit.click()
    workspacePage.edgeNode.click()

    cy.checkAccessibility(undefined, {
      rules: {
        region: { enabled: false },
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Page: Workspace')

    // NEW: Snapshot 2: Workspace with node context panel
    workspacePage.bridgeNode(mockBridge.id).click()
    workspacePage.toolbar.overview.click()

    cy.checkAccessibility(undefined, {
      rules: {
        region: { enabled: false },
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Workspace - Node Context Panel')
  })
})
