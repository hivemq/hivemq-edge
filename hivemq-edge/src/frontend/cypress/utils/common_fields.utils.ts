import { rjsf } from 'cypress/pages'

export const cy_identifierShouldBeVisible = () => {
  rjsf.field('id').requiredLabel.should('contain.text', 'Identifier')
  rjsf.field('id').input.should('have.attr', 'name', 'root_id')
  rjsf.field('id').errors.should('have.text', "must have required property 'Identifier'")
}

export const cy_identifierShouldBeValid = () => {
  rjsf.field('id').requiredLabel.should('contain.text', 'Identifier')
  rjsf.field('id').errors.should('have.text', "must have required property 'Identifier'")

  rjsf.field('id').input.type('no space allowed')
  // [TODO[E2E] The error message should not talk regex
  rjsf.field('id').errors.should('have.text', 'must match pattern "^([a-zA-Z_0-9-_])*$"')

  rjsf.field('id').input.clear()

  rjsf.field('id').input.type('opcua-1')
  rjsf.field('id').errors.should('have.text', 'This identifier is already in use for another adapter')
}
