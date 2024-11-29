import { FieldMappingsModel } from '@/api/__generated__'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import MappingInstruction from './MappingInstruction.tsx'

const MOCK_SUBS: FieldMappingsModel = {
  tag: 'my-node',
  topicFilter: 'my-topic',
  fieldMapping: [
    {
      source: 'this is a mapping',
      destination: 'my-node',
    },
  ],
}

const MOCK_PROPERTY_OBJECT: FlatJSONSchema7 = {
  description: undefined,
  path: [],
  key: 'billing-address',
  title: 'Billing address',
  type: 'object',
}

const MOCK_PROPERTY_STRING: FlatJSONSchema7 = {
  description: undefined,
  path: [],
  key: 'billing-address',
  title: 'Billing address',
  type: 'string',
}

describe('MappingInstruction', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render number properly', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        mapping={MOCK_SUBS.fieldMapping?.[1]}
      />
    )

    cy.getByAriaLabel('Clear mapping').should('be.disabled')
    cy.getByTestId('property-name').should('have.text', 'Billing address')
    cy.getByAriaLabel('Property').should('have.attr', 'data-type', 'string').should('not.have.attr', 'draggable')
    cy.getByTestId('mapping-instruction-dropzone').should('have.text', 'Drag a source property here')
    cy.get('[role="alert"]').should('contain.text', 'Required').should('have.attr', 'data-status', 'error')
  })

  it('should render mapping properly', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        mapping={MOCK_SUBS.fieldMapping?.[0]}
      />
    )

    cy.getByAriaLabel('Clear mapping').should('not.be.disabled')
    cy.getByTestId('property-name').should('have.text', 'Billing address')
    cy.getByAriaLabel('Property').should('have.attr', 'data-type', 'string').should('not.have.attr', 'draggable')
    cy.getByTestId('mapping-instruction-dropzone').should('have.text', 'this is a mapping')
    cy.get('[role="alert"]').should('contain.text', 'Matching').should('have.attr', 'data-status', 'success')
  })

  it('should render object properly', () => {
    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_OBJECT}
        showTransformation={false}
        onChange={cy.stub()}
        mapping={MOCK_SUBS.fieldMapping?.[1]}
      />
    )

    cy.getByTestId('property-name').should('have.text', 'Billing address')
    cy.getByAriaLabel('Property').should('have.attr', 'data-type', 'object').should('not.have.attr', 'draggable')
    cy.getByTestId('mapping-instruction-dropzone').should('not.exist')
    cy.getByAriaLabel('Clear mapping').should('not.exist')
    cy.get('[role="alert"]').should('contain.text', 'Not supported').should('have.attr', 'data-status', 'warning')
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <MappingInstruction
        property={MOCK_PROPERTY_STRING}
        showTransformation={false}
        onChange={cy.stub()}
        mapping={MOCK_SUBS.fieldMapping?.[0]}
      />
    )

    cy.checkAccessibility()
    // TODO[NVL] Test drag and drop
  })
})
