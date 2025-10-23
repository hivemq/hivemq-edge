/// <reference types="cypress" />

const getEditor = (win: Cypress.AUTWindow, subject: JQuery<HTMLElement>) => {
  // Ensure Monaco is loaded
  // @ts-ignore
  if (!win.monaco?.editor) {
    throw new Error('Monaco Editor is not loaded')
  }

  // @ts-ignore
  const editors = win.monaco.editor.getEditors()

  if (editors.length === 0) {
    throw new Error('No Monaco editor instances found')
  }

  // Find the editor - usually there's only one in component tests
  // If there are multiple, find the one that's currently visible
  let editor = editors[0]

  if (editors.length > 1) {
    const monacoContainer = subject.find('.monaco-editor').first()[0]
    editor =
      editors.find((ed: any) => {
        const domNode = ed.getDomNode()
        return monacoContainer && (monacoContainer.contains(domNode) || domNode === monacoContainer)
      }) || editors[0]
  }

  return editor
}

/**
 * Alternative approach: Interact with Monaco Editor using a simpler method
 * that focuses on triggering the React component's onChange callback
 */
export function setMonacoEditorValue(subject: JQuery<HTMLElement>, value: string) {
  return cy.window().then((win) => {
    const editor = getEditor(win, subject)

    // Get the model and replace all content
    const model = editor.getModel()
    if (!model) {
      editor.setValue(value)
    } else {
      // Use pushEditOperations for better undo/redo support
      model.pushEditOperations(
        [],
        [
          {
            range: model.getFullModelRange(),
            text: value,
          },
        ],
        () => null
      )
    }

    // No cy.wait() - instead return and let Cypress's built-in retry handle timing
    return cy.wrap(subject)
  })
}

/**
 * Get the current value from Monaco Editor
 */
export function getMonacoEditorValue(subject: JQuery<HTMLElement>) {
  return cy.window().then((win) => {
    const editor = getEditor(win, subject)

    return editor.getValue()
  })
}
