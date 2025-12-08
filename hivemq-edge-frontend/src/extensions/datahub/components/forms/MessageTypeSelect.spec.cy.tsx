import type { WidgetProps } from '@rjsf/utils'
import { MessageTypeSelect } from '@datahub/components/forms/MessageTypeSelect.tsx'

describe('MessageTypeSelect', () => {
  // @ts-ignore No need for the whole props for testing
  const getMockProps = (): WidgetProps => ({
    id: 'messageType',
    name: 'messageType',
    label: 'Message Type',
    onChange: () => {},
    onBlur: () => {},
    onFocus: () => {},
    schema: { title: 'Message Type', type: 'string' },
    options: {},
  })

  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render with empty options when no schemaSource', () => {
    const mockProps = {
      ...getMockProps(),
      formContext: { currentSchemaSource: '' },
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} />)

    // CreatableSelect renders as input with react-select
    cy.get('#messageType').should('exist')
    // Click to open dropdown and verify placeholder
    cy.get('#messageType').click()
    cy.contains('No messages found in schema').should('be.visible')
  })

  it('should extract and display message type', () => {
    const mockProps = {
      ...getMockProps(),
      formContext: {
        currentSchemaSource: `
          syntax = "proto3";
          message Person {
            string name = 1;
          }
        `,
      },
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} />)

    // Click to open the dropdown
    cy.get('#messageType').click()

    // Verify the option exists in the menu
    cy.get('[role="option"]').should('have.length', 1)
    cy.get('[role="option"]').first().should('contain.text', 'Person')
  })

  it('should extract and display multiple message types', () => {
    const mockProps = {
      ...getMockProps(),
      formContext: {
        currentSchemaSource: `
          syntax = "proto3";
          message Person {
            string name = 1;
          }
          message Address {
            string street = 1;
          }
        `,
      },
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} />)

    // Click to open the dropdown
    cy.get('#messageType').click()

    // Verify both options exist
    cy.get('[role="option"]').should('have.length', 2)
    cy.get('[role="option"]').eq(0).should('contain.text', 'Person')
    cy.get('[role="option"]').eq(1).should('contain.text', 'Address')
  })

  it('should handle onChange when selecting a message type', () => {
    const onChangeSpy = cy.spy().as('onChangeSpy')
    const mockProps = {
      ...getMockProps(),
      onChange: onChangeSpy,
      formContext: {
        currentSchemaSource: `
          syntax = "proto3";
          message Person {
            string name = 1;
          }
          message Address {
            string street = 1;
          }
        `,
      },
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} />)

    // Open dropdown
    cy.get('#messageType').click()

    // Select "Person"
    cy.get('[role="option"]').first().click()

    // Verify onChange was called with the selected value
    cy.get('@onChangeSpy').should('have.been.calledOnce')
    cy.get('@onChangeSpy').should('have.been.calledWith', 'Person')
  })

  it('should show placeholder when messages available', () => {
    const mockProps = {
      ...getMockProps(),
      formContext: {
        currentSchemaSource: `
          syntax = "proto3";
          message Person {
            string name = 1;
          }
        `,
      },
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} />)

    // Before opening dropdown, should show default react-select placeholder
    cy.get('#messageType').should('exist')

    // Open dropdown to see options available
    cy.get('#messageType').click()
    cy.get('[role="option"]').should('have.length', 1)
    cy.get('[role="option"]').first().should('contain.text', 'Person')
  })

  it('should handle nested messages with dot notation', () => {
    const mockProps = {
      ...getMockProps(),
      formContext: {
        currentSchemaSource: `
          syntax = "proto3";
          message Outer {
            message Inner {
              string value = 1;
            }
            Inner inner = 1;
          }
        `,
      },
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} />)

    // Open dropdown
    cy.get('#messageType').click()

    // Verify nested message is shown with dot notation
    cy.get('[role="option"]').should('have.length', 2)
    cy.get('[role="option"]').eq(0).should('contain.text', 'Outer')
    cy.get('[role="option"]').eq(1).should('contain.text', 'Outer.Inner')
  })

  it('should disable select when readonly prop is true', () => {
    const mockProps = {
      ...getMockProps(),
      readonly: true,
      formContext: {
        currentSchemaSource: `
          syntax = "proto3";
          message Person {
            string name = 1;
          }
        `,
      },
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} />)

    // Verify the select is disabled (CreatableSelect adds disabled state to input)
    cy.get('#messageType').should('be.disabled')
  })

  it('should disable select when disabled prop is true', () => {
    const mockProps = {
      ...getMockProps(),
      disabled: true,
      formContext: {
        currentSchemaSource: `
          syntax = "proto3";
          message Person {
            string name = 1;
          }
        `,
      },
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} />)

    // Verify the select is disabled
    cy.get('#messageType').should('be.disabled')
  })

  it('should update options when schemaSource changes', () => {
    // First mount with initial schema
    const initialProps = {
      ...getMockProps(),
      formContext: {
        currentSchemaSource: `
          syntax = "proto3";
          message Person {
            string name = 1;
          }
        `,
      },
    }

    cy.mountWithProviders(<MessageTypeSelect {...initialProps} />)

    // Verify initial message
    cy.get('#messageType').click()
    cy.get('[role="option"]').should('have.length', 1)
    cy.get('[role="option"]').first().should('contain.text', 'Person')
    cy.get('body').click() // Close dropdown

    // Remount with updated schema source
    const updatedProps = {
      ...getMockProps(),
      formContext: {
        currentSchemaSource: `
          syntax = "proto3";
          message Address {
            string street = 1;
          }
          message Company {
            string name = 1;
          }
        `,
      },
    }

    cy.mountWithProviders(<MessageTypeSelect {...updatedProps} />)

    // Verify options updated to new messages
    cy.get('#messageType').click()
    cy.get('[role="option"]').should('have.length', 2)
    cy.get('[role="option"]').eq(0).should('contain.text', 'Address')
    cy.get('[role="option"]').eq(1).should('contain.text', 'Company')
  })

  it('should preserve selected value when options update', () => {
    const onChangeSpy = cy.spy().as('onChangeSpy')
    const mockProps = {
      ...getMockProps(),
      value: 'Person', // Pre-selected value
      onChange: onChangeSpy,
      formContext: {
        currentSchemaSource: `
          syntax = "proto3";
          message Person {
            string name = 1;
          }
          message Address {
            string street = 1;
          }
        `,
      },
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} />)

    // Open dropdown to verify "Person" is in the list
    cy.get('#messageType').click()

    // Both options should be available
    cy.get('[role="option"]').should('have.length', 2)
    cy.get('[role="option"]').eq(0).should('contain.text', 'Person')
    cy.get('[role="option"]').eq(1).should('contain.text', 'Address')

    // Select Address to verify onChange still works with pre-selected value
    cy.get('[role="option"]').eq(1).click()
    cy.get('@onChangeSpy').should('have.been.calledOnce')
    cy.get('@onChangeSpy').should('have.been.calledWith', 'Address')
  })

  it('should handle invalid protobuf gracefully', () => {
    const mockProps = {
      ...getMockProps(),
      formContext: {
        currentSchemaSource: 'this is not valid protobuf syntax at all!',
      },
    }

    cy.mountWithProviders(<MessageTypeSelect {...mockProps} />)

    // Open dropdown
    cy.get('#messageType').click()

    // Should show "No messages found" since extraction failed
    cy.contains('No messages found in schema').should('be.visible')

    // Verify no options are shown
    cy.get('[role="option"]').should('not.exist')
  })
})
