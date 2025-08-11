import { Page } from '../Page.ts'
import { EDGE_MENU_LINKS } from 'cypress/utils/constants.utils.ts'

export class AssetsPage extends Page {
  get navLink() {
    return cy.get('nav [role="list"]').eq(0).find('li').eq(EDGE_MENU_LINKS.ASSETS)
  }
}

export const assetsPage = new AssetsPage()
