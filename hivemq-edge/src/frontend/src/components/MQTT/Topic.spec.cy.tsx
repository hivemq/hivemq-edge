/// <reference types="cypress" />

import { MOCK_TOPIC_REF1 } from '@/__test-utils__/react-flow/topics.ts'

import { formatTopicString } from '@/components/MQTT/topic-utils.ts'

import Topic from './Topic.tsx'

describe('Topic', () => {
  beforeEach(() => {
    cy.viewport(450, 250)
  })

  it('should render', () => {
    cy.mountWithProviders(<Topic topic={MOCK_TOPIC_REF1} />)

    cy.getByTestId('topic-wrapper').should('contain.text', formatTopicString('root/topic/ref/1'))
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<Topic topic={MOCK_TOPIC_REF1} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: Topic')
  })
})
