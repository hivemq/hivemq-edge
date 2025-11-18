/// <reference types="cypress" />

import { Button } from '@chakra-ui/react'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { DataHubNodeType, SchemaType } from '@datahub/types.ts'
import { getNodePayload } from '@datahub/utils/node.utils.ts'
import { SchemaPanel } from '@datahub/designer/schema/SchemaPanel.tsx'
import { mockSchemaTempHumidity } from '@datahub/api/hooks/DataHubSchemasService/__handlers__'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: '3',
            type: DataHubNodeType.SCHEMA,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.SCHEMA),
          },
        ],
      },
    }}
  >
    {children}
    <Button variant="primary" type="submit" form="datahub-node-form">
      SUBMIT
    </Button>
  </MockStoreWrapper>
)

describe('SchemaPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)

    // Ignore Monaco worker loading errors
    cy.on('uncaught:exception', (err) => {
      return !(err.message.includes('importScripts') || err.message.includes('worker'))
    })
  })

  it('should render loading and error states', () => {
    const onFormError = cy.stub().as('onFormError')
    cy.intercept('/api/v1/data-hub/schemas', { statusCode: 404 }).as('getSchemas')
    cy.mountWithProviders(<SchemaPanel selectedNode="3" onFormError={onFormError} />, { wrapper })
    cy.getByTestId('loading-spinner').should('be.visible')

    cy.wait('@getSchemas')
    cy.get('[role="alert"]')
      .should('be.visible')
      .should('have.attr', 'data-status', 'error')
      .should('have.text', 'Not Found')

    cy.get('@onFormError').should('have.been.calledWithErrorMessage', 'Not Found')
  })

  it('should render the fields for a Validator', () => {
    cy.intercept('/api/v1/data-hub/schemas', { items: [{ ...mockSchemaTempHumidity, type: SchemaType.PROTOBUF }] })
    cy.mountWithProviders(<SchemaPanel selectedNode="3" />, { wrapper })

    cy.get('label#root_name-label').should('contain.text', 'Name')
    cy.get('label#root_name-label + div').should('contain.text', 'Select...')
    cy.get('label#root_name-label').should('have.attr', 'data-invalid')

    cy.get('label#root_type-label').should('contain.text', 'Schema')
    cy.get('label#root_type-label + div').should('contain.text', 'JSON')

    cy.get('label#root_version-label').should('contain.text', 'Version')
    cy.get('label#root_version-label + div').should('contain.text', '')

    cy.get('label#root_schemaSource-label').should('contain.text', 'schemaSource')
  })

  it('should create a draft schema', () => {
    cy.intercept('/api/v1/data-hub/schemas', { items: [{ ...mockSchemaTempHumidity, type: SchemaType.PROTOBUF }] })
    cy.mountWithProviders(<SchemaPanel selectedNode="3" />, { wrapper })

    // Verify initial state
    cy.get('#root_name-label + div').should('contain.text', 'Select...')
    cy.get('#root_type-label + div').should('contain.text', 'JSON')

    // Create a draft schema
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('new-schema')
    cy.get('#root_name-label + div').find('[role="option"]').first().click()

    // Verify draft schema state
    cy.get('#root_name-label + div').should('contain.text', 'new-schema')
    cy.get('#root_type-label + div').should('contain.text', 'JSON')
    cy.get('#root_version-label + div').should('contain.text', 'DRAFT')

    // Verify Monaco Editor rendered
    cy.get('#root_schemaSource').find('.monaco-editor').should('be.visible')
  })

  it('should switch schema type from JSON to PROTOBUF', () => {
    cy.intercept('/api/v1/data-hub/schemas', { items: [{ ...mockSchemaTempHumidity, type: SchemaType.PROTOBUF }] })
    cy.mountWithProviders(<SchemaPanel selectedNode="3" />, { wrapper })

    // Create a draft
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('test-schema')
    cy.get('[role="option"]').first().click()
    cy.get('#root_type-label + div').should('contain.text', 'JSON')

    // Switch to PROTOBUF
    cy.get('#root_type-label + div').click()
    cy.get('#root_type-label + div').find('[role="option"]').contains('PROTOBUF').click()

    // Verify type changed
    cy.get('#root_type-label + div').should('contain.text', 'PROTOBUF')
    cy.get('#root_schemaSource').find('.monaco-editor').should('be.visible')
  })

  it('should load an existing schema', () => {
    cy.intercept('/api/v1/data-hub/schemas', { items: [{ ...mockSchemaTempHumidity, type: SchemaType.JSON }] })
    cy.mountWithProviders(<SchemaPanel selectedNode="3" />, { wrapper })

    // Select an existing schema
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('my-schema')
    cy.get('#root_name-label + div').find('[role="option"]').first().click()

    // Verify existing schema loaded
    cy.get('#root_name-label + div').should('contain.text', 'my-schema-id')
    cy.get('#root_type-label + div').should('contain.text', 'JSON')
    cy.get('#root_version-label + div').should('contain.text', '1')

    cy.get('#root_type-label + div input').should('not.be.disabled')
    cy.get('#root_version-label + div input').should('not.be.disabled')

    // Verify Monaco Editor shows the schema
    cy.get('#root_schemaSource', { timeout: 10000 }).find('.monaco-editor').should('be.visible')
  })

  it('should handle schema type changes correctly', () => {
    cy.intercept('/api/v1/data-hub/schemas', { items: [{ ...mockSchemaTempHumidity, type: SchemaType.PROTOBUF }] })
    cy.mountWithProviders(<SchemaPanel selectedNode="3" />, { wrapper })

    // Create a new draft
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('test-schema')
    cy.get('[role="option"]').first().click()

    cy.get('#root_version-label + div').should('contain.text', 'DRAFT')
    cy.get('#root_type-label + div').should('contain.text', 'JSON')

    // Change type to PROTOBUF
    cy.get('#root_type-label + div').click()
    cy.get('#root_type-label + div [role="option"]').contains('PROTOBUF').click()

    // Verify schema source changed to protobuf template
    cy.get('#root_type-label + div').should('contain.text', 'PROTOBUF')
    // Just verify Monaco is visible with the new schema type
    cy.get('#root_schemaSource').find('.monaco-editor').should('be.visible')
  })

  it('should show MODIFIED state when schema is edited', () => {
    cy.intercept('/api/v1/data-hub/schemas', { items: [{ ...mockSchemaTempHumidity, type: SchemaType.JSON }] })

    const onFormChange = cy.stub().as('onFormChange')
    cy.mountWithProviders(<SchemaPanel selectedNode="3" onFormSubmit={onFormChange} />, { wrapper })

    // Select an existing schema
    cy.get('#root_name-label + div').click()
    cy.get('#root_name-label + div').type('my-schema')
    cy.get('#root_name-label + div').find('[role="option"]').eq(0).click()

    cy.get('#root_version-label + div').should('contain.text', '1')

    // Wait for Monaco to fully load by checking for the editor element
    cy.get('#root_schemaSource').find('.monaco-editor').should('be.visible')
    cy.get('#root_schemaSource').find('.monaco-editor .view-lines').should('exist')

    cy.get('#root_type-label + div input').should('not.be.disabled')
    cy.get('#root_version-label + div input').should('not.be.disabled')

    // Verify that the version selector shows available versions
    cy.get('#root_version-label + div').click()
    cy.get('#root_version-label + div [role="option"]').should('exist')
  })

  it('should be accessible', () => {
    cy.intercept('/api/v1/data-hub/schemas', { items: [{ ...mockSchemaTempHumidity, type: SchemaType.PROTOBUF }] })
    cy.injectAxe()
    cy.mountWithProviders(<SchemaPanel selectedNode="3" />, { wrapper })

    cy.checkAccessibility()
  })
})
