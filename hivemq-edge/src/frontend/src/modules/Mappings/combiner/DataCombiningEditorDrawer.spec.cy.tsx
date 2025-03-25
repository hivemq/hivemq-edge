/// <reference types="cypress" />

import type { RJSFSchema, UiSchema } from '@rjsf/utils'
import DataCombiningEditorDrawer from './DataCombiningEditorDrawer'
import { mockCombinerMapping } from '../../../../api/hooks/useCombiners/__handlers__'

const mockCombinerMappingSchema: RJSFSchema = {
  title: 'The title',
  description: 'The long description under the title',
  properties: {
    test: { type: 'string' },
  },
  required: ['test'],
}

const mockUiSchema: UiSchema = {
  'ui:submitButtonOptions': {
    norender: true,
  },
}

describe('DataCombiningEditorDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    const onClose = cy.stub().as('onClose')
    const onSubmit = cy.stub().as('onSubmit')
    cy.mountWithProviders(
      <DataCombiningEditorDrawer
        onClose={onClose}
        onSubmit={onSubmit}
        schema={mockCombinerMappingSchema}
        uiSchema={mockUiSchema}
        formData={mockCombinerMapping}
      />
    )

    cy.get('header').should('have.text', 'Data combining mapping')
    cy.get('@onClose').should('not.have.been.called')
    cy.get('@onSubmit').should('not.have.been.called')

    cy.getByAriaLabel('Close').click()
    cy.get('@onClose').should('have.been.called')

    cy.get('form').within(() => {
      cy.get('button[type="submit"]').should('not.exist')

      cy.get('#root__title').should('have.text', 'The title')

      cy.get('#root_test-label').should('have.attr', 'data-invalid')

      cy.get('#root_test').click()
      cy.get('#root_test').type('123')
      cy.get('#root_test-label').should('not.have.attr', 'data-invalid')
    })

    cy.get('footer').within(() => {
      cy.get('button').eq(0).should('have.text', 'Cancel')
      cy.get('button').eq(0).click()
      cy.get('@onClose').should('have.been.calledTwice')

      cy.get('button').eq(1).should('have.text', 'Save').should('have.attr', 'type', 'submit')
      cy.get('button').eq(1).click()
      cy.get('@onSubmit').should('have.been.called')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <DataCombiningEditorDrawer
        onClose={cy.stub}
        onSubmit={cy.stub}
        schema={mockCombinerMappingSchema}
        uiSchema={mockUiSchema}
        formData={mockCombinerMapping}
      />
    )
    cy.checkAccessibility()
  })
})
