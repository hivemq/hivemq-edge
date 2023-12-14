import { FC } from 'react'
/// <reference types="cypress" />
import { useForm } from 'react-hook-form'

import { Bridge } from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import ButtonCTA from '@/components/Chakra/ButtonCTA.tsx'

import { SubscriptionType } from '@/modules/Bridges/types.ts'
import SubscriptionsPanel from './SubscriptionsPanel.tsx'

interface TestingComponentProps {
  onSubmit: (data: Bridge) => void
  defaultValues: Bridge
}

const MOCK_TYPE: SubscriptionType = 'remoteSubscriptions'

const TestingComponent: FC<TestingComponentProps> = ({ onSubmit, defaultValues }) => {
  const form = useForm<Bridge>({
    mode: 'all',
    criteriaMode: 'all',
    defaultValues: defaultValues,
  })
  return (
    <div>
      <form id="bridge-form" onSubmit={form.handleSubmit(onSubmit)}>
        <SubscriptionsPanel form={form} type={MOCK_TYPE} />
      </form>
      <ButtonCTA type={'submit'} form="bridge-form" data-testid={'form-submit'} mt={8}>
        Submit
      </ButtonCTA>
    </div>
  )
}

describe('SubscriptionsPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)

    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [] }).as('getConfig1')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [] }).as('getConfig2')
    cy.intercept('/api/v1/management/bridges', { items: [] }).as('getConfig3')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<TestingComponent onSubmit={cy.stub} defaultValues={mockBridge} />)
    cy.getByTestId('bridge-subscription-add').click()
    cy.getByTestId('bridge-subscription-add').click()
    cy.getByTestId(`${MOCK_TYPE}.0.advanced`).click()
    cy.getByTestId(`${MOCK_TYPE}.0.maxQoS`).should('be.visible')

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[#111] Color-contrast fixed but still not passing. Flaky with expandable panel
        'color-contrast': { enabled: false },
      },
    })
    cy.percySnapshot('Component: SubscriptionsPanel')
  })

  it('should initialise with OpenAPI defaults', () => {
    cy.mountWithProviders(<TestingComponent onSubmit={cy.stub} defaultValues={mockBridge} />)
    cy.getByTestId('bridge-subscription-add').click()
    // force validation to trigger error messages. Better alternative?
    cy.getByTestId('form-submit').click()

    cy.getByTestId(`${MOCK_TYPE}.0.filters`).should('be.visible').find('label').should('not.have.attr', 'data-invalid')

    cy.get('input[id="remoteSubscriptions.0.filters"').type('my topic{Enter}')
    // force validation to trigger error messages. Better alternative?
    cy.getByTestId('form-submit').click()
    cy.getByTestId(`${MOCK_TYPE}.0.destination`).should('be.visible').find('label').should('have.attr', 'data-invalid')
  })
})
