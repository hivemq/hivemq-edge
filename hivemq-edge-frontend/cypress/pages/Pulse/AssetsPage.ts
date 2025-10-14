import { ShellPage } from 'cypress/pages/ShellPage.ts'
import { EDGE_MENU_LINKS } from 'cypress/utils/constants.utils.ts'

const TABLE_TITLE = 'List of Pulse assets'
const COLUMN_NAMES = ['name', 'description', 'topic', 'status', 'sources', 'actions'] as const
type ColumnName = (typeof COLUMN_NAMES)[number]
type FilterableColumn = Extract<ColumnName, 'topic' | 'status'>

export class AssetsPage extends ShellPage {
  get navLink() {
    return cy.get('nav [role="list"]').eq(0).find('li').eq(EDGE_MENU_LINKS.ASSETS)
  }

  search = {
    get input() {
      return cy.getByTestId('table-search-control').find('input')
    },

    get clear() {
      return cy.getByTestId('table-search-control').find('[data-testid="table-search-clear"]')
    },

    get clearAll() {
      return cy.getByTestId('table-filters-clearAll')
    },

    get filters() {
      return cy.getByTestId('filter-wrapper')
    },

    filter(column: number | FilterableColumn) {
      if (typeof column === 'string')
        return assetsPage.table.headers.eq(COLUMN_NAMES.indexOf(column)).find('[data-testid="filter-wrapper"]')
      else return assetsPage.table.headers.eq(column).find('[data-testid="filter-wrapper"]')
    },

    clearFilter(column: number | FilterableColumn) {
      if (typeof column === 'string')
        return assetsPage.table.headers
          .eq(COLUMN_NAMES.indexOf(column))
          .find('[data-testid="filter-wrapper"] [aria-label="Clear selected options"]')
      else
        return assetsPage.table.headers
          .eq(column)
          .find('[data-testid="filter-wrapper"] [aria-label="Clear selected options"]')
    },
  }

  table = {
    get container() {
      return cy.get(`table[aria-label="${TABLE_TITLE}"]`)
    },

    get rows() {
      return cy.get(`table[aria-label="${TABLE_TITLE}"] tbody tr`)
    },

    get headers() {
      return cy.get(`table[aria-label="${TABLE_TITLE}"] thead tr th`)
    },

    row(index: number) {
      return this.rows.eq(index)
    },

    cell(row: number, column: number | ColumnName) {
      if (typeof column === 'string') return this.rows.eq(row).find('td').eq(COLUMN_NAMES.indexOf(column))
      else return this.rows.eq(row).find('td').eq(column)
    },

    actions(row: number) {
      return this.cell(row, 'actions').find('button')
    },

    action(row: number, action: 'view' | 'map' | 'unmap' | 'mapper') {
      const map = {
        view: 'assets-action-view',
        map: 'assets-action-map',
        unmap: 'assets-action-delete',
        mapper: 'assets-action-mapper',
      }
      this.cell(row, 'actions').within(() => {
        cy.get('button').click()
      })
      return cy.get(`[role="menu"] button[data-testid=${map[action]}]`)
    },
  }
}

export const assetsPage = new AssetsPage()
