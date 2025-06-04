/// <reference types="cypress" />

import type { Table, TableState } from '@tanstack/react-table'
import type { Adapter } from '@/api/__generated__'

import PaginationBar from './PaginationBar.tsx'

const MOCK_PAGE_SIZES = [5, 10, 20, 30, 40, 50]
const MOCK_PAGE_COUNT = 10
const MOCK_PAGE_INDEX = 2

const MOCK_TABLE: Partial<Table<Adapter>> = {
  initialState: undefined,
  setPageIndex: () => {},
  getCanPreviousPage: () => true,
  getCanNextPage: () => true,
  getState: () => ({ pagination: { pageIndex: MOCK_PAGE_INDEX } }) as TableState,
  getPageCount: () => MOCK_PAGE_COUNT,
  setPageSize: () => undefined,
  previousPage: () => undefined,
  nextPage: () => undefined,
}

describe('PaginationBar', () => {
  beforeEach(() => {
    cy.viewport(800, 150)

    cy.stub(MOCK_TABLE, 'setPageIndex').as('setPageIndex')
    cy.stub(MOCK_TABLE, 'previousPage').as('previousPage')
    cy.stub(MOCK_TABLE, 'nextPage').as('nextPage')
    cy.stub(MOCK_TABLE, 'setPageSize').as('setPageSize')
  })

  it('should navigate to first page', () => {
    // @ts-ignore force mocked partial object
    cy.mountWithProviders(<PaginationBar table={MOCK_TABLE} pageSizes={MOCK_PAGE_SIZES} />)

    cy.getByAriaLabel('Go to the first page').should('be.visible').click()
    cy.get('@setPageIndex').should('have.been.calledOnceWith', 0)
  })

  it('should navigate to last page', () => {
    // @ts-ignore force mocked partial object
    cy.mountWithProviders(<PaginationBar table={MOCK_TABLE} pageSizes={MOCK_PAGE_SIZES} />)

    cy.getByAriaLabel('Go to the last page').should('be.visible').click()
    cy.get('@setPageIndex').should('have.been.calledOnceWith', MOCK_PAGE_COUNT - 1)
  })

  it('should navigate to previous page', () => {
    // @ts-ignore force mocked partial object
    cy.mountWithProviders(<PaginationBar table={MOCK_TABLE} pageSizes={MOCK_PAGE_SIZES} />)

    cy.getByAriaLabel('Go to the previous page').should('be.visible').click()
    cy.get('@previousPage').should('have.been.calledOnce')
  })

  it('should navigate to next page', () => {
    // @ts-ignore force mocked partial object
    cy.mountWithProviders(<PaginationBar table={MOCK_TABLE} pageSizes={MOCK_PAGE_SIZES} />)

    cy.getByAriaLabel('Go to the next page').should('be.visible').click()
    cy.get('@nextPage').should('have.been.calledOnce')
  })

  it('should render properly', () => {
    cy.mountWithProviders(
      <PaginationBar
        // @ts-ignore force mocked partial object
        table={MOCK_TABLE}
        pageSizes={MOCK_PAGE_SIZES}
        options={{ enablePaginationSizes: true, enablePaginationGoTo: true }}
      />
    )

    cy.getByTestId('table-pagination-navigation').within(() => {
      cy.get('button').should('have.length', 4)
      cy.get('button').eq(0).should('have.attr', 'aria-label', 'Go to the first page').should('not.be.disabled')
      cy.get('button').eq(1).should('have.attr', 'aria-label', 'Go to the previous page').should('not.be.disabled')
      cy.get('button').eq(2).should('have.attr', 'aria-label', 'Go to the next page').should('not.be.disabled')
      cy.get('button').eq(3).should('have.attr', 'aria-label', 'Go to the last page').should('not.be.disabled')
    })

    cy.getByTestId('table-pagination-currentPage').should('have.text', 'Page 3 of 10')
    cy.getByTestId('table-pagination-goto').within(() => {
      cy.get('label').should('have.text', 'Go to page')
      cy.get('input')
        .should('have.value', 3)
        .should('have.attr', 'role', 'spinbutton')
        .should('have.attr', 'inputmode', 'decimal')
        .should('have.attr', 'aria-valuemin', '1')
        .should('have.attr', 'aria-valuemax', '10')
    })

    cy.getByTestId('table-pagination-perPages').within(() => {
      cy.get('label').should('have.text', 'Items per page')
      cy.get('select').should('have.value', 5)
    })
  })

  it('should hide page controls if no data', () => {
    const mockEmptyTable = { ...MOCK_TABLE, getPageCount: () => 0 }
    cy.mountWithProviders(
      <PaginationBar
        // @ts-ignore force mocked partial object
        table={mockEmptyTable}
        pageSizes={MOCK_PAGE_SIZES}
        options={{ enablePaginationSizes: true, enablePaginationGoTo: true }}
      />
    )

    cy.getByTestId('table-pagination-navigation').should('be.visible')
    cy.getByTestId('table-pagination-currentPage').should('not.exist')
    cy.getByTestId('table-pagination-goto').should('not.exist')
  })

  it('should hide pagination size', () => {
    cy.mountWithProviders(
      <PaginationBar
        // @ts-ignore force mocked partial object
        table={MOCK_TABLE}
        pageSizes={MOCK_PAGE_SIZES}
        options={{ enablePaginationSizes: false, enablePaginationGoTo: true }}
      />
    )

    cy.getByTestId('table-pagination-perPage').should('not.exist')
  })

  it('should hide page go to', () => {
    cy.mountWithProviders(
      <PaginationBar
        // @ts-ignore force mocked partial object
        table={MOCK_TABLE}
        pageSizes={MOCK_PAGE_SIZES}
        options={{ enablePaginationSizes: true, enablePaginationGoTo: false }}
      />
    )

    cy.getByTestId('table-pagination-goto').should('not.exist')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(
      <PaginationBar
        // @ts-ignore force mocked partial object
        table={MOCK_TABLE}
        pageSizes={MOCK_PAGE_SIZES}
        options={{ enablePaginationSizes: true, enablePaginationGoTo: true }}
      />
    )
    cy.checkAccessibility()
    cy.percySnapshot('Component: PaginationBar')
  })
})
