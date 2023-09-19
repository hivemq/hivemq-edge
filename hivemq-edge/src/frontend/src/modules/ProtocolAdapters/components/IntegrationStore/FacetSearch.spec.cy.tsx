/// <reference types="cypress" />

import FacetSearch from '@/modules/ProtocolAdapters/components/IntegrationStore/FacetSearch.tsx'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

describe('FacetSearch', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly when initialised with a search term', () => {
    const mockOnSubmit = cy.spy().as('onSubmit')

    cy.mountWithProviders(
      <FacetSearch items={[mockProtocolAdapter]} facet={{ search: '123', filter: undefined }} onChange={mockOnSubmit} />
    )
    cy.get('#facet-search-input').should('contain.value', '123')
    cy.getByTestId('facet-filter-clear').should('contain.text', 'No filter').should('have.attr', 'aria-pressed', 'true')
    cy.getByTestId('facet-filter-category-INDUSTRIAL').should('have.attr', 'aria-pressed', 'false')
    cy.getByTestId('facet-filter-tag-tag1').should('have.attr', 'aria-pressed', 'false')
    cy.getByTestId('facet-filter-tag-tag2').should('have.attr', 'aria-pressed', 'false')
    cy.getByTestId('facet-filter-tag-tag3').should('have.attr', 'aria-pressed', 'false')
  })

  it('should render properly when initialised with a filter', () => {
    const mockOnSubmit = cy.spy().as('onSubmit')

    cy.mountWithProviders(
      <FacetSearch
        items={[mockProtocolAdapter]}
        facet={{ search: undefined, filter: { key: 'tags', value: 'tag1' } }}
        onChange={mockOnSubmit}
      />
    )
    cy.get('#facet-search-input').should('not.contain.value')
    cy.getByTestId('facet-filter-clear')
      .should('contain.text', 'No filter')
      .should('have.attr', 'aria-pressed', 'false')
    cy.getByTestId('facet-filter-category-INDUSTRIAL').should('have.attr', 'aria-pressed', 'false')
    cy.getByTestId('facet-filter-tag-tag1').should('have.attr', 'aria-pressed', 'true')
    cy.getByTestId('facet-filter-tag-tag2').should('have.attr', 'aria-pressed', 'false')
    cy.getByTestId('facet-filter-tag-tag3').should('have.attr', 'aria-pressed', 'false')
  })

  it('should change the search term', () => {
    const mockOnSubmit = cy.spy().as('onSubmit')

    cy.mountWithProviders(
      <FacetSearch items={[mockProtocolAdapter]} facet={{ search: '123', filter: undefined }} onChange={mockOnSubmit} />
    )
    cy.get('#facet-search-input').type('4')
    cy.get('@onSubmit').should('have.been.calledWith', { search: '1234' })
  })

  it('should clear the search term', () => {
    const mockOnSubmit = cy.spy().as('onSubmit')

    cy.mountWithProviders(
      <FacetSearch items={[mockProtocolAdapter]} facet={{ search: '123', filter: undefined }} onChange={mockOnSubmit} />
    )
    cy.get('#facet-search-clear').click()
    cy.get('@onSubmit').should('have.been.calledWith', { search: null })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <FacetSearch
        items={[mockProtocolAdapter]}
        facet={{ search: 'ny term', filter: undefined }}
        onChange={cy.stub()}
      />
    )

    cy.checkAccessibility()
    cy.percySnapshot('Component: FacetSearch')
  })
})
