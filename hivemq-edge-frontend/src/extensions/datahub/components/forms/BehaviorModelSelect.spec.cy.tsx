import type { FC } from 'react'
import type { JSONSchema7 } from 'json-schema'
import type { FormProps } from '@rjsf/core'
import type { UiSchema } from '@rjsf/utils'
import { CustomFormTesting } from '@/__test-utils__/rjsf/CustomFormTesting'

import { BehaviorModelSelect } from '@datahub/components/forms/BehaviorModelSelect.tsx'
import { BehaviorPolicyType } from '@datahub/types.ts'

const mockBehaviorModelSelectUISchema: UiSchema = {
  model: {
    'ui:widget': 'datahub:behavior-model-selector',
  },
}

const mockBehaviorModelSelectSchema: JSONSchema7 = {
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

const MockBehaviorModelSelect: FC<MockComponentProps> = ({ formData, disabled, readonly, ...props }) => {
  const uiSchema = disabled
    ? {
        model: {
          'ui:widget': 'datahub:behavior-model-selector',
          'ui:disabled': true,
        },
      }
    : readonly
      ? {
          model: {
            'ui:widget': 'datahub:behavior-model-selector',
            'ui:readonly': true,
          },
        }
      : mockBehaviorModelSelectUISchema

  return (
    <CustomFormTesting
      schema={mockBehaviorModelSelectSchema}
      uiSchema={uiSchema}
      widgets={{
        'datahub:behavior-model-selector': BehaviorModelSelect,
      }}
      formData={formData || { model: BehaviorPolicyType.MQTT_EVENT }}
      {...props}
    />
  )
}

describe('BehaviorModelSelect', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render all three behavior models with descriptions', () => {
    cy.mountWithProviders(<MockBehaviorModelSelect />)

    cy.get('[role="radiogroup"]').should('exist')

    // Check Mqtt.events model
    cy.contains('MQTT - Events').should('be.visible')
    cy.contains(
      'The Mqtt.events behavior model allows you to intercept specific MQTT packets for further actions.'
    ).should('be.visible')
    cy.contains(/\d+ states?/).should('exist')
    cy.contains(/\d+ transitions?/).should('exist')

    // Check Publish.duplicate model
    cy.contains('Publish - Duplicate').should('be.visible')
    cy.contains('The Publish.duplicate model identifies consecutive identical client messages').should('be.visible')

    // Check Publish.quota model
    cy.contains('Publish - Quota').should('be.visible')
    cy.contains('The Publish.quota model tracks the number of MQTT PUBLISH messages').should('be.visible')
    cy.contains('Arguments Required').should('be.visible')
  })

  it('should show SUCCESS and FAILED badges for models with terminal states', () => {
    cy.mountWithProviders(<MockBehaviorModelSelect />)

    // Check that badges exist
    cy.contains('SUCCESS').should('be.visible')
    cy.contains('FAILED').should('be.visible')
  })

  it('should select a model and update form data', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<MockBehaviorModelSelect onChange={onChange} />)

    // Default selection should be Mqtt.events
    cy.get(`input[value="${BehaviorPolicyType.MQTT_EVENT}"]`).should('be.checked')

    // Click on Publish.quota radio button
    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_QUOTA}"]`).click({ force: true })

    cy.get('@onChange').should('have.been.called')
    cy.get('@onChange').then((stub) => {
      const onChangeStub = stub as unknown as Cypress.Agent<sinon.SinonStub>
      const lastCallArgs = onChangeStub.lastCall.args[0]
      expect(lastCallArgs.formData.model).to.equal(BehaviorPolicyType.PUBLISH_QUOTA)
    })

    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_QUOTA}"]`).should('be.checked')
  })

  it('should highlight selected model with different border', () => {
    cy.mountWithProviders(<MockBehaviorModelSelect formData={{ model: BehaviorPolicyType.PUBLISH_DUPLICATE }} />)

    // Check that the selected model card has different styling
    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_DUPLICATE}"]`)
      .closest('.chakra-card')
      .should('have.css', 'border-width', '2px')
  })

  it('should allow switching between models', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<MockBehaviorModelSelect onChange={onChange} />)

    // Click Publish.duplicate radio
    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_DUPLICATE}"]`).click({ force: true })
    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_DUPLICATE}"]`).should('be.checked')

    // Click Publish.quota radio
    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_QUOTA}"]`).click({ force: true })
    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_QUOTA}"]`).should('be.checked')

    // Click back to Mqtt.events radio
    cy.get(`input[value="${BehaviorPolicyType.MQTT_EVENT}"]`).click({ force: true })
    cy.get(`input[value="${BehaviorPolicyType.MQTT_EVENT}"]`).should('be.checked')

    cy.get('@onChange').should('have.callCount', 3)
  })

  it('should be disabled when disabled prop is true', () => {
    cy.mountWithProviders(<MockBehaviorModelSelect disabled />)

    cy.get(`input[value="${BehaviorPolicyType.MQTT_EVENT}"]`).should('be.disabled')
    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_DUPLICATE}"]`).should('be.disabled')
    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_QUOTA}"]`).should('be.disabled')
  })

  it('should be readonly when readonly prop is true', () => {
    cy.mountWithProviders(<MockBehaviorModelSelect readonly />)

    cy.get(`input[value="${BehaviorPolicyType.MQTT_EVENT}"]`).should('be.disabled')
    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_DUPLICATE}"]`).should('be.disabled')
    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_QUOTA}"]`).should('be.disabled')
  })

  it('should render with no initial selection', () => {
    cy.mountWithProviders(<MockBehaviorModelSelect formData={{}} />)

    cy.get('[role="radiogroup"]').should('exist')
    cy.get(`input[value="${BehaviorPolicyType.MQTT_EVENT}"]`).should('not.be.checked')
    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_DUPLICATE}"]`).should('not.be.checked')
    cy.get(`input[value="${BehaviorPolicyType.PUBLISH_QUOTA}"]`).should('not.be.checked')
  })

  it('should trigger onBlur and onFocus callbacks', () => {
    const onBlur = cy.stub().as('onBlur')
    const onFocus = cy.stub().as('onFocus')
    cy.mountWithProviders(<MockBehaviorModelSelect onBlur={onBlur} onFocus={onFocus} />)

    // Focus and blur on the first radio input
    cy.get(`input[value="${BehaviorPolicyType.MQTT_EVENT}"]`).focus()
    cy.get('@onFocus').should('have.been.called')

    cy.get(`input[value="${BehaviorPolicyType.MQTT_EVENT}"]`).blur()
    cy.get('@onBlur').should('have.been.called')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<MockBehaviorModelSelect />)
    cy.checkAccessibility()
    cy.checkI18nKeys()
  })
})
