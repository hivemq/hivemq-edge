/// <reference types="cypress" />

import { Status } from '@/api/__generated__'
import ConnectionStatusBadge from './ConnectionStatusBadge.tsx'

describe('ConnectionStatusBadge', () => {
  beforeEach(() => {
    // run these tests as if in a desktop
    // browser with a 720p monitor
    cy.viewport(800, 250)
  })

  const testCases: Status[] = [
    { runtime: Status.runtime.STOPPED },
    { connection: Status.connection.CONNECTED },
    { connection: Status.connection.DISCONNECTED },
    { connection: Status.connection.CONNECTING },
    { connection: Status.connection.STATELESS },
    { connection: Status.connection.ERROR },
    { connection: Status.connection.UNKNOWN },
    { connection: undefined },
    { runtime: undefined },
  ]

  it.each(testCases)(
    (status) => `should render and be accessible for ${status.connection || status.runtime}`,
    (selector) => {
      cy.injectAxe()
      cy.mountWithProviders(<ConnectionStatusBadge status={selector} />)
      cy.checkAccessibility()
    }
  )

  it('should render CONNECTING with its own label', () => {
    cy.mountWithProviders(<ConnectionStatusBadge status={{ connection: Status.connection.CONNECTING }} />)
    cy.getByTestId('connection-status').should('contain.text', 'Connecting')
  })

  it('should not break on a status this build does not know', () => {
    // A frontend older than the Edge it talks to, which is ordinary during a rolling upgrade. The lookup
    // returns undefined for an unmapped member, and before the fallback the next property access threw.
    cy.mountWithProviders(<ConnectionStatusBadge status={{ connection: 'SOMETHING_NEWER' as Status.connection }} />)
    cy.getByTestId('connection-status').should('contain.text', 'Unknown')
  })
})
