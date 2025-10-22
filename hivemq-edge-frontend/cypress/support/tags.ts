/**
 * Tags for Cypress grep
 * @see There is a bug with Cypress when using tags that are syntaxtically not a string
 * @see https://github.com/cypress-io/cypress/issues/30683
 */
export const CypressTags = {
  VISUAL_REGRESSION: '@percy',
} as const
