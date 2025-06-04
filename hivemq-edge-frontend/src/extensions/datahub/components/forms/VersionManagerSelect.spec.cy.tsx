/// <reference types="cypress" />

import { VersionManagerSelect } from '@datahub/components/forms/VersionManagerSelect.tsx'

// @ts-ignore No need for the whole props for testing
const MOCK_RESOURCE_NAME_PROPS: WidgetProps = {
  id: 'version',
  label: 'Select a version',
  name: 'version',
  onBlur: () => undefined,
  onChange: () => undefined,
  onFocus: () => undefined,
  schema: {},
  options: { selectOptions: [1, 2, 3] },
}

describe('VersionManagerSelect', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render the VersionManagerSelect', () => {
    cy.injectAxe()

    cy.mountWithProviders(<VersionManagerSelect {...MOCK_RESOURCE_NAME_PROPS} />)

    cy.get('label#version-label').should('contain.text', 'Select a version')
    cy.get('label#version-label + div').click()
    cy.get('div#react-select-2-listbox').find('[role="option"]').as('optionList')
    cy.get('@optionList').should('have.length', 3)
    cy.get('#react-select-2-option-0').should('contain.text', '1')
    cy.get('#react-select-2-option-1').should('contain.text', '2')
    cy.get('#react-select-2-option-2').should('contain.text', '3 (latest)')
    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] ReactSelect not tagging properly the listbox
        'aria-input-field-name': { enabled: false },
      },
    })
  })
})
