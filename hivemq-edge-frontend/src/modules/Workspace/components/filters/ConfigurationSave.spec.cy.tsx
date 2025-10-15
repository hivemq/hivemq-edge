import { ConfigurationSave } from '@/modules/Workspace/components/filters/index.ts'

describe('ConfigurationSave', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<ConfigurationSave isFilterActive={false} configurations={[]} />)

    cy.get('[role="group"] label#workspace-filter-configuration-label').should('have.text', 'Save filter')
    cy.get('[role="group"] input#workspace-filter-configuration-input')
      .should('have.attr', 'placeholder', 'Type a name')
      .should('have.attr', 'disabled', 'disabled')
      .should('have.value', '')

    cy.get('[role="group"] button#workspace-filter-configuration-save')
      .should('be.disabled')
      .should('have.attr', 'aria-label', 'Save')

    cy.get('[role="group"] #workspace-filter-configuration-helptext').should(
      'have.text',
      'Save the current configuration as a new quick filter'
    )
  })

  it('should save a quick filter', () => {
    const onSave = cy.stub().as('onSave')
    cy.mountWithProviders(
      <ConfigurationSave
        isFilterActive={true}
        onSave={onSave}
        configurations={[
          { label: 'test1', filter: {} },
          { label: 'test2', filter: {} },
        ]}
      />
    )

    cy.get('@onSave').should('not.have.been.called')

    cy.get('[role="group"] input#workspace-filter-configuration-input')
      .should('not.be.disabled')
      .should('have.value', '')

    cy.get('[role="group"] button#workspace-filter-configuration-save').should('be.disabled')

    cy.get('[role="group"] #workspace-filter-configuration-helptext').should(
      'have.text',
      'Save the current configuration as a new quick filter'
    )

    cy.get('[role="group"] input#workspace-filter-configuration-input').type('test')
    cy.get('[role="group"] button#workspace-filter-configuration-save').should('not.be.disabled')

    cy.get('[role="group"] input#workspace-filter-configuration-input').clear()
    cy.get('[role="group"] input#workspace-filter-configuration-input').type('test1')
    cy.get('[role="group"] button#workspace-filter-configuration-save').should('be.disabled')
    cy.get('[role="group"] input#workspace-filter-configuration-input').should('have.attr', 'aria-invalid', 'true')
    cy.get('[role="group"] #workspace-filter-configuration-feedback').should('have.text', 'The name is already in use')

    cy.get('[role="group"] input#workspace-filter-configuration-input').clear()
    cy.get('[role="group"] input#workspace-filter-configuration-input').type('test2')
    cy.get('[role="group"] button#workspace-filter-configuration-save').should('be.disabled')
    cy.get('[role="group"] input#workspace-filter-configuration-input').should('have.attr', 'aria-invalid', 'true')
    cy.get('[role="group"] #workspace-filter-configuration-feedback').should('have.text', 'The name is already in use')

    cy.get('[role="group"] input#workspace-filter-configuration-input').clear()
    cy.get('[role="group"] input#workspace-filter-configuration-input').type('test success')
    cy.get('[role="group"] button#workspace-filter-configuration-save').should('not.be.disabled')

    cy.get('[role="group"] button#workspace-filter-configuration-save').click()
    cy.get('@onSave').should('have.been.calledWith', 'test success')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <ConfigurationSave isFilterActive={true} configurations={[{ label: 'my first quick filter', filter: {} }]} />
    )

    cy.checkAccessibility()
  })
})
