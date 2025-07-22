import { Page } from '../Page.ts'
import { EDGE_MENU_LINKS } from 'cypress/utils/constants.utils.ts'

export class EventLogPage extends Page {
  get navLink() {
    return cy.get('nav [role="list"]').eq(0).find('li').eq(EDGE_MENU_LINKS.EVENT_LOG)
  }

  get updateEventLog() {
    return cy.getByTestId('eventLog-refetch')
  }

  table = {
    get container() {
      return cy.get('table[aria-label="Event Log"]')
    },

    get rows() {
      return cy.get('table[aria-label="Event Log"] tbody tr')
    },

    row(index: number) {
      return this.rows.eq(index)
    },

    cell(row: number, column: number | 'details' | 'created' | 'severity' | 'source' | 'event') {
      const map = ['details', 'created', 'severity', 'source', 'event']
      if (typeof column === 'string') return this.rows.eq(row).find('td').eq(map.indexOf(column))
      else return this.rows.eq(row).find('td').eq(column)
    },
  }

  detailPanel = {
    get panel() {
      return cy.get('[role="dialog"][aria-label="Event"]')
    },

    get closeButton() {
      return cy.get('[role="dialog"][aria-label="Event"] > button[aria-label="Close"] ')
    },

    get title() {
      return cy.get('[role="dialog"][aria-label="Event"] header')
    },

    get eventType() {
      return cy.get('[role="dialog"][aria-label="Event"] [data-testid="event-title-status"]')
    },
  }
}

export const eventLogPage = new EventLogPage()
