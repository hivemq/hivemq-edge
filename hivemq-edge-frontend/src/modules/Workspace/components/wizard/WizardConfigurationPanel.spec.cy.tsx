import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { EntityType } from './types'
import WizardConfigurationPanel from './WizardConfigurationPanel'

describe('WizardConfigurationPanel', () => {
  beforeEach(() => {
    // Reset wizard store
    const { actions } = useWizardStore.getState()
    actions.cancelWizard()

    // Mock API responses with proper aliases
    cy.intercept('/api/v1/management/protocol-adapters/types', { items: [mockProtocolAdapter] }).as('getAdapterTypes')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [] }).as('getAdapters')
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getBridges')
  })

  const mountComponent = () => {
    const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes: [],
          },
        }}
      >
        {children}
      </ReactFlowTesting>
    )
    return cy.mountWithProviders(<WizardConfigurationPanel />, { wrapper: Wrapper })
  }

  it('should render nothing when wizard is not active', () => {
    mountComponent()

    cy.getByTestId('wizard-configuration-panel').should('not.exist')
  })

  it('should render nothing on selection step', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.GROUP)
    // currentStep = 0 (selection)

    mountComponent()

    cy.getByTestId('wizard-configuration-panel').should('not.exist')
  })

  it('should render drawer for GROUP configuration', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.GROUP)
    actions.nextStep() // Move to configuration step

    mountComponent()

    cy.getByTestId('wizard-configuration-panel').should('be.visible')
    // Should contain the group form
    cy.contains('h2', /configure/i).should('be.visible')
  })

  it('should render drawer for ADAPTER configuration', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.ADAPTER)
    actions.nextStep()

    mountComponent()

    cy.wait('@getAdapterTypes')

    cy.getByTestId('wizard-configuration-panel').should('be.visible')
  })

  it('should render drawer for BRIDGE configuration', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.BRIDGE)
    actions.nextStep()

    mountComponent()

    cy.wait('@getBridges')

    cy.getByTestId('wizard-configuration-panel').should('be.visible')
  })

  it('should not render drawer for COMBINER (has its own drawer)', () => {
    cy.intercept('/api/v1/management/topic-filters', { statusCode: 202, log: false })

    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.COMBINER)
    // Select a node for combiner
    useWizardStore.setState({ selectedNodeIds: ['adapter-1'] })
    actions.nextStep()

    mountComponent()

    // Should not wrap in drawer
    cy.getByTestId('wizard-configuration-panel').should('not.exist')
  })

  it('should have non-closable drawer', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.GROUP)
    actions.nextStep()

    mountComponent()

    // Drawer should be visible
    cy.getByTestId('wizard-configuration-panel').should('be.visible')

    // Try to close with Escape - should not close
    cy.get('body').type('{esc}')
    cy.getByTestId('wizard-configuration-panel').should('be.visible')

    // Wizard should still be active
    cy.wrap(null).then(() => {
      expect(useWizardStore.getState().isActive).to.be.true
    })
  })

  it('should have proper ARIA label', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.GROUP)
    actions.nextStep()

    mountComponent()

    cy.getByTestId('wizard-configuration-panel').should('have.attr', 'aria-label')
  })

  it('should be accessible', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.GROUP)
    actions.nextStep()

    mountComponent()

    cy.injectAxe()
    cy.checkAccessibility()
  })
})
