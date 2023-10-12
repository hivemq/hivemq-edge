/// <reference types="cypress" />

import { MultiTopicsCreatableSelect, SingleTopicCreatableSelect } from './TopicCreatableSelect.tsx'
import { mockAdapter, mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

const MOCK_ID = 'my-id'

describe('SingleTopicCreatableSelect', () => {
  beforeEach(() => {
    cy.viewport(450, 250)
  })

  it('should render', () => {
    const mockOnChange = cy.stub().as('onChange')

    cy.mountWithProviders(
      <SingleTopicCreatableSelect options={[]} isLoading={false} id={MOCK_ID} value={''} onChange={mockOnChange} />
    )

    cy.get('#my-id').click()
    cy.get('div').contains('No topic loaded')
  })

  it('should render', () => {
    const mockOnChange = cy.stub().as('onChange')
    const mockOptions = ['topic/1', 'topic/2']

    cy.mountWithProviders(
      <SingleTopicCreatableSelect
        options={mockOptions}
        isLoading={false}
        id={MOCK_ID}
        value={mockOptions[0]}
        onChange={mockOnChange}
      />
    )
    cy.get('#my-id').contains('topic/1')
    cy.getByAriaLabel('Clear selected options').should('be.visible')
    cy.get('#my-id').click()
    cy.get('div').contains('topic/1')
    cy.get('div').contains('topic/2')
  })

  it('should be accessible', () => {
    const mockOnChange = cy.stub().as('onChange')
    const mockOptions = ['topic/1', 'topic/2']
    cy.injectAxe()

    cy.mountWithProviders(
      <SingleTopicCreatableSelect
        options={mockOptions}
        isLoading={false}
        id={MOCK_ID}
        value={mockOptions[0]}
        onChange={mockOnChange}
      />
    )
    cy.get('#my-id').click()
    cy.checkAccessibility()
    cy.percySnapshot('Component: TopicCreatableSelect')
  })
})

describe.only('MultiTopicsCreatableSelect', () => {
  beforeEach(() => {
    cy.viewport(450, 250)
  })

  it('should render an empty component', () => {
    const mockOnChange = cy.stub().as('onChange')
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [] }).as('getConfig1')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [] }).as('getConfig2')
    cy.intercept('/api/v1/management/bridges', { items: [] }).as('getConfig3')

    cy.mountWithProviders(<MultiTopicsCreatableSelect id={MOCK_ID} value={[]} onChange={mockOnChange} />)

    cy.wait(['@getConfig1', '@getConfig2', '@getConfig3'])
    cy.get('#my-id').click()
    cy.get('#react-select-2-listbox').contains('No topic loaded')
  })

  it('should render a single topic', () => {
    const mockOnChange = cy.stub().as('onChange')
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getConfig1')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getConfig2')
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getConfig3')

    cy.mountWithProviders(<MultiTopicsCreatableSelect id={MOCK_ID} value={[]} onChange={mockOnChange} />)
    cy.wait(['@getConfig1', '@getConfig2', '@getConfig3'])

    cy.get('#my-id').click()

    cy.get('#react-select-3-listbox').contains('#')

    cy.get('#my-id').type('123')
    cy.get('#react-select-3-listbox').contains('Add the topic ... 123')
    // cy.get('#my-id').type('{Enter}')

    cy.get('#react-select-3-option-4').click()

    cy.get('@onChange').should('have.been.calledWith', ['123'])
  })

  it('should render multiple topics', () => {
    const mockOnChange = cy.stub().as('onChange')
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getConfig1')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [mockAdapter] }).as('getConfig2')
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getConfig3')

    cy.mountWithProviders(<MultiTopicsCreatableSelect id={MOCK_ID} value={['old topic']} onChange={mockOnChange} />)
    cy.wait(['@getConfig1', '@getConfig2', '@getConfig3'])

    cy.get('#my-id').contains('old topic')
    cy.get('#my-id').click()

    cy.get('#react-select-4-listbox').contains('#')

    cy.get('#my-id').type('123')
    cy.get('#react-select-4-listbox').contains('Add the topic ... 123')
    // cy.get('#my-id').type('{Enter}')

    cy.get('#react-select-4-option-4').click()

    cy.get('@onChange').should('have.been.calledWith', ['old topic', '123'])

    cy.clearInterceptList('@getConfig3')
  })
})
