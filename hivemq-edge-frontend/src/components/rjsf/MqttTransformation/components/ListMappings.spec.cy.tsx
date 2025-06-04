import ListMappings from '@/components/rjsf/MqttTransformation/components/ListMappings.tsx'

describe('ListMappings', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    const onAdd = cy.stub().as('onAdd')
    cy.mountWithProviders(<ListMappings items={[]} onAdd={onAdd} />)

    cy.get('table').should('have.attr', 'aria-label', 'List of mappings')
    cy.get('table thead tr th').should('have.length', 3)
    cy.get('table thead tr th').eq(0).should('have.text', 'Sources')
    cy.get('table thead tr th').eq(1).should('have.text', 'Destination')
    cy.get('table thead tr th').eq(2).should('have.text', 'Actions')

    cy.get('table tbody tr td').should('have.length', 1).should('have.attr', 'colspan', 3)
    cy.get('table tbody tr td')
      .eq(0)
      .find('[role="alert"]')
      .should('have.attr', 'data-status', 'info')
      .should('have.text', 'No data received yet.')

    cy.get('table tfoot tr td').should('have.length', 3)
    cy.get('table tfoot tr td').eq(2).find('button').should('have.attr', 'aria-label', 'Add a new mapping')

    cy.get('@onAdd').should('not.have.been.called')
    cy.get('table tfoot tr td').eq(2).find('button').click()
    cy.get('@onAdd').should('have.been.called')
  })
})
