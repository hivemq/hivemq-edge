import { WrapperTestRoute } from '@/__test-utils__/hooks/WrapperTestRoute.tsx'
import { MOCK_CAPABILITIES } from '@/api/hooks/useFrontendServices/__handlers__'
import { mockBridge, mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'
import BridgeEditorDrawer from '@/modules/Bridges/components/BridgeEditorDrawer.tsx'
import { Route, Routes } from 'react-router-dom'

const getWrapperWith = (path: string) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    return (
      <WrapperTestRoute>
        <Routes>
          <Route path={path} element={children}></Route>
        </Routes>
      </WrapperTestRoute>
    )
  }

  return Wrapper
}

const cy_bridgeShouldBeDefinedProperly = () => {
  const expectedFormProperties = [
    {
      id: 'Connection',
      content: [
        { id: 'id' },
        { id: 'host' },
        { id: 'port' },
        { id: 'clientId' },
        { id: 'password' },
        { id: 'username' },
      ],
    },
    {
      id: 'Broker Configuration',
      content: [{ id: 'cleanStart' }, { id: 'keepAlive' }, { id: 'sessionExpiry' }, { id: 'loopPreventionEnabled' }],
    },
    { id: 'Subscriptions', content: [{ id: 'localSubscriptions' }, { id: 'remoteSubscriptions' }] },
    { id: 'Security', content: [{ id: 'tlsConfiguration' }, { id: 'tlsConfiguration_enabled' }] },
    { id: 'Websocket', content: [{ id: 'websocketConfiguration' }, { id: 'websocketConfiguration_enabled' }] },
    { id: 'Persistence', content: [{ id: 'persist' }] },
  ]

  cy.get('[role="tablist"] button').each((el, index) => {
    cy.wrap(el)
      .should('have.text', expectedFormProperties[index].id)
      .should('have.attr', 'aria-selected', index === 0 ? 'true' : 'false')

    cy.get('[role="tabpanel"]')
      .eq(index)
      .within(() => {
        cy.get('div > div > [role="group"][data-testid]').each((prop, propIndex) => {
          cy.wrap(prop).should(
            'have.attr',
            'data-testid',
            `root_${expectedFormProperties[index].content[propIndex]?.id}`
          )
        })
      })
  })
}

describe('BridgeEditorDrawer', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/frontend/capabilities', MOCK_CAPABILITIES)
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getBridges')
  })

  it('should render the error if misconfigured', () => {
    cy.mountWithProviders(<BridgeEditorDrawer isNew />, {
      wrapper: getWrapperWith('/mqtt-bridges/:bridgeId'),
      routerProps: { initialEntries: [`/mqtt-bridges/${mockBridgeId}`] },
    })
    cy.wait('@getBridges')

    cy.get('[role="status"]').should('contain.text', 'There was a problem trying to view the bridge')
    cy.get('[role="status"] > div').should('have.attr', 'data-status', 'error')
    cy.get('[role="status"]').should('contain.text', "The bridge bridge-id-01 doesn't exist anymore")
  })

  it('should render the edit form', () => {
    cy.mountWithProviders(<BridgeEditorDrawer />, {
      wrapper: getWrapperWith('/mqtt-bridges/:bridgeId'),
      routerProps: { initialEntries: [`/mqtt-bridges/${mockBridgeId}`] },
    })
    cy.wait('@getBridges')

    cy.getByTestId('test-pathname').should('have.text', `/mqtt-bridges/${mockBridgeId}`)
    cy.get('[role="dialog"]#chakra-modal-bridge-editor').within(() => {
      cy.get('header').should('have.text', 'Update the bridge configuration')

      cy_bridgeShouldBeDefinedProperly()
    })
  })

  it('should render the create form', () => {
    cy.mountWithProviders(<BridgeEditorDrawer isNew={true} />, {
      wrapper: getWrapperWith('/mqtt-bridges/new'),
      routerProps: { initialEntries: [`/mqtt-bridges/new`] },
    })
    cy.wait('@getBridges')

    cy.getByTestId('test-pathname').should('have.text', `/mqtt-bridges/new`)
    cy.get('[role="dialog"]#chakra-modal-bridge-editor').within(() => {
      cy.get('header').should('have.text', 'Create a new bridge configuration')

      cy_bridgeShouldBeDefinedProperly()
    })
  })

  describe('RJSF Editor', () => {
    it('should support interpolation for destinations', () => {
      cy.mountWithProviders(<BridgeEditorDrawer isNew={true} />, {
        wrapper: getWrapperWith('/mqtt-bridges/new'),
        routerProps: { initialEntries: [`/mqtt-bridges/new`] },
      })
      cy.wait('@getBridges')

      cy.getByTestId('root_id').within(() => {
        cy.get('input').type('123')
      })

      cy.getByTestId('root_host').within(() => {
        cy.get('input').type('abc')
      })

      cy.get('[role="tablist"] button').eq(2).click()

      cy.getByTestId('root_localSubscriptions').within(() => {
        cy.getByTestId('array-item-add').click()
      })

      cy.getByTestId('root_localSubscriptions_0_destination').within(() => {
        cy.get('input').type('topic/{{}#{}}{enter}')
      })

      cy.getByTestId('root_localSubscriptions_0_destination').should('not.have.attr', 'data-invalid')
      cy.get('#root_localSubscriptions_0_destination__error').should('not.exist')

      cy.getByTestId('root_remoteSubscriptions').within(() => {
        cy.getByTestId('array-item-add').click()
      })

      cy.getByTestId('root_remoteSubscriptions_0_destination').within(() => {
        cy.get('input').type('topic/remote/{{}#{}}{enter}')
      })

      cy.getByTestId('root_remoteSubscriptions_0_destination').should('not.have.attr', 'data-invalid')
      cy.get('#root_remoteSubscriptions_0_destination__error').should('not.exist')
    })
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<BridgeEditorDrawer isNew={true} />, {
      wrapper: getWrapperWith('/mqtt-bridges/new'),
      routerProps: { initialEntries: [`/mqtt-bridges/new`] },
    })
    cy.wait('@getBridges')
    cy.checkAccessibility()
  })
})
