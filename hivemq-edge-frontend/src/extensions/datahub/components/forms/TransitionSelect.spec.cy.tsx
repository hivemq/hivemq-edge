/// <reference types="cypress" />

import type { WidgetProps } from '@rjsf/utils'
import { TransitionSelect } from '@datahub/components/forms/TransitionSelect.tsx'
import { MOCK_BEHAVIOR_POLICY_SCHEMA } from '@datahub/designer/behavior_policy/BehaviorPolicySchema.ts'
import type { FiniteStateMachineSchema } from '@datahub/types.ts'

// @ts-ignore
const MOCK_OPTIONS: FiniteStateMachineSchema = MOCK_BEHAVIOR_POLICY_SCHEMA.schema.definitions?.['Publish.quota']

// @ts-ignore No need for the whole props for testing
const MOCK_TRANSITION_PROPS: WidgetProps = {
  id: 'transition',
  label: 'Select a transition',
  name: 'transition-select',
  onBlur: () => undefined,
  onChange: () => undefined,
  onFocus: () => undefined,
  schema: {},
  options: {
    metadata: MOCK_OPTIONS.metadata,
  },
}

describe('TransitionSelect', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render the TransitionSelect', () => {
    cy.injectAxe()

    cy.mountWithProviders(<TransitionSelect {...MOCK_TRANSITION_PROPS} />)

    cy.get('label#transition-label').should('contain.text', 'Select a transition')
    cy.get('div#transition-container').should('contain.text', 'Select...')

    cy.get('label#transition-label + div').click()

    cy.get('div#react-select-transition-listbox').find('[role="option"]').as('optionList')
    cy.get('@optionList').should('have.length', 8)
    cy.get('#react-select-transition-option-0').should('contain.text', 'Mqtt.OnInboundConnect')

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
        'scrollable-region-focusable': { enabled: false },
      },
    })

    cy.get('#react-select-transition-option-3').click()

    cy.get('div#transition-container').should('contain.text', 'Mqtt.OnInboundPublish (Publishing - Publishing)')
  })
})
