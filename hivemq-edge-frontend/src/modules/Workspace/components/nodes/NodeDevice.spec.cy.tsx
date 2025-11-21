import { MOCK_PROTOCOL_SIMULATION } from '@/__test-utils__/adapters'
import { MockAdapterType } from '@/__test-utils__/adapters/types.ts'
import { MOCK_NODE_DEVICE } from '@/__test-utils__/react-flow/nodes.ts'
import { mockReactFlow } from '@/__test-utils__/react-flow/providers.tsx'
import { CustomNodeTesting } from '@/__test-utils__/react-flow/CustomNodeTesting.tsx'
import { MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'

import { NodeDevice } from '@/modules/Workspace/components/nodes/index.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

describe('NodeDevice', () => {
  beforeEach(() => {
    cy.viewport(400, 400)
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [MOCK_PROTOCOL_SIMULATION] })
    cy.intercept('GET', '/api/v1/management/protocol-adapters/adapters/**/tags', (req) => {
      const pathname = new URL(req.url).pathname
      const id = pathname.split('/')[6]

      req.reply(200, { items: MOCK_DEVICE_TAGS(id, MockAdapterType.SIMULATION) })
    })
  })

  it('should render properly', () => {
    cy.mountWithProviders(mockReactFlow(<NodeDevice {...MOCK_NODE_DEVICE} />))

    cy.getByTestId('device-description')
      .should('have.text', 'Simulation')
      .find('svg')
      .should('have.attr', 'data-type', 'SIMULATION')

    cy.getByTestId('device-capabilities').find('svg').as('capabilities').should('have.length', 1)
    cy.get('@capabilities').eq(0).should('have.attr', 'data-type', 'READ')
  })

  it('should render the selected adapter properly', () => {
    cy.mountWithProviders(
      <CustomNodeTesting
        nodes={[{ ...MOCK_NODE_DEVICE, position: { x: 50, y: 100 }, selected: true }]}
        nodeTypes={{ [NodeTypes.DEVICE_NODE]: NodeDevice }}
      />
    )
    cy.getByTestId('device-description').should('contain', 'Simulation')
    cy.getByTestId('node-device-toolbar-metadata').should('have.attr', 'aria-label', 'Edit device tags')

    cy.getByTestId('test-navigate-pathname').should('have.text', '/')
    cy.getByTestId('node-device-toolbar-metadata').click()
    cy.getByTestId('test-navigate-pathname').should('have.text', `/workspace/device/${MOCK_NODE_DEVICE.id}`)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(mockReactFlow(<NodeDevice {...MOCK_NODE_DEVICE} />))
    cy.checkAccessibility()
  })
})
