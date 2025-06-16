import type { FC } from 'react'
import type { JSONSchema7 } from 'json-schema'
import type { FormProps } from '@rjsf/core'
import type { UiSchema } from '@rjsf/utils'
import { CustomFormTesting } from '@/__test-utils__/rjsf/CustomFormTesting'

import { MetricCounterInput } from '@datahub/components/forms/MetricCounterInput.tsx'

const mockMetricCounterInputUISchema: UiSchema = {
  counterInput: {
    'ui:widget': 'datahub:metric-counter',
  },
}

const mockMetricCounterInputSchema: JSONSchema7 = {
  properties: {
    counterInput: {
      type: 'string',
      minLength: 5,
    },
  },
}

type FormPropsStubs = Pick<FormProps<unknown>, 'onChange' | 'onBlur' | 'onFocus'>
const MockDataHubFunctions: FC<FormPropsStubs> = (props) => {
  return (
    <CustomFormTesting
      schema={mockMetricCounterInputSchema}
      uiSchema={mockMetricCounterInputUISchema}
      widgets={{
        'datahub:metric-counter': MetricCounterInput,
      }}
      formData={{ counterInput: 'my-counter' }}
      {...props}
    />
  )
}

describe('MetricCounterInput', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the prefixed input', () => {
    const onChange = cy.stub().as('onChange')
    const onBlur = cy.stub().as('onBlur')
    const onFocus = cy.stub().as('onFocus')
    cy.mountWithProviders(<MockDataHubFunctions onChange={onChange} onBlur={onBlur} onFocus={onFocus} />)

    cy.getByTestId('input-prefix').should('have.text', 'com.hivemq.com.data-hub.custom.counters.')
    cy.get("label[for='root_counterInput'] + div > input")
      .as('inputField')
      .should('have.attr', 'placeholder', 'metricName')
      .should('have.value', 'my-counter')

    cy.getByTestId('root_counterInput').should('not.have.attr', 'data-invalid')

    cy.get('@inputField').clear()
    cy.get('@onFocus').should('have.been.called')

    cy.get('@inputField').type('123')
    cy.get('@onChange').then((stub) => {
      const onChangeStub = stub as unknown as Cypress.Agent<sinon.SinonStub>
      const firstCallArgs = onChangeStub.args[0]
      expect(firstCallArgs[0]).to.deep.include({ formData: { counterInput: undefined } })
      expect(firstCallArgs[1]).to.equal('root_counterInput')
    })

    cy.getByTestId('root_counterInput').should('have.attr', 'data-invalid')
    cy.get('@inputField').should('have.attr', 'aria-invalid', 'true')
    cy.get('#root_counterInput__error').should('have.text', 'must NOT have fewer than 5 characters')
    cy.get("button[type='submit']").click()
    cy.get('@onBlur').should('have.been.calledWith', 'root_counterInput', '123')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<MockDataHubFunctions />)
    cy.checkAccessibility()
  })
})
