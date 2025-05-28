/// <reference types="cypress" />

import type { WidgetProps } from '@rjsf/utils'
import {
  MOCK_JAVASCRIPT_SCHEMA,
  MOCK_JSONSCHEMA_SCHEMA,
  MOCK_PROTOBUF_SCHEMA,
} from '@/extensions/datahub/__test-utils__/schema.mocks.ts'
import { JavascriptEditor, JSONSchemaEditor, ProtoSchemaEditor } from '@datahub/components/forms/CodeEditor.tsx'

// @ts-ignore No need for the whole props for testing
const MOCK_WIDGET_PROPS: WidgetProps = {
  id: 'code-widget',
  label: 'Source Code',
  name: 'code',
  onBlur: () => undefined,
  onChange: () => undefined,
  onFocus: () => undefined,
  schema: {},
  options: {},
}

describe('CodeEditor', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render the Javascript Editor', () => {
    cy.mountWithProviders(<JavascriptEditor {...MOCK_WIDGET_PROPS} value={MOCK_JAVASCRIPT_SCHEMA} />)
  })

  it('should render the Protobuf Editor', () => {
    cy.mountWithProviders(<ProtoSchemaEditor {...MOCK_WIDGET_PROPS} value={MOCK_PROTOBUF_SCHEMA} />)
  })

  it('should render the JSONSchema Editor', () => {
    cy.mountWithProviders(<JSONSchemaEditor {...MOCK_WIDGET_PROPS} value={MOCK_JSONSCHEMA_SCHEMA} />)
  })

  it.only('should render the fallback editor', () => {
    Cypress.on('uncaught:exception', (err) => {
      // we expect Monaco's Uncaught NetworkError and don't want to fail the test so we return false
      if (err.message.includes("Failed to execute 'importScripts' on 'WorkerGlobalScope'")) {
        return false
      }
      // we still want to ensure there are no other unexpected errors, so we let them fail the test
    })

    cy.intercept('https://cdn.jsdelivr.net/**', { statusCode: 404 }).as('getMonaco')
    cy.mountWithProviders(<JSONSchemaEditor {...MOCK_WIDGET_PROPS} value={MOCK_JSONSCHEMA_SCHEMA} />)

    cy.get('textarea').should('be.visible')
    cy.get("[role='group'] + p").should(
      'contain.text',
      'The advanced editor cannot be loaded. Syntax highlighting is not supported'
    )
  })
})
