import { MOCK_NODE_DEVICE } from '@/__test-utils__/react-flow/nodes.ts'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { CustomNodeTesting } from '@/__test-utils__/react-flow/CustomNodeTesting.tsx'

import { NodeDevice } from '@/modules/Workspace/components/nodes/index.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

describe('NodeDevice', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodeDevice {...MOCK_NODE_DEVICE} />))

    cy.getByTestId('device-description')
      .should('have.text', 'Simulation')
      .find('svg')
      .should('have.attr', 'data-type', 'INDUSTRIAL')

    cy.getByTestId('device-capabilities').find('svg').as('capabilities').should('have.length', 2)
    cy.get('@capabilities').eq(0).should('have.attr', 'data-type', 'READ')
    cy.get('@capabilities').eq(1).should('have.attr', 'data-type', 'DISCOVER')
  })

  it('should render the selected adapter properly', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 }, selected: true }]}
        nodeTypes={{ [NodeTypes.DEVICE_NODE]: NodeDevice }}
      />
    )
    cy.getByTestId('device-description').should('contain', 'Simulation')
    cy.get('[role="toolbar"] button').should('have.length', 1)
    cy.get('[role="toolbar"] button').eq(0).should('have.attr', 'aria-label', 'Open the overview panel')

    cy.getByTestId('test-navigate-pathname').should('have.text', '/')
    cy.get('[role="toolbar"] button').eq(0).click()
    cy.getByTestId('test-navigate-pathname').should('have.text', `/workspace/node/${MOCK_NODE_DEVICE.id}`)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeDevice {...MOCK_NODE_DEVICE} />))
    cy.checkAccessibility()
    cy.percySnapshot('Component: NodeDevice')
  })
})