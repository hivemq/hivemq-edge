/// <reference types="cypress" />

import { MockStoreWrapper } from '../../__test-utils__/MockStoreWrapper.tsx'
import { DataHubNodeType } from '../../types.ts'
import { getNodePayload } from '../../utils/node.utils.ts'
import { TopicFilterPanel } from '../panels/TopicFilterPanel.tsx'
import { Button } from '@chakra-ui/react'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: '3',
            type: DataHubNodeType.TOPIC_FILTER,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.TOPIC_FILTER),
          },
        ],
      },
    }}
  >
    {children}
    <Button variant={'primary'} type="submit" form="datahub-node-form">
      SUBMIT{' '}
    </Button>
  </MockStoreWrapper>
)

describe('TopicFilterPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the fields for a Validator', () => {
    cy.mountWithProviders(<TopicFilterPanel selectedNode={'3'} />, { wrapper })

    cy.get('h2').eq(0).should('contain.text', 'Topic Filters')
    // first item
    cy.get('label#root_topics_0-label').should('contain.text', 'topics-0')
    cy.get('label#root_topics_0-label + input').should('have.value', 'root/test1')
    // first item
    cy.get('label#root_topics_1-label').should('contain.text', 'topics-1')
    cy.get('label#root_topics_1-label + input').should('have.value', 'root/test2')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TopicFilterPanel selectedNode={'3'} />, { wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: TopicFilterPanel')
  })
})
