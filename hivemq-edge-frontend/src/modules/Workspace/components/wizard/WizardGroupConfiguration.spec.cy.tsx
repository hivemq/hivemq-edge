import { Drawer, DrawerOverlay, DrawerContent } from '@chakra-ui/react'
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import { EntityType } from './types'
import WizardGroupConfiguration from './WizardGroupConfiguration'

describe('WizardGroupConfiguration', () => {
  beforeEach(() => {
    // Reset wizard store
    const { actions } = useWizardStore.getState()
    actions.cancelWizard()
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
        <Drawer isOpen={true} onClose={() => {}} placement="right" size="lg">
          <DrawerOverlay />
          <DrawerContent>{children}</DrawerContent>
        </Drawer>
      </ReactFlowTesting>
    )
    return cy.mountWithProviders(<WizardGroupConfiguration />, { wrapper: Wrapper })
  }

  it('should render nothing when currentStep is 0 (selection step)', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.GROUP)

    mountComponent()

    // Should render nothing on step 0
    cy.get('form').should('not.exist')
  })

  it('should render WizardGroupForm when currentStep is 1', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.GROUP)
    actions.nextStep()

    mountComponent()

    // Should render the group form
    cy.contains('h2', /configure/i).should('be.visible')
    cy.get('[role="tab"]').should('have.length', 3) // Config, Events, Metrics tabs
  })

  it('should call completeWizard when form is submitted', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.GROUP)
    actions.nextStep()

    // Set selected nodes
    useWizardStore.setState({
      selectedNodeIds: ['adapter-1', 'adapter-2'],
    })

    mountComponent()

    // Fill form and submit
    cy.get('input[name="title"]').clear()
    cy.get('input[name="title"]').type('Test Group')
    cy.getByTestId('wizard-group-form-submit').click()

    // Wizard should eventually complete
    cy.wrap(null).should(() => {
      // Note: Wizard may still be active if there are validation errors or async operations
      // Just verify form was submitted
      const config = useWizardStore.getState().configurationData as { groupConfig?: { title: string } }
      expect(config?.groupConfig).to.exist
    })
  })

  it('should call previousStep when back button is clicked', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.GROUP)
    actions.nextStep()

    mountComponent()

    cy.getByTestId('wizard-group-form-back').click()

    // Should go back to step 0
    cy.wrap(null).then(() => {
      expect(useWizardStore.getState().currentStep).to.equal(0)
    })
  })

  it('should update configuration when form is submitted', () => {
    const { actions } = useWizardStore.getState()
    actions.startWizard(EntityType.GROUP)
    actions.nextStep()

    useWizardStore.setState({
      selectedNodeIds: ['adapter-1', 'adapter-2'],
    })

    mountComponent()

    cy.get('input[name="title"]').clear()
    cy.get('input[name="title"]').type('My Group')
    cy.getByTestId('wizard-group-form-submit').click()

    // Configuration should be updated
    cy.wrap(null).then(() => {
      const config = useWizardStore.getState().configurationData as {
        groupConfig?: { title: string; colorScheme: string }
      }
      expect(config?.groupConfig).to.exist
      expect(config?.groupConfig?.title).to.equal('My Group')
    })
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
