/// <reference types="cypress" />
import { useForm } from 'react-hook-form'
import { FC } from 'react'
import { Button } from '@chakra-ui/react'

import { Bridge } from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import SecurityPanel from './SecurityPanel.tsx'

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
        <SecurityPanel form={form} />
      </form>
      <Button variant={'primary'} type={'submit'} form="bridge-form" data-testid={'form-submit'} mt={8}>
        Submit
      </Button>
    </div>
  )
}

describe('SecurityPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TestingComponent onSubmit={cy.stub} defaultValues={mockBridge} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: SecurityPanel')
  })

  it('should render unexpanded', () => {
    const mockOnSubmit = cy.stub().as('onSubmit')
    cy.mountWithProviders(<TestingComponent onSubmit={mockOnSubmit} defaultValues={mockBridge} />)

    cy.getByTestId('form-submit').click()
    cy.get('@onSubmit').should(
      'have.been.calledWith',
      Cypress.sinon.match({ ...mockBridge, tlsConfiguration: { enabled: false } })
    )
  })

  it('should render expanded', () => {
    const mockOnSubmit = cy.stub().as('onSubmit')
    cy.mountWithProviders(
      <TestingComponent
        onSubmit={mockOnSubmit}
        defaultValues={{ ...mockBridge, tlsConfiguration: { enabled: true } }}
      />
    )

    cy.getByTestId('form-submit').click()
    cy.get('@onSubmit').should(
      'have.been.calledWith',
      Cypress.sinon.match({ ...mockBridge, tlsConfiguration: { enabled: true } })
    )
  })
})
