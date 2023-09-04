/// <reference types="cypress" />

import { ConnectionStatus } from '@/api/__generated__'
import ConnectionStatusBadge from './ConnectionStatusBadge.tsx'

describe('ConnectionStatusBadge', () => {
  beforeEach(() => {
    // run these tests as if in a desktop
    // browser with a 720p monitor
    cy.viewport(800, 250)
  })

  const selectors = [
    { status: undefined },
    { status: ConnectionStatus.status.CONNECTED },
    { status: ConnectionStatus.status.DISCONNECTED },
    { status: ConnectionStatus.status.CONNECTING },
    { status: ConnectionStatus.status.DISCONNECTING },
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
