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
    { status: Status.connectionStatus.CONNECTED },
    { status: Status.connectionStatus.DISCONNECTED },
    { status: Status.connectionStatus.STATELESS },
    { status: Status.connectionStatus.UNKNOWN },
    { status: Status.connectionStatus.ERROR },
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
