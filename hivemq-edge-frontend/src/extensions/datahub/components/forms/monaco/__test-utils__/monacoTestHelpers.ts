/**
 * Monaco Test Helpers
 *
 * Helper functions for testing Monaco editor in Cypress.
 * These handle Monaco-specific quirks like &nbsp; rendering.
 */

/**
 * Get the actual text value from Monaco editor, not the DOM representation.
 * Monaco renders spaces as &nbsp; in the DOM, so DOM text checks will fail.
 * Always use this to verify editor content.
 *
 * @example
 * getMonacoEditorValue().then((value) => {
 *   expect(value).to.include('function transform')
 * })
 */
export const getMonacoEditorValue = () => {
  return cy.window().then((win) => {
    // @ts-ignore
    const editors = win.monaco.editor.getEditors()
    if (editors.length === 0) {
      throw new Error('No Monaco editor found')
    }
    return editors[0].getValue()
  })
}

/**
 * Check if Monaco editor contains specific text.
 * Use this instead of cy.get('.view-lines').should('contain.text', ...)
 *
 * @param text - Text to search for in editor
 * @example
 * assertMonacoContains('function transform')
 */
export const assertMonacoContains = (text: string) => {
  getMonacoEditorValue().then((value) => {
    expect(value, `Monaco editor should contain: "${text}"`).to.include(text)
  })
}

/**
 * Get Monaco editor instance
 *
 * @example
 * getMonacoEditor().then((editor) => {
 *   editor.setPosition({ lineNumber: 1, column: 1 })
 * })
 */
export const getMonacoEditor = () => {
  return cy.window().then((win) => {
    // @ts-ignore
    const editors = win.monaco.editor.getEditors()
    if (editors.length === 0) {
      throw new Error('No Monaco editor found')
    }
    return editors[0]
  })
}

/**
 * Trigger autocomplete at current cursor position
 *
 * @example
 * getMonacoEditor().then((editor) => {
 *   editor.setPosition({ lineNumber: 6, column: 11 })
 *   editor.focus()
 * })
 * triggerMonacoAutocomplete()
 */
export const triggerMonacoAutocomplete = () => {
  cy.window().then((win) => {
    // @ts-ignore
    const editors = win.monaco.editor.getEditors()
    if (editors.length === 0) {
      throw new Error('No Monaco editor found')
    }
    const editor = editors[0]
    // @ts-ignore
    editor.trigger('test', 'editor.action.triggerSuggest', {})
  })
}

/**
 * Wait for Monaco suggest widget to appear and be visible
 *
 * @param timeout - Max time to wait in ms (default 5000)
 * @example
 * triggerMonacoAutocomplete()
 * waitForSuggestWidget()
 * cy.get('.monaco-editor .suggest-widget').should('be.visible')
 */
export const waitForSuggestWidget = (timeout = 5000) => {
  cy.get('.monaco-editor .suggest-widget', { timeout }).should('be.visible')
}

/**
 * Set cursor position in Monaco editor
 *
 * @param lineNumber - Line number (1-based)
 * @param column - Column number (1-based)
 * @example
 * setMonacoCursorPosition(6, 11) // After "publish."
 */
export const setMonacoCursorPosition = (lineNumber: number, column: number) => {
  getMonacoEditor().then((editor) => {
    editor.setPosition({ lineNumber, column })
    editor.focus()
  })
}
