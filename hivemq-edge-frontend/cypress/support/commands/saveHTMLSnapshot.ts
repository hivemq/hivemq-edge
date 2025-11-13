/**
 * Custom command to save HTML snapshot of the current page
 * This helps AI agents analyze the DOM structure when tests fail
 */
export function saveHTMLSnapshot(name: string) {
  cy.document().then((doc) => {
    const html = doc.documentElement.outerHTML
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
    const filename = `${name}_${timestamp}.html`
    cy.writeFile(`cypress/html-snapshots/${filename}`, html)
    cy.log(`HTML snapshot saved: ${filename}`)
  })
}

/**
 * Custom command to log available DOM elements for debugging
 * This provides structured information that AI agents can parse
 */
export function logDOMState(label = 'DOM State') {
  cy.document().then((doc) => {
    const ids = Array.from(doc.querySelectorAll('[id]')).map((el) => el.id)
    const testIds = Array.from(doc.querySelectorAll('[data-testid]')).map((el) => el.getAttribute('data-testid'))
    const roles = Array.from(doc.querySelectorAll('[role]')).map(
      (el) => `${el.tagName.toLowerCase()}[role="${el.getAttribute('role')}"]`
    )
    const headings = Array.from(doc.querySelectorAll('h1, h2, h3, h4, h5, h6')).map(
      (el) => `${el.tagName.toLowerCase()}: ${el.textContent?.trim().substring(0, 50)}`
    )

    const domInfo = {
      label,
      timestamp: new Date().toISOString(),
      url: doc.location.href,
      title: doc.title,
      availableIds: ids,
      availableTestIds: testIds,
      availableRoles: roles,
      headings: headings,
      bodyClasses: doc.body?.className || '',
    }

    // Write to a JSON file for AI agent parsing
    const filename = `dom-state-${label.replace(/\s+/g, '-').toLowerCase()}-${new Date().getTime()}.json`
    cy.writeFile(`cypress/html-snapshots/${filename}`, domInfo)

    cy.log(label, domInfo)
  })
}
