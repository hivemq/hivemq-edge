import type { FC } from 'react'
import type { JSONSchema7 } from 'json-schema'
import type { FormProps } from '@rjsf/core'
import type { UiSchema } from '@rjsf/utils'
import { CustomFormTesting } from '@/__test-utils__/rjsf/CustomFormTesting'

import { BehaviorModelSelectDropdown } from '@datahub/components/forms/BehaviorModelSelectDropdown.tsx'
import { BehaviorPolicyType } from '@datahub/types.ts'

const mockBehaviorModelSelectDropdownUISchema: UiSchema = {
  model: {
    'ui:widget': 'datahub:behavior-model-selector-dropdown',
  },
}

const mockBehaviorModelSelectDropdownSchema: JSONSchema7 = {
  type: 'object',
  required: ['model'],
  properties: {
    model: {
      title: 'Behavior Model',
      description: 'Select the behavior model you want to use for this policy.',
      type: 'string',
      enum: [BehaviorPolicyType.MQTT_EVENT, BehaviorPolicyType.PUBLISH_DUPLICATE, BehaviorPolicyType.PUBLISH_QUOTA],
    },
  },
}

type FormPropsStubs = Pick<FormProps<unknown>, 'onChange' | 'onBlur' | 'onFocus'>
interface MockComponentProps extends FormPropsStubs {
  formData?: { model?: BehaviorPolicyType }
  disabled?: boolean
  readonly?: boolean
}

const MockBehaviorModelSelectDropdown: FC<MockComponentProps> = ({ formData, disabled, readonly, ...props }) => {
  const uiSchema = disabled
    ? {
        model: {
          'ui:widget': 'datahub:behavior-model-selector-dropdown',
          'ui:disabled': true,
        },
      }
    : readonly
      ? {
          model: {
            'ui:widget': 'datahub:behavior-model-selector-dropdown',
            'ui:readonly': true,
          },
        }
      : mockBehaviorModelSelectDropdownUISchema

  return (
    <CustomFormTesting
      schema={mockBehaviorModelSelectDropdownSchema}
      uiSchema={uiSchema}
      widgets={{
        'datahub:behavior-model-selector-dropdown': BehaviorModelSelectDropdown,
      }}
      formData={formData || { model: BehaviorPolicyType.MQTT_EVENT }}
      {...props}
    />
  )
}

describe('BehaviorModelSelectDropdown', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render the select with default value', () => {
    cy.mountWithProviders(<MockBehaviorModelSelectDropdown />)

    cy.get('[role="combobox"]').should('exist')
    cy.contains('MQTT - Events').should('be.visible')
  })

  it('should show all three models with descriptions when opened', () => {
    cy.mountWithProviders(<MockBehaviorModelSelectDropdown />)

    // Open the dropdown
    cy.get('[role="combobox"]').click()

    // Wait for options to appear
    cy.get('[role="option"]').should('have.length', 3)

    // Check all three models are shown
    cy.get('[role="option"]').contains('MQTT - Events').should('exist')
    cy.get('[role="option"]').contains('Publish - Duplicate').should('exist')
    cy.get('[role="option"]').contains('Publish - Quota').should('exist')

    // Check descriptions are visible
    cy.contains('The MQTT - Events behavior model allows you to intercept specific MQTT packets').should('exist')
    cy.contains('The Publish - Duplicate model identifies consecutive identical client messages').should('exist')
    cy.contains('The Publish - Quota model tracks the number of MQTT PUBLISH messages').should('exist')
  })

  it('should show state and transition counts in options', () => {
    cy.mountWithProviders(<MockBehaviorModelSelectDropdown />)

    cy.get('[role="combobox"]').click()

    // Check that state/transition counts are visible
    cy.contains(/\d+ states?/).should('exist')
    cy.contains(/\d+ transitions?/).should('exist')
  })

  it('should show "End states:" label with SUCCESS/FAILED badges', () => {
    cy.mountWithProviders(<MockBehaviorModelSelectDropdown />)

    cy.get('[role="combobox"]').click()

    cy.contains('End states:').should('be.visible')
    cy.contains('SUCCESS').should('be.visible')
    cy.contains('FAILED').should('be.visible')
  })

  it('should show "Arguments Required" badge for Publish.quota', () => {
    cy.mountWithProviders(<MockBehaviorModelSelectDropdown />)

    cy.get('[role="combobox"]').click()

    // Wait for options to appear and check for badge within options
    cy.get('[role="option"]').should('have.length', 3)
    cy.get('[role="option"]').contains('Publish - Quota').should('exist')
    cy.contains('Arguments Required').should('exist')
  })

  it('should select a model and update form data', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<MockBehaviorModelSelectDropdown onChange={onChange} />)

    // Open dropdown
    cy.get('[role="combobox"]').click()

    // Wait for options and click on Publish.quota
    cy.get('[role="option"]').should('have.length', 3)
    cy.get('[role="option"]').contains('Publish - Quota').click()

    cy.get('@onChange').should('have.been.called')
    cy.get('@onChange').then((stub) => {
      const onChangeStub = stub as unknown as Cypress.Agent<sinon.SinonStub>
      const lastCallArgs = onChangeStub.lastCall.args[0]
      expect(lastCallArgs.formData.model).to.equal(BehaviorPolicyType.PUBLISH_QUOTA)
    })

    // Verify the form data was updated - that's the important assertion
    // Visual verification is already done by checking onChange was called with correct data
  })

  it('should allow switching between models', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<MockBehaviorModelSelectDropdown onChange={onChange} />)

    // Select Publish.duplicate
    cy.get('[role="combobox"]').click()
    cy.contains('Publish - Duplicate').click()

    // Select Publish.quota
    cy.get('[role="combobox"]').click()
    cy.contains('Publish - Quota').click()

    // Select back to Mqtt.events
    cy.get('[role="combobox"]').click()
    cy.contains('MQTT - Events').click()

    cy.get('@onChange').should('have.callCount', 3)
  })

  it('should be disabled when disabled prop is true', () => {
    cy.mountWithProviders(<MockBehaviorModelSelectDropdown disabled />)

    // Check the input is disabled
    cy.get('#root_model').should('be.disabled')

    // Try to click and verify dropdown doesn't open
    cy.get('[role="combobox"]').click({ force: true })
    cy.get('[role="option"]').should('not.exist')
  })

  it('should be readonly when readonly prop is true', () => {
    cy.mountWithProviders(<MockBehaviorModelSelectDropdown readonly />)

    // Check the input is disabled (readonly is treated as disabled in Select)
    cy.get('#root_model').should('be.disabled')
  })

  it('should render with no initial selection', () => {
    cy.mountWithProviders(<MockBehaviorModelSelectDropdown formData={{}} />)

    cy.get('[role="combobox"]').should('exist')
    // Placeholder or empty state should be shown
    cy.get('[role="combobox"]').should('not.contain', 'MQTT - Events')
  })

  it('should clear selection when clear button is clicked', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<MockBehaviorModelSelectDropdown onChange={onChange} />)

    // Find and click the clear indicator using ARIA label (selector priority #2)
    cy.get('[aria-label="Clear selected options"]').click()

    cy.get('@onChange').should('have.been.called')
    cy.get('@onChange').then((stub) => {
      const onChangeStub = stub as unknown as Cypress.Agent<sinon.SinonStub>
      const lastCallArgs = onChangeStub.lastCall.args[0]
      expect(lastCallArgs.formData.model).to.be.undefined
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<MockBehaviorModelSelectDropdown />)
    cy.checkAccessibility()
    cy.checkI18nKeys()
  })
})
