/// <reference types="cypress" />

import type { Edge, Node } from 'reactflow'
import { Text } from '@chakra-ui/react'
import { DataHubNodeType } from '@datahub/types.ts'
import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { getNodePayload } from '@datahub/utils/node.utils.ts'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

import { CopyPasteListener } from '@datahub/components/controls/CopyPasteListener.tsx'

const getWrapperWith = (initNodes: Node[], initEdges?: Edge[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    const { nodes, edges } = useDataHubDraftStore()
    return (
      <MockStoreWrapper
        config={{
          initialState: {
            nodes: initNodes,
            edges: initEdges,
          },
        }}
      >
        {children}
        <Text data-testid="nodes">{nodes.length}</Text>
        <Text data-testid="edges">{edges.length}</Text>
      </MockStoreWrapper>
    )
  }

  return Wrapper
}

describe('CopyPasteListener', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should not copy if no node selected', () => {
    cy.mountWithProviders(<CopyPasteListener render={(n) => <Text data-testid="copied">{n.length}</Text>} />, {
      wrapper: getWrapperWith(
        [
          {
            id: '3',
            type: DataHubNodeType.FUNCTION,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.FUNCTION),
          },
        ],
        []
      ),
    })

    cy.getByTestId('copied').should('have.text', 0)
    cy.getByTestId('nodes').should('have.text', 1)
    cy.get('body').type('{meta}C')
    cy.getByTestId('copied').should('have.text', 0)
    cy.get('body').type('{meta}V')
    cy.getByTestId('nodes').should('have.text', 1)
    cy.getByTestId('copied').should('have.text', 0)
  })

  it('should copy single node', () => {
    cy.mountWithProviders(<CopyPasteListener render={(n) => <Text data-testid="copied">{n.length}</Text>} />, {
      wrapper: getWrapperWith(
        [
          {
            id: '3',
            type: DataHubNodeType.FUNCTION,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.FUNCTION),
            selected: true,
          },
        ],
        []
      ),
    })

    cy.getByTestId('copied').should('have.text', 0)
    cy.getByTestId('nodes').should('have.text', 1)
    cy.get('body').type('{meta}C')
    cy.getByTestId('copied').should('have.text', 1)
    cy.get('body').type('{meta}V')
    cy.getByTestId('nodes').should('have.text', 2)
    cy.get('body').type('{esc}')
    cy.getByTestId('copied').should('have.text', 0)
  })

  it('should copy subgraph', () => {
    cy.mountWithProviders(<CopyPasteListener render={(n) => <Text data-testid="copied">{n.length}</Text>} />, {
      wrapper: getWrapperWith(
        [
          {
            id: '1',
            position: { x: 0, y: 0 },
            data: undefined,
            selected: true,
          },
          {
            id: '2',
            position: { x: 0, y: 0 },
            data: undefined,
            selected: false,
          },
          {
            id: '3',
            position: { x: 0, y: 0 },
            data: undefined,
            selected: true,
          },
        ],
        [
          { id: 'e1', source: '1', target: '3' },
          { id: 'e2', source: '1', target: '2' },
          { id: 'e3', source: '2', target: '3' },
        ]
      ),
    })

    cy.getByTestId('copied').should('have.text', 0)
    cy.getByTestId('nodes').should('have.text', 3)
    cy.getByTestId('edges').should('have.text', 3)
    cy.get('body').type('{meta}C')
    cy.getByTestId('copied').should('have.text', 2)
    cy.get('body').type('{meta}V')
    cy.getByTestId('nodes').should('have.text', 5)
    cy.getByTestId('edges').should('have.text', 4)
    cy.get('body').type('{esc}')
    cy.getByTestId('copied').should('have.text', 0)
  })
})
