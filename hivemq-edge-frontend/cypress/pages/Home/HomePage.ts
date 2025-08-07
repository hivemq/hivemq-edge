import { Page } from '../Page.ts'
import { EDGE_MENU_LINKS } from 'cypress/utils/constants.utils.ts'

export class HomePage extends Page {
  get navLink() {
    return cy.get('nav [role="list"]').eq(0).find('li').eq(EDGE_MENU_LINKS.HOME)
  }

  get tasksHeader() {
    return cy.get('main h2')
  }

  get tasks() {
    return cy.get('main aside')
  }

  task(index: number) {
    return cy.get('main aside').eq(index)
  }

  taskSections(index: number) {
    return cy.get('main aside').eq(index).find('section')
  }

  taskSection(task: number, section: number) {
    return cy.get('main aside').eq(task).find('section').eq(section)
  }
}

export const homePage = new HomePage()
