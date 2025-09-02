import type { JSONSchema7 } from 'json-schema'
import type { IChangeEvent } from '@rjsf/core'
import type { UiSchema } from '@rjsf/utils'
import { customSchemaValidator } from '@/modules/Pulse/utils/validation-utils.ts'

import { CustomFormTesting } from '@/__test-utils__/rjsf/CustomFormTesting'
import {
  MOCK_TOPIC_FILTER_SCHEMA_INVALID,
  MOCK_TOPIC_FILTER_SCHEMA_VALID,
} from '@/api/hooks/useTopicFilters/__handlers__'
import { MOCK_COMBINER_ASSET } from '@/api/hooks/useCombiners/__handlers__'
import SchemaWidget from '@/components/rjsf/Widgets/SchemaWidget.tsx'

const mockSchemaWidgetSchema: JSONSchema7 = {
  $ref: '#/definitions/Asset',
  definitions: {
    Asset: {
      properties: {
        schema: {
          type: 'string',
          description: `The schema associated with the asset, in a JSON Schema and data uri format.`,
          readOnly: true,
          format: 'data-url',
        },
      },
    },
  },
}

const mockSchemaWidgetUISchema: UiSchema = {
  schema: {
    'ui:widget': SchemaWidget,
  },
}

const generateSchemaWidgetComponent = (initialState: string | undefined, onSubmit?: (data: IChangeEvent) => void) => (
  <>
    <h2>My form</h2>
    <CustomFormTesting
      schema={mockSchemaWidgetSchema}
      uiSchema={mockSchemaWidgetUISchema}
      formData={{
        schema: initialState,
        mapping: {},
      }}
      onSubmit={onSubmit}
      // @ts-ignore Need to fix the types
      customValidate={customSchemaValidator(MOCK_COMBINER_ASSET)}
    />
  </>
)

describe('SchemaWidget', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    cy.mountWithProviders(generateSchemaWidgetComponent(MOCK_TOPIC_FILTER_SCHEMA_VALID))

    cy.getByTestId('root_schema').within(() => {
      cy.get('label[for="root_schema"]').should('have.attr', 'data-disabled')
      cy.get('#root_schema-helptext').should(
        'have.text',
        'The schema associated with the asset, in a JSON Schema and data uri format.'
      )
      cy.get('h3').should('have.text', 'This is a simple schema')
      cy.get('[role="list"] li').should('have.length', 2)
      cy.get('[role="list"] li')
        .eq(0)
        .within(() => {
          cy.getByTestId('property-type').should('have.attr', 'aria-label', 'String')
          cy.getByTestId('property-name')
            .should('have.attr', 'aria-label', 'description')
            .should('have.text', 'description')
        })

      cy.get('[role="list"] li')
        .eq(1)
        .within(() => {
          cy.getByTestId('property-type').should('have.attr', 'aria-label', 'String')
          cy.getByTestId('property-name').should('have.attr', 'aria-label', 'name').should('have.text', 'name')
        })
    })
  })

  it('should render errors', () => {
    cy.mountWithProviders(generateSchemaWidgetComponent(MOCK_TOPIC_FILTER_SCHEMA_INVALID))

    cy.getByTestId('root_schema').within(() => {
      cy.get('label[for="root_schema"]').should('have.attr', 'data-disabled')
      cy.get('label[for="root_schema"]').should('have.attr', 'data-invalid')

      cy.get('label[for="root_schema"] + div').should('have.text', '< unset >')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(generateSchemaWidgetComponent(MOCK_TOPIC_FILTER_SCHEMA_VALID))

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[#111] Disabled title are not accessible
        'color-contrast': { enabled: false },
      },
    })
  })
})
