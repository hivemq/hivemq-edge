/// <reference types="cypress" />

import { ColumnDef, type SortDirection } from '@tanstack/react-table'
import PaginatedTable from './PaginatedTable.tsx'

interface MOCK_TYPE {
  column1: string
  column2: number
}

const MOCK_COLUMN: ColumnDef<MOCK_TYPE>[] = [
  {
    accessorKey: 'column1',
    header: 'item',
  },
  {
    accessorKey: 'column2',
    header: 'value',
  },
]

const MOCK_DATA = (n: number): MOCK_TYPE[] =>
  Array.from(Array(n), (_, x) => ({
    column1: `item ${x}`,
    column2: n - x,
  }))

describe('PaginatedTable', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the basic table and pagination bar', () => {
    cy.mountWithProviders(
      <PaginatedTable<MOCK_TYPE>
        data={MOCK_DATA(100)}
        columns={MOCK_COLUMN}
        pageSizes={[5, 10, 20]}
        aria-label={'table'}
      />
    )

    cy.get('th').should('have.length', 2)
    cy.get('th').eq(0).should('contain.text', 'item')
    cy.get('th').eq(1).should('contain.text', 'value')
    cy.get('tr').should('have.length', 10 + 1)

    cy.get('[aria-label="Go to the first page"]').should('be.visible')
  })

  it('should sort the columns', () => {
    cy.mountWithProviders(
      <PaginatedTable<MOCK_TYPE>
        data={MOCK_DATA(4)}
        columns={MOCK_COLUMN}
        pageSizes={[5, 10, 20]}
        aria-label={'table'}
      />
    )

    const checkRowOrder = (direction?: SortDirection) => {
      if (!direction || direction === 'asc') {
        for (const element of [1, 2, 3, 4]) {
          cy.get('tr')
            .eq(element)
            .find('td')
            .eq(0)
            .should('have.text', `item ${element - 1}`)
          cy.get('tr')
            .eq(element)
            .find('td')
            .eq(1)
            .should('have.text', 4 - element + 1)
        }
      } else {
        for (const element of [1, 2, 3, 4]) {
          cy.get('tr')
            .eq(element)
            .find('td')
            .eq(0)
            .should('have.text', `item ${4 - element}`)
          cy.get('tr').eq(element).find('td').eq(1).should('have.text', element)
        }
      }
    }

    cy.get('th').eq(0).should('have.text', 'item')
    checkRowOrder()
    cy.get('th').eq(0).click()
    cy.get('th').eq(0).should('have.attr', 'aria-sort', 'ascending')
    checkRowOrder('asc')
    cy.get('th').eq(0).click()
    cy.get('th').eq(0).should('have.attr', 'aria-sort', 'descending')
    checkRowOrder('desc')
  })

  it('should indicate when there is no data to render', () => {
    cy.mountWithProviders(
      <PaginatedTable<MOCK_TYPE> data={[]} columns={MOCK_COLUMN} pageSizes={[5, 10, 20]} aria-label={'table'} />
    )

    cy.get('[role="alert"]').should('contain.text', 'No data received yet.')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'info')
  })

  it('should render the custom nodata message', () => {
    cy.mountWithProviders(
      <PaginatedTable<MOCK_TYPE>
        data={[]}
        columns={MOCK_COLUMN}
        pageSizes={[5, 10, 20]}
        noDataText={'This is a message'}
        aria-label={'table'}
      />
    )

    cy.get('[role="alert"]').should('contain.text', 'This is a message')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'info')
  })

  it.only('should render the filters', () => {
    cy.mountWithProviders(
      <PaginatedTable<MOCK_TYPE>
        data={MOCK_DATA(4)}
        columns={MOCK_COLUMN}
        pageSizes={[5, 10, 20]}
        enableColumnFilters
        aria-label={'table'}
      />
    )

    cy.getByAriaLabel('Clear selected options').should('not.exist')

    cy.get('th').eq(0).find('div#react-select-2-placeholder').should('have.text', 'Search... (4)')
    cy.get('th').eq(0).click()
    cy.get('div#react-select-2-listbox').find("[role='option']").should('have.length', 4)
    cy.get('th').eq(0).find('input').type('item 0{Enter}')

    // wait for Debounce (should be covered by timeout
    cy.get('tbody').find('tr').should('have.length', 1)
    cy.getByAriaLabel('Clear selected options').should('be.visible')
    cy.getByAriaLabel('Clear selected options').click()
    cy.get('tbody').find('tr').should('have.length', 4)
    cy.get('th').eq(0).find('div#react-select-2-placeholder').should('have.text', 'Search... (4)')

    // TODO[NVL] Cannot test for datalist updates
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <PaginatedTable<MOCK_TYPE>
        data={MOCK_DATA(100)}
        columns={MOCK_COLUMN}
        pageSizes={[5, 10, 20]}
        aria-label={'table'}
      />
    )
    cy.checkAccessibility()
    cy.percySnapshot('Component: PaginatedTable')
  })
})
