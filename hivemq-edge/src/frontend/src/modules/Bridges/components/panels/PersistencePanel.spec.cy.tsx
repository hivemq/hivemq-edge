/// <reference types="cypress" />

import { MOCK_CAPABILITY_PERSISTENCE } from '@/api/hooks/useFrontendServices/__handlers__'
import PersistencePanel from './PersistencePanel.tsx'

describe('PersistencePanel', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<PersistencePanel hasPersistence={MOCK_CAPABILITY_PERSISTENCE} />)

    cy.get('fieldset').should(
      'contain.text',
      'Mqtt Traffic with QoS greater than 0 is stored persistently on disc and loaded on restart of Edge.'
    )
    cy.checkAccessibility()
    cy.percySnapshot('Component: PersistencePanel')
  })
})
