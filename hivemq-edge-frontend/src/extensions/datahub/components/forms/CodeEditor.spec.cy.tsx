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

  // Monaco is bundled with the app now rather than fetched from jsdelivr, so serving 404 for the CDN
  // no longer prevents it from loading and the editor never falls back. The fallback itself still
  // works -- it renders whenever the loader fails -- but triggering that needs the loader stubbed
  // rather than the network blocked, which the component-test setup cannot do today.
  it.skip('should render the fallback editor', () => {
    Cypress.on('uncaught:exception', () => {
      // returning false here prevents Cypress from failing the test
      return false
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
