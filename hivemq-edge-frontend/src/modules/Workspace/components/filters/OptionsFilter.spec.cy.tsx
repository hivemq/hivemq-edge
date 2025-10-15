import { OptionsFilter } from '@/modules/Workspace/components/filters/index.ts'

describe('FilterTopics', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<OptionsFilter />)

    cy.getByTestId('workspace-filter-options-container').within(() => {
      cy.get('[role="group"] label#workspace-filter-join-label').should('have.text', 'Join filters using')
      cy.get('[role="radiogroup"]#workspace-filter-join-input label').should('have.length', 2)
      cy.get('[role="radiogroup"]#workspace-filter-join-input label')
        .eq(0)
        .should('have.text', 'OR')
        .should('have.attr', 'data-checked')
      cy.get('[role="radiogroup"]#workspace-filter-join-input label').eq(1).should('have.text', 'AND')
      cy.get('[role="group"] #workspace-filter-join-helptext').should(
        'have.text',
        'Choose how multiple filter criteria are combined'
      )

      cy.get('[role="group"] label#workspace-filter-dynamic-update-label').should('have.text', 'Live updating')
      cy.get('[role="group"] #workspace-filter-dynamic-update-helptext').should(
        'have.text',
        'Turn on if you wish the filters to be automatically updated'
      )
      cy.get('input#workspace-filter-dynamic-update-input').should('not.have.attr', 'checked')
    })
  })

  it('should support option editing', () => {
    const onChange = cy.stub().as('onChange')

    cy.mountWithProviders(
      <OptionsFilter
        onChange={onChange}
        value={{
          isLiveUpdate: true,
          joinOperator: 'AND',
        }}
      />
    )

    cy.get('@onChange').should('not.have.been.called')
    cy.getByTestId('workspace-filter-options-container').within(() => {
      cy.get('[role="radiogroup"]#workspace-filter-join-input label')
        .eq(1)
        .should('have.text', 'AND')
        .should('have.attr', 'data-checked')

      cy.get('input#workspace-filter-dynamic-update-input').should('have.attr', 'checked', 'checked')
    })

    cy.get('[role="radiogroup"]#workspace-filter-join-input label').eq(0).click()
    cy.get('@onChange').should('have.been.calledWith', 'joinOperator', 'OR')

    cy.get('[role="group"] label#workspace-filter-dynamic-update-label').click()
    cy.get('@onChange').should('have.been.calledWith', 'isLiveUpdate', false)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<OptionsFilter />)

    cy.checkAccessibility()
  })
})
