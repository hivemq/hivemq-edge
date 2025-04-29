/**
 *
 * @param dragLocator
 * @param dropLocator
 */
export const cy_dragAndDrop = (
  dragLocator: Cypress.Chainable<JQuery<HTMLElement>>,
  dropLocator: Cypress.Chainable<JQuery<HTMLElement>>
) => {
  dragLocator
    .realMouseDown({ button: 'left', position: 'center' })
    .realMouseMove(0, 10, { position: 'center' })
    .wait(200)
  dropLocator.realMouseMove(0, 0, { position: 'center' }).realMouseUp()
}
