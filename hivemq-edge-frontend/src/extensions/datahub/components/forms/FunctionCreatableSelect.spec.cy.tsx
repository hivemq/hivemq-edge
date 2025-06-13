import type { JSONSchema7 } from 'json-schema'
import type { UiSchema } from '@rjsf/utils'
import { CustomFormTesting } from '@/__test-utils__/rjsf/CustomFormTesting'

import { MOCK_DATAHUB_FUNCTIONS } from '@datahub/api/hooks/DataHubFunctionsService/__handlers__'
import FunctionCreatableSelect from '@datahub/components/forms/FunctionCreatableSelect.tsx'

const mockFunctionCreatableSelectUISchema: UiSchema = {
  functionId: {
    'ui:widget': 'datahub:function-selector',
  },
}

const mockFunctionCreatableSelectSchema: JSONSchema7 = {
  $ref: '#/definitions/functionId',
  definitions: {
    functionId: {
      properties: {
        functionId: {
          type: 'string',
        },
      },
    },
  },
}

describe('FunctionCreatableSelect', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the base table', () => {
    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockFunctionCreatableSelectSchema}
        uiSchema={mockFunctionCreatableSelectUISchema}
        formContext={{ functions: MOCK_DATAHUB_FUNCTIONS.items }}
        formData={{
          items: [],
        }}
        widgets={{
          'datahub:function-selector': FunctionCreatableSelect,
        }}
      />
    )

    cy.get("label[for='root_functionId']").should('contain.text', 'functionId')
    cy.get("label[for='root_functionId'] + div").click()

    cy.get('div#react-select-functions-listbox').find('[role="option"]').as('optionList')
    cy.get('@optionList').should('have.length', 8)
    cy.get('#react-select-functions-option-0')
      .should('contain.text', 'Mqtt.UserProperties.add')
      .should('contain.text', 'Adds a user property to the MQTT message.')

    cy.get('input#root_functionId').type('123')
    cy.get('div#react-select-functions-listbox').should('have.text', 'No options')

    cy.get('input#root_functionId').clear()
    cy.get('input#root_functionId').type('serial')
    cy.get('@optionList').should('have.length', 2)
    cy.get('@optionList').each((item, index) => {
      // Replace with your expected values
      const expectedTexts = ['Serdes.serialize', 'Serdes.deserialize']
      cy.wrap(item).should('contain.text', expectedTexts[index])
    })

    cy.get('input#root_functionId').clear()
    cy.get('input#root_functionId').type('PUBLISH')
    cy.get('@optionList').should('have.length', 1)
    cy.get('@optionList').each((item, index) => {
      // Replace with your expected values
      const expectedTexts = ['Delivery.redirectTo']
      cy.wrap(item).should('contain.text', expectedTexts[index])
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <CustomFormTesting
        schema={mockFunctionCreatableSelectSchema}
        uiSchema={mockFunctionCreatableSelectUISchema}
        formContext={{ functions: MOCK_DATAHUB_FUNCTIONS.items }}
        formData={{
          items: [],
        }}
        widgets={{
          'datahub:function-selector': FunctionCreatableSelect,
        }}
      />
    )
    cy.get("label[for='root_functionId'] + div").click()
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
        'scrollable-region-focusable': { enabled: false },
      },
    })
  })
})
