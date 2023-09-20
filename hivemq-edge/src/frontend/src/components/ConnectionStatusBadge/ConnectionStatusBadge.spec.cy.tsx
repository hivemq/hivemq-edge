/// <reference types="cypress" />

import { Status } from '@/api/__generated__'
import ConnectionStatusBadge from './ConnectionStatusBadge.tsx'

describe('ConnectionStatusBadge', () => {
  beforeEach(() => {
    // run these tests as if in a desktop
    // browser with a 720p monitor
    cy.viewport(800, 250)
  })

  const selectors = [
    { status: undefined },
    { status: Status.connection.CONNECTED },
    { status: Status.connection.DISCONNECTED },
    { status: Status.connection.STATELESS },
    { status: Status.connection.UNKNOWN },
    { status: Status.connection.ERROR },
  ]
  it.each(selectors)(
    (selector) => `should render and be accessible for ${selector.status}`,
    (selector) => {
      cy.injectAxe()
      cy.mountWithProviders(<ConnectionStatusBadge status={selector.status} />)
      cy.checkAccessibility()
    }
  )
})
