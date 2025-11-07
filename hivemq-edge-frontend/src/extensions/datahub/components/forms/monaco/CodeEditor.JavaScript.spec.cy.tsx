/// <reference types="cypress" />

import type { WidgetProps } from '@rjsf/utils'
import { JavascriptEditor } from '@datahub/components/forms/CodeEditor.tsx'
import {
  setMonacoCursorPosition,
  triggerMonacoAutocomplete,
  waitForSuggestWidget,
} from './__test-utils__/monacoTestHelpers.ts'

const getMockProps = (): WidgetProps => ({
  id: 'javascript-editor-test',
  label: 'JavaScript Code',
  name: 'code',
  value: '',
  onBlur: () => undefined,
  onChange: () => undefined,
  onFocus: () => undefined,
  schema: {},
  options: {},
  // @ts-ignore
  registry: {},
})

describe('Monaco JavaScript - Integration Tests (Configuration Verification)', () => {
  beforeEach(() => {
    cy.viewport(1200, 800)

    // Ignore Monaco worker loading errors
    cy.on('uncaught:exception', (err) => {
      return !(err.message.includes('importScripts') || err.message.includes('worker'))
    })
  })

  it('should load Monaco editor with JavaScript language', () => {
    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value="console.log('test')" />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    cy.window().then((win) => {
      // @ts-ignore
      expect(win.monaco).to.exist
      // @ts-ignore
      expect(win.monaco.editor).to.exist
    })
  })

  it('should provide console IntelliSense', () => {
    const code = 'console.'

    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value={code} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    // Position cursor after "console." and trigger autocomplete
    setMonacoCursorPosition(1, 9)
    triggerMonacoAutocomplete()

    // Wait for suggest widget - check it appears with suggestions
    waitForSuggestWidget()
    cy.get('.monaco-editor .suggest-widget .monaco-list-row').should('have.length.greaterThan', 0)
  })

  it('should provide JSON IntelliSense', () => {
    const code = 'JSON.'

    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value={code} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    // Position cursor after "JSON." and trigger autocomplete
    setMonacoCursorPosition(1, 6)
    triggerMonacoAutocomplete()

    // Wait for suggest widget - check it appears with suggestions
    waitForSuggestWidget()
    cy.get('.monaco-editor .suggest-widget .monaco-list-row').should('have.length.greaterThan', 0)
  })

  it('should provide Math IntelliSense', () => {
    const code = 'Math.'

    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value={code} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    // Position cursor after "Math." and trigger autocomplete
    setMonacoCursorPosition(1, 6)
    triggerMonacoAutocomplete()

    // Wait for suggest widget - Math methods should be suggested
    waitForSuggestWidget()
    cy.get('.monaco-editor .suggest-widget').should('exist')
  })

  it('should detect syntax errors', () => {
    const code = 'function test() {\n  returm true;\n}'

    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value={code} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    // Should show error decoration for "returm" typo
    cy.get('.monaco-editor').within(() => {
      // Monaco shows errors with specific classes
      cy.get('.squiggly-error, .redsquiggly, [class*="squiggly"]').should('exist')
    })
  })

  it('should have auto-closing brackets enabled', () => {
    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value="" />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    cy.window().then((win) => {
      // @ts-ignore
      const monaco = win.monaco
      // @ts-ignore
      const editors = monaco.editor.getEditors()

      const editor = editors[0]

      // Type an opening bracket
      editor.trigger('keyboard', 'type', { text: '{' })

      // Check if closing bracket was automatically added
      const value = editor.getValue()
      expect(value).to.include('{}')
    })
  })

  it('should have JavaScript configuration applied', () => {
    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value="" />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    cy.window().then((win) => {
      // @ts-ignore
      const monaco = win.monaco

      // Check JavaScript defaults are configured
      const jsDefaults = monaco.languages.typescript.javascriptDefaults

      // Get compiler options
      const compilerOptions = jsDefaults.getCompilerOptions()

      // Verify key options are set
      expect(compilerOptions.allowJs).to.be.true
      expect(compilerOptions.target).to.exist

      // Check diagnostics options
      const diagnosticsOptions = jsDefaults.getDiagnosticsOptions()
      expect(diagnosticsOptions.noSyntaxValidation).to.be.false
    })
  })
})
