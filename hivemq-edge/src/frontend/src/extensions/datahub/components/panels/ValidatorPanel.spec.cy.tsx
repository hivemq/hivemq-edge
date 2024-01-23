/// <reference types="cypress" />

import { MockStoreWrapper } from '../../__test-utils__/MockStoreWrapper.tsx'
import { DataHubNodeType } from '../../types.ts'
import { getNodePayload } from '../../utils/node.utils.ts'
import { ValidatorPanel } from '../panels/ValidatorPanel.tsx'
import { Button } from '@chakra-ui/react'

const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
  <MockStoreWrapper
    config={{
      initialState: {
        nodes: [
          {
            id: '3',
            type: DataHubNodeType.VALIDATOR,
            position: { x: 0, y: 0 },
            data: getNodePayload(DataHubNodeType.VALIDATOR),
          },
        ],
      },
    }}
  >
    {children}
    <Button variant={'primary'} type="submit" form="datahub-node-form">
      SUBMIT{' '}
    </Button>
  </MockStoreWrapper>
)

describe('ValidatorPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the fields for a Validator', () => {
    cy.mountWithProviders(<ValidatorPanel selectedNode={'3'} />, { wrapper })

    // first select
    cy.get('label#root_type-label').should('contain.text', 'Validator Type')
    cy.get('label#root_type-label + div').should('contain.text', 'schema')
    cy.get('label#root_type-label + div').click()
    cy.get('label#root_type-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(0)
      .should('contain.text', 'schema')
    cy.get('label#root_type-label + div').click()

    // second select
    cy.get('label#root_strategy-label').should('contain.text', 'Validation Strategy')
    cy.get('label#root_strategy-label + div').should('contain.text', 'ALL_OF')
    cy.get('label#root_strategy-label + div').click()
    cy.get('label#root_strategy-label + div')
      .find("[role='listbox']")
      .find("[role='option']")
      .eq(0)
      .should('contain.text', 'ANY_OF')
    cy.get('label#root_strategy-label + div').click()

    cy.get('h5').eq(0).should('contain.text', 'schemas')
    // first item
    cy.get('h5').eq(1).should('contain.text', 'schemas-0')
    // first item property
    cy.get('label#root_schemas_0_schemaId-label').should('contain.text', 'ID of the schema')
    cy.get('label#root_schemas_0_schemaId-label + input').should('have.value', 'first mock schema')
    // first item property
    cy.get('label#root_schemas_0_version-label').should('contain.text', 'version of the schema')
    cy.get('label#root_schemas_0_version-label + input').should('have.value', '1')
  })

  it('should render the error message with data not found', () => {
    cy.mountWithProviders(<ValidatorPanel selectedNode={'not valid'} />, { wrapper })

    cy.get("[role='alert']").find('div[data-status]').should('have.length', 2)
    cy.get("[role='alert']").find('div[data-status]').eq(0).should('contain.text', 'Error loading the node')
    cy.get("[role='alert']")
      .find('div[data-status]')
      .eq(1)
      .should('contain.text', 'The Policy Validator is not a valid element')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ValidatorPanel selectedNode={'3'} />, { wrapper })

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[18840] Need to change the heading wrapper in the RJSF template
        'heading-order': { enabled: false },
        region: { enabled: false },
      },
    })
    cy.percySnapshot('Component: ValidatorPanel')
  })
})
