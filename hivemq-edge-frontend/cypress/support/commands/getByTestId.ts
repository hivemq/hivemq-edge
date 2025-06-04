/**
 * * Attempts to find an element with a test id attribute of the given param value
 * @example
 * cy.getByTestId('btn')
 * */
export const getByTestId = (value: string) => {
  return cy.get(`[data-testid="${value}"]`)
}
