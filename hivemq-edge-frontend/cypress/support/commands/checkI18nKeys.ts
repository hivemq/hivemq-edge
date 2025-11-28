/**
 * Cypress command to check for missing i18n translation keys
 *
 * Usage in tests:
 * ```typescript
 * cy.mountWithProviders(<MyComponent />)
 * cy.checkI18nKeys() // Fails test if any missing keys detected
 * ```
 */

// Clear missing keys before each test to prevent accumulation
beforeEach(() => {
  cy.window().then((win) => {
    // @ts-expect-error - Custom property added by i18n.config.ts
    win.__i18nextMissingKeys = []
  })
})

declare global {
  namespace Cypress {
    interface Chainable {
      checkI18nKeys(): Chainable<void>
    }
  }
}

Cypress.Commands.add('checkI18nKeys', () => {
  cy.window().then((win) => {
    // @ts-expect-error - Custom property added by i18n.config.ts
    const missingKeys = win.__i18nextMissingKeys || []

    // Clear the array for next test
    // @ts-expect-error - Custom property added by i18n.config.ts
    win.__i18nextMissingKeys = []

    if (missingKeys.length > 0) {
      // Remove duplicates
      const uniqueKeys = Array.from(new Set(missingKeys.map((m: { key: string; ns: string }) => `${m.ns}:${m.key}`)))
      const keysList = uniqueKeys.map((k) => `  - ${k}`).join('\n')

      throw new Error(
        `\n❌ Missing i18n translation keys detected:\n\n${keysList}\n\n` +
          `Add these keys to src/locales/en/translation.json (or the appropriate namespace file).\n`
      )
    }
  })
})

export {}
