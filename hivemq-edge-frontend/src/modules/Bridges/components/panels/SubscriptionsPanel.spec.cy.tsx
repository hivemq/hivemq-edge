/// <reference types="cypress" />
import { useForm } from 'react-hook-form'
import type { FC } from 'react'
import { Button } from '@chakra-ui/react'

import type { Bridge } from '@/api/__generated__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

import SubscriptionsPanel from './SubscriptionsPanel.tsx'
import type { SubscriptionType } from '@/modules/Bridges/types.ts'
import { MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

interface TestingComponentProps {
  onSubmit: (data: Bridge) => void
  defaultValues: Bridge
  type?: SubscriptionType
}

const MOCK_TYPE: SubscriptionType = 'remoteSubscriptions'

const TestingComponent: FC<TestingComponentProps> = ({ onSubmit, defaultValues, type = MOCK_TYPE }) => {
  const form = useForm<Bridge>({
    mode: 'all',
    criteriaMode: 'all',
    defaultValues: defaultValues,
  })
  return (
    <div>
      <form id="bridge-form" onSubmit={form.handleSubmit(onSubmit)}>
        <SubscriptionsPanel form={form} type={type} />
      </form>
      <Button variant="primary" type="submit" form="bridge-form" data-testid="form-submit" mt={8}>
        Submit
      </Button>
    </div>
  )
}

describe('SubscriptionsPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)

    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [] })
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [] })
    cy.intercept('/api/v1/management/bridges', { items: [] })
    cy.intercept('/api/v1/management/topic-filters', { statusCode: 203, log: false })
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/frontend/capabilities', { statusCode: 404 })
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
        // TODO[NVL] CTooltip seems to generate false positives
        region: { enabled: false },
      },
    })
    cy.percySnapshot('Component: SubscriptionsPanel')
  })

  it('should initialise with OpenAPI defaults', () => {
    cy.intercept('/api/v1/frontend/capabilities', { statusCode: 404 })
    cy.intercept('/api/v1/management/topic-filters', {
      items: [
        MOCK_TOPIC_FILTER,
        {
          topicFilter: 'another/filter',
          description: 'This is a topic filter',
        },
      ],
    })
    cy.mountWithProviders(<TestingComponent onSubmit={cy.stub} defaultValues={mockBridge} />)
    cy.getByTestId('bridge-subscription-add').click()
    // force validation to trigger error messages.
    cy.getByTestId(`form-submit`).click()

    cy.getByTestId(`${MOCK_TYPE}.0.filters`).realPress('Tab') // switch the focus for a11y testing
    cy.getByTestId(`${MOCK_TYPE}.0.destination`).realPress('Tab') // switch the focus for a11y testing

    // TODO[NVL] There is a general problem with HTML validation and feedback. To explore
    // cy.getByTestId(`${MOCK_TYPE}.0.filters`).find('label').should('have.attr', 'data-invalid')
    cy.getByTestId(`${MOCK_TYPE}.0.destination`).find('label').should('have.attr', 'data-invalid')
  })

  describe('mqtt-persistence capability', () => {
    it('should render properly the persist options', () => {
      cy.intercept('/api/v1/frontend/capabilities', MOCK_CAPABILITIES)
      cy.mountWithProviders(
        <TestingComponent
          onSubmit={cy.stub}
          defaultValues={{ ...mockBridge, persist: true }}
          type="localSubscriptions"
        />
      )

      cy.getByTestId('localSubscriptions.0.advanced').click()
      cy.getByTestId('localSubscriptions.0.queueLimit').find('input').should('be.disabled')
      cy.getByTestId('localSubscriptions.0.maxQoS.options').find('label').eq(2).click()
      cy.getByTestId('localSubscriptions.0.queueLimit').find('input').should('not.be.disabled')
    })

    it('should disable the persist options if the flag is not activated', () => {
      cy.intercept('/api/v1/frontend/capabilities', MOCK_CAPABILITIES)
      cy.mountWithProviders(
        <TestingComponent
          onSubmit={cy.stub}
          defaultValues={{ ...mockBridge, persist: false }}
          type="localSubscriptions"
        />
      )

      cy.getByTestId('localSubscriptions.0.advanced').click()
      cy.getByTestId('localSubscriptions.0.queueLimit').find('input').should('be.disabled')
      cy.getByTestId('localSubscriptions.0.maxQoS.options').find('label').eq(2).click()
      cy.getByTestId('localSubscriptions.0.queueLimit').find('input').should('be.disabled')
    })

    it('should not render the persist options if mqtt-persistence is not in the capabilities', () => {
      cy.intercept('/api/v1/frontend/capabilities', { items: [] })
      cy.mountWithProviders(
        <TestingComponent
          onSubmit={cy.stub}
          defaultValues={{ ...mockBridge, persist: false }}
          type="localSubscriptions"
        />
      )

      cy.getByTestId('localSubscriptions.0.advanced').click()
      cy.getByTestId('localSubscriptions.0.queueLimit').should('not.exist')
    })
  })
})
