/// <reference types="cypress" />

import { Node } from 'reactflow'
import { CopyPasteListener } from '@datahub/components/controls/CopyPasteListener.tsx'
import { Text } from '@chakra-ui/react'
import { DataHubNodeType, DataPolicyData } from '@datahub/types.ts'
import { MOCK_DEFAULT_NODE } from '@/__test-utils__/react-flow/nodes.ts'
import { MockChecksStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'

const MOCK_NODE_DATA_POLICY: Node<DataPolicyData> = {
  id: 'node-id',
  type: DataHubNodeType.DATA_POLICY,
  data: {},
  ...MOCK_DEFAULT_NODE,
  position: { x: 0, y: 0 },
  selected: true,
}

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockChecksStoreWrapper
    config={{
      node: MOCK_NODE_DATA_POLICY,
    }}
  >
    {children}
  </MockChecksStoreWrapper>
)

describe('CopyPasteListener', () => {
  beforeEach(() => {
    cy.viewport(800, 250)
  })

  it('should renders properly', () => {
    cy.mountWithProviders(<CopyPasteListener render={(n) => <Text data-testid="ss">fdfddf : {n.length}</Text>} />, {
      wrapper,
    })

    // cy.getByTestId('ss').click()
    cy.get('body').type('{meta}C')
    cy.get('body').type('{meta}V')
  })
})
