/// <reference types="cypress" />

import type { WidgetProps } from '@rjsf/utils'

import { BehaviorPolicyType } from '@datahub/types.ts'

import { BehaviorModelReadOnlyDisplay } from './BehaviorModelReadOnlyDisplay.tsx'

// @ts-ignore No need for the whole props for testing
const MOCK_PROPS: WidgetProps = {
  id: 'model',
  label: 'Behavior Model',
  name: 'model-readonly',
  onBlur: () => undefined,
  onChange: () => undefined,
  onFocus: () => undefined,
  schema: {},
  options: {},
}

describe('BehaviorModelReadOnlyDisplay', () => {
  it('should render the selected model with title in a disabled input', () => {
    cy.mountWithProviders(<BehaviorModelReadOnlyDisplay {...MOCK_PROPS} value={BehaviorPolicyType.MQTT_EVENT} />)

    cy.get('input#model').should('have.value', 'MQTT - Events')
    cy.get('input#model').should('be.disabled')
  })

  it('should display the model title for Publish.quota', () => {
    cy.mountWithProviders(<BehaviorModelReadOnlyDisplay {...MOCK_PROPS} value={BehaviorPolicyType.PUBLISH_QUOTA} />)

    cy.get('input#model').should('have.value', 'Publish - Quota')
  })

  it('should display the model title for Publish.duplicate', () => {
    cy.mountWithProviders(<BehaviorModelReadOnlyDisplay {...MOCK_PROPS} value={BehaviorPolicyType.PUBLISH_DUPLICATE} />)

    cy.get('input#model').should('have.value', 'Publish - Duplicate')
  })

  it('should render the label', () => {
    cy.mountWithProviders(<BehaviorModelReadOnlyDisplay {...MOCK_PROPS} value={BehaviorPolicyType.MQTT_EVENT} />)

    cy.contains('Behavior Model').should('exist')
  })

  it('should display fallback text when no model is selected', () => {
    cy.mountWithProviders(<BehaviorModelReadOnlyDisplay {...MOCK_PROPS} value={undefined} />)

    cy.get('input#model').should('have.value', 'No model selected')
  })

  it('should display the internal ID when model is not found', () => {
    cy.mountWithProviders(<BehaviorModelReadOnlyDisplay {...MOCK_PROPS} value="Unknown.model" />)

    cy.get('input#model').should('have.value', 'Unknown.model')
  })

  it('should be read-only and disabled', () => {
    cy.mountWithProviders(<BehaviorModelReadOnlyDisplay {...MOCK_PROPS} value={BehaviorPolicyType.MQTT_EVENT} />)

    cy.get('input#model').should('have.attr', 'readonly')
    cy.get('input#model').should('be.disabled')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<BehaviorModelReadOnlyDisplay {...MOCK_PROPS} value={BehaviorPolicyType.MQTT_EVENT} />)
    cy.checkAccessibility()
    cy.checkI18nKeys()
  })
})
