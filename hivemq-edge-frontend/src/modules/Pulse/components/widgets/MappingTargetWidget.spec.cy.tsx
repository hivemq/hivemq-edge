import type { JSONSchema7 } from 'json-schema'
import type { IChangeEvent } from '@rjsf/core'
import type { UiSchema } from '@rjsf/utils'

import { CustomFormTesting } from '@/__test-utils__/rjsf/CustomFormTesting'
import { MOCK_COMBINER_ASSET } from '@/api/hooks/useCombiners/__handlers__'
import {
  MOCK_PULSE_ASSET,
  MOCK_PULSE_ASSET_MAPPED,
  MOCK_PULSE_ASSET_MAPPED_UNIQUE,
} from '@/api/hooks/usePulse/__handlers__'
import MappingTargetWidget from '@/modules/Pulse/components/widgets/MappingTargetWidget.tsx'
import { customSchemaValidator } from '@/modules/Pulse/utils/validation-utils.ts'

const mockMappingWidgetSchema: JSONSchema7 = {
  $ref: '#/definitions/Asset',
  definitions: {
    Asset: {
      properties: {
        mappingId: {
          type: 'string',
          description: `The id of a DataCombining payload that describes the mapping of that particular asset`,
          format: 'uuid',
        },
      },
    },
  },
}

const mockMappingWidgetUISchema: UiSchema = {
  mappingId: {
    'ui:widget': MappingTargetWidget,
    'ui:readonly': true,
  },
}

const generateMappingWidgetWrapper = (initialState: string | undefined, onSubmit?: (data: IChangeEvent) => void) => (
  <>
    <h2>My form</h2>
    <CustomFormTesting
      schema={mockMappingWidgetSchema}
      uiSchema={mockMappingWidgetUISchema}
      formData={{
        mappingId: initialState,
      }}
      onSubmit={onSubmit}
      // @ts-ignore Need to fix the types
      customValidate={customSchemaValidator(MOCK_COMBINER_ASSET)}
    />
  </>
)

describe('MappingTargetWidget', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly when unset', () => {
    cy.mountWithProviders(generateMappingWidgetWrapper(MOCK_PULSE_ASSET.mapping.mappingId))

    cy.getByTestId('root_mappingId').within(() => {
      cy.get('label[for="root_mappingId"]').should('have.attr', 'data-disabled')
      cy.get('#root_mappingId-helptext').should(
        'have.text',
        'The id of a DataCombining payload that describes the mapping of that particular asset'
      )
      cy.get('input[name="root_mappingId"]').should('have.value', '< unset >')
    })
  })

  it('should render properly when not found', () => {
    cy.mountWithProviders(generateMappingWidgetWrapper(MOCK_PULSE_ASSET_MAPPED_UNIQUE.mapping.mappingId))

    cy.getByTestId('root_mappingId').within(() => {
      cy.get('label[for="root_mappingId"]').should('have.attr', 'data-disabled')
      cy.get('#root_mappingId-helptext').should(
        'have.text',
        'The id of a DataCombining payload that describes the mapping of that particular asset'
      )
      cy.get('input[name="root_mappingId"]').should('have.value', '< not found >')
    })
  })

  it('should render properly', () => {
    cy.intercept('GET', '/api/v1/management/combiners', { items: [MOCK_COMBINER_ASSET] })

    cy.mountWithProviders(generateMappingWidgetWrapper(MOCK_PULSE_ASSET_MAPPED.mapping.mappingId))

    cy.getByTestId('root_mappingId').within(() => {
      cy.get('label[for="root_mappingId"]').should('have.attr', 'data-disabled')
      cy.get('#root_mappingId-helptext').should(
        'have.text',
        'The id of a DataCombining payload that describes the mapping of that particular asset'
      )
      cy.getByTestId('node-name').should('have.text', 'my-combiner-for-asset')
    })
  })
})
