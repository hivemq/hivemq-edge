/// <reference types="cypress" />

import type { Edge, Node } from '@xyflow/react'
import { Table, TableContainer, Tbody, Td, Th, Thead, Tr } from '@chakra-ui/react'
import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'

import DeleteListener from '@datahub/components/controls/DeleteListener.tsx'
import { DesignerStatus } from '@datahub/types.ts'

const getWrapperWith = (initNodes: Node[], initEdges?: Edge[], status?: DesignerStatus) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    const { nodes, edges } = useDataHubDraftStore()
    return (
      <MockStoreWrapper
        config={{
          initialState: {
            nodes: initNodes,
            edges: initEdges,
            status: status,
          },
        }}
      >
        {children}
        <TableContainer>
          <Table>
            <Thead>
              <Tr>
                <Th></Th>
                <Th>count</Th>
                <Th>selected</Th>
              </Tr>
            </Thead>
            <Tbody>
              <Tr>
                <Th>nodes</Th>
                <Td>{nodes.length}</Td>
                <Td>{nodes.filter((node) => node.selected).length}</Td>
              </Tr>
              <Tr>
                <Th>edges</Th>
                <Td>{edges.length}</Td>
                <Td>{edges.filter((edge) => edge.selected).length}</Td>
              </Tr>
            </Tbody>
          </Table>
        </TableContainer>
      </MockStoreWrapper>
    )
  }

  return Wrapper
}

describe('DeleteListener', () => {
  beforeEach(() => {
    cy.viewport(800, 400)
  })

  it('should not render a confirmation if nothing selected', () => {
    cy.mountWithProviders(<DeleteListener />, {
      wrapper: getWrapperWith([], []),
    })

    cy.get("[role='alertdialog']").should('not.exist')
    cy.get('body').type('{backspace}')
    cy.get("[role='alertdialog']").should('not.exist')
  })

  it('should render a confirmation modal when deleting', () => {
    cy.mountWithProviders(<DeleteListener />, {
      wrapper: getWrapperWith(
        [
          {
            id: '1',
            position: { x: 0, y: 0 },
            data: undefined,
            selected: true,
          },
        ],
        []
      ),
    })

    cy.get("[role='alertdialog']").should('not.exist')
    cy.get('body').type('{backspace}')
    cy.get("[role='alertdialog']")
      .should('be.visible')
      .should('contain.text', 'Are you sure you want to delete 1 node? The operation cannot be reversed.')
    cy.getByTestId('confirmation-submit').click()
    cy.get("[role='alertdialog']").should('not.exist')
  })

  it('should render a confirmation modal when deleting multiple elements', () => {
    cy.mountWithProviders(<DeleteListener />, {
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
        [{ id: 'e1', source: '1', target: '3', selected: true }]
      ),
    })

    cy.get('td').then((w) => {
      cy.wrap(w[0]).should('contain.text', 3)
      cy.wrap(w[1]).should('contain.text', 2)
      cy.wrap(w[2]).should('contain.text', 1)
      cy.wrap(w[3]).should('contain.text', 1)
    })

    cy.get("[role='alertdialog']").should('not.exist')
    cy.get('body').type('{backspace}')
    cy.get("[role='alertdialog']").as('confirm')

    cy.get('@confirm')
      .should('be.visible')
      .should(
        'contain.text',
        'Are you sure you want to delete 3 nodes and connections? The operation cannot be reversed.'
      )
    cy.getByTestId('confirmation-submit').click()
    cy.get("[role='alertdialog']").should('not.exist')
    cy.get('td').then((w) => {
      cy.wrap(w[0]).should('contain.text', 1)
      cy.wrap(w[1]).should('contain.text', 0)
      cy.wrap(w[2]).should('contain.text', 0)
      cy.wrap(w[3]).should('contain.text', 0)
    })
  })

  it('should not delete if the designer is readonly', () => {
    cy.mountWithProviders(<DeleteListener />, {
      wrapper: getWrapperWith(
        [
          {
            id: '1',
            position: { x: 0, y: 0 },
            data: undefined,
            selected: true,
          },
        ],
        [],
        DesignerStatus.LOADED
      ),
    })

    cy.get("[role='alertdialog']").should('not.exist')
    cy.get('body').type('{backspace}')
    cy.get("[role='alertdialog']").should('not.exist')
  })
})
