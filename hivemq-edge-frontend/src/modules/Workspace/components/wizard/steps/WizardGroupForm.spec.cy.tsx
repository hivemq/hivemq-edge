import type { Node } from '@xyflow/react'
import { Drawer, DrawerOverlay, DrawerContent } from '@chakra-ui/react'
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting'
import { NodeTypes } from '@/modules/Workspace/types'
import { useWizardStore } from '@/modules/Workspace/hooks/useWizardStore'
import WizardGroupForm from './WizardGroupForm'

const mockNodes: Node[] = [
  {
    id: 'adapter-1',
    type: NodeTypes.ADAPTER_NODE,
    position: { x: 100, y: 100 },
    data: { id: 'adapter-1', label: 'Adapter 1' },
  },
  {
    id: 'adapter-2',
    type: NodeTypes.ADAPTER_NODE,
    position: { x: 300, y: 100 },
    data: { id: 'adapter-2', label: 'Adapter 2' },
  },
]

const getWrapper = () => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    return (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes: mockNodes,
          },
        }}
      >
        {children}
      </ReactFlowTesting>
    )
  }
  return Wrapper
}

describe('WizardGroupForm', () => {
  beforeEach(() => {
    // Set wizard state with selected nodes
    const { actions } = useWizardStore.getState()
    actions.cancelWizard()

    // Set up wizard state for group
    useWizardStore.setState({
      selectedNodeIds: ['adapter-1', 'adapter-2'],
    })
  })

  const mountComponent = (mockOnSubmit: ReturnType<typeof cy.stub>, mockOnBack: ReturnType<typeof cy.stub>) => {
    // Component uses DrawerHeader/Body/Footer - needs Drawer wrapper
    const Wrapper = getWrapper()
    const Component = () => (
      <Wrapper>
        <Drawer isOpen={true} onClose={() => {}} placement="right" size="lg">
          <DrawerOverlay />
          <DrawerContent>
            <WizardGroupForm onSubmit={mockOnSubmit} onBack={mockOnBack} />
          </DrawerContent>
        </Drawer>
      </Wrapper>
    )
    return cy.mountWithProviders(<Component />)
  }

  describe('rendering', () => {
    it('should render successfully', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)

      cy.contains('h2', /configure/i).should('be.visible')
    })

    it('should render all tabs', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)

      cy.get('[role="tab"]').should('have.length', 3)
      cy.get('[role="tab"]').eq(0).should('contain.text', 'Config')
      cy.get('[role="tab"]').eq(1).should('contain.text', 'Events')
      cy.get('[role="tab"]').eq(2).should('contain.text', 'Metrics')
    })

    it('should render back button', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)

      cy.getByTestId('wizard-group-form-back').should('be.visible').should('contain.text', 'Back')
    })

    it('should render submit button', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)

      cy.getByTestId('wizard-group-form-submit').should('be.visible').should('contain.text', 'Create')
    })

    it('should render GroupMetadataEditor in config tab', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)

      // Config tab should be active by default
      cy.get('[role="tabpanel"]').first().should('be.visible')
      // GroupMetadataEditor renders a form with title and color inputs
      cy.get('input[name="title"]').should('exist')
    })
  })

  describe('interactions', () => {
    it('should call onBack when back button is clicked', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)

      cy.getByTestId('wizard-group-form-back').click()
      cy.get('@onBack').should('have.been.calledOnce')
    })

    it('should call onBack when close button is clicked', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)

      cy.get('button[aria-label="Close"]').click()
      cy.get('@onBack').should('have.been.calledOnce')
    })

    it('should allow tab navigation', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)

      // Click events tab
      cy.get('[role="tab"]').eq(1).click()
      cy.get('[role="tab"]').eq(1).should('have.attr', 'aria-selected', 'true')

      // Click metrics tab
      cy.get('[role="tab"]').eq(2).click()
      cy.get('[role="tab"]').eq(2).should('have.attr', 'aria-selected', 'true')

      // Click back to config tab
      cy.get('[role="tab"]').eq(0).click()
      cy.get('[role="tab"]').eq(0).should('have.attr', 'aria-selected', 'true')
    })
  })

  describe('form submission', () => {
    it('should call onSubmit with group data when form is submitted', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)

      // Fill in the title
      cy.get('input[name="title"]').clear()
      cy.get('input[name="title"]').type('My Test Group')

      // Submit the form
      cy.getByTestId('wizard-group-form-submit').click()

      // Verify onSubmit was called with correct data
      cy.get('@onSubmit').should('have.been.calledOnce')
      cy.get('@onSubmit').should('have.been.calledWithMatch', {
        title: 'My Test Group',
        colorScheme: Cypress.sinon.match.string,
      })
    })

    it('should handle color scheme selection', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)

      // GroupMetadataEditor has color scheme selector
      cy.get('input[name="title"]').clear()
      cy.get('input[name="title"]').type('Colored Group')

      // Submit
      cy.getByTestId('wizard-group-form-submit').click()

      cy.get('@onSubmit').should('have.been.calledOnce')
    })
  })

  describe('accessibility', () => {
    it('should be accessible', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)
      cy.injectAxe()
      cy.checkAccessibility()
    })

    it('should have proper ARIA roles and labels', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)

      // Check heading role
      cy.get('h2').should('exist')

      // Check tabs have proper roles
      cy.get('[role="tablist"]').should('exist')
      cy.get('[role="tab"]').should('have.length', 3)
      cy.get('[role="tabpanel"]').should('exist')

      // Check buttons are accessible
      cy.getByTestId('wizard-group-form-back').should('be.visible')
      cy.getByTestId('wizard-group-form-submit').should('be.visible')
    })

    it('should support keyboard navigation with Tab key', () => {
      const mockOnSubmit = cy.stub().as('onSubmit')
      const mockOnBack = cy.stub().as('onBack')

      mountComponent(mockOnSubmit, mockOnBack)

      // Focus on title input
      cy.get('input[name="title"]').focus()
      cy.focused().should('have.attr', 'name', 'title')

      // Use realPress from cypress-real-events for Tab key
      cy.focused().realPress('Tab')
      // After tab, focus should move to next element
      cy.focused().should('exist')
    })
  })
})
