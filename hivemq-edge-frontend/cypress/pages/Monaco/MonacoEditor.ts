/**
 * Monaco Editor Page Object Model
 *
 * Monaco Editor requires special handling in E2E tests because it uses a virtual DOM
 * and web workers. This POM provides methods to interact with Monaco in E2E tests.
 *
 * @see .tasks/MONACO_TESTING_GUIDE.md for detailed patterns
 */

export class MonacoEditorPOM {
  /**
   * Set the value of a Monaco editor using the Monaco API
   * This is the recommended way to set content in E2E tests
   *
   * @param containerSelector - The selector for the container (e.g., '#root_schemaSource')
   * @param value - The text content to set
   */
  setValue(containerSelector: string, value: string) {
    // Wait for Monaco to be loaded
    cy.window().should((win) => {
      // @ts-ignore
      expect(win.monaco).to.exist
      // @ts-ignore
      expect(win.monaco.editor).to.exist
    })

    // Wait for the editor container to be visible
    cy.get(containerSelector).find('.monaco-editor').should('be.visible')
    cy.get(containerSelector).find('.monaco-editor .view-lines').should('exist')

    // Set the value using Monaco API
    cy.window().then((win) => {
      // @ts-ignore
      const editors = win.monaco.editor.getEditors()

      // Find the editor in the specified container
      const editorElement = Cypress.$(containerSelector).find('.monaco-editor')[0]
      const editor = editors.find((e: any) => {
        return e.getDomNode() === editorElement || Cypress.$(e.getDomNode()).closest(containerSelector).length > 0
      })

      if (editor) {
        editor.setValue(value)
        // Trigger change event to update React state
        editor.trigger('keyboard', 'type', { text: '' })
      } else {
        throw new Error(`Monaco editor not found in ${containerSelector}`)
      }
    })
  }

  /**
   * Get the current value of a Monaco editor
   *
   * @param containerSelector - The selector for the container
   */
  getValue(containerSelector: string) {
    return cy.window().then((win) => {
      // @ts-ignore
      const editors = win.monaco.editor.getEditors()

      const editorElement = Cypress.$(containerSelector).find('.monaco-editor')[0]
      const editor = editors.find((e: any) => {
        return e.getDomNode() === editorElement || Cypress.$(e.getDomNode()).closest(containerSelector).length > 0
      })

      if (editor) {
        return editor.getValue()
      }
      throw new Error(`Monaco editor not found in ${containerSelector}`)
    })
  }

  /**
   * Wait for Monaco editor to be ready
   *
   * @param containerSelector - The selector for the container
   */
  waitForEditor(containerSelector: string) {
    cy.window().should((win) => {
      // @ts-ignore
      expect(win.monaco).to.exist
      // @ts-ignore
      expect(win.monaco.editor).to.exist
    })

    cy.get(containerSelector).find('.monaco-editor').should('be.visible')
    cy.get(containerSelector).find('.monaco-editor .view-lines').should('exist')
  }

  /**
   * Verify that Monaco editor contains specific text
   *
   * @param containerSelector - The selector for the container
   * @param text - The text to search for
   */
  shouldContain(containerSelector: string, text: string) {
    this.getValue(containerSelector).should('contain', text)
  }
}

export const monacoEditor = new MonacoEditorPOM()
