/// <reference types="cypress" />

import { formatTopicString } from '@/components/MQTT/topic-utils.ts'

import MappingBadge from './MappingBadge.tsx'
import { SelectEntityType } from '../../../../components/MQTT/types'

describe('TopicsContainer', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly all topics', () => {
    cy.mountWithProviders(
      <MappingBadge destinations={['my/first/topic', 'my/second/topic']} type={SelectEntityType.TOPIC} />
    )

    cy.getByTestId('topic-wrapper').should('have.length', 2)
    cy.getByTestId('topic-wrapper').eq(0).should('contain.text', formatTopicString('my/first/topic'))
    cy.getByTestId('topic-wrapper').eq(1).should('contain.text', formatTopicString('my/second/topic'))

    cy.getByTestId('topics-show-more').should('not.exist')
    cy.getByTestId('topic-wrapper').find('svg').should('have.attr', 'aria-label', 'Topic')
  })

  it('should render properly all tags', () => {
    cy.mountWithProviders(<MappingBadge destinations={['my/first/tag']} type={SelectEntityType.TAG} />)

    cy.getByTestId('topic-wrapper').should('have.length', 1)
    cy.getByTestId('topic-wrapper').eq(0).should('contain.text', formatTopicString('my/first/tag'))

    cy.getByTestId('topics-show-more').should('not.exist')
    cy.getByTestId('topic-wrapper').find('svg').should('have.attr', 'aria-label', 'Tag')
  })

  it('should render properly a show more if too many topics', () => {
    cy.mountWithProviders(
      <MappingBadge
        destinations={['my/first/topic', 'my/second/topic', 'my/second/topic']}
        type={SelectEntityType.TOPIC}
      />
    )

    cy.getByTestId('topic-wrapper').should('have.length', 2)
    cy.getByTestId('topic-wrapper').eq(0).should('contain.text', formatTopicString('my/first/topic'))
    cy.getByTestId('topic-wrapper').eq(1).should('contain.text', formatTopicString('my/second/topic'))

    cy.getByTestId('topics-show-more').should('be.visible').should('contain.text', '+1')
  })
})
