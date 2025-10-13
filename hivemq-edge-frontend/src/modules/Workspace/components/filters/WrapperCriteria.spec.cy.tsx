import { WrapperCriteria } from '@/modules/Workspace/components/filters/index.ts'
import { Text } from '@chakra-ui/react'

describe('WrapperCriteria', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(
      <WrapperCriteria id="my-id" label="the label" isActive onChange={onChange}>
        <Text data-testid="wrapper-content">The internal Form Control</Text>
      </WrapperCriteria>
    )

    cy.get('@onChange').should('not.have.been.called')

    cy.getByTestId('workspace-filter-my-id-container').within(() => {
      cy.get('label:has(input#workspace-filter-my-id-switch)')
        .should('have.text', 'the label')
        .should('have.attr', 'data-checked')

      cy.get('label:has(input#workspace-filter-my-id-switch)').click()
      cy.get('@onChange').should('have.been.calledWith', false)

      cy.getByTestId('workspace-filter-my-id-control').should('have.text', 'The internal Form Control')
    })
  })

  it('should render properly', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(
      <WrapperCriteria id="my-id" label="the label" isActive={false} onChange={onChange}>
        <Text data-testid="wrapper-content">The internal Form Control</Text>
      </WrapperCriteria>
    )

    cy.get('@onChange').should('not.have.been.called')

    cy.getByTestId('workspace-filter-my-id-container').within(() => {
      cy.get('label:has(input#workspace-filter-my-id-switch)')
        .should('have.text', 'the label')
        .should('not.have.attr', 'data-checked')

      cy.get('label:has(input#workspace-filter-my-id-switch)').click()
      cy.get('@onChange').should('have.been.calledWith', true)
    })
  })

  it('should be accessible', () => {
    const onChange = cy.stub().as('onChange')
    cy.injectAxe()
    cy.mountWithProviders(
      <WrapperCriteria id="my-id" label="the label" isActive onChange={onChange}>
        <Text data-testid="wrapper-content">The internal Form Control</Text>
      </WrapperCriteria>
    )

    cy.checkAccessibility()
  })
})
