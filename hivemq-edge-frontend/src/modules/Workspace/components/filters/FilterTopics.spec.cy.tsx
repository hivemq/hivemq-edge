import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_DEVICE_TAG_ADDRESS_MODBUS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import {
  MOCK_NORTHBOUND_MAPPING,
  MOCK_SOUTHBOUND_MAPPING,
} from '@/api/hooks/useProtocolAdapters/__handlers__/mapping.mocks.ts'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { formatTopicString } from '@/components/MQTT/topic-utils.ts'
import { FilterTopics } from '@/modules/Workspace/components/filters/index.ts'

describe('FilterTopics', () => {
  beforeEach(() => {
    cy.viewport(800, 800)

    cy.intercept('/api/v1/management/protocol-adapters/northboundMappings', {
      items: [MOCK_NORTHBOUND_MAPPING],
    })
    cy.intercept('/api/v1/management/protocol-adapters/southboundMappings', { items: [MOCK_SOUTHBOUND_MAPPING] })
    cy.intercept('/api/v1/management/protocol-adapters/tags', {
      items: [{ name: 'test/tag1', definition: MOCK_DEVICE_TAG_ADDRESS_MODBUS }],
    })
    cy.intercept('/api/v1/management/topic-filters', {
      items: [MOCK_TOPIC_FILTER],
    })
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getBridges')
  })

  it('should render properly', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<FilterTopics onChange={onChange} />)

    cy.get('[role="group"] label#workspace-filter-topics-label').should('have.text', 'Topics')
    cy.get('[role="group"] #react-select-topics-placeholder').should('have.text', 'Select topics to trace ...')
    cy.get('[role="group"] #workspace-filter-topics-trigger').click()
    cy.get('#react-select-topics-listbox [role="listbox"]').within(() => {
      cy.get('[role="option"]').should('have.length', 2)
      cy.get('[role="option"]').eq(0).should('contain.text', formatTopicString('my/topic'))
      cy.get('[role="option"]').eq(1).should('contain.text', formatTopicString('test/tag1'))
    })

    cy.get('[role="group"] #workspace-filter-topics-trigger').type('test{enter}')
    cy.getByTestId('workspace-filter-topics-values').should('have.length', 1)
    cy.getByTestId('workspace-filter-topics-values').eq(0).should('contain.text', 'test/tag1')

    cy.get('@onChange').should('have.been.calledWith', [
      {
        label: 'test/tag1',
        value: 'test/tag1',
        type: 'TAG',
      },
    ])

    cy.get('[role="group"] #workspace-filter-topics-trigger').type('topic{enter}')
    cy.getByTestId('workspace-filter-topics-values').should('have.length', 2)
    cy.getByTestId('workspace-filter-topics-values').eq(0).should('contain.text', 'test/tag1')
    cy.getByTestId('workspace-filter-topics-values').eq(1).should('contain.text', 'my/topic')
    cy.get('@onChange').should('have.been.calledWith', [
      {
        label: 'test/tag1',
        value: 'test/tag1',
        type: 'TAG',
      },
      {
        label: 'my/topic',
        value: 'my/topic',
        type: 'TOPIC',
      },
    ])

    cy.getByAriaLabel('Clear selected options').click()
    cy.getByTestId('workspace-filter-topics-values').should('have.length', 0)
    cy.get('@onChange').should('have.been.calledWith', [])
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<FilterTopics />)

    cy.checkAccessibility()
  })

  it('should render properly when disabled', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<FilterTopics onChange={onChange} isDisabled />)

    cy.get('[role="group"] label#workspace-filter-topics-label').should('have.text', 'Topics')
    cy.get('[role="group"] #workspace-filter-topics-trigger').should('have.attr', 'aria-disabled', 'true')

    cy.get('[role="group"] #workspace-filter-topics-trigger').click({ force: true })
    cy.get('#react-select-topics-listbox [role="listbox"]').should('not.exist')

    cy.get('@onChange').should('not.have.been.called')
  })
})
