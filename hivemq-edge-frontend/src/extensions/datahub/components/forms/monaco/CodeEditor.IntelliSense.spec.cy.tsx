/// <reference types="cypress" />

import type { WidgetProps } from '@rjsf/utils'
import { JavascriptEditor } from '@datahub/components/forms/CodeEditor.tsx'
import {
  assertMonacoContains,
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

describe('Monaco IntelliSense - Integration Tests (Configuration Verification)', () => {
  beforeEach(() => {
    cy.viewport(1200, 800)

    // Ignore Monaco worker loading errors
    cy.on('uncaught:exception', (err) => {
      return !(err.message.includes('importScripts') || err.message.includes('ts.worker'))
    })
  })

  it('should load Monaco editor successfully', () => {
    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value="" />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')
    cy.get('.monaco-editor .view-lines').should('exist')

    cy.window().then((win) => {
      // @ts-ignore
      expect(win.monaco).to.exist
      // @ts-ignore
      const editors = win.monaco.editor.getEditors()
      expect(editors.length).to.be.greaterThan(0)
    })
  })

  it('should provide autocomplete for publish parameter with JSDoc', () => {
    const code = `/**
 * @param {Publish} publish
 * @param {TransformContext} context
 */
function transform(publish, context) {
  publish.
}`

    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value={code} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    // Position cursor after "publish." and trigger autocomplete
    setMonacoCursorPosition(6, 11)
    triggerMonacoAutocomplete()

    // Wait for suggest widget and verify it has suggestions
    waitForSuggestWidget()
    cy.get('.monaco-editor .suggest-widget .monaco-list-row').should('exist')
  })

  it('should provide autocomplete for context parameter with JSDoc', () => {
    const code = `/**
 * @param {Publish} publish
 * @param {TransformContext} context
 */
function transform(publish, context) {
  context.
}`

    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value={code} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    // Position cursor after "context." and trigger autocomplete
    setMonacoCursorPosition(6, 11)
    triggerMonacoAutocomplete()

    // Wait for suggest widget and check for TransformContext properties
    waitForSuggestWidget()
    cy.get('.monaco-editor .suggest-widget').within(() => {
      cy.contains('clientId').should('be.visible')
    })
  })

  it('should provide autocomplete for initContext parameter with JSDoc', () => {
    const code = `/**
 * @param {InitContext} initContext
 */
function init(initContext) {
  initContext.
}`

    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value={code} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    // Position cursor after "initContext." and trigger autocomplete
    setMonacoCursorPosition(5, 15)
    triggerMonacoAutocomplete()

    // Wait for suggest widget and check for InitContext methods
    waitForSuggestWidget()
    cy.get('.monaco-editor .suggest-widget').within(() => {
      cy.contains('addBranch').should('be.visible')
    })
  })

  it('should accept full transform function code', () => {
    const code = `/**
 * @param {Publish} publish
 * @param {TransformContext} context
 */
function transform(publish, context) {
  publish.topic = 'new/topic';
  publish.payload.timestamp = Date.now();
  return publish;
}`

    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value={code} />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    // Verify code is rendered using helper that gets actual editor value
    assertMonacoContains('function transform')
    assertMonacoContains('publish.topic')
  })

  it('should load type definitions successfully', () => {
    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value="" />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    cy.window().then((win) => {
      // @ts-ignore
      const monaco = win.monaco

      // Check that TypeScript defaults are configured
      // @ts-ignore
      const jsDefaults = monaco.languages.typescript.javascriptDefaults

      expect(jsDefaults).to.exist

      // Verify extra libs are loaded (our type definitions)
      // @ts-ignore
      const extraLibs = jsDefaults.getExtraLibs()

      // Should have at least our DataHub types
      expect(Object.keys(extraLibs).length).to.be.greaterThan(0)
    })
  })
})

describe('Monaco IntelliSense - Template Insertion (API Verification)', () => {
  beforeEach(() => {
    cy.viewport(1200, 800)

    // Ignore Monaco worker loading errors
    cy.on('uncaught:exception', (err) => {
      return !(err.message.includes('importScripts') || err.message.includes('ts.worker'))
    })
  })

  it('should have template insertion command available', () => {
    cy.mountWithProviders(<JavascriptEditor {...getMockProps()} value="" />)

    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')

    cy.window().then((win) => {
      // @ts-ignore
      const monaco = win.monaco
      // @ts-ignore
      const editors = monaco.editor.getEditors()
      const editor = editors[0]

      // Check if action was registered by trying to get it
      // @ts-ignore
      const action = editor.getAction('datahub.insertTransformTemplate')

      expect(action, 'Template insertion action should be registered').to.exist
      expect(action.id).to.include('datahub.insertTransformTemplate')
    })
  })
})
