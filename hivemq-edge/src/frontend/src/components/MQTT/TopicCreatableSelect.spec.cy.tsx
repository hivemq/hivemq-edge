/// <reference types="cypress" />

import { SingleTopicCreatableSelect } from './TopicCreatableSelect.tsx'

const MOCK_ID = 'my-id'

describe('TopicCreatableSelect', () => {
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
