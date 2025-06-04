/// <reference types="cypress" />

import type { RJSFSchema } from '@rjsf/utils'
import { MockAdapterType } from '@/__test-utils__/adapters/types'
import { MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA, MOCK_DEVICE_TAGS } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { tagListUISchema } from '@/api/schemas/domain-tags.ui-schema'

import TagEditorDrawer from './TagEditorDrawer'

const mockTag = MOCK_DEVICE_TAGS('test-id', MockAdapterType.OPC_UA)[0]

describe('TagEditorDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    const onSubmit = cy.stub().as('onSubmit')
    const onClose = cy.stub().as('onClose')

    cy.mountWithProviders(
      <TagEditorDrawer
        // TODO[E2E] Should be done with real adapter E2E testing
        schema={MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA.configSchema as RJSFSchema}
        uiSchema={tagListUISchema.items.items}
        onSubmit={onSubmit}
        onClose={onClose}
        formData={mockTag}
      />
    )

    cy.get('[role="dialog"][aria-label="Edit the tag"]').within(() => {
      cy.get('header').should('have.text', 'Edit the tag')

      cy.get('form#tag--form').within(() => {
        cy.get('label').eq(0).should('contain.text', 'name')
        cy.get('label').eq(1).should('contain.text', 'description')
        cy.get('label').eq(2).should('contain.text', 'Destination Node ID')
      })

      cy.get('footer').within(() => {
        cy.get('button').eq(0).should('have.text', 'Cancel')
        cy.get('button').eq(1).should('have.text', 'Save')

        cy.get('@onSubmit').should('have.not.been.called')
        cy.get('@onClose').should('have.not.been.called')
        cy.get('button').eq(0).click()
        cy.get('@onClose').should('have.been.called')

        cy.get('button').eq(1).click()
        cy.get('@onSubmit').should('have.been.called')
      })
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()

    cy.mountWithProviders(
      <TagEditorDrawer
        schema={MOCK_DEVICE_TAG_JSON_SCHEMA_OPCUA.configSchema as RJSFSchema}
        uiSchema={tagListUISchema.items.items}
        onSubmit={cy.stub}
        onClose={cy.stub}
        formData={mockTag}
      />
    )

    cy.checkAccessibility()
  })
})
