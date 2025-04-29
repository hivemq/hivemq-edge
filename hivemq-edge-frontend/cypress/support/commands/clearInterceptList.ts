// @ts-ignore an import is not working
import { Interception } from 'cypress/types/net-stubbing'

export const clearInterceptList = (interceptAlias: string): void => {
  cy.get(interceptAlias + '.all').then((browserRequests: Interception) => {
    for (const request of browserRequests) {
      if (!request.requestWaited && request.state !== 'Received') {
        cy.wait(interceptAlias)
      }
    }
  })
}
