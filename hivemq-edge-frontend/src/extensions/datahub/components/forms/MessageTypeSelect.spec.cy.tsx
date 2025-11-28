import type { WidgetProps } from '@rjsf/utils'
import { MessageTypeSelect } from '@datahub/components/forms/MessageTypeSelect.tsx'
import type { SchemaData } from '@datahub/types.ts'
import { SchemaType } from '@datahub/types.ts'

describe('MessageTypeSelect', () => {
  const getMockProps = (): WidgetProps => ({
    id: 'messageType',
    name: 'messageType', // Added: required by WidgetProps
    value: undefined,
    required: true,
    disabled: false,
    readonly: false,
    onChange: () => {},
    onBlur: () => {},
    onFocus: () => {},
    schema: { title: 'Message Type', type: 'string' },
    formData: {} as SchemaData,
    uiSchema: {},
    rawErrors: [],
    label: 'Message Type',
    options: {},
    placeholder: '',
    autofocus: false,
    registry: {} as WidgetProps['registry'],
  })

  beforeEach(() => {
    cy.viewport(800, 600)
  })

  // ✅ ACTIVE - Basic rendering
  it('should render with empty options when no schemaSource', () => {
    const mockProps = getMockProps()
    const formData: SchemaData = {
      name: 'test',
      type: SchemaType.PROTOBUF,
      version: 1, // Added: required by SchemaData
      schemaSource: '',
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} formData={formData} />)

    // CreatableSelect renders as input with react-select
    cy.get('#messageType').should('exist')
    // Verify the placeholder text is visible somewhere in the component
    cy.contains('No messages found in schema').should('exist')
  })

  // ✅ ACTIVE - Extract multiple messages
  it('should extract and display multiple message types', () => {
    const mockProps = getMockProps()
    const formData: SchemaData = {
      name: 'test',
      type: SchemaType.PROTOBUF,
      version: 1,
      schemaSource: `
        syntax = "proto3";
        message Person {
          string name = 1;
        }
      `,
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} formData={formData} />)

    // Click to open the dropdown
    cy.get('#messageType').click()

    // Verify the option exists in the menu
    cy.get('[role="option"]').should('have.length', 1)
    cy.get('[role="option"]').first().should('contain.text', 'Person')
  })

  // ✅ ACTIVE - Extract multiple messages
  it('should extract and display multiple message types', () => {
    const mockProps = getMockProps()
    const formData: SchemaData = {
      name: 'test',
      type: SchemaType.PROTOBUF,
      version: 1,
      schemaSource: `
        syntax = "proto3";
        message Person {
          string name = 1;
        }
        message Address {
          string street = 1;
        }
      `,
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} formData={formData} />)

    // Click to open the dropdown
    cy.get('#messageType').click()

    // Verify both options exist
    cy.get('[role="option"]').should('have.length', 2)
    cy.get('[role="option"]').eq(0).should('contain.text', 'Person')
    cy.get('[role="option"]').eq(1).should('contain.text', 'Address')
  })

  // ⏭️ SKIPPED - Will activate during Phase 4
  it.skip('should handle onChange when selecting a message type', () => {
    // Test: Mount with multiple messages
    // Test: Select a message type
    // Test: Verify onChange called with selected value
  })

  it.skip('should show placeholder when messages available', () => {
    // Test: Mount with messages
    // Test: Verify placeholder is "Select a message type"
  })

  it.skip('should handle nested messages with dot notation', () => {
    // Test: Mount with nested protobuf message
    // Test: Verify nested message shown as "Outer.Inner"
  })

  it.skip('should disable select when readonly prop is true', () => {
    // Test: Mount with readonly=true
    // Test: Verify select is disabled
  })

  it.skip('should disable select when disabled prop is true', () => {
    // Test: Mount with disabled=true
    // Test: Verify select is disabled
  })

  it.skip('should update options when schemaSource changes', () => {
    // Test: Mount with one message
    // Test: Update formData with different schemaSource
    // Test: Verify options update
  })

  it.skip('should preserve selected value when options update', () => {
    // Test: Mount with messages including "Person"
    // Test: Select "Person"
    // Test: Update schemaSource to still include "Person"
    // Test: Verify "Person" still selected
  })

  it.skip('should handle invalid protobuf gracefully', () => {
    // Test: Mount with invalid protobuf source
    // Test: Verify shows "No messages found in schema"
    // Test: No console errors
  })
})
