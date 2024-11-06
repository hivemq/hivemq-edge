import { MappingValidation } from '@/modules/Mappings/types.ts'
import ValidationStatus from './ValidationStatus.tsx'

const MOCK_SUBS: MappingValidation = {
  status: 'error',
  errors: [],
}

describe('ValidationStatus', () => {
  beforeEach(() => {
    cy.viewport(800, 900)
  })

  it('should render properly', () => {
    cy.mountWithProviders(<ValidationStatus validation={MOCK_SUBS} />)
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'error').should('have.text', 'Some errors')
  })

  it('should be accessible ', () => {
    cy.injectAxe()

    cy.mountWithProviders(<ValidationStatus validation={MOCK_SUBS} />)

    cy.checkAccessibility()
  })
})
