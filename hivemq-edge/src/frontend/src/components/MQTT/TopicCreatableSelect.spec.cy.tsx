/// <reference types="cypress" />

import { MultiTopicsCreatableSelect, SingleTopicCreatableSelect } from './TopicCreatableSelect.tsx'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockClientSubscription } from '@/api/hooks/useClientSubscriptions/__handlers__'

const MOCK_ID = 'my-id'

describe.skip('SingleTopicCreatableSelect', () => {
  beforeEach(() => {
    cy.viewport(450, 250)
  })

  it('should render', () => {
    cy.mountWithProviders(
      <SingleTopicCreatableSelect options={[]} isLoading={false} id={MOCK_ID} value="" onChange={cy.stub()} />
    )

    cy.get('#my-id').click()
    cy.get('div').contains('No topic loaded')
  })

  it('should render', () => {
    const mockOptions = ['topic/1', 'topic/2']

    cy.mountWithProviders(
      <SingleTopicCreatableSelect
        options={mockOptions}
        isLoading={false}
        id={MOCK_ID}
        value={mockOptions[0]}
        onChange={cy.stub()}
      />
    )
    cy.get('#my-id').contains('topic/1')
    cy.getByAriaLabel('Clear selected options').should('be.visible')
    cy.get('#my-id').click()
    cy.get('div').contains('topic/1')
    cy.get('div').contains('topic/2')
  })

  it('should be accessible', () => {
    const mockOptions = ['topic/1', 'topic/2']
    cy.injectAxe()

    cy.mountWithProviders(
      <SingleTopicCreatableSelect
        options={mockOptions}
        isLoading={false}
        id={MOCK_ID}
        value={mockOptions[0]}
        onChange={cy.stub()}
      />
    )
    cy.get('#my-id').click()
    cy.checkAccessibility()
    cy.percySnapshot('Component: TopicCreatableSelect')
  })
})

describe('MultiTopicsCreatableSelect', () => {
  beforeEach(() => {
    cy.viewport(450, 250)
  })

  it('should render an empty component', () => {
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [] }).as('getConfig1')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [] }).as('getConfig2')
    cy.intercept('/api/v1/management/bridges', { items: [] }).as('getConfig3')
    cy.intercept('/api/v1/management/client/filters', [])

    cy.mountWithProviders(<MultiTopicsCreatableSelect id={MOCK_ID} value={[]} onChange={cy.stub()} />)

    cy.wait(['@getConfig1', '@getConfig2', '@getConfig3'])
    cy.get('#my-id').click()
    cy.get('#my-id').find('[role="listbox"]').contains('No topic loaded')
  })

  it('should render a single topic', () => {
    const mockOnChange = cy.stub().as('onChange')
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getConfig1')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getConfig2')
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getConfig3')
    cy.intercept('/api/v1/management/client/filters', [mockClientSubscription])

    cy.mountWithProviders(<MultiTopicsCreatableSelect id={MOCK_ID} value={[]} onChange={mockOnChange} />)
    cy.wait(['@getConfig1', '@getConfig2', '@getConfig3'])

    cy.get('#my-id').click()

    cy.get('#my-id').find('[role="listbox"]').contains('#')

    cy.get('#my-id').type('123')
    cy.get('#my-id').find('[role="listbox"]').contains('Add the topic ... 123')
    // cy.get('#my-id').type('{Enter}')

    cy.get('@onChange').should('not.have.been.called')
    cy.get('[role="option"]').eq(0).click()
    cy.get('@onChange').should('have.been.calledWith', ['123'])
  })

  it('should render multiple topics', () => {
    const mockOnChange = cy.stub().as('onChange')
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getConfig1')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getConfig2')
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getConfig3')
    cy.intercept('/api/v1/management/client/filters', [mockClientSubscription])

    cy.mountWithProviders(<MultiTopicsCreatableSelect id={MOCK_ID} value={['old topic']} onChange={mockOnChange} />)
    cy.wait(['@getConfig1', '@getConfig2', '@getConfig3'])

    cy.get('#my-id').contains('old topic')
    cy.get('#my-id').click()

    cy.get('#my-id').find('[role="listbox"]').contains('#')

    cy.get('#my-id').type('123')
    cy.get('#my-id').find('[role="listbox"]').contains('Add the topic ... 123')

    cy.get('@onChange').should('not.have.been.called')
    cy.get('[role="option"]').eq(0).click()
    cy.get('@onChange').should('have.been.calledWith', ['old topic', '123'])

    cy.clearInterceptList('@getConfig3')
  })
})
