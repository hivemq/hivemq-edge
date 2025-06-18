import { getPolicyWrapper, MOCK_NODE_DATA_POLICY } from '@datahub/__test-utils__/react-flow.mocks.tsx'
import DraftCTA from '@datahub/components/helpers/DraftCTA.tsx'
import { DesignerStatus } from '@datahub/types.ts'

describe('DraftCTA', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render the CTA', () => {
    cy.mountWithProviders(<DraftCTA />, {
      wrapper: getPolicyWrapper({ status: DesignerStatus.MODIFIED, nodes: [MOCK_NODE_DATA_POLICY] }),
    })

    cy.get('button').should('contain.text', 'Create a new policy')

    cy.get('[role="alertdialog"]').should('not.exist')
    cy.getByTestId('test-pathname').should('have.text', '/')
    cy.getByTestId('test-nodes').should('have.text', '1')
  })

  context('Draft confirmation', () => {
    it('should navigate if clean', () => {
      cy.mountWithProviders(<DraftCTA />, {
        wrapper: getPolicyWrapper({ status: DesignerStatus.MODIFIED, nodes: [] }),
      })

      cy.getByTestId('test-nodes').should('have.text', '0')
      cy.get('[role="alertdialog"]').should('not.exist')
      cy.get('button').click()
      cy.get('[role="alertdialog"]').should('not.exist')
      cy.getByTestId('test-pathname').should('have.text', '/datahub/CREATE_POLICY')
    })

    it('should trigger the confirmation dialog', () => {
      cy.mountWithProviders(<DraftCTA />, {
        wrapper: getPolicyWrapper({ status: DesignerStatus.MODIFIED, nodes: [MOCK_NODE_DATA_POLICY] }),
      })

      cy.getByTestId('test-nodes').should('have.text', '1')
      cy.get('[role="alertdialog"]').should('not.exist')
      cy.get('button').click()
      cy.get('section[role="alertdialog"]').within(() => {
        cy.get('header').should('contain.text', 'You already have an active draft')
        cy.getByTestId('confirmation-message').should(
          'have.text',
          'If you create a new one, the content of your current draft will be cleared from the canvas. This action cannot be reversed.'
        )
        cy.get('footer button').eq(0).should('have.text', 'Cancel')
        cy.get('footer button').eq(1).should('have.text', 'Open existing draft')
        cy.get('footer button').eq(2).should('have.text', 'Create new empty draft')

        cy.get('footer button').eq(0).click()
      })
      cy.get('[role="alertdialog"]').should('not.exist')
      cy.getByTestId('test-pathname').should('have.text', '/')
      cy.getByTestId('test-nodes').should('have.text', '1')
    })

    it('should confirm replacement', () => {
      cy.mountWithProviders(<DraftCTA />, {
        wrapper: getPolicyWrapper({ status: DesignerStatus.MODIFIED, nodes: [MOCK_NODE_DATA_POLICY] }),
      })

      cy.getByTestId('test-nodes').should('have.text', '1')
      cy.get('[role="alertdialog"]').should('not.exist')
      cy.get('button').click()
      cy.get('section[role="alertdialog"]').within(() => {
        cy.get('footer button').eq(2).click()
      })
      cy.get('[role="alertdialog"]').should('not.exist')
      cy.getByTestId('test-pathname').should('have.text', '/datahub/CREATE_POLICY')
      cy.getByTestId('test-nodes').should('have.text', '0')
    })

    it('should confirm navigation', () => {
      cy.mountWithProviders(<DraftCTA />, {
        wrapper: getPolicyWrapper({ status: DesignerStatus.MODIFIED, nodes: [MOCK_NODE_DATA_POLICY] }),
      })

      cy.getByTestId('test-nodes').should('have.text', '1')
      cy.get('[role="alertdialog"]').should('not.exist')
      cy.get('button').click()
      cy.get('section[role="alertdialog"]').within(() => {
        cy.get('footer button').eq(1).click()
      })
      cy.get('[role="alertdialog"]').should('not.exist')
      cy.getByTestId('test-pathname').should('have.text', '/datahub/CREATE_POLICY')
      cy.getByTestId('test-nodes').should('have.text', '1')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DraftCTA />)
    cy.checkAccessibility()
  })
})
