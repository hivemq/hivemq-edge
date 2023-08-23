/// <reference types="cypress" />

import { MOCK_TOPIC } from '@/__test-utils__/react-flow/topics.ts'

import Topic from './Topic.tsx'

describe('Topic', () => {
  beforeEach(() => {
    cy.viewport(450, 250)
  })

  it('should render', () => {
    cy.mountWithProviders(<Topic topic={MOCK_TOPIC} />)

    cy.getByTestId('topic-wrapper').should('contain.text', 'root/topic/ref/1')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<Topic topic={MOCK_TOPIC} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: Topic')
  })
})
