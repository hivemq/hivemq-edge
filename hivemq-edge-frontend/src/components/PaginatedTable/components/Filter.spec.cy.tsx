import { Filter } from '@/components/PaginatedTable/components/Filter.tsx'
import type { FilterMetadata } from '@/components/PaginatedTable/types.ts'
import type { ColumnDef } from '@tanstack/react-table'

const values = new Map()
values.set('test/topic', 1)
values.set('other/topic/2', 2)

describe('Filter', () => {
  beforeEach(() => {
    cy.viewport(800, 600)

    const values = new Map()
    values.set('test/topic', 1)
    values.set('other/topic/2', 2)
  })

  describe('text (faceted filter)', () => {
    it('should render properly', () => {
      const columnDefText: ColumnDef<unknown> = {
        header: 'My Column',
        meta: {
          filterOptions: {
            filterType: 'text',
          },
        } as FilterMetadata,
      }
      const onchange = cy.stub().as('onchange')

      cy.mountWithProviders(
        <Filter
          firstValue={undefined}
          id="my-filter"
          getFilterValue={() => undefined}
          getFacetedUniqueValues={() => {
            const values = new Map()
            values.set('test/topic', 1)
            values.set('other/topic/2', 2)
            return values
          }}
          getFacetedMinMaxValues={() => [0, 1]}
          setFilterValue={onchange}
          columnDef={columnDefText}
        />
      )

      cy.getByTestId('filter-wrapper').within(() => {
        cy.get('#react-select-my-filter-placeholder').should('have.text', 'Search... (2)')
        cy.get('#react-select-my-filter-placeholder + div > input').should('have.attr', 'aria-label', 'Filter column')
        cy.get('#react-select-my-filter-placeholder + div > input').click()
      })

      cy.get('#react-select-my-filter-listbox').should('be.visible')
      cy.get('@onchange').should('not.have.been.called')
      cy.get('#react-select-my-filter-listbox').within(() => {
        cy.get("[role='option']").should('have.length', 2)
        cy.get("[role='option']").eq(0).should('have.text', 'other/topic/2')
        cy.get("[role='option']").eq(1).should('have.text', 'test/topic')
        cy.get("[role='option']").eq(0).click()
      })
      cy.get('@onchange').should('have.been.calledWith', 'other/topic/2')

      cy.getByTestId('filter-wrapper').within(() => {
        cy.get('input').should('have.attr', 'aria-label', 'Filter column')
        cy.get('input').type('new term')
      })
      cy.get('#react-select-my-filter-listbox').within(() => {
        cy.get("[role='option']").should('have.length', 1)
        cy.get("[role='option']").eq(0).should('have.text', 'Add filter... new term')
      })
      cy.getByTestId('filter-wrapper').within(() => {
        cy.get('input').type('{enter}')
      })
      cy.get('@onchange').should('have.been.calledWith', 'new term')
    })
  })

  describe('datetime (faceted range)', () => {
    it('should render properly', () => {
      const onchange = cy.stub().as('onchange')
      const columnDefDate: ColumnDef<unknown> = {
        header: 'My Column',
        meta: {
          filterOptions: {
            filterType: 'datetime',
            canCreate: false,
          },
        } as FilterMetadata,
      }

      cy.mountWithProviders(
        <Filter
          firstValue={223}
          id="my-filter"
          getFilterValue={() => undefined}
          getFacetedUniqueValues={() => {
            const ff = new Map()
            ff.set('test/topic', 1)
            ff.set('test/topic/2', 2)
            return ff
          }}
          getFacetedMinMaxValues={() => [0, 1]}
          setFilterValue={onchange}
          columnDef={columnDefDate}
        />
      )

      cy.getByTestId('filter-wrapper').within(() => {
        cy.get('#react-select-my-filter-placeholder').should('have.text', 'Select a date')
        cy.get('#react-select-my-filter-placeholder + div > input').should('have.attr', 'aria-label', 'Select a date')
        cy.get('#react-select-my-filter-placeholder + div > input').click()
      })
      cy.get('#react-select-my-filter-listbox').should('be.visible')
      cy.get('@onchange').should('not.have.been.called')
      cy.get('#react-select-my-filter-listbox').within(() => {
        cy.get("[role='option']").should('have.length', 4)
        cy.get("[role='option']").eq(0).should('contain.text', 'last minute')
        cy.get("[role='option']").eq(1).should('contain.text', 'last 5 minutes')
        cy.get("[role='option']").eq(0).click()
      })
      cy.get('@onchange').should('have.been.called')
    })
  })

  it('should handle configurations', () => {
    const columnDefText = (create: boolean): ColumnDef<unknown> => ({
      header: 'My Column',
      meta: {
        filterOptions: {
          filterType: 'text',
          canCreate: create,
          placeholder: 'type your own',
          noOptionsMessage: () => 'Nothing here to see',
          formatCreateLabel: () => 'Create the thingy',
          ['aria-label']: 'My new toy',
        },
      } as FilterMetadata,
    })

    cy.mountWithProviders(
      <Filter
        firstValue={undefined}
        id="my-filter"
        getFilterValue={() => undefined}
        getFacetedUniqueValues={() => {
          return values
        }}
        getFacetedMinMaxValues={() => [0, 1]}
        setFilterValue={() => cy.stub()}
        columnDef={columnDefText(false)}
      />
    )

    // Check the placeholder props
    cy.get('#react-select-my-filter-placeholder').should('have.text', 'type your own')
    cy.get('#react-select-my-filter-placeholder + div > input').click()

    cy.get("#react-select-my-filter-listbox [role='option']").eq(0).click()

    cy.getByTestId('filter-wrapper').within(() => {
      // Check the aria-label props
      cy.get('input').should('have.attr', 'aria-label', 'My new toy')
      cy.get('input').type('new term')
    })
    // Check the noOptionsMessage props
    cy.get('#react-select-my-filter-listbox').should('have.text', 'Nothing here to see')

    cy.mountWithProviders(
      <Filter
        firstValue={undefined}
        id="my-filter"
        getFilterValue={() => undefined}
        getFacetedUniqueValues={() => {
          return values
        }}
        getFacetedMinMaxValues={() => [0, 1]}
        setFilterValue={() => cy.stub()}
        columnDef={columnDefText(true)}
      />
    )

    cy.getByTestId('filter-wrapper').within(() => {
      cy.get('input').type('new term')
    })

    cy.get('#react-select-my-filter-listbox').within(() => {
      cy.get("[role='option']").should('have.length', 1)
      // Check the formatCreateLabel props
      cy.get("[role='option']").should('have.text', 'Create the thingy')
    })
  })
})
