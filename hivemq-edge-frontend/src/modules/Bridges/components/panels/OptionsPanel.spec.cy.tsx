/// <reference types="cypress" />

import type { FC } from 'react'
import { useForm } from 'react-hook-form'

import type { Bridge } from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import OptionsPanel from '@/modules/Bridges/components/panels/OptionsPanel.tsx'
import { Button } from '@chakra-ui/react'

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
        <OptionsPanel form={form} />
      </form>
      <Button variant="primary" type="submit" form="bridge-form" data-testid="form-submit" mt={8}>
        Submit
      </Button>
    </div>
  )
}

describe('OptionsPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TestingComponent onSubmit={cy.stub().as('submit')} defaultValues={mockBridge} />)

    cy.get('#cleanStart').should('be.checked')
    cy.get('#keepAlive').should('have.value', 0)
    cy.get('#sessionExpiry').should('have.value', 0)
    cy.get('#loopPreventionEnabled').should('not.be.checked')
    cy.get('#loopPreventionHopCount').should('have.value', '')
    cy.get('#clientId').should('have.value', 'my-client-id')

    cy.get('#clientId').type('-test123')

    cy.getByTestId('form-submit').click()
    cy.get('@submit').should('have.been.calledWithMatch', { clientId: 'my-client-id-test123' })

    cy.checkAccessibility()
    cy.percySnapshot('Component: OptionsPanel')
  })
})
