import { WrapperTestRoute } from '@/__test-utils__/hooks/WrapperTestRoute.tsx'
import DraftCTA from '@datahub/components/helpers/DraftCTA.tsx'

describe('DraftCTA', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
  })

  it('should render ', () => {
    cy.mountWithProviders(<DraftCTA />, { wrapper: WrapperTestRoute })

    cy.get('button').should('contain.text', 'Create a new policy')

    cy.getByTestId('test-pathname').should('contain.text', '/')
    cy.get('button').click()
    cy.getByTestId('test-pathname').should('contain.text', '/datahub/CREATE_POLICY')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<DraftCTA />)
    cy.checkAccessibility()
  })
})
