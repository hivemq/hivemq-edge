import { Route, Routes } from 'react-router-dom'

import { MockStoreWrapper } from '@datahub/__test-utils__/MockStoreWrapper.tsx'
import PropertyPanelController from '@datahub/components/controls/PropertyPanelController.tsx'
import { DataHubNodeType, DesignerStatus } from '@datahub/types.ts'

const getWrapperWith = (status?: DesignerStatus) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    return (
      <MockStoreWrapper
        config={{
          initialState: {
            status: status,
          },
        }}
      >
        <Routes>
          <Route path="/node/:type/:nodeId" element={children}></Route>
        </Routes>
      </MockStoreWrapper>
    )
  }

  return Wrapper
}

describe('PropertyPanelController', () => {
  beforeEach(() => {
    cy.viewport(800, 800)
    cy.intercept('/api/v1/management/protocol-adapters/adapters', { statusCode: 404 })
    cy.intercept('/api/v1/data-hub/data-validation/policies', { statusCode: 404 })
  })

  it('should display an error panel without an action', () => {
    cy.mountWithProviders(<PropertyPanelController />, {
      routerProps: {
        initialEntries: [`/node/UNKNOWN/123`],
      },
      wrapper: getWrapperWith(DesignerStatus.DRAFT),
    })

    cy.getByTestId('node-editor-content').should('be.visible')
    cy.getByTestId('node-editor-name').should('contain.text', 'Unknown type')
    cy.getByTestId('node-editor-icon').find('svg').should('have.attr', 'aria-label', 'Unknown type')
    cy.getByTestId('node-editor-id').should('contain.text', '1')

    cy.getByTestId('node-editor-under-construction').should('be.visible')
    cy.get('button[type="submit"]').should('not.exist')
  })

  it('should render a panel with a submit button', () => {
    cy.mountWithProviders(<PropertyPanelController />, {
      routerProps: {
        initialEntries: [`/node/${DataHubNodeType.INTERNAL}/123`],
      },
      wrapper: getWrapperWith(DesignerStatus.DRAFT),
    })

    cy.getByTestId('node-editor-under-construction').should('not.exist')
    cy.getByTestId('loading-spinner').should('be.visible')
    cy.get('button[type="submit"]').should('be.visible')
  })

  it('should disable the configuration panel if readonly', () => {
    cy.mountWithProviders(<PropertyPanelController />, {
      routerProps: {
        initialEntries: ['/node/TOPIC_FILTER/123'],
      },
      wrapper: getWrapperWith(DesignerStatus.LOADED),
    })
    cy.get('button[type="submit"]').should('be.visible').should('be.disabled')
  })
})
