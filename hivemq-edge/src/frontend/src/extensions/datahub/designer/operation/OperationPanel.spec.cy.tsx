/// <reference types="cypress" />

import { Button } from '@chakra-ui/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { DataHubNodeType } from '@datahub/types.ts'
import { getNodePayload } from '@datahub/utils/node.utils.ts'
import { OperationPanel } from './OperationPanel.tsx'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: '3',
            type: DataHubNodeType.OPERATION,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.OPERATION),
          },
        ],
      },
    }}
  >
    {children}
    <Button variant="primary" type="submit" form="datahub-node-form">
      SUBMIT{' '}
    </Button>
  </MockStoreWrapper>
)

describe('OperationPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the fields for a Validator', () => {
    cy.mountWithProviders(<OperationPanel selectedNode="3" />, { wrapper })

    // first select
    cy.get('label#root_functionId-label').should('contain.text', 'Function')
    cy.get('label#root_functionId-label + div').should('contain.text', '')
    cy.get('label#root_functionId-label + div').click()
    cy.get('label#root_functionId-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(0)
      .should('contain.text', 'System.log')
    cy.get('label#root_functionId-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(1)
      .should('contain.text', 'Delivery.redirectTo')

    // TODO[18841] Editors for the functions need to be tested
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<OperationPanel selectedNode="3" />, { wrapper })

    cy.checkAccessibility()
    cy.percySnapshot('Component: OperationPanel')
  })
})
