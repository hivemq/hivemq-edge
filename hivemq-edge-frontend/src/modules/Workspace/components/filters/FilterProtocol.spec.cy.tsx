import { MOCK_ADAPTER_OPC_UA, MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters/opc-ua.ts'
import { MOCK_PROTOCOL_S7 } from '@/__test-utils__/adapters/s7.ts'
import { MOCK_ADAPTER_SIMULATION, MOCK_PROTOCOL_SIMULATION } from '@/__test-utils__/adapters/simulation.ts'
import { FilterProtocol } from '@/modules/Workspace/components/filters/index.ts'

describe('FilterProtocol', () => {
  beforeEach(() => {
    cy.viewport(800, 800)

    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_SIMULATION, MOCK_PROTOCOL_S7],
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [MOCK_ADAPTER_OPC_UA, { ...MOCK_ADAPTER_OPC_UA, id: 'opcua-boiler' }, MOCK_ADAPTER_SIMULATION],
    })
  })

  it('should render properly', () => {
    const onChange = cy.stub().as('onChange')

    cy.mountWithProviders(<FilterProtocol onChange={onChange} />)

    cy.get('[role="group"] label#workspace-filter-protocol-label').should('have.text', 'Protocols')
    cy.get('[role="group"] #react-select-protocol-placeholder').should('have.text', 'Select Protocols to filter ...')
    cy.get('[role="group"] #workspace-filter-protocol-trigger').click()
    cy.get('#react-select-protocol-listbox [role="listbox"]').within(() => {
      cy.get('[role="option"]').should('have.length', 2)
      cy.get('[role="option"]').eq(0).should('have.text', 'OPC UA')
      cy.get('[role="option"]').eq(1).should('have.text', 'Simulation')
    })

    cy.get('[role="group"] #workspace-filter-protocol-trigger').type('sim{enter}')
    cy.getByTestId('workspace-filter-protocol-values').should('have.length', 1)
    cy.getByTestId('workspace-filter-protocol-values').eq(0).should('have.text', 'Simulation')

    cy.get('@onChange').should('have.been.calledWith', [
      {
        type: 'simulation',
        label: 'Simulation',
      },
    ])

    cy.get('[role="group"] #workspace-filter-protocol-trigger').type('opc{enter}')
    cy.getByTestId('workspace-filter-protocol-values').should('have.length', 2)
    cy.getByTestId('workspace-filter-protocol-values').eq(0).should('have.text', 'Simulation')
    cy.getByTestId('workspace-filter-protocol-values').eq(1).should('have.text', 'OPC UA')
    cy.get('@onChange').should('have.been.calledWith', [
      {
        type: 'simulation',
        label: 'Simulation',
      },
      {
        type: 'opcua',
        label: 'OPC UA',
      },
    ])

    cy.getByAriaLabel('Clear selected options').click()
    cy.getByTestId('workspace-filter-protocol-values').should('have.length', 0)
    cy.get('@onChange').should('have.been.calledWith', [])
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<FilterProtocol />)

    cy.checkAccessibility()
  })
})
