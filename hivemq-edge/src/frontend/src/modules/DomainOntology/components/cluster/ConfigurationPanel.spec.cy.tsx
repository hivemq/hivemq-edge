import ConfigurationPanel from '@/modules/DomainOntology/components/cluster/ConfigurationPanel.tsx'

describe('ConfigurationPanel', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
  })

  it('should render properly', () => {
    const onSubmit = cy.stub().as('onSubmit')
    cy.mountWithProviders(<ConfigurationPanel groupKeys={['Runtime Status']} onSubmit={onSubmit} />)

    cy.getByTestId('cluster-form-trigger').should('have.text', 'Configuration')
    cy.get('[role="dialog"]').should('not.be.visible')

    cy.getByTestId('cluster-form-trigger').click()
    cy.get('[role="dialog"]').should('be.visible')

    cy.get('[role="dialog"] header').should('have.text', 'Configuration')
    cy.get('[role="dialog"] form h5').should('have.text', 'Grouping rules')
    cy.get('[role="dialog"] form sup').should(
      'have.text',
      'Group adapters, bridges and devices using a combination of the following rules'
    )

    cy.getByTestId('rjsf-compact-selector').should('have.length', 1)
    cy.getByTestId('rjsf-compact-selector').eq(0).as('groupRule')
    cy.get('@groupRule').find('label').should('contain.text', 'groups-0')
    cy.get('@groupRule').find('label + div').should('contain.text', 'Runtime Status')
    cy.get('@groupRule').find('label + div input').should('have.attr', 'role', 'combobox')

    cy.getByTestId('array-item-add').should('have.text', 'Add a group').click()
    cy.getByTestId('rjsf-compact-selector').should('have.length', 2)

    cy.getByAriaLabel('Remove').should('have.length', 2)
    cy.getByAriaLabel('Remove').eq(1).click()
    cy.getByTestId('rjsf-compact-selector').should('have.length', 1)

    cy.get('button[type="submit"]').should('have.text', 'Submit')
    cy.get('button[type="submit"]').click()
    cy.get('[role="dialog"]').should('not.be.visible')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ConfigurationPanel groupKeys={['key 1', 'key 2']} onSubmit={cy.stub} />)
    cy.getByTestId('cluster-form-trigger').click()

    cy.checkAccessibility(undefined, {
      rules: {
        // TODO[NVL] role="list" should be immediately followed by role="listitem"; not detected in browser
        'aria-required-children': { enabled: false },
        // h5 used for sections is not in order. Not detected on other tests
        'heading-order': { enabled: false },
        'color-contrast': { enabled: false },
      },
    })
  })
})
