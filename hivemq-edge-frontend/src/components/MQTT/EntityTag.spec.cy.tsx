import { MOCK_TOPIC_REF1 } from '@/__test-utils__/react-flow/topics.ts'

import { formatTopicString } from '@/components/MQTT/topic-utils.ts'
import { ClientTag, PLCTag, Topic, TopicFilter } from '@/components/MQTT/EntityTag.tsx'

describe('Topic', () => {
  beforeEach(() => {
    cy.viewport(450, 250)
  })

  it('should render properly and be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<Topic tagTitle={MOCK_TOPIC_REF1} />)
    cy.getByTestId('topic-wrapper').should('contain.text', formatTopicString('root/topic/ref/1'))
    cy.checkAccessibility()
  })
})

describe('PLCTag', () => {
  beforeEach(() => {
    cy.viewport(450, 250)
  })

  it('should render properly and be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<PLCTag tagTitle={MOCK_TOPIC_REF1} />)
    cy.getByTestId('topic-wrapper').should('contain.text', formatTopicString('root/topic/ref/1'))
    cy.checkAccessibility()
  })
})

describe('ClientTag', () => {
  beforeEach(() => {
    cy.viewport(450, 250)
  })

  it('should render properly and be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ClientTag tagTitle={MOCK_TOPIC_REF1} />)
    cy.getByTestId('topic-wrapper').should('contain.text', formatTopicString('root/topic/ref/1'))
    cy.checkAccessibility()
  })
})

describe('TopicFilter', () => {
  beforeEach(() => {
    cy.viewport(450, 250)
  })

  it('should render properly and be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TopicFilter tagTitle={MOCK_TOPIC_REF1} />)
    cy.getByTestId('topic-wrapper').should('contain.text', formatTopicString('root/topic/ref/1'))
    cy.checkAccessibility()
  })
})
