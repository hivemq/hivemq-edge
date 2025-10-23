import { FilterStatus } from '@/modules/Workspace/components/filters/index.ts'

describe('FilterStatus', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render properly', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<FilterStatus onChange={onChange} />)

    cy.get('[role="group"] label#workspace-filter-status-label').should('have.text', 'Status')
    cy.get('[role="group"] #react-select-status-placeholder').should('have.text', 'Select statuses to trace ...')
    cy.get('[role="group"] #workspace-filter-status-trigger').click()
    cy.get('#react-select-status-listbox [role="listbox"]').within(() => {
      cy.get('[role="option"]').should('have.length', 5)
      cy.get('[role="option"]').eq(0).should('have.text', 'Connected')
      cy.get('[role="option"]').eq(1).should('have.text', 'Running')
      cy.get('[role="option"]').eq(2).should('have.text', 'Disconnected')
      cy.get('[role="option"]').eq(3).should('have.text', 'Off')
      cy.get('[role="option"]').eq(4).should('have.text', 'Error')
    })

    cy.get('[role="group"] #workspace-filter-status-trigger').type('runni{enter}')
    cy.getByTestId('workspace-filter-status-values').should('have.length', 1)
    cy.getByTestId('workspace-filter-status-values').eq(0).should('have.text', 'Running')

    cy.get('@onChange').should('have.been.calledWith', [
      {
        label: 'Running',
        status: 'STATELESS',
      },
    ])

    cy.get('[role="group"] #workspace-filter-status-trigger').type('of{enter}')
    cy.getByTestId('workspace-filter-status-values').should('have.length', 2)
    cy.getByTestId('workspace-filter-status-values').eq(0).should('have.text', 'Running')
    cy.getByTestId('workspace-filter-status-values').eq(1).should('have.text', 'Off')
    cy.get('@onChange').should('have.been.calledWith', [
      {
        label: 'Running',
        status: 'STATELESS',
      },
      {
        label: 'Off',
        status: 'STOPPED',
      },
    ])

    cy.getByAriaLabel('Clear selected options').click()
    cy.getByTestId('workspace-filter-status-values').should('have.length', 0)
    cy.get('@onChange').should('have.been.calledWith', [])
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<FilterStatus />)

    cy.checkAccessibility()
  })

  it('should render properly when disabled', () => {
    const onChange = cy.stub().as('onChange')
    cy.mountWithProviders(<FilterStatus onChange={onChange} isDisabled />)

    cy.get('[role="group"] label#workspace-filter-status-label').should('have.text', 'Status')
    cy.get('[role="group"] #workspace-filter-status-trigger').should('have.attr', 'aria-disabled', 'true')

    cy.get('[role="group"] #workspace-filter-status-trigger').click({ force: true })
    cy.get('#react-select-status-listbox [role="listbox"]').should('not.exist')

    cy.get('@onChange').should('not.have.been.called')
  })
})
