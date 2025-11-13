/**
 * * Attempts to find an element with a test id attribute of the given param value
 * @example
 * cy.findByTestId('btn')
 * */
export const findByTestId = (subject: JQuery<HTMLElement>, testId: string) => {
  return cy.wrap(subject.find(`[data-testid="${testId}"]`))
}
