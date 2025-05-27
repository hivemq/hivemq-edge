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
})
