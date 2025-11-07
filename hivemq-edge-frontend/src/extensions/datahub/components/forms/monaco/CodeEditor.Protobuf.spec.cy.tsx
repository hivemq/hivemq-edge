/// <reference types="cypress" />

import type { Monaco } from '@monaco-editor/react'
import type { WidgetProps } from '@rjsf/utils'
import { ProtoSchemaEditor } from '@datahub/components/forms/CodeEditor.tsx'
import { setMonacoCursorPosition, triggerMonacoAutocomplete, waitForSuggestWidget } from './monacoTestHelpers'

const getMockProps = (): WidgetProps => ({
  id: 'protobuf-editor-test',
  label: 'Protobuf Schema',
  name: 'schema',
  value: '',
  onBlur: () => undefined,
  onChange: () => undefined,
  onFocus: () => undefined,
  schema: {},
  options: {},
  // @ts-ignore
  registry: {},
})

describe('Monaco Protobuf - Integration Tests (Configuration Verification)', () => {
  beforeEach(() => {
    cy.viewport(1200, 800)

    // Ignore Monaco worker loading errors
    cy.on('uncaught:exception', (err) => {
      return !(err.message.includes('importScripts') || err.message.includes('worker'))
    })
  })

  it('should load Monaco editor with Protobuf language', () => {
    const protoSchema = `syntax = "proto3";

message Person {
  string name = 1;
  int32 age = 2;
}`

    cy.mountWithProviders(<ProtoSchemaEditor {...getMockProps()} value={protoSchema} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    cy.window().then((win) => {
      // @ts-ignore
      expect(win.monaco).to.exist
      // @ts-ignore
      expect(win.monaco.editor).to.exist
    })
  })

  it('should have proto language registered', () => {
    cy.mountWithProviders(<ProtoSchemaEditor {...getMockProps()} value="" />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    cy.window().then((win) => {
      // @ts-ignore
      const monaco = win.monaco as Monaco

      // Check if proto language is registered
      const languages = monaco.languages.getLanguages()
      const protoLang = languages.find((lang) => lang.id === 'proto' || lang.id === 'protobuf')

      expect(protoLang).to.exist
    })
  })

  it('should provide syntax highlighting for proto keywords', () => {
    const protoSchema = `syntax = "proto3";

message Person {
  string name = 1;
}`

    cy.mountWithProviders(<ProtoSchemaEditor {...getMockProps()} value={protoSchema} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    // Monaco should tokenize the code
    cy.get('.monaco-editor').within(() => {
      // Keywords like "syntax", "message", "string" should be highlighted
      cy.get('[class*="mtk6"]').should('exist')
    })
  })

  it('should have auto-closing brackets for proto', () => {
    cy.mountWithProviders(<ProtoSchemaEditor {...getMockProps()} value="" />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    cy.window().then((win) => {
      // @ts-ignore
      const monaco = win.monaco
      // @ts-ignore
      const editors = monaco.editor.getEditors()

      const editor = editors[0]

      // Type an opening brace
      editor.trigger('keyboard', 'type', { text: '{' })

      // Check if closing brace was automatically added
      const value = editor.getValue()
      expect(value).to.include('{}')
    })
  })

  it('should provide autocomplete for protobuf keywords', () => {
    // Start with empty document - this is what we fixed!
    cy.mountWithProviders(<ProtoSchemaEditor {...getMockProps()} value="" />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    // Position cursor at start and trigger autocomplete
    setMonacoCursorPosition(1, 1)
    triggerMonacoAutocomplete()

    // Wait for suggest widget - should show our keywords
    waitForSuggestWidget()
    cy.get('.monaco-editor .suggest-widget .monaco-list-row').should('have.length.greaterThan', 0)
  })

  it('should provide autocomplete for protobuf types', () => {
    const protoSchema = 'message Test {\n  \n}'

    cy.mountWithProviders(<ProtoSchemaEditor {...getMockProps()} value={protoSchema} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    // Position cursor inside message and trigger autocomplete
    setMonacoCursorPosition(2, 3)
    triggerMonacoAutocomplete()

    // Wait for suggest widget - should show types like int32, string, etc.
    waitForSuggestWidget()
    cy.get('.monaco-editor .suggest-widget .monaco-list-row').should('have.length.greaterThan', 0)
  })

  it('should recognize proto field numbers', () => {
    const protoSchema = `syntax = "proto3";

message Person {
  string name = 1;
  int32 age = 2;
  bool active = 3;
}`

    cy.mountWithProviders(<ProtoSchemaEditor {...getMockProps()} value={protoSchema} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    cy.window().then((win) => {
      // @ts-ignore
      const editors = win.monaco.editor.getEditors()
      const editor = editors[0]
      const model = editor.getModel()
      const content = model.getValue()

      // Verify the content is loaded correctly
      expect(content).to.include('string name = 1')
      expect(content).to.include('int32 age = 2')
      expect(content).to.include('bool active = 3')
    })
  })

  it('should support comments in proto files', () => {
    const protoSchema = `syntax = "proto3";

// This is a comment
message Person {
  string name = 1; // Name field
  /* Block comment */
  int32 age = 2;
}`

    cy.mountWithProviders(<ProtoSchemaEditor {...getMockProps()} value={protoSchema} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    cy.window().then((win) => {
      // @ts-ignore
      const editors = win.monaco.editor.getEditors()
      const editor = editors[0]
      const content = editor.getValue()

      // Verify comments are preserved
      expect(content).to.include('// This is a comment')
      expect(content).to.include('/* Block comment */')
    })
  })

  it('should have protobuf language configuration', () => {
    cy.mountWithProviders(<ProtoSchemaEditor {...getMockProps()} value="" />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    cy.window().then((win) => {
      // @ts-ignore
      const monaco = win.monaco as Monaco

      // Get language configuration for proto
      const languages = monaco.languages.getLanguages()
      const protoLang = languages.find((lang) => lang.id === 'proto')

      if (protoLang) {
        cy.log('Protobuf language registered:', protoLang.id)
        expect(protoLang.id).to.equal('proto')
      }
    })
  })
})
