/// <reference types="cypress" />

import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import DeviceMetadataViewer from '@/modules/Device/components/DeviceMetadataViewer.tsx'

describe('DeviceMetadataViewer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the form', () => {
    cy.mountWithProviders(<DeviceMetadataViewer protocolAdapter={mockProtocolAdapter} />)

    cy.getByTestId('device-metadata-header').should('be.visible')
    cy.getByTestId('device-metadata-header').find('h2').should('contain.text', 'simulation')
    cy.getByTestId('device-metadata-header').find('h2 + p').should('contain.text', 'Simulation')

    cy.get('[role="alert"]').should('contain.text', 'No metadata loaded for the device')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'info')
  })
})
