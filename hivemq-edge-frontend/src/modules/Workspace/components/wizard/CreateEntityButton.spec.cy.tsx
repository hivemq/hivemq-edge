import { MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore.ts'
import CreateEntityButton from './CreateEntityButton'
import { EntityType, IntegrationPointType } from './types'

describe('CreateEntityButton', () => {
  beforeEach(() => {
    cy.viewport(800, 800)

    useWizardStore.getState().actions.cancelWizard()

    cy.intercept('/api/v1/frontend/capabilities', { items: [MOCK_CAPABILITY_PULSE_ASSETS] })
  })

  // ✅ ACCESSIBILITY TEST - ALWAYS UNSKIPPED
  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<CreateEntityButton />)
    cy.checkAccessibility()
  })

  // ⏭️ SKIPPED TESTS - Document expected behavior but skip for rapid development

  it('should render the button with correct label', () => {
    cy.mountWithProviders(<CreateEntityButton />)

    cy.getByTestId('create-entity-button').should('be.visible').should('contain', 'Create New')
  })

  it('should open menu when clicked', () => {
    cy.mountWithProviders(<CreateEntityButton />)

    cy.getByTestId('create-entity-button').click()

    // Menu should be visible
    cy.contains('Entities').should('be.visible')
    cy.contains('Integration Points').should('be.visible')
  })

  it('should display all entity types in menu', () => {
    cy.mountWithProviders(<CreateEntityButton />)

    cy.getByTestId('create-entity-button').click()

    // Check all entity types are present
    const entityTypes = Object.values(EntityType)
    entityTypes.forEach((type) => {
      cy.getByTestId(`wizard-option-${type}`).should('exist')
    })
  })

  it('should display all integration point types in menu', () => {
    cy.mountWithProviders(<CreateEntityButton />)

    cy.getByTestId('create-entity-button').click()

    // Check all integration point types are present
    const integrationTypes = Object.values(IntegrationPointType)
    integrationTypes.forEach((type) => {
      cy.getByTestId(`wizard-option-${type}`).should('exist')
    })
  })

  it('should have icons for each menu item', () => {
    cy.mountWithProviders(<CreateEntityButton />)

    cy.getByTestId('create-entity-button').click()

    // Each menu item should have an icon
    cy.get('[role="menuitem"]').should('have.length.greaterThan', 0)
    cy.get('[role="menuitem"]').first().find('svg').should('exist')
  })

  it('should call startWizard when entity type is selected', () => {
    cy.mountWithProviders(<CreateEntityButton />)

    cy.getByTestId('create-entity-button').click()
    cy.getByTestId(`wizard-option-${EntityType.ADAPTER}`).click()

    // Verify wizard was started by checking store state
    cy.wrap(null).then(() => {
      const { isActive, entityType } = useWizardStore.getState()
      expect(isActive).to.be.true
      expect(entityType).to.equal(EntityType.ADAPTER)
    })
  })

  it.skip('should call startWizard when integration point type is selected', () => {
    // Integration points are disabled
    cy.mountWithProviders(<CreateEntityButton />)

    cy.getByTestId('create-entity-button').click()
    cy.getByTestId(`wizard-option-${IntegrationPointType.TAG}`).click()

    // Verify wizard was started by checking store state
    cy.wrap(null).then(() => {
      const { isActive, entityType } = useWizardStore.getState()
      expect(isActive).to.be.true
      expect(entityType).to.equal(IntegrationPointType.TAG)
    })
  })

  it('should close menu after selecting an option', () => {
    cy.mountWithProviders(<CreateEntityButton />)

    cy.getByTestId('create-entity-button').click()
    cy.contains('Entities').should('be.visible')

    cy.getByTestId(`wizard-option-${EntityType.ADAPTER}`).click()

    // Menu should close
    cy.contains('Entities').should('not.be.visible')
  })

  it('should support keyboard navigation', () => {
    cy.mountWithProviders(<CreateEntityButton />)

    // Focus the button
    cy.getByTestId('create-entity-button').focus()

    // Open with Enter
    cy.getByTestId('create-entity-button').type('{enter}')
    cy.contains('Entities').should('be.visible')

    // Navigate with arrow keys
    cy.focused().type('{downarrow}')
    cy.focused().should('have.attr', 'role', 'menuitem')

    // Select with Enter
    cy.focused().type('{enter}')
  })

  it('should close menu with Escape key', () => {
    cy.mountWithProviders(<CreateEntityButton />)

    cy.getByTestId('create-entity-button').click()
    cy.contains('Entities').should('be.visible')

    cy.get('body').type('{esc}')

    // Menu should close
    cy.contains('Entities').should('not.be.visible')
  })

  it('should have correct ARIA attributes', () => {
    cy.mountWithProviders(<CreateEntityButton />)

    cy.getByTestId('create-entity-button')
      .should('have.attr', 'aria-label')
      .and('contain', 'Create new entity or integration point')

    cy.getByTestId('create-entity-button').click()

    cy.get('[role="menu"]').should('exist').should('have.attr', 'aria-label')
  })

  it('should handle rapid clicks gracefully', () => {
    cy.mountWithProviders(<CreateEntityButton />)

    // Click multiple times rapidly
    cy.getByTestId('create-entity-button').click()
    cy.contains('Entities').should('be.visible')
    cy.getByTestId('create-entity-button').click()
    cy.contains('Entities').should('not.be.visible')
    cy.getByTestId('create-entity-button').click()

    // Should still work
    cy.contains('Entities').should('be.visible')
  })
})
