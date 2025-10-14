import { ShellPage } from '../ShellPage.ts'
import { EDGE_MENU_LINKS, ONBOARDING } from 'cypress/utils/constants.utils.ts'

export class HomePage extends ShellPage {
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

  taskSectionTitle(task: number, section: number) {
    return cy.get('main aside').eq(task).find('section').eq(section).find('p').eq(0)
  }

  pulseOnboarding = {
    get title() {
      return homePage.taskSection(ONBOARDING.TASK_PULSE, 2).find('p').first()
    },

    get todos() {
      return homePage.taskSection(ONBOARDING.TASK_PULSE, 2).find('ul li')
    },

    get todosSummary() {
      return homePage.taskSection(ONBOARDING.TASK_PULSE, 2).find('ul li span')
    },
  }
}

export const homePage = new HomePage()
