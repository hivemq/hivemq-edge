import { rjsf } from 'cypress/pages'

export const cy_identifierShouldBeVisible = () => {
  rjsf.field('id').requiredLabel.should('contain.text', 'Identifier')
  rjsf.field('id').input.should('have.attr', 'name', 'root_id')
  rjsf.field('id').errors.should('have.text', "must have required property 'Identifier'")
}
