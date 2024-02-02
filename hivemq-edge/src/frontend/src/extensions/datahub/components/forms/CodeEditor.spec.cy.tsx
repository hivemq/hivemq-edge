/// <reference types="cypress" />

import { WidgetProps } from '@rjsf/utils'
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
})
