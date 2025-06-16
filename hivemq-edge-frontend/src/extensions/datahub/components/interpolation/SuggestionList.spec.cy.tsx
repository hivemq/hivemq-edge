import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import { MOCK_INTERPOLATION_VARIABLES } from '@datahub/api/hooks/DataHubInterpolationService/__handlers__'
import { DataHubNodeType, DesignerStatus } from '@datahub/types.ts'

import type { SuggestionListProps } from '@datahub/components/interpolation/SuggestionList.tsx'
import SuggestionList from '@datahub/components/interpolation/SuggestionList.tsx'

const mockSuggestionListProps: SuggestionListProps = {
  // @ts-ignore Not using the editor but too complex to mock
  editor: undefined,
  query: '',
}

const getWrapperFor = (type: DataHubNodeType.DATA_POLICY | DataHubNodeType.BEHAVIOR_POLICY) => {
  const wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => (
    <MockStoreWrapper
      config={{
        initialState: {
          status: DesignerStatus.DRAFT,
          type: type || DataHubNodeType.DATA_POLICY,
        },
      }}
    >
      {children}
    </MockStoreWrapper>
  )
  return wrapper
}
describe('SuggestionList', () => {
  beforeEach(() => {
    cy.viewport(400, 600)
  })

  it('should render loading states', () => {
    cy.intercept('/api/v1/data-hub/interpolation-variables', { statusCode: 404 }).as('getVariables')
    cy.mountWithProviders(<SuggestionList {...mockSuggestionListProps} command={cy.stub} />, {
      wrapper: getWrapperFor(DataHubNodeType.DATA_POLICY),
    })

    cy.wait('@getVariables')

    cy.getByTestId('interpolation-container').should('be.visible')
    cy.get('[role="alert"]').should('have.attr', 'data-status', 'loading').should('contain.text', 'Loading variables')
    cy.getByTestId('suggestion-loading-spinner').should('be.visible')

    cy.get('[role="alert"]')
      .should('have.attr', 'data-status', 'error')
      .should('contain.text', 'Error while loading the variables')
      .should('contain.text', 'Not Found')
  })

  it('should render variables for DATA POLICY', () => {
    cy.intercept('/api/v1/data-hub/interpolation-variables', MOCK_INTERPOLATION_VARIABLES).as('getVariables')
    cy.mountWithProviders(<SuggestionList {...mockSuggestionListProps} command={cy.stub} />, {
      wrapper: getWrapperFor(DataHubNodeType.DATA_POLICY),
    })

    cy.wait('@getVariables')

    cy.getByTestId('interpolation-container').should('be.visible')
    cy.getByTestId('interpolation-container').within(() => {
      cy.get('button').as('getVariables')
      cy.get('@getVariables').should('have.length', 5)
      cy.get('@getVariables').eq(0).should('contain.text', 'clientId').should('have.attr', 'aria-selected', 'true')
      cy.get('@getVariables')
        .eq(0)
        .within(() => {
          cy.getByTestId('suggestion-description').should('contain.text', 'The MQTT client ID')
        })

      cy.get('@getVariables').eq(1).should('contain.text', 'topic')
    })
  })

  it('should render variables for BEHAVIOUR POLICY', () => {
    cy.intercept('/api/v1/data-hub/interpolation-variables', MOCK_INTERPOLATION_VARIABLES).as('getVariables')
    cy.mountWithProviders(<SuggestionList {...mockSuggestionListProps} command={cy.stub} />, {
      wrapper: getWrapperFor(DataHubNodeType.BEHAVIOR_POLICY),
    })

    cy.wait('@getVariables')

    cy.getByTestId('interpolation-container').should('be.visible')
    cy.getByTestId('interpolation-container').within(() => {
      cy.get('button').as('getVariables')
      cy.get('@getVariables').should('have.length', 6)
      cy.get('@getVariables').eq(0).should('contain.text', 'clientId').should('have.attr', 'aria-selected', 'true')
      cy.get('@getVariables')
        .eq(0)
        .within(() => {
          cy.getByTestId('suggestion-description').should('contain.text', 'The MQTT client ID')
        })

      cy.get('@getVariables').eq(1).should('contain.text', 'policyId')
      cy.get('@getVariables').eq(2).should('contain.text', 'fromState')
      cy.get('@getVariables').eq(3).should('contain.text', 'toState')
    })
  })

  it('should select the variable', () => {
    const command = cy.stub().as('onCommand')
    cy.intercept('/api/v1/data-hub/interpolation-variables', MOCK_INTERPOLATION_VARIABLES).as('getVariables')

    cy.mountWithProviders(<SuggestionList {...mockSuggestionListProps} command={command} />, {
      wrapper: getWrapperFor(DataHubNodeType.DATA_POLICY),
    })
    cy.wait('@getVariables')

    cy.getByTestId('interpolation-container').should('be.visible')

    cy.getByTestId('interpolation-container').within(() => {
      cy.get('button').as('getVariables')
      cy.get('@getVariables').eq(4).click()

      cy.get('@onCommand').should('be.calledWith', { id: 'timestamp', label: 'timestamp' })
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.intercept('/api/v1/data-hub/interpolation-variables', MOCK_INTERPOLATION_VARIABLES).as('getVariables')
    cy.mountWithProviders(<SuggestionList {...mockSuggestionListProps} command={cy.stub} />, {
      wrapper: getWrapperFor(DataHubNodeType.DATA_POLICY),
    })
    cy.wait('@getVariables')

    cy.checkAccessibility()
  })
})
