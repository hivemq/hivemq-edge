/// <reference types="cypress" />
import { useForm } from 'react-hook-form'
import { FC } from 'react'
import { Button } from '@chakra-ui/react'

import { Bridge } from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import WebSocketPanel from '@/modules/Bridges/components/panels/WebSocketPanel.tsx'

interface TestingComponentProps {
  onSubmit: (data: Bridge) => void
  defaultValues: Bridge
}

const TestingComponent: FC<TestingComponentProps> = ({ onSubmit, defaultValues }) => {
  const form = useForm<Bridge>({
    mode: 'all',
    criteriaMode: 'all',
    defaultValues: defaultValues,
  })
  return (
    <div>
      <form id="bridge-form" onSubmit={form.handleSubmit(onSubmit)}>
        <WebSocketPanel form={form} />
      </form>
      <Button variant="primary" type="submit" form="bridge-form" data-testid="form-submit" mt={8}>
        Submit
      </Button>
    </div>
  )
}

describe('WebSocketPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    const mockOnSubmit = cy.stub().as('onSubmit')
    cy.mountWithProviders(<TestingComponent onSubmit={mockOnSubmit} defaultValues={mockBridge} />)

    cy.getByTestId('form-websocket-serverPath').should('have.value', '/mqtt')
    cy.getByTestId('form-websocket-subProtocol').should('have.value', 'mqtt')

    cy.getByTestId('form-websocket-enabled').click()
    cy.getByTestId('form-websocket-serverPath').clear()
    cy.getByTestId('form-websocket-serverPath').type('1234')
    cy.getByTestId('form-websocket-subProtocol').clear()
    cy.getByTestId('form-websocket-subProtocol').type('5678')

    cy.getByTestId('form-submit').click()
    cy.get('@onSubmit').should(
      'have.been.calledWith',
      Cypress.sinon.match({
        ...mockBridge,
        websocketConfiguration: { enabled: true, serverPath: '1234', subProtocol: '5678' },
      })
    )
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TestingComponent onSubmit={cy.stub} defaultValues={mockBridge} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: WebSocketPanel')
  })
})
