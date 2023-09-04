/// <reference types="cypress" />

import { Table, TableState } from '@tanstack/react-table'
import { Adapter } from '@/api/__generated__'

import PaginationBar from './PaginationBar.tsx'

const MOCK_PAGE_SIZES = [5, 10, 20, 30, 40, 50]
const MOCK_PAGE_COUNT = 10
const MOCK_PAGE_INDEX = 2

/* eslint-disable @typescript-eslint/no-empty-function */
const MOCK_TABLE: Partial<Table<Adapter>> = {
  initialState: undefined,
  setPageIndex: () => {},
  getCanPreviousPage: () => true,
  getCanNextPage: () => true,
  getState: () => ({ pagination: { pageIndex: MOCK_PAGE_INDEX } } as TableState),
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

  it('should be accessible', () => {
    cy.injectAxe()
    // @ts-ignore force mocked partial object
    cy.mountWithProviders(<PaginationBar table={MOCK_TABLE} pageSizes={MOCK_PAGE_SIZES} />)
    cy.checkAccessibility()
    cy.percySnapshot('Component: PaginationBar')
  })
})
