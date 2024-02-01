/// <reference types="cypress" />

import { NodeProps } from 'reactflow'

import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'

import { DataHubNodeType, OperationData } from '../../types.ts'
import { OperationNode } from '@/extensions/datahub/components/nodes/OperationNode.tsx'

const MOCK_NODE_OPERATION: NodeProps<OperationData> = {
  id: 'node-id',
  type: DataHubNodeType.OPERATION,
  data: {},
  ...MOCK_DEFAULT_NODE,
}

describe('OperationNode', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<OperationNode {...MOCK_NODE_OPERATION} selected={true} />))
    cy.getByTestId(`node-title`).should('contain.text', 'Operation')
    cy.getByTestId(`node-model`).should('contain.text', '< not set >')

    cy.get('div[data-handleid]').as('nodeHandles')

    cy.get('@nodeHandles').should('have.length', 2)
    cy.get('@nodeHandles')
      .eq(0)
      .should('have.attr', 'data-handlepos', 'left')
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('target')).to.be.true
      })

    cy.get('@nodeHandles')
      .eq(1)
      .should('have.attr', 'data-handlepos', 'right')
      .should('have.attr', 'data-handleid', 'output')
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('source')).to.be.true
      })
  })

  it('should render properly with arguments', () => {
    const data: NodeProps<OperationData> = {
      ...MOCK_NODE_OPERATION,
      data: { functionId: 'DataHub.transform', metadata: { hasArguments: true } },
    }
    cy.mountWithProviders(mockReactFlow(<OperationNode {...data} selected={true} />))
    cy.getByTestId(`node-title`).should('contain.text', 'Operation')
    cy.getByTestId(`node-model`).should('contain.text', 'DataHub.transform')

    cy.get('div[data-handleid]').as('nodeHandles')
    cy.get('@nodeHandles').should('have.length', 5)
    cy.get('@nodeHandles')
      .eq(2)
      .should('have.attr', 'data-handlepos', 'top')
      .should('have.attr', 'data-handleid', 'serialiser')
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('target')).to.be.true
      })
  })

  it('should render properly with terminal', () => {
    const data: NodeProps<OperationData> = {
      ...MOCK_NODE_OPERATION,
      data: { functionId: 'test', metadata: { isTerminal: true } },
    }
    cy.mountWithProviders(mockReactFlow(<OperationNode {...data} selected={true} />))
    cy.getByTestId(`node-title`).should('contain.text', 'Operation')
    cy.getByTestId(`node-model`).should('contain.text', 'test')

    cy.get('div[data-handleid]').as('nodeHandles')
    cy.get('@nodeHandles').should('have.length', 1)
    cy.get('@nodeHandles')
      .eq(0)
      .should('have.attr', 'data-handlepos', 'left')
      .should('have.attr', 'data-id')
      .then((attr) => {
        expect((attr as unknown as string).endsWith('target')).to.be.true
      })
  })

  // it('should be accessible', () => {
  //   cy.injectAxe()
  //   cy.mountWithProviders(mockReactFlow(<OperationNode {...MOCK_NODE_OPERATION} />))
  //   cy.checkAccessibility()
  //   cy.percySnapshot('Component: DataHub - OperationNode')
  // })
})
